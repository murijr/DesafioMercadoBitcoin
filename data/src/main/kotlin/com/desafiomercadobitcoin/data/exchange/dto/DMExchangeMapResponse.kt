package com.desafiomercadobitcoin.data.exchange.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DMExchangeMapResponse(
    val data: List<DMExchangeMapEntry>,
)

@Serializable
data class DMExchangeMapEntry(
    val id: Int,
    val name: String,
    val slug: String? = null,
    @SerialName("is_active") val isActive: Int? = null,
)
