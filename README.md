# Sistema de Controle Financeiro

Sistema pessoal de controle financeiro desenvolvido com o objetivo de acompanhar contas, movimentações, compromissos financeiros, crédito, recorrências e projeções futuras.

O projeto também tem como objetivo servir como estudo prático de **Java, Spring Boot, MongoDB e desenvolvimento de uma aplicação modular**, aplicando as decisões de domínio definidas antes da implementação.

---

## 📌 Objetivos

O sistema deve permitir:

* Gerenciar o perfil financeiro do usuário.
* Gerenciar contas financeiras.
* Registrar entradas, despesas e transferências.
* Criar transações recorrentes.
* Organizar movimentações por categorias.
* Gerenciar obrigações financeiras.
* Registrar pagamentos parciais ou integrais.
* Gerenciar utilização de crédito.
* Organizar operações de crédito em faturas mensais.
* Gerar reembolsos quando operações de crédito forem revertidas após pagamentos.
* Projetar a evolução financeira futura.
* Disponibilizar informações consolidadas para um dashboard financeiro.

---

# 🏗️ Arquitetura

O projeto utiliza uma arquitetura:

> **Modular Monolith + Layered Architecture**

A aplicação será executada como um único sistema, mas organizada internamente em módulos independentes por responsabilidade e coesão de negócio.

### Princípio principal

> Módulos são definidos principalmente por responsabilidade e coesão de negócio, e não simplesmente por dependência entre entidades.

Uma relação entre dois objetos **não significa que eles precisam pertencer ao mesmo módulo**.

Por exemplo:

* `Account` ↔ `Transaction` → módulos diferentes.
* `Recurrence` ↔ `Transaction` → módulos diferentes.
* `Credit` ↔ `Obligation` → módulos diferentes.
* `Credit` + `Bill` → mesmo módulo.
* `Obligation` + `Payment` → mesmo módulo.

---

# 📦 Organização dos módulos

```text
financial-system/
├── profile/
├── account/
├── category/
├── transaction/
├── recurrence/
├── obligation/
│   └── Payment
├── credit/
│   ├── CreditOperation
│   └── Bill
└── projection/
```

Cada módulo possui responsabilidade própria.

---

# 📁 Organização interna dos módulos

A organização padrão de um módulo é:

```text
module/
├── presentation/
├── application/
├── domain/
└── infrastructure/
```

### `presentation`

Responsável pela entrada e saída externa da aplicação.

Exemplos:

* Controllers
* Requests
* Responses

Não deve conter regras de negócio.

---

### `application`

Responsável por coordenar os casos de uso.

É onde a aplicação:

* recebe uma solicitação;
* coordena operações;
* chama o domínio;
* coordena outros módulos quando necessário.

A camada de aplicação **não deve duplicar regras que pertencem ao domínio**.

---

### `domain`

Contém o núcleo das regras de negócio.

Exemplos:

* Entidades
* Value Objects
* Enums
* Regras de domínio
* Invariantes

O domínio não deve depender de detalhes de infraestrutura.

---

### `infrastructure`

Responsável pelos detalhes técnicos necessários para executar o sistema.

Exemplos:

* MongoDB
* Spring Data
* Implementações de persistência
* Configurações técnicas

---

# 👤 Profile

O módulo `Profile` representa o contexto financeiro ao qual os demais dados pertencem.

Outros módulos possuem referência ao Profile através de:

```text
profileId
```

O Profile não é responsável por gerenciar os outros domínios.

---

# 🏦 Account

Uma `Account` representa uma conta financeira.

Não existe uma entidade separada `CreditAccount`.

Uma Account pode possuir:

* dinheiro;
* crédito;
* ambos;
* somente dinheiro;
* somente crédito.

## Estrutura conceitual

```text
Account
├── id
├── profileId
├── name
├── initialBalance
└── creditLimit
```

### `initialBalance`

Representa o dinheiro existente na Account no momento de sua criação.

Não representa o saldo atual.

O saldo atual é consequência das movimentações financeiras.

`initialBalance` é um valor histórico e não deve ser simplesmente alterado posteriormente.

---

### `creditLimit`

Representa o **limite total de crédito da Account**.

Não representa o crédito disponível atual.

O crédito utilizado e o crédito disponível são derivados das `CreditOperations`.

```text
creditUsed =
    Σ CreditOperation.amount
    onde status = APPLIED
```

```text
creditAvailable =
    creditLimit - creditUsed
```

---

## Regras de `creditLimit`

### Valor nulo

Permitido.

```text
creditLimit = null
```

Significa que a Account não possui limite de crédito.

Nesse estado, não é possível criar `CreditOperation` para a Account.

### Zero

Permitido.

```text
creditLimit = 0
```

Significa que o limite total é zero.

Nenhuma CreditOperation pode ser criada enquanto o limite disponível for zero.

### Valor negativo

Nunca permitido.

```text
creditLimit < 0
```

é inválido.

---

## Alteração do limite

O `creditLimit` pode ser alterado depois da criação da Account.

Aumentar o limite é permitido.

Reduzir o limite é permitido somente quando:

```text
newCreditLimit >= creditUsed
```

Isso impede que o crédito disponível se torne negativo.

Uma Account também pode passar de:

```text
creditLimit = null
```

para um valor positivo.

A operação inversa:

```text
creditLimit = valor
        ↓
creditLimit = null
```

somente é permitida quando:

```text
creditUsed = 0
```

---

## Ciclo de vida

A Account não é fisicamente excluída.

Quando o cliente solicita a exclusão:

```text
ACTIVE → INACTIVE
```

Os dados históricos permanecem.

Uma Account `INACTIVE` não pode ser utilizada para novas operações.

Além disso, uma Account não pode ser desativada enquanto possuir Transactions ativas:

```text
EXPECTED
NOT_RECEIVED
```

Transactions `COMPLETED` e `CANCELLED` não impedem a desativação.

---

# 💰 Transaction

O módulo `Transaction` representa movimentações financeiras.

## Tipos

```text
INCOME
EXPENSE
TRANSFER
```

### INCOME

Entrada de dinheiro.

Requer:

```text
destinationAccountId
```

### EXPENSE

Saída de dinheiro.

Requer:

```text
sourceAccountId
```

### TRANSFER

Transferência entre duas Accounts.

Requer:

```text
sourceAccountId
destinationAccountId
```

As duas contas devem ser diferentes.

Uma transferência normal representa somente:

```text
MONEY → MONEY
```

Não representa uma despesa nem uma receita.

---

## Status

```text
EXPECTED
COMPLETED
NOT_RECEIVED
CANCELLED
```

### Transições

```text
EXPECTED
├── COMPLETED
├── NOT_RECEIVED
└── CANCELLED

NOT_RECEIVED
└── EXPECTED
```

`COMPLETED` e `CANCELLED` são estados terminais.

---

## Criação diretamente como COMPLETED

Uma Transaction pode ser criada diretamente como:

```text
COMPLETED
```

Nesse caso:

```text
expectedAmount = null
expectedDate = null
```

Os valores realizados são obrigatórios.

Isso também é válido para `TRANSFER`.

---

## Expectativa e realização

Quando uma Transaction é criada como `EXPECTED`, seus dados esperados são preservados.

```text
expectedAmount
expectedDate
```

Quando ela é concluída:

```text
realizedAmount
realizedDate
```

representam o que realmente aconteceu.

A diferença é derivada:

```text
difference =
    realizedAmount - expectedAmount
```

Não é persistida como fonte de verdade.

Se a Transaction foi criada diretamente como `COMPLETED`, sem expectativa, não existe diferença esperada.

---

## Valores realizados

`realizedAmount` e `realizedDate` são obrigatórios em:

```text
COMPLETED
```

E não existem em:

```text
EXPECTED
NOT_RECEIVED
CANCELLED
```

Uma Transaction `COMPLETED` pode ter seus valores realizados corrigidos posteriormente, mas não pode deixar de possuir esses valores.

Uma correção não cria uma nova Transaction.

---

## NOT_RECEIVED

Representa uma expectativa que não aconteceu.

Não produz entrada ou saída financeira.

Pode retornar para:

```text
NOT_RECEIVED → EXPECTED
```

A expectativa continua válida.

---

## CANCELLED

Representa o cancelamento definitivo da Transaction.

Uma Transaction cancelada não volta a ser concluída.

Se uma movimentação que já foi `COMPLETED` precisar ser desfeita, isso não significa alterar seu status para `CANCELLED`; trata-se de um conceito separado de reversão/estorno.

---

# 🔁 Recurrence

`Recurrence` representa uma regra capaz de gerar Transactions recorrentes.

Uma Recurrence **não é uma Transaction**.

Ela possui um modelo da Transaction que será gerada.

## Estrutura conceitual

```text
Recurrence
├── id
├── profileId
├── transactionTemplate
│   ├── type
│   ├── sourceAccountId
│   ├── destinationAccountId
│   ├── categoryId
│   ├── expectedAmount
│   └── description
├── frequency
├── rule
├── startDate
├── endDate
└── transactionIds[]
```

A Transaction gerada possui:

```text
recurrenceId
```

Uma Transaction criada manualmente possui `recurrenceId = null`.

---

## Regras de recorrência

A frequência determina:

> Com que frequência ocorre.

A regra determina:

> Como a data da ocorrência é determinada.

Regras previstas:

1. Dia específico do mês.
2. Dia específico da semana.
3. Último dia do mês.
4. Primeiro dia útil.
5. Último dia útil.
6. Penúltimo dia útil.
7. A cada N dias.

---

## Valor recorrente

O valor é inicialmente fixo.

A Transaction gerada recebe o:

```text
expectedAmount
```

definido na Recurrence.

Uma Transaction individual pode posteriormente possuir valor realizado diferente.

O valor da Recurrence continua sendo o modelo original.

---

## Sincronização

Quando a Recurrence é alterada, as Transactions vinculadas que ainda estejam:

```text
EXPECTED
```

são atualizadas.

Transactions já processadas não são alteradas.

Se a alteração da Recurrence produzir uma configuração inválida para uma Transaction existente, a alteração deve ser rejeitada.

---

# 🏷️ Category

Category representa uma classificação para Transactions.

## Estrutura

```text
Category
├── id
├── profileId
├── name
├── expectedMonthlyAmount
└── active
```

A categoria possui uma expectativa mensal, mas **não funciona como limite de gastos**.

Não bloqueia Transactions.

---

## Expected Monthly Amount

```text
null
```

Significa que não existe expectativa definida.

```text
0
```

Significa expectativa explicitamente igual a zero.

```text
> 0
```

Representa a expectativa mensal de gasto.

O valor efetivamente gasto é calculado a partir das Transactions.

---

## Desativação

Categories não são fisicamente excluídas.

```text
active = false
```

Uma categoria desativada:

* não pode ser escolhida em novas Transactions;
* não aparece como opção ao editar Transactions;
* continua aparecendo no histórico;
* não participa das métricas atuais baseadas em categorias ativas.

Transactions antigas continuam apontando para a categoria.

Se uma categoria desativada for criada novamente com o mesmo nome para o mesmo Profile, a categoria existente é reativada.

---

# 📋 Obligation

`Obligation` representa um compromisso financeiro.

Exemplos:

```text
CREDIT_CARD_BILL
RENT
UTILITY
EDUCATION
LOAN
OTHER
```

## Status

```text
PENDING
PARTIALLY_PAID
PAID
OVERDUE
CANCELLED
```

---

## Valor

```text
amount
```

representa o valor esperado da obrigação.

O valor não é substituído pelos pagamentos realizados.

---

## Pagamentos

Uma Obligation pode possuir vários Payments.

Isso permite:

```text
Obligation = R$ 1.000

Payment = R$ 300
Payment = R$ 400
Payment = R$ 300
```

---

## Valores derivados

```text
paidAmount =
    Σ Payment.amount
    onde status = COMPLETED
```

```text
remainingAmount =
    max(
        0,
        amount - paidAmount - waivedAmount
    )
```

```text
difference =
    paidAmount - amount
```

Esses valores são derivados.

---

## Waiver / abatimento

Uma obrigação pode ter parte do valor abatida sem que isso seja um Payment.

Exemplo:

```text
Obligation = R$ 500
Payment = R$ 450
waivedAmount = R$ 50
```

Resultado:

```text
remainingAmount = R$ 0
status = PAID
```

Se houver apenas um abatimento parcial e ainda existir saldo, o status continua sendo determinado pelas regras normais da obrigação.

---

## Status da Obligation

### PENDING

Antes do vencimento e ainda existe valor restante sem pagamento suficiente.

### PARTIALLY_PAID

Existe algum Payment concluído, ainda existe saldo restante e o vencimento ainda não passou.

### OVERDUE

A data de vencimento passou e ainda existe valor restante.

Pode existir pagamento parcial.

### PAID

O valor restante é zero através de pagamentos e/ou abatimento.

### CANCELLED

O compromisso foi cancelado.

O cancelamento extingue o saldo pendente, mas não apaga Payments históricos.

---

# 💳 Payment

Payment representa um pagamento que **realmente aconteceu**.

Não existe estado `PENDING`.

Se algo ainda não foi pago, a `Obligation` representa essa previsão.

## Estrutura

```text
Payment
├── id
├── obligationId
├── accountId
├── amount
├── paymentDate
└── status
```

## Status

```text
COMPLETED
CANCELLED
```

### COMPLETED

Representa um pagamento efetivamente realizado.

O pagamento pode utilizar:

```text
MONEY
```

ou:

```text
CREDIT
```

da Account.

### CANCELLED

Um Payment concluído pode ser cancelado.

```text
COMPLETED → CANCELLED
```

O cancelamento reverte seu efeito financeiro.

---

## Pagamento com MONEY

O valor é retirado da Account.

---

## Pagamento com CREDIT

O Payment solicita ao módulo de Credit a utilização do crédito.

O Payment não manipula diretamente:

```text
creditUsed
creditAvailable
Bill
```

Essas regras pertencem ao módulo de Credit.

---

## Cancelamento

Ao cancelar um Payment:

### MONEY

O valor retorna à Account.

### CREDIT

A CreditOperation associada é revertida.

O Payment continua preservado como histórico.

---

# 💳 Credit

O módulo `Credit` controla a utilização de crédito e suas faturas.

Não existe `CreditAccount`.

O limite pertence à:

```text
Account.creditLimit
```

O módulo Credit gerencia:

```text
CreditOperation
Bill
```

---

# 💳 CreditOperation

Representa uma utilização efetiva do crédito.

## Estrutura

```text
CreditOperation
├── id
├── paymentId
├── accountId
├── billId
├── amount
├── operationDate
└── status
```

## Status

```text
APPLIED
REVERSED
```

Transição:

```text
APPLIED → REVERSED
```

Não é permitido retornar para `APPLIED`.

---

## Crédito utilizado

Somente operações `APPLIED` consomem crédito.

```text
creditUsed =
    Σ CreditOperation.amount
    onde status = APPLIED
```

```text
creditAvailable =
    creditLimit - creditUsed
```

Uma CreditOperation só pode ser criada se:

```text
creditAvailable >= amount
```

---

## Reversão

Quando:

```text
APPLIED → REVERSED
```

a operação deixa de consumir crédito.

A reversão não cria crédito diretamente; ela remove aquela operação do conjunto de operações que representam o crédito utilizado.

---

# 🧾 Bill

Bill representa o ciclo mensal de crédito de uma Account.

Existe uma Bill para cada mês de uma Account que possui crédito, inclusive quando não houver gastos.

---

## Estrutura

```text
Bill
├── id
├── accountId
├── referenceMonth
├── closingDate
├── dueDate
└── status
```

## Status

```text
OPEN
CLOSED
```

---

## Ciclo

O `closingDate` pertence ao próximo ciclo.

Exemplo:

```text
closingDate = dia 10

09/09 → Bill anterior
10/09 → próximo Bill
11/09 → próximo Bill
```

O `CreditOperation` pertence ao Bill cujo ciclo contém sua `operationDate`.

---

## Bill OPEN

Pode receber novas CreditOperations.

## Bill CLOSED

Não pode receber novas CreditOperations.

---

## Fechamento

Quando uma Bill é fechada:

```text
Bill CLOSED
      ↓
Obligation
```

Uma Obligation é criada para representar a responsabilidade financeira daquela Bill.

Mesmo uma Bill com valor zero gera uma Obligation:

```text
amount = 0
status = PAID
```

---

# 💰 Bill e CreditOperations

O valor atual da Bill é derivado das CreditOperations:

```text
billAmount =
    Σ CreditOperation.amount
    onde status = APPLIED
```

Uma CreditOperation `REVERSED` deixa de participar do valor atual da Bill.

O valor original da operação permanece preservado.

Isso permite manter o histórico:

```text
Original
   ↓
Operation APPLIED
   ↓
Operation REVERSED
   ↓
Bill recalculada
```

---

# 💸 Refunds

Um refund não é uma entidade própria.

Quando uma CreditOperation é revertida depois que a Bill já foi paga, pode surgir um pagamento em excesso.

O excesso é devolvido através de uma:

```text
Transaction
type = INCOME
```

---

## Cálculo

```text
currentBillAmount =
    Σ CreditOperations APPLIED
```

```text
totalPaid =
    Σ Payments COMPLETED
```

```text
refundDue =
    totalPaid
    - currentBillAmount
    - totalRefunded
```

Somente quando:

```text
refundDue > 0
```

é criado um novo refund.

O refund retorna para a Account que realizou o Payment.

---

## Distribuição dos refunds

Quando existem múltiplos Payments:

1. Os Payments concluídos são ordenados pelo `paymentDate` mais recente.
2. Em caso de empate, utiliza-se o `id`.
3. O refund é distribuído primeiro para o Payment mais recente.
4. Caso o excesso ultrapasse o valor desse Payment, continua no próximo.

Os Payments originais permanecem intactos.

---

# 📊 Projection

Projection calcula a evolução financeira esperada.

Não representa uma entidade financeira persistida.

É uma visão calculada do estado atual e dos eventos futuros conhecidos.

---

## Estado atual

A Projection considera:

```text
currentBalance
creditUsed
creditAvailable
```

---

## Eventos futuros

São considerados compromissos que ainda podem afetar o futuro.

Por exemplo:

```text
Transaction EXPECTED
Obligation com remainingAmount > 0
```

Transactions já concluídas não são projetadas novamente.

Payments concluídos também não são eventos futuros.

---

## Expected Income

Uma entrada futura é representada por:

```text
Transaction
type = INCOME
status = EXPECTED
```

Não existe uma entidade específica para salário.

---

## Recebimento

Quando a entrada acontece:

```text
EXPECTED → COMPLETED
```

com:

```text
realizedAmount
realizedDate
```

O valor realizado pode ser diferente do esperado.

Exemplo:

```text
expectedAmount = R$ 3.600
realizedAmount = R$ 3.450
```

A diferença será:

```text
difference = -R$ 150
```

A Recurrence permanece com seu valor original de R$ 3.600.

---

## NOT_RECEIVED

Uma entrada `NOT_RECEIVED` não adiciona dinheiro ao saldo atual e não altera o valor da Recurrence.

---

# 📅 Daily Spending Limit

A Projection também fornece um limite diário de gasto.

Ele representa:

> Quanto do dinheiro atualmente disponível pode ser consumido por dia até a próxima entrada esperada, depois de reservar os compromissos conhecidos nesse intervalo.

Exemplo:

```text
Saldo atual:       R$ 1.000
Próxima entrada:   10 dias
Obrigações:        R$ 300

Valor disponível:
R$ 1.000 - R$ 300 = R$ 700

Limite diário:
R$ 700 / 10 = R$ 70
```

As obrigações são consideradas na projeção desde o início do período.

O vencimento da obrigação não cria um novo limite naquele dia.

---

# 🔄 Fluxos principais

## Transaction

```text
EXPECTED
   │
   ├── COMPLETED
   ├── NOT_RECEIVED
   └── CANCELLED

NOT_RECEIVED
   │
   └── EXPECTED
```

---

## Payment

```text
Obligation
     ↓
Payment
     ↓
COMPLETED
     │
     └── CANCELLED
```

---

## Payment com crédito

```text
Payment.COMPLETED
       ↓
Credit.useCredit()
       ↓
CreditOperation.APPLIED
       ↓
Bill
```

Cancelamento:

```text
Payment.CANCELLED
       ↓
CreditOperation.REVERSED
       ↓
crédito disponível aumenta
```

---

## Bill

```text
Bill OPEN
    ↓
closingDate
    ↓
Bill CLOSED
    ↓
Obligation criada
```

---

## Reversão após pagamento

```text
CreditOperation.REVERSED
          ↓
Bill recalculada
          ↓
Obligation recalculada
          ↓
Payments históricos preservados
          ↓
excesso identificado
          ↓
Transaction INCOME
          ↓
dinheiro devolvido
```

---

## Recurrence

```text
Recurrence
     ↓
gera Transaction EXPECTED
     ↓
Transaction possui recurrenceId
```

Alteração:

```text
Recurrence alterada
       ↓
Transactions EXPECTED vinculadas
       ↓
atualizadas
```

Transactions processadas não são alteradas.

---

# 🔗 Relações entre módulos

```text
Profile
   │
   ├──────────────→ Account
   │                   │
   │                   ├────────→ Transaction
   │                   │
   │                   └────────→ Credit
   │                                │
   │                                ├── CreditOperation
   │                                │
   │                                └── Bill
   │                                      │
   │                                      ↓
   │                                  Obligation
   │                                      │
   │                                      ↓
   │                                   Payment
   │
   ├──────────────→ Category
   │                    │
   │                    ↓
   │                Transaction
   │
   └──────────────→ Recurrence
                        │
                        ↓
                    Transaction

Todos os módulos relevantes
          │
          ↓
     Projection
```

---

# 🧠 Princípios de domínio

### 1. Histórico não deve ser destruído

Dados históricos não devem ser apagados apenas porque deixaram de ser relevantes no presente.

Isso se aplica principalmente a:

* Accounts;
* Categories;
* Transactions;
* Payments;
* CreditOperations;
* Bills.

---

### 2. Estado atual deve ser derivado quando possível

Não armazenar informações que podem ser obtidas de eventos ou operações históricas.

Exemplos:

```text
currentBalance
creditUsed
creditAvailable
paidAmount
remainingAmount
Bill.total
difference
```

são conceitos derivados.

---

### 3. Previsão e realidade são diferentes

O sistema mantém separado:

```text
EXPECTED
```

e:

```text
REALIZED
```

Isso permite comparar aquilo que era esperado com aquilo que realmente aconteceu.

---

### 4. Payment não é previsão

Se algo ainda não aconteceu:

```text
Obligation
```

representa a previsão.

Quando o pagamento acontece:

```text
Payment.COMPLETED
```

representa o fato ocorrido.

---

### 5. CreditOperation não é Payment

```text
Payment
```

representa o pagamento.

```text
CreditOperation
```

representa o consumo de crédito provocado por esse pagamento.

---

### 6. Bill não é Obligation

```text
Bill
```

representa o ciclo de crédito.

```text
Obligation
```

representa a responsabilidade financeira gerada pelo fechamento da Bill.

---

### 7. Projection não é fonte de verdade

Projection calcula informações a partir dos módulos de domínio.

O Dashboard deve consumir a Projection e não duplicar as regras financeiras.

---

# 🗂️ Estrutura esperada do projeto

```text
financial-system/
│
├── profile/
│   ├── presentation/
│   ├── application/
│   ├── domain/
│   └── infrastructure/
│
├── account/
│   ├── presentation/
│   ├── application/
│   ├── domain/
│   └── infrastructure/
│
├── category/
│   ├── presentation/
│   ├── application/
│   ├── domain/
│   └── infrastructure/
│
├── transaction/
│   ├── presentation/
│   ├── application/
│   ├── domain/
│   └── infrastructure/
│
├── recurrence/
│   ├── presentation/
│   ├── application/
│   ├── domain/
│   └── infrastructure/
│
├── obligation/
│   ├── presentation/
│   ├── application/
│   ├── domain/
│   └── infrastructure/
│
├── credit/
│   ├── presentation/
│   ├── application/
│   ├── domain/
│   └── infrastructure/
│
└── projection/
    ├── presentation/
    ├── application/
    ├── domain/
    └── infrastructure/
```

A estrutura interna pode ser adaptada quando um módulo não precisar de alguma camada. O objetivo é manter a separação de responsabilidades sem criar complexidade artificial.

---

# 🛠️ Tecnologias

Tecnologias previstas para o projeto:

* Java
* Spring Boot
* MongoDB
* Spring Data MongoDB
* Maven
* Git / GitHub

O frontend será desenvolvido posteriormente.

A implementação da persistência não deve determinar as regras do domínio.

---

# 🚧 Ordem de desenvolvimento

O desenvolvimento será realizado gradualmente:

```text
1. Domínio
   ↓
2. Organização dos módulos
   ↓
3. Modelos de domínio
   ↓
4. MongoDB
   ↓
5. Repositories
   ↓
6. Use Cases
   ↓
7. Services / Application
   ↓
8. Controllers
   ↓
9. Testes
   ↓
10. Frontend / Dashboard
```

A implementação deve respeitar as regras de domínio definidas neste documento.

---

# 📌 Estado atual do projeto

### Domínio

**Definido e auditado.**

Módulos principais:

* [x] Profile
* [x] Account
* [x] Category
* [x] Transaction
* [x] Recurrence
* [x] Obligation
* [x] Payment
* [x] CreditOperation
* [x] Bill
* [x] Projection

### Arquitetura

**Definida:**

```text
Modular Monolith
+
Layered Architecture
```

### Próxima etapa

Com o domínio definido, o próximo passo é transformar as regras conceituais em **modelos de domínio concretos**, mantendo as regras independentes da infraestrutura e do MongoDB.
