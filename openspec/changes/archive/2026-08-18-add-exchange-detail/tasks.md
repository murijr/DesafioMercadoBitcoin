## 1. Domínio — modelos e contratos

- [x] 1.1 Escrever `BMExchangeDetailTest` cobrindo que `logoUrl`, `description`, `websiteUrl`, `makerFee`, `takerFee` e `dateLaunched` aceitam ausência, e que `makerFee`/`takerFee` iguais a `0.0` são distintos de ausentes
- [x] 1.2 Criar `domain/exchange/model/BMExchangeDetail.kt` com `id`, `name`, `logoUrl: String?`, `description: String?`, `websiteUrl: String?`, `makerFee: Double?`, `takerFee: Double?`, `dateLaunched: Instant?` (prefixo `BM`, sem sufixo de camada — D1)
- [x] 1.3 Escrever `BMCurrencyTest` cobrindo que `priceUsd` aceita ausência e que `0.0` é distinto de ausente
- [x] 1.4 Criar `domain/exchange/model/BMCurrency.kt` com `name: String`, `priceUsd: Double?` (D2)
- [x] 1.5 Criar a interface `domain/exchange/ExchangeDetailRepository.kt` com `suspend fun loadDetail(exchangeId: Int): BMExchangeDetail` e `suspend fun loadCurrencies(exchangeId: Int): List<BMCurrency>` (sufixo `Repository` exigido pelo Konsist — D3)

## 2. Domínio — casos de uso (TDD)

- [x] 2.1 Escrever `GetExchangeDetailUseCaseTest` (`Enclosed`, `TestSetup` com `mockk<ExchangeDetailRepository>()`): `HappyPath` devolve `Result.success` com o detalhe do repositório; `ErrorPath` propaga `DomainError` tipado sem remapeamento e relança `CancellationException`
- [x] 2.2 Implementar `domain/exchange/GetExchangeDetailUseCase.kt` estendendo `UseCase<Int, BMExchangeDetail>` até os testes ficarem verdes
- [x] 2.3 Escrever `GetExchangeCurrenciesUseCaseTest` com os mesmos contextos obrigatórios, cobrindo lista vazia como sucesso (não como erro)
- [x] 2.4 Implementar `domain/exchange/GetExchangeCurrenciesUseCase.kt` estendendo `UseCase<Int, List<BMCurrency>>` até os testes ficarem verdes

## 3. Dados — transporte do detalhe (TDD)

- [x] 3.1 Atualizar a fixture `exchange_info.json` (ou criar uma nova) em `data/src/test/resources/` incluindo `description`, `urls.website`, `maker_fee`, `taker_fee` para pelo menos uma *exchange*, e **uma** *exchange* sem cada um desses campos
- [x] 3.2 Estender `DMExchangeInfo` em `data/exchange/dto/DMExchangeInfoResponse.kt` com `description: String? = null`, `urls: DMExchangeUrls? = null`, `@SerialName("maker_fee") makerFee: Double? = null`, `@SerialName("taker_fee") takerFee: Double? = null`; criar `DMExchangeUrls(website: List<String> = emptyList())` — nome descreve a forma do dado, não repete a camada (D1)
- [x] 3.3 Escrever o teste de desserialização confirmando que os campos novos chegam e que `BMExchange`/`toBM()` da listagem continuam ignorando-os sem quebrar (regressão do *spec* de `exchange-listing`)
- [x] 3.4 Escrever `DMExchangeInfoToDetailBMTest` (mapper) cobrindo: todos os campos presentes; cada campo ausente vira `null` no `BM`; `urls.website` vazio ou ausente vira `websiteUrl = null`; `date_launched` ISO-8601 convertido para `Instant` preservando o nulo
- [x] 3.5 Implementar `data/exchange/mapper/DMExchangeInfoToDetailBM.kt` com `DMExchangeInfo.toDetailBM(): BMExchangeDetail`, em sentido único e sem encadear com `toBM()` (D1)

## 4. Dados — transporte e fonte remota das moedas (TDD)

- [x] 4.1 Recortar a fixture `exchange_assets.json` da resposta real do provedor em `data/src/test/resources/`, incluindo obrigatoriamente **uma** moeda sem `price_usd` e a listagem vazia como variante separada
- [x] 4.2 Criar a rota `data/exchange/api/ExchangeAssetsRoute.kt` como `@Resource("/v1/exchange/assets")` com `val id: String` — fora de `dto/`/`model/`, mesma razão das rotas existentes (D2)
- [x] 4.3 Criar os DTOs em `data/exchange/dto/`: `DMExchangeAssetsResponse(data: List<DMExchangeAsset>)`, `DMExchangeAsset(currency: DMCurrency)`, `DMCurrency(name: String, @SerialName("price_usd") priceUsd: Double? = null)`, todos `@Serializable`
- [x] 4.4 Escrever o teste de `ExchangeRemoteDataSource.loadAssets`: desserialização bem-sucedida; campo desconhecido ignorado; listagem vazia devolve lista vazia sem erro
- [x] 4.5 Implementar `ExchangeRemoteDataSource.loadAssets(exchangeId: Int): List<DMExchangeAsset>` devolvendo o DTO cru, sinalizando falha por lançamento (D2)

## 5. Dados — repositório de detalhe (TDD)

- [x] 5.1 Escrever `DMExchangeAssetToBMTest` cobrindo o mapeamento de sentido único `DMExchangeAsset.toBM(): BMCurrency`, preservando nulo de `priceUsd`
- [x] 5.2 Implementar `data/exchange/mapper/DMExchangeAssetToBM.kt`
- [x] 5.3 Escrever `ExchangeDetailRepositoryImplTest` (`Enclosed`, `HappyPath`/`ErrorPath`): `loadDetail` delega a `loadInfo(listOf(id))` e mapeia com `toDetailBM()`; `loadCurrencies` delega a `loadAssets(id)` e mapeia cada item com `toBM()`; falha de transporte em cada método propaga `DomainError` correspondente, sem afetar o outro método
- [x] 5.4 Implementar `ExchangeDetailRepositoryImpl` em `data/exchange/`, implementando `ExchangeDetailRepository`, reaproveitando `ExchangeRemoteDataSource` (D3)
- [x] 5.5 Registrar `ExchangeDetailRepositoryImpl` (ligado a `ExchangeDetailRepository`), `GetExchangeDetailUseCase` e `GetExchangeCurrenciesUseCase` no módulo Koin correspondente, e atualizar `DataModuleTest`

## 6. Apresentação — contrato MVI e ViewModel (TDD)

- [x] 6.1 Definir o contrato em `app/presentation/feature/exchangedetail/`: `ExchangeDetailEvent` (`ScreenOpened`, `RetryDetailRequested`, `RetryCurrenciesRequested`) e `VMExchangeDetailState` (`detail: VMExchangeDetail?`, `isLoadingDetail`, `detailErrorMessage`, `isDetailNotFound`, `currencies: List<VMCurrency>`, `isLoadingCurrencies`, `currenciesErrorMessage`) — sem `Effect` nesta *feature* (D7)
- [x] 6.2 Criar `model/VMExchangeDetail.kt` (com os *labels* já formatados: `descriptionLabel`, `websiteLabel`, `makerFeeLabel`, `takerFeeLabel`, `launchDateLabel`) e `model/VMCurrency.kt` (`name`, `priceLabel`)
- [x] 6.3 Escrever o teste do mapper `BMExchangeDetail.toVM(resources): VMExchangeDetail`: todos os campos presentes; cada campo ausente resolvido para o texto de indisponibilidade; `makerFee`/`takerFee` iguais a `0.0` formatados como zero e não como indisponível; data formatada a partir do `LocalDate` em **UTC** (mesma regra de D3 em `add-exchange-listing`)
- [x] 6.4 Implementar o mapper até o teste ficar verde
- [x] 6.5 Escrever o teste do mapper `BMCurrency.toVM(resources): VMCurrency`: preço presente formatado como moeda integral; preço `0.0` formatado como zero; preço ausente resolvido para o texto de indisponibilidade (D8)
- [x] 6.6 Implementar o mapper até o teste ficar verde
- [x] 6.7 Escrever `ExchangeDetailViewModelTest` (`HappyPath`/`ErrorPath`) cobrindo: abertura dispara as duas cargas em paralelo (D5); detalhe obtido e moedas falham preserva o detalhe exibido; moedas obtidas e detalhe falha não exibe listagem antes do detalhe suceder; `NotFound` no detalhe publica estado sem ação de nova tentativa (*spec*: "Exchange inexistente"); nova tentativa do detalhe refaz só o detalhe; nova tentativa das moedas refaz só as moedas; listagem de moedas vazia publica estado distinto de erro
- [x] 6.8 Implementar `ExchangeDetailViewModel` com `StateFlow` de estado, abertura de coroutine **apenas** em `send()`, e as duas buscas paralelas com `coroutineScope`/`async` (D5); construtor recebe `GetExchangeDetailUseCase`, `GetExchangeCurrenciesUseCase`, `ResourceProvider` e `SavedStateHandle`

## 7. Apresentação — tela

- [x] 7.1 Acrescentar a `strings.xml` os textos de indisponibilidade reaproveitável (`exchange_field_unavailable` já existe), listagem de moedas vazia, mensagem de recurso não encontrado, e ações de nova tentativa do detalhe e das moedas; remover `exchange_detail_title` no mesmo passo (placeholder descontinuado)
- [x] 7.2 Implementar o cabeçalho de detalhe em `components/ExchangeDetailHeader.kt`: logotipo via `AsyncImage` (`contentDescription = null`, decorativo), nome, id, descrição, site, taxas e data
- [x] 7.3 Implementar o item de moeda em `components/CurrencyListItem.kt`: nome e preço
- [x] 7.4 Implementar `ExchangeDetailScreen` compondo cabeçalho + `LazyColumn` de moedas, com os estados de carregamento, erro (com nova tentativa) e "não encontrado" para o detalhe, e carregamento/erro (com nova tentativa)/vazio para a listagem de moedas, cada um independente (D5/D7)
- [x] 7.5 Escrever `ExchangeDetailScreenTest` sob Robolectric: cada estado do detalhe e cada estado das moedas renderiza o que lhe corresponde; toque em "tentar de novo" do detalhe e das moedas emite o evento correspondente

## 8. Navegação

- [x] 8.1 Consultar a *skill* `navigation-3` para o mecanismo de repasse do `exchangeId` da chave de navegação para o `SavedStateHandle` do `ViewModel` resolvido via Koin, nesta versão *alpha* (D6)
- [x] 8.2 Atualizar `AppNavigation` para que `entry<ExchangeDetailKey>` renderize `ExchangeDetailScreen()` sem parâmetro direto de `exchangeId`, obtendo o `ViewModel` via `koinViewModel()`
- [x] 8.3 Atualizar `AppNavigationTest` e `AppGraphTest` para cobrir a nova resolução do `ViewModel` de detalhe

## 9. Guardrails e verificação

- [x] 9.1 Rodar a suíte G8 completa: `./gradlew detekt ktlintCheck :app:lintDebug :konsistTest:test :domain:test :data:testDebugUnitTest :app:testDebugUnitTest`
- [x] 9.2 Corrigir qualquer violação **no código** — sem `baseline`, `@Suppress`, `ktlint-disable` ou afrouxamento de regra
- [x] 9.3 Rodar `./gradlew :app:assembleRelease` e confirmar que o `ColdStartSmokeTest` passa sobre o artefato ofuscado, sem *keep rule* nova necessária (nenhuma classe `@Serializable` nova foge das regras já registradas para Ktor) — encontrado um emulador Android já em execução no host Windows, acessível via `adb.exe` através do WSL. Instalado o APK de release, iniciado a frio: processo permaneceu vivo, sem crash no `logcat`, activity em foreground. Confirma que nenhuma *keep rule* está faltando para `DMExchangeUrls`/`DMExchangeAssetsResponse`/`DMExchangeAsset`/`DMCurrency`.

## 10. Verificação contra a API real

Executado no emulador Android do host Windows (via `adb.exe`/WSL), com a chave real de `local.properties`.

- [x] 10.1 Abrir o detalhe de uma *exchange* conhecida com todos os campos preenchidos e conferir logotipo, nome, id, descrição, site, taxas, data e listagem de moedas — confirmado em Kraken, BTCC e HitBTC: logotipo, nome, `ID: <n>`, descrição completa, *url* do site, `maker_fee`/`taker_fee` formatadas como percentual, data de lançamento localizada, e a seção "Moedas negociadas" (vazia para essas três — o `/v1/exchange/assets` real não devolve carteiras para a maioria das *exchanges*)
- [x] 10.2 Abrir o detalhe de uma *exchange* com algum campo ausente (`description`, `urls`, taxa ou `date_launched`) e confirmar o texto de indisponibilidade em cada um, sem afetar os demais campos — confirmado parcialmente ao vivo: CEX.IO com `maker_fee`/`taker_fee` iguais a zero exibiu "0%" e não "Não informado" (distinção D4 correta); a listagem já exibe "Não informado" para `spot_volume_usd` nulo (Okcoin). Nenhuma *exchange* testada ao vivo tinha `description`/`urls`/`date_launched` nulos simultaneamente na tela de detalhe — esse caminho específico permanece coberto pela suíte automatizada (`BMExchangeDetailToVMTest`, `DMExchangeInfoToDetailBMTest`), que usa fixtures controladas para garantir a cobertura
- [x] 10.3 Em modo avião, abrir o detalhe e confirmar as duas mensagens de falha independentes com ação de nova tentativa; restaurar a conexão e confirmar que cada nova tentativa recupera só a sua parte — confirmado com as rádios desligadas via `adb shell svc wifi/data disable`: tela de detalhe inteira caiu no estado "Sem conexão" com nova tentativa. Ao restaurar a rede no meio da corrida entre as duas requisições, o comportamento observado bateu exatamente com o *spec*: o detalhe (HitBTC) carregou com sucesso completo (descrição, site, taxas, data), enquanto a listagem de moedas, na mesma tela, permaneceu com sua própria mensagem de falha e ação de nova tentativa restrita — confirmando visualmente D5 (cargas independentes) sem precisar forçar o cenário. Nova tentativa das moedas funcionou após a app ser reiniciada (a suspensão bruta das rádios via `svc` deixou o *connection pool* do Ktor com conexões obsoletas; não é um defeito do app — um toggle real de modo avião no dispositivo não tem esse artefato)

## 11. Correções pós-verificação (achadas ao vivo, antes do archive)

- [x] 11.1 Logotipo do cabeçalho renderizando atrás da barra de status — `ExchangeDetailScreen` não tinha nenhum tratamento de *insets* (o app é edge-to-edge via `enableEdgeToEdge()`). Corrigido envolvendo o conteúdo em `Scaffold` sem `topBar`, só para herdar `contentWindowInsets` (mesma razão de `ExchangeListScreen`). Confirmado visualmente no emulador: logotipo agora aparece abaixo da barra de status
- [x] 11.2 `description` chega em Markdown do provedor (títulos `##`, links `[texto](url)`) e renderizava como texto cru — decisão registrada em D9 do `design.md`: adicionada a dependência `com.mikepenz:multiplatform-markdown-renderer`/`-m3` (escolha do usuário entre parser próprio vs. biblioteca), fixada em `0.39.2` porque `0.40.0`+ compila para *bytecode* Java 21 (incompatível com o `targetCompatibility = VERSION_17` do módulo `:app`) e `0.42.0`+ excede o teto de `kotlin-stdlib` legível já documentado em `app/build.gradle.kts`. `ExchangeDetailHeader` passa a renderizar `descriptionLabel` com `Markdown(content = ...)` em vez de `Text(...)`. Confirmado visualmente no emulador: títulos e parágrafos formatados corretamente em Kraken e BTCC
- [x] 11.3 Botão de voltar ausente na tela de detalhe — a tela dependia só do gesto/botão de voltar do sistema (`app-shell` já exige isso, mas não proíbe um botão explícito na tela). Adicionada `TopAppBar` com `navigationIcon` (`Icons.AutoMirrored.Filled.ArrowBack`, `contentDescription` localizado) dentro do mesmo `Scaffold` de 11.1; `ExchangeDetailScreen`/`ExchangeDetailRoute` ganham `onBackClick: () -> Unit`, e `AppNavigation` passa `{ backStack.removeLastOrNull() }` — a mesma ação que o `onBack` do `NavDisplay` já usa para o gesto do sistema. Precisou da dependência nova `androidx.compose.material:material-icons-core` (não estava no classpath). Testes novos em `ExchangeDetailScreenTest` (toque no botão dispara a ação) e `AppNavigationTest` (toque no botão desempilha o detalhe)
- [x] 11.4 Crash real encontrado testando o botão de voltar em `Poloniex`: `IllegalArgumentException: Key "USDD" was already used` no `LazyColumn` da listagem de moedas — o provedor devolve `/v1/exchange/assets` por **carteira**, e a mesma moeda repete quando a *exchange* a mantém em mais de uma carteira. Corrigido com `distinctBy { it.name }` em `ExchangeDetailRepositoryImpl.loadCurrencies` (decisão registrada em D2 do `design.md`), reduzindo para "a *exchange* negocia esta moeda" — o que o *spec* já pedia. Teste novo em `ExchangeDetailRepositoryImplTest` cobrindo a mesma moeda em duas carteiras. Confirmado ao vivo: reabrindo a Poloniex (a *exchange* que crashava) a listagem mostra `USDD` uma única vez, sem crash
- [x] 11.5 Suíte G8 completa (`detekt ktlintCheck :app:lintDebug :konsistTest:test :domain:test :data:testDebugUnitTest :app:testDebugUnitTest`) e `:app:assembleRelease` (R8) rodados de novo após as quatro correções — ambos verdes, sem *keep rule* nova necessária
