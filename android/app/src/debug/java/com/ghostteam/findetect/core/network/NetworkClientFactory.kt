package com.ghostteam.findetect.core.network

import com.ghostteam.findetect.BuildConfig
import com.ghostteam.findetect.data.demo.DemoData
import com.ghostteam.findetect.data.remote.dto.AccountPageDto
import com.ghostteam.findetect.data.remote.dto.ApiErrorDto
import com.ghostteam.findetect.data.remote.dto.BalanceAdjustmentPageDto
import com.ghostteam.findetect.data.remote.dto.CategoryListDto
import com.ghostteam.findetect.data.remote.dto.TransactionPageDto
import io.ktor.client.HttpClient
import io.ktor.client.engine.config
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import javax.inject.Inject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Fixed API examples only. Interactive application data is stored by the Room repositories. */
class NetworkClientFactory @Inject constructor(
    private val json: Json,
    private val data: DemoData,
) {
    fun create(): HttpClient {
        val basePath = Url(BuildConfig.BASE_URL.trimEnd('/') + "/").encodedPath.trimEnd('/')
        return createNetworkClient(
            engine = MockEngine.config {
                addHandler { request ->
                    val path = request.url.encodedPath.trimEnd('/').removePrefix(basePath)
                    val body = if (request.method == HttpMethod.Get && request.url.parameters.names().isEmpty()) {
                        when (path) {
                            "/categories" -> json.encodeToString(CategoryListDto(data.categories))
                            "/accounts" -> json.encodeToString(AccountPageDto(data.accounts, nextCursor = null))
                            "/transactions" -> json.encodeToString(TransactionPageDto(data.transactions, nextCursor = null))
                            "/balance-adjustments" -> json.encodeToString(BalanceAdjustmentPageDto(data.adjustments, nextCursor = null))
                            else -> null
                        }
                    } else null
                    respond(
                        content = body ?: json.encodeToString(ApiErrorDto("NOT_FOUND", "No static demo response for this request")),
                        status = if (body == null) HttpStatusCode.NotFound else HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            },
            serializer = json,
        )
    }
}
