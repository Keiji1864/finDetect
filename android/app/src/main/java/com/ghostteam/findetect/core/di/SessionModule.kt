package com.ghostteam.findetect.core.di

import com.ghostteam.findetect.data.auth.SessionManager
import com.ghostteam.findetect.domain.repository.AuthRepository
import com.ghostteam.findetect.domain.repository.ProfileRepository
import com.ghostteam.findetect.domain.repository.PasswordRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SessionModule {
    @Binds @Singleton abstract fun auth(manager: SessionManager): AuthRepository
    @Binds @Singleton abstract fun profile(manager: SessionManager): ProfileRepository
    @Binds @Singleton abstract fun passwords(manager: SessionManager): PasswordRepository
}
