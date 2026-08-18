## MODIFIED Requirements

### Requirement: Aplicativo inicia em uma tela Compose

O aplicativo SHALL declarar uma atividade de entrada exportada que renderiza conteúdo Compose sob o tema Material 3 do projeto, com suporte a tema claro e escuro. O conteúdo inicial SHALL ser a primeira tela funcional do produto — a listagem de *exchanges* —, e não um conteúdo de espaço reservado.

#### Scenario: Inicialização a frio
- **WHEN** o aplicativo é iniciado a partir do lançador
- **THEN** a atividade de entrada abre e renderiza a listagem de *exchanges* sem falha

#### Scenario: Tema escuro do sistema
- **WHEN** o sistema está em tema escuro
- **THEN** a interface é renderizada com a paleta escura do tema do projeto

#### Scenario: Atividade declarada corretamente
- **WHEN** o manifesto é verificado pela análise de plataforma
- **THEN** a atividade de entrada declara explicitamente sua exportação, sem aviso ou erro

## ADDED Requirements

### Requirement: Casca hospeda múltiplos destinos com pilha de retorno

A casca do aplicativo SHALL hospedar mais de um destino de navegação sobre uma pilha de retorno observável, de modo que abrir um destino empilhe e o gesto ou botão de voltar do sistema desempilhe. A pilha SHALL ser o único lugar que decide qual destino está visível.

#### Scenario: Avanço para um destino
- **WHEN** a interação do usuário solicita a abertura de outro destino
- **THEN** o novo destino passa a ser exibido e o anterior permanece na pilha

#### Scenario: Retorno pelo sistema
- **WHEN** o usuário aciona o retorno do sistema com mais de um destino empilhado
- **THEN** o destino do topo é desempilhado e o anterior volta a ser exibido

#### Scenario: Retorno no destino inicial
- **WHEN** o usuário aciona o retorno do sistema estando no destino inicial
- **THEN** o aplicativo entrega o retorno ao sistema, em vez de exibir uma tela vazia

#### Scenario: Pilha sobrevive à recriação
- **WHEN** a atividade é recriada por mudança de configuração
- **THEN** a pilha de retorno é restaurada com os mesmos destinos e na mesma ordem

### Requirement: Destino carrega apenas dados de identificação

Um destino de navegação SHALL transportar somente os dados necessários para se identificar, em forma serializável. Um destino MUST NOT transportar modelo de negócio, modelo de transporte ou objeto de estado de tela.

#### Scenario: Destino parametrizado
- **WHEN** um destino que representa um item específico é aberto
- **THEN** ele recebe o identificador do item, e busca o restante por meio de seu próprio `ViewModel`

#### Scenario: Sobrevivência à morte do processo
- **WHEN** o processo é destruído pelo sistema e restaurado a partir de um destino parametrizado
- **THEN** o destino é recriado com o mesmo identificador que possuía antes

### Requirement: Cada destino resolve seu próprio `ViewModel` com escopo próprio

Cada destino SHALL obter seu `ViewModel` do grafo de dependências com ciclo de vida atrelado à sua entrada na pilha, de modo que sair do destino libere o `ViewModel` e voltar a ele por avanço crie uma instância nova.

#### Scenario: Escopo por entrada na pilha
- **WHEN** um destino é desempilhado
- **THEN** o `ViewModel` associado àquela entrada é encerrado

#### Scenario: Estado preservado ao empilhar por cima
- **WHEN** outro destino é empilhado por cima e depois desempilhado
- **THEN** o destino de baixo reencontra seu `ViewModel` e seu estado, sem recriação
