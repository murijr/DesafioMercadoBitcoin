## 1. Fonte de dados remota

- [x] 1.1 Em `ExchangeRemoteDataSource`, receber o cliente de forma preguiçosa em vez de já construído, de modo que a resolução do grafo não force sua construção
- [x] 1.2 Executar `loadActiveIndex`, `loadInfo` e `loadAssets` no dispatcher de IO da camada
- [x] 1.3 KDoc curto explicando por que as duas mudanças são inseparáveis: preguiça sem troca de dispatcher apenas adia a violação para a primeira chamada
- [x] 1.4 Ajustar o *binding* em `data/src/main/kotlin/com/desafiomercadobitcoin/data/di/DataModule.kt` (linha 45) para entregar o cliente preguiçosamente

## 2. Testes de `:data`

- [x] 2.1 Ajustar o único ponto de construção da fonte de dados em `ExchangeRemoteDataSourceTest` (linha 47) à nova assinatura
- [x] 2.2 Confirmar que os testes existentes de caminho feliz e de erro continuam passando sem afrouxar asserção
- [x] 2.3 Acrescentar teste de que o cliente **não** é construído ao resolver a fonte de dados, e é construído na primeira requisição — é a metade JVM da regra nova
- [x] 2.4 Rodar `./gradlew :data:testDebugUnitTest` e confirmar verde

## 3. Verificação dos guardrails

- [x] 3.1 Rodar o comando de G8 completo e confirmar verde
- [x] 3.2 Rodar `./gradlew :app:connectedDebugAndroidTest` (G9) e confirmar que o `ColdStartSmokeTest` volta a passar, sem `DiskReadViolation` no log — **43/43 verdes, zero ocorrências de `DiskReadViolation` ou `StrictMode ThreadPolicy violation` no log**
- [x] 3.3 Confirmar, no log da suíte instrumentada, que nenhuma outra violação de política de *thread* ficou escondida atrás desta — a execução aborta no primeiro teste que morre, então só uma passagem verde prova o conjunto

## 4. Documentação

- [x] 4.1 `data/AGENTS.md`: registrar a regra de dispatcher da camada — IO de `:data` roda em dispatcher de IO, nunca no do chamador — e por que o cliente é construído no primeiro uso

## 5. Fechamento do change irmão

- [x] 5.1 Com G9 verde, atualizar `openspec/changes/add-strictmode-guardrail/tasks.md` (itens 3.2 e 3.3) e arquivar os dois *changes*
