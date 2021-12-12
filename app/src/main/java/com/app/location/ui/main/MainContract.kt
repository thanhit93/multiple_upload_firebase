package com.app.location.ui.main
import com.app.location.ui.base.BaseContract

/**
 * Created by tt on 20/02/2021.
 */
class MainContract {

    interface View: BaseContract.View {
        fun showAboutFragment()
        fun showListFragment()
    }

    interface Presenter: BaseContract.Presenter<MainContract.View> {
        fun onDrawerOptionAboutClick()
        fun initDatabaseHelper()
        fun enqueue(url: String, savedDir: String, filename: String, headers: String = "download_file",
                        showNotification: Boolean = true, openFileFromNotification: Boolean = false,
                        requiresStorageNotLow: Boolean = false): String
    }
}