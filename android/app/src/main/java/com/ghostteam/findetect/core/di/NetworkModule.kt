package com.ghostteam.findetect.core.di

import com.ghostteam.findetect.core.network.NetworkClientFactory
import com.ghostteam.findetect.core.network.attachSession
import com.ghostteam.findetect.data.auth.SessionManager
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import javax.inject.Singleton
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun httpClient(factory: NetworkClientFactory, session: Lazy<SessionManager>): HttpClient = factory.create().attachSession(session)

    @Provides @Singleton @AuthClient
    fun authClient(factory: NetworkClientFactory): HttpClient = factory.create()
}
