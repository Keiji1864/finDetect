package com.ghostteam.findetect.core.di

import android.content.Context
import androidx.room.Room
import com.ghostteam.findetect.data.local.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun db(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "findetective.db").build()

    @Provides
    fun accountDao(db: AppDatabase) = db.accountDao()
    @Provides fun transactionDao(db: AppDatabase) = db.transactionDao()
    @Provides fun categoryDao(db: AppDatabase) = db.categoryDao()
    @Provides fun adjustmentDao(db: AppDatabase) = db.adjustmentDao()
}