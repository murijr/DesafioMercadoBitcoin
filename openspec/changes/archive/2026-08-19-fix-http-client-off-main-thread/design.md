## Context

Ver `proposal.md - Why` para o rastro completo da violação. Pontos de partida relevantes para o "como":

- `ExchangeRemoteDataSource` recebe `HttpClient` pronto no construtor (`data/.../ExchangeRemoteDataSource.kt`), e suas três funções `suspend` não trocam de dispatcher: rodam no do chamador.
- O chamador é o `ViewModel`, cujo `viewModelScope.launch { }` roda em `Dispatchers.Main.immediate`. Logo, o corpo das funções de IO começa na *main thread* — o socket sai dali porque o engine OkHttp do Ktor tem o próprio dispatcher, mas tudo que acontece **antes** dele não sai.
- Pior: o cliente sequer chega a esse ponto. Koin resolve `ExchangeRemoteDataSource` — e portanto constrói o `HttpClient` — quando o `ViewModel` é criado por `koinViewModel()`, durante a composição. A construção acontece antes de qualquer `suspend`.
- O projeto **não tem** regra de dispatcher: `data/AGENTS.md`, `domain/AGENTS.md` e o spec `data-network-foundation` não mencionam `Dispatchers`, `withContext` nem *main thread*. A única regra vizinha, no `AGENTS.md` raiz, é sobre *escopo* ("não acoplar UseCase a `CoroutineScope` — quem chama controla o escopo"), não sobre dispatcher.
- `ExchangeRemoteDataSource` é construído em exatamente três lugares: o *binding* Koin, o teste da própria fonte, e a definição da classe.

## Goals / Non-Goals

**Goals:**
- Nenhuma construção de cliente HTTP nem I/O de disco decorrente dela na *main thread*.
- A camada de dados declara, em spec, onde despacha seu IO — a omissão é o que deixou o problema passar.
- G9 verde, destravando o arquivamento de `add-strictmode-guardrail`.

**Non-Goals:**
- Remover ou substituir o plugin `Resources` do Ktor. As três rotas `@Resource` são reais e o roteamento *type-safe* é decisão já tomada em `data-network-foundation`.
- Dispatcher injetável na fonte de dados "para testabilidade". YAGNI: os testes usam `MockEngine` e `runTest`, que atravessam `withContext` sem cerimônia; parâmetro de dispatcher entra quando um teste concreto precisar dele.
- Mexer no `ViewModel` ou no `UseCase`. O dispatcher de IO é responsabilidade de quem faz IO, e quem faz IO é `:data`.
- Aquecer o cliente em segundo plano no arranque. Ver *Decisions*.

## Decisions

**Preguiça na injeção do cliente e troca de dispatcher no IO — as duas juntas.**
Só preguiça adiaria a construção para a primeira chamada, que continua começando na *main thread* (`viewModelScope` é `Main.immediate`): a violação mudaria de instante, não desapareceria. Só `withContext` não ajudaria, porque a construção acontece na resolução do grafo, antes de qualquer `suspend`. A combinação é que garante que o primeiro toque no cliente aconteça já dentro do dispatcher de IO.

**Dispatcher fixado na fonte de dados, não no `UseCase` nem no `ViewModel`.**
Quem conhece a natureza da operação é quem a executa. Subir a responsabilidade obrigaria todo chamador a lembrar de envolver a chamada — regra que depende de disciplina humana, que é exatamente o tipo de acordo verbal que os guardrails deste projeto existem para substituir. Descer para o `HttpClientFactory` também não resolve: a fábrica não é `suspend` e é chamada de dentro do *binding* Koin.

**Aquecimento em segundo plano no arranque foi considerado e rejeitado.**
Resolver o cliente numa corrotina de IO durante `Application.onCreate` tiraria a construção da *main thread* sem tocar em `:data`. Mas é uma corrida: se a UI pedir o cliente antes do aquecimento terminar, a *main thread* bloqueia dentro do `synchronized` do `SingleInstanceFactory` do Koin em vez de fazer o trabalho — o `StrictMode` cala, e o *jank* continua, agora invisível. Trocar um problema visível por um escondido é pior que não corrigir.

**A regra de dispatcher vira spec, não só comentário.**
O problema não foi um deslize de implementação: foi a ausência de uma regra. Registrá-la apenas em KDoc a deixaria fora do alcance de qualquer revisão sistemática; no spec, ela passa a ser o contrato que o próximo `DataSource` herda.

## Risks / Trade-offs

- **[Trade-off] Um salto de dispatcher a mais por requisição.** Custo de microssegundos, contra dezenas a centenas de milissegundos de `kotlin-reflect` retirados da *main thread* no arranque. Troca claramente favorável.
- **[Risco] Testes que dependiam de execução síncrona no dispatcher do `runTest`** podem passar a precisar de `advanceUntilIdle` ou equivalente, porque `withContext(Dispatchers.IO)` sai do escalonador virtual. → Mitigação: há um único ponto de construção da fonte de dados em teste; o ajuste, se necessário, é local e aparece imediatamente ao rodar a suíte de `:data`.
- **[Risco] A construção preguiçosa move a falha de configuração do cliente** (chave ausente, engine mal montado) da resolução do grafo para a primeira requisição. → Aceito: essa falha já se manifestava como `DomainError.Network` em tempo de execução, por decisão registrada no `AGENTS.md` raiz ("a ausência da chave só se manifesta em tempo de execução"). O comportamento observável não muda.
