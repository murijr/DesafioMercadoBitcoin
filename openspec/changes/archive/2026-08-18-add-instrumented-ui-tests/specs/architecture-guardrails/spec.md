## MODIFIED Requirements

### Requirement: Execução consolidada no fim da feature (G8)

O projeto SHALL disponibilizar os guardrails que rodam sem emulador (G1–G7) em um único comando, ordenada do mais barato ao mais caro, e esse comando SHALL passar integralmente sobre a base entregue por este *change*. O guardrail instrumentado (G9) SHALL rodar em um segundo comando, que exige dispositivo ou emulador conectado. A *feature* MUST NOT ser considerada concluída até que os dois comandos tenham passado sobre a base entregue.

#### Scenario: Suíte verde na base
- **WHEN** `./gradlew detekt ktlintCheck :app:lintDebug :konsistTest:test :domain:test :data:testDebugUnitTest` é executado no repositório recém-preparado, sem emulador disponível
- **THEN** todas as tarefas concluem com sucesso

#### Scenario: Suíte instrumentada verde na base
- **WHEN** `./gradlew :app:connectedDebugAndroidTest` é executado com um dispositivo ou emulador conectado
- **THEN** todas as tarefas concluem com sucesso

#### Scenario: Feature concluída exige as duas frentes
- **WHEN** a *feature* é declarada concluída
- **THEN** tanto o comando sem emulador quanto o comando instrumentado já passaram sobre a base entregue

## ADDED Requirements

### Requirement: Suíte de testes instrumentados de tela e componente (G9)

O projeto SHALL disponibilizar uma suíte de testes instrumentados de tela e componente, executável por `./gradlew :app:connectedDebugAndroidTest` sobre um dispositivo ou emulador Android real, e essa suíte SHALL bloquear a conclusão da *feature* quando vermelha — da mesma forma que G7 bloqueia a suíte JVM. G9 é aditivo a G7: nenhum dos dois substitui o outro.

#### Scenario: Suíte instrumentada completa
- **WHEN** `./gradlew :app:connectedDebugAndroidTest` é executado com um dispositivo ou emulador conectado
- **THEN** todos os testes de tela e de componente concluem com sucesso

#### Scenario: Suíte instrumentada vermelha bloqueia a feature
- **WHEN** qualquer teste da suíte instrumentada falha
- **THEN** a *feature* não SHALL ser considerada concluída até que a suíte volte a passar

#### Scenario: Ausência de dispositivo não dispensa a suíte
- **WHEN** nenhum dispositivo ou emulador está conectado
- **THEN** `./gradlew :app:connectedDebugAndroidTest` falha ao iniciar, e a *feature* permanece bloqueada até que a suíte seja executada com sucesso em um dispositivo disponível
