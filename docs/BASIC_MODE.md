# Modo Básico — comportamento documentado (Fase 2)

Cobertura automatizada: `BasicModeTest`, `InputHelpersTest` (e os testes já existentes `ExpressionUnitTest`, `NumberFormatterTest`). Os testes usam separador decimal `.` e de milhar `,` fixos, independentes do locale da máquina.

## Operações e precedência
| Regra | Exemplo | Resultado |
|---|---|---|
| `×` e `÷` antes de `+` e `−` | `2+3×4` | 14 |
| Encadeamento da esquerda para a direita | `10−4−5`, `20÷5÷2` | 1, 2 |
| Parênteses | `(2+3)×4` | 20 |
| Multiplicação implícita | `4(2+3)`, `(2+3)4` | 20 |
| Parêntese aberto é fechado automaticamente | `(2+3` | 5 |
| Negativos | `2×-3`, `-2×-3`, `2--3` | −6, 6, 5 |

## Números
- Aritmética em `BigDecimal`: `0.1+0.2 = 0.3` exato. Inteiros e somas muito grandes são exatos.
- Divisão não exata é arredondada na escala configurada (padrão 10, `HALF_DOWN`): `1÷3 = 0.3333333333`. Quocientes menores que `10⁻¹⁰` viram `0` (limite da escala configurada).
- Zeros à esquerda são ignorados (`007` = 7); ponto inicial vale zero antes (`.5` = 0,5).
- Dois separadores decimais no mesmo número (`1.2.3`) ou ponto isolado → **erro de sintaxe**.
- Exibição: agrupamento de milhares e separador decimal conforme o idioma e as configurações; notação científica opcional (configuração existente).

## Porcentagem
| Contexto | Exemplo | Resultado | Regra |
|---|---|---|---|
| Isolada | `50%` | 0,5 | divide por 100 |
| Com `×` | `200×10%` | 20 | fração simples |
| Com `÷` | `100÷10%` | 1000 | fração simples |
| Com `+`/`−` | `100+10%`, `100−10%` | 110, 90 | relativa à base (estilo calculadora de bolso) |
| Parêntese com `%` | `50+(5+5)%` | 55 | relativa à base |

## Erros
- Divisão por zero (`5÷0`, `0÷0`, `1+2÷(3−3)`): mensagem "Can't divide by 0" (traduzida), resultado e expressão em vermelho.
- Os demais erros (sintaxe, domínio, número real exigido, infinito) usam mensagens existentes.
- Os indicadores de erro ainda são variáveis globais (`Calculator.kt`); a substituição por resultado tipado está planejada (risco R2) para a Fase 3.

## Continuidade após `=`
O resultado vira a nova entrada: operador após `=` continua o cálculo; dígito após `=` inicia um novo cálculo (comportamento original preservado).

## Funcionalidades adicionadas na Fase 2
- **Inverter sinal:** *toque longo* no botão `−` inverte o sinal do número digitado (`5` → `-5`; `2+5` → `2-5`; `2×5` → `2×-5`; `(5` → `(-5`). O leitor de tela anuncia a ação ("Inverter o sinal do número atual"). Escolhido por não haver espaço livre na grade 4×5 e por não alterar o layout.
- **Colar número:** colar no display aceita apenas número decimal simples (com sinal opcional, agrupamento e o separador do idioma). Texto inválido, expressões e notação científica são rejeitados com aviso; nada é inserido.

## Já existente e preservado
Adicionar, subtrair, multiplicar, dividir, `%`, ponto decimal, apagar (backspace), AC (limpar tudo; toque longo no apagar também limpa), histórico, copiar resultado (toque longo no resultado), temas, retrato e paisagem.

## Não implementado (e por quê)
- **Repetir a última operação ao pressionar `=` novamente:** incompatível com o mecanismo atual (o resultado substitui a entrada); mudaria o comportamento original. Fica como possível melhoria futura.
- **"Limpar entrada" (CE) separado de "limpar cálculo":** o app possui AC e apagar; um CE exigiria espaço no teclado. Adiado.
