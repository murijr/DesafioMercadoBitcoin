package com.desafiomercadobitcoin.data.exchange.mapper

import com.desafiomercadobitcoin.data.exchange.dto.DMExchangeAsset
import com.desafiomercadobitcoin.domain.exchange.model.BMCurrency

fun DMExchangeAsset.toBM(): BMCurrency =
    BMCurrency(
        name = currency.name,
        priceUsd = currency.priceUsd,
    )
