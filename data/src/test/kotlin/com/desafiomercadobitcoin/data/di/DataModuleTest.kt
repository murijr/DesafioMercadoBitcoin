package com.desafiomercadobitcoin.data.di

import com.desafiomercadobitcoin.data.network.CoinMarketCapConfig
import io.ktor.client.HttpClient
import org.junit.After
import org.junit.Assert.assertNotNull
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
    fun `given the data module when the graph starts then the http client is resolvable`() {
        val koin =
            startKoin {
                modules(dataModule(CoinMarketCapConfig(apiKey = "key", isDebug = false)))
            }.koin

        assertNotNull(koin.get<HttpClient>())
    }
}
