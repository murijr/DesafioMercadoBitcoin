package com.desafiomercadobitcoin.data.exchange.mapper

import com.desafiomercadobitcoin.data.exchange.dto.DMCurrency
import com.desafiomercadobitcoin.data.exchange.dto.DMExchangeAsset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DMExchangeAssetToBMTest {
    @Test
    fun `given a currency with price when mapping then it is preserved`() {
        val dm = DMExchangeAsset(currency = DMCurrency(name = "Bitcoin", priceUsd = PRICE_USD))

        val bm = dm.toBM()

        assertEquals("Bitcoin", bm.name)
        assertEquals(PRICE_USD, bm.priceUsd)
    }

    @Test
    fun `given a currency without price when mapping then it comes back null`() {
        val dm = DMExchangeAsset(currency = DMCurrency(name = "Ethereum", priceUsd = null))

        val bm = dm.toBM()

        assertNull(bm.priceUsd)
    }
}

private const val PRICE_USD = 45_000.0
