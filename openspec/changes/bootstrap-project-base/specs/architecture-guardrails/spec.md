## Purpose

Define o comportamento verificável das oito proteções mecânicas do projeto (G1–G8): o que exatamente deve **falhar o build** quando uma fronteira de camada, um prefixo de modelo, uma regra de estilo, uma *keep rule* ou a suíte de testes é violada. É o contrato que permite que convenções de arquitetura sejam fatos executáveis em vez de acordos verbais.

## ADDED Requirements

### Requirement: Fronteira Kotlin puro em `:domain` (G1)

O módulo `:domain` SHALL ser compilado como biblioteca Kotlin/JVM pura, sem o plugin Android e sem acesso ao Android SDK, de modo que qualquer referência a `android.*` ou `androidx.*` no módulo resulte em **erro de compilação**.

#### Scenario: Import de Android SDK em `:domain`
- **WHEN** um arquivo de `:domain` importa qualquer símbolo de `android.*` ou `androidx.*`
- **THEN** `./gradlew :domain:compileKotlin` falha com erro de referência não resolvida

#### Scenario: Domínio compila isolado
- **WHEN** `./gradlew :domain:test` é executado sem nenhum emulador ou SDK Android disponível
- **THEN** a compilação e a suíte de testes concluem com sucesso

### Requirement: Grafo de dependências apontando para dentro (G1)

O grafo de módulos SHALL ser exatamente `:app → :data → :domain`. `:domain` MUST NOT declarar dependência de nenhum outro módulo do projeto; `:data` MUST NOT depender de `:app`.

#### Scenario: Dependência declarada corretamente
- **WHEN** o build é configurado
- **THEN** `:app` resolve tipos de `:data` e de `:domain`, e `:data` resolve tipos de `:domain`

#### Scenario: Dependência invertida
- **WHEN** `:domain` ou `:data` declara dependência de um módulo acima na topologia
- **THEN** o Gradle falha a configuração do projeto por dependência circular ou o Konsist reprova o grafo

### Requirement: Verificação estática de camadas e nomenclatura (G2)

O projeto SHALL expor um módulo de teste de arquitetura executável por `./gradlew :konsistTest:test` que reprova, com mensagem identificando o arquivo infrator, as seguintes violações:

- classe de domínio fora do prefixo `BM`, de transporte fora de `DM`, de apresentação fora de `VM`;
- `BM` com anotação de framework (serialização, persistência, `@Parcelize`);
- `DM` referenciado fora de `:data`;
- `VM` referenciado fora de `:app`;
- interface de repositório sem sufixo `Repository`, implementação sem sufixo `RepositoryImpl`, caso de uso sem sufixo `UseCase`;
- `ViewModel` cujo construtor recebe algo que não seja um `UseCase` ou o provedor de recursos.

#### Scenario: ViewModel recebe repositório no construtor
- **WHEN** um `ViewModel` declara um parâmetro de construtor de tipo `*Repository` ou `*DataSource`
- **THEN** `./gradlew :konsistTest:test` falha e a mensagem nomeia a classe infratora

#### Scenario: DataModel vaza para apresentação
- **WHEN** um arquivo sob `:app` importa um tipo com prefixo `DM`
- **THEN** `./gradlew :konsistTest:test` falha e a mensagem nomeia o import infrator

#### Scenario: Modelo de domínio anotado
- **WHEN** uma classe com prefixo `BM` recebe uma anotação de serialização ou persistência
- **THEN** `./gradlew :konsistTest:test` falha

#### Scenario: Projeto conforme
- **WHEN** nenhuma das violações acima está presente
- **THEN** `./gradlew :konsistTest:test` passa

### Requirement: Proibição de texto literal na interface (G2)

Nenhum texto destinado ao usuário SHALL ser escrito literalmente dentro de uma função `@Composable`; todo texto SHALL vir de um recurso localizável. A verificação estática de camadas SHALL reprovar a violação, nomeando a função infratora.

Esta regra vive no G2 e não no G6 porque a verificação de plataforma não enxerga literais em Compose.

#### Scenario: Literal em Composable
- **WHEN** um `@Composable` passa uma string literal para um argumento de texto visível ao usuário
- **THEN** `./gradlew :konsistTest:test` falha nomeando a função infratora

#### Scenario: Texto vindo de recurso
- **WHEN** um `@Composable` obtém seu texto de um recurso localizável
- **THEN** a verificação passa

### Requirement: Análise estática de código (G3)

O projeto SHALL executar análise estática por `./gradlew detekt` sobre os três módulos, configurada por um arquivo de regras versionado na raiz, cobrindo no mínimo: limites de complexidade ciclomática, tamanho de função e de classe, número de funções por classe, tamanho mínimo de nome de função, proibição de *wildcard imports*, e exigência de sufixos de nomenclatura.

#### Scenario: `CancellationException` engolida
- **WHEN** um `try/catch (Throwable)` captura `CancellationException` sem re-lançá-la
- **THEN** `./gradlew detekt` reporta a violação e falha

#### Scenario: Wildcard import
- **WHEN** um arquivo Kotlin usa import com `*`
- **THEN** `./gradlew detekt` falha apontando o arquivo e a linha

#### Scenario: Código conforme
- **WHEN** nenhuma regra configurada é violada
- **THEN** `./gradlew detekt` termina com sucesso e sem *baseline*

### Requirement: Verificação de estilo (G4)

O projeto SHALL verificar formatação por `./gradlew ktlintCheck` usando o *code style* oficial do Kotlin, com as regras versionadas em um arquivo de configuração de editor na raiz do repositório. A formatação automática SHALL estar disponível apenas como tarefa local, nunca acoplada à verificação.

#### Scenario: Arquivo fora do estilo
- **WHEN** um arquivo Kotlin viola indentação, ordenação de imports ou espaçamento definidos na configuração
- **THEN** `./gradlew ktlintCheck` falha listando arquivo, linha e regra

#### Scenario: Correção local
- **WHEN** `./gradlew ktlintFormat` é executado
- **THEN** os arquivos são reescritos em conformidade e `ktlintCheck` passa em seguida

### Requirement: Integridade do *build* de release ofuscado (G5)

O *build type* `release` SHALL executar o encolhedor/ofuscador com as *keep rules* versionadas no repositório, de modo que `./gradlew :app:assembleRelease` produza um artefato em que serialização JSON, roteamento HTTP, resolução de dependências por reflexão e restauração de estado da UI continuem funcionando.

#### Scenario: Release monta com otimização ligada
- **WHEN** `./gradlew :app:assembleRelease` é executado
- **THEN** a otimização está habilitada e o build conclui com sucesso

#### Scenario: Keep rule ausente
- **WHEN** uma classe necessária em tempo de execução por reflexão não está coberta pelas *keep rules*
- **THEN** o teste de fumaça de inicialização a frio executado sobre o artefato de release falha

### Requirement: Verificação de plataforma e de API de Composables (G6)

O projeto SHALL executar `./gradlew :app:lintDebug` com aborto em erro, tratando como **erro** (não aviso) texto literal em layout XML, uso de API acima do nível mínimo suportado, recurso não utilizado, e problemas de acessibilidade e de manifesto; e SHALL incluir as verificações de design de API de funções `@Composable`.

Nota de escopo: a verificação de plataforma **não** alcança texto literal dentro de `@Composable` — o `HardcodedText` do Android Lint analisa apenas layouts XML e não existe detector equivalente para Compose. Essa regra pertence ao G2 (ver *Proibição de texto literal na interface*).

#### Scenario: Texto literal em layout XML
- **WHEN** um layout XML declara um texto literal em vez de referenciar um recurso
- **THEN** `./gradlew :app:lintDebug` falha

#### Scenario: Recurso órfão
- **WHEN** um recurso declarado deixa de ser referenciado por qualquer código ou layout
- **THEN** `./gradlew :app:lintDebug` falha

#### Scenario: Composable com API mal desenhada
- **WHEN** um `@Composable` viola uma das regras de design de API verificadas (por exemplo, modificador ausente ou fora da posição convencional)
- **THEN** `./gradlew :app:lintDebug` falha

### Requirement: Suíte de testes unitários sem emulador (G7)

Os testes unitários dos três módulos e o teste de arquitetura SHALL executar inteiramente na JVM, sem emulador ou dispositivo, e SHALL bloquear a conclusão da feature quando vermelhos. Testes de UI Compose SHALL ser executáveis nessa mesma suíte por meio de uma sombra Android em JVM.

#### Scenario: Suíte JVM completa
- **WHEN** `./gradlew :konsistTest:test :domain:test :data:testDebugUnitTest` é executado em máquina sem emulador
- **THEN** todas as tarefas concluem com sucesso

#### Scenario: Teste de Composable em JVM
- **WHEN** um teste de UI Compose é escrito como teste unitário do `:app`
- **THEN** ele executa na JVM e reporta resultado sem exigir dispositivo

### Requirement: Execução consolidada no fim da feature (G8)

O projeto SHALL disponibilizar a suíte completa de guardrails em um único comando, ordenada do mais barato ao mais caro, e essa suíte SHALL passar integralmente sobre a base entregue por este *change*.

#### Scenario: Suíte verde na base
- **WHEN** `./gradlew detekt ktlintCheck :app:lintDebug :konsistTest:test :domain:test :data:testDebugUnitTest` é executado no repositório recém-preparado
- **THEN** todas as tarefas concluem com sucesso

### Requirement: Guardrail não pode ser enfraquecido

Nenhum guardrail SHALL ser satisfeito por supressão, *baseline*, desabilitação de regra ou substituição por um conjunto de regras mais fraco. O repositório MUST NOT conter arquivos de *baseline* de análise estática nem supressões inline introduzidas para fazer a suíte passar.

#### Scenario: Ausência de baseline
- **WHEN** o repositório é inspecionado após a conclusão deste *change*
- **THEN** não existe arquivo de *baseline* de Detekt ou de Lint, nem diretiva de desabilitação inline de estilo
