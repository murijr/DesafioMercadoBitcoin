package com.desafiomercadobitcoin.data.exchange.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Envelope de `/v1/exchange/map`. O `data` deste endpoint é um **array**.
 *
 * `Response` e `Entry` descrevem a forma do dado — envelope e elemento —, informação que o
 * prefixo `DM` não carrega, e por isso sobrevivem à regra de sufixo do G2.
 */
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
