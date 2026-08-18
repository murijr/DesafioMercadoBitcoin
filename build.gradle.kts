// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

subprojects {
    apply(
        plugin =
            rootProject.libs.plugins.ktlint
                .get()
                .pluginId,
    )

    apply(
        plugin =
            rootProject.libs.plugins.detekt
                .get()
                .pluginId,
    )

    configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        // Sem type resolution: Detekt 1.23.x nao acompanha Kotlin 2.2 com tipos (D4).
        config.setFrom(rootProject.file("detekt.yml"))
        buildUponDefaultConfig = true
        ignoreFailures = false
        // Nada de baseline: falha de guardrail se corrige no codigo.
        parallel = true
    }

    // Detekt 1.23.x embute um compilador Kotlin que nao roda sobre JDK 25 (o toolchain
    // do projeto) nem aceita jvmTarget acima de 22. Isolar a tarefa num launcher JDK 17
    // mantem TODAS as regras ativas -- nada aqui enfraquece o guardrail (D4).
    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        jvmTarget = "17"
    }

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        // O estilo vem do .editorconfig na raiz (G4). Nada de regras desabilitadas aqui.
        android.set(false)
        ignoreFailures.set(false)
        reporters {
            reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        }
    }
}
