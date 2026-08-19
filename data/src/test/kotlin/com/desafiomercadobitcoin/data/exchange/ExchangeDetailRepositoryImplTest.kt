package com.desafiomercadobitcoin.data.exchange

import com.desafiomercadobitcoin.data.exchange.dto.DMCurrency
import com.desafiomercadobitcoin.data.exchange.dto.DMExchangeAsset
import com.desafiomercadobitcoin.data.exchange.dto.DMExchangeInfo
import com.desafiomercadobitcoin.domain.error.DomainError
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(Enclosed::class)
class ExchangeDetailRepositoryImplTest {
    abstract class TestSetup {
        protected val remote = mockk<ExchangeRemoteDataSource>()
        protected val repository = ExchangeDetailRepositoryImpl(remote)

        protected fun info(id: Int) =
            DMExchangeInfo(
                id = id,
                name = "Exchange $id",
                logo = "https://logo/$id.png",
            )

        protected fun asset(name: String) = DMExchangeAsset(currency = DMCurrency(name = name, priceUsd = 1.0))
    }

    class HappyPath : TestSetup() {
        @Test
        fun `given a known id when loading the detail then it delegates to a single-id info request`() =
            runTest {
                coEvery { remote.loadInfo(listOf(BINANCE_ID)) } returns mapOf("$BINANCE_ID" to info(BINANCE_ID))

                val detail = repository.loadDetail(BINANCE_ID)

                assertEquals(BINANCE_ID, detail.id)
                assertEquals("Exchange $BINANCE_ID", detail.name)
            }

        @Test
        fun `given a known id when loading currencies then it delegates to the assets request`() =
            runTest {
                coEvery { remote.loadAssets(BINANCE_ID) } returns listOf(asset("Bitcoin"), asset("Ethereum"))

                val currencies = repository.loadCurrencies(BINANCE_ID)

                assertEquals(listOf("Bitcoin", "Ethereum"), currencies.map { it.name })
            }

        @Test
        fun `given an exchange without currencies when loading currencies then it returns an empty list`() =
            runTest {
                coEvery { remote.loadAssets(BINANCE_ID) } returns emptyList()

                val currencies = repository.loadCurrencies(BINANCE_ID)

                assertTrue(currencies.isEmpty())
            }

        @Test
        fun `given the same currency in more than one wallet when loading currencies then it appears once`() =
            runTest {
                coEvery { remote.loadAssets(BINANCE_ID) } returns
                    listOf(asset("USDD"), asset("Bitcoin"), asset("USDD"))

                val currencies = repository.loadCurrencies(BINANCE_ID)

                assertEquals(listOf("USDD", "Bitcoin"), currencies.map { it.name })
            }

        @Test
        fun `given the same currency in different casing when loading currencies then it appears once`() =
            runTest {
                coEvery { remote.loadAssets(BINANCE_ID) } returns
                    listOf(asset("Avantis"), asset("avantis"))

                val currencies = repository.loadCurrencies(BINANCE_ID)

                assertEquals(listOf("Avantis"), currencies.map { it.name })
            }

        @Test
        fun `given the same currency with surrounding whitespace when loading currencies then it appears once`() =
            runTest {
                coEvery { remote.loadAssets(BINANCE_ID) } returns
                    listOf(asset("Avantis"), asset("  Avantis "))

                val currencies = repository.loadCurrencies(BINANCE_ID)

                assertEquals(listOf("Avantis"), currencies.map { it.name })
            }

        @Test
        fun `given the padded variant arrives first when loading currencies then the displayed name has no surrounding whitespace`() =
            runTest {
                coEvery { remote.loadAssets(BINANCE_ID) } returns
                    listOf(asset("  Avantis "), asset("Avantis"))

                val currencies = repository.loadCurrencies(BINANCE_ID)

                assertEquals(listOf("Avantis"), currencies.map { it.name })
            }

        @Test
        fun `given case variants the first priceUsd wins when loading currencies`() =
            runTest {
                val first = DMExchangeAsset(currency = DMCurrency(name = "Avantis", priceUsd = 1.20))
                val second = DMExchangeAsset(currency = DMCurrency(name = "avantis", priceUsd = 1.21))
                coEvery { remote.loadAssets(BINANCE_ID) } returns listOf(first, second)

                val currencies = repository.loadCurrencies(BINANCE_ID)

                assertEquals(listOf("Avantis"), currencies.map { it.name })
                assertEquals(1.20, currencies.single().priceUsd!!, 0.0001)
            }
    }

    class ErrorPath : TestSetup() {
        @Test
        fun `given an id absent from the info response when loading the detail then it fails with NotFound`() =
            runTest {
                coEvery { remote.loadInfo(listOf(BINANCE_ID)) } returns emptyMap()

                val error = runCatching { repository.loadDetail(BINANCE_ID) }.exceptionOrNull()

                assertEquals(DomainError.NotFound(), error)
            }

        @Test
        fun `given a transport failure when loading the detail then it is not converted here`() =
            runTest {
                coEvery { remote.loadInfo(listOf(BINANCE_ID)) } throws IOException("offline")

                val error = runCatching { repository.loadDetail(BINANCE_ID) }.exceptionOrNull()

                assertTrue(error is IOException)
            }

        @Test
        fun `given a transport failure when loading currencies then the detail is unaffected`() =
            runTest {
                coEvery { remote.loadInfo(listOf(BINANCE_ID)) } returns mapOf("$BINANCE_ID" to info(BINANCE_ID))
                coEvery { remote.loadAssets(BINANCE_ID) } throws IOException("offline")

                val detail = repository.loadDetail(BINANCE_ID)
                val error = runCatching { repository.loadCurrencies(BINANCE_ID) }.exceptionOrNull()

                assertEquals(BINANCE_ID, detail.id)
                assertTrue(error is IOException)
            }
    }
}

private const val BINANCE_ID = 270
