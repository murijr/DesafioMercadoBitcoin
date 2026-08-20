package com.desafiomercadobitcoin.domain.exchange.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class BMExchangeTest {
    @Test
    fun `given a provider without volume and launch date when modelling then both are absent`() {
        val exchange = exchange(spotVolumeUsd = null, dateLaunched = null)

        assertNull(exchange.spotVolumeUsd)
        assertNull(exchange.dateLaunched)
    }

    @Test
    fun `given a zero volume when modelling then it is distinct from an absent volume`() {
        val zero = exchange(spotVolumeUsd = 0.0)
        val absent = exchange(spotVolumeUsd = null)

        assertEquals(0.0, zero.spotVolumeUsd)
        assertNotEquals(zero, absent)
    }

    @Test
    fun `given every field informed when modelling then each one is preserved`() {
        val launch = Instant.parse("2013-07-04T00:00:00.000Z")

        val exchange = exchange(spotVolumeUsd = SOME_VOLUME, dateLaunched = launch)

        assertEquals(1, exchange.id)
        assertEquals("Binance", exchange.name)
        assertEquals("https://logo", exchange.logoUrl)
        assertEquals(SOME_VOLUME, exchange.spotVolumeUsd)
        assertEquals(launch, exchange.dateLaunched)
    }

    private fun exchange(
        spotVolumeUsd: Double? = null,
        dateLaunched: Instant? = null,
    ) = BMExchange(
        id = 1,
        name = "Binance",
        logoUrl = "https://logo",
        spotVolumeUsd = spotVolumeUsd,
        dateLaunched = dateLaunched,
    )
}

private const val SOME_VOLUME = 1_234.5
