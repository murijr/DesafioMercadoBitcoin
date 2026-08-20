package com.desafiomercadobitcoin

import android.os.StrictMode
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DesafioApplicationTest {
    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `given the debug application when it starts then the strict thread policy is installed`() {
        val expected =
            StrictMode.ThreadPolicy
                .Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .penaltyDeath()
                .build()

        assertEquals(expected.toString(), StrictMode.getThreadPolicy().toString())
    }
}
