# data-network-foundation Specification

## Purpose
Define o comportamento do único ponto de saída HTTP do aplicativo: como as requisições à API pública da CoinMarketCap são autenticadas, serializadas e limitadas no tempo, e como uma falha de transporte atravessa a fronteira da camada de dados já convertida em erro de domínio.

## Requirements

### Requirement: Cliente HTTP único e centralizado

A camada de dados SHALL montar seu cliente HTTP em um único ponto de configuração, responsável por engine, negociação de conteúdo, registro de log e limites de tempo. Nenhum outro ponto do aplicativo SHALL construir cliente HTTP próprio.

#### Scenario: Ponto único de configuração
- **WHEN** o código do projeto é inspecionado
- **THEN** existe exatamente uma construção de cliente HTTP, e ela é obtida pelos consumidores por injeção de dependência

#### Scenario: Limite de tempo aplicado
- **WHEN** o servidor não responde dentro do limite configurado
- **THEN** a requisição é interrompida e a falha atravessa a camada como erro de domínio de rede, e não como travamento indefinido

### Requirement: Autenticação na CoinMarketCap por chave injetada

Toda requisição à API da CoinMarketCap SHALL enviar a chave de API no cabeçalho de autenticação do provedor. A chave SHALL ser fornecida à camada de dados por injeção a partir da configuração de build do aplicativo, e MUST NOT estar embutida literalmente no código da camada de dados.

#### Scenario: Cabeçalho presente
- **WHEN** uma requisição é emitida pelo cliente configurado
- **THEN** ela carrega o cabeçalho de chave de API do provedor com o valor injetado

#### Scenario: Chave ausente na configuração local
- **WHEN** o projeto é compilado em uma máquina sem a chave configurada
- **THEN** o build conclui com sucesso, e a ausência da chave se manifesta apenas em tempo de execução como falha de rede tratada

#### Scenario: Chave não versionada
- **WHEN** o repositório é inspecionado
- **THEN** nenhum valor de chave de API real está versionado

### Requirement: Desserialização tolerante e explícita

As respostas JSON SHALL ser desserializadas em modelos de transporte com configuração que ignore campos desconhecidos, de modo que a adição de campos pela API não quebre o aplicativo.

#### Scenario: Campo novo na resposta
- **WHEN** a resposta contém um campo não declarado no modelo de transporte
- **THEN** a desserialização conclui com sucesso e o campo é ignorado

#### Scenario: Campo obrigatório ausente
- **WHEN** a resposta omite um campo obrigatório do modelo de transporte
- **THEN** a falha atravessa a camada como erro de domínio de serialização

### Requirement: Fonte de dados remota faz apenas entrada/saída

Uma fonte de dados remota SHALL apenas configurar e emitir a requisição e devolver o modelo de transporte cru. Ela MUST NOT aplicar regra de negócio, validação semântica, nem devolver resultado encapsulado — falhas SHALL ser sinalizadas por lançamento.

#### Scenario: Retorno cru
- **WHEN** a requisição é bem-sucedida
- **THEN** a fonte de dados devolve o modelo de transporte sem mapeamento nem interpretação

#### Scenario: Falha de transporte
- **WHEN** a requisição falha por rede, status de erro ou desserialização
- **THEN** a fonte de dados lança, e não devolve um resultado encapsulado

### Requirement: Modelo de transporte não atravessa a fronteira

A camada de dados MUST NOT expor modelos de transporte para fora de si. O que cruza a fronteira SHALL ser modelo de negócio, produzido por uma função de mapeamento de sentido único.

#### Scenario: Assinatura pública da camada
- **WHEN** as assinaturas públicas das implementações de repositório são inspecionadas
- **THEN** nenhum tipo de transporte aparece em parâmetro ou tipo de retorno

#### Scenario: Mapeamento de sentido único
- **WHEN** uma função de mapeamento é inspecionada
- **THEN** ela converte transporte em negócio em um único passo, sem encadeamento de conversões

### Requirement: Conversão de falha ocorre em um ponto definido

A conversão de exceção de transporte em erro de domínio SHALL acontecer em exatamente um ponto do fluxo, de modo que o erro observado pelo caso de uso seja sempre um erro de domínio tipado e nunca uma exceção da biblioteca de rede.

#### Scenario: Erro de rede tipado no consumidor
- **WHEN** a fonte de dados lança uma exceção de entrada/saída
- **THEN** o consumidor da camada observa um erro de domínio de rede

#### Scenario: Erro de serialização tipado no consumidor
- **WHEN** a fonte de dados lança uma exceção de desserialização
- **THEN** o consumidor da camada observa um erro de domínio de serialização

#### Scenario: Cancelamento preservado
- **WHEN** a coroutine que executa a requisição é cancelada
- **THEN** o cancelamento é propagado e **não** é convertido em erro de domínio
