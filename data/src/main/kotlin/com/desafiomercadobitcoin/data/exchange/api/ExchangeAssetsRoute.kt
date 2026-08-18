package com.desafiomercadobitcoin.data.exchange.api

import io.ktor.resources.Resource

/**
 * Rota tipada das moedas negociadas por uma corretora. Fora de `dto/` e `model/` pela mesma
 * razão de [ExchangeInfoRoute].
 */
@Resource("/v1/exchange/assets")
class ExchangeAssetsRoute(
    val id: String,
)
