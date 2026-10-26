# Modo Científico — comportamento documentado (Fase 3)

Testes: `ScientificModeTest` (67), `CalculatorMemoryTest`, mais `ExpressionUnitTest`.

## Funções disponíveis
| Função | Entrada | Observações |
|---|---|---|
| Parênteses, `^`, `√`, `!`, `%`, `mod` (`#`) | teclado | já existiam |
| sin, cos, tan e inversas | teclado (INV para inversas) | graus ou radianos (botão DEG/RAD) |
| ln, log₁₀, log₂, `e^`, `10^`, `2^` | teclado | |
| π, e | teclado | |
| **Raiz de índice n** | INV + `π` → `ⁿ√` | `3ⁿ√27 = 3`; índice ímpar aceita radicando negativo; índice par com radicando negativo → "número real exigido"; índice 0 → erro de domínio |
| **Inverso 1/x** | INV + `e` → `1/x` | insere `^(-1)`; `0^(-1)` → divisão por zero |
| **Módulo (valor absoluto)** | INV + `!` → `\|x\|` | insere `abs(` |
| **Memória M+, M−, MR, MC** | menu ⋮ | um registrador, persistido, soma exata em `BigDecimal` |
| **Resultado anterior (Ans)** | menu ⋮ | insere o resultado do último item do histórico |
| Histórico de expressões | painel deslizante | registra a origem (modo) |

## Política de precisão e tolerâncias
- Operações exatas (`+ − × ÷`, potência inteira, fatorial de inteiros, √ de quadrados perfeitos, raiz inteira exata) usam `BigDecimal`; os testes comparam com tolerância 0 (`compareTo`).
- sin, cos, tan, ln, log, exp, potência fracionária e fatorial não inteiro passam por `double` (~15 dígitos): testes com tolerância absoluta **1e‑9** (1e‑12 para √ e π).
- Ruído de ponto flutuante: resultados trigonométricos com |valor| < **1e‑14** viram 0 (`sin(180°) = 0`, `cos(90°) = 0`, também para negativos).
- Tangente indefinida: erro de domínio quando |cos| < **1e‑12** (90°, 270°, −90°, π/2 rad). Antes só era detectado um caso e em unidade errada (corrigido).

## Erros (prioridade quando há mais de um)
sintaxe > domínio > número real exigido > divisão por zero > infinito. Propagam-se por soma, multiplicação, funções aninhadas.

## API testável
`Calculator.evaluateSafe(expr, graus)` devolve `CalcResult.Value` ou `CalcResult.Error(CalcError)`, zera as flags globais antes e depois e serializa o acesso (sem vazamento entre cálculos). A UI ainda usa `evaluate` e as flags (migração completa fica fora do escopo para não arriscar regressões).

## Mudanças de comportamento
- `tan(90°)` agora é erro de domínio (antes devolvia 1.6e16).
- `sin(-180°)` e similares agora dão 0 exato (antes -1.2e-16).
- INV passou a trocar também `π`, `e` e `!` (ver tabela).
