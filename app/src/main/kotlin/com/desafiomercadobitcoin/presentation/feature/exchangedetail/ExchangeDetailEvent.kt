package com.desafiomercadobitcoin.presentation.feature.exchangedetail

sealed interface ExchangeDetailEvent {
    data class ScreenOpened(
        val exchangeId: Int,
    ) : ExchangeDetailEvent

    data object RetryDetailRequested : ExchangeDetailEvent

    data object RetryCurrenciesRequested : ExchangeDetailEvent
}
