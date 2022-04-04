package com.app.upload_file.di.component

import com.app.upload_file.BaseApp
import com.app.upload_file.di.module.ApplicationModule
import dagger.Component

/**
 * Created by tt on 20/02/2021.
 */
@Component(modules = arrayOf(ApplicationModule::class))
interface ApplicationComponent {

    fun inject(application: BaseApp)

}