package com.desafiomercadobitcoin.domain.error

/**
 * Chave de texto do domínio. O domínio nunca conhece o texto final nem o recurso que o
 * carrega — quem resolve `TextKey` para uma string localizada é o `ResourceProvider`, em `:app`.
 *
 * Hierarquia fechada de propósito: acrescentar uma chave quebra a resolução exaustiva em `:app`
 * até que a tradução exista.
 */
sealed interface TextKey {
    data object InvalidInput : TextKey

    data object NotFound : TextKey

    data object NetworkUnavailable : TextKey

    data object UnexpectedResponse : TextKey

    data object Unexpected : TextKey
}
