package com.desafiomercadobitcoin.domain.exchange

import com.desafiomercadobitcoin.domain.UseCase
import com.desafiomercadobitcoin.domain.exchange.model.BMExchangeDetail

/**
 * Entrega o detalhe da *exchange* `input`. Sem regra além da delegação — mesma razão de
 * existir de [GetExchangePageUseCase].
 */
class GetExchangeDetailUseCase(
    private val repository: ExchangeDetailRepository,
) : UseCase<Int, BMExchangeDetail>() {
    override suspend fun doExecute(input: Int): BMExchangeDetail = repository.loadDetail(input)
}
