package com.desafiomercadobitcoin.data.exchange.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Envelope de `/v1/exchange/assets`. `data` é um array, um item por carteira/moeda. */
@Serializable
data class DMExchangeAssetsResponse(
    val data: List<DMExchangeAsset>,
)

/** Um item de carteira do provedor; só [currency] é exibido nesta mudança. */
@Serializable
data class DMExchangeAsset(
    val currency: DMCurrency,
)

/** A moeda negociada. `priceUsd` é anulável porque o provedor de fato o omite. */
@Serializable
data class DMCurrency(
    val name: String,
    @SerialName("price_usd") val priceUsd: Double? = null,
)
