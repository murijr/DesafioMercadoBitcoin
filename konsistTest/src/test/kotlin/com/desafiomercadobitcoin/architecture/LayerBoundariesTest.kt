package com.desafiomercadobitcoin.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

class LayerBoundariesTest {
    @Test
    fun `domain never imports the android sdk`() {
        Konsist
            .scopeFromDirectory("domain/src")
            .imports
            .assertFalse { it.name.startsWith("android.") || it.name.startsWith("androidx.") }
    }

    @Test
    fun `domain never imports the data or app layers`() {
        Konsist
            .scopeFromDirectory("domain/src")
            .imports
            .assertFalse {
                it.name.startsWith("com.desafiomercadobitcoin.data") ||
                    it.name.startsWith("com.desafiomercadobitcoin.presentation") ||
                    it.name.startsWith("com.desafiomercadobitcoin.di")
            }
    }

    @Test
    fun `domain never imports serialization annotations`() {
        Konsist
            .scopeFromDirectory("domain/src")
            .imports
            .assertFalse { it.name.startsWith("kotlinx.serialization") }
    }

    @Test
    fun `data never imports the app layer`() {
        Konsist
            .scopeFromDirectory("data/src")
            .imports
            .assertFalse {
                it.name.startsWith("com.desafiomercadobitcoin.presentation") ||
                    it.name.startsWith("com.desafiomercadobitcoin.di") ||
                    it.name.startsWith("androidx.compose")
            }
    }

    @Test
    fun `presentation never imports the data layer`() {
        Konsist
            .scopeFromDirectory("app/src/main/kotlin/com/desafiomercadobitcoin/presentation")
            .imports
            .assertFalse { it.name.startsWith("com.desafiomercadobitcoin.data") }
    }

    @Test
    fun `only the app dependency injection package may touch the data layer`() {
        Konsist
            .scopeFromDirectory("app/src/main")
            .files
            .filter { file -> file.imports.any { it.name.startsWith("com.desafiomercadobitcoin.data") } }
            .assertTrue { it.packagee?.name?.endsWith(".di") == true || it.name == "DesafioApplication" }
    }
}
