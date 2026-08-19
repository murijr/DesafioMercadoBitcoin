# AGENTS.md — `:app`

> **Android application.** Único módulo que conhece `Activity`, `Composable`, recursos, manifest e DI wiring. Aqui vivem as decisões de plataforma que não cabem em `:domain`/`:data`.

## Layout

```
app/src/main/kotlin/com/desafiomercadobitcoin/
├── di/                  # módulos Koin do app (wire-up das camadas)
├── presentation/
│   ├── common/          # ResourceProvider, TextKey sealed
│   ├── theme/
│   └── feature/
│       └── <feature>/
│           ├── components/   # Composables reutilizáveis da feature
│           ├── <Feature>Screen.kt
│           ├── <Feature>ViewModel.kt
│           ├── <Feature>Event.kt        # sealed
│           ├── <Feature>State.kt        # data class @Parcelize
│           ├── <Feature>Effect.kt       # sealed (one-shot events)
│           └── mapper/                  # BM → StateModel
```

## Prefixo `VM` (ViewModel model)

Modelos de apresentação. Vivem em `:app/.../presentation/feature/<feature>/`. Ex.: `VMExchangeDetail`, `VMAssetListItem`.

- **Nunca** atravessam a fronteira para `:domain` ou `:data`. Apresentação é só apresentação.
- O que chega aqui é `BM` (ver [`domain/AGENTS.md`](../domain/AGENTS.md)) via `BM.to()` em `<feature>/mapper/`.

## MVI enxuto

- **Event** (sealed) — entrada: o que o usuário fez. A UI dispara via `viewModel.send(event)` (sempre esse nome, sem variações).
- `viewModelScope.launch { }` só existe dentro de `send()` — é o único ponto de entrada de coroutine do ViewModel. Os métodos privados que tratam cada `Event` (`handleLoadInitial()`, `handleLoadNextPage()`, etc.) nunca abrem seu próprio `launch`; são chamados de dentro desse launch e só viram `suspend fun` quando o tratamento daquele evento específico realmente precisar suspender (chamada a `UseCase`, por exemplo) — um handler puramente síncrono continua uma função comum.
- **State** (data class `@Parcelize`) — saída: o que a UI deve renderizar agora.
- **Effect** (sealed) — one-shot events: snackbar, navegação, dialog. Não colocar no State coisas que devem ser consumidas uma única vez.
- ViewModel expõe `state: StateFlow<State>` + `effect: SharedFlow<Effect>`.
- `SavedStateHandle.getMutableStateFlow(KEY, State())` para sobreviver a process death.
- ViewModel depende **apenas** de UseCases e `ResourceProvider` — nunca de `RepositoryImpl` ou DataSource. (G2 enforça.)

### Mappers presentation

- `BM.toVM(): VM` em `:app/.../presentation/.../mapper/`.
- Uma direção só. `BM.toState()` é ok quando precisa nomear; `toVM()` é o padrão.
  (`to()` puro colide com o detekt `FunctionMinLength` (mínimo 3 caracteres),
  ativo no projeto desde antes desta convenção existir — `toVM()` é o nome
  canônico daqui em diante.)
- Para o lado transporte (`DM.to()` → `BM`), ver [`data/AGENTS.md`](../data/AGENTS.md).

## `ResourceProvider` e i18n

- `ResourceProvider` em `:app/presentation/common/` resolve `TextKey` (do `:domain`) → `R.string.*`.
- ViewModels usam `ResourceProvider` para tornar `DomainError` em string localizável **antes** de emitir `State.Error`/`Effect.ShowSnackbar`.
- **Nunca** string hardcoded em `Composable`/`State`. Atenção: `HardcodedText` do Android Lint só analisa layouts XML e o `compose-lint-checks` não tem detector equivalente — quem reprova literal em `Composable` é o **G2** (`PresentationRulesTest`), não o G6.
- **Nunca** passar `Context` para `:domain`/`:data`.

## Injeção de dependência (app)

- Módulo(s) Koin em `app/di/`.
- Bindings do app: `viewModel { ... }` para ViewModels, `factory`/`single` para o que `:app` adiciona (tema, navegação, `ResourceProvider`).
- Bind de Repository: `factory<Interface> { Impl(get()) }` — `Interface` vem de `:domain`, `Impl` vem de `:data`.
- `koinViewModel()` nas telas Compose.
- Nada de Service Locator global espalhado pelo código — `get()` apenas dentro de módulos Koin e no construtor de ViewModels.

## Testes

| O que se testa | Onde mora o erro |
|---|---|
| Fluxo de State/Effect em resposta a sucesso e erro (ViewModel) | `State.Error`, `Effect.ShowSnackbar` |
| Renderização + interação (Screen/Composables) via Robolectric | Cobertura do happy path e do error path do ViewModel subjacente |
| Renderização + interação (Screen/Composables/navegação) via `androidTest`, em dispositivo/emulador real | Mesma cobertura acima, verificada sobre compositor real em vez de sombra JVM (G9) |

UI Compose é testada com `createComposeRule()` via Robolectric (JVM, sem emulador, incluído em G7 se for unit test). A mesma cobertura também existe, de forma aditiva, como suíte instrumentada em `src/androidTest` (G9) — nenhuma das duas substitui a outra.

Convenção completa (Gherkin + Enclosed + Happy/Error) no root.

## Recursos e manifest

- `app/src/main/AndroidManifest.xml` — manter mínimo. Se adicionar `<activity>`, declarar `android:exported` conforme requisitos do target SDK 37.
- `app/src/main/keepRules/rules.keep` — regras Proguard/R8 consumidas pelo AGP. Sem essa checagem, `kotlinx-serialization`, Ktor DSL, Koin resolvido por reflexão, `@Parcelize` em `SavedStateHandle` quebram silenciosamente no release (G5).
- `scripts/release-smoke-check.sh` — verificação do G5 sobre o **artefato ofuscado**: monta o release com R8, instala e confirma que o app sobe. Exige dispositivo, então roda manualmente ou em CI, **fora** do comando G8. Nota: enquanto a casca não exercitar serialização, `@Parcelize` ou reflexão no arranque, nenhuma *keep rule* é load-bearing — o valor desta guarda cresce com a primeira feature.

## G8 — execução dos guardrails no fim da feature

As guardas mecânicas (G2, G3, G4, G6, G7) são responsabilidade do **agente orquestrador** (ou do desenvolvedor humano em modo manual): ao concluir cada feature — antes de sinalizar o trabalho como pronto para revisão —, ele deve executar a suíte completa e validar que tudo está verde.

### Comando

```text
./gradlew detekt ktlintCheck :app:lintDebug :konsistTest:test :domain:test :data:testDebugUnitTest :app:testDebugUnitTest
```

A ordem segue o critério "barato primeiro, caro por último":
- `detekt` — diff, milissegundos
- `ktlintCheck` — diff, segundos
- `:app:lintDebug` — segundos a 30s
- `:konsistTest:test` — JVM puro, segundos
- `:domain:test` — JVM puro, segundos
- `:data:testDebugUnitTest` — JVM com Robolectric, ~30s
- `:app:testDebugUnitTest` — JVM com Robolectric + Compose UI Test, ~1min

### Quando uma guarda falha

A causa do erro deve ser corrigida no código — **nunca** na configuração do guardrail:

- **Não suprimir regras** (Detekt baseline, Lint baseline, `@Suppress`).
- **Não desabilitar checks** (`lintRelease --ignore-rules`, `disable+=...`).
- **Não substituir regras por versões mais fracas** (`ktlint_official` → `intellij_idea` para "passar").
- **Não contornar** (`// ktlint-disable`, `// noinspection`).

## G9 — testes instrumentados de tela e componente

Cobre, em `app/src/androidTest`, as mesmas telas e componentes já verificados pela suíte JVM/Robolectric (`ExchangeListScreen`, `ExchangeDetailScreen`, `AppNavigation`, e os componentes `ExchangeListItem`, `ExchangeDetailHeader`, `CurrencyListItem`), mas rodando sobre o compositor e o ciclo de vida reais do Android em vez de uma sombra em JVM. É aditivo a G7: nenhuma das duas suítes substitui a outra.

### Comando

```text
./gradlew :app:connectedDebugAndroidTest
```

Exige dispositivo ou emulador conectado, então roda **fora** do comando de G8 — mesma razão de `scripts/release-smoke-check.sh` (G5): instrumentar exige hardware, e G8 é uma suíte JVM (G7). Vermelho bloqueia a finalização da *feature*, do mesmo jeito que G7.

O caminho correto é **buscar a solução apropriada para o problema** — não enfraquecer a regra.

## O que `:app` **não** pode importar

- **Nada** de `:data` para dentro de ViewModel/Screen. ViewModel só conhece `UseCase` e `ResourceProvider`. Mappers e `RepositoryImpl` ficam para a DI.
- **Nada** de `LiveData`, RxJava, Gson/Moshi, Hilt/Dagger — o stack está fixado.
- **Nada** de regra de negócio. Regra de negócio é `:domain` (UseCase).
- **Nada** de `DM` (DataModel). Se está vazando, falta mapper em `:data`.

## O que `:app` **pode** importar

- Tudo: Compose, Material 3, `Context`, `Activity`, recursos, navegação.
- `:domain` (interfaces, `BM`, `DomainError`, `UseCase`, `ResourceProvider`).
- `:data` apenas a partir de `app/di/` (faz o bind). Nunca dentro de `presentation/`.
