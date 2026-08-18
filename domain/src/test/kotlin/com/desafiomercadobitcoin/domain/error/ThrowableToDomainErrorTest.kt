package com.desafiomercadobitcoin.domain.error

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import java.io.IOException
import java.nio.channels.UnresolvedAddressException

@RunWith(Enclosed::class)
class ThrowableToDomainErrorTest {
    abstract class TestSetup {
        protected fun serializationFailure(message: String): Throwable = FakeSerializationException(message)
    }

    class HappyPath : TestSetup() {
        @Test
        fun `given an io exception when converting then returns the network error`() {
            assertEquals(DomainError.Network, IOException("offline").toDomainError())
        }

        @Test
        fun `given an unresolved address exception when converting then returns the network error`() {
            assertEquals(DomainError.Network, UnresolvedAddressException().toDomainError())
        }

        @Test
        fun `given a serialization exception when converting then returns the serialization error`() {
            val error = serializationFailure("missing field")

            assertEquals(DomainError.Serialization, error.toDomainError())
        }

        @Test
        fun `given an unknown exception when converting then returns the unexpected error`() {
            assertEquals(DomainError.Unexpected, IllegalStateException("?").toDomainError())
        }

        @Test
        fun `given an already typed domain error when converting then returns it untouched`() {
            assertEquals(DomainError.NotFound, DomainError.NotFound.toDomainError())
        }
    }

    class EdgeCases : TestSetup() {
        @Test
        fun `given a serialization failure wrapped in a cause when converting then unwraps it`() {
            val wrapped = RuntimeException("wrapped", serializationFailure("bad json"))

            assertEquals(DomainError.Serialization, wrapped.toDomainError())
        }

        @Test
        fun `given a cancellation when converting then it is rethrown instead of converted`() {
            assertThrows(CancellationException::class.java) {
                CancellationException("cancelled").toDomainError()
            }
        }
    }
}

private class FakeSerializationException(
    message: String,
) : RuntimeException(message)
