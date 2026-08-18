package com.desafiomercadobitcoin.data.exchange.mapper

import com.desafiomercadobitcoin.data.exchange.dto.DMExchangeInfo
import com.desafiomercadobitcoin.data.exchange.dto.DMExchangeUrls
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class DMExchangeInfoToDetailBMTest {
    @Test
    fun `given every field present when mapping then all are preserved`() {
        val dm =
            DMExchangeInfo(
                id = BINANCE_ID,
                name = "Binance",
                logo = "https://logo",
                description = "Uma exchange",
                urls = DMExchangeUrls(website = listOf("https://binance.com", "https://binance.com/alt")),
                dateLaunched = "2017-07-14T00:00:00.000Z",
                spotVolumeUsd = SPOT_VOLUME_USD,
                makerFee = MAKER_FEE,
                takerFee = TAKER_FEE,
            )

        val bm = dm.toDetailBM()

        assertEquals(BINANCE_ID, bm.id)
        assertEquals("Binance", bm.name)
        assertEquals("https://logo", bm.logoUrl)
        assertEquals("Uma exchange", bm.description)
        assertEquals("https://binance.com", bm.websiteUrl)
        assertEquals(Instant.parse("2017-07-14T00:00:00.000Z"), bm.dateLaunched)
        assertEquals(MAKER_FEE, bm.makerFee)
        assertEquals(TAKER_FEE, bm.takerFee)
    }

    @Test
    fun `given every optional field absent when mapping then all come back null`() {
        val dm =
            DMExchangeInfo(
                id = MERCADO_BITCOIN_ID,
                name = "Mercado Bitcoin",
                logo = null,
                description = null,
                urls = null,
                dateLaunched = null,
                spotVolumeUsd = null,
                makerFee = null,
                takerFee = null,
            )

        val bm = dm.toDetailBM()

        assertNull(bm.logoUrl)
        assertNull(bm.description)
        assertNull(bm.websiteUrl)
        assertNull(bm.dateLaunched)
        assertNull(bm.makerFee)
        assertNull(bm.takerFee)
    }

    @Test
    fun `given an empty website list when mapping then the website url is null`() {
        val dm =
            DMExchangeInfo(
                id = 1,
                name = "Any",
                urls = DMExchangeUrls(website = emptyList()),
            )

        val bm = dm.toDetailBM()

        assertNull(bm.websiteUrl)
    }
}

private const val BINANCE_ID = 270
private const val MERCADO_BITCOIN_ID = 302
private const val SPOT_VOLUME_USD = 123.0
private const val MAKER_FEE = 0.02
private const val TAKER_FEE = 0.04
