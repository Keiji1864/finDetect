package com.ghostteam.findetect.core.network

import com.ghostteam.findetect.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Both transports use the same HTTP/JSON pipeline. The client owns its engine's lifecycle. */
internal fun <T : HttpClientEngineConfig> createNetworkClient(
    engine: HttpClientEngineFactory<T>,
    serializer: Json,
): HttpClient = HttpClient(engine) {
    expectSuccess = true
    install(ContentNegotiation) { json(serializer) }
    install(HttpTimeout) { requestTimeoutMillis = 15_000 }
    defaultRequest {
        url(BuildConfig.BASE_URL.trimEnd('/') + "/")
    }
}
