# Sistema de Controle Financeiro

Sistema pessoal de controle financeiro desenvolvido com foco em **modelagem de domínio**, projeção financeira e acompanhamento do dinheiro disponível, compromissos futuros, cartões de crédito e movimentações recorrentes.

O projeto será desenvolvido inicialmente como um sistema para múltiplos usuários/perfis, permitindo que cada perfil possua diversas contas e instrumentos financeiros.

> **Status do projeto:** Domínio conceitual definido. A stack tecnológica e a implementação ainda serão definidas.

---

# 1. Conceito do Sistema

O sistema tem como objetivo permitir que o usuário acompanhe:

* Quanto dinheiro possui atualmente;
* Onde esse dinheiro está;
* Quanto possui disponível em crédito;
* Quais despesas já aconteceram;
* Quais receitas já aconteceram;
* Quais movimentações são esperadas;
* Quais obrigações financeiras estão previstas;
* Quais obrigações já foram pagas;
* Quanto ainda precisa ser pago;
* O que provavelmente acontecerá com o dinheiro no futuro;
* Quanto pode ser gasto por dia considerando os compromissos conhecidos;
* Como o dinheiro está distribuído entre suas contas.

O sistema diferencia explicitamente:

> **O que foi planejado, o que aconteceu e o que ainda precisa acontecer.**

Essa separação é fundamental para a Projection.

---

# 2. Princípios do Domínio

## 2.1 Histórico é preservado

O sistema não deve apagar uma previsão quando ela deixa de ser uma previsão.

Por exemplo:

```text
Salário esperado:
R$ 3.600 em 15/10

Recebido:
R$ 3.450 em 17/10
```

O sistema preserva:

```text
Esperado:  R$ 3.600
Realizado: R$ 3.450
```

Isso permite comparar expectativa e realidade.

---

## 2.2 Valor realizado não altera automaticamente uma recorrência

Uma ocorrência concreta pertence a uma determinada recorrência, mas sua realização não altera a regra da recorrência.

Exemplo:

```text
Recurrence: Salário
Valor recorrente: R$ 3.600

Outubro:
Esperado:  R$ 3.600
Realizado: R$ 3.450

Novembro:
Esperado: R$ 3.600
```

O recebimento de R$ 3.450 em outubro não altera automaticamente o salário previsto para novembro.

---

## 2.3 Saldos são derivados do histórico

O saldo atual não deve ser tratado como a fonte de verdade.

Ele é resultado das movimentações financeiras realizadas.

Para uma conta de dinheiro:

```text
Saldo =
    saldo inicial
    + receitas realizadas
    - despesas realizadas
    + transferências recebidas
    - transferências enviadas
```

---

## 2.4 Transferências não são receitas nem despesas

Transferir R$ 100 do Nubank para o Itaú não aumenta nem diminui o patrimônio total.

Exemplo:

```text
Antes:

Nubank = R$ 500
Itaú   = R$ 300

Total  = R$ 800
```

Após:

```text
Nubank = R$ 400
Itaú   = R$ 400

Total  = R$ 800
```

A transferência apenas altera a distribuição do dinheiro.

---

# 3. Principais Conceitos do Domínio

O sistema possui os seguintes conceitos principais:

```text
Profile
   │
   ├── FinancialAccount
   │
   ├── Transaction
   │      └── Recurrence
   │
   ├── Obligation
   │      └── Payment
   │
   ├── CreditAccount
   │      └── Bill
   │
   └── Projection
```

`Projection` é um conceito de cálculo e não uma entidade persistida como fonte de verdade.

---

# 4. Profile

Representa o usuário/perfil que possui os dados financeiros.

O sistema deverá suportar múltiplos perfis.

Cada perfil possui seus próprios:

* FinancialAccounts;
* Transactions;
* Recurrences;
* Obligations;
* Payments;
* configurações financeiras.

A autenticação e autorização não fazem parte da primeira etapa do domínio.

---

# 5. FinancialAccount

## Conceito

`FinancialAccount` representa uma fonte ou instrumento financeiro acompanhado pelo usuário.

O conceito não está limitado a contas bancárias tradicionais.

Exemplos:

```text
Nubank
Itaú
Dinheiro em espécie
Nubank Crédito
PicPay Crédito
Cartão do João
```

O sistema não precisa saber necessariamente quem é o proprietário de um cartão. "Cartão do João" pode simplesmente ser o nome utilizado pelo usuário para identificar aquele instrumento.

---

## Tipos

```text
MONEY
CREDIT
```

### MONEY

Representa dinheiro líquido disponível.

Exemplos:

```text
Nubank
Itaú
Dinheiro
```

Possui:

```text
initialBalance
```

---

### CREDIT

Representa um instrumento de crédito.

Exemplos:

```text
Nubank Crédito
Cartão do João
PicPay
```

Possui:

```text
creditLimit
```

---

## Regra

Os campos específicos não devem ser misturados.

```text
MONEY
→ initialBalance

CREDIT
→ creditLimit
```

O saldo atual de uma conta MONEY é derivado das movimentações.

A utilização de crédito é derivada das movimentações/faturas e obrigações relacionadas.

---

# 6. Transaction

## Conceito

`Transaction` representa uma movimentação financeira concreta ou prevista.

Ela é utilizada para:

* receitas;
* despesas;
* transferências;
* movimentações previstas;
* movimentações já realizadas.

Uma Transaction pode nascer como uma previsão e posteriormente se tornar realizada.

---

# 7. Transaction — Tipos

```text
INCOME
EXPENSE
TRANSFER
```

## INCOME

Representa dinheiro entrando em uma conta.

Exemplo:

```text
Salário
Freelance
Reembolso
```

---

## EXPENSE

Representa uma despesa.

Exemplo:

```text
Mercado
Internet
Restaurante
Compra
```

---

## TRANSFER

Representa movimentação de dinheiro entre duas contas.

Exemplo:

```text
Nubank → Itaú
R$ 100
```

Uma transferência não é contabilizada como:

```text
+R$ 100 INCOME
-R$ 100 EXPENSE
```

Ela apenas altera a distribuição do dinheiro.

---

# 8. Transaction — Estados

```text
EXPECTED
COMPLETED
NOT_RECEIVED
CANCELLED
```

---

## EXPECTED

A movimentação está prevista, mas ainda não aconteceu.

Exemplo:

```text
Salário
R$ 3.600
15/10
EXPECTED
```

Uma Transaction EXPECTED:

* participa da Projection como evento futuro;
* não altera o saldo real;
* pode posteriormente ser confirmada;
* pode ser marcada como não recebida;
* pode ser cancelada.

---

## COMPLETED

A movimentação realmente aconteceu.

Exemplo:

```text
Esperado:  R$ 3.600
Realizado: R$ 3.450
Status:    COMPLETED
```

O valor realizado passa a afetar o estado financeiro real.

---

## NOT_RECEIVED

Utilizado para uma entrada prevista que não aconteceu.

Exemplo:

```text
Esperado: R$ 3.600
Data:     15/10
Status:   NOT_RECEIVED
```

Nesse estado:

* nenhum dinheiro é adicionado ao saldo;
* a ocorrência deixa de ser considerada uma entrada futura;
* a previsão original permanece registrada;
* a Recurrence não é alterada.

`NOT_RECEIVED` é aplicável a entradas.

---

## CANCELLED

Indica que a movimentação prevista não acontecerá.

Exemplo:

```text
Despesa prevista:
Internet
R$ 100

Status:
CANCELLED
```

A Transaction cancelada:

* não afeta o saldo;
* não deve continuar como evento futuro;
* permanece registrada para histórico.

---

# 9. Ciclo da Transaction

Para uma movimentação prevista:

```text
                    EXPECTED
                   /    |    \
                  /     |     \
                 ↓      ↓      ↓
          COMPLETED  NOT_RECEIVED  CANCELLED
```

A mesma Transaction é atualizada durante seu ciclo.

Não é criada uma segunda Transaction quando uma previsão é realizada.

---

# 10. Transaction — Dados Esperados

Toda Transaction possui uma parte que representa a expectativa:

```text
expectedAmount
expectedDate
```

Esses dados representam:

> O que o sistema esperava que acontecesse.

---

# 11. Transaction — Dados Realizados

Quando a movimentação efetivamente acontece:

```text
realizedAmount
realizedDate
```

Esses campos representam:

> O que realmente aconteceu.

São opcionais enquanto a Transaction ainda não foi realizada.

---

# 12. Transaction — Modelo Consolidado

```text
Transaction
├── id
├── description
├── type
├── status
│
├── expectedAmount
├── expectedDate
│
├── realizedAmount
├── realizedDate
│
├── sourceAccount
├── destinationAccount
│
├── category
└── recurrence
```

---

## Obrigatoriedade dos campos

### Obrigatórios

```text
id
description
type
status
expectedAmount
expectedDate
```

### Opcionais

```text
realizedAmount
realizedDate
category
recurrence
```

### Contas

As contas possuem obrigatoriedade condicionada ao tipo:

```text
INCOME
→ destinationAccount obrigatório

EXPENSE
→ sourceAccount obrigatório

TRANSFER
→ sourceAccount obrigatório
→ destinationAccount obrigatório
```

---

# 13. Regras dos Dados Realizados

Quando:

```text
status = EXPECTED
```

não existem dados realizados.

```text
realizedAmount = inexistente
realizedDate   = inexistente
```

Quando:

```text
status = COMPLETED
```

os dados realizados devem existir:

```text
realizedAmount
realizedDate
```

Para:

```text
NOT_RECEIVED
CANCELLED
```

não existe realização.

---

# 14. Diferença entre Esperado e Realizado

A diferença não precisa ser armazenada.

Ela é derivada:

```text
difference =
    realizedAmount - expectedAmount
```

Exemplo:

```text
Esperado:  R$ 3.600
Realizado: R$ 3.450

Diferença:
R$ 3.450 - R$ 3.600 = -R$ 150
```

Outro exemplo:

```text
Esperado:  R$ 3.600
Realizado: R$ 3.800

Diferença:
+R$ 200
```

A diferença pertence somente àquela ocorrência.

Ela não altera a Recurrence automaticamente.

---

# 15. Recurrence

## Conceito

`Recurrence` representa uma regra utilizada para gerar ou prever ocorrências futuras.

Uma Recurrence não é uma Transaction.

Ela funciona como uma regra/template:

```text
Recurrence
     ↓
ocorrência
     ↓
Transaction
```

Exemplo:

```text
Recurrence:
Salário
R$ 3.600
Penúltimo dia útil

        ↓

Transaction:
Setembro
R$ 3.600
EXPECTED

        ↓

Transaction:
Outubro
R$ 3.600
EXPECTED
```

---

# 16. Dados da Recurrence

```text
Recurrence
├── id
├── description
├── amount
├── type
├── financialAccount
├── category
├── frequency
├── rule
├── startDate
├── endDate
└── active
```

---

# 17. Frequency × Rule

Esses conceitos são separados.

### Frequency

Define:

> Com que frequência acontece?

### Rule

Define:

> Como determinar a data da ocorrência?

---

# 18. Regras de Recurrence

A primeira versão contempla:

```text
SPECIFIC_DAY_OF_MONTH
SPECIFIC_DAY_OF_WEEK
LAST_DAY_OF_MONTH
FIRST_BUSINESS_DAY
LAST_BUSINESS_DAY
PENULTIMATE_BUSINESS_DAY
EVERY_N_DAYS
```

Exemplos:

```text
Todo dia 10
Toda segunda-feira
Último dia do mês
Primeiro dia útil
Último dia útil
Penúltimo dia útil
A cada 15 dias
```

---

# 19. Recurrence e alterações pontuais

Uma alteração em uma ocorrência específica não deve alterar automaticamente a regra original.

Exemplo:

```text
Recurrence:
Salário = R$ 3.600
```

Outubro:

```text
Esperado: R$ 3.600
Realizado: R$ 3.450
```

Novembro:

```text
Esperado: R$ 3.600
```

A ocorrência de outubro não modifica a Recurrence.

Exceções específicas de recorrência, como um salário excepcionalmente diferente em determinado mês, poderão ser tratadas futuramente.

---

# 20. Obligation

## Conceito

`Obligation` representa um compromisso financeiro que precisa ser pago.

Exemplos:

```text
Fatura do cartão
Aluguel
Conta de energia
Faculdade
Empréstimo
Dívida
Outros compromissos
```

---

# 21. Tipos de Obligation

```text
CREDIT_CARD_BILL
RENT
UTILITY
EDUCATION
LOAN
OTHER
```

---

# 22. Dados da Obligation

```text
Obligation
├── id
├── description
├── amount
├── dueDate
├── type
├── status
└── payments
```

---

# 23. Valor da Obligation

`Obligation.amount` representa o **valor esperado/projetado** da obrigação.

Ele não deve ser sobrescrito pelo valor efetivamente pago.

Exemplo:

```text
Obligation:
Valor esperado = R$ 500
```

Pagamento:

```text
Payment:
Valor realizado = R$ 550
```

Continuamos preservando:

```text
Expected = R$ 500
Paid     = R$ 550
Difference = +R$ 50
```

---

# 24. Estados da Obligation

```text
PENDING
PARTIALLY_PAID
PAID
OVERDUE
CANCELLED
```

Fluxo normal:

```text
             PENDING
                │
          pagamento parcial
                ↓
        PARTIALLY_PAID
           │          │
           │          └── vencimento com saldo
           │                    ↓
           │                 OVERDUE
           │
           └── restante = 0
                       ↓
                     PAID
```

---

# 25. Payment

## Conceito

`Payment` representa especificamente a **liquidação de uma Obligation**.

Não representa qualquer saída de dinheiro.

Exemplo:

```text
Comprar refrigerante:
→ Transaction EXPENSE

Pagar fatura do cartão:
→ Payment
```

---

# 26. Dados do Payment

```text
Payment
├── id
├── obligation
├── moneyAccount
├── amount
├── paymentDate
└── status
```

A `moneyAccount` obrigatoriamente representa uma conta do tipo:

```text
MONEY
```

---

# 27. Estados do Payment

```text
PENDING
COMPLETED
CANCELLED
```

Fluxo:

```text
PENDING
   ├──→ COMPLETED
   └──→ CANCELLED
```

---

# 28. Payment parcial

Uma Obligation pode possuir vários Payments.

Exemplo:

```text
Obligation:
R$ 800
```

Pagamentos:

```text
08/10 → R$ 300
09/10 → R$ 200
10/10 → R$ 300
```

Total pago:

```text
R$ 800
```

A obrigação passa a:

```text
PAID
```

---

# 29. Valor restante da Obligation

O valor restante é derivado:

```text
remainingAmount =
    max(
        0,
        obligation.amount
        - sum(completed payments)
    )
```

Não é necessário tratá-lo como fonte de verdade.

---

# 30. Diferença entre esperado e pago

Também é derivada:

```text
difference =
    paidAmount - expectedAmount
```

Exemplo:

```text
Esperado: R$ 500
Pago:     R$ 550

Diferença: +R$ 50
```

---

# 31. Pagamento acima do esperado

Se:

```text
Expected = R$ 500
Paid = R$ 550
```

A obrigação fica quitada.

A diferença de R$ 50 é preservada como diferença daquela obrigação.

---

# 32. Pagamento abaixo do esperado

Se:

```text
Expected = R$ 500
Paid = R$ 450
```

Existem duas possibilidades de negócio:

### Pagamento insuficiente

```text
Remaining = R$ 50
Status = PARTIALLY_PAID
```

### Pagamento aceito como liquidação

Se o credor aceitar R$ 450 como quitação, os R$ 50 restantes são tratados como valor dispensado/waived.

Os pagamentos anteriores não são revertidos.

---

# 33. Waiver / Dispensa da obrigação

Existe uma diferença importante entre:

### Cancelar Payment

```text
Obligation = R$ 300

Payment = R$ 300
Status = CANCELLED
```

A obrigação continua existindo.

Se passar do vencimento:

```text
OVERDUE
```

Outro pagamento poderá ser realizado posteriormente.

---

### Dispensar o restante da Obligation

Exemplo:

```text
Obligation = R$ 300
Paid = R$ 200
Waived = R$ 100
```

Resultado:

```text
Expected = R$ 300
Paid = R$ 200
Waived = R$ 100
Remaining = R$ 0
```

Os R$ 200 pagos continuam no histórico.

A dispensa remove apenas o restante que deixou de ser exigido.

A forma técnica de representar essa dispensa ainda será definida na implementação.

---

# 34. Overdue

Se a data de vencimento passar e existir valor restante:

```text
remainingAmount > 0
```

a obrigação passa a:

```text
OVERDUE
```

Ela permanece na Projection.

Importante:

> Uma obrigação vencida não gera uma nova despesa todos os dias.

Ela continua sendo **uma única obrigação em aberto**.

Exemplo:

```text
Obligation:
R$ 500
Vencimento: 10/10

11/10:
Remaining = R$ 500
Status = OVERDUE
```

Se pagar R$ 200:

```text
Paid = R$ 200
Remaining = R$ 300
Status = OVERDUE
```

Se pagar os R$ 300 restantes:

```text
Remaining = R$ 0
Status = PAID
```

---

# 35. CreditAccount

Um cartão/instrumento de crédito é tratado como uma `FinancialAccount` do tipo `CREDIT`.

O conceito pode ser detalhado como:

```text
CreditAccount
├── id
├── name
├── creditLimit
├── closingDay
└── dueRule
```

Exemplos:

```text
Nubank
Cartão do João
PicPay
```

---

# 36. Fechamento da fatura

O cartão possui um dia fixo de fechamento.

O próprio dia de fechamento já pertence ao **próximo ciclo**.

Exemplo:

```text
closingDay = 10
```

Então:

```text
09/09 → fatura atual
10/09 → próxima fatura
11/09 → próxima fatura
```

---

# 37. Ciclo da Bill

Exemplo:

```text
closingDay = 10
```

Uma fatura pode ser:

```text
11/08 → 09/09
```

A próxima:

```text
10/09 → 10/10
```

Uma compra realizada em:

```text
10/09
```

pertence à próxima fatura.

---

# 38. Due Date da Bill

A data de vencimento é:

> **7 dias corridos após o fechamento.**

Fins de semana também são contabilizados.

Exemplo:

```text
Fechamento: 10/09
Vencimento: 17/09
```

---

# 39. Bill

## Conceito

`Bill` representa uma fatura de um `CreditAccount`.

Ela agrupa as Transactions de crédito pertencentes àquele ciclo.

Conceitualmente:

```text
Bill
├── obligation
├── creditAccount
├── startDate
├── endDate
└── closingDate
```

A Bill é conceitualmente uma especialização de `Obligation`.

A decisão entre herança ou composição será feita durante a implementação.

---

# 40. Bill — Estados

```text
OPEN
CLOSED
PAID
OVERDUE
CANCELLED
```

O total da Bill é derivado das Transactions.

`totalAmount` não deve ser tratado como fonte de verdade.

---

# 41. Compra no crédito

Quando uma Transaction de despesa é realizada em uma conta CREDIT:

```text
Transaction EXPENSE
        ↓
CreditAccount
        ↓
Bill
        ↓
Obligation
```

A compra:

* não reduz imediatamente o dinheiro disponível;
* aumenta o compromisso financeiro futuro;
* reduz o crédito disponível;
* pode reduzir a capacidade diária de gasto.

Exemplo:

```text
Dinheiro atual = R$ 1.000

Compra no crédito = R$ 300
```

Resultado:

```text
Dinheiro = R$ 1.000
Compromisso futuro += R$ 300
Crédito disponível -= R$ 300
```

---

# 42. Pagamento da fatura

Quando a fatura é paga:

```text
Payment
   ↓
Obligation / Bill
   ↓
MONEY Account
```

O pagamento:

* reduz o dinheiro da conta MONEY;
* reduz o valor restante da obrigação;
* quando totalmente quitado, encerra a obrigação;
* deve liberar o crédito utilizado conforme o estado financeiro do cartão.

---

# 43. Projection

## Conceito

`Projection` representa o cálculo da situação financeira futura.

Ela **não é uma entidade persistida como fonte de verdade**.

A Projection é produzida a partir dos dados atuais e dos eventos futuros conhecidos.

---

# 44. Princípio da Projection

> Projection começa pelo estado financeiro atual e simula os eventos financeiros futuros conhecidos em ordem cronológica para determinar como o dinheiro e os compromissos irão evoluir.

Ela utiliza:

* saldos atuais;
* Transactions realizadas;
* Payments realizados;
* Transactions futuras;
* ocorrências de Recurrence;
* Obligations futuras;
* Payments futuros;
* compromissos de crédito.

---

# 45. Histórico não deve ser reaplicado

Transactions e Payments concluídos são utilizados para determinar o estado atual.

Depois que o estado atual é estabelecido:

> Os eventos históricos não são novamente aplicados como eventos futuros.

Isso evita dupla contagem.

Exemplo:

```text
Salário de setembro já recebido
```

Ele contribuiu para o saldo atual.

A Projection não deve adicionar novamente o salário de setembro.

---

# 46. ProjectionEvent

Durante o cálculo, diferentes tipos de eventos futuros podem ser normalizados em um conceito temporário:

```text
ProjectionEvent
├── date
├── amount
├── type
└── source
```

Exemplo:

```text
10/09   +R$ 3.600   INCOME
12/09   -R$   100   TRANSACTION
17/09   -R$   400   OBLIGATION
20/09   -R$   200   PAYMENT
```

Os eventos são ordenados cronologicamente.

---

# 47. Estrutura conceitual da Projection

```text
Projection
├── CurrentState
├── FutureEvents
└── ProjectionResult
```

---

## CurrentState

```text
CurrentState
├── moneyBalance
├── creditUsed
└── creditAvailable
```

Representa a situação real no momento do cálculo.

---

## FutureEvents

Conjunto dos eventos futuros conhecidos.

Podem vir de:

* Transactions EXPECTED;
* Recurrences;
* Obligations;
* Payments;
* Bills;
* compromissos de crédito.

---

## ProjectionResult

Resultado calculado:

```text
ProjectionResult
├── currentBalance
├── projectedBalance
├── committedAmount
├── expectedIncome
├── expectedExpenses
├── dailySpendingLimit
└── timeline
```

O Dashboard consome esse resultado.

O Dashboard não deve duplicar as regras financeiras da Projection.

---

# 48. Exemplo de Projection

Estado atual:

```text
Saldo atual: R$ 440
```

Eventos:

```text
03/10 → obrigação R$ 100
05/10 → obrigação R$ 150
10/10 → salário R$ 3.600
```

Projection:

```text
Hoje:
R$ 440

03/10:
R$ 340

05/10:
R$ 190

10/10:
R$ 3.790
```

A Projection deve permitir que o saldo fique negativo.

Não deve esconder ou corrigir artificialmente uma insuficiência futura.

---

# 49. Expected Income

Uma entrada prevista não representa dinheiro líquido ainda disponível.

Exemplo:

```text
Hoje: 10/10

Salário esperado:
15/10
R$ 3.600
```

Até o dia 15:

```text
Saldo atual
≠
Saldo atual + salário
```

O salário pode aparecer na Projection, mas não faz parte do dinheiro líquido atual.

---

# 50. Confirmação de uma entrada

Quando a entrada realmente acontece, a mesma Transaction é atualizada:

```text
EXPECTED
    ↓
COMPLETED
```

Exemplo:

```text
Esperado:
R$ 3.600 em 15/10

Recebido:
R$ 3.450 em 15/10
```

Resultado:

```text
expectedAmount = R$ 3.600
expectedDate   = 15/10

realizedAmount = R$ 3.450
realizedDate   = 15/10

status = COMPLETED
```

O valor realizado:

```text
R$ 3.450
```

passa a fazer parte do saldo real.

---

# 51. Entrada recebida em data diferente

Também é permitido que a entrada seja recebida em outro dia.

Exemplo:

```text
Esperado:
15/10
R$ 3.600

Recebido:
17/10
R$ 3.450
```

A Transaction preserva:

```text
expectedDate  = 15/10
realizedDate  = 17/10
```

A Projection só considera o valor como dinheiro real a partir da realização.

---

# 52. Entrada não recebida

Quando o usuário informa que a entrada não aconteceu:

```text
EXPECTED
    ↓
NOT_RECEIVED
```

A previsão original permanece registrada.

Porém:

* nenhum dinheiro entra;
* a entrada não continua sendo projetada como futura;
* a Recurrence não é alterada.

Exemplo:

```text
Recurrence:
Salário R$ 3.600

15/10:
NOT_RECEIVED

15/11:
EXPECTED
R$ 3.600
```

---

# 53. Daily Spending Limit

## Conceito

`dailySpendingLimit` representa:

> Quanto do dinheiro atualmente disponível pode ser consumido por dia até a próxima entrada esperada, depois de reservar todas as obrigações conhecidas que ocorrerão nesse intervalo.

---

# 54. Exemplo do Daily Spending Limit

Situação:

```text
Saldo atual: R$ 1.000
Próxima entrada: em 10 dias
Obrigações no intervalo: R$ 300
```

Primeiro reservamos as obrigações:

```text
R$ 1.000 - R$ 300 = R$ 700
```

Depois:

```text
R$ 700 / 10 dias = R$ 70/dia
```

Resultado:

```text
dailySpendingLimit = R$ 70
```

---

# 55. Não recalcular artificialmente no dia da obrigação

O limite já considera a obrigação desde o início.

Não deve acontecer:

```text
Antes da obrigação:
R$ 70/dia

No dia da obrigação:
"agora recalcula"

Depois:
outro limite artificial
```

A obrigação já estava provisionada.

Isso evita criar a ilusão de que o usuário pode gastar mais dinheiro antes do vencimento.

---

# 56. Recalculo do Daily Spending Limit

O limite deve ser recalculado quando ocorrer uma mudança financeira relevante.

Exemplos:

* dinheiro entra;
* dinheiro é gasto;
* Payment é realizado;
* nova Obligation é criada;
* Obligation é alterada;
* Obligation é cancelada;
* nova compra no crédito aumenta compromisso futuro;
* outro evento altera significativamente o estado financeiro ou os compromissos conhecidos.

---

# 57. Exemplo de recalculo

Inicial:

```text
Saldo: R$ 1.000
Obrigação: R$ 300
Próxima entrada: em 10 dias

Limite:
(R$1.000 - R$300) / 10
= R$70/dia
```

Usuário gasta R$100:

```text
Saldo:
R$900
```

Mantendo a obrigação provisionada:

```text
R$900 - R$300 = R$600
```

Se restarem 9 dias:

```text
R$600 / 9
≈ R$66,67/dia
```

---

# 58. Expected Income e Daily Spending Limit

Uma entrada esperada não deve ser adicionada ao saldo atual antes de sua realização.

Entretanto, a Projection conhece essa entrada futura.

Exemplo:

```text
Hoje:
R$ 1.000

15/10:
+R$ 3.600
```

Hoje:

```text
Saldo real = R$ 1.000
```

A Projection:

```text
Hoje → R$ 1.000
15/10 → R$ 4.600
```

O dinheiro esperado pode ser utilizado para planejamento futuro, mas não é tratado como dinheiro líquido atual.

---

# 59. Fluxo Financeiro Geral

O fluxo conceitual do sistema é:

```text
Recurrence
    ↓
Transaction EXPECTED
    ↓
Projection
    ↓
Confirmação
    ↓
Transaction COMPLETED
    ↓
Estado financeiro real
    ↓
Nova Projection
```

Para obrigações:

```text
Transaction / Bill / Regra
          ↓
      Obligation
          ↓
       Projection
          ↓
       Payment
          ↓
     MONEY Account
          ↓
   Novo estado financeiro
          ↓
      Nova Projection
```

---

# 60. Fluxo de Crédito

```text
CreditAccount
      ↓
   closingDay
      ↓
     Bill
      ↓
 Transactions
      ↓
 Total da Bill
      ↓
 Obligation
      ↓
 Projection
      ↓
 Payment
      ↓
 MONEY Account
```

Uma compra no crédito:

```text
Não reduz dinheiro imediatamente
        ↓
Aumenta compromisso futuro
        ↓
Reduz crédito disponível
```

O pagamento da fatura:

```text
Reduz dinheiro
        ↓
Reduz obrigação
        ↓
Libera crédito
```

---

# 61. Relações conceituais

```text
Profile
│
├── 1:N FinancialAccount
│
├── 1:N Transaction
│
├── 1:N Recurrence
│
├── 1:N Obligation
│
└── 1:N Payment
```

```text
Recurrence
    │
    └── gera → Transaction
```

```text
Transaction
    │
    ├── sourceAccount
    ├── destinationAccount
    ├── category
    └── recurrence
```

```text
Obligation
    │
    └── 1:N Payment
```

```text
Bill
    │
    ├── CreditAccount
    ├── Transactions
    └── Obligation
```

---

# 62. Fonte de verdade

Uma regra fundamental do sistema é evitar armazenar dados derivados como fonte principal.

### Exemplos de dados derivados

Não são fontes primárias:

```text
currentBalance
spentInCategory
remainingAmount
billTotal
difference
creditAvailable
projectedBalance
dailySpendingLimit
```

Esses valores devem ser calculados a partir dos dados de origem.

---

# 63. Dados que devem ser preservados

O sistema deve preservar informações importantes para histórico:

### Transaction

```text
expectedAmount
expectedDate
realizedAmount
realizedDate
status
```

### Obligation

```text
expectedAmount
Payments realizados
possíveis valores dispensados
status
```

Isso permite comparar:

```text
planejado
     ×
realizado
```

---

# 64. Regras de negócio consolidadas

## Transaction

1. Uma Transaction pode representar uma movimentação prevista ou realizada.
2. A mesma Transaction evolui durante seu ciclo.
3. `EXPECTED` representa uma previsão.
4. `COMPLETED` representa uma movimentação realizada.
5. `NOT_RECEIVED` representa uma entrada prevista que não aconteceu.
6. `CANCELLED` representa uma movimentação que não acontecerá.
7. Uma Transaction realizada possui dados esperados e realizados.
8. A diferença entre esperado e realizado é derivada.
9. Uma diferença pontual não altera uma Recurrence.
10. Transactions de transferência não são receitas nem despesas.

---

## Recurrence

1. Recurrence representa uma regra, não uma ocorrência.
2. Recurrence gera/prepara Transactions concretas.
3. Uma alteração em uma ocorrência não altera automaticamente a regra.
4. Salário é uma Recurrence de `INCOME`, não uma entidade especial.
5. A primeira versão suporta regras de calendário e intervalos definidos.

---

## Obligation

1. Obligation representa um compromisso financeiro.
2. O valor da Obligation é o valor esperado.
3. Payments representam a liquidação.
4. Uma Obligation pode ter vários Payments.
5. Payments podem ser parciais.
6. Payment CANCELLED não reduz a obrigação.
7. Uma obrigação vencida não gera novos débitos diariamente.
8. O valor restante é derivado.
9. O valor efetivamente pago é derivado dos Payments.
10. A diferença entre esperado e pago é preservada como informação derivada.
11. O restante pode ser dispensado sem apagar Payments anteriores.

---

## Projection

1. Projection não é fonte de verdade persistida.
2. Projection parte do estado atual.
3. Histórico já realizado não é reaplicado como evento futuro.
4. Eventos futuros são processados cronologicamente.
5. Entradas esperadas não são dinheiro líquido atual.
6. Saldo projetado pode ficar negativo.
7. Projection concentra os cálculos utilizados pelo Dashboard.

---

## Daily Spending Limit

1. O limite representa capacidade de consumo diário até a próxima entrada esperada.
2. Obrigações futuras dentro desse intervalo são reservadas desde o início.
3. O vencimento da obrigação não cria artificialmente um novo limite.
4. O limite muda quando o estado financeiro ou os compromissos conhecidos mudam.
5. Entradas esperadas não são adicionadas ao dinheiro atual antes da realização.

---

# 65. O que ainda não foi definido

Os seguintes pontos permanecem deliberadamente para etapas futuras:

* tecnologia e stack;
* estrutura dos módulos;
* arquitetura técnica detalhada;
* modelagem específica do MongoDB;
* documentos e coleções;
* índices;
* estratégia de persistência;
* autenticação;
* autorização;
* API;
* frontend;
* implementação de calendário de dias úteis;
* tratamento técnico de exceções de Recurrence;
* representação técnica de waiver/dispensa;
* detalhes de liberação de crédito após pagamentos;
* metas/objetivos financeiros;
* relatórios;
* notificações;
* funcionalidades avançadas de Dashboard.

Esses pontos não fazem parte das decisões de domínio já fechadas.

---

# 66. Funcionalidades futuras

Algumas funcionalidades foram deliberadamente deixadas para uma etapa posterior.

## Goals / Allocation

O sistema poderá futuramente possuir:

```text
Goal
Allocation
```

para representar objetivos e alocação de dinheiro.

Essa parte não faz parte do núcleo financeiro definido até o momento.

---

# 67. Próxima etapa do projeto

O domínio conceitual inicial está consolidado.

A sequência planejada agora é:

```text
[✓] Conceito do sistema
[✓] Modelagem conceitual
[✓] FinancialAccount
[✓] Transaction
[✓] Recurrence
[✓] Obligation
[✓] Payment
[✓] CreditAccount
[✓] Bill
[✓] Projection
[✓] Daily Spending Limit
[✓] Expected × Realized
[✓] Estados e regras de negócio

[ ] Arquitetura técnica
[ ] Stack tecnológica
[ ] Modelagem MongoDB
[ ] Estrutura do projeto
[ ] Implementação do domínio
[ ] Persistência
[ ] API
[ ] Frontend
```

A próxima etapa será decidir **como transformar esse domínio em uma arquitetura e stack tecnológica**, sem alterar as regras de negócio já definidas.
