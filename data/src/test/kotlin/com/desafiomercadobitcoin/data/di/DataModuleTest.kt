package com.desafiomercadobitcoin.data.di

import com.desafiomercadobitcoin.data.network.CoinMarketCapConfig
import com.desafiomercadobitcoin.domain.exchange.ExchangeDetailRepository
import com.desafiomercadobitcoin.domain.exchange.ExchangeRepository
import io.ktor.client.HttpClient
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner

/**
 * Roda sob Robolectric porque o engine `Android` do Ktor precisa do runtime Android
 * (JVM com sombra, sem emulador — G7).
 */
@RunWith(RobolectricTestRunner::class)
class DataModuleTest {
    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `given the data module when the graph starts then both http clients are resolvable`() {
        val koin = startGraph()

        assertNotNull(koin.get<HttpClient>(apiHttpClient))
        assertNotNull(koin.get<HttpClient>(imageHttpClient))
    }

    @Test
    fun `given the data module when the graph starts then the two clients are distinct`() {
        val koin = startGraph()

        assertNotSame(koin.get<HttpClient>(apiHttpClient), koin.get<HttpClient>(imageHttpClient))
    }

    @Test
    fun `given the data module when the graph starts then the exchange repository is resolvable`() {
        val koin = startGraph()

        assertNotNull(koin.get<ExchangeRepository>())
    }

    @Test
    fun `given the data module when the graph starts then the exchange detail repository is resolvable`() {
        val koin = startGraph()

        assertNotNull(koin.get<ExchangeDetailRepository>())
    }

    private fun startGraph() =
        startKoin {
            modules(dataModule(CoinMarketCapConfig(apiKey = "key", isDebug = false)))
        }.koin
}
