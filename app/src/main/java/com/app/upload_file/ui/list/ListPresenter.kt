package com.app.upload_file.ui.list

import android.content.Context
import com.app.upload_file.api.ApiServiceInterface
import com.app.upload_file.models.Album
import com.app.upload_file.models.DetailsViewModel
import com.app.upload_file.models.Post
import com.app.upload_file.models.User
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Function3
import io.reactivex.schedulers.Schedulers

/**
 * Created by tt on 20/02/2021.
 */
class ListPresenter: ListContract.Presenter {

    private val subscriptions = CompositeDisposable()
    private val api: ApiServiceInterface = ApiServiceInterface.create()
    private lateinit var view: ListContract.View

    override fun subscribe() {

    }

    override fun unsubscribe() {
        subscriptions.clear()
    }

    override fun attach(view: ListContract.View) {
        this.view = view
    }

    override fun attachContext(view: ListContract.View, context: Context) {
        this.view = view
    }

    override fun loadData() {
        var subscribe = api.getPostList().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ list: List<Post>? ->
                    view.showProgress(false)
                    view.loadDataSuccess(list!!.take(10))
                }, { error ->
                    view.showProgress(false)
                    view.showErrorMessage(error.localizedMessage)
                })

        subscriptions.add(subscribe)
    }

    override fun loadDataAll() {
        var subscribe = Observable.zip(api.getPostList(), api.getUserList(), api.getAlbumList(),
                Function3<List<Post>, List<User>, List<Album>, DetailsViewModel> {
                    posts, users, albums ->
                    createDetailsViewModel(posts, users, albums)
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ model: DetailsViewModel? ->
                    view.showProgress(false)
                    view.loadDataAllSuccess(model!!)
                },{ error ->
                    view.showProgress(false)
                    view.showErrorMessage(error.localizedMessage)
                })

        subscriptions.add(subscribe)
    }

    private fun createDetailsViewModel(posts: List<Post>, users: List<User>, albums: List<Album>): DetailsViewModel {
        val postList = posts.take(30)
        val userList = users.take(30)
        val albumList = albums.take(30)
        return DetailsViewModel(postList, userList, albumList)
    }

    override fun deleteItem(item: Post) {
        //api.deleteUser(item.id)
    }
}