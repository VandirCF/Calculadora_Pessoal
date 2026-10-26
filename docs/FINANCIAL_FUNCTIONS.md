# Funções financeiras — especificação

Código em `app/src/main/java/com/darkempire78/opencalculator/finance/` (Kotlin puro, sem dependência de Android). Casos de teste e fontes de validação: [FINANCIAL_TEST_CASES.md](FINANCIAL_TEST_CASES.md). Uso pelo teclado: [RPN_GUIDE.md](RPN_GUIDE.md).

## Política numérica

| Item | Decisão |
|---|---|
| Tipo | `BigDecimal` em todo o motor. Nenhum `Double` participa dos cálculos. Única exceção: a **semente** de `ln` (e de `√`) usa `Math.log`/`Math.sqrt` e é refinada por iteração de Newton até a precisão total |
| Precisão dos resultados | 34 dígitos significativos (`MathContext(34, HALF_EVEN)`, equivalente a decimal128) |
| Precisão interna | 50 dígitos (`WORK_MC`) em exp, ln, potências não inteiras, raiz e anuidades; arredondado a 34 no resultado |
| Arredondamento | `HALF_EVEN` (arredondamento bancário) |
| Exibição | só apresentação: padrão 2 casas para dinheiro (configurável de 0 a 8), n e i com até 6 casas sem zeros à direita; notação científica para |valor| ≥ 10¹⁵ ou valores não nulos que arredondariam para 0. A exibição **nunca** realimenta o cálculo |
| Entrada | até 20 dígitos por número digitado |
| Funções transcendentais | `exp` por série de Taylor com redução de argumento; `ln` por iteração de Newton/Halley; `x^y` não inteiro = `exp(y·ln x)` (exige x > 0) |
| API mínima | `BigDecimal.sqrt` exige Android 13; a raiz é calculada por Newton (compatível com API 21) |

## Entradas inválidas

Cada função devolve `FinancialResult.Success` ou `Failure(error, detail)`. Erros possíveis: `INVALID_INPUT`, `DIVISION_BY_ZERO`, `OUT_OF_RANGE`, `OVERFLOW`, `INCONSISTENT_SIGNS`, `NO_SOLUTION`, `NO_CONVERGENCE`, `MULTIPLE_SOLUTIONS`, `EMPTY_CASH_FLOWS`, `NO_SIGN_CHANGE`, `MISSING_REGISTER`. Nunca há exceção não tratada; a pilha RPN preserva seu estado quando ocorre erro.

## TVM

Sinais: dinheiro recebido positivo, pago negativo. `r = i/100`; `b = 1` com pagamento no início.

```
PV·(1+r)^n + PMT·(1 + r·b)·((1+r)^n − 1)/r + FV = 0        (r ≠ 0)
PV + PMT·n + FV = 0                                          (r = 0)
```

| Incógnita | Fórmula |
|---|---|
| PMT | `−(PV·(1+r)^n + FV) / ((1 + r·b)·((1+r)^n − 1)/r)` |
| PV | `−(FV + PMT·(1 + r·b)·((1+r)^n − 1)/r) / (1+r)^n` |
| FV | `−(PV·(1+r)^n + PMT·(1 + r·b)·((1+r)^n − 1)/r)` |
| n | `k = PMT·(1 + r·b)/r`; `n = ln((k − FV)/(PV + k)) / ln(1+r)` (taxa zero: `−(PV+FV)/PMT`) |
| i | raiz de `f(r)` pela busca descrita abaixo |

Regras: `n ≥ 0`; `r > −1` (taxa > −100 %); para `n` e `i` é necessário que PV, PMT e FV (os não nulos) tenham sinais opostos, senão `INCONSISTENT_SIGNS`; `n` pode ser fracionário.

## Busca de raízes (taxa e TIR)

`RootFinder` varre uma malha de taxas de −99 % a 100000 % (≈ 430 pontos: passo de 1 % de −99 a 0 %, 0,5 % de 0 a 100 %, 10 % de 100 a 1000 % e crescimento geométrico de ×1,25 depois). Cada troca de sinal de `f` delimita uma raiz, refinada por **bisseção**.

| Parâmetro | Valor |
|---|---|
| Máximo de iterações por raiz | 100 (a bisseção parte de 0,01 e precisa de ≈ 43) |
| Critério de convergência | largura do intervalo ≤ 1e‑15 na taxa (1e‑13 ponto percentual) |
| Sem convergência | `NO_CONVERGENCE`, sem laço infinito |
| Nenhuma raiz na faixa | `NO_SOLUTION` |
| Mais de uma raiz | `MULTIPLE_SOLUTIONS`, com todos os candidatos (não se escolhe um) |

## Fluxo de caixa

`CashFlows(CF0, lista de (valor, repetições))`. Até **80** entradas, cada uma repetida de **1 a 99** vezes. Lista vazia (sem CF0 nem fluxos) → `EMPTY_CASH_FLOWS`.

- **VPL**: `Σ CFt/(1+r)^t`, com CF0 em t = 0 (sem desconto) e o primeiro fluxo seguinte em t = 1.
- **TIR**: taxa por período que zera o VPL, em %. Resultado arredondado a 12 casas decimais (`HALF_EVEN`). Sem troca de sinal → `NO_SIGN_CHANGE`. Troca de sinal mais de uma vez com uma só raiz na faixa → resultado + aviso `MULTIPLE_SIGN_CHANGES`.

## Amortização

Entradas: valor do empréstimo e parcela (módulos), taxa por período, intervalo de períodos, modo início/fim.

```
juros_k     = arredonda(saldo_{k−1} · r, 2 casas, HALF_EVEN)
principal_k = parcela − juros_k
saldo_k     = saldo_{k−1} − principal_k
```

Com pagamento no início, a primeira parcela não tem juros. O saldo é levado desde o período 1 mesmo que o intervalo comece depois. O resíduo final (centavos) decorre do arredondamento da parcela, como em um contrato real.

## Conversão de taxas

Sempre **capitalização composta**. Toda conversão devolve: taxa de origem, periodicidade de origem, tipo (nominal/efetiva), taxa resultante, periodicidade de destino, tipo e o método.

| Conversão | Fórmula | Método |
|---|---|---|
| Nominal anual `j` (capitalizada `m` vezes) → efetiva anual | `(1 + j/m)^m − 1` | `NOMINAL_TO_EFFECTIVE` |
| Efetiva anual `e` → nominal anual (capitalizada `m` vezes) | `m·((1+e)^(1/m) − 1)` | `EFFECTIVE_TO_NOMINAL` |
| Efetiva entre periodicidades | `(1 + r_dest) = (1 + r_orig)^(P_orig/P_dest)` | `COMPOUND_EQUIVALENT` |

Periodicidades (períodos por ano): diária 365, semanal 52, quinzenal 26, mensal 12, bimestral 6, trimestral 4, semestral 2, anual 1. A diária usa ano de 365 dias (não há convenção de dias úteis).

**Divisão simples não é conversão de taxa:** 12 % ao ano ÷ 12 = 1 % ao mês **não** equivale a 12 % efetivos ao ano (isso daria 12,6825 %). A equivalente mensal de 12 % efetivos é 0,9489 %.

## Datas

Esta versão **não inclui** funções de data (juros por dias corridos, ΔDYS, DATE).
