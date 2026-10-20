package com.ghostteam.findetect.core.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {
    // @Binds @IntoSet abstract fun accounts(impl: OfflineFirstAccountRepository): Syncable
    // @Binds @IntoSet abstract fun transactions(impl: OfflineFirstTransactionRepository): Syncable
}