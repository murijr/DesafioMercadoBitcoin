## Why

Os nove guardrails do projeto (G1–G9) são todos **estáticos ou de build**: compilação, Konsist, Detekt, KtLint, R8, Android Lint, suíte JVM e suíte instrumentada. Nenhum deles enxerga o que o app faz **enquanto roda**. Um `RepositoryImpl` que leia disco na *main thread*, um `ViewModel` que dispare rede fora do dispatcher de IO, um mapeamento que abra arquivo durante a composição — tudo isso atravessa os nove guardrails verdes e só se manifesta no dispositivo do usuário, como *jank* de rolagem ou ANR. O sinal chega tarde, longe da causa e sem quem o reproduza.

O `StrictMode` do Android é exatamente o detector que falta: instrumenta as chamadas de I/O da *main thread* dentro do processo e denuncia a violação no ponto onde ela acontece, com *stack trace*. Custa nada em release (não é instalado) e transforma uma classe inteira de erro invisível em falha imediata durante o desenvolvimento.

## What Changes

- **Novo guardrail G10 — `StrictMode.ThreadPolicy` no arranque, apenas em depuração.** `DesafioApplication.onCreate` passa a instalar, sob `BuildConfig.DEBUG`, uma política que detecta **leitura em disco**, **escrita em disco** e **acesso à rede** na *main thread*, com `penaltyLog()` (registra a violação com *stack trace*) e `penaltyDeath()` (encerra o processo). A instalação acontece **antes** do `startKoin`, para que a própria montagem do grafo esteja sob a política.
- **`penaltyDeath` é deliberado.** `penaltyLog` sozinho vira ruído de Logcat que ninguém lê; a doutrina do projeto — *"falha de guardrail = corrigir no código, nunca na configuração"* — só tem dentes se a violação for bloqueante. Violação se corrige movendo o I/O para fora da *main thread* ou delimitando a chamada inevitável do framework, nunca removendo uma detecção nem rebaixando a penalidade.
- **Prova mecânica em duas frentes**, porque G10 é um guardrail de *runtime* e não falha um build sozinho:
  - **JVM (entra no comando de G8)**: teste Robolectric novo em `:app` afirmando que, depois do `onCreate` da `Application` real, a política vigente na *main thread* não é a permissiva padrão.
  - **Dispositivo (G9)**: a suíte instrumentada existente (`ColdStartSmokeTest`, telas, componentes e navegação) passa a rodar sobre um processo debug com `penaltyDeath` armado — qualquer I/O acidental no arranque ou nas telas cobertas mata o processo e deixa a suíte vermelha.
- **G8 é reescrito**: o requisito passa a descrever **G1–G7 e G10** na frente sem emulador, e o comando do cenário é alinhado ao comando realmente documentado em `AGENTS.md` e em `app/AGENTS.md`, que inclui `:app:testDebugUnitTest` — hoje o spec o omite, e é justamente a tarefa onde o teste novo de G10 roda.
- **Escopo fechado em `ThreadPolicy`**: `VmPolicy` fica de fora por YAGNI (`AGENTS.md`: *"novo guardrail só depois de o problema concreto ter aparecido ao menos uma vez"*).

## Capabilities

### Modified Capabilities

- `architecture-guardrails`: novo guardrail G10 (detecção de I/O na *main thread* em tempo de execução, bloqueante em depuração) e reescrita de G8 para incorporar a metade JVM do G10 e corrigir o comando consolidado.

## Impact

**Código**

- `app/src/main/kotlin/com/desafiomercadobitcoin/DesafioApplication.kt` — `onCreate` ganha uma chamada a um `private fun installStrictModeThreadPolicy()` novo, antes do `startKoin`. Nenhuma assinatura pública muda; nenhuma outra classe de produção é tocada.
- `app/src/test/kotlin/com/desafiomercadobitcoin/DesafioApplicationTest.kt` — teste Robolectric novo, no mesmo estilo de `di/AppGraphTest.kt` (que já se apoia no fato de a `Application` real subir sob Robolectric).

**Dependências novas**: nenhuma. `android.os.StrictMode` é API da plataforma.

**Guardrails**: G10 é novo e bloqueante em depuração; G8 é reescrito. G9 não muda de texto — passa apenas a exercitar um processo com a política ativa, relação declarada dentro do próprio G10.

**Documentação**: `AGENTS.md` (raiz) ganha a linha G10 na tabela de guardrails e ajusta a linha G8; `app/AGENTS.md` ganha a seção `## G10` ao lado das seções `## G8` e `## G9` existentes.

**Configuração**: nenhuma. Sem mudança de `build.gradle.kts`, de `detekt.yml`, de `.editorconfig` ou de *keep rules*.
