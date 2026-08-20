package com.desafiomercadobitcoin.data.network

import com.desafiomercadobitcoin.domain.error.DomainError
import com.desafiomercadobitcoin.domain.error.toDomainError
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.resources.Resources
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

object HttpClientFactory {
    fun create(
        engine: HttpClientEngine,
        config: CoinMarketCapConfig,
    ): HttpClient =
        runBlocking(Dispatchers.IO) {
            HttpClient(engine) {
                expectSuccess = true

                install(Resources)

                install(ContentNegotiation) {
                    json(defaultJson(), contentType = ContentType.Application.Json)
                }

                install(HttpTimeout) {
                    requestTimeoutMillis = CoinMarketCapConfig.TIMEOUT_MILLIS
                    connectTimeoutMillis = CoinMarketCapConfig.TIMEOUT_MILLIS
                    socketTimeoutMillis = CoinMarketCapConfig.TIMEOUT_MILLIS
                }

                if (config.isDebug) {
                    install(Logging) {
                        level = LogLevel.HEADERS
                    }
                }

                defaultRequest {
                    url(CoinMarketCapConfig.BASE_URL)
                    headers.append(CoinMarketCapConfig.API_KEY_HEADER, config.apiKey)
                }

                HttpResponseValidator {
                    handleResponseExceptionWithRequest { cause, _ ->
                        throw cause.toDomainErrorWithStatus()
                    }
                }
            }
        }

    fun createImageClient(engine: HttpClientEngine): HttpClient =
        runBlocking(Dispatchers.IO) {
            HttpClient(engine) {
                install(HttpTimeout) {
                    requestTimeoutMillis = CoinMarketCapConfig.TIMEOUT_MILLIS
                    connectTimeoutMillis = CoinMarketCapConfig.TIMEOUT_MILLIS
                    socketTimeoutMillis = CoinMarketCapConfig.TIMEOUT_MILLIS
                }
            }
        }

    private fun Throwable.toDomainErrorWithStatus(): Throwable =
        when {
            this !is ResponseException -> toDomainError()
            response.status == HttpStatusCode.NotFound -> DomainError.NotFound()
            else -> DomainError.Network()
        }

    fun defaultJson(): Json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
}
