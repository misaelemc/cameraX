package com.mmunoz.camera.wiring_impl.di.modules

import com.mmunoz.camera.api.ui.loader.CameraFragmentLoader
import com.mmunoz.camera.impl.ui.impls.CameraFragmentLoaderImpl
import dagger.Binds
import dagger.Module

@Module
abstract class CameraWiringModule {

    @Binds
    abstract fun bindLoader(impl: CameraFragmentLoaderImpl): CameraFragmentLoader
}