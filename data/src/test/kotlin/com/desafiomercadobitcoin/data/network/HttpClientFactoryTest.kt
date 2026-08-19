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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(Enclosed::class)
class HttpClientFactoryTest {
    abstract class TestSetup {
        protected val config = CoinMarketCapConfig(apiKey = "the-secret-key", isDebug = false)

        protected suspend fun errorFrom(status: HttpStatusCode): Throwable? {
            val client = HttpClientFactory.create(MockEngine { respondError(status) }, config)

            return runCatching { client.get("v1/exchange") }.exceptionOrNull()
        }

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
        fun `given the image client when a request is issued then it carries no api key header`() =
            runTest {
                var sentKey: String? = "nao-lido"
                val client =
                    HttpClientFactory.createImageClient(
                        MockEngine { request ->
                            sentKey = request.headers[CoinMarketCapConfig.API_KEY_HEADER]
                            respond("", HttpStatusCode.OK)
                        },
                    )

                client.get("https://s2.coinmarketcap.com/static/img/exchanges/64x64/270.png")

                assertNull(sentKey)
            }

        @Test
        fun `given the image client when the host refuses the image then no domain error is raised`() =
            runTest {
                val client =
                    HttpClientFactory.createImageClient(
                        MockEngine { respondError(HttpStatusCode.NotFound) },
                    )

                val error =
                    runCatching {
                        client.get("https://s2.coinmarketcap.com/static/img/exchanges/64x64/1.png")
                    }.exceptionOrNull()

                assertFalse(error is DomainError)
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

                assertEquals(DomainError.Serialization(), error)
            }

        @Test
        fun `given a transport failure when requesting then fails with Network`() =
            runTest {
                val client = HttpClientFactory.create(MockEngine { throw IOException("offline") }, config)

                val error = runCatching { client.get("v1/exchange") }.exceptionOrNull()

                assertEquals(DomainError.Network(), error)
            }

        @Test
        fun `given a missing or refused api key when requesting then fails with Network`() =
            runTest {
                val error = errorFrom(HttpStatusCode.Unauthorized)

                assertEquals(DomainError.Network(), error)
            }

        @Test
        fun `given a forbidden response when requesting then fails with Network`() =
            runTest {
                val error = errorFrom(HttpStatusCode.Forbidden)

                assertEquals(DomainError.Network(), error)
            }

        @Test
        fun `given the call limit is exceeded when requesting then fails with Network`() =
            runTest {
                val error = errorFrom(HttpStatusCode.TooManyRequests)

                assertEquals(DomainError.Network(), error)
            }

        @Test
        fun `given a missing resource when requesting then fails with NotFound`() =
            runTest {
                val error = errorFrom(HttpStatusCode.NotFound)

                assertEquals(DomainError.NotFound(), error)
            }

        @Test
        fun `given a server failure when requesting then fails with Network`() =
            runTest {
                val error = errorFrom(HttpStatusCode.InternalServerError)

                assertEquals(DomainError.Network(), error)
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
