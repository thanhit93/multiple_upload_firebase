package com.app.location.di.component

import com.app.location.di.module.ActivityModule
import com.app.location.ui.main.MainActivity
import dagger.Component

/**
 * Created by tt on 20/02/2021.
 */
@Component(modules = arrayOf(ActivityModule::class))
interface ActivityComponent {

    fun inject(mainActivity: MainActivity)

}