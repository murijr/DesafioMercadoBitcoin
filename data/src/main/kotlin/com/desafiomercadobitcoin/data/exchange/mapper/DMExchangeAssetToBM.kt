package com.desafiomercadobitcoin.data.exchange.mapper

import com.desafiomercadobitcoin.data.exchange.dto.DMExchangeAsset
import com.desafiomercadobitcoin.domain.exchange.model.BMCurrency

/** Sentido único: transporte → negócio, mesma razão de [toBM] e [toDetailBM]. */
fun DMExchangeAsset.toBM(): BMCurrency =
    BMCurrency(
        name = currency.name,
        priceUsd = currency.priceUsd,
    )
