package com.app.upload_file.di.module

import android.app.Activity
import com.app.upload_file.ui.main.MainContract
import com.app.upload_file.ui.main.MainPresenter
import dagger.Module
import dagger.Provides

/**
 * Created by ogulcan on 07/02/2018.
 */
@Module
class ActivityModule(private var activity: Activity) {

    @Provides
    fun provideActivity(): Activity {
        return activity
    }

    @Provides
    fun providePresenter(): MainContract.Presenter {
        return MainPresenter()
    }

}