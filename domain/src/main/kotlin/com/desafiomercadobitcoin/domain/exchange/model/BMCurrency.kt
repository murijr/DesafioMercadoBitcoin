package com.desafiomercadobitcoin.domain.exchange.model

/**
 * Uma moeda negociada em uma *exchange*. `priceUsd` é anulável porque o provedor de fato o
 * omite — ausência é caminho normal, não erro (mesma regra de [BMExchange]).
 */
data class BMCurrency(
    val name: String,
    val priceUsd: Double?,
)
