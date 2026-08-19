package com.desafiomercadobitcoin.domain.error

/**
 * Erro de negócio. É um `Throwable` porque viaja dentro de `Result.failure`, mas nunca escapa
 * de um `UseCase` como exceção — ver [com.desafiomercadobitcoin.domain.UseCase].
 *
 * Cada subtipo carrega sua [TextKey]: nenhuma mensagem destinada ao usuário existe aqui.
 */
sealed class DomainError(
    val textKey: TextKey,
) : Throwable() {
    /** Duas instâncias do mesmo subtipo representam o mesmo erro de negócio. */
    override fun equals(other: Any?): Boolean = other != null && this::class == other::class

    override fun hashCode(): Int = this::class.hashCode()

    class Validation : DomainError(TextKey.InvalidInput)

    class NotFound : DomainError(TextKey.NotFound)

    class Network : DomainError(TextKey.NetworkUnavailable)

    class Serialization : DomainError(TextKey.UnexpectedResponse)

    class Unexpected : DomainError(TextKey.Unexpected)
}
