## Context

Motivação e escopo: ver `proposal.md`. Requisitos: ver `specs/exchange-detail/spec.md`.

O que a mudança anterior (`add-exchange-listing`) já deixou pronto e que esta mudança reaproveita:

- `ExchangeInfoRoute` (`/v1/exchange/info?id=…`) já é consultado pela listagem e devolve, por *exchange*, um objeto JSON bem mais rico do que o `DMExchangeInfo` atual mapeia — `ignoreUnknownKeys = true` faz `description`, `urls`, `maker_fee` e `taker_fee` chegarem hoje e serem silenciosamente descartados.
- `ExchangeDetailKey(exchangeId: Int)` já navega até `ExchangeDetailScreen`, que hoje só ecoa o id recebido como parâmetro direto do Composable — não via `ViewModel`.
- `ExchangeRemoteDataSource` já centraliza IO de *exchange*; `HttpClientFactory` já entrega o cliente autenticado e o cliente de imagens; `ThrowableToDomainError` e o `HttpResponseValidator` já traduzem status HTTP para `DomainError` (`404` → `NotFound`, demais `4xx`/`5xx` → `Network`).
- O padrão MVI do `ExchangeListViewModel` — `send()` como único ponto de abertura de coroutine, estado acumulado em vez de substituído em falha parcial — é o modelo a seguir para as duas cargas independentes desta tela.

Fato novo, medido contra a API real (`2026-08-18`), sobre o endpoint introduzido nesta mudança:

| Fato | Valor |
|---|---|
| `/v1/exchange/assets?id=…` | 1 crédito por chamada, devolve **todas** as moedas da *exchange* em uma única resposta — sem paginação do provedor |
| `data` de `assets` | **array** JSON, cada item com `currency: { name, price_usd, … }` entre outros campos ignorados |
| Id inexistente | mesmo comportamento de `info`: HTTP **400**, hoje traduzido para `DomainError.Network` (trade-off já aceito em D4 de `add-exchange-listing`) |

## Goals / Non-Goals

**Goals:**

- Preencher o conteúdo real do detalhe reaproveitando o endpoint já consultado pela listagem, sem nova rota nem nova credencial.
- Modelar a listagem de moedas como uma segunda fonte independente, cuja falha não derruba o detalhe já obtido.
- Levar o `ExchangeDetailScreen` para o mesmo padrão MVI (`ViewModel` + `UseCase`) que a listagem já estabeleceu, encerrando a exceção documentada na mudança anterior.

**Non-Goals:**

- Paginação ou rolagem incremental da listagem de moedas — o provedor devolve o conjunto inteiro em uma resposta, então não há "próximo lote" a buscar (ver tabela acima).
- Abrir a *url* do site em navegador ou *custom tab* — a tela exibe o texto do link; a ação de abrir fica para uma mudança futura caso o produto peça.
- Cache em disco, *pull-to-refresh* ou revalidação temporal — mesma fronteira que a listagem já assumiu.
- Qualquer outro campo do `info` ou do `assets` que o provedor devolva além dos listados no *spec* (ex.: `notice`, `countries`, `balance` por carteira) — fora do pedido desta mudança.

## Decisions

### D1 — O detalhe reaproveita `/v1/exchange/info`; `DMExchangeInfo` ganha os campos, um novo `BM` nasce ao lado do existente

`DMExchangeInfo` ganha `description: String?`, `urls: DMExchangeUrls?` (com `website: List<String>` — o provedor devolve lista, a tela usa o primeiro item não vazio), `makerFee: Double?` e `takerFee: Double?`. Um novo modelo de domínio, `BMExchangeDetail`, carrega todos os campos que a tela de detalhe precisa (incluindo os que `BMExchange` já tem — id, name, logoUrl, dateLaunched); um novo mapeamento `DMExchangeInfo.toDetailBM(): BMExchangeDetail` convive com o `toBM(): BMExchange` existente, sem que um chame o outro.

*Por quê*: o *envelope* e a consulta já existem — criar uma segunda chamada ao mesmo *endpoint* para os mesmos dados desperdiçaria crédito e lote. Dois `BM` distintos (em vez de acrescentar os campos novos a `BMExchange`) porque a listagem e o detalhe têm necessidades de dado diferentes e o *spec* de `exchange-listing` já fecha o contrato de `BMExchange` — inflar esse modelo com campos que a listagem nunca lê violaria a coesão que `ISP` pede no `AGENTS.md` raiz, e o mapper de sentido único (`data/AGENTS.md`) já pressupõe um `DM` alimentando um `BM` por vez, não um `BM` condicional.

*Alternativas descartadas*: (a) acrescentar os campos a `BMExchange` e deixá-los `null` na listagem — a listagem passaria a carregar dado que nunca usa, e o `ViewModel` da listagem ganharia código morto de formatação; (b) uma segunda consulta dedicada a `info` só para o detalhe — mesma URL, mesmo *payload*, sem ganho, com um crédito a mais por abertura de tela.

### D2 — Novo `endpoint` `/v1/exchange/assets` para a listagem de moedas, com `BMCurrency` próprio

Nova rota `ExchangeAssetsRoute` (`/v1/exchange/assets`, parâmetro `id`), novo envelope `DMExchangeAssetsResponse { data: List<DMExchangeAsset> }`, `DMExchangeAsset { currency: DMCurrency }`, `DMCurrency { name: String, priceUsd: Double? }`. Mapeamento de sentido único `DMExchangeAsset.toBM(): BMCurrency`. `ExchangeRemoteDataSource` ganha `loadAssets(id: Int): List<DMExchangeAsset>`, ao lado de `loadActiveIndex()`/`loadInfo()` já existentes.

*Por quê um `DataSource` só*: `loadAssets` é IO sobre o mesmo cliente autenticado, do mesmo pacote `data/exchange/`, sem estado compartilhado com o índice memoizado da listagem — dividir em duas classes só multiplicaria *binding* de DI sem separar responsabilidade real.

*Por quê `BMCurrency` e não reaproveitar algo existente*: não há modelo de moeda no projeto hoje; `AGENTS.md` já antecipa `BMCurrency` como exemplo de nome de modelo de domínio, então o nome não é novo mesmo sendo a primeira aparição da classe.

*Correção encontrada ao vivo*: o provedor devolve `/v1/exchange/assets` por **carteira**, não por moeda — a mesma moeda aparece uma vez por carteira que a possui (reproduzido contra a API real: `USDD` em mais de uma carteira de uma mesma *exchange*). `ExchangeDetailRepositoryImpl.loadCurrencies` aplica `distinctBy { it.name }` sobre o resultado mapeado, reduzindo para "a *exchange* negocia esta moeda" — o que o *spec* já pede ("a listagem exibe as moedas negociadas", não "uma linha por carteira"). Sem essa redução, o nome duplicado quebra a chave que o `LazyColumn` da apresentação exige por item.

### D3 — `ExchangeDetailRepository` novo, com dois métodos independentes

Contrato:

```kotlin
interface ExchangeDetailRepository {
    suspend fun loadDetail(exchangeId: Int): BMExchangeDetail
    suspend fun loadCurrencies(exchangeId: Int): List<BMCurrency>
}
```

`ExchangeDetailRepositoryImpl` implementa os dois métodos delegando a `ExchangeRemoteDataSource` (reaproveitado da listagem) e aplicando o mapper de cada um. Dois `UseCase` — `GetExchangeDetailUseCase` e `GetExchangeCurrenciesUseCase` — cada um chamando um método.

*Por quê um repositório com dois métodos, e não dois repositórios*: os dois métodos servem a mesma tela, o mesmo `exchangeId`, e nenhum dos dois tem estado a manter (ao contrário de `ExchangeRepositoryImpl`, que memoiza o índice) — não há razão concreta para a divisão, e o `AGENTS.md` de `:domain` só pede interface pequena e coesa, não uma interface por método. Dois `UseCase`, e não um só devolvendo uma tupla: preserva o paralelismo de D5 e o tratamento de erro independente que o *spec* exige — um único `UseCase` juntando as duas chamadas obrigaria escolher qual falha "vence" o `Result`.

*Alternativas descartadas*: estender `ExchangeRepository` (o da listagem) com os dois métodos novos — misturaria o contrato memoizado e paginado da listagem com consultas pontuais por id, ferindo a coesão que o próprio `ExchangeRepository` já tem hoje.

### D4 — Campo ausente é `null` no `BM`, texto no `VM` — mesma regra de D2 em `add-exchange-listing`

`BMExchangeDetail.description`, `.websiteUrl`, `.makerFee`, `.takerFee`, `.logoUrl` e `.dateLaunched` são anuláveis; `BMCurrency.priceUsd` é anulável. A resolução para texto localizável de indisponibilidade acontece no mapeamento `BM.toVM()`, nunca no `Composable` nem no domínio — mesma divisão de responsabilidade já estabelecida.

*Por quê repetir a decisão em vez de generalizar*: o *spec* já pede a mesma regra para seis campos diferentes; criar uma abstração ("campo opcional formatável") antes de ela se repetir em uma terceira *feature* seria antecipar necessidade (YAGNI, `AGENTS.md` raiz).

### D5 — As duas cargas iniciam em paralelo no mesmo evento de abertura

`ExchangeDetailViewModel.send(ScreenOpened)` dispara as duas buscas dentro do único `launch` do `send()`, usando `coroutineScope { launch { … }; launch { … } }` (ou `async`/`awaitAll`) para que a falha ou a demora de uma não atrase a outra. Cada busca atualiza seu próprio recorte do `State` (`detail`/`detailError`/`isLoadingDetail` de um lado, `currencies`/`currenciesError`/`isLoadingCurrencies` do outro) de forma independente.

*Por quê*: o *spec* exige que a exibição de uma não espere a outra; buscá-las em sequência dentro do mesmo `launch` faria a segunda esperar a primeira terminar sem necessidade, já que não há dependência de dado entre elas.

*Cuidado de implementação*: `CancellationException` de uma coroutine filha cancelada não deve escapar sem re-lançamento, mas isso já é responsabilidade do `UseCase.execute()` existente — nenhum tratamento novo é necessário aqui além do padrão já estabelecido.

### D6 — `ExchangeDetailScreen` passa a resolver `ViewModel`; a tela deixa de receber `exchangeId` como parâmetro de Composable

O destino `entry<ExchangeDetailKey>` continua carregando só o id (`app-shell` já exige isso). `ExchangeDetailRoute` recebe `exchangeId: Int` como parâmetro comum de `Composable` — não do `ViewModel` — e o repassa para dentro do `ViewModel` chamando `ExchangeDetailViewModel.ensureExchangeId(id)` de um `LaunchedEffect`, antes de `send(ScreenOpened)`. O método grava o id no `SavedStateHandle` só se ainda estiver ausente, o que o torna seguro contra recomposição e coerente com a restauração após morte do processo (o `NavKey` recriado carrega o mesmo id).

*Nota de implementação verificada*: a tentativa inicial construía um `SavedStateHandle` fora do `ViewModel` e o injetava via `koinViewModel { parametersOf(handle) }`, esperando que o `get()` posicional do módulo Koin o resolvesse no lugar do `SavedStateHandle` de plataforma. Em teste de integração (`AppNavigationTest`) isso **não** se confirmou: `koinViewModel()` (a integração Compose) prioriza o `SavedStateHandle` real da plataforma para esse tipo, ignorando o valor de `parametersOf`, e o `ViewModel` recebia o id ausente. A alternativa acima — `Composable` comum + método idempotente no `ViewModel` — não depende desse comportamento interno do Koin e foi a que os testes confirmaram funcionar.

### D7 — Sem `Effect` nesta tela

`ExchangeDetailEvent` (`ScreenOpened`, `RetryDetailRequested`, `RetryCurrenciesRequested`) e `ExchangeDetailState` existem; um `ExchangeDetailEffect` sealed não é criado nesta mudança porque não há evento de disparo único a comunicar — não há navegação adiante nem *snackbar* previstos no *spec*. O retorno à listagem é o botão de voltar do sistema, já coberto por `app-shell`.

*Por quê registrar isso como decisão*: o layout padrão de *feature* em `app/AGENTS.md` lista `<Feature>Effect.kt` como parte do conjunto de arquivos; omiti-lo é um desvio deliberado, não um esquecimento, e fica documentado para quem ler a estrutura de pastas depois. Se uma necessidade concreta de efeito aparecer (ex.: abrir o site em *custom tab*, adiado por Non-Goals), o arquivo nasce naquele momento.

### D8 — Formatação: taxas como percentual, preço de moeda como moeda integral

`makerFee`/`takerFee` chegam como fração (`0.001` = 0,1%) e são formatados com `NumberFormat.getPercentInstance()` da localidade do dispositivo. `priceUsd` é formatado por `NumberFormat.getCurrencyInstance(Locale.US)` sem casas compactadas — ao contrário do volume da listagem (`CompactDecimalFormat`, D7 de `add-exchange-listing`), o preço unitário de uma moeda precisa da precisão integral para não confundir "US$ 0" com "indisponível" em moedas de preço fracionário pequeno.

### D9 — `description` renderiza como Markdown, com a biblioteca `multiplatform-markdown-renderer-m3`

O campo `description` chega do provedor em Markdown (títulos `##`, links `[texto](url)`, ênfase) — confirmado ao vivo contra a API real (ver Seção 10 de `tasks.md`), não documentado no *spec* original porque só ficou evidente depois da primeira execução contra dados reais. `ExchangeDetailHeader` passa a renderizar `descriptionLabel` com `Markdown(content = ...)` de `com.mikepenz.markdown.m3`, em vez de `Text(...)`.

*Por quê uma biblioteca, e não um *parser* próprio*: a escolha foi posta ao usuário explicitamente — um conversor Markdown→`AnnotatedString` em Kotlin puro evitaria dependência nova, mas cobriria só os construtos antecipados (títulos, links, ênfase, parágrafos); qualquer coisa fora disso (tabela, lista numerada, *nested* blockquote) cairia como texto cru de novo, o mesmo problema que motivou a mudança. O usuário escolheu a biblioteca pela cobertura completa de CommonMark/GFM e por já vir com estilo Material 3 pronto (`markdownColor()`/`markdownTypography()` herdam do `MaterialTheme` ambiente, sem código de tema adicional).

*Por quê `multiplatform-markdown-renderer` (Mike Penz), e não *compose-richtext**: ambas são opções maduras; a do Mike Penz foi escolhida por já publicar um módulo `-m3` dedicado (estilo Material 3 pronto, sem adaptação) e por já compartilhar o padrão de biblioteca *Compose Multiplatform* consumida por um app Android puro — o mesmo padrão de resolução de variante via metadata Gradle que o Coil3 já usa neste projeto (`add-exchange-listing`, D5).

*Versão fixada em `0.39.2`, não a mais recente (`0.44.0`)*: dois limites já existentes no projeto, verificados nesta mudança:
1. **Metadata do Kotlin** — `app/build.gradle.kts` já documenta o teto de `kotlin-stdlib` legível pelo compilador Kotlin 2.2.10 do projeto (`maxReadableKotlinStdlib = "2.3.21"`, forçado via `resolutionStrategy`). Versões `0.42.0`+ da biblioteca declaram `kotlin-stdlib 2.4.x`, acima desse teto.
2. **Versão de bytecode JVM** — a partir de `0.40.0`, o artefato Android (`multiplatform-markdown-renderer-m3-android`) passa a compilar para *class file version* 65 (Java 21); o módulo `:app` declara `sourceCompatibility`/`targetCompatibility = VERSION_17` (*class file version* 61), e o JDK que executa o Gradle nesta sessão é o 17 — carregar uma classe versão 65 falha com `UnsupportedClassVersionError`, reproduzido ao vivo em `ExchangeDetailScreenTest` antes da fixação da versão.

`0.39.2` é a versão mais recente que respeita os dois tetos ao mesmo tempo (confirmado inspecionando o `.class` compilado de cada versão candidata, não só a *stdlib* declarada no POM — a primeira checagem, só pela *stdlib*, teria escolhido `0.41.0` e ainda assim quebrado pelo segundo limite).

*Escopo*: só os módulos `multiplatform-markdown-renderer` (núcleo) e `-m3` (estilo) entram — nenhum carregamento de imagem embutida no Markdown (`-coil3`) nem destaque de sintaxe de código (`-code`), porque a descrição da CoinMarketCap não usa nenhum dos dois construtos nas amostras observadas (YAGNI).

*Consequência para links internos da descrição*: `Markdown()` torna `[texto](url)` clicável por padrão, abrindo o link no navegador do sistema. Isso é comportamento de biblioteca, não uma funcionalidade construída por esta mudança — continua distinto do Non-Goal "abrir o *site* (`websiteLabel`) em navegador", que seguia deliberadamente fora de escopo e continua.

## Risks / Trade-offs

- **Nova dependência externa para um único campo de texto** — `multiplatform-markdown-renderer` + `-m3` entram no `:app` só para `description`. Aceito porque o *parser* próprio foi posto como alternativa e recusado pelo usuário em favor de cobertura completa (D9); o risco de manutenção fica com uma biblioteca de terceiros ativa (última publicação: hoje, `2026-08-18`) em vez de código próprio.
- **Versão da biblioteca presa em `0.39.2`** — futuras atualizações do Kotlin do projeto (para além de `2.3.21` de *stdlib* legível, ou do `targetCompatibility` além de Java 17) destravariam versões mais novas; até lá, correções e recursos posteriores da biblioteca não chegam ao projeto sem revisitar essa decisão.

- **`/v1/exchange/assets` sem paginação confirmada em contrato público** — a medição de `2026-08-18` não achou lote nem `next`, mas a documentação do provedor não garante isso para toda *exchange*. Se uma *exchange* grande devolver uma lista muito extensa, a lista renderiza inteira sem virtualização especial além da já usada em `LazyColumn`. Sem mitigação nesta mudança; reversível como ajuste local se aparecer um caso real.
- **`400` de id inexistente cai em `DomainError.Network`, não `NotFound`** — mesmo trade-off aceito em D4 de `add-exchange-listing`, herdado por reaproveitar o mesmo `HttpResponseValidator`. O *spec* desta mudança inclui um cenário de "*exchange* inexistente" com `NotFound`; hoje a API real não permite diferenciar sem uma mudança em `data-network-foundation` fora do escopo proposto. **Decisão**: tratar como *open question* abaixo, não como bloqueio — a distinção exigiria a API devolver um sinal que `400` genérico não carrega.
- **Duas chamadas de rede por abertura de tela (`info` + `assets`)** — 2 créditos por visita ao detalhe, contra 1 da listagem por lote. Aceito: é o mínimo necessário para os dois conjuntos de dados que o *spec* pede, e ambos já respeitam o crédito único por consulta do provedor.
- **`urls.website` como lista, tela mostra só o primeiro item** — perde *urls* adicionais que o provedor eventualmente forneça (rede social, *blog*). Aceito porque o *spec* pede apenas "a *url* do site", no singular.

## Migration Plan

Aditiva sobre uma base ainda não publicada — sem dado persistido nem contrato externo. A única substituição é o conteúdo de `ExchangeDetailScreen` (e seu teste), feita em um único passo junto da remoção de `exchange_detail_title`. Reversão é o `revert` do *commit*.

## Open Questions

- Como distinguir "*exchange* inexistente" (`NotFound`) de outra falha `400` sem que `:domain` aprenda HTTP — herdar o trade-off de `add-exchange-listing` aqui, ou ampliar `data-network-foundation` para inspecionar o corpo do erro do provedor? Não é decidido nesta mudança; o *spec* mantém o cenário descrito e a implementação decide na hora se o alcança com o `HttpResponseValidator` atual ou se registra a limitação como o D4 anterior já fez.
- `urls.website` vem vazio (`[]`) ou ausente quando a *exchange* não informa site — o *fixture* de teste precisa cobrir os dois formatos, mas isso não muda o *spec* nem a decisão de D1.
