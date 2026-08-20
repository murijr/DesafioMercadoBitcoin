package com.desafiomercadobitcoin

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.core.error.NoDefinitionFoundException

@RunWith(AndroidJUnit4::class)
class ColdStartSmokeTest {
    @Test
    fun theApplicationStartsItsDependencyGraph() {
        InstrumentationRegistry.getInstrumentation().targetContext

        val koin = requireNotNull(GlobalContext.getKoinApplicationOrNull()?.koin)

        assertNotNull(koin)
    }

    @Test
    fun theResourceProviderSurvivesObfuscationAndResolvesEveryTextKey() {
        val koin = requireNotNull(GlobalContext.getKoinApplicationOrNull()?.koin)

        val provider =
            try {
                koin.get<com.desafiomercadobitcoin.presentation.common.ResourceProvider>()
            } catch (error: NoDefinitionFoundException) {
                throw AssertionError("keep rule ausente: o Koin nao resolveu ResourceProvider", error)
            }

        val texts =
            listOf(
                com.desafiomercadobitcoin.domain.error.TextKey.InvalidInput,
                com.desafiomercadobitcoin.domain.error.TextKey.NotFound,
                com.desafiomercadobitcoin.domain.error.TextKey.NetworkUnavailable,
                com.desafiomercadobitcoin.domain.error.TextKey.UnexpectedResponse,
                com.desafiomercadobitcoin.domain.error.TextKey.Unexpected,
            ).map(provider::resolve)

        assertEquals(texts.size, texts.count { it.isNotBlank() })
    }

    @Test
    fun theLauncherActivityReachesTheResumedState() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            assertNotNull(scenario.state)
        }
    }
}
