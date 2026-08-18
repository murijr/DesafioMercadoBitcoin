import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.parcelize)
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

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.koin.android)
    implementation(libs.koin.compose.viewmodel)

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
