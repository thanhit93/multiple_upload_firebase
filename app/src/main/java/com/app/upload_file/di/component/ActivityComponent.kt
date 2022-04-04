package com.app.upload_file.di.component

import com.app.upload_file.di.module.ActivityModule
import com.app.upload_file.ui.main.MainActivity
import dagger.Component

/**
 * Created by tt on 20/02/2021.
 */
@Component(modules = arrayOf(ActivityModule::class))
interface ActivityComponent {

    fun inject(mainActivity: MainActivity)

}