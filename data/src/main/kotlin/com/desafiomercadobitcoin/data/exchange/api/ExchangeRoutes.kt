package com.desafiomercadobitcoin.data.exchange.api

import io.ktor.resources.Resource
import kotlinx.serialization.SerialName

/**
 * Rotas tipadas do catálogo de corretoras.
 *
 * Vivem fora de `dto/` e `model/` de propósito: são endereço, não modelo de transporte, e o
 * G2 exige prefixo `DM` apenas naqueles dois pacotes.
 */
@Resource("/v1/exchange/map")
class ExchangeMapRoute(
    @SerialName("listing_status") val listingStatus: String = "active",
)

@Resource("/v1/exchange/info")
class ExchangeInfoRoute(
    val id: String,
)
