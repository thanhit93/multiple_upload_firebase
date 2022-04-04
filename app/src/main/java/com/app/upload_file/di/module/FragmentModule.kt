package com.app.upload_file.di.module

import com.app.upload_file.api.ApiServiceInterface
import com.app.upload_file.ui.about.AboutContract
import com.app.upload_file.ui.about.AboutPresenter
import com.app.upload_file.ui.list.ListContract
import com.app.upload_file.ui.list.ListPresenter
import dagger.Module
import dagger.Provides

/**
 * Created by ogulcan on 07/02/2018.
 */
@Module
class FragmentModule {

    @Provides
    fun provideAboutPresenter(): AboutContract.Presenter {
        return AboutPresenter()
    }

    @Provides
    fun provideListPresenter(): ListContract.Presenter {
        return ListPresenter()
    }

    @Provides
    fun provideApiService(): ApiServiceInterface {
        return ApiServiceInterface.create()
    }
}