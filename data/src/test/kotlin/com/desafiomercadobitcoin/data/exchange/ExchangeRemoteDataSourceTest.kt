package com.desafiomercadobitcoin.data.exchange

import com.desafiomercadobitcoin.data.network.CoinMarketCapConfig
import com.desafiomercadobitcoin.data.network.HttpClientFactory
import com.desafiomercadobitcoin.domain.error.DomainError
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith

@RunWith(Enclosed::class)
class ExchangeRemoteDataSourceTest {
    abstract class TestSetup {
        protected val requests = mutableListOf<HttpRequestData>()
        protected val everyKnownId = listOf(BINANCE_ID, BITFINEX_ID, MERCADO_BITCOIN_ID)

        protected fun fixture(name: String): String =
            checkNotNull(javaClass.classLoader?.getResourceAsStream(name)) {
                "fixtura ausente: $name"
            }.bufferedReader().use { it.readText() }

        protected fun client(payload: String): HttpClient =
            client { request ->
                requests += request
                respond(
                    content = payload,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            }

        protected fun client(
            handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
        ): HttpClient =
            HttpClientFactory.create(
                MockEngine { request -> handler(request) },
                CoinMarketCapConfig(apiKey = "key"),
            )

        protected fun dataSource(payload: String): ExchangeRemoteDataSource = ExchangeRemoteDataSource(client(payload))

        protected fun dataSource(
            handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
        ): ExchangeRemoteDataSource = ExchangeRemoteDataSource(client(handler))
    }

    class HappyPath : TestSetup() {
        @Test
        fun `given the index response when loading the map then every active entry is deserialized`() =
            runTest {
                val source = dataSource(fixture("exchange_map.json"))

                val entries = source.loadActiveIndex()

                assertEquals(listOf(BINANCE_ID, BITFINEX_ID, MERCADO_BITCOIN_ID, UNLISTED_ID), entries.map { it.id })
                assertEquals("Mercado Bitcoin", entries[2].name)
            }

        @Test
        fun `given the content response when loading info then it is keyed by the string id`() =
            runTest {
                val source = dataSource(fixture("exchange_info.json"))

                val content = source.loadInfo(everyKnownId)

                assertEquals(everyKnownId.map(Int::toString).toSet(), content.keys)
                assertEquals("Binance", content.getValue("$BINANCE_ID").name)
            }

        @Test
        fun `given content without an optional field when deserializing then it comes back absent`() =
            runTest {
                val source = dataSource(fixture("exchange_info.json"))

                val content = source.loadInfo(listOf(BITFINEX_ID, MERCADO_BITCOIN_ID))

                assertNull(content.getValue("$BITFINEX_ID").spotVolumeUsd)
                assertNull(content.getValue("$MERCADO_BITCOIN_ID").dateLaunched)
            }

        @Test
        fun `given content with description urls and fees when deserializing then they are captured`() =
            runTest {
                val source = dataSource(fixture("exchange_info.json"))

                val content = source.loadInfo(listOf(BINANCE_ID))
                val binance = content.getValue("$BINANCE_ID")

                assertEquals("Binance e uma exchange de criptomoedas.", binance.description)
                assertEquals(listOf("https://www.binance.com/"), binance.urls?.website)
                assertEquals(BINANCE_MAKER_FEE, binance.makerFee)
                assertEquals(BINANCE_TAKER_FEE, binance.takerFee)
            }

        @Test
        fun `given content without description urls and fees when deserializing then they come back absent`() =
            runTest {
                val source = dataSource(fixture("exchange_info.json"))

                val content = source.loadInfo(listOf(MERCADO_BITCOIN_ID))
                val mercadoBitcoin = content.getValue("$MERCADO_BITCOIN_ID")

                assertNull(mercadoBitcoin.description)
                assertNull(mercadoBitcoin.urls)
                assertNull(mercadoBitcoin.makerFee)
                assertNull(mercadoBitcoin.takerFee)
            }

        @Test
        fun `given a response with an unknown field when deserializing then the field is ignored`() =
            runTest {
                val source =
                    dataSource(
                        """{"data":[{"id":1,"name":"Any","futureField":true}],"status":{"error_code":0}}""",
                    )

                val entries = source.loadActiveIndex()

                assertEquals(listOf(1), entries.map { it.id })
            }

        @Test
        fun `given a set of ids when loading info then a single request carries all of them`() =
            runTest {
                val source = dataSource(fixture("exchange_info.json"))

                source.loadInfo(everyKnownId)

                assertEquals(1, requests.size)
                assertEquals(everyKnownId.joinToString(separator = ","), requests.single().url.parameters["id"])
            }

        @Test
        fun `given the assets response when loading assets then every currency is deserialized`() =
            runTest {
                val source = dataSource(fixture("exchange_assets.json"))

                val assets = source.loadAssets(BINANCE_ID)

                assertEquals(listOf("Bitcoin", "Ethereum", "Tether"), assets.map { it.currency.name })
                assertEquals("$BINANCE_ID", requests.single().url.parameters["id"])
            }

        @Test
        fun `given a currency without price when loading assets then it comes back absent`() =
            runTest {
                val source = dataSource(fixture("exchange_assets.json"))

                val assets = source.loadAssets(BINANCE_ID)

                assertNull(assets.single { it.currency.name == "Ethereum" }.currency.priceUsd)
            }

        @Test
        fun `given an unknown field in an asset when deserializing then it is ignored`() =
            runTest {
                val source =
                    dataSource(
                        """{"data":[{"currency":{"name":"Bitcoin","price_usd":1.0,"futureField":true}}]}""",
                    )

                val assets = source.loadAssets(BINANCE_ID)

                assertEquals("Bitcoin", assets.single().currency.name)
            }
    }

    class ErrorPath : TestSetup() {
        @Test
        fun `given a response missing a required field when deserializing then fails with Serialization`() =
            runTest {
                val source = dataSource("""{"data":[{"name":"Sem id"}],"status":{"error_code":0}}""")

                val error = runCatching { source.loadActiveIndex() }.exceptionOrNull()

                assertEquals(DomainError.Serialization(), error)
            }

        @Test
        fun `given an empty set of ids when loading info then no request reaches the network`() =
            runTest {
                val source = dataSource(fixture("exchange_info.json"))

                val content = source.loadInfo(emptyList())

                assertTrue(content.isEmpty())
                assertTrue(requests.isEmpty())
            }

        @Test
        fun `given more ids than the provider accepts when loading info then it is refused before the network`() =
            runTest {
                val source = dataSource(fixture("exchange_info.json"))
                val tooMany = (1..ExchangeRemoteDataSource.MAX_IDS_PER_REQUEST + 1).toList()

                val error = runCatching { source.loadInfo(tooMany) }.exceptionOrNull()

                assertTrue(error is IllegalArgumentException)
                assertTrue(requests.isEmpty())
            }

        @Test
        fun `given an exchange without currencies when loading assets then it returns an empty list`() =
            runTest {
                val source = dataSource("""{"data":[]}""")

                val assets = source.loadAssets(BINANCE_ID)

                assertTrue(assets.isEmpty())
            }
    }
}

private const val BINANCE_ID = 270
private const val BITFINEX_ID = 294
private const val MERCADO_BITCOIN_ID = 302
private const val BINANCE_MAKER_FEE = 0.02
private const val BINANCE_TAKER_FEE = 0.04

/** Presente no indice e ausente do conteudo, de proposito. */
private const val UNLISTED_ID = 999
