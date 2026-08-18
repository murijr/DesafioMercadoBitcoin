package com.desafiomercadobitcoin.data.exchange.mapper

import com.desafiomercadobitcoin.data.exchange.dto.DMExchangeInfo
import com.desafiomercadobitcoin.domain.exchange.model.BMExchange
import java.time.Instant

/**
 * Sentido único: transporte → negócio. O nome é `toBM()` e não `to()` porque o Detekt exige
 * nome de função com ao menos três caracteres — a mesma colisão que o `app/AGENTS.md` já
 * resolve com `toVM()` do lado da apresentação.
 *
 * `date_launched` chega como ISO-8601 em UTC; ausente continua ausente, porque o domínio
 * descreve o fato e não a sua apresentação.
 */
fun DMExchangeInfo.toBM(): BMExchange =
    BMExchange(
        id = id,
        name = name,
        logoUrl = logo,
        spotVolumeUsd = spotVolumeUsd,
        dateLaunched = dateLaunched?.let(Instant::parse),
    )
