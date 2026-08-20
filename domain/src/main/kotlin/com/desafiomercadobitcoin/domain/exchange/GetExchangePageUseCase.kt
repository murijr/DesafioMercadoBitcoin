package com.desafiomercadobitcoin.domain.exchange

import com.desafiomercadobitcoin.domain.UseCase
import com.desafiomercadobitcoin.domain.exchange.model.BMExchangePage

class GetExchangePageUseCase(
    private val repository: ExchangeRepository,
) : UseCase<Int, BMExchangePage>() {
    override suspend fun doExecute(input: Int): BMExchangePage = repository.loadPage(input)
}
