package com.app.location.ui.main

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.app.location.R
import com.app.location.di.component.DaggerActivityComponent
import com.app.location.di.module.ActivityModule
import com.app.location.models.DownloadStatus
import com.app.location.ui.about.AboutFragment
import com.app.location.ui.list.ListFragment
import com.app.location.util.IntentUtils.validatedFileIntent
import com.app.location.workers.DownloadWorker
import com.app.location.workers.TaskDao
import com.app.location.workers.TaskDbHelper
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.collections.HashMap


/**
 * Created by tt on 20/02/2021.
 */
class MainActivity : AppCompatActivity(), MainContract.View {

    @Inject
    lateinit var presenter: MainContract.Presenter

    private lateinit var dbHelper: TaskDbHelper
    private lateinit var taskDao: TaskDao
    private val debugMode = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        injectDependency()

        presenter.attach(this)

        dbHelper = TaskDbHelper.getInstance(MainActivity@ this)!!
        taskDao = TaskDao(dbHelper)


        //https://www.learningcontainer.com/sample-audio-file/
        //val savedDir = Environment.getDataDirectory().absolutePath
        val savedDir = applicationContext.filesDir.absolutePath
        enqueue("https://www.learningcontainer.com/wp-content/uploads/2020/02/Kalimba.mp3", savedDir,
                "Kalimba.mp3")
    }

    override fun onResume() {
        super.onResume()
        test()
    }

    override fun showAboutFragment() {
        if (supportFragmentManager.findFragmentByTag(AboutFragment.TAG) == null) {
            supportFragmentManager.beginTransaction()
                    .addToBackStack(null)
                    .setCustomAnimations(AnimType.FADE.getAnimPair().first, AnimType.FADE.getAnimPair().second)
                    .replace(R.id.frame, AboutFragment().newInstance(), AboutFragment.TAG)
                    .commit()
        } else {
            // Maybe an animation like shake hello text
        }
    }

    override fun showListFragment() {
        supportFragmentManager.beginTransaction()
                .disallowAddToBackStack()
                .setCustomAnimations(AnimType.SLIDE.getAnimPair().first, AnimType.SLIDE.getAnimPair().second)
                .replace(R.id.frame, ListFragment().newInstance(), ListFragment.TAG)
                .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.nav_item_info -> {
                presenter.onDrawerOptionAboutClick()
                return true
            }
            else -> {

            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        val fragmentManager = supportFragmentManager
        val fragment = fragmentManager.findFragmentByTag(AboutFragment.TAG)

        if (fragment == null) {
            super.onBackPressed()
        } else {
            supportFragmentManager.popBackStack()
        }
    }

    private fun injectDependency() {
        val activityComponent = DaggerActivityComponent.builder()
                .activityModule(ActivityModule(this))
                .build()

        activityComponent.inject(this)
    }

    private fun test() {
        //hello.setText("Hello world with kotlin extensions")
    }

    enum class AnimType() {
        SLIDE,
        FADE;

        fun getAnimPair(): Pair<Int, Int> {
            when (this) {
                SLIDE -> return Pair(R.anim.slide_left, R.anim.slide_right)
                FADE -> return Pair(R.anim.fade_in, R.anim.fade_out)
            }

            return Pair(R.anim.slide_left, R.anim.slide_right)
        }
    }

    companion object {
        val TAG = MainActivity::class.java.simpleName
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
                .addTag(TAG)
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

    private fun enqueue(url: String, savedDir: String, filename: String, headers: String = "download_file",
                        showNotification: Boolean = true, openFileFromNotification: Boolean = false,
                        requiresStorageNotLow: Boolean = false): String {
        val request = buildRequest(url, savedDir, filename, headers, showNotification,
                openFileFromNotification, false, requiresStorageNotLow)
        WorkManager.getInstance(MainActivity@ this).enqueue(request!!)
        val taskId = request.id.toString()
        sendUpdateProgress(taskId, DownloadStatus.ENQUEUED, 0)
        taskDao.insertOrUpdateNewTask(taskId, url, DownloadStatus.ENQUEUED, 0,
                filename, savedDir, headers, showNotification, openFileFromNotification)
        return taskId;
    }

    private fun sendUpdateProgress(id: String, status: Int, progress: Int) {
        val args: MutableMap<String, Any> = HashMap()
        args["task_id"] = id
        args["status"] = status
        args["progress"] = progress
        Log.d(TAG, "updateProgress: ${args.toString()}")
    }

    private fun cancel(taskId: String) {
        WorkManager.getInstance(applicationContext).cancelWorkById(UUID.fromString(taskId))
    }

    private fun cancelAll() {
        WorkManager.getInstance(applicationContext).cancelAllWorkByTag(TAG)
    }

    private fun pause(taskId: String) {
        taskDao.updateTask(taskId, true)
        WorkManager.getInstance(applicationContext).cancelWorkById(UUID.fromString(taskId))
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
                    WorkManager.getInstance(applicationContext).enqueue(request)
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
                    WorkManager.getInstance(applicationContext).enqueue(request)
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
                val intent = validatedFileIntent(applicationContext, saveFilePath, task.mimeType)
                if (intent != null) {
                    applicationContext.startActivity(intent)
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
                WorkManager.getInstance(applicationContext).cancelWorkById(UUID.fromString(taskId))
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
            NotificationManagerCompat.from(applicationContext).cancel(task.primaryId)
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
        val contentResolver: ContentResolver = applicationContext.contentResolver

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