package com.app.location.di.module

import com.app.location.api.ApiServiceInterface
import com.app.location.ui.about.AboutContract
import com.app.location.ui.about.AboutPresenter
import com.app.location.ui.list.ListContract
import com.app.location.ui.list.ListPresenter
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