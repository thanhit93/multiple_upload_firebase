package com.app.upload_file.di.component

import com.app.upload_file.di.module.FragmentModule
import com.app.upload_file.ui.about.AboutFragment
import com.app.upload_file.ui.list.ListFragment
import dagger.Component

/**
 * Created by tt on 20/02/2021.
 */
@Component(modules = arrayOf(FragmentModule::class))
interface FragmentComponent {

    fun inject(aboutFragment: AboutFragment)

    fun inject(listFragment: ListFragment)

}