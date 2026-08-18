package com.desafiomercadobitcoin.presentation.common

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.desafiomercadobitcoin.R
import com.desafiomercadobitcoin.domain.error.DomainError
import com.desafiomercadobitcoin.domain.error.TextKey
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ResourceProviderTest {
    private val provider =
        AndroidResourceProvider(ApplicationProvider.getApplicationContext<Application>())

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `given every text key when resolving then a non blank text comes back`() {
        val keys =
            listOf(
                TextKey.InvalidInput,
                TextKey.NotFound,
                TextKey.NetworkUnavailable,
                TextKey.UnexpectedResponse,
                TextKey.Unexpected,
            )

        keys.forEach { key ->
            assertFalse("chave sem traducao: $key", provider.resolve(key).isBlank())
        }
    }

    @Test
    fun `given a string resource when resolving it then the localized text comes back`() {
        val text = provider.resolve(R.string.exchange_field_unavailable)

        assertEquals(
            ApplicationProvider
                .getApplicationContext<Application>()
                .getString(R.string.exchange_field_unavailable),
            text,
        )
        assertFalse(text.isBlank())
    }

    @Test
    fun `given a domain error when resolving its key then the user facing text is produced`() {
        val text = provider.resolve(DomainError.Network.textKey)

        assertFalse(text.isBlank())
    }
}
