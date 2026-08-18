package com.desafiomercadobitcoin.data.network

import com.desafiomercadobitcoin.domain.error.toDomainError
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.resources.Resources
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Ponto **único** de construção do cliente HTTP da camada (G-spec `data-network-foundation`).
 * Nenhum outro lugar do projeto monta um `HttpClient`.
 */
object HttpClientFactory {
    fun create(
        engine: HttpClientEngine,
        config: CoinMarketCapConfig,
    ): HttpClient =
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

            // Ponto unico de traducao: acima daqui so circula DomainError tipado.
            HttpResponseValidator {
                handleResponseExceptionWithRequest { cause, _ ->
                    throw cause.toDomainError()
                }
            }
        }

    fun defaultJson(): Json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
}
