package com.desafiomercadobitcoin.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

class PresentationRulesTest {
    @Test
    fun `view models only depend on use cases and the resource provider`() {
        Konsist
            .scopeFromProject()
            .classes()
            .filter { it.path.contains("/src/main/") }
            .filter { it.name.endsWith("ViewModel") }
            .flatMap { it.primaryConstructor?.parameters.orEmpty() }
            .assertTrue { parameter ->
                val type = parameter.type.name.substringBefore('<')
                type.endsWith("UseCase") ||
                    type == "ResourceProvider" ||
                    type == "SavedStateHandle"
            }
    }

    @Test
    fun `use cases never receive a coroutine scope`() {
        Konsist
            .scopeFromProject()
            .classes()
            .filter { it.path.contains("/src/main/") }
            .filter { it.name.endsWith("UseCase") }
            .flatMap { it.primaryConstructor?.parameters.orEmpty() }
            .assertFalse { it.type.name.contains("CoroutineScope") }
    }

    @Test
    fun `composables never render a hardcoded string`() {
        val literalArgument = Regex("""\b(text|contentDescription|label|placeholder)\s*=\s*"""")

        Konsist
            .scopeFromDirectory("app/src/main")
            .functions()
            .filter { function -> function.annotations.any { it.name == "Composable" } }
            .assertFalse { it.text.contains(literalArgument) }
    }
}
