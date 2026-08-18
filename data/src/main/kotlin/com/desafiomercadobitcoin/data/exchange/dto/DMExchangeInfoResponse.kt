package com.desafiomercadobitcoin.data.exchange.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Envelope de `/v1/exchange/info`. Ao contrário do `map`, o `data` aqui é um **objeto**
 * cuja chave é o id da corretora em forma de texto.
 */
@Serializable
data class DMExchangeInfoResponse(
    val data: Map<String, DMExchangeInfo>,
)

/**
 * Conteúdo exibível de uma corretora. `logo`, `dateLaunched` e `spotVolumeUsd` são anuláveis
 * porque o provedor de fato os omite — ausência é caminho normal, não erro.
 */
@Serializable
data class DMExchangeInfo(
    val id: Int,
    val name: String,
    val logo: String? = null,
    @SerialName("date_launched") val dateLaunched: String? = null,
    @SerialName("spot_volume_usd") val spotVolumeUsd: Double? = null,
)
