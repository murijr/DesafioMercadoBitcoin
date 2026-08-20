package com.desafiomercadobitcoin.data.exchange.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DMExchangeInfoResponse(
    val data: Map<String, DMExchangeInfo>,
)

@Serializable
data class DMExchangeInfo(
    val id: Int,
    val name: String,
    val logo: String? = null,
    val description: String? = null,
    val urls: DMExchangeUrls? = null,
    @SerialName("date_launched") val dateLaunched: String? = null,
    @SerialName("spot_volume_usd") val spotVolumeUsd: Double? = null,
    @SerialName("maker_fee") val makerFee: Double? = null,
    @SerialName("taker_fee") val takerFee: Double? = null,
)

@Serializable
data class DMExchangeUrls(
    val website: List<String> = emptyList(),
)
