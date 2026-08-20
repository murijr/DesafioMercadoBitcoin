package com.desafiomercadobitcoin.domain.exchange.model

import java.time.Instant

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
