package com.desafiomercadobitcoin.domain.exchange.model

import java.time.Instant

/**
 * Uma corretora do catálogo do provedor.
 *
 * `logoUrl`, `spotVolumeUsd` e `dateLaunched` são anuláveis porque o provedor de fato os
 * omite — `spot_volume_usd` em ~14% das corretoras e `date_launched` em ~3%. Ausência é
 * caminho normal, não erro: quem decide **como** comunicá-la é a apresentação (D2).
 */
data class BMExchange(
    val id: Int,
    val name: String,
    val logoUrl: String?,
    val spotVolumeUsd: Double?,
    val dateLaunched: Instant?,
)
