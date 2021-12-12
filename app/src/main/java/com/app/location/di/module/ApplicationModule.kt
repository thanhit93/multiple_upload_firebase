package com.app.location.di.module

import android.app.Application
import com.app.location.BaseApp
import com.app.location.di.scope.PerApplication
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by ogulcan on 07/02/2018.
 */
@Module
class ApplicationModule(private val baseApp: BaseApp) {

    @Provides
    @Singleton
    @PerApplication
    fun provideApplication(): Application {
        return baseApp
    }
}