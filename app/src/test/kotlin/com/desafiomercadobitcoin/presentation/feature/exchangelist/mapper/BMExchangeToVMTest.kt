package com.desafiomercadobitcoin.presentation.feature.exchangelist.mapper

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.desafiomercadobitcoin.R
import com.desafiomercadobitcoin.domain.exchange.model.BMExchange
import com.desafiomercadobitcoin.presentation.common.AndroidResourceProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
class BMExchangeToVMTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val resources = AndroidResourceProvider(context)
    private val unavailable = context.getString(R.string.exchange_field_unavailable)
    private val defaultTimeZone = TimeZone.getDefault()

    /**
     * Fuso a oeste de Greenwich: se a data fosse lida no fuso do dispositivo em vez de UTC,
     * a meia-noite UTC do provedor viraria o dia anterior.
     */
    @Before
    fun useTimeZoneWestOfGreenwich() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(defaultTimeZone)
        stopKoin()
    }

    @Test
    fun `given a volume when mapping then it is formatted as compact usd`() {
        val vm = exchange(spotVolumeUsd = 5_234_567_890.12).toVM(resources)

        assertNotEquals(unavailable, vm.volumeLabel)
        assertTrue(vm.volumeLabel, vm.volumeLabel.contains("5"))
        assertTrue(vm.volumeLabel, vm.volumeLabel.any { it.isLetter() })
    }

    @Test
    fun `given a zero volume when mapping then it is formatted as zero and not as unavailable`() {
        val vm = exchange(spotVolumeUsd = 0.0).toVM(resources)

        assertNotEquals(unavailable, vm.volumeLabel)
        assertTrue(vm.volumeLabel, vm.volumeLabel.contains("0"))
    }

    @Test
    fun `given no volume when mapping then the unavailable text takes its place`() {
        val vm = exchange(spotVolumeUsd = null).toVM(resources)

        assertEquals(unavailable, vm.volumeLabel)
    }

    @Test
    fun `given no launch date when mapping then the unavailable text takes its place`() {
        val vm = exchange(dateLaunched = null).toVM(resources)

        assertEquals(unavailable, vm.launchDateLabel)
    }

    @Test
    fun `given a launch date at utc midnight when mapping then the utc day is displayed`() {
        val vm = exchange(dateLaunched = Instant.parse("2017-07-14T00:00:00.000Z")).toVM(resources)

        val expected =
            DateTimeFormatter
                .ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(Locale.getDefault())
                .format(LAUNCH_DAY_AT_UTC)
        assertEquals(expected, vm.launchDateLabel)
    }

    @Test
    fun `given a launch date when mapping then it is not shifted by the device time zone`() {
        val instant = Instant.parse("2017-07-14T00:00:00.000Z")

        val vm = exchange(dateLaunched = instant).toVM(resources)

        val shifted =
            DateTimeFormatter
                .ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(Locale.getDefault())
                .format(LocalDate.ofInstant(instant, TimeZone.getDefault().toZoneId()))
        assertNotEquals(shifted, vm.launchDateLabel)
        assertEquals(LAUNCH_DAY_AT_UTC, LocalDate.ofInstant(instant, ZoneOffset.UTC))
    }

    @Test
    fun `given identification fields when mapping then they cross unchanged`() {
        val vm = exchange().toVM(resources)

        assertEquals(BINANCE_ID, vm.id)
        assertEquals("Binance", vm.name)
        assertEquals("https://logo/$BINANCE_ID.png", vm.logoUrl)
    }

    private fun exchange(
        spotVolumeUsd: Double? = 1.0,
        dateLaunched: Instant? = Instant.parse("2017-07-14T00:00:00.000Z"),
    ) = BMExchange(
        id = BINANCE_ID,
        name = "Binance",
        logoUrl = "https://logo/$BINANCE_ID.png",
        spotVolumeUsd = spotVolumeUsd,
        dateLaunched = dateLaunched,
    )
}

private const val BINANCE_ID = 270

/** A meia-noite UTC de `LAUNCH_INSTANT` cai neste dia, e nao no anterior. */
private val LAUNCH_DAY_AT_UTC: LocalDate = LocalDate.parse("2017-07-14")
