package com.mmunoz.camerax

import com.mmunoz.camerax.di.components.AppComponent
import com.mmunoz.camerax.di.components.DaggerAppComponent
import com.mmunoz.core.di.holder.ComponentHolder
import dagger.android.AndroidInjector
import dagger.android.support.DaggerApplication

class CameraApp: DaggerApplication() {

    private var component: AppComponent? = null

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        ComponentHolder.components.clear()
        component = DaggerAppComponent.builder()
            .application(this)
            .build()

        ComponentHolder.components.add(component!!)
        return component!!
    }
}