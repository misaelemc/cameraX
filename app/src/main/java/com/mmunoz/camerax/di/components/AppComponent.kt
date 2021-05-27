package com.mmunoz.camerax.di.components

import android.app.Application
import com.mmunoz.camera.impl.di.components.CameraComponent
import com.mmunoz.camera.wiring_impl.di.modules.CameraWiringModule
import com.mmunoz.camerax.CameraApp
import com.mmunoz.camerax.di.modules.MainActivityBuilderModule
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        CameraWiringModule::class,
        MainActivityBuilderModule::class,
        AndroidSupportInjectionModule::class
    ]
)
interface AppComponent : AndroidInjector<CameraApp>, CameraComponent.Parent {


    @Component.Builder
    interface Builder {

        @BindsInstance
        fun application(application: Application): Builder

        fun build(): AppComponent
    }
}