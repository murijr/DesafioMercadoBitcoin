## 1. Produção — política no arranque

- [x] 1.1 Em `app/src/main/kotlin/com/desafiomercadobitcoin/DesafioApplication.kt`, extrair o bloco `StrictMode` para um `private fun installStrictModeThreadPolicy()`, chamado de `onCreate()` **antes** do `startKoin` — mantém `onCreate` legível e longe do limite de `LongMethod` do Detekt (*threshold* 30, `detekt.yml`)
- [x] 1.2 Guarda de saída `if (!BuildConfig.DEBUG) return` no topo da função, em vez de `if` envolvendo o corpo
- [x] 1.3 Manter as cinco chamadas exatamente como especificado: `detectDiskReads()`, `detectDiskWrites()`, `detectNetwork()`, `penaltyLog()`, `penaltyDeath()`
- [x] 1.4 KDoc curto na função: por que só em depuração, e por que `penaltyDeath` (violação bloqueia; correção é no código, nunca na política)
- [x] 1.5 Corrigir a formatação para `ktlint_official`: remover as linhas em branco duplicadas, quebrar a cadeia como `StrictMode.ThreadPolicy` / `.Builder()`, e vírgula final no argumento de `setThreadPolicy(...)`

## 2. Prova em JVM (G7, dentro do comando de G8)

- [x] 2.1 Criar `app/src/test/kotlin/com/desafiomercadobitcoin/DesafioApplicationTest.kt` no estilo de `di/AppGraphTest.kt` (`@RunWith(RobolectricTestRunner::class)`, sem `Enclosed` — é um comportamento só)
- [x] 2.2 Teste `given the debug application when it starts then the strict thread policy is installed`: sob Robolectric a `Application` real já executou `onCreate`; afirmar que a política vigente na *main thread* não é a permissiva padrão da plataforma
- [x] 2.3 Provar o teste **vermelho**: remover temporariamente a chamada em `onCreate`, confirmar que o teste falha, restaurar. Se ele não conseguir ficar vermelho (Robolectric ignorando `StrictMode`), descartar o teste e registrar em `design.md` que a prova de G10 fica só em G9 — sem teste decorativo no repositório
- [x] 2.4 Rodar `./gradlew :app:testDebugUnitTest --tests "*.DesafioApplicationTest"` e confirmar verde

## 3. Verificação dos guardrails

- [x] 3.1 Rodar o comando de G8 completo (`./gradlew detekt ktlintCheck :app:lintDebug :konsistTest:test :domain:test :data:testDebugUnitTest :app:testDebugUnitTest`) e confirmar verde
- [x] 3.2 Rodar `./gradlew :app:connectedDebugAndroidTest` (G9) com dispositivo ou emulador conectado e confirmar que nenhum teste morre por violação de `StrictMode` — **executado, reprovou**: `ColdStartSmokeTest.theLauncherActivityReachesTheResumedState` morreu com `DiskReadViolation` vinda de `install(Resources)` → `kotlin-reflect` → `ServiceLoader`; após a correção do *change* irmão, reexecutado com 43/43 verdes
- [x] 3.3 Corrigir a causa no código — **encaminhado para o *change* irmão `fix-http-client-off-main-thread`**, já implementado e verde, porque a correção fixa a política de dispatcher de `:data`, que o projeto nunca teve, e por isso não cabe num *change* de guardrail. Com ele fechado, G9 voltou a 43/43 e este *change* passou a ser arquivável (regra do G8)

## 4. Documentação

- [x] 4.1 `AGENTS.md` (raiz): acrescentar a linha `10 | StrictMode | runtime | ...` à tabela de guardrails
- [x] 4.2 `AGENTS.md` (raiz): atualizar a linha 8 para registrar que a metade JVM do G10 roda no mesmo comando, e que a metade em execução roda junto com G9
- [x] 4.3 `app/AGENTS.md`: seção `## G10 — StrictMode na main thread (debug)` depois da seção `## G9`, com a política, a razão de ficar fora do release, as duas frentes de verificação e a regra de correção no código
