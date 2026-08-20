package com.desafiomercadobitcoin.data.exchange.api

import io.ktor.resources.Resource

@Resource("/v1/exchange/assets")
class ExchangeAssetsRoute(
    val id: String,
)
