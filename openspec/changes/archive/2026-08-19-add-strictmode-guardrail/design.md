## Context

Ver `proposal.md - Why` para a motivação. Pontos de partida relevantes para o "como":

- `DesafioApplication.onCreate` hoje faz uma coisa só: `startKoin { ... }` com o `dataModule` (parametrizado por `CoinMarketCapConfig`) e o `appModule`. É o único ponto do app executado antes de qualquer `Activity`, e portanto o único lugar de onde uma política de *thread* alcança o processo inteiro.
- A mesma classe implementa `SingletonImageLoader.Factory`: o Coil monta o `ImageLoader` preguiçosamente (`newImageLoader`), na *main thread*, quando a primeira imagem é composta. Era o suspeito inicial de violação de disco (diretório de cache) — a execução mostrou que não é ele; ver *Risks / Trade-offs*.
- O projeto já tem precedente exato para "guardrail que não roda no comando de G8": o G9, introduzido pelo change `2026-08-18-add-instrumented-ui-tests`, ganhou requisito próprio e forçou a reescrita completa do requisito de G8. Este change segue a mesma forma.
- `app/src/test/.../di/AppGraphTest.kt` documenta, em comentário, que *"a `Application` real já iniciou o Koin ao subir sob Robolectric"* — ou seja, `onCreate` roda de graça em teste JVM. É o gancho que torna a metade JVM do G10 barata.
- Não há *pipeline* de CI no repositório: G1–G9 são comandos rodados manualmente. G10 segue o mesmo padrão.

## Goals / Non-Goals

**Goals:**
- `StrictMode.ThreadPolicy` instalada no arranque do build de depuração, detectando leitura em disco, escrita em disco e rede na *main thread*, com log e morte do processo.
- G10 como requisito próprio no spec de `architecture-guardrails`, com cenários verificáveis.
- Prova mecânica em duas frentes: teste JVM (dentro de G8) + suíte instrumentada G9 rodando sobre o processo com a política armada.
- Documentação nos dois `AGENTS.md`, no formato já usado por G8 e G9.

**Non-Goals:**
- `StrictMode.VmPolicy` (vazamento de `Activity`, `Closeable` não fechado, `Cursor` aberto, URI `file://` exposta) — YAGNI: nenhum desses problemas apareceu no projeto ainda, e a regra do `AGENTS.md` é que guardrail novo nasce depois do problema concreto.
- Política em builds de release — ver *Decisions*.
- Automação em CI (workflow, runner com emulador) — fora de escopo, assim como já é para G1–G9.
- Reescrever a suíte instrumentada (G9) para "testar StrictMode". G9 não ganha teste novo: ela vira a prova por rodar sobre um processo onde a violação é fatal.

## Decisions

**`penaltyDeath()` e não apenas `penaltyLog()`.**
`penaltyLog` sozinho produz uma linha de Logcat entre milhares e depende de alguém estar olhando na hora certa — na prática, um guardrail que nunca reprova. O `AGENTS.md` já fixa que *falha de guardrail se corrige no código, nunca na configuração*; isso só é aplicável se a falha for inescapável. `penaltyLog` é mantido **junto** com `penaltyDeath` porque é ele que carrega o *stack trace* da violação: sem o log, a morte do processo não diz onde o I/O aconteceu. Alternativa considerada e rejeitada: `penaltyLog` para disco e `penaltyDeath` só para rede — rejeitada por criar duas classes de violação com rigor diferente sem critério objetivo que as separe; a leitura de disco na *main thread* é a causa mais comum de *jank* de rolagem, justamente a que o app tem (listas com imagens).

**Política apenas sob `BuildConfig.DEBUG`.**
Em release, `penaltyDeath` converteria um problema de *performance* em *crash* para o usuário final — troca ruim em qualquer leitura. E a política tem custo de instrumentação por chamada de I/O, que não se justifica em produção. A cláusula é uma guarda de saída no topo da função (`if (!BuildConfig.DEBUG) return`), e não um `if` envolvendo o corpo, para manter o nível de indentação baixo e a leitura linear.

**Instalação antes do `startKoin`, em função privada extraída.**
Antes do `startKoin` porque a montagem do grafo é o primeiro código do projeto a rodar na *main thread* e um candidato natural a I/O acidental (leitura de configuração, cache, preferências) — deixá-la fora da política seria abrir um buraco exatamente onde o guardrail é mais útil. Extraída em `installStrictModeThreadPolicy()` porque o bloco inline empurra `onCreate` para perto do limite de `LongMethod` do Detekt (*threshold* 30, `detekt.yml`) e porque o "por quê" da política precisa de um KDoc que não cabe no meio de `onCreate`.

**Somente `ThreadPolicy`; `VmPolicy` fica para quando houver caso.**
`VmPolicy` detecta outra família de problema (objetos não fechados, vazamento de `Activity`, URI `file://`). O app não tem `SQLite`, não tem `Cursor`, não expõe arquivo por URI, e tem uma única `Activity`. Adicionar `VmPolicy` agora seria defesa especulativa contra um problema que o projeto não tem — exatamente o que o `AGENTS.md` proíbe ao dizer que guardrail novo só nasce depois de o problema aparecer ao menos uma vez.

**Prova mecânica dividida entre G7/G8 (JVM) e G9 (dispositivo), sem duplicar responsabilidade.**
O teste JVM responde *"a política foi instalada?"* — barato, roda em segundos, entra no comando que já é obrigatório no fim de toda feature. A suíte instrumentada responde *"alguém a viola?"* — só um dispositivo real executa o `BlockGuard` nativo que faz `penaltyDeath` valer, então nenhum teste JVM poderia responder isso. Alternativa considerada: um teste instrumentado novo, explicitamente afirmando a política dentro do `ColdStartSmokeTest` — rejeitada porque afirmaria em dispositivo (caro, exige hardware) o mesmo que o teste JVM já afirma de graça, enquanto a parte que só o dispositivo prova já é provada pela suíte inteira passar.

**G8 reescrito em vez de deixado como está.**
O requisito de G8 enumera explicitamente quais guardrails o comando único cobre; a metade JVM do G10 roda dentro dele, então a enumeração passa a mentir se não for atualizada. Na mesma passagem, o comando do cenário é corrigido: ele omite `:app:testDebugUnitTest`, que `AGENTS.md` e `app/AGENTS.md` já listam há tempo — divergência entre spec e documentação que este change encerra em vez de propagar.

## Risks / Trade-offs

- **[Confirmado, não mais hipótese] `detectDiskReads()` + `penaltyDeath()` matou o processo no arranque — e a causa é código do projeto.** Na primeira execução da suíte instrumentada com a política armada, `ColdStartSmokeTest.theLauncherActivityReachesTheResumedState` morreu com `DiskReadViolation`: `koinViewModel()` resolve o grafo durante a composição, na *main thread*; Koin constrói ali o `HttpClient` da API; o `install(Resources)` (`HttpClientFactory.kt:32`) inicializa o `kotlin-reflect`, que lê `META-INF/services` do APK por `ServiceLoader`. Não é ruído de plataforma nem o Coil, como se suspeitava: é a inicialização do `kotlin-reflect` sendo paga na *main thread*, no caminho da primeira tela. Sem G10 isso era apenas *jank* de abertura, invisível para os outros nove guardrails — exatamente a classe de erro que justificou este *change*. → **Encaminhamento**: o plugin `Resources` é usado de verdade (três rotas `@Resource`) e `allowThreadDiskReads` seria maquiagem, porque este I/O *pode* sair da *main thread*. A correção — construção preguiçosa do cliente e IO em dispatcher próprio de `:data` — fixa uma política de dispatcher que o projeto nunca teve, e por isso vive no *change* irmão `fix-http-client-off-main-thread`, com delta próprio em `data-network-foundation`. Esse *change* foi implementado em seguida — cliente preguiçoso e IO em `Dispatchers.IO` na fonte de dados —, e a suíte instrumentada voltou a 43/43 sem nenhuma violação. O guardrail cumpriu exatamente o papel para o qual foi criado: achou, no primeiro uso, um custo de arranque que nenhum dos outros nove enxergava.
- **[Resolvido] Robolectric podia não honrar `StrictMode`**, deixando o teste JVM verde por acidente — um teste que nunca reprova é pior que teste nenhum. → **Verificado**: removida a chamada do `onCreate`, o teste reprova com `expected mask=1342177287 but was mask=0`. O Robolectric faz o *round-trip* exato da máscara, então remover qualquer detecção ou rebaixar a penalidade também reprova. O teste é uma guarda real, não decorativa.
- **[Trade-off] G10 é o único guardrail que não reprova um build sozinho.** G1–G9 falham uma tarefa Gradle; G10 mata um processo. → Aceito: seu poder vem de estar armado dentro do processo que G9 já exercita, e a documentação diz isso explicitamente em vez de sugerir uma equivalência que não existe.
- **[Trade-off] Desenvolver com `penaltyDeath` ligado é mais incômodo que com `penaltyLog`**: o app morre em vez de reclamar. → Aceito deliberadamente; é o incômodo que faz o I/O sair da *main thread* hoje em vez de virar ANR do usuário depois.
