package com.app.location.workers

import android.Manifest
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import androidx.annotation.NonNull
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.app.location.R
import com.app.location.models.DownloadStatus
import com.app.location.models.DownloadTask
import com.app.location.util.IntentUtils.validatedFileIntent
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern

//https://heartbeat.fritz.ai/workmanager-in-android-enqueue-and-execute-background-tasks-f5eac17b4dbb
class DownloadWorker(@NonNull val context: Context,
                     @NonNull params: WorkerParameters) : Worker(context, params) {
    private val charsetPattern = Pattern.compile("(?i)\\bcharset=\\s*\"?([^\\s;\"]*)")
    private var dbHelper: TaskDbHelper? = null
    private var taskDao: TaskDao? = null
    private var showNotification = false
    private var clickToOpenDownloadedFile = false
    private var debug = false
    private var lastProgress = 0
    private var primaryId = 0
    private var msgStarted: String? = null
    private var msgInProgress: String? = null
    private var msgCanceled: String? = null
    private var msgFailed: String? = null
    private var msgPaused: String? = null
    private var msgComplete: String? = null
    private var lastCallUpdateNotification: Long = 0

    private fun startBackgroundIsolate(context: Context) {
    }

    @NonNull
    override fun doWork(): Result {
        val context: Context = applicationContext
        dbHelper = TaskDbHelper.getInstance(context)
        taskDao = TaskDao(dbHelper!!)
        val url: String = inputData.getString(ARG_URL)!!
        val filename: String = inputData.getString(ARG_FILE_NAME)!!
        val savedDir: String = inputData.getString(ARG_SAVED_DIR)!!
        val headers: String = inputData.getString(ARG_HEADERS)!!
        val isResume: Boolean = inputData.getBoolean(ARG_IS_RESUME, false)
        debug = inputData.getBoolean(ARG_DEBUG, false)
        val res: Resources = applicationContext.resources
        msgStarted = res.getString(R.string.downloader_notification_started)
        msgInProgress = res.getString(R.string.downloader_notification_in_progress)
        msgCanceled = res.getString(R.string.downloader_notification_canceled)
        msgFailed = res.getString(R.string.downloader_notification_failed)
        msgPaused = res.getString(R.string.downloader_notification_paused)
        msgComplete = res.getString(R.string.downloader_notification_complete)
        log("DownloadWorker{url=$url,filename=$filename,savedDir=$savedDir,header=$headers,isResume=$isResume")
        showNotification = inputData.getBoolean(ARG_SHOW_NOTIFICATION, false)
        clickToOpenDownloadedFile = inputData.getBoolean(ARG_OPEN_FILE_FROM_NOTIFICATION, false)
        val task: DownloadTask = taskDao!!.loadTask(id.toString())!!
        primaryId = task.primaryId
        setupNotification(context)
        updateNotification(context, filename ?: url, DownloadStatus.RUNNING, task.progress, null, false)
        taskDao!!.updateTask(id.toString(), DownloadStatus.RUNNING, task.progress)
        return try {
            downloadFile(context, url, savedDir, filename, headers, isResume)
            cleanUp()
            dbHelper = null
            taskDao = null
            Result.success()
        } catch (e: Exception) {
            updateNotification(context, filename ?: url, DownloadStatus.FAILED, -1, null, true)
            taskDao!!.updateTask(id.toString(), DownloadStatus.FAILED, lastProgress)
            e.printStackTrace()
            dbHelper = null
            taskDao = null
            Result.failure()
        }
    }

    private fun setupHeaders(conn: HttpURLConnection?, headers: String) {
        if (!TextUtils.isEmpty(headers)) {
            log("Headers = $headers")
            try {
                val json = JSONObject(headers)
                val it = json.keys()
                while (it.hasNext()) {
                    val key = it.next()
                    conn!!.setRequestProperty(key, json.getString(key))
                }
                conn!!.doInput = true
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }

    private fun setupPartialDownloadedDataHeader(conn: HttpURLConnection?, filename: String?, savedDir: String): Long {
        val saveFilePath = savedDir + File.separator + filename
        val partialFile = File(saveFilePath)
        val downloadedBytes = partialFile.length()
        log("Resume download: Range: bytes=$downloadedBytes-")
        conn!!.setRequestProperty("Accept-Encoding", "identity")
        conn.setRequestProperty("Range", "bytes=$downloadedBytes-")
        conn.doInput = true
        return downloadedBytes
    }

    @Throws(IOException::class)
    private fun downloadFile(context: Context, fileURL: String, savedDir: String, filename: String?, headers: String,
                             isResume: Boolean) {
        var filename = filename
        var url = fileURL
        var resourceUrl: URL
        var base: URL
        var next: URL
        val visited: MutableMap<String, Int>
        var httpConn: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null
        val saveFilePath: String
        var location: String
        var downloadedBytes: Long = 0
        var responseCode: Int
        var times: Int
        visited = HashMap()
        try {
            // handle redirection logic
            loop@ while (true) {
                if (!visited.containsKey(url)) {
                    times = 1
                    visited[url] = times
                } else {
                    times = visited[url]!! + 1
                }
                if (times > 3) throw IOException("Stuck in redirect loop")
                resourceUrl = URL(url)
                log("Open connection to $url")
                httpConn = resourceUrl.openConnection() as HttpURLConnection
                httpConn!!.connectTimeout = 15000
                httpConn.readTimeout = 15000
                httpConn.instanceFollowRedirects = false // Make the logic below easier to detect redirections
                httpConn.setRequestProperty("User-Agent", "Mozilla/5.0...")

                // setup request headers if it is set
                setupHeaders(httpConn, headers)
                // try to continue downloading a file from its partial downloaded data.
                if (isResume) {
                    downloadedBytes = setupPartialDownloadedDataHeader(httpConn, filename, savedDir)
                }
                responseCode = httpConn.responseCode
                when (responseCode) {
                    HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_SEE_OTHER, HttpURLConnection.HTTP_MOVED_TEMP -> {
                        log("Response with redirection code")
                        location = httpConn.getHeaderField("Location")
                        log("Location = $location")
                        base = URL(fileURL)
                        next = URL(base, location) // Deal with relative URLs
                        url = next.toExternalForm()
                        log("New url: $url")
                        continue@loop
                    }
                }
                break
            }
            httpConn!!.connect()
            if ((responseCode == HttpURLConnection.HTTP_OK || isResume && responseCode == HttpURLConnection.HTTP_PARTIAL) && !isStopped) {
                val contentType = httpConn.contentType
                val contentLength = httpConn.contentLength
                log("Content-Type = $contentType")
                log("Content-Length = $contentLength")
                val charset = getCharsetFromContentType(contentType)
                log("Charset = $charset")
                if (!isResume) {
                    // try to extract filename from HTTP headers if it is not given by user
                    if (filename == null) {
                        val disposition = httpConn.getHeaderField("Content-Disposition")
                        log("Content-Disposition = $disposition")
                        if (disposition != null && !disposition.isEmpty()) {
                            val name = disposition.replaceFirst("(?i)^.*filename=\"?([^\"]+)\"?.*$".toRegex(), "$1")
                            filename = URLDecoder.decode(name, charset ?: "ISO-8859-1")
                        }
                        if (filename == null || filename.isEmpty()) {
                            filename = url.substring(url.lastIndexOf("/") + 1)
                        }
                    }
                }
                saveFilePath = savedDir + File.separator + filename
                log("fileName = $filename")
                taskDao!!.updateTask(id.toString(), filename, contentType)

                // opens input stream from the HTTP connection
                inputStream = httpConn.inputStream

                // opens an output stream to save into file
                outputStream = FileOutputStream(saveFilePath, isResume)
                var count = downloadedBytes
                var bytesRead = -1
                val buffer = ByteArray(BUFFER_SIZE)
                while (inputStream.read(buffer).also { bytesRead = it } != -1 && !isStopped) {
                    count += bytesRead.toLong()
                    val progress = (count * 100 / (contentLength + downloadedBytes)).toInt()
                    outputStream.write(buffer, 0, bytesRead)
                    if ((lastProgress == 0 || progress > lastProgress + STEP_UPDATE || progress == 100)
                            && progress != lastProgress) {
                        lastProgress = progress
                        updateNotification(context, filename, DownloadStatus.RUNNING, progress, null, false)

                        // This line possibly causes system overloaded because of accessing to DB too many ?!!!
                        // but commenting this line causes tasks loaded from DB missing current downloading progress,
                        // however, this missing data should be temporary and it will be updated as soon as
                        // a new bunch of data fetched and a notification sent
                        taskDao!!.updateTask(id.toString(), DownloadStatus.RUNNING, progress)
                    }
                }
                val task: DownloadTask = taskDao!!.loadTask(id.toString())!!
                val progress = if (isStopped && task.resumable) lastProgress else 100
                val status = if (isStopped) if (task.resumable) DownloadStatus.PAUSED else DownloadStatus.CANCELED else DownloadStatus.COMPLETE
                val storage = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                var pendingIntent: PendingIntent? = null
                if (status == DownloadStatus.COMPLETE) {
                    if (isImageOrVideoFile(contentType) && isExternalStoragePath(saveFilePath)) {
                        addImageOrVideoToGallery(filename, saveFilePath, getContentTypeWithoutCharset(contentType))
                    }
                    if (clickToOpenDownloadedFile && storage == PackageManager.PERMISSION_GRANTED) {
                        val intent = validatedFileIntent(applicationContext, saveFilePath, contentType)
                        if (intent != null) {
                            log("Setting an intent to open the file $saveFilePath")
                            pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)
                        } else {
                            log("There's no application that can open the file $saveFilePath")
                        }
                    }
                }
                updateNotification(context, filename, status, progress, pendingIntent, true)
                taskDao!!.updateTask(id.toString(), status, progress)
                log(if (isStopped) "Download canceled" else "File downloaded")
            } else {
                val task: DownloadTask = taskDao!!.loadTask(id.toString())!!
                val status = if (isStopped) if (task.resumable) DownloadStatus.PAUSED else DownloadStatus.CANCELED else DownloadStatus.FAILED
                updateNotification(context, filename ?: fileURL, status, -1, null, true)
                taskDao!!.updateTask(id.toString(), status, lastProgress)
                log(if (isStopped) "Download canceled" else "Server replied HTTP code: $responseCode")
            }
        } catch (e: IOException) {
            updateNotification(context, filename ?: fileURL, DownloadStatus.FAILED, -1, null, true)
            taskDao!!.updateTask(id.toString(), DownloadStatus.FAILED, lastProgress)
            e.printStackTrace()
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            httpConn?.disconnect()
        }
    }

    private fun cleanUp() {
        val task: DownloadTask = taskDao!!.loadTask(id.toString())!!
        if (task != null && task.status != DownloadStatus.COMPLETE && !task.resumable) {
            var filename = task.filename
            if (filename == null) {
                filename = task.url.substring(task.url.lastIndexOf("/") + 1, task.url.length)
            }

            // check and delete uncompleted file
            val saveFilePath = task.savedDir + File.separator + filename
            val tempFile = File(saveFilePath)
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    private val notificationIconRes: Int
        private get() {
            try {
                val applicationInfo: ApplicationInfo = applicationContext.packageManager.getApplicationInfo(applicationContext
                        .packageName, PackageManager.GET_META_DATA)
                val appIconResId = applicationInfo.icon
                return applicationInfo.metaData.getInt("com.app.downloader.NOTIFICATION_ICON", appIconResId)
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
            return 0
        }

    private fun setupNotification(context: Context) {
        if (!showNotification) return
        // Make a channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            val res: Resources = context.resources
            val channelName = res.getString(R.string.downloader_notification_channel_name)
            val channelDescription = res.getString(R.string.downloader_notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, channelName, importance)
            channel.description = channelDescription
            channel.setSound(null, null)

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            // Add the channel
            val notificationManager = applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(context: Context, title: String?, status: Int, progress: Int,
                                   intent: PendingIntent?, finalize: Boolean) {
        sendUpdateProcessEvent(status, progress)

        // Show the notification
        if (showNotification) {
            // Create the notification
            val builder: NotificationCompat.Builder = NotificationCompat.Builder(context, CHANNEL_ID).setContentTitle(title)
                    .setContentIntent(intent)
                    .setOnlyAlertOnce(true)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
            when (status) {
                DownloadStatus.RUNNING -> {
                    when {
                        progress <= 0 -> {
                            builder.setContentText(msgStarted)
                                    .setProgress(0, 0, false)
                            builder.setOngoing(false)
                                    .setSmallIcon(notificationIconRes)
                        }
                        progress < 100 -> {
                            builder.setContentText(msgInProgress)
                                    .setProgress(100, progress, false)
                            builder.setOngoing(true).setSmallIcon(android.R.drawable.stat_sys_download)
                        }
                        else -> {
                            builder.setContentText(msgComplete).setProgress(0, 0, false)
                            builder.setOngoing(false)
                                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                        }
                    }
                }
                DownloadStatus.CANCELED -> {
                    builder.setContentText(msgCanceled).setProgress(0, 0, false)
                    builder.setOngoing(false)
                            .setSmallIcon(android.R.drawable.stat_sys_download_done)
                }
                DownloadStatus.FAILED -> {
                    builder.setContentText(msgFailed).setProgress(0, 0, false)
                    builder.setOngoing(false)
                            .setSmallIcon(android.R.drawable.stat_sys_download_done)
                }
                DownloadStatus.PAUSED -> {
                    builder.setContentText(msgPaused).setProgress(0, 0, false)
                    builder.setOngoing(false)
                            .setSmallIcon(android.R.drawable.stat_sys_download_done)
                }
                DownloadStatus.COMPLETE -> {
                    builder.setContentText(msgComplete).setProgress(0, 0, false)
                    builder.setOngoing(false)
                            .setSmallIcon(android.R.drawable.stat_sys_download_done)
                }
                else -> {
                    builder.setProgress(0, 0, false)
                    builder.setOngoing(false).setSmallIcon(notificationIconRes)
                }
            }

            // Note: Android applies a rate limit when updating a notification.
            // If you post updates to a notification too frequently (many in less than one second),
            // the system might drop some updates. (https://developer.android.com/training/notify-user/build-notification#Updating)
            //
            // If this is progress update, it's not much important if it is dropped because there're still incoming updates later
            // If this is the final update, it must be success otherwise the notification will be stuck at the processing state
            // In order to ensure the final one is success, we check and sleep a second if need.
            if (System.currentTimeMillis() - lastCallUpdateNotification < 1000) {
                if (finalize) {
                    log("Update too frequently!!!!, but it is the final update, we should sleep a second to ensure the update call can be processed")
                    try {
                        Thread.sleep(1000)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                } else {
                    log("Update too frequently!!!!, this should be dropped")
                    return
                }
            }
            log("Update notification: {notificationId: $primaryId, title: $title, status: $status, progress: $progress}")
            NotificationManagerCompat.from(context).notify(primaryId, builder.build())
            lastCallUpdateNotification = System.currentTimeMillis()
        }
    }

    private fun sendUpdateProcessEvent(status: Int, progress: Int) {
//        val args: MutableList<Any> = ArrayList()
//        val callbackHandle: Long = getInputData().getLong(ARG_CALLBACK_HANDLE, 0)
//        args.add(callbackHandle)
//        args.add(getId().toString())
//        args.add(status)
//        args.add(progress)
//        synchronized(isolateStarted) {
//            if (!isolateStarted.get()) {
//                isolateQueue.add(args)
//            } else {
//                Handler(getApplicationContext().getMainLooper()).post(Runnable {
//                    backgroundChannel.invokeMethod("", args)
//                })
//            }
//        }
    }

    private fun getCharsetFromContentType(contentType: String?): String? {
        if (contentType == null) return null
        val m = charsetPattern.matcher(contentType)
        return if (m.find()) {
            m.group(1).trim { it <= ' ' }.toUpperCase()
        } else null
    }

    private fun getContentTypeWithoutCharset(contentType: String?): String? {
        return contentType?.split(";")?.toTypedArray()?.get(0)?.trim { it <= ' ' }
    }

    private fun isImageOrVideoFile(contentType: String): Boolean {
        var contentType: String? = contentType
        contentType = getContentTypeWithoutCharset(contentType)
        return contentType != null && (contentType.startsWith("image/") || contentType.startsWith("video"))
    }

    private fun isExternalStoragePath(filePath: String?): Boolean {
        val externalStorageDir = Environment.getExternalStorageDirectory()
        return filePath != null && externalStorageDir != null && filePath.startsWith(externalStorageDir.path)
    }

    private fun addImageOrVideoToGallery(fileName: String?, filePath: String?, contentType: String?) {
        if (contentType != null && filePath != null && fileName != null) {
            if (contentType.startsWith("image/")) {
                val values = ContentValues()
                values.put(MediaStore.Images.Media.TITLE, fileName)
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                values.put(MediaStore.Images.Media.DESCRIPTION, "")
                values.put(MediaStore.Images.Media.MIME_TYPE, contentType)
                values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis())
                values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                values.put(MediaStore.Images.Media.DATA, filePath)
                log("insert $values to MediaStore")
                val contentResolver: ContentResolver = applicationContext.contentResolver
                contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            } else if (contentType.startsWith("video")) {
                val values = ContentValues()
                values.put(MediaStore.Video.Media.TITLE, fileName)
                values.put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                values.put(MediaStore.Video.Media.DESCRIPTION, "")
                values.put(MediaStore.Video.Media.MIME_TYPE, contentType)
                values.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis())
                values.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())
                values.put(MediaStore.Video.Media.DATA, filePath)
                log("insert $values to MediaStore")
                val contentResolver: ContentResolver = applicationContext.contentResolver
                contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            }
        }
    }

    private fun log(message: String) {
        if (debug) {
            Log.d(TAG, message)
        }
    }

    companion object {
        const val ARG_URL = "url"
        const val ARG_FILE_NAME = "file_name"
        const val ARG_SAVED_DIR = "saved_file"
        const val ARG_HEADERS = "headers"
        const val ARG_IS_RESUME = "is_resume"
        const val ARG_SHOW_NOTIFICATION = "show_notification"
        const val ARG_OPEN_FILE_FROM_NOTIFICATION = "open_file_from_notification"
        const val ARG_CALLBACK_HANDLE = "callback_handle"
        const val ARG_DEBUG = "debug"
        private val TAG = DownloadWorker::class.java.simpleName
        private const val BUFFER_SIZE = 4096
        private const val CHANNEL_ID = "FLUTTER_DOWNLOADER_NOTIFICATION"
        private const val STEP_UPDATE = 10
        private val isolateStarted = AtomicBoolean(false)
        private val isolateQueue = ArrayDeque<List<*>>()
    }

    init {
        Handler(context.mainLooper).post { startBackgroundIsolate(context) }
    }
}