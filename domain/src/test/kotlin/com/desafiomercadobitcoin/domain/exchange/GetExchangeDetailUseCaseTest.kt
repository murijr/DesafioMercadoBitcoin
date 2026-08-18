package com.desafiomercadobitcoin.domain.exchange

import com.desafiomercadobitcoin.domain.error.DomainError
import com.desafiomercadobitcoin.domain.exchange.model.BMExchangeDetail
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
class GetExchangeDetailUseCaseTest {
    abstract class TestSetup {
        protected val repository = mockk<ExchangeDetailRepository>()
        protected val useCase = GetExchangeDetailUseCase(repository)

        protected fun detail(id: Int) =
            BMExchangeDetail(
                id = id,
                name = "Binance",
                logoUrl = null,
                description = null,
                websiteUrl = null,
                makerFee = null,
                takerFee = null,
                dateLaunched = null,
            )
    }

    class HappyPath : TestSetup() {
        @Test
        fun `given a valid id when execute then returns the repository detail`() =
            runTest {
                coEvery { repository.loadDetail(1) } returns detail(1)

                val result = useCase.execute(1)

                assertEquals(detail(1), result.getOrNull())
            }
    }

    class ErrorPath : TestSetup() {
        @Test
        fun `given an arbitrary repository failure when execute then fails with a domain error`() =
            runTest {
                coEvery { repository.loadDetail(1) } throws IOException("offline")

                val result = useCase.execute(1)

                assertEquals(DomainError.Network, result.exceptionOrNull())
            }

        @Test
        fun `given an already typed domain error when execute then it crosses without remapping`() =
            runTest {
                coEvery { repository.loadDetail(1) } throws DomainError.NotFound

                val result = useCase.execute(1)

                assertEquals(DomainError.NotFound, result.exceptionOrNull())
            }

        @Test
        fun `given a cancelled caller scope when execute then the cancellation escapes`() =
            runTest {
                val started = CompletableDeferred<Unit>()
                coEvery { repository.loadDetail(1) } coAnswers {
                    started.complete(Unit)
                    CompletableDeferred<BMExchangeDetail>().await()
                }
                var observed: Throwable? = null

                val job =
                    launch(start = CoroutineStart.UNDISPATCHED) {
                        try {
                            useCase.execute(1)
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
