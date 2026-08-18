## MODIFIED Requirements

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

## ADDED Requirements

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
