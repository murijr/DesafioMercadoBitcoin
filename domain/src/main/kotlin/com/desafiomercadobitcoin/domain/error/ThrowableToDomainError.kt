package com.desafiomercadobitcoin.domain.error

import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.nio.channels.UnresolvedAddressException

/**
 * Traduz uma exceção arbitrária do mundo externo em [DomainError].
 *
 * Ponto único da conversão: quem estiver acima só enxerga erro tipado.
 * O cancelamento não é traduzível — re-lançar é o comportamento correto.
 */
fun Throwable.toDomainError(): DomainError =
    when (this) {
        is CancellationException -> throw this
        is DomainError -> this
        is IOException -> DomainError.Network()
        // O engine CIO do Ktor resolve o host via NIO: sem DNS/conectividade, lança essa
        // exceção *antes* de qualquer I/O, então ela não é uma IOException.
        is UnresolvedAddressException -> DomainError.Network()
        else -> if (isSerializationFailure()) DomainError.Serialization() else DomainError.Unexpected()
    }

/**
 * A serialização vive em `:data` (kotlinx.serialization) e `:domain` não pode importá-la.
 * A checagem é feita pelo nome do tipo, que é o preço de manter a fronteira Kotlin puro (G1).
 */
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
