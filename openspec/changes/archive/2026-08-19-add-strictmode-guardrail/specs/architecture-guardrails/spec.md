## ADDED Requirements

### Requirement: Detecção de I/O na main thread em tempo de execução (G10)

A `Application` SHALL instalar, **apenas** quando o *build* é de depuração e **antes** de montar o grafo de dependências, uma política de *thread* que detecte, na *main thread*, **leitura em disco**, **escrita em disco** e **acesso à rede**, registrando cada violação no log com o rastro de pilha do ponto infrator e **encerrando o processo**.

A política MUST NOT ser instalada em *build* de release: uma violação de política de desenvolvimento não SHALL virar falha para o usuário final.

Violação SHALL ser corrigida no código — movendo o I/O para fora da *main thread*, ou delimitando explicitamente no ponto exato a chamada de plataforma que comprovadamente não pode sair dela. A política MUST NOT ser satisfeita removendo uma das três detecções, rebaixando a penalidade de encerramento para apenas registro em log, ou envolvendo o arranque inteiro numa janela permissiva.

Este guardrail é de **execução**, não de *build*: sozinho ele não reprova nenhuma tarefa Gradle. Sua verificação mecânica vem de duas frentes que não se substituem — a suíte JVM prova que a política **é instalada**; a suíte instrumentada, rodando sobre o processo com a política armada, prova que **ninguém a viola**.

#### Scenario: Política instalada em depuração
- **WHEN** o processo do *build* de depuração conclui a inicialização da `Application`
- **THEN** a política de *thread* vigente na *main thread* não é a política permissiva padrão da plataforma
- **AND** `./gradlew :app:testDebugUnitTest` reprova caso a instalação seja removida

#### Scenario: Release sem política
- **WHEN** o *build* é de release
- **THEN** nenhuma política de detecção é instalada, e nenhuma operação de I/O na *main thread* encerra o processo

#### Scenario: Rede na main thread em depuração
- **WHEN** código em execução no *build* de depuração realiza acesso à rede na *main thread*
- **THEN** a violação é registrada no log com o rastro de pilha do ponto infrator, e o processo é encerrado

#### Scenario: Disco na main thread em depuração
- **WHEN** código em execução no *build* de depuração lê ou escreve em disco na *main thread*
- **THEN** a violação é registrada no log com o rastro de pilha do ponto infrator, e o processo é encerrado

#### Scenario: Suíte instrumentada como prova em dispositivo
- **WHEN** `./gradlew :app:connectedDebugAndroidTest` é executado com dispositivo ou emulador conectado
- **THEN** a suíte conclui integralmente com sucesso, evidenciando que nem o arranque do processo nem as telas e componentes cobertos violam a política

#### Scenario: Guardrail não enfraquecido
- **WHEN** uma violação da política aparece durante o desenvolvimento
- **THEN** a causa é corrigida no código, e a política permanece com as três detecções e com o encerramento do processo

## MODIFIED Requirements

### Requirement: Execução consolidada no fim da feature (G8)

O projeto SHALL disponibilizar os guardrails que rodam sem emulador (G1–G7 e a metade JVM do G10) em um único comando, ordenada do mais barato ao mais caro, e esse comando SHALL passar integralmente sobre a base entregue por este *change*. Os guardrails que exigem dispositivo ou emulador conectado — a suíte instrumentada (G9) e, com ela, a verificação em execução da política de *thread* (G10) — SHALL rodar em um segundo comando. A *feature* MUST NOT ser considerada concluída até que os dois comandos tenham passado sobre a base entregue.

#### Scenario: Suíte verde na base
- **WHEN** `./gradlew detekt ktlintCheck :app:lintDebug :konsistTest:test :domain:test :data:testDebugUnitTest :app:testDebugUnitTest` é executado no repositório recém-preparado, sem emulador disponível
- **THEN** todas as tarefas concluem com sucesso

#### Scenario: Suíte instrumentada verde na base
- **WHEN** `./gradlew :app:connectedDebugAndroidTest` é executado com um dispositivo ou emulador conectado
- **THEN** todas as tarefas concluem com sucesso

#### Scenario: Feature concluída exige as duas frentes
- **WHEN** a *feature* é declarada concluída
- **THEN** tanto o comando sem emulador quanto o comando instrumentado já passaram sobre a base entregue
