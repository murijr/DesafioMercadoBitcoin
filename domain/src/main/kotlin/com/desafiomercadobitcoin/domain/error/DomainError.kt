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
    data object Validation : DomainError(TextKey.InvalidInput)

    data object NotFound : DomainError(TextKey.NotFound)

    data object Network : DomainError(TextKey.NetworkUnavailable)

    data object Serialization : DomainError(TextKey.UnexpectedResponse)

    data object Unexpected : DomainError(TextKey.Unexpected)
}
