package com.app.upload_file.ui.about

import com.app.upload_file.ui.base.BaseContract

/**
 * Created by tt on 20/02/2021.
 */
class AboutContract {

    interface View: BaseContract.View {
        fun showProgress(show: Boolean)
        fun loadMessageSuccess(message: String)
        // fun loadMessageError() // When it's a real request, this function should be implemented, too
    }

    interface Presenter: BaseContract.Presenter<View> {
        fun loadMessage() // Let's assume that this will be a retrofit request
    }
}