import java.util.Properties

/** Maior `kotlin-stdlib` cuja metadata o compilador Kotlin do projeto consegue ler. */
val maxReadableKotlinStdlib = "2.3.21"

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
}

/**
 * Chave da CoinMarketCap: `local.properties` (nao versionado) ou variavel de ambiente.
 * Padrao vazio de proposito -- quem clona o repositorio compila e roda a suite sem
 * credencial; a ausencia da chave so aparece em runtime, como DomainError.Network.
 *
 * Lido dentro de um Provider para nao invalidar o configuration cache.
 */
val cmcApiKey: Provider<String> =
    providers
        .environmentVariable("CMC_API_KEY")
        .orElse(
            providers.of(LocalPropertyValueSource::class) {
                parameters.propertiesFile.set(rootProject.layout.projectDirectory.file("local.properties"))
                parameters.key.set("cmc.api.key")
            },
        ).orElse("")

android {
    namespace = "com.desafiomercadobitcoin"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.desafiomercadobitcoin"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "CMC_API_KEY", "\"${cmcApiKey.get()}\"")
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    buildTypes {
        release {
            // Permite rodar o ColdStartSmokeTest contra o artefato ja ofuscado (G5).
            enableAndroidTestCoverage = false
            testProguardFiles("test-rules.pro")
            // G5: com a otimizacao desligada o release vira uma copia do debug e nenhuma
            // quebra de reflexao e detectavel. As keep rules vivem em src/main/keepRules.
            optimization {
                enable = true
            }
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    lint {
        abortOnError = true
        warningsAsErrors = false
        checkDependencies = true
        // G6: estes deixam de ser aviso e passam a barrar o build.
        error +=
            listOf(
                "HardcodedText",
                "NewApi",
                "InlinedApi",
                "ContentDescription",
                "ClickableViewAccessibility",
                "LabelFor",
                "TouchTargetSizeCheck",
                "ExportedActivity",
                "ExportedService",
                "ExportedReceiver",
                "ExportedContentProvider",
                "MissingApplicationIcon",
                "AllowBackup",
                "UnusedResources",
            )
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

/**
 * Coil 3.5.0 declara `kotlin-stdlib:2.4.0`, e a resolucao por maior versao arrastaria a
 * stdlib inteira para la -- cuja metadata o compilador Kotlin 2.2.10 do projeto nao le
 * ("can read versions up to 2.3.0"). O bytecode do proprio Coil e metadata 2.2.0, entao
 * o conflito e so de versao da stdlib.
 *
 * O teto e a maior stdlib legivel pelo compilador, e nao a versao do plugin: descer ate
 * 2.2.10 tira do classpath classes que as bibliotecas mais novas ja referenciam em runtime.
 */
configurations.configureEach {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:$maxReadableKotlinStdlib")
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor3)
    implementation(libs.koin.android)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.mikepenz.markdown.core)
    implementation(libs.mikepenz.markdown.m3)
    // Coil traz ktor-client-core 3.1.0 transitivamente; declarar aqui alinha o compile
    // classpath a versao que :data ja usa em runtime.
    implementation(libs.ktor.client.core)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.junit)
    testImplementation(libs.koin.test)
    testImplementation(libs.koin.test.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)

    lintChecks(libs.compose.lint.checks)

    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.tracing)
    androidTestImplementation(libs.koin.core)
}

/**
 * ValueSource mantem a leitura do arquivo fora da fase de configuracao rastreada,
 * preservando o configuration cache.
 */
abstract class LocalPropertyValueSource : ValueSource<String, LocalPropertyValueSource.Parameters> {
    interface Parameters : ValueSourceParameters {
        val propertiesFile: RegularFileProperty
        val key: Property<String>
    }

    override fun obtain(): String? {
        val file = parameters.propertiesFile.get().asFile
        if (!file.exists()) return null
        val properties = Properties()
        file.inputStream().use(properties::load)
        return properties.getProperty(parameters.key.get())
    }
}
