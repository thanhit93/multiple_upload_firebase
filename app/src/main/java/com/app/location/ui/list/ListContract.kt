package com.app.location.ui.list

import com.app.location.ui.base.BaseContract
import com.app.location.models.DetailsViewModel
import com.app.location.models.Post

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