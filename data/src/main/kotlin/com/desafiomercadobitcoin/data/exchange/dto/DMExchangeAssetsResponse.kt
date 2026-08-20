package com.desafiomercadobitcoin.data.exchange.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DMExchangeAssetsResponse(
    val data: List<DMExchangeAsset>,
)

@Serializable
data class DMExchangeAsset(
    val currency: DMCurrency,
)

@Serializable
data class DMCurrency(
    val name: String,
    @SerialName("price_usd") val priceUsd: Double? = null,
)
