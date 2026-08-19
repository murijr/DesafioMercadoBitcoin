## Purpose

Define o que a suíte de testes instrumentados de tela e componente SHALL verificar em dispositivo ou emulador Android real — quais telas, quais componentes isolados, quais cenários — e a garantia de que essa verificação usa o compositor e o ciclo de vida reais do Android, não uma sombra em JVM.

## ADDED Requirements

### Requirement: Suíte instrumentada cobre os fluxos observáveis da listagem

A suíte SHALL verificar, em dispositivo ou emulador real, os estados observáveis da tela de listagem de *exchanges*: carregamento inicial, lista carregada, erro no carregamento inicial com ação de nova tentativa, erro ao carregar um lote posterior sem descartar o conteúdo já exibido, e abertura do detalhe ao tocar em um item.

#### Scenario: Lista carregada com sucesso
- **WHEN** a tela de listagem é aberta em dispositivo/emulador e o provedor responde com sucesso
- **THEN** a suíte verifica que os itens carregados ficam visíveis na tela

#### Scenario: Falha no carregamento inicial
- **WHEN** a obtenção do índice ou do primeiro lote falha
- **THEN** a suíte verifica que a mensagem de falha e a ação de nova tentativa ficam visíveis, e que nenhuma lista é exibida

#### Scenario: Nova tentativa restaura a lista
- **WHEN** o usuário aciona a nova tentativa a partir do estado de falha e o provedor passa a responder com sucesso
- **THEN** a suíte verifica que a lista substitui a mensagem de falha

#### Scenario: Toque em item navega ao detalhe
- **WHEN** o usuário toca em um item da lista carregada
- **THEN** a suíte verifica que a tela de detalhe correspondente àquele item é aberta

### Requirement: Suíte instrumentada cobre os fluxos observáveis do detalhe

A suíte SHALL verificar, em dispositivo ou emulador real, os estados observáveis da tela de detalhe de uma *exchange*: carregamento, detalhe exibido, listagem de moedas exibida, e a independência entre a falha de uma consulta e o sucesso da outra.

#### Scenario: Detalhe e moedas carregados com sucesso
- **WHEN** a tela de detalhe é aberta em dispositivo/emulador e as duas consultas (detalhe e moedas) respondem com sucesso
- **THEN** a suíte verifica que os campos do detalhe e a listagem de moedas ficam visíveis

#### Scenario: Detalhe exibido apesar de falha nas moedas
- **WHEN** a consulta do detalhe é bem-sucedida e a consulta das moedas falha
- **THEN** a suíte verifica que o detalhe permanece visível e que a área de moedas exibe mensagem de falha com nova tentativa restrita àquela área

#### Scenario: Nova tentativa restrita à consulta que falhou
- **WHEN** o usuário aciona a nova tentativa a partir de uma falha restrita ao detalhe ou às moedas
- **THEN** a suíte verifica que apenas o conteúdo daquela consulta é substituído, e o conteúdo já exibido pela outra consulta permanece na tela

### Requirement: Componentes isolados são verificados fora do contexto da tela inteira

`ExchangeListItem`, `ExchangeDetailHeader` e `CurrencyListItem` SHALL cada um ter cobertura própria na suíte instrumentada, renderizado isoladamente com dados de entrada controlados, sem montar a tela completa em que normalmente aparecem.

#### Scenario: Componente renderiza os dados recebidos
- **WHEN** um componente isolado é renderizado com um conjunto de dados válido
- **THEN** a suíte verifica que os campos correspondentes ficam visíveis com os valores fornecidos

#### Scenario: Componente degrada campo ausente
- **WHEN** um componente isolado é renderizado com um campo opcional ausente
- **THEN** a suíte verifica que o texto localizável de indisponibilidade aparece no lugar daquele campo, sem afetar os demais

#### Scenario: Componente interativo dispara callback ao toque
- **WHEN** um componente isolado que aceita toque (por exemplo, um item de lista) é tocado durante o teste
- **THEN** a suíte verifica que o callback correspondente é disparado exatamente uma vez

### Requirement: Suíte roda em dispositivo/emulador real e é aditiva à suíte JVM

A suíte instrumentada SHALL executar sobre o compositor e o ciclo de vida reais do Android (não uma sombra em JVM), e é aditiva à suíte de testes de Compose que já roda em JVM: nenhuma das duas substitui a outra, e ambas continuam existindo lado a lado.

#### Scenario: Execução em dispositivo real
- **WHEN** a suíte instrumentada é executada
- **THEN** ela roda em um dispositivo ou emulador Android real, e não em uma simulação do *framework* Android sobre a JVM

#### Scenario: Suíte JVM permanece intacta
- **WHEN** a suíte instrumentada é adicionada ao projeto
- **THEN** os testes de tela existentes na suíte JVM continuam existindo e continuam passando, sem depender de dispositivo ou emulador

### Requirement: Elementos de tela expõem identificadores estáveis para a suíte instrumentada

Todo elemento verificado pela suíte instrumentada SHALL ser localizável por um identificador semântico estável, independente do texto exibido — que varia por localidade — e independente de mudanças puramente visuais.

#### Scenario: Localização independente de idioma
- **WHEN** a suíte instrumentada localiza um elemento para verificação ou interação
- **THEN** ela o faz por um identificador semântico estável, e a troca do idioma do dispositivo não quebra a localização do elemento
