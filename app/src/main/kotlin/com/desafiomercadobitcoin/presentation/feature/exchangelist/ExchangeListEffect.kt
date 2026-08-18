package com.desafiomercadobitcoin.presentation.feature.exchangelist

/** Acontece uma vez só e não pertence ao estado — navegação, neste caso. */
sealed interface ExchangeListEffect {
    data class OpenExchangeDetail(
        val exchangeId: Int,
    ) : ExchangeListEffect
}
