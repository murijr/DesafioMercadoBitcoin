## Why

Os quatro `AGENTS.md` do repositório descrevem um projeto multi-módulo com stack Compose/Koin/Ktor e oito guardrails mecânicos (G1–G8), mas o código atual é o *scaffold* cru do template do Android Studio: os três módulos existem e não se conhecem, nenhuma dependência da stack está declarada, nenhum guardrail além do G1 parcial existe, e não há sequer uma `Activity` no manifesto. Enquanto essa lacuna existir, toda feature nasce sem rede de proteção e cada decisão de arquitetura vira convenção verbal em vez de falha de build.

## What Changes

**Topologia e build (G1)**
- Declarar as arestas `:app → :data → :domain` nos `build.gradle.kts` (hoje inexistentes) e registrar o módulo `:konsistTest` no `settings.gradle.kts` — ele já existe em disco mas está fora do build.
- Popular `gradle/libs.versions.toml` com a stack fixada nos `AGENTS.md`: Compose BOM + Material 3, Koin, Ktor Client (engine `Android`, `ContentNegotiation`, `Resources`, `Logging`, `HttpTimeout`), `kotlinx-serialization`, `kotlinx-coroutines`, e o ferramental de teste (JUnit 4, MockK, Robolectric, Compose UI Test, `coroutines-test`).
- **BREAKING (template):** remover `androidx.appcompat` e `com.google.android.material` (stack de *Views*) de `:app` e `:data`, e trocar o tema `Theme.MaterialComponents.*` por tema Compose/M3.
- Elevar `sourceCompatibility`/`targetCompatibility`/`jvmTarget` de 11 para 17 nos três módulos, alinhando com o *toolchain* JDK 25 e com os requisitos das bibliotecas AndroidX atuais.
- Aplicar `kotlin-parcelize` em `:app` (exigido pelo `State` `@Parcelize`) e o plugin de compilador do Compose.

**Guardrails (G2–G8)**
- **G2 Konsist:** módulo `:konsistTest` com os *asserts* de grafo de camadas, prefixos `VM`/`BM`/`DM`, sufixos `Repository`/`Impl`/`UseCase` e a regra "ViewModel depende só de `UseCase` + `ResourceProvider`".
- **G3 Detekt:** plugin + `detekt.yml` na raiz (complexidade, LOC, `FunctionMinLength`, *wildcard imports*, e a regra de re-lançar `CancellationException`).
- **G4 KtLint:** plugin (`ktlintCheck` em CI) + `.editorconfig` na raiz com `ktlint_official`.
- **G5 R8:** **BREAKING (template):** habilitar a otimização do *build type* `release` (hoje `optimization { enable = false }`, o que desliga o guardrail) e escrever as *keep rules* de `kotlinx-serialization`, Ktor, Koin e `@Parcelize` em `app/src/main/keepRules/rules.keep` e `data/consumer-rules.keep`.
- **G6 Lint:** `abortOnError = true` em `:app`, severidades elevadas para `HardcodedText`/`NewApi`/a11y, mais as regras `androidx.compose.lint` e `slack-compose-lints`.
- **G7 Testes:** remover os `ExampleUnitTest`/`ExampleInstrumentedTest` do template e deixar cada módulo com sua suíte configurada (Robolectric em `:data` e `:app`).
- **G8 Processo:** garantir que `./gradlew detekt ktlintCheck :app:lintDebug :konsistTest:test :domain:test :data:testDebugUnitTest` execute e passe de ponta a ponta.

**Bases de código (o mínimo que os `AGENTS.md` exigem para as features nascerem)**
- `:domain` — `UseCase<I, S>` com o contrato de `Result` descrito em `domain/AGENTS.md`, `DomainError` *sealed*, `TextKey` *sealed* e `Throwable.toDomainError()`.
- `:data` — `HttpClientFactory`, o cliente CoinMarketCap com o header `X-CMC_PRO_API_KEY` injetado via `BuildConfig` do `:app`, e o módulo Koin da camada.
- `:app` — `Application` que inicia o Koin, `MainActivity` com Compose/M3, tema, `ResourceProvider` resolvendo `TextKey` → `R.string.*`, e o módulo Koin de *wire-up*.

**Documentação**
- Adicionar ao `AGENTS.md` da raiz a seção de convenção de testes (Gherkin + `Enclosed` + Happy/Error) — os três `AGENTS.md` de módulo remetem a ela ("Convenção completa no root"), mas ela não existe.
- Corrigir em `app/AGENTS.md` o pacote raiz ilustrado (`com/desafiomb/` → `com/desafiomercadobitcoin/`, que é o `namespace`, o `applicationId` e o pacote dos fontes já existentes) e o cabeçalho truncado `## Idiom/new-` na raiz.

**Fora de escopo:** as features de negócio (listagem de *exchanges* e *drill-down* de ativos). Elas virão em *changes* próprias, sobre a base entregue aqui.

## Capabilities

### New Capabilities
- `architecture-guardrails`: os oito guardrails G1–G8 como comportamento verificável do build — o que exatamente deve falhar quando a fronteira de camada, o prefixo de modelo, o estilo, a *keep rule* ou a suíte de testes é violado.
- `domain-foundations`: o contrato de `UseCase` (semântica de `Result`, re-lançamento de `CancellationException`, mapeamento para `DomainError`), a hierarquia `DomainError` e o mecanismo `TextKey` de i18n sem `Context`.
- `data-network-foundation`: o cliente HTTP da camada `:data` — autenticação na CoinMarketCap, negociação de conteúdo, *timeouts*, e a tradução de exceções de transporte em `DomainError`.
- `app-shell`: a casca do aplicativo — inicialização do grafo Koin, entrada Compose/M3 e a resolução de `TextKey` → recurso pelo `ResourceProvider`.

### Modified Capabilities
<!-- Nenhuma: openspec/specs/ está vazio; este é o primeiro conjunto de capabilities do projeto. -->

## Impact

- **Build:** `settings.gradle.kts`, `build.gradle.kts` da raiz, os três `build.gradle.kts` de módulo, `gradle/libs.versions.toml`, `gradle.properties`; novos `detekt.yml` e `.editorconfig` na raiz; novo `konsistTest/build.gradle.kts`.
- **Código novo:** bases em `:domain` (`UseCase`, `error/`), `:data` (`network/`, `di/`) e `:app` (`di/`, `presentation/common/`, `presentation/theme/`, `MainActivity`, `Application`).
- **Código removido:** testes de exemplo do template nos três módulos; dependências de *Views* (`appcompat`, `material`).
- **Recursos/manifesto:** `AndroidManifest.xml` do `:app` ganha a `<activity>` com `android:exported="true"`; `themes.xml`/`colors.xml` migram para o tema Compose.
- **Segredo:** a chave da API CoinMarketCap passa a ser lida de `local.properties` → `BuildConfig` do `:app`. Sem chave, o build **não** deve quebrar (valor vazio); a falha aparece em tempo de execução como `DomainError`.
- **Risco de configuração:** habilitar R8 no `release` (G5) pode expor quebras de reflexão que hoje estão escondidas — é exatamente o que o guardrail existe para pegar, e será validado por `:app:assembleRelease`.
- **CI:** ainda não existe pipeline no repositório; este *change* entrega o comando G8 executável localmente e deixa a automação de CI para depois (YAGNI).
