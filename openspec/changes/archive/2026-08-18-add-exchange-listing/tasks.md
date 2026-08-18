## 1. Dependências e catálogo

- [x] 1.1 Adicionar ao `gradle/libs.versions.toml` as versões `coil = "3.5.0"`, `navigation3 = "1.2.0-alpha07"` e as bibliotecas `coil-compose`, `coil-network-ktor3`, `androidx-navigation3-runtime`, `androidx-navigation3-ui`, `androidx-lifecycle-viewmodel-navigation3` (esta usando o `lifecycle = 2.11.0` já existente)
- [x] 1.2 Declarar as cinco bibliotecas em `app/build.gradle.kts`; `coil-network-ktor3` fica em `:app`, e nenhuma delas entra em `:domain` ou `:data`
- [x] 1.3 Confirmar por `./gradlew :app:dependencies` que **nenhuma** pilha HTTP além do Ktor entrou no grafo (cenário "Biblioteca de imagem sem pilha própria")
- [x] 1.4 Registrar a chave da CoinMarketCap em `local.properties` como `cmc.api.key=...` e confirmar que o arquivo permanece fora do controle de versão

## 2. Guardrail de nomenclatura (G2)

- [x] 2.1 Escrever em `konsistTest` o *assert* `models never repeat their layer in the name suffix`: classe com prefixo `BM`, `DM` ou `VM` cujo nome termine em `Dto`, `Model`, `Entity`, `Data`, `Payload`, `Body`, `Json` ou `Schema` reprova, comparando em caixa insensível para pegar `DTO` e `dto`
- [x] 2.2 Verificar que o *assert* nasce **verde** sobre a base atual — nenhuma classe existente viola — e que a mensagem de falha nomeia a classe infratora
- [x] 2.3 Confirmar com um nome de teste descartável (`DMExchangeDto`) que o *assert* de fato reprova, e desfazer em seguida
- [x] 2.4 Registrar a regra na tabela de prefixos do `AGENTS.md`, junto da explicação de que sufixo estrutural (`Response`, `Entry`) continua permitido

## 3. Domínio — modelo e contrato

- [x] 3.1 Escrever `BMExchangeTest` cobrindo que `spotVolumeUsd` e `dateLaunched` aceitam ausência e que `spotVolumeUsd = 0.0` é distinto de ausente
- [x] 3.2 Criar `domain/exchange/model/BMExchange.kt` com `id`, `name`, `logoUrl: String?`, `spotVolumeUsd: Double?`, `dateLaunched: Instant?` (prefixo `BM` exigido pelo Konsist em `..model..`)
- [x] 3.3 Criar `domain/exchange/model/BMExchangePage.kt` com `items`, `page` e `hasMore`
- [x] 3.4 Criar a interface `domain/exchange/ExchangeRepository.kt` com `suspend fun loadPage(page: Int): BMExchangePage` (sufixo `Repository` exigido pelo Konsist)

## 4. Domínio — caso de uso (TDD)

- [x] 4.1 Escrever `GetExchangePageUseCaseTest` com `@RunWith(Enclosed::class)`, `TestSetup` com `mockk<ExchangeRepository>()`, e os contextos `HappyPath` e `ErrorPath` obrigatórios
- [x] 4.2 `HappyPath`: página válida devolve `Result.success` com os itens na ordem do repositório
- [x] 4.3 `ErrorPath`: exceção arbitrária do repositório vira `Result.failure` com `DomainError`; `DomainError` já tipado atravessa sem remapeamento
- [x] 4.4 `ErrorPath`: cancelamento do escopo chamador propaga `CancellationException` e **não** vira `Result.failure`
- [x] 4.5 Implementar `domain/exchange/GetExchangePageUseCase.kt` estendendo `UseCase<Int, BMExchangePage>` até os testes ficarem verdes

## 5. Dados — transporte e fonte remota (TDD)

- [x] 5.1 Recortar das respostas reais do provedor as fixturas `exchange_map.json` e `exchange_info.json` em `data/src/test/resources/`, incluindo obrigatoriamente **um item sem `spot_volume_usd`** e **um sem `date_launched`**
- [x] 5.2 Escrever `ExchangeRemoteDataSourceTest` com `MockEngine`, cobrindo desserialização do `map` (array), do `info` (objeto com chave string de id), campo desconhecido ignorado e campo obrigatório ausente virando `DomainError.Serialization`
- [x] 5.3 Criar os DTOs em `data/exchange/dto/` com prefixo `DM`: `DMExchangeMapEntry`, `DMExchangeMapResponse`, `DMExchangeInfo`, `DMExchangeInfoResponse` (com `data: Map<String, DMExchangeInfo>`), todos `@Serializable` e com os campos opcionais anuláveis
- [x] 5.4 Criar as rotas tipadas `@Resource` em `data/exchange/api/` — **fora** de `dto/` e `model/`, para não colidir com a regra de prefixo `DM` do Konsist
- [x] 5.5 Implementar `ExchangeRemoteDataSource` devolvendo o DTO cru e sinalizando falha por lançamento, sem `Result` e sem regra de negócio
- [x] 5.6 Escrever o teste do limite de lote: conjunto vazio de ids não emite requisição; conjunto acima de 100 é impedido antes de alcançar a rede
- [x] 5.7 Aplicar a proteção de limite na fonte de dados até o teste ficar verde

## 6. Dados — composição e repositório (TDD)

- [x] 6.1 Escrever `ExchangeRepositoryImplTest` com `HappyPath` e `ErrorPath`, cobrindo: composição `map` + `info`; id do índice sem conteúdo correspondente é omitido; `hasMore` verdadeiro enquanto restam ids e falso no último lote; ordem do índice preservada
- [x] 6.2 Escrever o teste de memoização: duas chamadas a `loadPage` emitem **uma** requisição de `map` e duas de `info`
- [x] 6.3 Escrever o teste de concorrência: duas chamadas simultâneas a `loadPage(0)` não disparam dois `map`
- [x] 6.4 Criar o mapper `data/exchange/mapper/` com `DMExchangeInfo.to(): BMExchange` em sentido único, convertendo `date_launched` ISO-8601 em `Instant` e preservando o nulo quando ausente
- [x] 6.5 Implementar `ExchangeRepositoryImpl` com o índice memoizado sob `Mutex` e o fatiamento em lotes de 100, até os testes ficarem verdes
- [x] 6.6 Registrar `ExchangeRemoteDataSource` e `ExchangeRepositoryImpl` (ligado a `ExchangeRepository`) no `dataModule`, e o `GetExchangePageUseCase` no módulo que o expõe à apresentação

## 7. Dados — tradução de status HTTP (TDD)

- [x] 7.1 Escrever em `HttpClientFactoryTest`, com `MockEngine`, os casos: `401` → `DomainError.Network`; `429` → `DomainError.Network`; `404` → `DomainError.NotFound`; `500` → `DomainError.Network`; `IOException` → `Network`; falha de desserialização → `Serialization`; cancelamento → `CancellationException` propagada
- [x] 7.2 Estender o `HttpResponseValidator` do `HttpClientFactory` para inspecionar a `ResponseException` do Ktor antes de delegar a `toDomainError()`, sem alterar `ThrowableToDomainError` em `:domain`
- [x] 7.3 Confirmar que os testes existentes de `HttpClientFactory` e `ThrowableToDomainError` continuam verdes

## 8. Dados — cliente de imagens

- [x] 8.1 Escrever o teste de `createImageClient`: a requisição emitida **não** carrega o cabeçalho `X-CMC_PRO_API_KEY`, enquanto a do cliente da API carrega
- [x] 8.2 Implementar `HttpClientFactory.createImageClient(engine)` sem `defaultRequest`, sem credencial e sem o validador de domínio
- [x] 8.3 Publicar os dois clientes no `dataModule` sob qualificadores nomeados distintos e atualizar `DataModuleTest` para exigir que ambos resolvam

## 9. Apresentação — contrato e ViewModel (TDD)

- [x] 9.1 Definir o contrato MVI em `app/presentation/feature/exchangelist/`: `VMExchangeListState` (`items`, `page`, `hasMore`, `isLoadingMore`, `errorMessage`, `pagingErrorMessage`), eventos e efeitos
- [x] 9.2 Criar `model/VMExchange.kt` com `volumeLabel` e `launchDateLabel` já formatados como `String`
- [x] 9.3 Acrescentar `fun resolve(@StringRes id: Int): String` a `ResourceProvider` e a `AndroidResourceProvider`, mantendo o `resolve(TextKey)` existente, e cobrir a sobrecarga em `ResourceProviderTest`
- [x] 9.4 Escrever o teste do mapper `BMExchange.to(...): VMExchange`: volume formatado em USD compacto; volume `0.0` formatado como zero e **não** como indisponível; volume nulo e data nula resolvidos para o texto de indisponibilidade; data formatada a partir do `LocalDate` em **UTC**
- [x] 9.5 Implementar o mapper de apresentação até o teste ficar verde
- [x] 9.6 Escrever `ExchangeListViewModelTest` com `HappyPath` e `ErrorPath` cobrindo: primeira página publica conteúdo; rolagem acrescenta o lote ao fim sem duplicar nem reordenar; lote em andamento não aceita solicitação concorrente; `hasMore` falso encerra a paginação; falha inicial publica estado de erro com texto localizado; nova tentativa recomeça; falha de lote posterior **preserva** os itens já publicados; índice vazio publica estado de lista vazia distinto de erro
- [x] 9.7 Implementar `ExchangeListViewModel` com `StateFlow` de estado, `SharedFlow` de efeito, abertura de coroutine **apenas** no `onEvent`, e `SavedStateHandle` guardando a página alcançada (não os itens)

## 10. Apresentação — tela de listagem

- [x] 10.1 Acrescentar a `strings.xml` os textos de indisponibilidade, lista vazia, ação de nova tentativa e título da tela
- [x] 10.2 Implementar o item da lista com logotipo via `AsyncImage` (`contentDescription = null`, por ser decorativo), nome, volume e data
- [x] 10.3 Implementar `ExchangeListScreen` com os estados de carregamento inicial, conteúdo, lista vazia e erro com ação de nova tentativa, e o rodapé de carga do próximo lote
- [x] 10.4 Disparar a solicitação do próximo lote a partir da posição de rolagem, sem chamar o `ViewModel` durante a composição
- [x] 10.5 Escrever `ExchangeListScreenTest` sob Robolectric: cada estado renderiza o que lhe corresponde; item sem volume mostra o texto de indisponibilidade; toque em um item emite a seleção
- [x] 10.6 Configurar o `SingletonImageLoader` do Coil com `KtorNetworkFetcherFactory` apontando para o cliente de imagens injetado

## 11. Navegação e substituição da casca

- [x] 11.1 Consultar a *skill* `navigation-3` antes de escrever a navegação — as assinaturas citadas no `design.md` vêm de uma *alpha* e podem ter mudado
- [x] 11.2 Criar `app/presentation/navigation/` com as chaves `@Serializable` `ExchangeListKey` (`data object`) e `ExchangeDetailKey(exchangeId: Int)`, ambas `NavKey`
- [x] 11.3 Implementar a casca de navegação com `rememberNavBackStack` e `NavDisplay`, com os decoradores de `savedState` e de `viewModelStore` para dar escopo próprio a cada entrada
- [x] 11.4 Implementar `ExchangeDetailScreen` como destino mínimo, exibindo apenas a identificação recebida — o conteúdo real é a mudança seguinte
- [x] 11.5 Substituir `HomeScreen` pela casca de navegação em `MainActivity`; remover `HomeScreen.kt`, `HomeScreenTest.kt` e as *strings* `home_title` e `home_placeholder` no **mesmo** passo, porque `UnusedResources` é erro no Lint
- [x] 11.6 Escrever o teste de navegação: seleção empilha o detalhe; retorno desempilha; retorno no destino inicial devolve o controle ao sistema
- [x] 11.7 Atualizar `AppGraphTest` para cobrir as novas definições do grafo

## 12. Release e guardrails

- [x] 12.1 Acrescentar a `app/src/main/keepRules/rules.keep` as regras para Coil, Navigation 3 e as chaves de rota `@Serializable`
- [x] 12.2 Rodar `./gradlew :app:assembleRelease` e verificar que o `ColdStartSmokeTest` passa sobre o artefato ofuscado (G5)
- [x] 12.3 Rodar a suíte G8 completa: `./gradlew detekt ktlintCheck :app:lintDebug :konsistTest:test :domain:test :data:testDebugUnitTest :app:testDebugUnitTest`
- [x] 12.4 Corrigir qualquer violação **no código** — sem `baseline`, `@Suppress`, `ktlint-disable` ou afrouxamento de regra

## 13. Verificação contra a API real

- [x] 13.1 Executar o app com a chave configurada e confirmar a listagem rolando até o fim do catálogo, observando a troca de lotes
- [x] 13.2 Executar o app **sem** chave configurada e confirmar que a tela exibe a mensagem de indisponibilidade de rede, e não a de erro inesperado
- [x] 13.3 Confirmar em modo avião que o estado de erro aparece com ação de nova tentativa, e que a nova tentativa funciona ao restaurar a conexão
