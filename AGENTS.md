# AGENTS.md

> **Documentação por módulo.** As regras de cada camada vivem no `AGENTS.md` do módulo — este arquivo só descreve o que **atravessa** o projeto.
>
> - [`domain/AGENTS.md`](./domain/AGENTS.md) — UseCases, `DomainError`, prefixo `BM`, fronteira Kotlin puro.
> - [`data/AGENTS.md`](./data/AGENTS.md) — `RepositoryImpl`, DataSources Ktor, prefixo `DM`, DI da camada.
> - [`app/AGENTS.md`](./app/AGENTS.md) — Compose, MVI, ViewModel, `ResourceProvider`, G8 no fim da feature.

## Propósito

App Android do desafio **"Quero ser MB"**: consome a **API pública da CoinMarketCap** para listar **exchanges** de criptomoedas e permitir drill-down até os **ativos** negociados em cada corretora.

## Stack

- **UI**: Jetpack Compose + Material 3.
- **DI**: Koin (sem codegen, módulos explícitos).
- **Rede**: Ktor Client (engine `OkHttp`) + `kotlinx.serialization`.
- **Concorrência**: Coroutines + `Flow` / `StateFlow` / `SharedFlow`.
- **Testes**: JUnit 4 + MockK + Robolectric (JVM com sombra Android) + Espresso/Compose UI Test (instrumentados).

## Princípios (filtro de cada decisão)

- **YAGNI** — não criar abstração, módulo, use case ou DI binding antes de existir um caso de uso concreto. Na dúvida, copie e duplique; refatore no terceiro repetido (rule of three).
- **KISS** — a solução mais simples que satisfaz o requisito atual. Padrão só existe pra resolver problema; sem problema, sem padrão.
- **DRY** — duplicação só é proibida quando a **regra de negócio é a mesma**. Coisas que só "parecem" iguais não se unificam — acoplamento prematuro mata manutenção.
- **SOLID (prático, não dogmático):**
  - *S* — uma classe, uma razão pra mudar. UseCase orquestra validação + chamada; DataSource faz IO; `RepositoryImpl` mapeia.
  - *O* — estender via interface/subclasse, nunca editando código existente. `UseCase<I, S>` cresce por subclasses de `doExecute`.
  - *L* — implementação substituível pela interface sem surpreender o chamador.
  - *I* — interfaces pequenas e coesas. Repositório com 10 métodos é sinal pra dividir.
  - *D* — camadas de alto nível não importam nada de camadas inferiores. `:domain` não conhece `:data` nem `:app`; o grafo aponta pra dentro.

Antes de adicionar dependência, abstração ou módulo, pergunte: *"qual problema concreto isso resolve?"*. Sem resposta imediata, não adicione.

## Prática TDD

Toda feature nasce **teste primeiro**: vermelho → verde → refatorar. Aplica-se em ciclo dentro do **módulo-alvo** da regra de negócio (`:domain` para UseCase, priorizando JVM puro; `:data` para integração com DataSource; `:app` para ViewModel/Screen). O PR deve trazer pelo menos um teste do caminho feliz como evidência. UI Compose é testada com `createComposeRule()` via Robolectric (JVM, sem emulador).

A **mecanização** desse processo vive no **G7/G8** (ver Guardrails) — quem decide se o teste nasceu vermelho é quem está escrevendo o código; o orchestrator só verifica que a suíte continua passando.

## Convenção de testes

Referenciada pelos três `AGENTS.md` de módulo como "convenção completa". Vale para `:domain`, `:data` e `:app`.

### Estrutura: `Enclosed` + contextos aninhados

Uma classe de teste por unidade sob teste, anotada com `@RunWith(Enclosed::class)`. O *setup* comum vive numa classe base aberta `TestSetup`; cada contexto é uma classe aninhada que a estende.

```kotlin
@RunWith(Enclosed::class)
class CreateShortUrlUseCaseTest {

    abstract class TestSetup {
        protected val repository = mockk<ShortUrlRepository>()
        protected val useCase = CreateShortUrlUseCase(repository)
    }

    class HappyPath : TestSetup() {
        @Test
        fun `given a valid url when execute then returns the shortened url`() = runTest {
            // ...
        }
    }

    class ErrorPath : TestSetup() {
        @Test
        fun `given a blank url when execute then fails with Validation`() = runTest {
            // ...
        }
    }
}
```

- **`HappyPath`** e **`ErrorPath`** são obrigatórios. O mínimo por `UseCase` e por `RepositoryImpl` está no `AGENTS.md` do módulo.
- **`EdgeCases`** é opcional: entra quando houver caso intermediário (limite, múltiplos erros, entrada degenerada).
- Contexto sem teste não existe. Não criar `EdgeCases` vazio "para depois".

### Nomes em Gherkin

O nome do teste é uma frase em *backticks*, no formato `given <contexto> when <ação> then <resultado observável>`.

- Em inglês, como todo identificador de código — só a prosa da documentação é pt-BR.
- O `then` descreve o que se **observa**, não o que se implementou: `then fails with Validation`, não `then calls validate`.
- Sem `should`, sem numeração, sem nome do método sob teste repetido no início.

### Asserções

- Comparar o **subtipo** do erro e seus dados, nunca o texto da mensagem — `DomainError` carrega `TextKey`, e o texto só existe depois do `ResourceProvider`.
- Verificar ausência de interação quando a regra é "não deve chamar": `coVerify(exactly = 0) { repository.foo() }`.
- Um comportamento por `@Test`. Se o nome precisa de "and", provavelmente são dois testes.

### Corrotinas

- `runTest { }` para tudo que suspende.
- Teste de cancelamento verifica que `CancellationException` **escapa**, e não que virou `Result.failure`.

## Configuração local

A chave da API da CoinMarketCap é lida de `local.properties` (não versionado) ou da variável de ambiente `CMC_API_KEY`:

```properties
# local.properties
cmc.api.key=SUA_CHAVE_AQUI
```

O padrão é **string vazia**: quem clona o repositório compila e roda a suíte G8 inteira sem credencial. A ausência da chave só se manifesta em tempo de execução, como `DomainError.Network` — o build nunca quebra por isso. O valor chega a `:data` por injeção (`CoinMarketCapConfig` no módulo Koin), nunca lido diretamente pela camada de dados.

O SDK do Android vem de `sdk.dir` no mesmo `local.properties`. Em WSL, aponte para um SDK **Linux** — um SDK do Windows (`aapt.exe`) faz o AGP reportar "Build Tools corrupted".

## Estrutura multi-módulo

Topologia `:domain` (Kotlin puro) → `:data` (android-library) → `:app` (android-application). Setas = "depende de".

```
:app  ──▶  :data  ──▶  :domain
```

| Módulo     | Tipo                 | Responsabilidade                                                                |
|------------|----------------------|---------------------------------------------------------------------------------|
| `:domain`  | `kotlin-library`     | Entities/Models, interfaces de Repository, UseCases, erros de domínio. Kotlin puro, sem Android SDK. |
| `:data`    | `android-library`    | Implementações de Repository, DataSources (rede, banco), DTOs, mapeamentos.     |
| `:app`     | `android-application`| Presentation (Compose, ViewModels), DI wiring (Koin), navegação, recursos, manifest. |

**Por que essa divisão:**
- `:domain` como Kotlin puro é a proteção mecânica mais barata contra vazamento de framework — se Android SDK entrar aqui, o módulo **falha em compilar**.
- `:data` separado impede o presentation de conhecer detalhes de transporte (Ktor, Serialization). Trocar HTTP exige mexer só em `:data`.
- `:app` é o único módulo que conhece `Context`, `Activity`, `Composable` e recursos — onde decisões de plataforma inevitavelmente vivem.

## Prefixos de modelo

Fronteira visível na assinatura da classe; o tipo de retorno já diz em qual camada o objeto vive e em que direção o mapper flui.

- **VM** (ViewModel) — apresentação. `:app/.../presentation/feature/<feature>/`. Nunca cruza pra `:domain`/`:data`.
- **BM** (BusinessModel) — domínio. `:domain/<feature>/` ou `:domain/model/`. Única representação de negócio que `:domain` e `:data` veem.
- **DM** (DataModel) — transporte/persistência. `:data/<feature>/dto/` ou `:data/<feature>/model/`. Sempre `@Serializable` quando vêm de JSON.

**O prefixo é a única marca de camada no nome.** Classe com prefixo `BM`, `DM` ou `VM` não pode terminar em rótulo de camada — `Dto`, `Model`, `Entity`, `Data`, `Payload`, `Body`, `Json` ou `Schema`, em qualquer caixa. `DMExchangeDto` declara "camada de dados" duas vezes, e no dia em que os dois rótulos discordarem o nome passa a mentir. O G2 reprova (`models never repeat their layer in the name suffix`).

Sufixo que descreve a **forma** do dado continua permitido, porque carrega informação que o prefixo não carrega: `DMExchangeMapResponse` (o envelope `{ data: [...] }`) e `DMExchangeMapEntry` (o elemento do array) precisam se distinguir dentro do mesmo *endpoint*.

Mapers: `DM.to(): BM` em `:data/mapper/`; `BM.to(): VM` em `:app/.../presentation/.../mapper/`. **Um único sentido por função de extensão** — nada de `DM.to().to()`.

## Guardrails (proteção do projeto)

A arquitetura é protegida por **mais de um guardrail em camadas** — cada um cobre uma classe diferente de erro. YAGNI também vale aqui: novo guardrail **só** depois de o problema concreto ter aparecido ao menos uma vez.

| # | Nome              | Tipo      | O que protege                                                                  |
|---|-------------------|-----------|--------------------------------------------------------------------------------|
| 1 | Gradle            | mecânico  | Topologia `:domain` (Kotlin puro) — Android SDK em `:domain` falha em compilar.|
| 2 | Konsist           | estático  | Grafo de camadas, prefixos `VM`/`BM`/`DM`, proibição de rótulo de camada no fim do nome, sufixos `Repository`/`Impl`/`UseCase`, fronteiras, regra "ViewModel depende só de UseCases + `ResourceProvider`". Asserts em `:konsistTest/`. |
| 3 | Detekt            | estático  | Complexidade, LOC, funções/classe, sufixos, wildcards, regra "`CancellationException` re-lançada em `try/catch (Throwable)`". `detekt.yml` na raiz. |
| 4 | KtLint            | estilo    | `## Estilo` (`:ktlintCheck` em CI; `:ktlintFormat` **só local**). `.editorconfig` na raiz. |
| 5 | R8 / Proguard     | mecânico  | `keepRules/rules.keep` consistente — `kotlinx-serialization`, Ktor DSL, Koin via reflexão, `@Parcelize`. Falha se `:app:assembleRelease` quebrar ou `ColdStartSmokeTest` falhar. |
| 6 | Android Lint + Slack Compose | estático | Severidade de `Manifest`/recursos/a11y/`NewApi`/`HardcodedText`, regras `androidx.compose.lint` + `slack-compose-lints` (API design de Composables). `abortOnError = true` em `app/build.gradle.kts`. |
| 7 | Testes unitários  | mecânico  | `:domain:test` + `:data:testDebugUnitTest` + `:app:testDebugUnitTest` + `:konsistTest:test` (JVM puro, Robolectric sem emulador). Vermelho = bloqueia a finalização. |
| 8 | Execução no fim da feature | processo | O orchestrator (ou humano) roda `./gradlew detekt ktlintCheck :app:lintDebug :konsistTest:test :domain:test :data:testDebugUnitTest :app:testDebugUnitTest` antes de declarar a feature pronta. Ordem: barato → caro. Cobre G1–G7 e a metade JVM do G10, sem emulador. Detalhes em [`app/AGENTS.md`](./app/AGENTS.md). |
| 9 | Testes instrumentados | mecânico | `./gradlew :app:connectedDebugAndroidTest` — telas e componentes de Compose sobre dispositivo/emulador real, aditivo a G7 (nenhum substitui o outro). Exige dispositivo conectado, então roda **fora** do comando de G8. Vermelho = bloqueia a finalização. Detalhes em [`app/AGENTS.md`](./app/AGENTS.md). |
| 10 | StrictMode | runtime | `StrictMode.ThreadPolicy` instalada por `DesafioApplication` **só em debug**: leitura/escrita em disco e rede na *main thread* viram log com rastro de pilha + morte do processo. Único guardrail de execução — não reprova tarefa Gradle sozinho. Verificado em duas frentes: teste JVM prova que é instalada (dentro de G8), suíte instrumentada prova que ninguém a viola (junto de G9). Detalhes em [`app/AGENTS.md`](./app/AGENTS.md). |

**Falha de guardrail = corrigir no código, nunca na configuração.** Nada de `baseline.xml`/`@Suppress`/`--ignore-rules`/`// ktlint-disable`/`disable+=...` pra forçar passar.

## Estilo

- Kotlin official style (`kotlin.code.style=official`).
- Nomes expressivos — `CreateShortUrlUseCase`, não `CreateShortUrlManager`.
- Comentários só onde a intenção **não** é óbvia pelo código. Nada de comentário que repete o nome do método.
- Não introduzir dependência nova sem necessidade clara (YAGNI).

## O que **não** fazer (regras globais)

- Não importar Android SDK dentro de `:domain` (G1 + G2 seguram).
- Não acessar `Context`, recursos ou `Composable` a partir de `:data` ou `:domain`.
- Não criar `Repository` "genérico" ou base abstrata antes de ter ≥3 repositórios concretos com necessidade real de compartilhar código.
- Não criar nova camada (ex.: `core/`, `shared/`, `utils/`) "por organização" — `:domain`/`:data`/`:app` já são organização.
- Não acoplar UseCase a `CoroutineScope` — quem chama controla o escopo.
- Não capturar `CancellationException` em `try/catch` genérico sem re-lançá-la.
- Não introduzir `LiveData`, RxJava, Retrofit, Gson/Moshi, Hilt/Dagger sem decisão explícita — o stack já está fixado.
- Não desabilitar/suprimir/contornar guardrail pra forçar passar — corrigir a causa no código.

## Idioma

- Toda documentação gerada pelo agente (.md, comentários, respostas em chat) deve ser escrita em português do Brasil (pt-BR).
- Nomes de arquivos, chaves YAML, mensagens de commit, identificadores de código e comandos CLI permanecem em inglês. Apenas o conteúdo prose é em pt-BR.
- Quando citar ferramentas/conceitos com nome consolidado em inglês ("git worktree", "merge", "review"), mantenha o termo em itálico ou entre aspas, sem traduzir.