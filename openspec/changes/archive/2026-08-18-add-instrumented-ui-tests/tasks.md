## 1. Setup

- [x] 1.1 Adicionar `androidTestImplementation(libs.androidx.compose.ui.test.junit4)` em `app/build.gradle.kts`, ao lado das demais dependências `androidTest` existentes
- [x] 1.2 Confirmar que `./gradlew :app:connectedDebugAndroidTest` inicia (mesmo que ainda sem testes novos) com um dispositivo/emulador conectado, antes de escrever qualquer teste

## 2. Instrumentado — `ExchangeListScreen`

- [x] 2.1 Criar `app/src/androidTest/kotlin/com/desafiomercadobitcoin/presentation/feature/exchangelist/ExchangeListScreenTest.kt`, espelhando os mesmos casos de `app/src/test/.../ExchangeListScreenTest.kt`, com `createAndroidComposeRule<ComponentActivity>()` (ou a *activity* de teste equivalente) no lugar de `createComposeRule()` + `RobolectricTestRunner`
- [x] 2.2 Portar `given the initial loading state when rendering then the loading indicator is displayed`
- [x] 2.3 Portar `given content when rendering then every exchange name is displayed`
- [x] 2.4 Portar `given an exchange without volume when rendering then the unavailable text is displayed`
- [x] 2.5 Portar `given an empty catalog when rendering then the empty message is displayed`
- [x] 2.6 Portar `given an error when rendering then the message and the retry action are displayed`
- [x] 2.7 Portar `given the error state when the retry is tapped then the retry event is emitted`
- [x] 2.8 Portar `given a batch in flight when rendering then the paging indicator is displayed`
- [x] 2.9 Portar `given a failed batch when rendering then the content is kept alongside the message`
- [x] 2.10 Portar `given content when an item is tapped then the selection event carries its id`
- [x] 2.11 Rodar `./gradlew :app:connectedDebugAndroidTest --tests "*.exchangelist.ExchangeListScreenTest"` e confirmar suíte verde

## 3. Instrumentado — `ExchangeDetailScreen`

- [x] 3.1 Criar `app/src/androidTest/kotlin/com/desafiomercadobitcoin/presentation/feature/exchangedetail/ExchangeDetailScreenTest.kt`, espelhando `app/src/test/.../ExchangeDetailScreenTest.kt` com `createAndroidComposeRule`
- [x] 3.2 Portar `given any state when rendering then the back button is displayed`
- [x] 3.3 Portar `given the back button when it is tapped then the back action fires`
- [x] 3.4 Portar `given the detail is loading when rendering then the loading indicator is displayed`
- [x] 3.5 Portar `given the detail failed when rendering then the message and the retry action are displayed`
- [x] 3.6 Portar `given the detail error when the retry is tapped then the detail retry event is emitted`
- [x] 3.7 Portar `given the exchange is not found when rendering then the message is displayed without retry`
- [x] 3.8 Portar `given the detail when rendering then its fields are displayed`
- [x] 3.9 Portar `given currencies loading when rendering then the currencies loading indicator is displayed`
- [x] 3.10 Portar `given no currencies when rendering then the empty message is displayed`
- [x] 3.11 Portar `given currencies when rendering then each one is displayed`
- [x] 3.12 Portar `given currencies with duplicate names when rendering then the list does not crash`
- [x] 3.13 Portar `given currencies failed when rendering then the message and the retry action are displayed`
- [x] 3.14 Portar `given currencies error when the retry is tapped then the currencies retry event is emitted`
- [x] 3.15 Rodar `./gradlew :app:connectedDebugAndroidTest --tests "*.exchangedetail.ExchangeDetailScreenTest"` e confirmar suíte verde

## 4. Instrumentado — componentes isolados

- [x] 4.1 Criar `app/src/androidTest/kotlin/com/desafiomercadobitcoin/presentation/feature/exchangelist/components/ExchangeListItemTest.kt`: renderiza `ExchangeListItem` sozinho com dados completos (todos os campos visíveis), com volume ausente (texto de indisponibilidade), com data ausente (texto de indisponibilidade), com logotipo indisponível (marcador de substituição), e verifica que o toque no item dispara o *callback* de seleção exatamente uma vez
- [x] 4.2 Criar `app/src/androidTest/kotlin/com/desafiomercadobitcoin/presentation/feature/exchangedetail/components/ExchangeDetailHeaderTest.kt`: renderiza `ExchangeDetailHeader` sozinho com todos os campos presentes, e com cada campo opcional ausente individualmente (descrição, site, data, logotipo)
- [x] 4.3 Criar `app/src/androidTest/kotlin/com/desafiomercadobitcoin/presentation/feature/exchangedetail/components/CurrencyListItemTest.kt`: renderiza `CurrencyListItem` sozinho com preço presente, com preço igual a zero (exibido como zero, não indisponível), e com preço ausente (texto de indisponibilidade)
- [x] 4.4 Rodar `./gradlew :app:connectedDebugAndroidTest --tests "*.components.*"` e confirmar suíte verde

## 5. Instrumentado — navegação

- [x] 5.1 Criar `app/src/androidTest/kotlin/com/desafiomercadobitcoin/presentation/navigation/AppNavigationTest.kt`, espelhando `app/src/test/.../AppNavigationTest.kt`: mesmos duplos `mockk` dos três *use cases* de domínio, registrados em módulo Koin de teste com `startKoin`/`stopKoin`, rodando com `createAndroidComposeRule`
- [x] 5.2 Portar `given the list when an item is selected then the detail is pushed onto the stack`
- [x] 5.3 Portar `given the detail on top when the back is triggered then it is popped`
- [x] 5.4 Portar `given the detail on top when the back button is tapped then it is popped`
- [x] 5.5 Portar `given more than one destination when stacked then the shell handles the back`
- [x] 5.6 Portar `given the start destination when the back is triggered then the system takes over`
- [x] 5.7 Rodar `./gradlew :app:connectedDebugAndroidTest --tests "*.navigation.AppNavigationTest"` e confirmar suíte verde

## 6. Guardrail G9 e fechamento

- [x] 6.1 Rodar a suíte instrumentada completa (`./gradlew :app:connectedDebugAndroidTest`) e confirmar que todos os testes novos (telas, componentes, navegação) e o `ColdStartSmokeTest` já existente passam juntos
- [x] 6.2 Confirmar que a suíte JVM/Robolectric existente (`./gradlew :konsistTest:test :domain:test :data:testDebugUnitTest`) continua passando sem alteração — G9 é aditivo, não substitui G7
- [x] 6.3 Revisar que todo elemento localizado pelos testes novos usa `testTag` (ou identificador semântico equivalente) já existente na produção, e não texto localizado ou posição — nenhuma alteração de produção deveria ser necessária para satisfazer esse ponto, já que os `testTag`s reaproveitados já existem

## 7. Documentação dos guardrails

- [x] 7.1 Em `AGENTS.md` (raiz), acrescentar a linha `9 | Testes instrumentados | mecânico | ...` à tabela de guardrails, e atualizar a linha 8 (`Execução no fim da feature`) para descrever os dois comandos (sem emulador + `:app:connectedDebugAndroidTest`) em vez de um único comando
- [x] 7.2 Em `app/AGENTS.md`, acrescentar uma seção `## G9 — testes instrumentados de tela e componente` ao lado da seção `## G8` existente, com o comando `./gradlew :app:connectedDebugAndroidTest`, a lista de telas/componentes cobertos, e a mesma ressalva já usada para `release-smoke-check.sh` (G5) de que a verificação exige dispositivo e roda **fora** do comando G8 sem emulador
- [x] 7.3 Em `app/AGENTS.md`, atualizar a tabela `## Testes` (linha "Renderização + interação (Screen/Composables) via Robolectric") para registrar que a mesma cobertura agora também existe, de forma aditiva, como suíte instrumentada (G9)
