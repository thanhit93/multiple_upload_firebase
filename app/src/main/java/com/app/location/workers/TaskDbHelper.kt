package com.app.location.workers

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns._ID


class TaskDbHelper private constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(SQL_DELETE_ENTRIES)
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    companion object {
        const val DATABASE_VERSION = 2
        const val DATABASE_NAME = "download_tasks.db"
        private var instance: TaskDbHelper? = null
        private const val SQL_CREATE_ENTRIES = "CREATE TABLE " + TaskContract.TaskEntry.TABLE_NAME + " (" +
                _ID + " INTEGER PRIMARY KEY," +
                TaskContract.TaskEntry.COLUMN_NAME_TASK_ID + " VARCHAR(256), " +
                TaskContract.TaskEntry.COLUMN_NAME_URL + " TEXT, " +
                TaskContract.TaskEntry.COLUMN_NAME_STATUS + " INTEGER DEFAULT 0, " +
                TaskContract.TaskEntry.COLUMN_NAME_PROGRESS + " INTEGER DEFAULT 0, " +
                TaskContract.TaskEntry.COLUMN_NAME_FILE_NAME + " TEXT, " +
                TaskContract.TaskEntry.COLUMN_NAME_SAVED_DIR + " TEXT, " +
                TaskContract.TaskEntry.COLUMN_NAME_HEADERS + " TEXT, " +
                TaskContract.TaskEntry.COLUMN_NAME_MIME_TYPE + " VARCHAR(128), " +
                TaskContract.TaskEntry.COLUMN_NAME_RESUMABLE + " TINYINT DEFAULT 0, " +
                TaskContract.TaskEntry.COLUMN_NAME_SHOW_NOTIFICATION + " TINYINT DEFAULT 0, " +
                TaskContract.TaskEntry.COLUMN_NAME_OPEN_FILE_FROM_NOTIFICATION + " TINYINT DEFAULT 0, " +
                TaskContract.TaskEntry.COLUMN_NAME_TIME_CREATED + " INTEGER DEFAULT 0" + ")"
        private const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + TaskContract.TaskEntry.TABLE_NAME

        fun getInstance(ctx: Context): TaskDbHelper? {

            // Use the application context, which will ensure that you
            // don't accidentally leak an Activity's context.
            // See this article for more information: http://bit.ly/6LRzfx
            if (instance == null) {
                instance = TaskDbHelper(ctx.applicationContext)
            }
            return instance
        }
    }
}
