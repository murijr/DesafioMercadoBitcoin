package com.desafiomercadobitcoin.domain.exchange

import com.desafiomercadobitcoin.domain.exchange.model.BMCurrency
import com.desafiomercadobitcoin.domain.exchange.model.BMExchangeDetail

interface ExchangeDetailRepository {
    suspend fun loadDetail(exchangeId: Int): BMExchangeDetail

    suspend fun loadCurrencies(exchangeId: Int): List<BMCurrency>
}
