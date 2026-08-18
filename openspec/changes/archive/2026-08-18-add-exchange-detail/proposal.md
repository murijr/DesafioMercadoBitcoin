## Why

A `ExchangeDetailScreen` hoje é um destino mínimo — só ecoa o `exchangeId` recebido — construído deliberadamente assim na mudança anterior (`add-exchange-listing`) para provar que a navegação funciona, com o conteúdo real adiado "para a mudança seguinte". Esta é essa mudança: sem ela, o *drill-down* que o `AGENTS.md` do projeto descreve como propósito do app ("listar *exchanges* e permitir *drill-down* até os ativos negociados em cada corretora") permanece incompleto, e o usuário que toca em um item da listagem não vê nada além do próprio id que já conhecia.

## What Changes

- **Conteúdo real da tela de detalhe da *exchange***: logotipo, nome, id, descrição, *url* do site, taxa de *maker*, taxa de *taker* e data de lançamento — substituindo o placeholder atual.
- **Listagem das moedas negociadas na *exchange***, abaixo do detalhe, com nome e preço em dólar de cada moeda.
- **Reaproveitamento do endpoint `/v1/exchange/info`** para o detalhe: o *envelope* já é consultado pela listagem; o modelo de transporte ganha os campos que hoje chegam e são ignorados (`description`, `urls.website`, `maker_fee`, `taker_fee`), sem nova rota.
- **Novo endpoint `/v1/exchange/assets`** para a listagem de moedas, com novo modelo de transporte, novo modelo de domínio (`BMCurrency`) e nova consulta de repositório.
- **Campo ausente continua caminho normal, não erro** — mesma regra da listagem (D2 de `add-exchange-listing`): o domínio carrega `null`, a apresentação resolve para texto localizável de indisponibilidade.
- **Duas cargas independentes na mesma tela**: a falha em obter as moedas não impede a exibição do detalhe já obtido, e vice-versa quando aplicável.

## Capabilities

### New Capabilities

- `exchange-detail`: o que a tela de detalhe de uma *exchange* exibe — identificação, descrição, *site*, taxas, data de lançamento e a listagem de moedas negociadas —, como cada campo ausente é apresentado, e o que acontece em carregamento, falha e nova tentativa para o detalhe e para a listagem de moedas, independentemente.

### Modified Capabilities

Nenhuma. `app-shell` já descreve destino parametrizado com `ViewModel` de escopo próprio de forma genérica; `data-network-foundation` já descreve cliente único, lote respeitando limite do provedor e tradução de status HTTP de forma genérica — nenhum requisito de nível de especificação muda, só a implementação ganha um novo modelo de transporte e uma nova rota.

## Impact

**Código**

- `:domain` — `BMExchangeDetail` (ou extensão equivalente do modelo de negócio de *exchange*), `BMCurrency`, `ExchangeDetailRepository` (contrato), `GetExchangeDetailUseCase`, `GetExchangeCurrenciesUseCase`.
- `:data` — `DMExchangeInfo` ganha os campos novos (`description`, `urls`, `maker_fee`, `taker_fee`); novos `DMExchangeAssetsResponse`/`DMExchangeAsset`/`DMCurrency`; nova rota `ExchangeAssetsRoute` (`/v1/exchange/assets`); `ExchangeRemoteDataSource` ganha a consulta de *assets*; `ExchangeDetailRepositoryImpl` nova, ao lado da `ExchangeRepositoryImpl` existente.
- `:app` — `ExchangeDetailScreen` deixa de ser placeholder; ganha `ExchangeDetailViewModel`, contrato MVI (`Event`/`State`, e `Effect` só se necessidade concreta aparecer), mapeadores `BM.toVM()`, componentes de Compose para o cabeçalho de detalhe e para o item de moeda, e *strings* localizáveis novas. `exchange_detail_title` (placeholder atual) é removida.

**Dependências novas**: nenhuma — reaproveita Ktor, Coil e Navigation 3 já introduzidos pela mudança anterior.

**Guardrails**: nenhum guardrail novo previsto; os modelos novos seguem os prefixos e sufixos que G2 já verifica.

**Configuração**: nenhuma mudança — mesma chave de API, mesmo cliente autenticado.
