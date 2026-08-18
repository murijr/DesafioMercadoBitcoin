# exchange-detail Specification

## Purpose
Define o que o usuário vê e pode fazer na tela de detalhe de uma *exchange*: quais campos identificam e descrevem a corretora, quais moedas ela negocia, como um campo ausente é apresentado sem virar erro, e o que acontece em carregamento, falha e nova tentativa para o detalhe e para a listagem de moedas, cada um de forma independente.

## Requirements

### Requirement: O detalhe identifica a *exchange* e seu conteúdo descritivo

A tela SHALL apresentar o logotipo, o nome, o identificador, a descrição, a *url* do site e a data de lançamento da *exchange* selecionada. O nome e o identificador SHALL estar sempre presentes; os demais campos SHALL degradar individualmente quando o provedor não os fornecer.

#### Scenario: Detalhe com todos os campos
- **WHEN** o provedor fornece logotipo, descrição, *url* do site e data de lançamento da *exchange*
- **THEN** a tela exibe os seis campos, com a data formatada segundo a localidade do dispositivo

#### Scenario: Descrição não fornecida
- **WHEN** o provedor devolve a *exchange* sem descrição
- **THEN** a tela exibe os demais campos e apresenta no lugar da descrição um texto localizável de indisponibilidade

#### Scenario: Url do site não fornecida
- **WHEN** o provedor devolve a *exchange* sem *url* de site
- **THEN** a tela exibe os demais campos e apresenta no lugar do site um texto localizável de indisponibilidade, sem oferecer ação de abrir link

#### Scenario: Data de lançamento não fornecida
- **WHEN** o provedor devolve a *exchange* sem data de lançamento
- **THEN** a tela exibe os demais campos e apresenta no lugar da data um texto localizável de indisponibilidade

#### Scenario: Logotipo indisponível
- **WHEN** a imagem do logotipo não pode ser obtida ou o provedor não informa seu endereço
- **THEN** a tela exibe um marcador visual de substituição no lugar do logotipo, e nenhum outro campo é afetado

### Requirement: O detalhe exibe as taxas de negociação da *exchange*

A tela SHALL apresentar a taxa de *maker* e a taxa de *taker* da *exchange*. Cada taxa SHALL degradar individualmente quando o provedor não a fornecer, e uma taxa igual a zero SHALL ser exibida como zero, e não como indisponível.

#### Scenario: Taxas fornecidas
- **WHEN** o provedor fornece as duas taxas
- **THEN** a tela exibe ambas formatadas como percentual

#### Scenario: Taxa de maker não fornecida
- **WHEN** o provedor devolve a *exchange* sem taxa de *maker*
- **THEN** a tela exibe a taxa de *taker* normalmente e apresenta no lugar da taxa de *maker* um texto localizável de indisponibilidade

#### Scenario: Taxa de taker não fornecida
- **WHEN** o provedor devolve a *exchange* sem taxa de *taker*
- **THEN** a tela exibe a taxa de *maker* normalmente e apresenta no lugar da taxa de *taker* um texto localizável de indisponibilidade

#### Scenario: Taxa igual a zero
- **WHEN** o provedor informa uma taxa igual a zero
- **THEN** a tela exibe o valor zero formatado como percentual, e **não** o texto de indisponibilidade

### Requirement: A listagem exibe as moedas negociadas na *exchange*

A tela SHALL apresentar, para cada moeda negociada pela *exchange* selecionada, seu nome e seu preço em dólar. O nome SHALL estar sempre presente; o preço SHALL degradar quando o provedor não o fornecer, e um preço igual a zero SHALL ser exibido como zero, e não como indisponível.

#### Scenario: Moeda com preço
- **WHEN** o provedor fornece nome e preço em dólar de uma moeda negociada
- **THEN** o item exibe os dois campos, com o preço formatado como moeda

#### Scenario: Moeda sem preço
- **WHEN** o provedor devolve uma moeda negociada sem preço em dólar
- **THEN** o item permanece na listagem, exibe o nome, e apresenta no lugar do preço um texto localizável de indisponibilidade

#### Scenario: Preço igual a zero
- **WHEN** o provedor informa preço em dólar igual a zero para uma moeda
- **THEN** o item exibe o valor zero formatado como moeda, e **não** o texto de indisponibilidade

#### Scenario: Nenhuma moeda negociada
- **WHEN** o provedor devolve a listagem de moedas vazia para a *exchange*
- **THEN** a tela exibe um texto localizável indicando que não há moedas, distinto da mensagem de falha

### Requirement: O carregamento do detalhe e o da listagem de moedas são independentes

O detalhe da *exchange* e a listagem de moedas SHALL ser obtidos por consultas independentes. A falha em uma delas MUST NOT impedir a exibição do resultado bem-sucedido da outra.

#### Scenario: Detalhe obtido, moedas falham
- **WHEN** a consulta do detalhe é bem-sucedida e a consulta da listagem de moedas falha
- **THEN** a tela exibe o detalhe normalmente, e a área de moedas exibe uma mensagem de falha localizada com ação de nova tentativa restrita àquela área

#### Scenario: Moedas obtidas, detalhe falha
- **WHEN** a consulta da listagem de moedas é bem-sucedida e a consulta do detalhe falha
- **THEN** a tela exibe uma mensagem de falha localizada com ação de nova tentativa, e nenhuma listagem de moedas é exibida antes de o detalhe ser obtido com sucesso

#### Scenario: Ambas bem-sucedidas
- **WHEN** as duas consultas são bem-sucedidas
- **THEN** a tela exibe o detalhe e a listagem de moedas, sem esperar uma pela outra

### Requirement: Falha é comunicada com texto localizado e caminho de recuperação

Toda falha SHALL chegar à interface como texto já localizado, nunca como código, exceção ou chave. A tela SHALL sempre oferecer ao usuário uma forma de tentar novamente a consulta que falhou.

#### Scenario: Falha do detalhe
- **WHEN** a obtenção do detalhe falha
- **THEN** a tela exibe uma mensagem localizada correspondente ao tipo de falha e uma ação de nova tentativa do detalhe

#### Scenario: Falha da listagem de moedas
- **WHEN** a obtenção da listagem de moedas falha
- **THEN** a área de moedas exibe uma mensagem localizada correspondente ao tipo de falha e uma ação de nova tentativa restrita à listagem de moedas

#### Scenario: Exchange inexistente
- **WHEN** o identificador recebido pela tela não corresponde a nenhuma *exchange* do provedor
- **THEN** a tela exibe uma mensagem localizada de recurso não encontrado, sem ação de nova tentativa para o detalhe

#### Scenario: Nova tentativa após falha
- **WHEN** o usuário aciona a nova tentativa a partir de um estado de falha, do detalhe ou da listagem de moedas
- **THEN** apenas a consulta correspondente é refeita, e o conteúdo já exibido pela outra consulta permanece na tela
