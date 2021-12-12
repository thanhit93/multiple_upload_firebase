package com.app.location

import android.app.Application
import com.app.location.di.component.ApplicationComponent
import com.app.location.di.component.DaggerApplicationComponent
import com.app.location.di.module.ApplicationModule

/**
 * Created by tt on 20/02/2021.
 */
class BaseApp: Application() {

    lateinit var component: ApplicationComponent

    override fun onCreate() {
        super.onCreate()

        instance = this
        setup()

        if (BuildConfig.DEBUG) {
            // Maybe TimberPlant etc.
        }
    }

    fun setup() {
        component = DaggerApplicationComponent.builder()
                .applicationModule(ApplicationModule(this)).build()
        component.inject(this)
    }

    fun getApplicationComponent(): ApplicationComponent {
        return component
    }

    companion object {
        lateinit var instance: BaseApp private set
    }
}