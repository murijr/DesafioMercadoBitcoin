package com.desafiomercadobitcoin.domain

import com.desafiomercadobitcoin.domain.error.DomainError
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith

@RunWith(Enclosed::class)
class UseCaseTest {
    abstract class TestSetup {
        protected fun useCaseReturning(value: String): UseCase<String, String> =
            object : UseCase<String, String>() {
                override suspend fun doExecute(input: String): String = value
            }

        protected fun useCaseThrowing(error: Throwable): UseCase<String, String> =
            object : UseCase<String, String>() {
                override suspend fun doExecute(input: String): String = throw error
            }
    }

    class HappyPath : TestSetup() {
        @Test
        fun `given a valid input when execute then returns success with the produced value`() =
            runTest {
                val result = useCaseReturning("ok").execute("input")

                assertEquals("ok", result.getOrNull())
                assertTrue(result.isSuccess)
            }
    }

    class ErrorPath : TestSetup() {
        @Test
        fun `given an arbitrary throwable when execute then fails with a domain error`() =
            runTest {
                val result = useCaseThrowing(IllegalStateException("boom")).execute("input")

                assertTrue(result.isFailure)
                assertTrue(result.exceptionOrNull() is DomainError)
            }

        @Test
        fun `given an already typed domain error when execute then fails with the very same error`() =
            runTest {
                val expected = DomainError.NotFound

                val result = useCaseThrowing(expected).execute("input")

                assertEquals(expected, result.exceptionOrNull())
            }

        @Test
        fun `given a cancelled scope when execute then the cancellation escapes instead of failing`() =
            runTest {
                val started = CompletableDeferred<Unit>()
                val useCase =
                    object : UseCase<String, String>() {
                        override suspend fun doExecute(input: String): String {
                            started.complete(Unit)
                            CompletableDeferred<Unit>().await()
                            return "unreachable"
                        }
                    }
                var observed: Throwable? = null

                val job =
                    launch(start = CoroutineStart.UNDISPATCHED) {
                        try {
                            useCase.execute("input")
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

        @Test
        fun `given an input that fails validation when execute then the repository is never touched`() =
            runTest {
                val repository = mockk<FakeRepository>()
                val useCase = ValidatingUseCase(repository)

                val result = useCase.execute("")

                assertEquals(DomainError.Validation, result.exceptionOrNull())
                coVerify(exactly = 0) { repository.fetch(any()) }
            }
    }
}

interface FakeRepository {
    suspend fun fetch(query: String): String
}

class ValidatingUseCase(
    private val repository: FakeRepository,
) : UseCase<String, String>() {
    override suspend fun doExecute(input: String): String {
        if (input.isBlank()) throw DomainError.Validation
        return repository.fetch(input)
    }
}
