## MODIFIED Requirements

### Requirement: Verificação estática de camadas e nomenclatura (G2)

O projeto SHALL expor um módulo de teste de arquitetura executável por `./gradlew :konsistTest:test` que reprova, com mensagem identificando o arquivo infrator, as seguintes violações:

- classe de domínio fora do prefixo `BM`, de transporte fora de `DM`, de apresentação fora de `VM`;
- classe com prefixo `BM`, `DM` ou `VM` cujo nome termina em rótulo de camada — o prefixo já declara em que camada o objeto vive, e repeti-lo no fim do nome é redundância. A lista negada SHALL conter, no mínimo, `Dto`, `Model`, `Entity`, `Data`, `Payload`, `Body`, `Json` e `Schema`, em qualquer variação de caixa;
- `BM` com anotação de framework (serialização, persistência, `@Parcelize`);
- `DM` referenciado fora de `:data`;
- `VM` referenciado fora de `:app`;
- interface de repositório sem sufixo `Repository`, implementação sem sufixo `RepositoryImpl`, caso de uso sem sufixo `UseCase`;
- `ViewModel` cujo construtor recebe algo que não seja um `UseCase` ou o provedor de recursos.

Sufixo que descreve a **forma** do dado, e não a camada — por exemplo o envelope de uma resposta ou o elemento de uma coleção —, MUST NOT ser reprovado: ele carrega informação que o prefixo não carrega.

#### Scenario: ViewModel recebe repositório no construtor
- **WHEN** um `ViewModel` declara um parâmetro de construtor de tipo `*Repository` ou `*DataSource`
- **THEN** `./gradlew :konsistTest:test` falha e a mensagem nomeia a classe infratora

#### Scenario: DataModel vaza para apresentação
- **WHEN** um arquivo sob `:app` importa um tipo com prefixo `DM`
- **THEN** `./gradlew :konsistTest:test` falha e a mensagem nomeia o import infrator

#### Scenario: Modelo de domínio anotado
- **WHEN** uma classe com prefixo `BM` recebe uma anotação de serialização ou persistência
- **THEN** `./gradlew :konsistTest:test` falha

#### Scenario: Modelo de transporte com rótulo de camada no fim do nome
- **WHEN** uma classe com prefixo `DM` é nomeada terminando em `Dto`, `Model`, `Entity`, `Data`, `Payload`, `Body`, `Json` ou `Schema`
- **THEN** `./gradlew :konsistTest:test` falha e a mensagem nomeia a classe infratora

#### Scenario: Modelo de domínio ou de apresentação com rótulo de camada no fim do nome
- **WHEN** uma classe com prefixo `BM` ou `VM` é nomeada terminando em um dos rótulos negados
- **THEN** `./gradlew :konsistTest:test` falha e a mensagem nomeia a classe infratora

#### Scenario: Sufixo estrutural preservado
- **WHEN** uma classe com prefixo `DM` é nomeada terminando em um termo que descreve a forma do dado, como o envelope de uma resposta ou o elemento de uma coleção
- **THEN** `./gradlew :konsistTest:test` passa quanto a essa regra

#### Scenario: Projeto conforme
- **WHEN** nenhuma das violações acima está presente
- **THEN** `./gradlew :konsistTest:test` passa
