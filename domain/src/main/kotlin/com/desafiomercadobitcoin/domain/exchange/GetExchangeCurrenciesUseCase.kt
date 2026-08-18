package com.desafiomercadobitcoin.domain.exchange

import com.desafiomercadobitcoin.domain.UseCase
import com.desafiomercadobitcoin.domain.exchange.model.BMCurrency

/**
 * Entrega as moedas negociadas pela *exchange* `input`. Sem regra além da delegação — mesma
 * razão de existir de [GetExchangePageUseCase].
 */
class GetExchangeCurrenciesUseCase(
    private val repository: ExchangeDetailRepository,
) : UseCase<Int, List<BMCurrency>>() {
    override suspend fun doExecute(input: Int): List<BMCurrency> = repository.loadCurrencies(input)
}
