package com.desafiomercadobitcoin.data.exchange.mapper

import com.desafiomercadobitcoin.data.exchange.dto.DMExchangeInfo
import com.desafiomercadobitcoin.domain.exchange.model.BMExchange
import java.time.Instant

fun DMExchangeInfo.toBM(): BMExchange =
    BMExchange(
        id = id,
        name = name,
        logoUrl = logo,
        spotVolumeUsd = spotVolumeUsd,
        dateLaunched = dateLaunched?.let(Instant::parse),
    )
