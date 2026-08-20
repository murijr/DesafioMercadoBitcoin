package com.desafiomercadobitcoin.domain.exchange.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class BMExchangeDetailTest {
    @Test
    fun `given a provider without optional fields when modelling then all are absent`() {
        val detail = baseDetail()

        assertNull(detail.logoUrl)
        assertNull(detail.description)
        assertNull(detail.websiteUrl)
        assertNull(detail.makerFee)
        assertNull(detail.takerFee)
        assertNull(detail.dateLaunched)
    }

    @Test
    fun `given zero fees when modelling then they are distinct from absent fees`() {
        val zero = baseDetail().copy(makerFee = ZERO_FEE, takerFee = ZERO_FEE)
        val absent = baseDetail()

        assertEquals(ZERO_FEE, zero.makerFee)
        assertEquals(ZERO_FEE, zero.takerFee)
        assertNotEquals(zero, absent)
    }

    @Test
    fun `given every field informed when modelling then each one is preserved`() {
        val launch = Instant.parse("2013-07-04T00:00:00.000Z")

        val detail =
            baseDetail().copy(
                logoUrl = "https://logo",
                description = "Uma exchange",
                websiteUrl = "https://exchange.example",
                makerFee = MAKER_FEE,
                takerFee = TAKER_FEE,
                dateLaunched = launch,
            )

        assertEquals(EXCHANGE_ID, detail.id)
        assertEquals("Binance", detail.name)
        assertEquals("https://logo", detail.logoUrl)
        assertEquals("Uma exchange", detail.description)
        assertEquals("https://exchange.example", detail.websiteUrl)
        assertEquals(MAKER_FEE, detail.makerFee)
        assertEquals(TAKER_FEE, detail.takerFee)
        assertEquals(launch, detail.dateLaunched)
    }

    private fun baseDetail() =
        BMExchangeDetail(
            id = EXCHANGE_ID,
            name = "Binance",
            logoUrl = null,
            description = null,
            websiteUrl = null,
            makerFee = null,
            takerFee = null,
            dateLaunched = null,
        )
}

private const val EXCHANGE_ID = 1
private const val ZERO_FEE = 0.0
private const val MAKER_FEE = 0.001
private const val TAKER_FEE = 0.002
