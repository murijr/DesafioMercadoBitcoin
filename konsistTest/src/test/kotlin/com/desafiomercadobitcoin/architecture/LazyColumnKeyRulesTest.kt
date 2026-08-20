package com.desafiomercadobitcoin.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

class LazyColumnKeyRulesTest {
    @Test
    fun `lazy list items in app must declare a key`() {
        Konsist
            .scopeFromDirectory("app/src/main")
            .functions()
            .filter { function ->
                function.text.contains("items(") || function.text.contains("itemsIndexed(")
            }.assertTrue { it.text.contains("key =") }
    }

    @Test
    fun `lazy list keys must be a primary key or the index`() {
        val allowedKey =
            Regex(
                """key\s*=\s*\{[^}]*(?:\b(?:index|i)\b|\.id\b|\.[A-Za-z][a-zA-Z0-9_]*[Ii]d\b)""",
            )

        Konsist
            .scopeFromDirectory("app/src/main")
            .functions()
            .filter { function ->
                function.text.contains("items(") || function.text.contains("itemsIndexed(")
            }.assertTrue { function ->
                val text = function.text
                if (!text.contains("key =")) return@assertTrue true
                val keyBlock = Regex("""key\s*=\s*\{[^}]*\}""").find(text)?.value.orEmpty()
                allowedKey.containsMatchIn(keyBlock)
            }
    }
}
