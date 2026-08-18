package com.desafiomercadobitcoin.presentation.feature.exchangedetail.mapper

import com.desafiomercadobitcoin.R
import com.desafiomercadobitcoin.domain.exchange.model.BMCurrency
import com.desafiomercadobitcoin.presentation.common.ResourceProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.text.NumberFormat
import java.util.Locale

class BMCurrencyToVMTest {
    private val resources =
        mockk<ResourceProvider> {
            every { resolve(R.string.exchange_field_unavailable) } returns UNAVAILABLE
        }

    @Test
    fun `given a price when mapping then it is formatted as full usd currency`() {
        val vm = BMCurrency(name = "Bitcoin", priceUsd = PRICE_USD).toVM(resources)

        assertEquals("Bitcoin", vm.name)
        assertEquals(
            NumberFormat.getCurrencyInstance(Locale.US).format(PRICE_USD),
            vm.priceLabel,
        )
    }

    @Test
    fun `given a zero price when mapping then it is formatted as zero and not as unavailable`() {
        val vm = BMCurrency(name = "Bitcoin", priceUsd = 0.0).toVM(resources)

        assertNotEquals(UNAVAILABLE, vm.priceLabel)
        assertEquals(NumberFormat.getCurrencyInstance(Locale.US).format(0.0), vm.priceLabel)
    }

    @Test
    fun `given no price when mapping then the unavailable text takes its place`() {
        val vm = BMCurrency(name = "Bitcoin", priceUsd = null).toVM(resources)

        assertEquals(UNAVAILABLE, vm.priceLabel)
    }
}

private const val UNAVAILABLE = "unavailable"
private const val PRICE_USD = 45_000.12
