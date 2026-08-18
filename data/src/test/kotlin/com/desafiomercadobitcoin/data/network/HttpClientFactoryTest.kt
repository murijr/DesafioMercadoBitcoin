package com.desafiomercadobitcoin.data.network

import com.desafiomercadobitcoin.domain.error.DomainError
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(Enclosed::class)
class HttpClientFactoryTest {
    abstract class TestSetup {
        protected val config = CoinMarketCapConfig(apiKey = "the-secret-key", isDebug = false)

        protected fun clientRespondingJson(payload: String) =
            HttpClientFactory.create(
                MockEngine {
                    respond(
                        content = payload,
                        status = HttpStatusCode.OK,
                        headers =
                            headersOf(
                                HttpHeaders.ContentType,
                                ContentType.Application.Json.toString(),
                            ),
                    )
                },
                config,
            )
    }

    class HappyPath : TestSetup() {
        @Test
        fun `given a configured client when a request is issued then it carries the api key header`() =
            runTest {
                var sentKey: String? = null
                val client =
                    HttpClientFactory.create(
                        MockEngine { request ->
                            sentKey = request.headers[CoinMarketCapConfig.API_KEY_HEADER]
                            respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                        },
                        config,
                    )

                client.get("v1/anything")

                assertEquals("the-secret-key", sentKey)
            }

        @Test
        fun `given a response with an unknown field when deserializing then the field is ignored`() =
            runTest {
                val client = clientRespondingJson("""{"name":"Binance","unknownFuture":42}""")

                val body: SampleDto = client.get("v1/exchange").body()

                assertEquals("Binance", body.name)
            }
    }

    class ErrorPath : TestSetup() {
        @Test
        fun `given a response missing a required field when deserializing then fails with Serialization`() =
            runTest {
                val client = clientRespondingJson("""{"unrelated":1}""")

                val error =
                    runCatching { client.get("v1/exchange").body<SampleDto>() }.exceptionOrNull()

                assertEquals(DomainError.Serialization, error)
            }

        @Test
        fun `given a transport failure when requesting then fails with Network`() =
            runTest {
                val client = HttpClientFactory.create(MockEngine { throw IOException("offline") }, config)

                val error = runCatching { client.get("v1/exchange") }.exceptionOrNull()

                assertEquals(DomainError.Network, error)
            }

        @Test
        fun `given an http error status when requesting then fails with a domain error`() =
            runTest {
                val client =
                    HttpClientFactory.create(
                        MockEngine { respondError(HttpStatusCode.InternalServerError) },
                        config,
                    )

                val error = runCatching { client.get("v1/exchange") }.exceptionOrNull()

                assertTrue(error is DomainError)
            }

        @Test
        fun `given a cancelled request when it is aborted then the cancellation is not converted`() =
            runTest {
                val started = CompletableDeferred<Unit>()
                val client =
                    HttpClientFactory.create(
                        MockEngine {
                            started.complete(Unit)
                            CompletableDeferred<Unit>().await()
                            respond("{}", HttpStatusCode.OK)
                        },
                        config,
                    )
                var observed: Throwable? = null

                val job =
                    launch(start = CoroutineStart.UNDISPATCHED) {
                        try {
                            client.get("v1/exchange")
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

@Serializable
data class SampleDto(
    val name: String,
)
