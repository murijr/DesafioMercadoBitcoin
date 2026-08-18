# AGENTS.md — `:data`

> **Android library.** Implementa as interfaces de `:domain` e fala com o mundo externo (HTTP, banco, IO). Conhece Android SDK, mas **não** conhece Compose, ViewModel, `Context` de Activity nem recursos — `Context` apenas se alguma dependência nativa (ex.: `ConnectivityManager`) exigir e mesmo assim sem vazar para o domínio.

## Layout

```
data/
├── <feature>/
│   ├── api/             # definição de endpoint (Ktor @Resource)
│   ├── dto/             # modelos de transporte (@Serializable)
│   ├── mapper/          # funções de extensão DTO → DomainModel
│   ├── <Feature>RemoteDataSource.kt
│   └── <Feature>RepositoryImpl.kt   # implementa a interface de :domain
├── network/             # HttpClientFactory, plugins Ktor
└── di/                  # módulo(s) Koin desta camada
```

## Prefixo `DM` (DataModel)

Modelos de transporte / persistência. Vivem em `:data/<feature>/dto/` ou `:data/<feature>/model/`. Ex.: `DMExchangeInfo`, `DMAssetList`.

- **Sempre** `@Serializable` quando vêm de JSON.
- **Nunca** atravessam a fronteira para `:domain` ou `:app`. O que sai daqui é `BM` (ver [`domain/AGENTS.md`](../domain/AGENTS.md)).
- Mapeamento ocorre em `:data/<feature>/mapper/`.

## `RemoteDataSource`

- Faz IO. **Só** IO. Sem regra de negócio, sem validação semântica.
- Recebe `HttpClient` via DI (Koin), configura o request e devolve o `DM` cru.
- Converte exceções de transporte (`ResponseException`, `IOException`, `SerializationException`) em `DomainError` via `Throwable.toDomainError()` — **ou** relança como `DomainError.Network`/`DomainError.Serialization` direto, dependendo do ponto.
- Lança, não retorna `Result`. Quem converte `Throwable` → `Result` é o `UseCase` (ver [`domain/AGENTS.md`](../domain/AGENTS.md)).

## `RepositoryImpl`

- Implementa a **interface** definida em `:domain/<feature>/`.
- Apenas delega ao DataSource e aplica o mapper. **Não** decide regra de negócio.
- Mapper: `DM.to(): BM` em `:data/<feature>/mapper/`. **Um único sentido** por função de extensão — nada de `DM.to().to()`.
- Captura `Throwable` de transporte e converte em `DomainError` antes de chegar no UseCase (alternativa: deixar o UseCase converter via `Throwable.toDomainError()`; o importante é que **um** ponto faz isso).

## Mappers

- `DM.to(): BM` em `:data/<feature>/mapper/`.
- O nome da função é literalmente `to()` — o tipo de retorno já diz em qual camada o resultado vive e para onde o dado está fluindo.
- Para o lado presentation (`BM.to()` → `VM`), ver [`app/AGENTS.md`](../app/AGENTS.md).

## HTTP / Ktor

- `HttpClientFactory` em `data/network/`. Único ponto onde o engine (`Android`), plugins (`ContentNegotiation`, `Logging`, `HttpTimeout`), e serialização (`Json`) são montados.
- Endpoints definidos em `<feature>/api/` como `@Resource` (type-safe routing do Ktor).
- API key da CoinMarketCap: leitura de `BuildConfig` do `:app` via injeção — **não** hardcoded em `:data`. Padrão: `data/di` expõe um `CMCAPIClient` (ou similar) já configurado com o header `X-CMC_PRO_API_KEY`.

## Injeção de dependência (camada)

- Módulo(s) Koin em `data/di/`.
- Bindings explícitos: `factory<Interface> { Impl(get()) }`. Sem `@Binds`/codegen.
- O que mora aqui: `HttpClient`, `RemoteDataSource`s, `RepositoryImpl`s.
- `Repository` (interface) **não** é bindado aqui — o `:app` importa a interface de `:domain` e a `Impl` de `:data`, e dá o bind no módulo do app. Alternativa: bindar aqui e exportar; escolha do módulo, manter consistente.
- Nada de `viewModel { ... }` aqui — isso é do `:app`.

## Testes (Robolectric)

`RepositoryImpl` + mappers DTO rodam com sombra Android do Robolectric (JVM, sem emulador, incluído em G7).

| O que se testa | Onde mora o erro |
|---|---|
| Integração com DataSource (rede, parsing, IO) | `DomainError.Network`, `DomainError.Serialization` |
| Mapeamento `DM.to()` (campos ausentes, tipos errados) | Input do teste é `DM` montado à mão, saída é `BM` |
| Erro de transporte mockado | `coEvery { dataSource.foo() } throws ...` |

**Mínimo obrigatório por `RepositoryImpl`:**
- `HappyPath` — DataSource retorna `DM` válido → `RepositoryImpl` devolve `BM` correto.
- `ErrorPath` — DataSource lança `IOException`/`SerializationException` → `RepositoryImpl` propaga `DomainError.Network`/`DomainError.Serialization`.

Convenção completa (Gherkin + Enclosed + Happy/Error) no root.

## O que `:data` **não** pode importar

- **Nada** de `:app` (Compose, ViewModel, `Activity`, `R.string.*`).
- **Nada** de `Composable`, `LiveData`, `MutableStateFlow` (StateFlow cru em DataSource é ok se virar `Flow` no contrato).
- **Nada** de regra de negócio — `:domain` ou Uso (UseCase) decidem.
- **Não** retornar `DM` para fora da camada. Se isso apareceu, falta mapper.

## O que `:data` **pode** importar

- Android SDK quando inevitável (engine `Android` do Ktor, `ConnectivityManager` se necessário).
- `kotlinx-serialization`, `kotlinx-coroutines`.
- `:domain` (interfaces, `BM`, `DomainError`, `UseCase`).
