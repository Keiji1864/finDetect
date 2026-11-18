package com.ghostteam.findetect.core.di

import com.ghostteam.findetect.data.demo.DemoData
import com.ghostteam.findetect.data.demo.DemoFixtures
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DemoDataModule {
    @Provides
    @Singleton
    fun data(): DemoData = DemoFixtures.create()
}
