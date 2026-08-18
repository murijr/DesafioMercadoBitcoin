package com.desafiomercadobitcoin.presentation.feature.exchangedetail.mapper

import com.desafiomercadobitcoin.R
import com.desafiomercadobitcoin.domain.exchange.model.BMExchangeDetail
import com.desafiomercadobitcoin.presentation.common.ResourceProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class BMExchangeDetailToVMTest {
    private val resources =
        mockk<ResourceProvider> {
            every { resolve(R.string.exchange_field_unavailable) } returns UNAVAILABLE
        }

    @Test
    fun `given every field present when mapping then all labels are formatted`() {
        val launch = Instant.parse("2017-07-14T00:00:00.000Z")
        val bm =
            baseDetail().copy(
                description = "Uma exchange",
                websiteUrl = "https://exchange.example",
                makerFee = MAKER_FEE,
                takerFee = TAKER_FEE,
                dateLaunched = launch,
            )

        val vm = bm.toVM(resources)

        assertEquals(bm.id, vm.id)
        assertEquals(bm.name, vm.name)
        assertEquals("Uma exchange", vm.descriptionLabel)
        assertEquals("https://exchange.example", vm.websiteLabel)
        assertEquals(expectedPercent(MAKER_FEE), vm.makerFeeLabel)
        assertEquals(expectedPercent(TAKER_FEE), vm.takerFeeLabel)
        assertEquals(expectedDate(launch), vm.launchDateLabel)
    }

    @Test
    fun `given every optional field absent when mapping then all labels are unavailable`() {
        val vm = baseDetail().toVM(resources)

        assertEquals(UNAVAILABLE, vm.descriptionLabel)
        assertEquals(UNAVAILABLE, vm.websiteLabel)
        assertEquals(UNAVAILABLE, vm.makerFeeLabel)
        assertEquals(UNAVAILABLE, vm.takerFeeLabel)
        assertEquals(UNAVAILABLE, vm.launchDateLabel)
    }

    @Test
    fun `given zero fees when mapping then they are formatted as zero and not as unavailable`() {
        val bm = baseDetail().copy(makerFee = ZERO_FEE, takerFee = ZERO_FEE)

        val vm = bm.toVM(resources)

        assertEquals(expectedPercent(ZERO_FEE), vm.makerFeeLabel)
        assertEquals(expectedPercent(ZERO_FEE), vm.takerFeeLabel)
    }

    private fun expectedPercent(value: Double): String =
        NumberFormat.getPercentInstance(Locale.getDefault()).format(value)

    private fun expectedDate(instant: Instant): String {
        val date = LocalDate.ofInstant(instant, ZoneOffset.UTC)
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()).format(date)
    }

    private fun baseDetail() =
        BMExchangeDetail(
            id = EXCHANGE_ID,
            name = "Binance",
            logoUrl = "https://logo",
            description = null,
            websiteUrl = null,
            makerFee = null,
            takerFee = null,
            dateLaunched = null,
        )
}

private const val UNAVAILABLE = "unavailable"
private const val EXCHANGE_ID = 270
private const val MAKER_FEE = 0.02
private const val TAKER_FEE = 0.04
private const val ZERO_FEE = 0.0
