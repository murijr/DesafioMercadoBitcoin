## Why

O guardrail G10 (`add-strictmode-guardrail`) foi instalado e, na primeira execução da suíte instrumentada, matou o processo no arranque. A causa é real e **anterior** ao guardrail:

```
DiskReadViolation
  ServiceLoader.parseLine  ← lê META-INF/services do APK
  kotlin.reflect.jvm.internal … Reflection.typeOf
  io.ktor.client.plugins.resources.Resources.<clinit>
  HttpClientFactory.create              (HttpClientFactory.kt:32 — install(Resources))
  DataModuleKt.dataModule$lambda        (DataModule.kt:43)
  org.koin…SingleInstanceFactory.get
  …
  AndroidComposeView.onAttachedToWindow
```

`koinViewModel()` resolve o grafo durante a composição, na *main thread*. Koin então constrói o `HttpClient` da API, e o `install(Resources)` inicializa o `kotlin-reflect` — que descompacta e lê o APK por `ServiceLoader`. Inicialização do `kotlin-reflect` custa dezenas a centenas de milissegundos, gastas na *main thread*, exatamente no caminho da primeira tela.

O sintoma sem G10 é *jank* na abertura, invisível para todos os outros nove guardrails. Com G10, é morte do processo — que é o ponto do guardrail.

O plugin `Resources` **não** é dispensável: `ExchangeMapRoute`, `ExchangeInfoRoute` e `ExchangeAssetsRoute` são rotas `@Resource` de verdade. E `StrictMode.allowThreadDiskReads { }` seria maquiagem, porque este I/O *pode* sair da *main thread* — a exceção existe só para chamada de plataforma que comprovadamente não sai.

## What Changes

- **`ExchangeRemoteDataSource` passa a receber o cliente preguiçosamente** em vez de já construído. Hoje o cliente nasce no instante em que Koin resolve a fonte de dados — que é durante a composição, na *main thread*. Recebê-lo preguiçosamente adia a construção para o primeiro uso.
- **As três funções de IO da fonte de dados passam a executar em `Dispatchers.IO`.** É o que garante que o "primeiro uso" — e portanto a construção do cliente e a inicialização do `kotlin-reflect` — aconteça fora da *main thread*. As duas mudanças só funcionam juntas: preguiça sem troca de dispatcher apenas adia a violação para a primeira chamada.
- **Nova regra de spec em `data-network-foundation`**: a camada de dados declara onde despacha seu IO. Hoje o projeto não tem uma linha sobre isso — nem em `data/AGENTS.md`, nem no spec —, e é justamente essa omissão que permitiu a construção do cliente na *main thread* passar despercebida.
- **G9 volta a verde**, e com ele o `add-strictmode-guardrail` fica arquivável.

## Capabilities

### Modified Capabilities

- `data-network-foundation`: a fonte de dados remota passa a despachar seu IO fora da *main thread*, e o cliente HTTP passa a ser construído no primeiro uso em vez de na resolução do grafo.

## Impact

**Código**

- `data/src/main/kotlin/com/desafiomercadobitcoin/data/exchange/ExchangeRemoteDataSource.kt` — assinatura do construtor e corpo das três funções de IO.
- `data/src/main/kotlin/com/desafiomercadobitcoin/data/di/DataModule.kt` (linha 45) — o *binding* passa a entregar o cliente preguiçosamente.
- `data/src/test/kotlin/com/desafiomercadobitcoin/data/exchange/ExchangeRemoteDataSourceTest.kt` (linha 47) — único ponto de construção da fonte de dados em teste.

**Dependências novas**: nenhuma. `Dispatchers.IO` já vem das coroutines, que o projeto já usa.

**Guardrails**: G9 volta a passar; G10 deixa de matar o processo no arranque. Nenhum guardrail muda de definição.

**Documentação**: `data/AGENTS.md` ganha a regra de dispatcher da camada.

**Dependência entre changes**: este *change* destrava o `add-strictmode-guardrail`, que não pode ser arquivado enquanto G9 estiver vermelho (regra do G8). O inverso não vale: este *change* corrige um problema que existia antes do G10 e faz sentido sozinho.
