package com.desafiomercadobitcoin.data.exchange

import com.desafiomercadobitcoin.data.exchange.dto.DMExchangeInfo
import com.desafiomercadobitcoin.data.exchange.dto.DMExchangeMapEntry
import com.desafiomercadobitcoin.domain.error.DomainError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import java.io.IOException
import java.time.Instant

@RunWith(Enclosed::class)
class ExchangeRepositoryImplTest {
    abstract class TestSetup {
        protected val remote = mockk<ExchangeRemoteDataSource>()
        protected val repository = ExchangeRepositoryImpl(remote)

        protected fun givenIndexOf(vararg ids: Int) {
            coEvery { remote.loadActiveIndex() } returns
                ids.map { DMExchangeMapEntry(id = it, name = "Exchange $it") }
        }

        protected fun givenIndexOfSize(size: Int) {
            coEvery { remote.loadActiveIndex() } returns
                (1..size).map { DMExchangeMapEntry(id = it, name = "Exchange $it") }
        }

        protected fun givenContentFor(vararg ids: Int) {
            coEvery { remote.loadInfo(any()) } answers {
                val requested = firstArg<List<Int>>()
                requested
                    .filter { it in ids }
                    .associate { it.toString() to info(it) }
            }
        }

        protected fun givenContentForEveryRequestedId() {
            coEvery { remote.loadInfo(any()) } answers {
                firstArg<List<Int>>().associate { it.toString() to info(it) }
            }
        }

        protected fun info(
            id: Int,
            dateLaunched: String? = "2017-07-14T00:00:00.000Z",
            spotVolumeUsd: Double? = 1.0,
        ) = DMExchangeInfo(
            id = id,
            name = "Exchange $id",
            logo = "https://logo/$id.png",
            dateLaunched = dateLaunched,
            spotVolumeUsd = spotVolumeUsd,
        )
    }

    class HappyPath : TestSetup() {
        @Test
        fun `given an index and its content when loading a page then both are composed`() =
            runTest {
                givenIndexOf(BINANCE_ID, BITFINEX_ID)
                givenContentForEveryRequestedId()

                val page = repository.loadPage(0)

                assertEquals(listOf(BINANCE_ID, BITFINEX_ID), page.items.map { it.id })
                assertEquals("https://logo/$BINANCE_ID.png", page.items.first().logoUrl)
                assertEquals(Instant.parse("2017-07-14T00:00:00.000Z"), page.items.first().dateLaunched)
            }

        @Test
        fun `given an index id without matching content when loading a page then it is omitted`() =
            runTest {
                givenIndexOf(BINANCE_ID, UNLISTED_ID, BITFINEX_ID)
                givenContentFor(BINANCE_ID, BITFINEX_ID)

                val page = repository.loadPage(0)

                assertEquals(listOf(BINANCE_ID, BITFINEX_ID), page.items.map { it.id })
            }

        @Test
        fun `given remaining ids in the index when loading a page then it reports more content`() =
            runTest {
                givenIndexOfSize(ONE_AND_A_HALF_BATCHES)
                givenContentForEveryRequestedId()

                val page = repository.loadPage(0)

                assertEquals(BATCH_SIZE, page.items.size)
                assertTrue(page.hasMore)
            }

        @Test
        fun `given the last batch when loading it then it reports no more content`() =
            runTest {
                givenIndexOfSize(ONE_AND_A_HALF_BATCHES)
                givenContentForEveryRequestedId()

                val page = repository.loadPage(SECOND_PAGE)

                assertEquals(ONE_AND_A_HALF_BATCHES - BATCH_SIZE, page.items.size)
                assertEquals(SECOND_PAGE, page.page)
                assertFalse(page.hasMore)
            }

        @Test
        fun `given an index order when loading a page then the items keep it`() =
            runTest {
                givenIndexOf(MERCADO_BITCOIN_ID, BINANCE_ID, BITFINEX_ID)
                givenContentForEveryRequestedId()

                val page = repository.loadPage(0)

                assertEquals(listOf(MERCADO_BITCOIN_ID, BINANCE_ID, BITFINEX_ID), page.items.map { it.id })
            }

        @Test
        fun `given content without optional fields when composing then absence is preserved`() =
            runTest {
                givenIndexOf(BITFINEX_ID)
                coEvery { remote.loadInfo(any()) } returns
                    mapOf("$BITFINEX_ID" to info(BITFINEX_ID, dateLaunched = null, spotVolumeUsd = null))

                val page = repository.loadPage(0)

                assertNull(page.items.single().spotVolumeUsd)
                assertNull(page.items.single().dateLaunched)
            }

        @Test
        fun `given an empty index when loading the first page then the page is empty and final`() =
            runTest {
                givenIndexOf()
                givenContentForEveryRequestedId()

                val page = repository.loadPage(0)

                assertTrue(page.items.isEmpty())
                assertFalse(page.hasMore)
                coVerify(exactly = 0) { remote.loadInfo(any()) }
            }

        @Test
        fun `given two sequential pages when loading them then the index is fetched once`() =
            runTest {
                givenIndexOfSize(ONE_AND_A_HALF_BATCHES)
                givenContentForEveryRequestedId()

                repository.loadPage(0)
                repository.loadPage(SECOND_PAGE)

                coVerify(exactly = 1) { remote.loadActiveIndex() }
                coVerify(exactly = 2) { remote.loadInfo(any()) }
            }

        @Test
        fun `given two concurrent calls for the same page when loading then the index is fetched once`() =
            runTest {
                givenIndexOfSize(ONE_AND_A_HALF_BATCHES)
                givenContentForEveryRequestedId()

                listOf(
                    async { repository.loadPage(0) },
                    async { repository.loadPage(0) },
                ).awaitAll()

                coVerify(exactly = 1) { remote.loadActiveIndex() }
            }
    }

    class ErrorPath : TestSetup() {
        @Test
        fun `given the index request fails when loading a page then the failure propagates`() =
            runTest {
                coEvery { remote.loadActiveIndex() } throws DomainError.Network()

                val error = runCatching { repository.loadPage(0) }.exceptionOrNull()

                assertEquals(DomainError.Network(), error)
            }

        @Test
        fun `given the content request fails when loading a page then the failure propagates`() =
            runTest {
                givenIndexOf(BINANCE_ID)
                coEvery { remote.loadInfo(any()) } throws DomainError.Serialization()

                val error = runCatching { repository.loadPage(0) }.exceptionOrNull()

                assertEquals(DomainError.Serialization(), error)
            }

        @Test
        fun `given a transport failure when loading a page then it is not converted here`() =
            runTest {
                coEvery { remote.loadActiveIndex() } throws IOException("offline")

                val error = runCatching { repository.loadPage(0) }.exceptionOrNull()

                assertTrue(error is IOException)
            }

        @Test
        fun `given a failed index when retrying then the index is requested again`() =
            runTest {
                coEvery { remote.loadActiveIndex() } throws DomainError.Network()
                runCatching { repository.loadPage(0) }
                givenIndexOf(BINANCE_ID)
                givenContentForEveryRequestedId()

                val page = repository.loadPage(0)

                assertEquals(listOf(BINANCE_ID), page.items.map { it.id })
            }
    }
}

private const val BINANCE_ID = 270
private const val BITFINEX_ID = 294
private const val MERCADO_BITCOIN_ID = 302

private const val UNLISTED_ID = 999

private const val SECOND_PAGE = 1
private const val BATCH_SIZE = 100
private const val ONE_AND_A_HALF_BATCHES = 150
