package com.desafiomercadobitcoin.presentation.feature.exchangedetail

sealed interface ExchangeDetailEvent {
    data object ScreenOpened : ExchangeDetailEvent

    data object RetryDetailRequested : ExchangeDetailEvent

    data object RetryCurrenciesRequested : ExchangeDetailEvent
}
