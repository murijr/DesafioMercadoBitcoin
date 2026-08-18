## Purpose

Define o que o usuário vê e pode fazer na tela que lista as *exchanges* de criptomoedas do provedor: quais dados identificam cada corretora, como uma lista grande é revelada em partes conforme a rolagem, como um dado indisponível é apresentado sem virar erro, e o que acontece em carregamento, falha, lista vazia e seleção de um item.

## ADDED Requirements

### Requirement: Cada item da lista identifica a *exchange* e suas métricas

Todo item exibido SHALL apresentar o logotipo da *exchange*, seu nome, seu volume de negociação *spot* em dólar e sua data de lançamento. O nome SHALL estar sempre presente; os demais campos SHALL degradar individualmente quando o provedor não os fornecer.

#### Scenario: Item com todos os dados
- **WHEN** o provedor fornece logotipo, nome, volume e data de lançamento de uma *exchange*
- **THEN** o item exibe os quatro campos, com o volume formatado como moeda e a data formatada segundo a localidade do dispositivo

#### Scenario: Volume não fornecido
- **WHEN** o provedor devolve a *exchange* sem valor de volume *spot*
- **THEN** o item permanece na lista, exibe os demais campos, e apresenta no lugar do volume um texto localizável de indisponibilidade

#### Scenario: Data de lançamento não fornecida
- **WHEN** o provedor devolve a *exchange* sem data de lançamento
- **THEN** o item permanece na lista, exibe os demais campos, e apresenta no lugar da data um texto localizável de indisponibilidade

#### Scenario: Logotipo indisponível
- **WHEN** a imagem do logotipo não pode ser obtida ou o provedor não informa seu endereço
- **THEN** o item exibe um marcador visual de substituição no lugar do logotipo, e nenhum outro campo do item é afetado

#### Scenario: Volume igual a zero
- **WHEN** o provedor informa volume *spot* igual a zero
- **THEN** o item exibe o valor zero formatado como moeda, e **não** o texto de indisponibilidade

### Requirement: A lista compõe índice e conteúdo do provedor

O provedor expõe o catálogo de *exchanges* em duas consultas distintas: um **índice**, que enumera as corretoras ativas e seus identificadores, e um **conteúdo**, que fornece os campos exibíveis para um conjunto de identificadores e é limitado a 100 identificadores por consulta. A lista SHALL ser produzida pela composição das duas, e MUST NOT exibir uma *exchange* cujo conteúdo ainda não foi obtido.

#### Scenario: Composição bem-sucedida
- **WHEN** o índice devolve um conjunto de identificadores e o conteúdo é obtido para eles
- **THEN** cada *exchange* aparece uma única vez, com os campos vindos do conteúdo

#### Scenario: Identificador sem conteúdo correspondente
- **WHEN** o índice enumera um identificador que a consulta de conteúdo não devolve
- **THEN** essa *exchange* é omitida da lista, e as demais do mesmo lote são exibidas normalmente

#### Scenario: Índice restrito a corretoras ativas
- **WHEN** o índice é consultado
- **THEN** apenas *exchanges* em atividade são consideradas para exibição

### Requirement: A lista é revelada em lotes conforme a rolagem

O índice SHALL ser obtido uma única vez por sessão de tela. O conteúdo SHALL ser obtido em lotes sucessivos, respeitando o limite de identificadores por consulta do provedor, e um novo lote SHALL ser solicitado apenas quando o usuário se aproximar do fim do que já está exibido.

#### Scenario: Primeiro lote
- **WHEN** a tela é aberta
- **THEN** o índice é obtido e, em seguida, o conteúdo do primeiro lote, e a lista se torna visível sem esperar pelos lotes seguintes

#### Scenario: Avanço por rolagem
- **WHEN** o usuário rola até próximo do fim dos itens já exibidos e ainda restam identificadores no índice
- **THEN** o lote seguinte é solicitado e acrescentado ao fim da lista, com indicação visual de que há carga em andamento

#### Scenario: Fim do catálogo
- **WHEN** o conteúdo de todos os identificadores do índice já foi obtido
- **THEN** rolar até o fim não dispara nova solicitação, e nenhuma indicação de carga é exibida

#### Scenario: Solicitação concorrente
- **WHEN** o usuário continua rolando enquanto um lote ainda está sendo obtido
- **THEN** nenhuma solicitação duplicada para o mesmo lote é emitida

#### Scenario: Nenhum item repetido
- **WHEN** vários lotes já foram acrescentados
- **THEN** nenhuma *exchange* aparece mais de uma vez na lista

### Requirement: A ordem do índice é preservada

A lista SHALL apresentar as *exchanges* na ordem em que o índice as enumera. Um item já exibido MUST NOT mudar de posição quando lotes posteriores forem acrescentados.

#### Scenario: Ordem dentro de um lote
- **WHEN** um lote é acrescentado à lista
- **THEN** seus itens aparecem na mesma ordem relativa em que o índice os enumera

#### Scenario: Posição estável
- **WHEN** um novo lote é acrescentado
- **THEN** os itens já visíveis permanecem nas mesmas posições, e os novos entram apenas ao fim

### Requirement: Falha é comunicada com texto localizado e caminho de recuperação

Toda falha SHALL chegar à interface como texto já localizado, nunca como código, exceção ou chave. A tela SHALL sempre oferecer ao usuário uma forma de tentar novamente, e uma falha ao carregar um lote posterior MUST NOT descartar o conteúdo já exibido.

#### Scenario: Falha no carregamento inicial
- **WHEN** a obtenção do índice ou do primeiro lote falha
- **THEN** a tela exibe uma mensagem localizada correspondente ao tipo de falha e uma ação de nova tentativa, e nenhuma lista é exibida

#### Scenario: Nova tentativa após falha inicial
- **WHEN** o usuário aciona a nova tentativa a partir do estado de falha
- **THEN** o carregamento recomeça e, em caso de sucesso, a lista substitui a mensagem de falha

#### Scenario: Falha ao carregar lote posterior
- **WHEN** a obtenção de um lote seguinte falha
- **THEN** os itens já exibidos permanecem na tela, a falha é comunicada de forma não destrutiva, e uma nova tentativa apenas daquele lote é oferecida

#### Scenario: Chave de acesso ausente ou recusada
- **WHEN** o provedor recusa a requisição por credencial ausente ou inválida
- **THEN** o usuário observa a mensagem localizada de indisponibilidade de rede, e não uma mensagem de erro inesperado

#### Scenario: Limite de uso do provedor atingido
- **WHEN** o provedor recusa a requisição por exceder o limite de chamadas
- **THEN** o usuário observa uma mensagem localizada de indisponibilidade temporária, e a ação de nova tentativa permanece disponível

### Requirement: Ausência de resultados é distinta de falha

Quando o provedor responde com sucesso e nenhuma *exchange* atende ao critério de exibição, a tela SHALL apresentar um estado de lista vazia, distinto do estado de falha e do estado de carregamento.

#### Scenario: Catálogo vazio
- **WHEN** o índice é obtido com sucesso e não enumera nenhuma *exchange* ativa
- **THEN** a tela exibe uma mensagem localizada de lista vazia, sem mensagem de erro e sem indicador de carga

### Requirement: Selecionar um item abre o detalhe daquela *exchange*

Tocar em um item da lista SHALL levar o usuário a um destino de detalhe correspondente àquela *exchange*, carregando consigo a identificação necessária para que o destino saiba qual corretora apresentar.

#### Scenario: Toque em um item
- **WHEN** o usuário toca em um item da lista
- **THEN** o destino de detalhe é aberto e apresenta a *exchange* correspondente ao item tocado

#### Scenario: Retorno para a lista
- **WHEN** o usuário retorna do detalhe para a listagem
- **THEN** a lista é reapresentada com os mesmos itens já carregados e na mesma posição de rolagem, sem refazer as consultas ao provedor

### Requirement: O conteúdo já carregado sobrevive à recriação da tela

O conteúdo obtido SHALL ser preservado através de mudança de configuração e de recriação da tela, de modo que uma rotação de dispositivo não gere novas consultas ao provedor para dados já obtidos.

#### Scenario: Mudança de configuração
- **WHEN** o dispositivo é rotacionado com a lista já carregada
- **THEN** a lista continua exibida com os mesmos itens, e nenhuma consulta ao provedor é reemitida para os lotes já obtidos
