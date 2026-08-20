package com.desafiomercadobitcoin.domain.exchange

import com.desafiomercadobitcoin.domain.UseCase
import com.desafiomercadobitcoin.domain.exchange.model.BMExchangeDetail

class GetExchangeDetailUseCase(
    private val repository: ExchangeDetailRepository,
) : UseCase<Int, BMExchangeDetail>() {
    override suspend fun doExecute(input: Int): BMExchangeDetail = repository.loadDetail(input)
}
