## Context

Ver `proposal.md - Why` para a motivação. Pontos de partida relevantes para o "como":

- `ExchangeListScreen` e `ExchangeDetailScreen` já são *stateless*: recebem `VMState` + *callback* de evento, sem `ViewModel` nem grafo de dependências (comentário no próprio arquivo: "Tela sem estado próprio: recebe o que renderizar e devolve o que o usuário fez. É o que a torna testável sem `ViewModel` e sem grafo de dependências."). Os testes JVM existentes (`ExchangeListScreenTest`, `ExchangeDetailScreenTest`) exploram exatamente essa propriedade: renderizam a tela com um `VMState` fixo via `createComposeRule` + `RobolectricTestRunner`, sem tocar rede.
- A navegação ("tocar em um item abre o detalhe") não pertence a `ExchangeListScreen` — pertence à casca (`AppNavigation`), testada hoje por `AppNavigationTest`, que registra `mockk` dos *use cases* em um módulo Koin de teste e observa a pilha de navegação.
- Todo elemento relevante já expõe `testTag` estável (`TAG_INITIAL_LOADING`, `TAG_LIST`, `TAG_DETAIL_BACK`, etc.) — a suíte JVM já depende deles, então o requisito de identificadores estáveis do capability `instrumented-ui-testing` já está satisfeito pela base de código atual, sem trabalho extra.
- `app/build.gradle.kts` já declara `androidTestImplementation` para `espresso-core`, `androidx-junit`, `androidx-tracing` e `koin-core` (usados por `ColdStartSmokeTest`); falta apenas `ui-test-junit4`, hoje só em `testImplementation`.
- Não há *pipeline* de CI no repositório — G7/G8 também são hoje comandos rodados manualmente, não automatizados. G9 segue o mesmo padrão.

## Goals / Non-Goals

**Goals:**
- Suíte instrumentada nova em `app/src/androidTest`, espelhando 1:1 a estrutura e os casos já cobertos pelos testes JVM de tela (`ExchangeListScreenTest`, `ExchangeDetailScreenTest`) e de navegação (`AppNavigationTest`), mas executando sobre compositor e ciclo de vida reais.
- Testes de componente isolado (`ExchangeListItem`, `ExchangeDetailHeader`, `CurrencyListItem`) sem montar a tela inteira.
- G9 como guardrail bloqueante próprio, documentado em G8 como segunda frente de execução.

**Non-Goals:**
- Automação em CI (criação de *workflow*, *runner* com emulador) — fora de escopo, assim como já é para G1–G8 hoje.
- Testes de integração real com rede (MockWebServer, servidor de teste) — a suíte instrumentada usa a mesma estratégia de duplos de teste (`mockk` de *use case* + Koin de teste) que a suíte JVM já usa; não introduz uma segunda estratégia de teste.
- Cobertura de código (`enableAndroidTestCoverage`) — já está `false` (linha 56 de `app/build.gradle.kts`) e este *change* não altera isso.

## Decisions

**Espelhar a suíte JVM em vez de escrever cenários novos do zero.**
Os testes de tela e de navegação já existentes descrevem exatamente os cenários que a versão instrumentada precisa verificar; a única variável que muda é o ambiente de execução (dispositivo real vs. sombra JVM). Reescrever os mesmos cenários do zero arriscaria divergência entre as duas suítes. Alternativa considerada — escrever cenários próprios e menores para a suíte instrumentada, cobrindo só "os casos mais críticos" — rejeitada porque criaria uma noção ambígua de "crítico" sem contrato claro, e o objetivo do change é paridade de confiança entre as duas execuções, não uma amostra.

**Duplos de teste idênticos aos já usados na suíte JVM (`mockk` de *use case*, Koin de teste), portados para `androidTest`.**
`ExchangeListScreenTest`/`ExchangeDetailScreenTest` não usam duplo nenhum (são *stateless*, recebem `VMState` direto); `AppNavigationTest` usa `mockk` dos três *use cases* de domínio via módulo Koin de teste. A suíte instrumentada reaproveita a mesma divisão: testes de tela/componente recebem `VMState` fixo diretamente; o teste de navegação instrumentado usa os mesmos duplos `mockk` + Koin de teste que já rodam em `androidTest` no `ColdStartSmokeTest`. Alternativa considerada — subir um servidor HTTP de teste (MockWebServer) para exercitar a pilha de rede real no dispositivo — rejeitada por não ser necessária: G9 verifica que a *UI* se comporta corretamente para um dado estado/resultado de *use case*, não que a camada de rede funciona (isso já é responsabilidade de `:data` e dos testes de `ExchangeRemoteDataSource`).

**`androidTestImplementation(libs.androidx.compose.ui.test.junit4)` adicionado ao lado das dependências `androidTest` já existentes.**
A biblioteca já está no catálogo de versões e já é usada em `testImplementation`; só falta declará-la também para `androidTest`. `debugImplementation(libs.androidx.compose.ui.test.manifest)` já cobre a variante `debug`, que é a que `connectedDebugAndroidTest` executa.

**G9 como guardrail separado de G7/G8, não fundido ao comando único existente.**
G7/G8 descrevem explicitamente uma suíte que roda "sem emulador ou dispositivo" — fundir um guardrail que *precisa* de dispositivo nesse mesmo comando quebraria essa garantia para quem depende dela (por exemplo, rodar G1–G8 em uma máquina sem emulador configurado). Por isso G9 ganha seu próprio comando (`:app:connectedDebugAndroidTest`) e G8 é reescrito para descrever duas frentes obrigatórias em vez de uma.

## Risks / Trade-offs

- **[Risco] Testes instrumentados são mais lentos e mais frágeis (dependem de emulador/dispositivo real, boot, animações) que os equivalentes em JVM** → Mitigação: escopo restrito aos mesmos cenários já provados estáveis em JVM (nenhum cenário novo "só porque é instrumentado"); reaproveita os `testTag`s já estáveis, evitando localização por texto ou por posição.
- **[Risco] G9 exige dispositivo/emulador conectado para ser verificado, o que não é automatizável sem CI** → Mitigação: aceito como *non-goal* deste *change* (mesma situação de G1–G8 hoje); G9 documenta a exigência explicitamente (`Ausência de dispositivo não dispensa a suíte`) em vez de silenciosamente permitir pular a verificação.
- **[Trade-off] Duplicação de cenários entre suíte JVM e suíte instrumentada** → Aceito deliberadamente: o valor de G9 é confirmar, em ambiente real, o que G7 já confirma em sombra — divergência entre as duas é o próprio sinal que a suíte instrumentada existe para capturar.
