plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
}

// Modulo de arquitetura (G2). Nao depende de :app/:data/:domain de proposito -- o Konsist
// varre o repositorio pelo sistema de arquivos, entao o julgamento nao acopla ao julgado
// e a suite roda em JVM pura, em segundos.
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.konsist)
}

tasks.withType<Test>().configureEach {
    useJUnit()
}
