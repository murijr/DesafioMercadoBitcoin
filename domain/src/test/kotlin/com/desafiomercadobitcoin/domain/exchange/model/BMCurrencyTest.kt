package com.desafiomercadobitcoin.domain.exchange.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BMCurrencyTest {
    @Test
    fun `given a provider without price when modelling then it is absent`() {
        val currency = BMCurrency(name = "Bitcoin", priceUsd = null)

        assertNull(currency.priceUsd)
    }

    @Test
    fun `given a zero price when modelling then it is distinct from an absent price`() {
        val zero = BMCurrency(name = "Bitcoin", priceUsd = 0.0)
        val absent = BMCurrency(name = "Bitcoin", priceUsd = null)

        assertEquals(0.0, zero.priceUsd)
        assertNotEquals(zero, absent)
    }
}
