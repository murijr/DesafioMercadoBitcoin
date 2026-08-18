package com.desafiomercadobitcoin.domain.exchange.model

import java.time.Instant

/**
 * O conteúdo completo de uma *exchange*, exibido na tela de detalhe.
 *
 * Todo campo além de `id` e `name` é anulável porque o provedor de fato os omite — mesma regra
 * de [BMExchange]: ausência é caminho normal, não erro. Modelo separado de `BMExchange` porque a
 * listagem e o detalhe têm necessidades de dado diferentes (D1 de `add-exchange-detail`).
 */
data class BMExchangeDetail(
    val id: Int,
    val name: String,
    val logoUrl: String?,
    val description: String?,
    val websiteUrl: String?,
    val makerFee: Double?,
    val takerFee: Double?,
    val dateLaunched: Instant?,
)
