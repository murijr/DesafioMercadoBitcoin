## Context

Motivação e escopo: ver `proposal.md`. Requisitos: ver `specs/`.

O que a base já oferece e restringe esta implementação:

- `UseCase<I, S>` devolve `Result<S>` e re-lança `CancellationException`; `ThrowableToDomainError` traduz `IOException` → `Network` e falhas de `kotlinx.serialization` → `Serialization`, e **tudo mais** → `Unexpected`.
- `HttpClientFactory` é o ponto único de construção do cliente, já com `expectSuccess = true`, `Resources`, `ContentNegotiation` (`ignoreUnknownKeys = true`), *timeout* de 15 s e um `HttpResponseValidator` que converte a exceção em `DomainError`.
- `minSdk = 26`, o que torna `java.time` disponível **sem** *desugaring*.
- Guardrails ativos que moldam o código: Konsist exige prefixo `BM` em `domain/**/model/**`, `DM` em `data/**/{dto,model}/**`, sufixos `Repository`/`RepositoryImpl`/`UseCase`, `ViewModel` cujo construtor só aceita `*UseCase`, `ResourceProvider` ou `SavedStateHandle`, e nenhum literal em argumento de texto de `@Composable`. O Android Lint trata `ContentDescription` e `UnusedResources` como **erro**. Esta mudança acrescenta um *assert* ao G2 (ver D10).

Restrições do provedor, medidas contra a API real (`2026-08-18`):

| Fato | Valor |
|---|---|
| `/v1/exchange/map?listing_status=active` | 968 *exchanges*, 1 crédito, ~200 KB |
| `/v1/exchange/info?id=…` | máx. **100** ids por chamada, 1 crédito, ~360 KB, ~1 s |
| `data` de `map` | **array** JSON |
| `data` de `info` | **objeto** JSON com chave string do id |
| `spot_volume_usd` nulo | ~14% das *exchanges* |
| `date_launched` ausente | ~3% das *exchanges* |
| `sort=volume_24h` no `map` | aceito, mas a ordenação por volume **não** se confirma no resultado |
| Chave inválida / ausente | HTTP **401** |
| Id inexistente em `info` | HTTP **400** |

## Goals / Non-Goals

**Goals:**

- Manter a paginação como detalhe da camada de dados: a apresentação pede "a página N", não conhece o limite de 100 nem a existência de duas consultas.
- Fechar a lacuna de tradução de status HTTP sem que `:domain` ganhe conhecimento de HTTP.
- Introduzir carga de imagem sem trazer uma segunda pilha HTTP para o aplicativo.
- Deixar a casca de navegação pronta para a tela de detalhes completa da mudança seguinte.

**Non-Goals:**

- Cache em disco, banco local ou funcionamento *offline*. Nada é persistido além do ciclo de vida do processo.
- Busca, filtro ou ordenação escolhida pelo usuário.
- Tela de detalhes com conteúdo real (`description`, `urls`, taxas, países) — mudança seguinte.
- Atualização por *pull-to-refresh* ou revalidação temporal do índice.

## Decisions

### D1 — A paginação é contrato de domínio; o índice é memoizado no repositório

`ExchangeRepository` expõe `suspend fun loadPage(page: Int): BMExchangePage`, e `BMExchangePage` carrega `items`, `page` e `hasMore`. `ExchangeRepositoryImpl` obtém o índice na primeira chamada, guarda a lista de ids em memória protegida por `Mutex`, e fatia `ids[page * 100 until (page + 1) * 100]` para alimentar a consulta de conteúdo. O tamanho do lote é constante privada de `:data`, igual ao limite do provedor.

*Por quê*: é a única alocação que mantém as três fronteiras honestas. A apresentação não pode conhecer "100 ids por chamada" — é detalhe de transporte; o domínio não pode conhecer "duas consultas" — é detalhe do provedor. O `Mutex` existe porque o *spec* exige que o índice seja obtido uma única vez mesmo com rolagem rápida disparando páginas em sequência.

*Alternativas descartadas*: (a) dois casos de uso, um para o índice e outro para o conteúdo, com o `ViewModel` fatiando — vaza a regra de lotes para a apresentação e espalha a composição por duas camadas; (b) repositório sem estado, refazendo o `map` a cada página — 10 chamadas extras e 10 créditos desperdiçados por sessão; (c) Paging 3 — dependência grande para um caso em que o índice inteiro já cabe em memória, e sua `PagingSource` não modela bem "um índice + N consultas de conteúdo".

*Consequência*: `ExchangeRepositoryImpl` é `single` no Koin. A memoização morre com o processo, que é exatamente o tempo de vida desejado.

### D2 — Campo indisponível é `null` no modelo de negócio, texto no modelo de apresentação

`BMExchange` declara `spotVolumeUsd: Double?` e `dateLaunched: Instant?`. A decisão de *como* comunicar a ausência é da apresentação, que resolve para um texto localizável.

*Por quê*: o domínio descreve o fato ("o provedor não informou"), não a sua apresentação. Colocar `"—"` ou `0.0` no `BM` destruiria a distinção entre "volume zero" e "volume desconhecido", que o *spec* exige preservar.

### D3 — `java.time` no domínio, sem nova dependência de data

`dateLaunched` é `java.time.Instant`. O `:domain` é Kotlin/JVM puro, então `java.time` não fere o G1 (a proibição é de `android.*`/`androidx.*`), e `minSdk = 26` o torna disponível nas outras camadas sem *desugaring*.

*Alternativas descartadas*: (a) `kotlinx-datetime` — dependência nova sem problema concreto que a justifique (YAGNI); (b) manter a `String` ISO crua e formatar na UI — empurra *parsing* para a apresentação e torna a data intestável no `ViewModel`.

*Cuidado de implementação*: `date_launched` chega como meia-noite **UTC**. A formatação deve converter em `LocalDate` no fuso **UTC** antes de exibir, sob pena de um dia a menos para usuários a oeste de Greenwich.

### D4 — Status HTTP vira `DomainError` no `HttpResponseValidator`, não em `:domain`

`ThrowableToDomainError` permanece como está. A tradução de status entra no `HttpResponseValidator` já existente do `HttpClientFactory`, que passa a inspecionar a `ResponseException` do Ktor antes de delegar:

- `404` → `DomainError.NotFound`
- demais `4xx` (inclui `401` por chave ausente e `429` por limite de taxa) e `5xx` → `DomainError.Network`
- qualquer outra exceção → `toDomainError()` como hoje, preservando o re-lançamento de `CancellationException`

*Por quê*: o *spec* vigente de `data-network-foundation` já promete que a ausência de chave "se manifesta em tempo de execução como falha de rede tratada", e a implementação atual não cumpre — `ClientRequestException` não é `IOException`, então cai em `Unexpected`. A correção pertence a `:data` porque status HTTP é vocabulário de transporte; levá-la para `:domain` exigiria ou uma dependência de Ktor (fere o G1) ou mais checagem por nome de tipo, que já é a parte mais frágil do arquivo.

*Trade-off aceito*: `400` (id inválido) é agrupado em `Network`, o que é semanticamente impreciso. Não há cenário de produto que o distinga hoje, e criar um subtipo `DomainError` para ele seria antecipar necessidade.

### D5 — Um segundo `HttpClient`, sem credencial, para as imagens

`HttpClientFactory` ganha `createImageClient(engine)`: sem `defaultRequest`, sem cabeçalho de chave, sem `HttpResponseValidator` de domínio. Os dois clientes são distinguidos no Koin por qualificador nomeado. O Coil é configurado com `KtorNetworkFetcherFactory` apontando para o cliente de imagens.

*Por quê*: reaproveitar o cliente da API enviaria `X-CMC_PRO_API_KEY` para `s2.coinmarketcap.com` a cada logotipo — vazamento de credencial para um host que não a exige — e faria o `HttpResponseValidator` converter um 404 de imagem em `DomainError`, dentro do Coil, onde ninguém o trata.

*Alternativas descartadas*: (a) `coil-network-okhttp`, o padrão da biblioteca — traz OkHttp como segunda pilha HTTP num projeto que escolheu Ktor deliberadamente; (b) reusar o cliente da API — o vazamento acima.

*Consequência*: o *spec* de `data-network-foundation` passa de "exatamente um cliente" para "exatamente dois, no mesmo ponto único de construção" (ver `specs/data-network-foundation/spec.md`).

### D6 — Navigation 3 com chaves serializáveis e `ViewModel` por entrada da pilha

A pilha de retorno é uma lista observável de `NavKey` (`rememberNavBackStack`), renderizada por `NavDisplay`. As chaves são `@Serializable`: `ExchangeListKey` (`data object`) e `ExchangeDetailKey(exchangeId: Int)`. Os decoradores de `savedState` e de `viewModelStore` dão a cada entrada seu próprio escopo, e `koinViewModel()` resolve dentro dele.

*Por quê a chave carregar só o id*: o *spec* de `app-shell` proíbe transportar modelo de negócio ou de estado no destino. O detalhe busca o que precisa pelo seu próprio `ViewModel`, o que já deixa a mudança seguinte pronta para acontecer sem tocar na navegação.

*Nota para quem implementar*: o Navigation 3 está em `1.2.0-alpha07` e os nomes exatos de decoradores e do `entryProvider` mudaram entre *alphas*. Consultar a *skill* `navigation-3` antes de escrever o `NavDisplay`, em vez de confiar na assinatura citada aqui.

### D7 — O `ViewModel` publica texto pronto; `ResourceProvider` ganha uma sobrecarga

`VMExchange` carrega `volumeLabel: String` e `launchDateLabel: String` já formatados, não `Double?`/`Instant?`. Para isso `ResourceProvider` ganha `fun resolve(@StringRes id: Int): String`, ao lado do `resolve(TextKey)` existente.

*Por quê*: o *spec* de `app-shell` exige que a apresentação publique "um texto já resolvido, e não a chave nem o erro cru", e o guardrail do Konsist proíbe literais em `@Composable`. Formatar no `Composable` com `stringResource` passaria nos guardrails, mas tornaria a formatação — moeda, fuso, ausência — invisível para o teste de `ViewModel`, que é onde ela tem regra.

*Formatação*: volume por `CompactDecimalFormat` em USD (`US$ 5,2 bi`), porque o valor absoluto ocupa a linha inteira do item; data por `DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)` sobre o `LocalDate` em UTC (ver D3).

*Acessibilidade*: o logotipo é decorativo — o nome ao lado já é texto — logo `contentDescription = null`. Isso satisfaz o `ContentDescription` do Lint sem criar literal, que o Konsist reprovaria.

### D8 — Estado da lista acumulado no `ViewModel`, evento único de entrada

O contrato MVI segue o que o `app-shell` já exige: `StateFlow` para estado, `SharedFlow` para efeito de disparo único, e **uma única** abertura de coroutine, no `onEvent`. O estado guarda `items: List<VMExchange>`, `page`, `hasMore`, `isLoadingMore` e `error`, de modo que a falha de um lote posterior seja um campo do estado de conteúdo, e não a substituição do estado por um estado de erro.

*Por quê `SavedStateHandle`*: o *spec* pede que a rolagem e o conteúdo sobrevivam à recriação. `ViewModel` já cobre mudança de configuração; o `SavedStateHandle` cobre morte de processo guardando a **página alcançada**, não os itens — restaurar 900 objetos por `Bundle` estouraria o `TransactionTooLargeException`.

### D9 — Testes sem rede e sem emulador

`MockEngine` do Ktor (já no catálogo) alimenta `HttpClientFactory` com respostas fixas em `:data`; `:domain` testa o caso de uso com repositório `mockk`; `:app` testa `ViewModel` e `Composable` sob Robolectric. Os JSONs de `map` e `info` usados como fixtura vêm da resposta real do provedor, recortados, incluindo **um item sem `spot_volume_usd` e um sem `date_launched`** — os dois casos que o *spec* obriga a tratar e que uma fixtura inventada tenderia a omitir.

### D10 — O prefixo é a única marca de camada no nome

O G2 passa a reprovar classe com prefixo `BM`, `DM` ou `VM` cujo nome termine em `Dto`, `Model`, `Entity`, `Data`, `Payload`, `Body`, `Json` ou `Schema`, em qualquer caixa. A regra vale para os três prefixos, não só para `DM`.

*Por quê*: `DMExchangeDto` declara "camada de dados" duas vezes, e o dia em que os dois rótulos discordarem — `DMExchangeEntity` num projeto sem persistência — o nome passa a mentir. Vale para `BM` e `VM` porque a pressão é a mesma: `VMExchangeUiModel` e `BMExchangeEntity` nascem pelo mesmo reflexo que faria nascer `DMExchangeDto`.

*Fronteira da regra*: sufixo que descreve a **forma** do dado sobrevive. `DMExchangeMapResponse` (o envelope `{ data: [...] }`) e `DMExchangeMapEntry` (o elemento do array) carregam informação que o prefixo não carrega — sem eles, o envelope e o item do mesmo *endpoint* disputariam o mesmo nome. Por isso a regra é uma **lista negada fechada**, e não "nenhum sufixo": a segunda é inverificável, porque nenhum *assert* sabe distinguir sufixo de substantivo composto.

*Alcance*: o *assert* varre a base inteira, não só o código desta mudança. As classes existentes já estão conformes — a regra nasce verde e passa a barrar a próxima violação.

## Risks / Trade-offs

- **Navigation 3 em *alpha*** → A superfície usada é pequena (uma pilha, dois destinos, três decoradores) e fica isolada em `presentation/navigation/`. Uma quebra de API atinge um arquivo, não as telas.
- **Ordem do topo da lista é pouco relevante** — `sort=volume_24h` não ordena de fato, então *exchanges* obscuras aparecem acima da OKX → Trade-off aceito na decisão de escopo, em troca de posições estáveis durante a paginação. Ordenação real exigiria carregar as 968 antes de exibir qualquer coisa. Reversível: com o índice já em memória, ordenar por `spotVolumeUsd` é uma mudança local no `ViewModel`.
- **10 créditos por sessão que role até o fim** → O limite gratuito do provedor é de milhares por mês; o índice memoizado impede o desperdício óbvio. Sem mitigação adicional nesta mudança.
- **Chave ausente produz uma tela de erro logo na abertura** → É o comportamento especificado, e agora com a mensagem certa (D4) em vez de "algo deu errado". O `README`/`AGENTS.md` já documenta `local.properties`.
- **`CompactDecimalFormat` arredonda** — `US$ 5,2 bi` esconde precisão → Aceito para a listagem; a tela de detalhes da mudança seguinte pode exibir o valor integral.
- **A lista negada do D10 é fechada e pode envelhecer** — um rótulo de camada não previsto (`Resource`, `Record`) passaria batido → Aceito: a alternativa inverificável seria pior, e acrescentar um termo à lista é uma linha no *assert*.
- **Remoção de `home_title`/`home_placeholder`** — deixam de ser referenciados quando `HomeScreen` sai, e `UnusedResources` é **erro** no Lint → Removê-los no mesmo passo em que a tela é substituída, não depois.

## Migration Plan

Não há dado persistido, contrato público nem usuário instalado — a mudança é aditiva sobre uma base ainda não publicada. A única substituição é `HomeScreen` (e seu teste) pela tela de listagem, feita em um único passo junto da remoção das *strings* órfãs. Reversão é o `revert` do *commit*.

## Open Questions

- Qual o tamanho de "aproximar-se do fim" que dispara o próximo lote — os últimos 10 itens, 20, ou uma fração da janela? Não altera *spec*, contrato nem tarefas; calibra-se com a lista rolando.
- Se o `info` de um lote falhar de forma persistente, vale pular o lote e seguir para o próximo em vez de parar a paginação? O *spec* exige apenas preservar o já exibido e oferecer nova tentativa; refinar isso é ajuste posterior, com dado de uso real.
