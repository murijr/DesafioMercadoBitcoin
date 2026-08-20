# data-network-foundation Specification

## Purpose
Define o comportamento do único ponto de saída HTTP do aplicativo: como as requisições à API pública da CoinMarketCap são autenticadas, serializadas e limitadas no tempo, e como uma falha de transporte atravessa a fronteira da camada de dados já convertida em erro de domínio.

## Requirements

### Requirement: Cliente HTTP único e centralizado

A camada de dados SHALL montar seus clientes HTTP em um único ponto de configuração, responsável por engine, negociação de conteúdo, registro de log e limites de tempo. Nenhum outro ponto do aplicativo SHALL construir cliente HTTP próprio, incluindo bibliotecas de terceiros que tragam a própria pilha de rede.

Esse ponto único SHALL produzir exatamente dois clientes, com propósitos e credenciais distintos:

1. o **cliente da API**, autenticado com a chave do provedor, usado para toda consulta de dados;
2. o **cliente de imagens**, sem credencial alguma, usado exclusivamente para obter recursos binários de hosts de conteúdo estático.

A chave de API MUST NOT acompanhar requisição emitida pelo cliente de imagens.

#### Scenario: Ponto único de configuração
- **WHEN** o código do projeto é inspecionado
- **THEN** existe exatamente um ponto de construção de cliente HTTP, ele produz os dois clientes previstos, e ambos são obtidos pelos consumidores por injeção de dependência

#### Scenario: Credencial restrita ao cliente da API
- **WHEN** uma requisição de imagem é emitida
- **THEN** ela não carrega o cabeçalho de chave de API do provedor

#### Scenario: Biblioteca de imagem sem pilha própria
- **WHEN** as dependências resolvidas do aplicativo são inspecionadas
- **THEN** nenhuma segunda biblioteca cliente HTTP é introduzida para a carga de imagens

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

Uma fonte de dados remota SHALL apenas configurar e emitir a requisição e devolver o modelo de transporte cru, executando-a no dispatcher de quem a chama. Ela MUST NOT aplicar regra de negócio, validação semântica, nem devolver resultado encapsulado — falhas SHALL ser sinalizadas por lançamento.

#### Scenario: Retorno cru
- **WHEN** a requisição é bem-sucedida
- **THEN** a fonte de dados devolve o modelo de transporte sem mapeamento nem interpretação

#### Scenario: Falha de transporte
- **WHEN** a requisição falha por rede, status de erro ou desserialização
- **THEN** a fonte de dados lança, e não devolve um resultado encapsulado

#### Scenario: Cancelamento atravessa
- **WHEN** o escopo do chamador é cancelado durante a requisição
- **THEN** o cancelamento atravessa a fonte de dados sem ser convertido em falha de domínio

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

A conversão SHALL cobrir também as respostas com **status HTTP de erro**, e não apenas as falhas de entrada/saída e de desserialização. Uma resposta recusada pelo servidor MUST NOT chegar ao consumidor como erro de domínio inespecífico quando existe subtipo adequado.

#### Scenario: Erro de rede tipado no consumidor
- **WHEN** a fonte de dados lança uma exceção de entrada/saída
- **THEN** o consumidor da camada observa um erro de domínio de rede

#### Scenario: Erro de serialização tipado no consumidor
- **WHEN** a fonte de dados lança uma exceção de desserialização
- **THEN** o consumidor da camada observa um erro de domínio de serialização

#### Scenario: Credencial recusada pelo provedor
- **WHEN** o provedor responde com status de não autorizado ou de acesso proibido, inclusive por a chave de API estar ausente na configuração local
- **THEN** o consumidor da camada observa um erro de domínio de rede, e **não** um erro inesperado

#### Scenario: Limite de chamadas excedido
- **WHEN** o provedor responde com status de excesso de requisições
- **THEN** o consumidor da camada observa um erro de domínio de rede, e **não** um erro inesperado

#### Scenario: Recurso inexistente
- **WHEN** o provedor responde com status de recurso não encontrado
- **THEN** o consumidor da camada observa um erro de domínio de recurso não encontrado

#### Scenario: Falha do servidor
- **WHEN** o provedor responde com status de erro de servidor
- **THEN** o consumidor da camada observa um erro de domínio de rede

#### Scenario: Cancelamento preservado
- **WHEN** a coroutine que executa a requisição é cancelada
- **THEN** o cancelamento é propagado e **não** é convertido em erro de domínio

### Requirement: Consulta em lote respeita o limite do provedor

Quando o provedor limita a quantidade de identificadores aceitos por consulta, a camada de dados SHALL respeitar esse limite ao montar a requisição, e MUST NOT delegar ao chamador a responsabilidade de fatiar o conjunto.

#### Scenario: Conjunto dentro do limite
- **WHEN** a camada recebe um conjunto de identificadores dentro do limite do provedor
- **THEN** uma única requisição é emitida contendo todos eles

#### Scenario: Conjunto acima do limite
- **WHEN** a camada recebe um conjunto de identificadores maior que o limite do provedor
- **THEN** a requisição não é emitida com o conjunto integral, e a violação do limite é impedida antes de alcançar a rede

#### Scenario: Conjunto vazio
- **WHEN** a camada recebe um conjunto vazio de identificadores
- **THEN** nenhuma requisição é emitida e o resultado é um conjunto vazio

### Requirement: Construção do cliente HTTP fora da main thread

O cliente HTTP SHALL ser construído dentro de um dispatcher próprio de IO, independente de quando a construção for disparada — inclusive durante a resolução do grafo de dependências, que acontece na *main thread*, quando a tela pede seu `ViewModel`. Sem essa garantia, o custo de inicialização de suas dependências, incluindo o carregamento por reflexão exigido pelo roteamento *type-safe*, cairia na *main thread*.

Esta regra é verificada em execução pelo guardrail de política de *thread* (G10): no *build* de depuração, a violação encerra o processo.

#### Scenario: Cliente não é construído na main thread
- **WHEN** o grafo de dependências resolve a fonte de dados remota durante a composição da tela
- **THEN** a construção do cliente HTTP roda dentro do dispatcher de IO, não na *main thread* que disparou a resolução

#### Scenario: Arranque sem violação de política de thread
- **WHEN** `./gradlew :app:connectedDebugAndroidTest` é executado com dispositivo ou emulador conectado
- **THEN** a suíte conclui integralmente com sucesso, sem que nenhum teste seja encerrado por violação de política de *thread*
