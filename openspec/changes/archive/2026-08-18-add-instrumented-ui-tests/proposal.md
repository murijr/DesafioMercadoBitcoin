## Why

Hoje a única prova de que as telas de listagem e detalhe de *exchange* renderizam e reagem corretamente vem de testes de Compose rodando sob uma sombra Android em JVM (Robolectric) — que simula o *framework*, mas não o hardware real, o compositor real de UI, nem o ciclo de vida real de uma `Activity`. Não há nenhuma verificação de que essas telas realmente funcionam quando compostas e renderizadas por um dispositivo/emulador Android de verdade. O único teste instrumentado do projeto hoje é o `ColdStartSmokeTest`, que só cobre a inicialização a frio, não interação de tela.

## What Changes

- **Nova suíte de testes instrumentados de tela e componente**, em `app/src/androidTest`, usando `androidx.compose.ui.test` (`createAndroidComposeRule`) rodando em dispositivo/emulador real, cobrindo:
  - `ExchangeListScreen`: estado de carregamento, lista carregada, erro e nova tentativa, navegação ao tocar em um item.
  - `ExchangeDetailScreen`: estado de carregamento, detalhe carregado, erro e nova tentativa — para o detalhe e para a listagem de moedas, independentemente (mesma independência de carga já coberta pelos testes Robolectric existentes).
  - Componentes isolados: `ExchangeListItem`, `ExchangeDetailHeader`, `CurrencyListItem` — cada um testado sozinho, fora do contexto da tela inteira, verificando que renderiza os dados recebidos e (quando aplicável) dispara o callback de toque.
- **Suíte é aditiva, não substitui os testes JVM/Robolectric existentes** (`ExchangeListScreenTest`, `ExchangeDetailScreenTest` continuam existindo e continuam sendo a suíte que roda sem emulador, G7).
- **Novo guardrail bloqueante (G9)**: a suíte instrumentada SHALL bloquear a conclusão da *feature* quando vermelha, assim como G7 bloqueia a suíte JVM — mas em um comando próprio (`./gradlew :app:connectedDebugAndroidTest`), separado do comando único de G8, porque exige dispositivo/emulador conectado.
- **G8 ganha uma segunda linha de execução consolidada**: o comando único existente continua cobrindo apenas os guardrails que rodam sem emulador (G1–G7); um segundo comando, também obrigatório antes de considerar a *feature* concluída, executa G9. G8 deixa de implicar "um único comando cobre todos os guardrails" e passa a descrever duas frentes: uma sem emulador, uma com.
- **Nova dependência**: `androidTestImplementation(libs.androidx.compose.ui.test.junit4)` (hoje só declarada em `testImplementation`) e `debugImplementation(libs.androidx.compose.ui.test.manifest)` já cobre o `androidTest` por compartilhar a variante `debug`.

## Capabilities

### New Capabilities

- `instrumented-ui-testing`: o que a suíte de testes instrumentados de tela e componente cobre — quais telas, quais componentes, quais cenários (carregamento, sucesso, erro, nova tentativa, navegação, interação) — e a garantia de que ela roda em dispositivo/emulador real via Compose UI Test, não como sombra JVM.

### Modified Capabilities

- `architecture-guardrails`: novo guardrail G9 (suíte instrumentada bloqueante) e ajuste em G8 para refletir duas frentes de execução consolidada (sem emulador e com emulador) em vez de um único comando cobrindo tudo.

## Impact

**Código**

- `:app` — novo diretório `app/src/androidTest/kotlin/com/desafiomercadobitcoin/presentation/...` espelhando a estrutura de `presentation/feature/exchangelist` e `presentation/feature/exchangedetail` já existente em `src/test`; nenhuma classe de produção muda de assinatura — os testes consomem os mesmos `Composable`s, `ViewModel`s e mapeadores já existentes.
- `app/build.gradle.kts` — `androidTestImplementation(libs.androidx.compose.ui.test.junit4)` adicionado ao lado das dependências `androidTest` já existentes (`espresso-core`, `androidx-junit`, `androidx-tracing`, `koin-core`).

**Dependências novas**: nenhuma nova biblioteca — apenas mover `ui-test-junit4` para também ser visível em `androidTest` (já presente no catálogo de versões e já usada em `testImplementation`).

**Guardrails**: G9 é novo e bloqueante; G8 é reescrito para descrever duas execuções consolidadas em vez de uma.

**Documentação**: `AGENTS.md` (raiz) ganha uma linha G9 na tabela de guardrails e a linha G8 é atualizada para os dois comandos; `app/AGENTS.md` ganha a seção "G9" ao lado da seção "G8" existente, com o comando `:app:connectedDebugAndroidTest` e a mesma explicação de "fora do comando sem emulador" já usada para `release-smoke-check.sh` (G5).

**Configuração**: nenhuma mudança de chave de API ou de cliente de rede — a suíte instrumentada usa os mesmos mecanismos de injeção de dependência de teste (Koin test) já usados no `ColdStartSmokeTest`.
