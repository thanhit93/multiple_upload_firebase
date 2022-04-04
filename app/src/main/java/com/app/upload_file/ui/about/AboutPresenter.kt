package com.app.upload_file.ui.about

import android.annotation.SuppressLint
import android.content.Context
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.Observable
import java.util.concurrent.TimeUnit


/**
 * Created by tt on 20/02/2021.
 */
class AboutPresenter: AboutContract.Presenter {

    //private val subscriptions = CompositeDisposable()
    private lateinit var view: AboutContract.View

    override fun subscribe() {

    }

    override fun unsubscribe() {

    }

    override fun attach(view: AboutContract.View) {
        this.view = view
    }

    override fun attachContext(view: AboutContract.View, context: Context) {
        this.view = view
    }

    @SuppressLint("CheckResult")
    override fun loadMessage() {
        // Wait for a moment
        Observable.just(true).delay(1000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    view.showProgress(false)
                    view.loadMessageSuccess("Success")
                }
    }
}