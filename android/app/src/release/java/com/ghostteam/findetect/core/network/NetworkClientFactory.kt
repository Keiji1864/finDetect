package com.ghostteam.findetect.core.network

import io.ktor.client.HttpClient
import javax.inject.Inject
import kotlinx.serialization.json.Json

/** Release has no dependency on the debug mock or its scenario controls. */
class NetworkClientFactory @Inject constructor(private val json: Json) {
    fun create(): HttpClient = createRemoteNetworkClient(json)
}
