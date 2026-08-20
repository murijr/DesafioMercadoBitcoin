package com.desafiomercadobitcoin.data.exchange.mapper

import com.desafiomercadobitcoin.data.exchange.dto.DMExchangeInfo
import com.desafiomercadobitcoin.domain.exchange.model.BMExchangeDetail
import java.time.Instant

fun DMExchangeInfo.toDetailBM(): BMExchangeDetail =
    BMExchangeDetail(
        id = id,
        name = name,
        logoUrl = logo,
        description = description,
        websiteUrl = urls?.website?.firstOrNull(),
        makerFee = makerFee,
        takerFee = takerFee,
        dateLaunched = dateLaunched?.let(Instant::parse),
    )
