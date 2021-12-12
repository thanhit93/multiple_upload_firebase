package com.app.location.di.component

import com.app.location.BaseApp
import com.app.location.di.module.ApplicationModule
import dagger.Component

/**
 * Created by tt on 20/02/2021.
 */
@Component(modules = arrayOf(ApplicationModule::class))
interface ApplicationComponent {

    fun inject(application: BaseApp)

}