package com.desafiomercadobitcoin.domain.exchange

import com.desafiomercadobitcoin.domain.exchange.model.BMExchangePage

interface ExchangeRepository {
    suspend fun loadPage(page: Int): BMExchangePage
}
