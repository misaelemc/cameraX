package com.mmunoz.camerax.di.modules

import com.mmunoz.camerax.MainActivity
import com.mmunoz.core.di.scopes.ActivityScope
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class MainActivityBuilderModule {

    @ActivityScope
    @ContributesAndroidInjector
    abstract fun bindMainActivity(): MainActivity
}