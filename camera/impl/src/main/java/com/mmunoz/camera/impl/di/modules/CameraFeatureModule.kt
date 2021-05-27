package com.mmunoz.camera.impl.di.modules

import com.mmunoz.camera.impl.ui.fragments.CameraFragment
import com.mmunoz.core.di.scopes.FragmentScope
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class CameraFeatureModule {

    @FragmentScope
    @ContributesAndroidInjector
    abstract fun bindFragment(): CameraFragment
}