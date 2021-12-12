package com.app.location.ui.base

/**
 * Created by tt on 20/02/2021.
 */
class BaseContract {

    interface Presenter<in T> {
        fun subscribe()
        fun unsubscribe()
        fun attach(view: T)
    }

    interface View {

    }
}