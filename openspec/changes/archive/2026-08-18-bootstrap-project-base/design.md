## Context

Estado atual (verificado em disco): Gradle 9.5, *toolchain* JDK 25 (fixado em 17 durante a implementacao — ver D4), AGP 9.3.1, Kotlin 2.2.10, `compileSdk`/`targetSdk` 37, `minSdk` 26, *configuration cache* ligado. Os três módulos existem, mas **nenhum declara dependência de outro**; `:app` e `:data` ainda carregam `appcompat` + `material` (stack de *Views*); `:domain` é `java-library` + `kotlin.jvm` (a fronteira G1 já está mecanicamente correta). Não há `Activity` no manifesto, nem `detekt.yml`, `.editorconfig`, plugins de qualidade, ou `:konsistTest` no `settings.gradle.kts` — embora o diretório `konsistTest/` exista em disco com artefatos de uma tentativa anterior e **nenhum fonte**.

Motivação em `proposal.md — Why`. Requisitos comportamentais nas quatro *specs* deste *change*.

Restrição relevante: o cache Gradle local já contém, de uma tentativa anterior, as versões Koin 4.2.2, Ktor 3.5.2, Compose BOM 2026.08.00, Konsist 0.17.3, Detekt 1.23.8, Robolectric 4.16.1, MockK 1.14.11, `compose-lint-checks` 1.5.4, lifecycle 2.11.0, `activity-compose` 1.13.0, `kotlinx-serialization-json` 1.11.0, `kotlinx-coroutines-test` 1.11.0. Adotar esse conjunto evita resolução nova e é o ponto de partida do catálogo.

## Goals / Non-Goals

**Goals:**
- Deixar `./gradlew detekt ktlintCheck :app:lintDebug :konsistTest:test :domain:test :data:testDebugUnitTest` verde sobre uma base que **já exercita** cada guardrail — um guardrail que nunca reprovou nada não está provado.
- Entregar as bases mínimas (`UseCase`, `DomainError`/`TextKey`, `HttpClientFactory`, `ResourceProvider`, grafo Koin, `MainActivity`) na menor forma que satisfaz as *specs*, sem antecipar abstração.
- Ordenar o trabalho de modo que cada guardrail seja ligado **junto** do código que ele protege, não todos no fim.

**Non-Goals:**
- Pipeline de CI. Não há `.github/workflows` no repositório; G8 é entregue como comando local (YAGNI — CI vira *change* própria quando houver onde rodar).
- Cobertura de teste medida (JaCoCo/Kover). Nenhum `AGENTS.md` pede.
- Navegação entre telas. A casca tem uma tela; navegação nasce com a segunda.
- Qualquer modelo `BM`/`DM`/`VM` de negócio — eles pertencem às *changes* de feature.

## Decisions

### D1 — Ligar cada guardrail junto do código que ele protege, com uma violação deliberada como prova

**Escolha:** para cada guardrail, o trabalho é: configurar → introduzir uma violação temporária → confirmar que o build **falha** → remover a violação → confirmar verde.

**Por quê:** o modo de falha real de um projeto guardrail-first é um guardrail configurado que não verifica nada (escopo de fontes errado, tarefa não ligada ao `check`, regra desabilitada por padrão). Só a reprovação observada prova a proteção. É o mesmo ciclo vermelho→verde da prática TDD do `AGENTS.md` da raiz, aplicado à configuração.

**Alternativa descartada:** configurar tudo e confiar que "passou" significa "protege". Foi exatamente assim que a tentativa anterior deixou `konsistTest/` com classes compiladas e zero fontes.

### D2 — `:konsistTest` como módulo Kotlin/JVM separado, fora da topologia

**Escolha:** módulo `kotlin("jvm")` registrado no `settings.gradle.kts`, sem dependência de `:app`/`:data`/`:domain`. O Konsist varre o repositório pelo sistema de arquivos (escopo do projeto), não pelo *classpath*.

**Por quê:** hospedar os *asserts* dentro de `:app` faria o módulo de arquitetura depender do módulo que ele julga, e obrigaria a suíte a compilar todo o Android para verificar nomes de arquivo. Módulo isolado mantém `:konsistTest:test` em JVM pura, na casa dos segundos, como o `AGENTS.md` da raiz exige. Ele também já é o nome usado no comando G8 e no diretório existente.

**Consequência:** o diretório `konsistTest/build/` e `konsistTest/tmp/` atuais são lixo da tentativa anterior e devem ser apagados antes de registrar o módulo.

### D3 — Chave da CoinMarketCap: `local.properties` → `BuildConfig` do `:app` → Koin → `:data`

**Escolha:** o `build.gradle.kts` do `:app` lê `CMC_API_KEY` de `local.properties` (ou da variável de ambiente homônima), com **string vazia como padrão**, e a publica em `BuildConfig`. O módulo Koin do `:app` injeta esse valor no módulo do `:data`, que o aplica como cabeçalho padrão do cliente HTTP.

**Por quê:** `:data` não pode conhecer `BuildConfig` do `:app` (dependência invertida), e `AGENTS.md` do `:data` proíbe *hardcode*. Injetar o valor como parâmetro do módulo Koin é o caminho que mantém a seta apontando para dentro. O padrão vazio garante que quem clona o repositório consegue compilar e rodar a suíte G8 sem credencial — a falta de chave só aparece como `DomainError.Network` em tempo de execução, que é o comportamento especificado.

**Alternativa descartada:** `buildConfigField` no `:data`. Funciona, mas espalha a leitura de segredo por dois módulos e coloca configuração de aplicação numa biblioteca.

### D4 — Detekt 1.23.8 sem *type resolution*

**Escolha:** rodar `detekt` na tarefa padrão (sem `detektMain`/`--build-upon-default-config` com *classpath*), com `detekt.yml` na raiz partindo do *default config* e ajustando o que os `AGENTS.md` pedem.

**Por quê:** Detekt 1.23.8 é a última linha 1.x e sua análise com *type resolution* não acompanha Kotlin 2.2 de forma confiável. As regras exigidas pelos `AGENTS.md` — complexidade, LOC, contagem de funções, `FunctionMinLength`, `WildcardImport`, sufixos — são todas sintáticas e não precisam de tipos.

**Correcao aplicada na implementacao (substitui a premissa de toolchain JDK 25):** o compilador embutido do Detekt 1.23.8 nao roda sobre JDK 25 — `JavaVersion.current()` do IntelliJ-util lanca `IllegalArgumentException: 25.0.3` ao construir o `KotlinCoreEnvironment`. Nao ha Detekt 2.x publicado (1.23.8 e a ultima versao no Maven Central) e a tarefa `Detekt` de 1.23.8 nao expoe `javaLauncher` para isolar o JVM. A unica saida que preserva **todas** as regras foi fixar o JVM do *daemon* em 17 (`gradle-daemon-jvm.properties`, `toolchainVersion=17`), o que tambem alinha o JVM de build ao alvo de compilacao escolhido em D6. Verificado: `:app:assembleDebug`, `ktlintCheck` e `detekt` verdes em JDK 17 com AGP 9.3.1 e `compileSdk` 37.

**Trade-off registrado:** a regra "`CancellationException` re-lançada em `try/catch (Throwable)`" (G3) é a única que se beneficiaria de tipos. Ela será implementada como **regra customizada sintática** no `detekt.yml` (`ForbiddenMethodCall` não serve; usar `SwallowedException` com `ignoredExceptionTypes` ajustado + `TooGenericExceptionCaught` com `allowedExceptionNameRegex`), aceitando falsos negativos em casos exóticos. Se isso provar insuficiente na prática, o passo seguinte é o Konsist (G2), que enxerga a árvore sintática e pode assertar o `catch`/`throw` — mas só depois do problema aparecer (YAGNI).

### D5 — KtLint `ktlint_official` com exceção explícita para `@Composable`

**Escolha:** `.editorconfig` na raiz com `ktlint_code_style = ktlint_official` e `ktlint_function_naming_ignore_when_annotated_with = Composable`. Equivalente no Detekt: `FunctionNaming.ignoreAnnotated: ['Composable']`.

**Por quê:** `ktlint_official` exige `camelCase` em nomes de função, e todo `@Composable` é `PascalCase` por convenção do Compose. Sem a exceção, o guardrail reprova código correto — e a saída errada seria rebaixar o estilo para `intellij_idea`, explicitamente proibida pelo `AGENTS.md` do `:app`. Esta exceção **não** é um enfraquecimento: é a configuração correta da regra para o dialeto do projeto, feita antes de existir violação, e não um contorno introduzido para calar uma falha.

### D6 — Java 17 nos três módulos

**Escolha:** `sourceCompatibility`/`targetCompatibility`/`jvmTarget` = 17, mantendo o *toolchain* JDK 25.

**Por quê:** Java 11 é o padrão do template e está desalinhado com a stack: AndroidX recente, Compose BOM 2026.08 e Robolectric 4.16 assumem 17. Manter 11 gera avisos de *desugaring* e limita o que a stack pode usar. 17 é o mínimo atual e não custa nada com `minSdk` 26.

### D7 — R8 ligado no `release` com *keep rules* mínimas escritas à mão

**Escolha:** trocar `optimization { enable = false }` por otimização habilitada e escrever as *keep rules* em `app/src/main/keepRules/rules.keep` (e `data/consumer-rules.keep` para o que a biblioteca precisa impor a quem a consome), cobrindo: `@Serializable` e seus `Companion.serializer()`, tipos de recurso do Ktor, classes resolvidas por reflexão pelo Koin e `@Parcelize`.

**Por quê:** desligar a otimização torna o G5 decorativo — o *build* de release passa a ser idêntico ao *debug* e nenhuma quebra de reflexão é detectável. O `AGENTS.md` da raiz descreve G5 como "falha se `:app:assembleRelease` quebrar ou `ColdStartSmokeTest` falhar", o que pressupõe R8 ativo.

**Risco aceito:** ligar R8 pode expor quebras hoje escondidas. É o propósito do guardrail; o custo aparece uma vez, na base, e não em cada feature.

### D8 — `ColdStartSmokeTest` fica em `androidTest`, fora do comando G8

**Escolha:** escrever o teste de fumaça de inicialização a frio como teste instrumentado do `:app`, executado contra a variante `release`, documentado como verificação **manual/CI** — não incluído no comando G8.

**Por quê:** G8 é explicitamente uma suíte JVM sem emulador (G7). Um teste instrumentado exige dispositivo e violaria a promessa de "segundos". A `spec` de G5 já o trata como cenário separado do cenário de `assembleRelease`.

### D9 — Inconsistências entre os `AGENTS.md` a resolver na entrega

Três divergências foram encontradas entre os quatro documentos e o código. Cada uma vira tarefa:

1. **Pacote raiz.** `app/AGENTS.md` ilustra o layout como `app/src/main/java/com/desafiomb/`, mas `namespace`, `applicationId` e todos os fontes existentes usam `com.desafiomercadobitcoin`. **Decisão:** adotar `com.desafiomercadobitcoin` (é o que o build já impõe) e corrigir o `AGENTS.md` do `:app`. Renomear o pacote seria mudança maior, sem ganho, e quebraria o `applicationId`.
2. **Convenção de testes ausente.** Os três `AGENTS.md` de módulo remetem a "Convenção completa (Gherkin + `Enclosed` + Happy/Error) no root", e essa seção **não existe** no `AGENTS.md` da raiz. **Decisão:** escrevê-la a partir do que os três documentos já pressupõem — classes `Enclosed` com aninhamento `HappyPath`/`ErrorPath`/`EdgeCases` sobre um `TestSetup` comum, e nomes de teste em Gherkin (`given ... when ... then ...`). Sem ela, os testes deste *change* não têm norma a seguir.
3. **`:app:testDebugUnitTest` fora do comando G8.** O `AGENTS.md` da raiz lista `:domain:test` e `:data:testDebugUnitTest`, mas não os testes unitários do `:app` — embora o `AGENTS.md` do `:app` diga que os testes de Compose via Robolectric estão "incluídos em G7 se for unit test". **Recomendação:** acrescentar `:app:testDebugUnitTest` ao comando G8 e ao `AGENTS.md` da raiz; caso contrário todo teste de ViewModel e de tela fica fora da rede de proteção. A tarefa registra a alteração do documento junto da alteração do comando.

## Risks / Trade-offs

- **Compose com Kotlin embutido do AGP 9** → O `:app` hoje não aplica plugin Kotlin algum (AGP 9 traz Kotlin embutido). Não está confirmado se o plugin de compilador do Compose é ativado por `buildFeatures { compose = true }` ou se exige `org.jetbrains.kotlin.plugin.compose` explícito nesse arranjo. **Mitigação:** a primeira tarefa de `:app` é subir um Composable trivial e compilar; se falhar, aplicar o plugin explícito (já presente no cache local). Resolver isso antes de qualquer outro trabalho em `:app`.
- **Detekt 1.23.8 vs. Kotlin 2.2.10** → possível ruído de *parser* em construções novas. **Mitigação:** rodar `detekt` já na primeira tarefa, sobre a base mínima, antes de haver muito código; se houver incompatibilidade, avaliar a linha 2.x do Detekt — nunca desabilitar regra.
- **`ktor-client-resources` e `ktor-client-logging` não estão no cache** → primeira execução precisará de rede. **Mitigação:** nenhuma; apenas expectativa. Se `@Resource` se mostrar custoso demais para o pouco que a base precisa, ele pode ser adiado para a *change* de feature — a *spec* de `data-network-foundation` não exige roteamento tipado, só cliente único e autenticado.
- **Configuration cache + leitura de `local.properties`** → ler o arquivo em tempo de configuração invalida o cache de forma incorreta se feito ingenuamente. **Mitigação:** ler via `providers.gradleProperty`/`providers.environmentVariable` ou `Properties` carregado dentro de um `Provider`, nunca com `File.readText()` solto no *script*.
- **`compileSdk`/`targetSdk` 37 com Robolectric 4.16.1** → o Robolectric pode não ter SDK 37 empacotado. **Mitigação:** fixar `@Config(sdk = [...])` em uma API suportada, ou `robolectric.properties` no módulo; não rebaixar o `targetSdk` do aplicativo por causa do runner de teste.
- **Guardrail que reprova a própria base** → ligar seis verificações de uma vez sobre código novo tende a produzir uma enxurrada de falhas simultâneas. **Mitigação:** é exatamente o motivo de D1 — um guardrail por vez, com a base crescendo junto.

## Migration Plan

Não há usuários nem release anterior: o *change* é aditivo sobre um *scaffold*. A reversão é `git revert` do *commit* correspondente. Dois pontos merecem atenção na aplicação:

1. **Limpeza antes de configurar.** Apagar `konsistTest/build/` e `konsistTest/tmp/` (resíduo de tentativa anterior, com classes compiladas e nenhum fonte) e os `ExampleUnitTest`/`ExampleInstrumentedTest` dos três módulos, antes de ligar G7 — senão a suíte "passa" verificando exemplos de template.
2. **Ordem barata→cara.** A sequência de ativação segue a mesma ordem do comando G8 (Detekt, KtLint, Lint, Konsist, testes), de modo que a cada passo o *feedback loop* mais rápido já esteja protegendo o passo seguinte.

## Open Questions

- Qual nível de log do Ktor em `debug` (`ALL` vs. `HEADERS`)? Não afeta *spec*, *design* nem quebra de tarefas; decide-se ao escrever o `HttpClientFactory`. Em `release` o *plugin* de log fica fora.
- Paleta e tipografia do tema Material 3 (cores dinâmicas ou paleta fixa)? A *spec* exige apenas claro/escuro funcionais; a escolha estética pode mudar depois sem tocar em nenhum contrato.
