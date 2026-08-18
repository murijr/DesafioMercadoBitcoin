package com.desafiomercadobitcoin.presentation.feature.exchangedetail.model

/** Uma moeda negociada como a listagem a exibe. `priceLabel` já chega formatado. */
data class VMCurrency(
    val name: String,
    val priceLabel: String,
)
