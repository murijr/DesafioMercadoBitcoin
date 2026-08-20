package com.desafiomercadobitcoin.domain.error

import kotlinx.coroutines.CancellationException
import java.io.IOException

fun Throwable.toDomainError(): DomainError =
    when (this) {
        is CancellationException -> throw this
        is DomainError -> this
        is IOException -> DomainError.Network()
        else -> if (isSerializationFailure()) DomainError.Serialization() else DomainError.Unexpected()
    }

private fun Throwable.isSerializationFailure(): Boolean {
    var current: Throwable? = this
    while (current != null) {
        val name = current::class.qualifiedName.orEmpty()
        if (name.startsWith("kotlinx.serialization.") || name.endsWith("SerializationException")) {
            return true
        }
        current = current.cause?.takeIf { it !== current }
    }
    return false
}
