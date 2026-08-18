package com.desafiomercadobitcoin.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

/**
 * G2 — prefixos de modelo (`BM`/`DM`/`VM`) e sufixos de papel.
 * A fronteira fica visivel na assinatura: o tipo ja diz em que camada o objeto vive.
 */
class NamingConventionsTest {
    @Test
    fun `business models live in domain and carry the BM prefix`() {
        Konsist
            .scopeFromDirectory("domain/src/main")
            .classes()
            .filter { it.resideInPackage("..model..") }
            .assertTrue { it.name.startsWith("BM") }
    }

    @Test
    fun `business models carry no framework annotation`() {
        Konsist
            .scopeFromProject()
            .classes()
            .filter { it.name.startsWith("BM") }
            .assertFalse { klass ->
                klass.annotations.any { annotation ->
                    annotation.name in listOf("Serializable", "Parcelize", "Entity")
                }
            }
    }

    @Test
    fun `data models live in data and carry the DM prefix`() {
        Konsist
            .scopeFromDirectory("data/src/main")
            .classes()
            .filter { it.resideInPackage("..dto..") || it.resideInPackage("..model..") }
            .assertTrue { it.name.startsWith("DM") }
    }

    @Test
    fun `data models never leak outside the data layer`() {
        Konsist
            .scopeFromProject()
            .files
            .filter { !it.path.contains("/data/src/") && !it.path.contains("/konsistTest/") }
            .assertFalse { file ->
                file.imports.any { it.name.substringAfterLast('.').startsWith("DM") }
            }
    }

    @Test
    fun `presentation models never leak outside the app layer`() {
        Konsist
            .scopeFromProject()
            .files
            .filter { !it.path.contains("/app/src/") && !it.path.contains("/konsistTest/") }
            .assertFalse { file ->
                file.imports.any { it.name.substringAfterLast('.').startsWith("VM") }
            }
    }

    @Test
    fun `repository interfaces are declared in domain with the Repository suffix`() {
        Konsist
            .scopeFromDirectory("domain/src/main")
            .interfaces()
            .filter { it.name.endsWith("Repository") }
            .assertTrue { it.resideInPackage("com.desafiomercadobitcoin.domain..") }
    }

    @Test
    fun `repository implementations live in data and end with RepositoryImpl`() {
        Konsist
            .scopeFromProject()
            .classes()
            .filter { it.path.contains("/src/main/") }
            .filter { klass ->
                klass.parents().any { it.name.substringBefore('<').endsWith("Repository") }
            }.assertTrue { it.name.endsWith("RepositoryImpl") && it.path.contains("/data/src/") }
    }

    @Test
    fun `use cases end with the UseCase suffix and live in domain`() {
        Konsist
            .scopeFromProject()
            .classes()
            .filter { it.path.contains("/src/main/") }
            .filter { klass ->
                klass.parents().any { it.name.substringBefore('<') == "UseCase" }
            }.assertTrue { it.name.endsWith("UseCase") && it.path.contains("/domain/src/main/") }
    }
}
