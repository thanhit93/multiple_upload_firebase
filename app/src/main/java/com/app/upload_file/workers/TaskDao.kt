package com.app.upload_file.workers

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.provider.BaseColumns
import com.app.upload_file.models.DownloadTask
import java.util.*


class TaskDao(private val dbHelper: TaskDbHelper) {
    private val projection = arrayOf(
            BaseColumns._ID,
            TaskContract.TaskEntry.COLUMN_NAME_TASK_ID,
            TaskContract.TaskEntry.COLUMN_NAME_PROGRESS,
            TaskContract.TaskEntry.COLUMN_NAME_STATUS,
            TaskContract.TaskEntry.COLUMN_NAME_URL,
            TaskContract.TaskEntry.COLUMN_NAME_FILE_NAME,
            TaskContract.TaskEntry.COLUMN_NAME_SAVED_DIR,
            TaskContract.TaskEntry.COLUMN_NAME_HEADERS,
            TaskContract.TaskEntry.COLUMN_NAME_MIME_TYPE,
            TaskContract.TaskEntry.COLUMN_NAME_RESUMABLE,
            TaskContract.TaskEntry.COLUMN_NAME_OPEN_FILE_FROM_NOTIFICATION,
            TaskContract.TaskEntry.COLUMN_NAME_SHOW_NOTIFICATION,
            TaskContract.TaskEntry.COLUMN_NAME_TIME_CREATED
    )

    fun insertOrUpdateNewTask(taskId: String?, url: String?, status: Int, progress: Int, fileName: String?,
                              savedDir: String?, headers: String?, showNotification: Boolean,
                              openFileFromNotification: Boolean) {
        val db = dbHelper.writableDatabase
        val values = ContentValues()
        values.put(TaskContract.TaskEntry.COLUMN_NAME_TASK_ID, taskId)
        values.put(TaskContract.TaskEntry.COLUMN_NAME_URL, url)
        values.put(TaskContract.TaskEntry.COLUMN_NAME_STATUS, status)
        values.put(TaskContract.TaskEntry.COLUMN_NAME_PROGRESS, progress)
        values.put(TaskContract.TaskEntry.COLUMN_NAME_FILE_NAME, fileName)
        values.put(TaskContract.TaskEntry.COLUMN_NAME_SAVED_DIR, savedDir)
        values.put(TaskContract.TaskEntry.COLUMN_NAME_HEADERS, headers)
        values.put(TaskContract.TaskEntry.COLUMN_NAME_MIME_TYPE, "unknown")
        values.put(TaskContract.TaskEntry.COLUMN_NAME_SHOW_NOTIFICATION, if (showNotification) 1 else 0)
        values.put(TaskContract.TaskEntry.COLUMN_NAME_OPEN_FILE_FROM_NOTIFICATION, if (openFileFromNotification) 1 else 0)
        values.put(TaskContract.TaskEntry.COLUMN_NAME_RESUMABLE, 0)
        values.put(TaskContract.TaskEntry.COLUMN_NAME_TIME_CREATED, System.currentTimeMillis())
        db.beginTransaction()
        try {
            db.insertWithOnConflict(TaskContract.TaskEntry.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
    }

    fun loadAllTasks(): List<DownloadTask> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
                TaskContract.TaskEntry.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                null
        )
        val result: MutableList<DownloadTask> = ArrayList()
        while (cursor.moveToNext()) {
            result.add(parseCursor(cursor))
        }
        cursor.close()
        return result
    }

    fun loadTasksWithRawQuery(query: String?): List<DownloadTask> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(query, null)
        val result: MutableList<DownloadTask> = ArrayList()
        while (cursor.moveToNext()) {
            result.add(parseCursor(cursor))
        }
        cursor.close()
        return result
    }

    fun loadTask(taskId: String): DownloadTask? {
        val db = dbHelper.readableDatabase
        val whereClause = TaskContract.TaskEntry.COLUMN_NAME_TASK_ID + " = ?"
        val whereArgs = arrayOf(taskId)
        val cursor = db.query(
                TaskContract.TaskEntry.TABLE_NAME,
                projection,
                whereClause,
                whereArgs,
                null,
                null,
                BaseColumns._ID + " DESC",
                "1"
        )
        var result: DownloadTask? = null
        while (cursor.moveToNext()) {
            result = parseCursor(cursor)
        }
        cursor.close()
        return result
    }

    fun updateTask(taskId: String, status: Int, progress: Int) {
        val db = dbHelper.writableDatabase
        val values = ContentValues()
        values.put(TaskContract.TaskEntry.COLUMN_NAME_STATUS, status)
        values.put(TaskContract.TaskEntry.COLUMN_NAME_PROGRESS, progress)
        db.beginTransaction()
        try {
            db.update(TaskContract.TaskEntry.TABLE_NAME, values, TaskContract.TaskEntry.COLUMN_NAME_TASK_ID + " = ?", arrayOf(taskId))
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
    }

    fun updateTask(currentTaskId: String, newTaskId: String?, status: Int, progress: Int, resumable: Boolean) {
        val db = dbHelper.writableDatabase
        val values = ContentValues()
        values.put(TaskContract.TaskEntry.COLUMN_NAME_TASK_ID, newTaskId)
        values.put(TaskContract.TaskEntry.COLUMN_NAME_STATUS, status)
        values.put(TaskContract.TaskEntry.COLUMN_NAME_PROGRESS, progress)
        values.put(TaskContract.TaskEntry.COLUMN_NAME_RESUMABLE, if (resumable) 1 else 0)
        values.put(TaskContract.TaskEntry.COLUMN_NAME_TIME_CREATED, System.currentTimeMillis())
        db.beginTransaction()
        try {
            db.update(TaskContract.TaskEntry.TABLE_NAME, values, TaskContract.TaskEntry.COLUMN_NAME_TASK_ID + " = ?", arrayOf(currentTaskId))
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
    }

    fun updateTask(taskId: String, resumable: Boolean) {
        val db = dbHelper.writableDatabase
        val values = ContentValues()
        values.put(TaskContract.TaskEntry.COLUMN_NAME_RESUMABLE, if (resumable) 1 else 0)
        db.beginTransaction()
        try {
            db.update(TaskContract.TaskEntry.TABLE_NAME, values, TaskContract.TaskEntry.COLUMN_NAME_TASK_ID + " = ?", arrayOf(taskId))
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
    }

    fun updateTask(taskId: String, filename: String?, mimeType: String?) {
        val db = dbHelper.writableDatabase
        val values = ContentValues()
        values.put(TaskContract.TaskEntry.COLUMN_NAME_FILE_NAME, filename)
        values.put(TaskContract.TaskEntry.COLUMN_NAME_MIME_TYPE, mimeType)
        db.beginTransaction()
        try {
            db.update(TaskContract.TaskEntry.TABLE_NAME, values, TaskContract.TaskEntry.COLUMN_NAME_TASK_ID + " = ?", arrayOf(taskId))
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
    }

    fun deleteTask(taskId: String) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            val whereClause = TaskContract.TaskEntry.COLUMN_NAME_TASK_ID + " = ?"
            val whereArgs = arrayOf(taskId)
            db.delete(TaskContract.TaskEntry.TABLE_NAME, whereClause, whereArgs)
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
    }

    private fun parseCursor(cursor: Cursor): DownloadTask {
        val primaryId = cursor.getInt(cursor.getColumnIndexOrThrow(BaseColumns._ID))
        val taskId = cursor.getString(cursor.getColumnIndexOrThrow(TaskContract.TaskEntry.COLUMN_NAME_TASK_ID))
        val status = cursor.getInt(cursor.getColumnIndexOrThrow(TaskContract.TaskEntry.COLUMN_NAME_STATUS))
        val progress = cursor.getInt(cursor.getColumnIndexOrThrow(TaskContract.TaskEntry.COLUMN_NAME_PROGRESS))
        val url = cursor.getString(cursor.getColumnIndexOrThrow(TaskContract.TaskEntry.COLUMN_NAME_URL))
        val filename = cursor.getString(cursor.getColumnIndexOrThrow(TaskContract.TaskEntry.COLUMN_NAME_FILE_NAME))
        val savedDir = cursor.getString(cursor.getColumnIndexOrThrow(TaskContract.TaskEntry.COLUMN_NAME_SAVED_DIR))
        val headers = cursor.getString(cursor.getColumnIndexOrThrow(TaskContract.TaskEntry.COLUMN_NAME_HEADERS))
        val mimeType = cursor.getString(cursor.getColumnIndexOrThrow(TaskContract.TaskEntry.COLUMN_NAME_MIME_TYPE))
        val resumable = cursor.getShort(cursor.getColumnIndexOrThrow(TaskContract.TaskEntry.COLUMN_NAME_RESUMABLE)).toInt()
        val showNotification = cursor.getShort(cursor.getColumnIndexOrThrow(TaskContract.TaskEntry.COLUMN_NAME_SHOW_NOTIFICATION)).toInt()
        val clickToOpenDownloadedFile = cursor.getShort(cursor.getColumnIndexOrThrow(TaskContract.TaskEntry.COLUMN_NAME_OPEN_FILE_FROM_NOTIFICATION)).toInt()
        val timeCreated = cursor.getLong(cursor.getColumnIndexOrThrow(TaskContract.TaskEntry.COLUMN_NAME_TIME_CREATED))
        return DownloadTask(primaryId, taskId, status, progress, url, filename, savedDir, headers,
                mimeType, resumable == 1, showNotification == 1, clickToOpenDownloadedFile == 1, timeCreated)
    }

}