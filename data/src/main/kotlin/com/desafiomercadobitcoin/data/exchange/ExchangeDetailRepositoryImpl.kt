package com.desafiomercadobitcoin.data.exchange

import com.desafiomercadobitcoin.data.exchange.mapper.toBM
import com.desafiomercadobitcoin.data.exchange.mapper.toDetailBM
import com.desafiomercadobitcoin.domain.error.DomainError
import com.desafiomercadobitcoin.domain.exchange.ExchangeDetailRepository
import com.desafiomercadobitcoin.domain.exchange.model.BMCurrency
import com.desafiomercadobitcoin.domain.exchange.model.BMExchangeDetail

class ExchangeDetailRepositoryImpl(
    private val remote: ExchangeRemoteDataSource,
) : ExchangeDetailRepository {
    override suspend fun loadDetail(exchangeId: Int): BMExchangeDetail {
        val content = remote.loadInfo(listOf(exchangeId))
        return content[exchangeId.toString()]?.toDetailBM() ?: throw DomainError.NotFound()
    }

    override suspend fun loadCurrencies(exchangeId: Int): List<BMCurrency> =
        remote
            .loadAssets(exchangeId)
            .map { it.toBM() }
            .distinctBy { it.name.normalizeForDeduplication() }
            .map { it.copy(name = it.name.trim()) }

    private fun String.normalizeForDeduplication(): String = trim().lowercase()
}
