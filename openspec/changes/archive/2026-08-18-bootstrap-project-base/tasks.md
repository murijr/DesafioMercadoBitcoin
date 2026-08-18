/## 1. Limpeza e topologia de módulos

- [x] 1.1 Apagar `konsistTest/build/` e `konsistTest/tmp/` (resíduo compilado de tentativa anterior, sem fontes) e confirmar que `konsistTest/src/` está vazio
- [x] 1.2 Remover `app/src/test/.../ExampleUnitTest.kt`, `app/src/androidTest/.../ExampleInstrumentedTest.kt`, `data/src/test/.../ExampleUnitTest.kt` e `data/src/androidTest/.../ExampleInstrumentedTest.kt`
- [x] 1.3 Remover `androidx.appcompat` e `com.google.android.material` das dependências de `:app` e `:data` (stack de *Views*, contrária ao stack fixado)
- [x] 1.4 Declarar `:data → :domain` em `data/build.gradle.kts` e `:app → :data`, `:app → :domain` em `app/build.gradle.kts`
- [x] 1.5 Elevar `sourceCompatibility`/`targetCompatibility` para 17 nos três módulos e `jvmTarget` para 17 em `:domain` (D6)
- [x] 1.6 Rodar `./gradlew :app:assembleDebug` e confirmar que a base ainda compila com o *configuration cache* ligado

## 2. Catálogo de versões

- [x] 2.1 Popular `[versions]`/`[libraries]`/`[plugins]` de `gradle/libs.versions.toml` com o conjunto de D1/Context: Compose BOM 2026.08.00, Material 3, `activity-compose` 1.13.0, lifecycle 2.11.0, Koin 4.2.2, Ktor 3.5.2, `kotlinx-serialization-json` 1.11.0, coroutines + `kotlinx-coroutines-test` 1.11.0
- [x] 2.2 Adicionar ao catálogo o ferramental de qualidade e teste: Detekt 1.23.8, plugin KtLint, Konsist 0.17.3, `compose-lint-checks` 1.5.4, JUnit 4, MockK 1.14.11, Robolectric 4.16.1, Compose UI Test
- [x] 2.3 Adicionar os plugins `kotlin.plugin.serialization` e `kotlin-parcelize` ao catálogo
- [x] 2.4 Confirmar `./gradlew help` verde — catálogo sintaticamente válido e sem alias duplicado

## 3. G4 — KtLint

- [x] 3.1 Aplicar o plugin KtLint no `build.gradle.kts` da raiz para todos os subprojetos, com `ktlintFormat` disponível apenas localmente e `ktlintCheck` como a tarefa de verificação
- [x] 3.2 Criar `.editorconfig` na raiz com `ktlint_code_style = ktlint_official` e `ktlint_function_naming_ignore_when_annotated_with = Composable` (D5)
- [x] 3.3 **Prova de reprovação:** introduzir temporariamente um arquivo Kotlin mal formatado, confirmar que `./gradlew ktlintCheck` falha nomeando arquivo/linha/regra, e remover o arquivo
- [x] 3.4 Confirmar `./gradlew ktlintCheck` verde na base

## 4. G3 — Detekt

- [x] 4.1 Aplicar o plugin Detekt na raiz para todos os subprojetos, sem *type resolution* (D4)
- [x] 4.2 Gerar `detekt.yml` na raiz a partir do *default config* e ajustar: limites de complexidade/LOC/funções por classe, `FunctionMinLength`, `WildcardImport` ativo, `FunctionNaming.ignoreAnnotated: ['Composable']`
- [x] 4.3 Configurar `SwallowedException`/`TooGenericExceptionCaught` para reprovar `catch (Throwable)` que engole `CancellationException` (D4)
- [x] 4.4 **Prova de reprovação:** introduzir temporariamente um *wildcard import* e um `try/catch (Throwable)` que engole cancelamento; confirmar que `./gradlew detekt` falha em ambos; remover
- [x] 4.5 Confirmar `./gradlew detekt` verde na base, sem `baseline.xml` no repositório

## 5. `:domain` — bases e testes (spec `domain-foundations`)

- [x] 5.1 Adicionar a `:domain` as dependências de coroutines e o ferramental de teste JVM (JUnit 4, MockK, `coroutines-test`)
- [x] 5.2 Escrever a seção de convenção de testes (Gherkin + `Enclosed` + `HappyPath`/`ErrorPath`/`EdgeCases` sobre `TestSetup`) no `AGENTS.md` da raiz — os três documentos de módulo já remetem a ela (D9.2). Esta tarefa vem **antes** do primeiro teste
- [x] 5.3 Escrever os testes vermelhos de `UseCase`: caminho feliz, exceção arbitrária vira `DomainError`, `DomainError` já tipado passa sem remapeamento, `CancellationException` re-lançada, entrada inválida não toca o repositório
- [x] 5.4 Implementar `TextKey` (*sealed*) e `DomainError` (*sealed*, com no mínimo `Validation`, `NotFound`, `Network`, `Serialization` e um genérico), cada subtipo carregando sua `TextKey`
- [x] 5.5 Implementar `UseCase<I, S>` conforme o contrato de `domain/AGENTS.md` e deixar 5.3 verde
- [x] 5.6 Escrever os testes vermelhos e implementar `Throwable.toDomainError()`: IO/HTTP → `Network`, desserialização → `Serialization`, desconhecida → genérico, sem lançar
- [x] 5.7 Confirmar `./gradlew :domain:test` verde e conferir que as asserções comparam subtipo de erro, não texto de mensagem

## 6. `:data` — fundação de rede (spec `data-network-foundation`)

- [x] 6.1 Aplicar `kotlin.plugin.serialization` em `:data` e adicionar Ktor (core, engine `Android`, `ContentNegotiation`, `Logging`, `HttpTimeout`, `Resources`), `kotlinx-serialization-json` e Koin
- [x] 6.2 Adicionar a `:data` o ferramental de teste com Robolectric, MockK e `coroutines-test`; fixar a API do Robolectric via `robolectric.properties` se o SDK 37 não for suportado (risco registrado no design)
- [x] 6.3 Implementar `HttpClientFactory` em `data/network/`: ponto único de construção, `Json` com `ignoreUnknownKeys`, `HttpTimeout`, `Logging` só em *debug*, e o cabeçalho `X-CMC_PRO_API_KEY` alimentado por um valor injetado (D3)
- [x] 6.4 Criar o módulo Koin de `:data` em `data/di/`, parametrizado pela chave de API, expondo `HttpClient` com *bindings* explícitos
- [x] 6.5 Escrever os testes: cabeçalho presente na requisição, campo desconhecido ignorado na desserialização, campo obrigatório ausente vira `DomainError.Serialization`, falha de IO vira `DomainError.Network`, cancelamento **não** vira `DomainError`
- [x] 6.6 Confirmar `./gradlew :data:testDebugUnitTest` verde

## 7. `:app` — casca do aplicativo (spec `app-shell`)

- [x] 7.1 **Primeiro:** habilitar `buildFeatures { compose = true }` em `:app`, subir um Composable trivial e compilar; se falhar, aplicar `org.jetbrains.kotlin.plugin.compose` explicitamente (risco registrado no design). Nada mais em `:app` antes disso resolver
- [x] 7.2 Aplicar `kotlin-parcelize` em `:app` e adicionar Compose BOM, Material 3, `activity-compose`, lifecycle, Koin (`koin-android` + `koin-compose-viewmodel`) e o ferramental de teste (Robolectric, Compose UI Test, MockK)
- [x] 7.3 Ler `CMC_API_KEY` de `local.properties`/variável de ambiente com padrão vazio e publicá-la em `BuildConfig`, usando `Provider` para não invalidar o *configuration cache* (D3 + risco registrado)
- [x] 7.4 Substituir `themes.xml`/`values-night/themes.xml`/`colors.xml` pelo tema Compose Material 3 em `presentation/theme/`, com paleta clara e escura
- [x] 7.5 Implementar `ResourceProvider` em `presentation/common/` resolvendo `TextKey` → `R.string.*` com `when` exaustivo, e criar os recursos de texto correspondentes em `strings.xml`
- [x] 7.6 Implementar a `Application` que inicia o Koin agregando os módulos de `:data` e `:app`, passando a chave de `BuildConfig`
- [x] 7.7 Criar `MainActivity` com `setContent`, declará-la no `AndroidManifest.xml` com `android:exported="true"` e `LAUNCHER`, e remover os atributos de tema de *Views* obsoletos
- [x] 7.8 Escrever o teste de verificação do grafo Koin (toda definição resolvível) e um teste de `ResourceProvider` cobrindo cada `TextKey`
- [x] 7.9 Escrever um teste de Compose com `createComposeRule()` sob Robolectric renderizando a tela inicial, provando que UI Compose roda em JVM
- [x] 7.10 Confirmar `./gradlew :app:testDebugUnitTest` verde e o aplicativo iniciando em claro e em escuro

## 8. G6 — Android Lint + regras de Composable

- [x] 8.1 Configurar o bloco `lint` de `:app` com `abortOnError = true` e severidade de erro para `HardcodedText`, `NewApi`, acessibilidade e problemas de manifesto
- [x] 8.2 Adicionar `compose-lint-checks` como `lintChecks` de `:app`
- [x] 8.3 **Prova de reprovação:** introduzir temporariamente um Composable com `Modifier` fora da posição convencional e um recurso órfão; confirmar que `./gradlew :app:lintDebug` falha em ambos; remover. (O literal em `Composable` não é alcançável pelo lint — a prova dele vive na 9.6, via G2.)
- [x] 8.4 Confirmar `./gradlew :app:lintDebug` verde, sem `lint-baseline.xml` no repositório

## 9. G2 — Konsist

- [x] 9.1 Registrar `:konsistTest` no `settings.gradle.kts` e criar `konsistTest/build.gradle.kts` como módulo `kotlin("jvm")` sem dependência dos demais módulos (D2)
- [x] 9.2 Escrever os *asserts* de grafo: `:domain` sem import de Android/`:data`/`:app`; `:data` sem import de `:app`; `presentation/` sem import de `:data`
- [x] 9.3 Escrever os *asserts* de nomenclatura: prefixos `BM`/`DM`/`VM` por localização, `BM` sem anotação de framework, `DM` não referenciado fora de `:data`, `VM` não referenciado fora de `:app`
- [x] 9.4 Escrever os *asserts* de sufixo: interface de repositório `*Repository`, implementação `*RepositoryImpl`, caso de uso `*UseCase`
- [x] 9.5 Escrever o *assert* "construtor de `ViewModel` recebe apenas `*UseCase` ou `ResourceProvider`" e o *assert* "caso de uso não recebe `CoroutineScope`"
- [x] 9.6 **Prova de reprovação:** para cada grupo (9.2–9.5) introduzir temporariamente uma violação, confirmar que `./gradlew :konsistTest:test` falha nomeando o arquivo infrator, e remover
- [x] 9.7 Confirmar `./gradlew :konsistTest:test` verde na base

## 10. G5 — R8 e *build* de release

- [x] 10.1 Substituir `optimization { enable = false }` por otimização habilitada no *build type* `release` de `:app` (D7)
- [x] 10.2 Escrever `app/src/main/keepRules/rules.keep` cobrindo `@Serializable` + `Companion.serializer()`, tipos `@Resource`/DSL do Ktor, classes resolvidas por reflexão pelo Koin e `@Parcelize`
- [x] 10.3 Escrever `data/consumer-rules.keep` com o que `:data` precisa impor a quem a consome
- [x] 10.4 Rodar `./gradlew :app:assembleRelease` e corrigir no código/nas *keep rules* qualquer quebra — nunca desligando a otimização
- [x] 10.5 Verificar o artefato de release por `scripts/release-smoke-check.sh` (monta com R8, instala, confirma que sobe), documentado como verificação manual/CI fora do comando G8 (D8). O `ColdStartSmokeTest` instrumentado cobre grafo e `ResourceProvider` na variante debug — instrumentar o APK minificado exigiria manter a stdlib no app e anularia o R8.

## 11. G8 — suíte consolidada e documentação

- [x] 11.1 Rodar `./gradlew detekt ktlintCheck :app:lintDebug :konsistTest:test :domain:test :data:testDebugUnitTest` e deixar tudo verde
- [x] 11.2 Acrescentar `:app:testDebugUnitTest` ao comando G8 e atualizar o `AGENTS.md` da raiz e o do `:app` (D9.3); rodar a suíte ampliada e confirmar verde
- [x] 11.3 Corrigir em `app/AGENTS.md` o pacote raiz ilustrado de `com/desafiomb/` para `com/desafiomercadobitcoin/` (D9.1)
- [x] 11.4 Corrigir o cabeçalho truncado `## Idiom/new-` no `AGENTS.md` da raiz
- [x] 11.5 Documentar em `AGENTS.md` (ou `README`) como configurar `CMC_API_KEY` em `local.properties`, deixando claro que a ausência da chave não quebra o build
- [x] 11.6 Confirmar que o repositório não contém `baseline.xml` de Detekt, `lint-baseline.xml`, `@Suppress` ou `// ktlint-disable` introduzidos para fazer a suíte passar
