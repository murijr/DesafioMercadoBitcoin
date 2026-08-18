# app-shell Specification

## Purpose
Define a casca executável do aplicativo: o que acontece quando ele é iniciado, como o grafo de dependências fica disponível para as telas, e como um erro de domínio se torna um texto localizado na interface sem que domínio ou dados conheçam recursos de Android.

## Requirements

### Requirement: Aplicativo inicia em uma tela Compose

O aplicativo SHALL declarar uma atividade de entrada exportada que renderiza conteúdo Compose sob o tema Material 3 do projeto, com suporte a tema claro e escuro.

#### Scenario: Inicialização a frio
- **WHEN** o aplicativo é iniciado a partir do lançador
- **THEN** a atividade de entrada abre e renderiza conteúdo Compose sem falha

#### Scenario: Tema escuro do sistema
- **WHEN** o sistema está em tema escuro
- **THEN** a interface é renderizada com a paleta escura do tema do projeto

#### Scenario: Atividade declarada corretamente
- **WHEN** o manifesto é verificado pela análise de plataforma
- **THEN** a atividade de entrada declara explicitamente sua exportação, sem aviso ou erro

### Requirement: Grafo de dependências disponível na inicialização

O grafo de injeção de dependências SHALL ser iniciado uma única vez no arranque do processo, agregando os módulos de todas as camadas, de modo que qualquer tela obtenha seu `ViewModel` já resolvido.

#### Scenario: Grafo consistente
- **WHEN** o grafo é iniciado e verificado
- **THEN** toda dependência declarada é resolvível, e nenhuma definição fica sem provedor

#### Scenario: Resolução de ViewModel pela tela
- **WHEN** um Composable de tela solicita seu `ViewModel`
- **THEN** a instância é fornecida pelo grafo, com o ciclo de vida da tela

#### Scenario: Ligação de contrato a implementação
- **WHEN** um caso de uso declara depender da interface de repositório definida no domínio
- **THEN** o grafo entrega a implementação da camada de dados, sem que a apresentação referencie essa implementação

### Requirement: Apresentação depende apenas de casos de uso e do provedor de recursos

Um `ViewModel` SHALL declarar como dependências exclusivamente casos de uso e o provedor de recursos. Ele MUST NOT declarar dependência de repositório, de fonte de dados ou de qualquer tipo da camada de dados.

#### Scenario: Construtor conforme
- **WHEN** os construtores de `ViewModel` são verificados
- **THEN** todos os parâmetros são casos de uso ou o provedor de recursos

### Requirement: Erro de domínio vira texto localizado antes de chegar à UI

O provedor de recursos SHALL resolver, de forma exaustiva, cada chave de texto da hierarquia do domínio para um recurso de texto localizável. A conversão SHALL ocorrer na camada de apresentação, antes de o erro ser publicado no estado ou como efeito.

#### Scenario: Erro publicado como texto
- **WHEN** um caso de uso devolve falha com um erro de domínio
- **THEN** a camada de apresentação publica um texto já resolvido, e não a chave nem o erro cru

#### Scenario: Cobertura exaustiva das chaves
- **WHEN** uma nova chave de texto é introduzida no domínio
- **THEN** a resolução deixa de compilar até que a chave receba um recurso correspondente

#### Scenario: Domínio e dados sem `Context`
- **WHEN** os módulos de domínio e de dados são inspecionados
- **THEN** nenhum deles referencia `Context` de interface ou recursos do aplicativo

### Requirement: Interface não contém texto literal

Nenhum texto visível ao usuário SHALL ser escrito literalmente em Composable ou em modelo de estado; todo texto SHALL vir de recurso localizável.

#### Scenario: Literal em Composable
- **WHEN** um Composable renderiza um texto literal
- **THEN** a verificação de plataforma falha o build com severidade de erro

### Requirement: Estado sobrevive à morte do processo

O estado de tela SHALL ser persistido de forma a ser restaurado após a destruição do processo pelo sistema, e o contrato de apresentação SHALL expor estado como fluxo contínuo e efeitos de disparo único como fluxo separado.

#### Scenario: Restauração após morte do processo
- **WHEN** o processo é destruído pelo sistema e a tela é recriada
- **THEN** o estado anterior é restaurado sem nova busca de dados obrigatória

#### Scenario: Efeito consumido uma única vez
- **WHEN** um efeito de disparo único é emitido e consumido, e em seguida a tela é recomposta
- **THEN** o efeito não é reapresentado

#### Scenario: Entrada única de coroutine
- **WHEN** os `ViewModel` são inspecionados
- **THEN** a abertura de coroutine ocorre apenas no ponto único de recepção de eventos, e não nos tratadores individuais
