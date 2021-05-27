package com.mmunoz.camera.impl.di.components

import androidx.fragment.app.Fragment
import com.mmunoz.camera.impl.di.modules.CameraFeatureModule
import com.mmunoz.core.di.holder.ComponentHolder
import dagger.Component
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Scope

@CameraScope
@Component(
    dependencies = [CameraComponent.Parent::class],
    modules = [
        CameraFeatureModule::class,
        AndroidSupportInjectionModule::class
    ]
)
interface CameraComponent {

    fun androidInjector(): DispatchingAndroidInjector<Any>

    interface Parent {

    }

    @Component.Factory
    interface Factory {

        fun create(parent: Parent): CameraComponent
    }
}

@Scope
@Retention
annotation class CameraScope

fun Fragment.inject(clazz: Any) {
    DaggerCameraComponent.factory()
        .create(ComponentHolder.component())
        .androidInjector().inject(clazz)
}
