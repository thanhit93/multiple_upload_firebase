package com.app.upload_file.ui.main

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.app.upload_file.models.DownloadStatus
import com.app.upload_file.util.IntentUtils
import com.app.upload_file.workers.DownloadWorker
import com.app.upload_file.workers.TaskDao
import com.app.upload_file.workers.TaskDbHelper
import io.reactivex.disposables.CompositeDisposable
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap

/**
 * Created by tt on 20/02/2021.
 */
class MainPresenter : MainContract.Presenter {

    private val subscriptions = CompositeDisposable()
    private lateinit var view: MainContract.View
    private lateinit var context: Context
    private lateinit var dbHelper: TaskDbHelper
    private lateinit var taskDao: TaskDao
    private val debugMode = 0

    override fun subscribe() {

    }

    override fun unsubscribe() {
        subscriptions.clear()
    }

    override fun attach(view: MainContract.View) {
        this.view = view
        view.showListFragment() // as default
    }

    override fun attachContext(view: MainContract.View, context: Context) {
        this.view = view
        this.context = context
        view.showListFragment() // as default
    }

    override fun onDrawerOptionAboutClick() {
        view.showAboutFragment()
    }

    override fun initDatabaseHelper() {
        dbHelper = TaskDbHelper.getInstance(context)!!
        taskDao = TaskDao(dbHelper)
    }

    // DOWNLOAD FILE
    //https://github.com/fluttercommunity/flutter_downloader/blob/master/android/src/main/java/vn/hunghd/flutterdownloader/FlutterDownloaderPlugin.java
    private fun buildRequest(url: String, savedDir: String, filename: String, headers: String,
                             showNotification: Boolean, openFileFromNotification: Boolean, isResume: Boolean,
                             requiresStorageNotLow: Boolean): WorkRequest? {
        return OneTimeWorkRequest.Builder(DownloadWorker::class.java)
                .setConstraints(Constraints.Builder()
                        .setRequiresStorageNotLow(requiresStorageNotLow)
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .addTag(MainActivity.TAG)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.SECONDS)
                .setInputData(Data.Builder()
                        .putString(DownloadWorker.ARG_URL, url)
                        .putString(DownloadWorker.ARG_SAVED_DIR, savedDir)
                        .putString(DownloadWorker.ARG_FILE_NAME, filename)
                        .putString(DownloadWorker.ARG_HEADERS, headers)
                        .putBoolean(DownloadWorker.ARG_SHOW_NOTIFICATION, showNotification)
                        .putBoolean(DownloadWorker.ARG_OPEN_FILE_FROM_NOTIFICATION, openFileFromNotification)
                        .putBoolean(DownloadWorker.ARG_IS_RESUME, isResume)
                        //.putLong(DownloadWorker.ARG_CALLBACK_HANDLE, callbackHandle)
                        .putBoolean(DownloadWorker.ARG_DEBUG, debugMode == 1)
                        .build()
                )
                .build()
    }

    override fun enqueue(url: String, savedDir: String, filename: String, headers: String, showNotification: Boolean, openFileFromNotification: Boolean, requiresStorageNotLow: Boolean): String {
        val request = buildRequest(url, savedDir, filename, headers, showNotification,
                openFileFromNotification, false, requiresStorageNotLow)
        WorkManager.getInstance(context).enqueue(request!!)
        val taskId = request.id.toString()
        sendUpdateProgress(taskId, DownloadStatus.ENQUEUED, 0)
        taskDao.insertOrUpdateNewTask(taskId, url, DownloadStatus.ENQUEUED, 0,
                filename, savedDir, headers, showNotification, openFileFromNotification)
        return taskId
    }

    private fun sendUpdateProgress(id: String, status: Int, progress: Int) {
        val args: MutableMap<String, Any> = HashMap()
        args["task_id"] = id
        args["status"] = status
        args["progress"] = progress
        Log.d(MainActivity.TAG, "updateProgress: ${args.toString()}")
    }

    private fun cancel(taskId: String) {
        WorkManager.getInstance(context).cancelWorkById(UUID.fromString(taskId))
    }

    private fun cancelAll() {
        WorkManager.getInstance(context).cancelAllWorkByTag(MainActivity.TAG)
    }

    private fun pause(taskId: String) {
        taskDao.updateTask(taskId, true)
        WorkManager.getInstance(context).cancelWorkById(UUID.fromString(taskId))
    }

    private fun resume(taskId: String, requiresStorageNotLow: Boolean = false): String {
        val task = taskDao.loadTask(taskId)
        if (task != null) {
            if (task.status == DownloadStatus.PAUSED) {
                var filename = task.filename
                if (filename == null) {
                    filename = task.url.substring(task.url.lastIndexOf("/") + 1, task.url.length)
                }
                val partialFilePath = task.savedDir + File.separator + filename
                val partialFile = File(partialFilePath)
                if (partialFile.exists()) {
                    val request = buildRequest(task.url, task.savedDir, task.filename, task.headers,
                            task.showNotification, task.openFileFromNotification, true, requiresStorageNotLow)
                    val newTaskId = request!!.id.toString()
                    sendUpdateProgress(newTaskId, DownloadStatus.RUNNING, task.progress)
                    taskDao.updateTask(taskId, newTaskId, DownloadStatus.RUNNING, task.progress, false)
                    WorkManager.getInstance(context).enqueue(request)
                    return newTaskId
                } else {
                    Log.e("invalid_data", "not found partial downloaded data, this task cannot be resumed", null)
                    return ""
                }
            } else {
                Log.e("invalid_status", "only paused task can be resumed", null)
                return ""
            }
        } else {
            Log.e("invalid_task_id", "not found task corresponding to given task id", null)
            return ""
        }
    }

    private fun retry(taskId: String, requiresStorageNotLow: Boolean = false): String {
        val task = taskDao.loadTask(taskId)
        if (task != null) {
            when (task.status) {
                DownloadStatus.FAILED, DownloadStatus.CANCELED -> {
                    val request = buildRequest(task.url, task.savedDir, task.filename, task.headers, task.showNotification,
                            task.openFileFromNotification, false, requiresStorageNotLow)
                    val newTaskId = request!!.id.toString()
                    sendUpdateProgress(newTaskId, DownloadStatus.ENQUEUED, task.progress)
                    taskDao.updateTask(taskId, newTaskId, DownloadStatus.ENQUEUED, task.progress, false)
                    WorkManager.getInstance(context).enqueue(request)
                    return newTaskId
                }
                else -> {
                    Log.e("invalid_status", "only failed and canceled task can be retried", null)
                    return ""

                }
            }
        } else {
            Log.e("invalid_task_id", "not found task corresponding to given task id", null)
            return ""
        }
    }

    private fun open(taskId: String): Boolean {
        val task = taskDao.loadTask(taskId)
        if (task != null) {
            if (task.status == DownloadStatus.COMPLETE) {
                val fileURL = task.url
                val savedDir = task.savedDir
                var filename = task.filename
                if (filename == null) {
                    filename = fileURL.substring(fileURL.lastIndexOf("/") + 1, fileURL.length)
                }
                val saveFilePath = savedDir + File.separator + filename
                val intent = IntentUtils.validatedFileIntent(context, saveFilePath, task.mimeType)
                if (intent != null) {
                    context.startActivity(intent)
                    return true
                } else {
                    return false
                }
            } else {
                Log.e("invalid_status", "only success task can be opened", null)
                return false
            }
        } else {
            Log.e("invalid_task_id", "not found task corresponding to given task id", null)
            return false
        }
    }

    private fun remove(taskId: String, shouldDeleteContent: Boolean): Boolean {
        val task = taskDao.loadTask(taskId)
        if (task != null) {
            if (task.status == DownloadStatus.ENQUEUED || task.status == DownloadStatus.RUNNING) {
                WorkManager.getInstance(context).cancelWorkById(UUID.fromString(taskId))
            }
            if (shouldDeleteContent) {
                var filename = task.filename
                if (filename == null) {
                    filename = task.url.substring(task.url.lastIndexOf("/") + 1, task.url.length)
                }
                val saveFilePath = task.savedDir + File.separator + filename
                val tempFile = File(saveFilePath)
                if (tempFile.exists()) {
                    deleteFileInMediaStore(tempFile)
                    tempFile.delete()
                }
            }
            taskDao.deleteTask(taskId)
            NotificationManagerCompat.from(context).cancel(task.primaryId)
            return true
        } else {
            Log.e("invalid_task_id", "not found task corresponding to given task id", null)
            return false
        }
    }

    private fun deleteFileInMediaStore(file: File) {
        // Set up the projection (we only need the ID)
        val projection = arrayOf(MediaStore.Images.Media._ID)

        // Match on the file path
        val imageSelection = MediaStore.Images.Media.DATA + " = ?"
        val videoSelection = MediaStore.Video.Media.DATA + " = ?"
        val selectionArgs = arrayOf<String>(file.absolutePath)

        // Query for the ID of the media matching the file path
        val imageQueryUri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val videoQueryUri: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val contentResolver: ContentResolver = context.contentResolver

        // search the file in image store first
        val imageCursor: Cursor? = contentResolver.query(imageQueryUri, projection, imageSelection, selectionArgs, null)
        if (imageCursor != null && imageCursor.moveToFirst()) {
            // We found the ID. Deleting the item via the content provider will also remove the file
            val id: Long = imageCursor.getLong(imageCursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
            val deleteUri: Uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            contentResolver.delete(deleteUri, null, null)
        } else {
            // File not found in image store DB, try to search in video store
            val videoCursor: Cursor? = contentResolver.query(imageQueryUri, projection, imageSelection, selectionArgs, null)
            if (videoCursor != null && videoCursor.moveToFirst()) {
                // We found the ID. Deleting the item via the content provider will also remove the file
                val id: Long = videoCursor.getLong(videoCursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                val deleteUri: Uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                contentResolver.delete(deleteUri, null, null)
            } else {
                // can not find the file in media store DB at all
            }
            videoCursor?.close()
        }
        imageCursor?.close()
    }
}