package com.app.location.di.component

import com.app.location.di.module.FragmentModule
import com.app.location.ui.about.AboutFragment
import com.app.location.ui.list.ListFragment
import dagger.Component

/**
 * Created by tt on 20/02/2021.
 */
@Component(modules = arrayOf(FragmentModule::class))
interface FragmentComponent {

    fun inject(aboutFragment: AboutFragment)

    fun inject(listFragment: ListFragment)

}