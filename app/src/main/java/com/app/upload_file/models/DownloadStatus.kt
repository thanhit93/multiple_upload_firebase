package com.app.upload_file.models

object DownloadStatus {
    var UNDEFINED = 0
    var ENQUEUED = 1
    var RUNNING = 2
    var COMPLETE = 3
    var FAILED = 4
    var CANCELED = 5
    var PAUSED = 6
}