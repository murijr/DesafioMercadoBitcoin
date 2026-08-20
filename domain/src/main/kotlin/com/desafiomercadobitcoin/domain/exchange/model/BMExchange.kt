package com.desafiomercadobitcoin.domain.exchange.model

import java.time.Instant

data class BMExchange(
    val id: Int,
    val name: String,
    val logoUrl: String?,
    val spotVolumeUsd: Double?,
    val dateLaunched: Instant?,
)
