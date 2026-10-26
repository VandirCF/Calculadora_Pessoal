# Formatação numérica brasileira

Por padrão o aplicativo mostra números no formato do Brasil: **vírgula** como separador decimal e **ponto** como separador de milhares.

| Valor | Na tela |
|---|---|
| 1234567,89 | `1.234.567,89` |
| −100000 (PV) | `-100.000,00` |
| 10,5 % | `10,5%` |

## Duas representações, nunca misturadas

```
 entrada do usuário        motores de cálculo           apresentação
 "1.234,5" (teclado)   →   BigDecimal / "1234.5"   →   "1.234,50"
 colar "−100.000,00"       (sem localização)           (formatadores)
```

| Camada | Representação | Onde |
|---|---|---|
| **Interna** | números como `BigDecimal`; expressões com `.` como único separador decimal e sem separador de milhares; JSON salvo com `.` | `Calculator`, `RpnEngine`, `Tvm`, `FinancialStateStore` |
| **Exibida** | texto localizado: vírgula decimal, ponto de milhares | `NumberFormatter` (básica e científica), `RpnFormatter` (financeira), `FinancialPanel` |

A conversão acontece **somente nas bordas**:

- **Entrada.** `Expression.getCleanExpression(texto, decimal, milhares)` troca o separador decimal por `.` e remove os separadores de milhares antes de qualquer cálculo. No modo financeiro as teclas montam um número em texto neutro (`1234.5`) que vira `BigDecimal`.
- **Apresentação.** `NumberFormatter.format` e `RpnFormatter.format` aplicam o agrupamento e a vírgula só na hora de desenhar.
- **Colar.** `PastedNumber.parse` lê o texto na convenção em uso. Ele **recusa** números escritos em outra convenção em vez de interpretá-los errado: `1,234.5` (formato americano) não é aceito, porque lido em português seria `1,2345`. Aceita `-100.000,00` e `10,5`.
- **Cópia.** O resultado copiado sai no formato exibido.

O histórico guarda **texto já formatado**; mudar o formato nas Configurações não altera itens salvos.

## De onde vêm os separadores

`SeparatorSettings.resolve` decide com base nas Configurações (*Separador decimal* e *Separador de milhares*). No modo **Automático** (padrão) usa as convenções do Brasil, **independentemente do idioma do celular**: o aplicativo fixa o contexto em pt-BR (`BaseActivity`) e `MainActivity` usa `DecimalFormatSymbols` de pt-BR. Opções: ponto, vírgula, espaço, nenhum; o separador de milhares nunca é igual ao decimal (se coincidissem, o de milhares troca de símbolo).

## Testes

`FormatoBrasileiroTest` (e `SeparatorSettingsTest`, `InputHelpersTest`, `RpnFormatterTest`):

| Caso | Resultado |
|---|---|
| `1,5 + 2,5` | `4` |
| `1.000 + 500` | `1500` |
| `1.234.567,89` digitado | `1234567.89` internamente |
| `200 × 10,5%` | `21` |
| `10,5%` | `0.105` |
| PV = `-100.000,00` colado | `-100000,00` (texto de entrada) |
| `-100000` exibido (2 casas) | `-100.000,00` |
| `1234567,89` exibido | `1.234.567,89` |
| `1,234.5` colado | recusado |
| resultado do motor | `"3.75"`, nunca `"3,75"` |
| JSON do estado financeiro | contém `88.8487...`, nunca `88,848...`, mesmo com locale pt-BR |
| `-100.000+1.000` | `-99000` |

Teste de interface (`EssentialFlowsTest`): `1`, `0`, `0`, `0` aparece como `1.000` no visor básico; `PV = -1.000,00` no painel financeiro.
