package com.ghostteam.findetect.core.network

import com.ghostteam.findetect.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.config
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** A lost registration response must not cause a transparent second POST at the transport layer. */
internal fun createRemoteNetworkClient(serializer: Json): HttpClient = createNetworkClient(
    engine = OkHttp.config { config { retryOnConnectionFailure(false) } },
    serializer = serializer,
)

/** Both transports use the same HTTP/JSON pipeline. The client owns its engine's lifecycle. */
internal fun <T : HttpClientEngineConfig> createNetworkClient(
    engine: HttpClientEngineFactory<T>,
    serializer: Json,
): HttpClient = HttpClient(engine) {
    expectSuccess = false
    followRedirects = false
    install(ContentNegotiation) { json(serializer) }
    install(HttpTimeout) { requestTimeoutMillis = 15_000 }
    defaultRequest {
        url(BuildConfig.BASE_URL.trimEnd('/') + "/")
    }
}
