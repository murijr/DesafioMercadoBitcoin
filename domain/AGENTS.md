# AGENTS.md — `:domain`

> **Kotlin puro.** Sem Android SDK. Sem `androidx.*`. Se essa fronteira for violada, o módulo **falha em compilar** (G1). É a proteção mais barata do projeto e o motivo de `:domain` existir como módulo separado.

## Layout

```
domain/
├── model/               # data classes de domínio (puras, sem anotações de framework)
├── error/               # sealed class DomainError + Throwable → DomainError
├── <feature>/
│   ├── <Feature>Repository.kt   # interface
│   └── <Feature>UseCase.kt      # orquestra validação + chamada ao repository
└── UseCase.kt                   # classe base abstrata
```

## Prefixo `BM` (BusinessModel)

Modelos de domínio. Vivem em `:domain/<feature>/` ou `:domain/model/`. Ex.: `BMExchange`, `BMAsset`, `BMCurrency`.

- **Única** representação de negócio que `:domain` e `:data` veem.
- Puros: sem `@Serializable`, sem `@Parcelize`, sem `@Entity`, sem nada de framework.
- Maper recebe `DM` em `:data/mapper/` e devolve `BM` — ver [`data/AGENTS.md`](../data/AGENTS.md).

## UseCase

Toda regra de negócio executável vive em um `<Feature>UseCase`. Estende a base:

```kotlin
abstract class UseCase<I, S> {
    protected abstract suspend fun doExecute(input: I): S

    suspend fun execute(input: I): Result<S> {
        return try {
            Result.success(doExecute(input))
        } catch (error: CancellationException) {
            throw error                                              // não encapsula cancelamento
        } catch (error: DomainError) {
            Result.failure(error)                                    // já tipado, sem remapeamento
        } catch (error: Throwable) {
            Result.failure(error.toDomainError())                    // mapeia qualquer outra exceção
        }
    }
}
```

**Contrato:**
- `execute` captura qualquer `Throwable` inesperado e o mapeia para `DomainError`, devolvendo `Result.failure`. Nada escapa como exceção crua para o chamador.
- `CancellationException` é re-lançada **antes** do `catch (Throwable)` — respeito à structured concurrency.
- `DomainError` já tipado passa direto para `Result.failure`, sem remapeamento.
- O `when` em cima do `DomainError` no call site (ViewModel) continua exaustivo.

**Regras:**
- Validação de input fica dentro do UseCase, não no ViewModel nem no Repository.
- UseCases **não conhecem CoroutineScope** — quem chama gerencia o escopo (`viewModelScope`, etc.).
- UseCases **não recebem `Context`** nem classes de `:app`/`:data`.

## Repository (interface)

- A **interface** vive em `:domain/<feature>/`. A **implementação** vive em `:data/<feature>/` (ver [`data/AGENTS.md`](../data/AGENTS.md)).
- Apenas o **contrato** vive aqui. Zero import de `:data`.
- Interface pequena e coesa. Se uma classe precisa de 10 métodos, ela precisa ser dividida (Interface Segregation).
- Não criar base abstrata ou "Repository genérico" antes de ter ≥3 repositórios concretos com necessidade real de compartilhar código.

## `DomainError` e i18n

- `DomainError` é uma `sealed class` em `:domain/error/`.
- Cada subtipo carrega uma `TextKey` (também `sealed`) — **nunca** string hardcoded no domínio.
- O `ResourceProvider` em `:app/presentation/common/` resolve `TextKey` → `R.string.*`. UseCases e Repositories não importam `Context`.
- `Throwable.toDomainError()` mora em `:domain/error/` e converte exceções genéricas (rede, serialização, IO) em subtipos de `DomainError`.

## Testes (JVM puro)

UseCases e mappers de erro rodam **sem emulador**. Convenção completa no root (Gherkin + Enclosed + Happy/Error) — aqui ficam os pontos específicos desta camada.

| O que se testa | Onde mora o erro |
|---|---|
| Regra de negócio + validação de input | `DomainError.Validation`, `DomainError.NotFound` |
| Mapeamento `Throwable`.toDomainError() | `DomainError.Network`, `DomainError.Serialization` |

**Mínimo obrigatório por UseCase:**
- `HappyPath` — entrada válida → `Result.success` com saída esperada.
- `ErrorPath` — precondição que falha → `Result.failure(DomainError.X)`.

Casos intermediários (edge cases, múltiplos erros) ganham `EdgeCases : TestSetup()` dentro do mesmo `Enclosed`.

**Nota importante:** o `Result.failure` carrega a `DomainError` como `Throwable`. Use `assertEquals(expected, (result.exceptionOrNull() as DomainError.X))` ou similar — não compare por `.message`.

## O que `:domain` **não** pode importar

- **Nada** de Android SDK (`android.*`, `androidx.*`).
- **Nada** de `:data` ou `:app`.
- **Nada** de Compose, `Context`, `Activity`, `ViewModel`, `LiveData`.
- **Nada** de `kotlinx-serialization` (anotações são DM, não BM).
- **Nada** de `R.string.*`/recursos.

Se aparecer, é hoje. Quem está lendo isso é porque o G1+G2 já deveriam ter segurado — corrija a fronteira, não silencie o guardrail.
