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
                    throw cause.toDomainErrorWithStatus()
                }
            }
        }

    /**
     * Cliente de imagens: **sem** credencial, sem `defaultRequest` e sem o validador de
     * dominio.
     *
     * Reusar o cliente da API mandaria `X-CMC_PRO_API_KEY` para o host de imagens a cada
     * logotipo -- vazamento de credencial para quem nao a exige -- e converteria um 404 de
     * imagem em `DomainError` dentro do Coil, onde ninguem o trata (D5).
     */
    fun createImageClient(engine: HttpClientEngine): HttpClient =
        HttpClient(engine) {
            install(HttpTimeout) {
                requestTimeoutMillis = CoinMarketCapConfig.TIMEOUT_MILLIS
                connectTimeoutMillis = CoinMarketCapConfig.TIMEOUT_MILLIS
                socketTimeoutMillis = CoinMarketCapConfig.TIMEOUT_MILLIS
            }
        }

    /**
     * Status HTTP e vocabulario de transporte, entao a leitura dele mora aqui e nao em
     * `:domain` -- leva-la para la exigiria ou uma dependencia de Ktor (fere o G1) ou mais
     * checagem por nome de tipo (D4). `ThrowableToDomainError` segue intacto.
     *
     * `400` cai em `Network` junto do resto do `4xx`: e semanticamente impreciso, mas nao
     * ha cenario de produto que o distinga hoje.
     */
    private fun Throwable.toDomainErrorWithStatus(): Throwable =
        when {
            this !is ResponseException -> toDomainError()
            response.status == HttpStatusCode.NotFound -> DomainError.NotFound
            else -> DomainError.Network
        }

    fun defaultJson(): Json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
}
