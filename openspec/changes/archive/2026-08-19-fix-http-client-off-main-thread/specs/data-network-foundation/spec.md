## ADDED Requirements

### Requirement: IO da camada de dados fora da main thread

A camada de dados SHALL executar suas operações de entrada/saída em um dispatcher próprio de IO, e MUST NOT depender do dispatcher de quem a chamou. Nenhuma leitura de disco, escrita em disco ou acesso à rede originado por `:data` SHALL ocorrer na *main thread*.

O cliente HTTP SHALL ser construído no **primeiro uso**, dentro do dispatcher de IO, e MUST NOT ser construído durante a resolução do grafo de dependências — que acontece na *main thread*, quando a tela pede seu `ViewModel`. Construir o cliente ali arrasta para a *main thread* todo o custo de inicialização de suas dependências, incluindo o carregamento por reflexão exigido pelo roteamento *type-safe*.

Esta regra é verificada em execução pelo guardrail de política de *thread* (G10): no *build* de depuração, a violação encerra o processo.

#### Scenario: Requisição não começa na main thread
- **WHEN** um `ViewModel` invoca um caso de uso que alcança a fonte de dados remota a partir de seu escopo, cujo dispatcher é o da interface
- **THEN** a operação de entrada/saída é executada no dispatcher de IO da camada de dados, e não naquele dispatcher

#### Scenario: Cliente construído no primeiro uso
- **WHEN** o grafo de dependências resolve a fonte de dados remota durante a composição da tela
- **THEN** o cliente HTTP ainda não foi construído, e sua construção ocorre apenas quando a primeira requisição é emitida

#### Scenario: Arranque sem violação de política de thread
- **WHEN** `./gradlew :app:connectedDebugAndroidTest` é executado com dispositivo ou emulador conectado
- **THEN** a suíte conclui integralmente com sucesso, sem que nenhum teste seja encerrado por violação de política de *thread*

## MODIFIED Requirements

### Requirement: Fonte de dados remota faz apenas entrada/saída

Uma fonte de dados remota SHALL apenas configurar e emitir a requisição e devolver o modelo de transporte cru, executando-a no dispatcher de IO da camada. Ela MUST NOT aplicar regra de negócio, validação semântica, nem devolver resultado encapsulado — falhas SHALL ser sinalizadas por lançamento.

#### Scenario: Retorno cru
- **WHEN** a requisição é bem-sucedida
- **THEN** a fonte de dados devolve o modelo de transporte sem mapeamento nem interpretação

#### Scenario: Falha de transporte
- **WHEN** a requisição falha por rede, status de erro ou desserialização
- **THEN** a fonte de dados lança, e não devolve um resultado encapsulado

#### Scenario: Cancelamento atravessa
- **WHEN** o escopo do chamador é cancelado durante a requisição
- **THEN** o cancelamento atravessa a fonte de dados sem ser convertido em falha de domínio
