package com.desafiomercadobitcoin.domain.exchange.model

data class BMExchangePage(
    val items: List<BMExchange>,
    val page: Int,
    val hasMore: Boolean,
)
