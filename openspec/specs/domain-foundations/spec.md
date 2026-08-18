# domain-foundations Specification

## Purpose
Define o contrato de execução de regra de negócio do projeto: como um caso de uso reporta sucesso e falha, como qualquer exceção do mundo externo vira um erro de domínio tipado, e como uma mensagem de erro chega ao usuário traduzida sem que o domínio conheça recursos de Android.

## Requirements

### Requirement: Caso de uso nunca lança exceção para o chamador

Todo caso de uso SHALL expor uma operação suspensa que recebe uma entrada e devolve um resultado encapsulado — sucesso com o valor produzido, ou falha portando um erro de domínio tipado. Nenhuma exceção crua SHALL escapar para o chamador.

#### Scenario: Caminho feliz
- **WHEN** a execução recebe uma entrada válida e a operação subjacente conclui
- **THEN** o resultado é um sucesso portando o valor esperado

#### Scenario: Falha inesperada de infraestrutura
- **WHEN** a operação subjacente lança uma exceção arbitrária não tipada
- **THEN** o resultado é uma falha portando um erro de domínio, e nenhuma exceção é propagada ao chamador

#### Scenario: Erro já tipado
- **WHEN** a operação subjacente lança um erro que já é de domínio
- **THEN** o resultado é uma falha portando exatamente esse erro, sem remapeamento nem troca de subtipo

### Requirement: Cancelamento respeita *structured concurrency*

O encapsulamento de erros SHALL re-lançar a exceção de cancelamento de coroutine antes de qualquer captura genérica, de modo que o cancelamento do escopo chamador nunca seja convertido em falha de negócio.

#### Scenario: Escopo cancelado durante a execução
- **WHEN** o escopo que invocou o caso de uso é cancelado enquanto a operação está suspensa
- **THEN** a exceção de cancelamento é propagada, e o chamador **não** recebe um resultado de falha

### Requirement: Caso de uso é agnóstico de escopo e de plataforma

Um caso de uso MUST NOT receber, criar ou reter um escopo de coroutine, e MUST NOT depender de `Context`, recursos, ou de qualquer tipo das camadas de dados ou de apresentação. A gestão do escopo SHALL pertencer a quem invoca.

#### Scenario: Assinatura livre de escopo
- **WHEN** as assinaturas dos casos de uso são inspecionadas pelo teste de arquitetura
- **THEN** nenhuma delas declara parâmetro de escopo de coroutine

#### Scenario: Chamador controla o ciclo de vida
- **WHEN** o mesmo caso de uso é invocado a partir de dois escopos distintos
- **THEN** cada invocação é cancelada independentemente, junto do escopo que a originou

### Requirement: Validação de entrada pertence ao caso de uso

A validação semântica da entrada SHALL ocorrer dentro do caso de uso, antes de qualquer chamada ao repositório, e SHALL produzir uma falha de validação sem tocar a camada de dados.

#### Scenario: Entrada inválida
- **WHEN** a execução recebe uma entrada que viola uma precondição de negócio
- **THEN** o resultado é uma falha do tipo validação e nenhuma chamada ao repositório é realizada

### Requirement: Hierarquia fechada de erros de domínio

Os erros de domínio SHALL formar uma hierarquia fechada (exaustiva em tempo de compilação), cobrindo no mínimo validação, recurso não encontrado, falha de rede e falha de desserialização, de modo que o tratamento no ponto de chamada possa ser verificado exaustivamente pelo compilador.

#### Scenario: Tratamento exaustivo
- **WHEN** o consumidor ramifica sobre o erro de domínio sem cláusula de exceção residual
- **THEN** o código compila, e passa a **não** compilar se um novo subtipo de erro for introduzido

#### Scenario: Comparação por identidade do erro
- **WHEN** um teste verifica a falha produzida
- **THEN** é possível comparar o subtipo e seus dados diretamente, sem depender do texto da mensagem

### Requirement: Tradução de exceções externas em erros de domínio

O domínio SHALL oferecer uma conversão de exceção arbitrária em erro de domínio, mapeando falhas de transporte e de entrada/saída para o erro de rede, falhas de desserialização para o erro de serialização, e o restante para um erro genérico de domínio.

#### Scenario: Falha de rede
- **WHEN** uma exceção de entrada/saída ou de resposta HTTP é convertida
- **THEN** o resultado é o erro de domínio de rede

#### Scenario: Falha de desserialização
- **WHEN** uma exceção de desserialização é convertida
- **THEN** o resultado é o erro de domínio de serialização

#### Scenario: Exceção desconhecida
- **WHEN** uma exceção que não corresponde a nenhuma categoria conhecida é convertida
- **THEN** o resultado é o erro de domínio genérico, e nenhuma exceção é lançada pela própria conversão

### Requirement: Mensagens de erro são chaves, não textos

Cada erro de domínio SHALL carregar uma chave de texto pertencente a uma hierarquia fechada. O domínio MUST NOT conter texto literal destinado ao usuário nem referência a recursos de plataforma.

#### Scenario: Erro carrega chave
- **WHEN** um erro de domínio é construído
- **THEN** ele expõe uma chave de texto, e nenhum literal voltado ao usuário aparece no módulo de domínio

#### Scenario: Chave nova exige tradução
- **WHEN** uma nova chave de texto é adicionada à hierarquia
- **THEN** o resolvedor da camada de apresentação deixa de compilar até que a chave receba um recurso correspondente
