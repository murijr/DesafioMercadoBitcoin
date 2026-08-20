package com.desafiomercadobitcoin.domain.error

sealed class DomainError(
    val textKey: TextKey,
) : Throwable() {
    override fun equals(other: Any?): Boolean = other != null && this::class == other::class

    override fun hashCode(): Int = this::class.hashCode()

    class Validation : DomainError(TextKey.InvalidInput)

    class NotFound : DomainError(TextKey.NotFound)

    class Network : DomainError(TextKey.NetworkUnavailable)

    class Serialization : DomainError(TextKey.UnexpectedResponse)

    class Unexpected : DomainError(TextKey.Unexpected)
}
