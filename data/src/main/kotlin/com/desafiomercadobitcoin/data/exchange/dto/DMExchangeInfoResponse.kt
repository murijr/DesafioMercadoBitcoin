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
 * Conteúdo exibível de uma corretora. Todo campo além de `id`/`name` é anulável porque o
 * provedor de fato os omite — ausência é caminho normal, não erro. `description`, `urls`,
 * `makerFee` e `takerFee` alimentam só o detalhe (D1 de `add-exchange-detail`); a listagem
 * continua ignorando-os.
 */
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

/** O envelope `urls` do provedor; só `website` é exibido nesta mudança. */
@Serializable
data class DMExchangeUrls(
    val website: List<String> = emptyList(),
)
