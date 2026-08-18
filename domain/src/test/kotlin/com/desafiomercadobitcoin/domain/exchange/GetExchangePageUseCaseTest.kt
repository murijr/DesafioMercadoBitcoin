package com.desafiomercadobitcoin.domain.exchange

import com.desafiomercadobitcoin.domain.error.DomainError
import com.desafiomercadobitcoin.domain.exchange.model.BMExchange
import com.desafiomercadobitcoin.domain.exchange.model.BMExchangePage
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

@RunWith(Enclosed::class)
class GetExchangePageUseCaseTest {
    abstract class TestSetup {
        protected val repository = mockk<ExchangeRepository>()
        protected val useCase = GetExchangePageUseCase(repository)

        protected fun exchange(
            id: Int,
            name: String,
        ) = BMExchange(
            id = id,
            name = name,
            logoUrl = null,
            spotVolumeUsd = null,
            dateLaunched = null,
        )
    }

    class HappyPath : TestSetup() {
        @Test
        fun `given a valid page when execute then returns it in the repository order`() =
            runTest {
                val page =
                    BMExchangePage(
                        items = listOf(exchange(1, "Binance"), exchange(2, "OKX")),
                        page = 0,
                        hasMore = true,
                    )
                coEvery { repository.loadPage(0) } returns page

                val result = useCase.execute(0)

                assertEquals(page, result.getOrNull())
                assertEquals(listOf("Binance", "OKX"), result.getOrNull()?.items?.map { it.name })
            }

        @Test
        fun `given the last page when execute then it reports no more content`() =
            runTest {
                coEvery { repository.loadPage(LAST_PAGE) } returns
                    BMExchangePage(items = emptyList(), page = LAST_PAGE, hasMore = false)

                val result = useCase.execute(LAST_PAGE)

                assertEquals(false, result.getOrNull()?.hasMore)
            }
    }

    class ErrorPath : TestSetup() {
        @Test
        fun `given an arbitrary repository failure when execute then fails with a domain error`() =
            runTest {
                coEvery { repository.loadPage(0) } throws IOException("offline")

                val result = useCase.execute(0)

                assertEquals(DomainError.Network, result.exceptionOrNull())
            }

        @Test
        fun `given an already typed domain error when execute then it crosses without remapping`() =
            runTest {
                coEvery { repository.loadPage(0) } throws DomainError.NotFound

                val result = useCase.execute(0)

                assertEquals(DomainError.NotFound, result.exceptionOrNull())
            }

        @Test
        fun `given a cancelled caller scope when execute then the cancellation escapes`() =
            runTest {
                val started = CompletableDeferred<Unit>()
                coEvery { repository.loadPage(0) } coAnswers {
                    started.complete(Unit)
                    CompletableDeferred<BMExchangePage>().await()
                }
                var observed: Throwable? = null

                val job =
                    launch(start = CoroutineStart.UNDISPATCHED) {
                        try {
                            useCase.execute(0)
                        } catch (error: CancellationException) {
                            observed = error
                            throw error
                        }
                    }
                started.await()
                job.cancel()
                job.join()

                assertTrue(observed is CancellationException)
            }
    }
}

private const val LAST_PAGE = 9
