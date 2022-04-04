package com.app.upload_file.ui.base

import android.content.Context

/**
 * Created by tt on 20/02/2021.
 */
class BaseContract {

    interface Presenter<in T> {
        fun subscribe()
        fun unsubscribe()
        fun attach(view: T)
        fun attachContext(view: T, context: Context)
    }

    interface View {

    }
}