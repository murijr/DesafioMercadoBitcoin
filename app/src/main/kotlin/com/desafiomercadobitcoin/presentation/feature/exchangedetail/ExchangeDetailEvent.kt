package com.desafiomercadobitcoin.presentation.feature.exchangedetail

/** O que o usuário fez. Entrada única do `ViewModel`. */
sealed interface ExchangeDetailEvent {
    data object ScreenOpened : ExchangeDetailEvent

    data object RetryDetailRequested : ExchangeDetailEvent

    data object RetryCurrenciesRequested : ExchangeDetailEvent
}
