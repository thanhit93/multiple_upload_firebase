package com.app.upload_file.ui.list

import com.app.upload_file.ui.base.BaseContract
import com.app.upload_file.models.DetailsViewModel
import com.app.upload_file.models.Post

/**
 * Created by tt on 20/02/2021.
 */
class ListContract {

    interface View: BaseContract.View {
        fun showProgress(show: Boolean)
        fun showErrorMessage(error: String)
        fun loadDataSuccess(list: List<Post>)
        fun loadDataAllSuccess(model: DetailsViewModel)
    }

    interface Presenter: BaseContract.Presenter<View> {
        fun loadData()
        fun loadDataAll()
        fun deleteItem(item: Post)
    }
}