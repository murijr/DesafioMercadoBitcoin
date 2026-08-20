package com.desafiomercadobitcoin.presentation.feature.exchangelist

sealed interface ExchangeListEffect {
    data class OpenExchangeDetail(
        val exchangeId: Int,
    ) : ExchangeListEffect
}
