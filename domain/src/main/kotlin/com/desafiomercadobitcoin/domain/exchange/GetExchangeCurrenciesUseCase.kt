package com.desafiomercadobitcoin.domain.exchange

import com.desafiomercadobitcoin.domain.UseCase
import com.desafiomercadobitcoin.domain.exchange.model.BMCurrency

class GetExchangeCurrenciesUseCase(
    private val repository: ExchangeDetailRepository,
) : UseCase<Int, List<BMCurrency>>() {
    override suspend fun doExecute(input: Int): List<BMCurrency> = repository.loadCurrencies(input)
}
