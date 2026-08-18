## Why

A base do projeto está pronta (contrato de `UseCase`, `DomainError`, cliente Ktor, grafo Koin, guardrails G1–G8), mas nenhum dado de produto circula por ela: o app abre numa `HomeScreen` de placeholder. Esta mudança entrega a **primeira funcionalidade real do desafio** — a listagem de *exchanges* da CoinMarketCap — e com ela valida de ponta a ponta as fundações que hoje só existem como contrato testado em isolamento.

O escopo foi deliberadamente cortado na **listagem**. A tela de detalhes completa (descrição, *urls*, taxas, países/moedas) fica para uma mudança seguinte; aqui ela existe apenas como destino mínimo, o suficiente para provar que a navegação funciona e o item selecionado chega do outro lado.

## What Changes

- **Nova tela de listagem de *exchanges*** exibindo, por item, `logo`, `name`, `spot_volume_usd` e `date_launched`, com estados de carregamento, erro e lista vazia.
- **Composição de dois endpoints**: `/v1/exchange/map` fornece o índice de *exchanges* ativas (968 hoje); `/v1/exchange/info` fornece o conteúdo exibível (logo, volume, data de lançamento), aceitando no máximo **100 `id` por chamada**. Nenhum dos dois isoladamente atende à tela.
- **Paginação incremental por rolagem**: o índice é buscado uma vez; o conteúdo entra em lotes de 100 conforme o usuário rola. A ordem devolvida pelo `map` é preservada.
- **Campo ausente é caminho normal, não erro**: sondagem da API real mostra `spot_volume_usd` nulo em ~14% das *exchanges* e `date_launched` ausente em ~3%. O item renderiza com marcador de indisponível em vez de sumir ou quebrar.
- **Navegação com pilha de retorno** entre listagem e um destino de detalhe mínimo, introduzindo no `:app` a casca de navegação que hoje não existe.
- **Carga remota de imagem** para o logo, hoje inexistente no *stack*.
- **Correção de tradução de falha HTTP**: status de erro (401 por chave ausente, 429 por limite de taxa, 5xx) hoje atravessa como `DomainError.Unexpected`. O *spec* vigente de `data-network-foundation` já determina que a ausência de chave "se manifesta em tempo de execução como falha de rede tratada" — a implementação atual não cumpre isso, porque `toDomainError` só reconhece `IOException`.

## Capabilities

### New Capabilities

- `exchange-listing`: o que a tela de listagem exibe, como o índice e o conteúdo se compõem em uma lista paginada, como um campo ausente é apresentado, e o que o usuário observa em carregamento, falha, lista vazia e nova tentativa.

### Modified Capabilities

- `data-network-foundation`: (1) a construção de cliente HTTP deixa de ser exatamente uma e passa a ser exatamente duas — o cliente autenticado da API e o cliente de imagens **sem** credencial —, ambas no mesmo ponto único de configuração, com a garantia explícita de que a chave de API nunca acompanha requisição a host de imagem; (2) a conversão de falha ganha requisito sobre **status HTTP de erro**, hoje ausente do *spec* e por isso implementado como erro genérico.
- `architecture-guardrails`: a verificação estática de nomenclatura (G2) ganha a proibição de **rótulo de camada no fim do nome** de classes com prefixo `BM`, `DM` ou `VM`. O prefixo já declara a camada; `DMExchangeDto` a declara duas vezes. Sufixo que descreve a forma do dado (envelope, elemento) continua permitido.
- `app-shell`: a casca passa a hospedar mais de um destino com pilha de retorno, e o destino inicial passa a ser a listagem em vez do placeholder. O *spec* atual só descreve "uma tela Compose".

## Impact

**Código**

- `:domain` — modelo de negócio da *exchange*, contrato de repositório e caso de uso de listagem paginada. Primeiro uso concreto do `UseCase<I, S>` com entrada não trivial.
- `:data` — modelos de transporte dos dois endpoints, fonte de dados remota, implementação de repositório com a composição `map` + `info` e o mapeamento de sentido único; ajuste em `ThrowableToDomainError` e no cliente de imagens.
- `:app` — tela de listagem, `ViewModel` com contrato MVI, casca de navegação, mapeamento para modelo de apresentação (formatação de moeda e data), *strings* localizáveis. `HomeScreen` é substituída.

**Dependências novas** (nenhuma no *stack* atual atende)

- `io.coil-kt.coil3:coil-compose` + `coil-network-ktor3` `3.5.0` — carga e cache do logo reaproveitando Ktor, sem arrastar OkHttp como segunda pilha HTTP.
- `androidx.navigation3:navigation3-runtime` / `navigation3-ui` `1.2.0-alpha07` e `androidx.lifecycle:lifecycle-viewmodel-navigation3` `2.11.0`. **Risco assumido**: o Navigation 3 ainda está em *alpha*; a API pode mudar antes do estável.

**Guardrails** — o G2 ganha um *assert* novo (rótulo de camada no fim do nome), aplicado a toda a base e não só ao código desta mudança. Os demais permanecem: as regras de prefixo não alcançam chaves de rota, e a *keep rule* do G5 já é enunciada de forma genérica, apenas ganhando entradas para Coil, Navigation 3 e as novas classes `@Serializable`.

**Configuração** — a chave de API continua vindo de `local.properties` (não versionado) ou de `CMC_API_KEY`. Nenhum valor real de chave entra no repositório.
