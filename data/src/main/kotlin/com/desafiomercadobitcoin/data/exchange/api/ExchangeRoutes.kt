package com.desafiomercadobitcoin.data.exchange.api

import io.ktor.resources.Resource
import kotlinx.serialization.SerialName

@Resource("/v1/exchange/map")
class ExchangeMapRoute(
    @SerialName("listing_status") val listingStatus: String = "active",
)

@Resource("/v1/exchange/info")
class ExchangeInfoRoute(
    val id: String,
)
