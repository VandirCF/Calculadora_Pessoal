# Casos de teste financeiros

Todos os valores esperados vêm de **cálculo independente**, não do código testado:
[`docs/reference/financial_reference_values.py`](reference/financial_reference_values.py) usa o módulo `decimal` do Python com 60 dígitos, **fórmulas fechadas escritas de forma diferente** da implementação (por exemplo, `PMT = -PV·r / (1 − (1+r)^−n)` com expoente negativo) e **bisseção simples** sobre intervalos escolhidos à mão para taxas e TIR. Algumas constantes (e, ln 2, ln 10, √2) são constantes matemáticas conhecidas. Onde há solução algébrica fechada (TIR de três fluxos), ela foi usada em vez de bisseção.

Os testes correspondentes ficam em `app/src/test/java/com/darkempire78/opencalculator/finance/` (`TvmTest`, `CashFlowTest`, `AmortizationRatesTest`, `RpnEngineTest`, `BigMathTest`).

**Convenção de sinais:** dinheiro recebido é positivo, dinheiro pago é negativo. **Taxas** `i` e as taxas de conversão são percentuais **por período**. **Tolerância** é erro absoluto máximo aceito pelo teste.

## TVM — equação resolvida

`PV·(1+r)^n + PMT·(1 + r·b)·((1+r)^n − 1)/r + FV = 0`, com `r = i/100`, `b = 1` para pagamento no início (BEGIN) e `b = 0` no final (END). Para `r = 0`: `PV + PMT·n + FV = 0`.

| Caso | Entradas | Sinais | Periodicidade | Fórmula independente | Esperado | Tolerância |
|---|---|---|---|---|---|---|
| Parcela de financiamento (final) | PV=100000, n=360, i=0,5, FV=0 | PV +, PMT − | mensal | `−PV·r/(1−(1+r)^−n)` | PMT = −599,55052515275239459146 | 1e‑12 |
| Parcela com pagamento no início | idem, BEGIN | idem | mensal | parcela do final ÷ (1+r) | PMT = −596,56768671915661153379 | 1e‑12 |
| Parcela com taxa zero | PV=−1200, n=12, i=0, FV=0 | PV −, PMT + | mensal | `−(PV+FV)/n` | PMT = 100 | 1e‑12 |
| Sinais não são corrigidos | PV=−100000, n=360, i=0,5 | PV − | mensal | oposto do caso 1 | PMT = +599,55052515275239459146 | 1e‑12 |
| Valor futuro de poupança | PMT=−200, n=120, i=0,75, PV=0 | PMT − | mensal | `−PMT·((1+r)^n−1)/r` | FV = 38702,85541666115129651715 | 1e‑12 |
| Valor futuro, n fracionário | PV=−1000, n=2,5, i=10, PMT=0 | PV − | por período | `−PV·1,1^2,5` | FV = 1269,05870628588337185966 | 1e‑10 |
| Valor presente de anuidade | PMT=−500, n=60, i=1, FV=0 | PMT − | mensal | `−PMT·(1−(1+r)^−n)/r` | PV = 22477,51920311200096972380 | 1e‑12 |
| n para dobrar o capital | PV=−1000, FV=2000, PMT=0, i=1 | PV −, FV + | por período | `ln 2 / ln 1,01` | n = 69,66071689357488922414 | 1e‑10 |
| n para quitar financiamento | PV=10000, PMT=−300, FV=0, i=1 | PV +, PMT − | mensal | `−ln(1 − PV·r/|PMT|)/ln(1+r)` | n = 40,74890715609405773008 | 1e‑10 |
| n com taxa zero | PV=−1200, PMT=100, FV=0, i=0 | opostos | por período | `−(PV+FV)/PMT` | n = 12 | 1e‑12 |
| Taxa de crescimento | n=2, PV=−1000, FV=1210, PMT=0 | PV −, FV + | por período | `(FV/−PV)^(1/n) − 1` | i = 10 % | 1e‑9 |
| Taxa de financiamento | PV=100000, n=360, PMT=−599,55 | PV +, PMT − | mensal | bisseção em Decimal de 60 dígitos | i = 0,49999931931192 % | 1e‑9 |
| Ida e volta | PMT calculado → FV recalculado | — | — | consistência da equação | FV = −3000 | 1e‑20 |

Erros esperados (sem valor numérico): sinais iguais para n ou i → `INCONSISTENT_SIGNS`; registrador ausente → `MISSING_REGISTER`; n negativo ou i ≤ −100 % → `OUT_OF_RANGE`; n=0 para PMT ou i → `NO_SOLUTION`; parcela menor que os juros → `NO_SOLUTION`; nenhuma taxa entre −99 % e 100000 % → `NO_SOLUTION`.

## Fluxo de caixa, VPL e TIR

Convenção temporal: CF0 em t=0 (sem desconto); o primeiro fluxo seguinte ao final do período 1, e assim por diante. `VPL(r) = Σ CFt / (1+r)^t`.

Projeto A = [−1000, 300, 400, 500]; Projeto B = [−5000, 1500 × 4].

| Caso | Entradas | Fórmula independente | Esperado | Tolerância |
|---|---|---|---|---|
| VPL, taxa positiva | A, i=10 % | soma descontada em Decimal | −21,03681442524417731029 | 1e‑12 |
| VPL, taxa zero | A, i=0 | soma simples | 200 | 1e‑12 |
| VPL com frequência | B, i=8 % | soma descontada | −31,80973993350155520556 | 1e‑12 |
| VPL, taxa negativa | A, i=−50 % | `−1000 + 300·2 + 400·4 + 500·8` | 5200 | 1e‑10 |
| TIR convencional (uma raiz) | A | bisseção em [0, 1] com 300 passos | 8,896339469335 % | 1e‑9 |
| TIR com frequência | B | bisseção | 7,713847295208 % | 1e‑9 |
| TIR zera o VPL | A | VPL(TIR) | 0 | 1e‑9 |
| TIR alta | [−100, 1100] | `1100/100 − 1` | 1000 % | 1e‑8 |
| TIR negativa | [−100, 50] | `50/100 − 1` | −50 % | 1e‑8 |
| Duas mudanças de sinal, uma raiz no intervalo | [−1000, 1050, −0,5] | fórmula quadrática: `x² − 2100x + 2000 = 0`, `r = 1/x − 1` | 4,9523593368305979007 % + aviso | 1e‑9 |
| Duas raízes (ambígua) | [−100, 230, −132] | bisseção em [5 %, 15 %] e [15 %, 30 %] | 10 % e 20 % → `MULTIPLE_SOLUTIONS` | 1e‑9 |
| Raiz negativa extra | [−1000, 1500, −100] | varredura da malha | raízes ≈ −93 % e 43,007352543677 % → `MULTIPLE_SOLUTIONS` | 1e‑9 |

Casos de erro: lista vazia → `EMPTY_CASH_FLOWS`; sem troca de sinal → `NO_SIGN_CHANGE`; frequência fora de 1–99 → `INVALID_INPUT`; mais de 80 fluxos → `OUT_OF_RANGE`; taxa ≤ −100 % → `OUT_OF_RANGE`; limite de iterações (5) → `NO_CONVERGENCE`.

## Amortização

Empréstimo 10000, 12 meses, 1 % ao mês, parcela 888,49 (parcela exata arredondada a centavos, `HALF_EVEN`). Juros de cada período arredondados a centavos (`HALF_EVEN`) como fazem as instituições financeiras. Referência: planilha em Decimal no script.

| Período | Juros | Principal | Saldo |
|---|---|---|---|
| 1 | 100,00 | 788,49 | 9211,51 |
| 2 | 92,12 | 796,37 | 8415,14 |
| 3 | 84,15 | 804,34 | 7610,80 |
| 12 | 8,80 | 879,69 | −0,02 (resíduo do arredondamento da parcela) |

Resumo dos períodos 1 a 6: juros 480,14; principal 4850,80; saldo 5149,20. Tolerância: **exato** (igualdade de `BigDecimal` com 2 casas).

## Conversão de taxas (composta)

| Caso | Fórmula independente | Esperado | Tolerância |
|---|---|---|---|
| Nominal 12 % a.a. capitalizada mensalmente → efetiva anual | `(1 + 0,12/12)^12 − 1` | 12,682503013196972066 % | 1e‑15 |
| Nominal 24 % a.a. capitalizada trimestralmente → efetiva anual | `(1 + 0,24/4)^4 − 1` | 26,247696 % | 1e‑15 |
| Efetiva 10 % a.a. → nominal capitalizada mensalmente | `12·((1,10)^(1/12) − 1)` | 9,568968514684489279 % | 1e‑15 |
| Efetiva anual 10 % → mensal equivalente | `1,10^(1/12) − 1` | 0,797414042890374107 % | 1e‑15 |
| Mensal 1 % → anual equivalente | `1,01^12 − 1` | 12,682503013196972066 % | 1e‑15 |
| Diária 0,05 % → anual equivalente (365) | `1,0005^365 − 1` | 20,015941067771088857 % | 1e‑14 |
| Anual 12 % → trimestral equivalente | `1,12^(1/4) − 1` | 2,873734472208028043 % | 1e‑15 |
| Anual 12 % → mensal equivalente | `1,12^(1/12) − 1` | 0,948879293458297 % (≠ 1 %, prova que a divisão simples não é equivalente) | 1e‑8 |
| Ida e volta anual → mensal → anual | composição | 7,5 % | 1e‑25 |

## Funções matemáticas de base (`BigMath`)

Constantes conhecidas a 30+ dígitos: e = 2,7182818284590452353602874713527; ln 2 = 0,69314718055994530941723212145818; ln 10 = 2,3025850929940456840179914546844; √2 = 1,4142135623730950488016887242097. Tolerância 1e‑30. Raiz quadrada de 10¹⁰⁰ e de 10⁻²⁰⁰ testadas nos extremos.

## Pilha RPN — sequências de teclas

| Sequência | Resultado | Fundamento |
|---|---|---|
| `2 ENTER 3 +` | X = 5 | definição |
| `10 ENTER 4 -` | X = 6 | Y − X |
| `100 ENTER 10 %` | X = 10, Y = 100 | `X ← Y·X/100`, Y mantido |
| `100 ENTER 10 % +` | 110 | aumento percentual |
| `1000 CHS PV 12 n 1 i 0 FV PMT` | PMT = 88,848788678341707339987831227886 | `1000·0,01/(1−1,01^−12)` em Decimal, tolerância 1e‑25 |
| Idem com `BEGIN` | 87,969097701328423108898842799887 | caso anterior ÷ 1,01 |
| `1000 CHS CF0 300 CFJ 400 CFJ 500 CFJ 10 i NPV` | −21,03681442524417731029 | tabela VPL |
| `10000 PV 888.49 CHS PMT 1 i 6 AMORT` | X=−480,14; Y=−4850,80; Z=5149,20 | tabela de amortização |

Os testes da mecânica da pilha (elevação, descida, ENTER, giros, LAST X, CLX, erros) verificam o conteúdo dos quatro registradores, ver `docs/RPN_GUIDE.md`.
