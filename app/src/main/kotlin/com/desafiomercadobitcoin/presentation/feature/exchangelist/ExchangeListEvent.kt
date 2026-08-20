package com.desafiomercadobitcoin.presentation.feature.exchangelist

sealed interface ExchangeListEvent {
    data object ScreenOpened : ExchangeListEvent

    data object NextPageRequested : ExchangeListEvent

    data object RetryRequested : ExchangeListEvent

    data class ExchangeSelected(
        val exchangeId: Int,
    ) : ExchangeListEvent
}
