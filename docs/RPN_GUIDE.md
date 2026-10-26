# Guia da Calculadora Financeira RPN

Texto original, escrito para este aplicativo.

## O que é RPN

Em **RPN** (notação polonesa reversa) você digita primeiro os números e depois a operação. Não existe a tecla `=`: cada operação usa os valores já guardados na **pilha**.

```
2 ENTER 3 +      →  5
```

`ENTER` separa dois números: ele copia o valor de X para Y. Depois, ao digitar `3`, o `2` fica em Y e o `3` em X. A tecla `+` calcula `Y + X` e deixa o resultado em X.

## A pilha: X, Y, Z e T

A pilha tem quatro níveis. **X** é o valor mostrado; Y, Z e T ficam acima dele.

| Ação | O que acontece |
|---|---|
| Digitar um número | Se a elevação da pilha estiver ativa, o conteúdo sobe (T←Z, Z←Y, Y←X) e o número entra em X. Caso contrário, o número substitui X. |
| `ENTER` | Copia X para Y (a pilha sobe) e **desativa** a elevação: o próximo número substitui X. |
| Operação com dois números (`+ − × ÷ yˣ`) | Calcula `Y op X`, guarda o operando em LAST X, põe o resultado em X, **desce** a pilha (Y←Z, Z←T, T permanece) e ativa a elevação. |
| Operação com um número (`√x`, `1/x`, `%`, `Δ%`) | Calcula sobre X, guarda LAST X, ativa a elevação. |
| `x⇄y` | Troca X e Y. |
| `R↓` / `R↑` (2ª) | Gira a pilha para baixo ou para cima. |
| `LSTx` (2ª, tecla 6) | Traz de volta o último valor de X usado por uma operação. |
| `CLx` | Zera X e **desativa** a elevação. |
| `CLR` (2ª, tecla `R↓`) | Zera X, Y, Z e T (não apaga registradores). |
| `⌫` | Apaga o último dígito digitado; sem digitação em andamento, zera X. |

Depois de um **erro**, a pilha e os registradores ficam como estavam, a mensagem aparece abaixo dos registradores, e a próxima tecla limpa o erro e funciona normalmente.

### Porcentagem

`%` calcula **X% de Y**: `X ← Y · X / 100`, e **mantém Y**.

```
100 ENTER 10 %      →  10   (Y continua 100)
100 ENTER 10 % +    →  110
100 ENTER 10 % −    →  90
```

`Δ%` (2ª, tecla 3) calcula a variação percentual de Y para X: `80 ENTER 100 Δ%` dá 25.

## Registradores financeiros

`n`, `i`, `PV`, `PMT` e `FV` guardam os dados de uma operação financeira:

| Tecla | Significado |
|---|---|
| `n` | número de períodos |
| `i` | taxa de juros **por período**, em porcentagem |
| `PV` | valor presente |
| `PMT` | pagamento por período |
| `FV` | valor futuro |

**Guardar × calcular.** Digite um valor e pressione a tecla para **guardá-lo**. Pressione uma delas **logo após outra tecla financeira, sem digitar nada**, e o aplicativo **calcula** aquele registrador com base nos outros quatro. Os registradores preenchidos aparecem em destaque acima do teclado; tocar em um deles traz o valor para X.

### Convenção de sinais

Entradas e saídas de caixa usam **sinais opostos**: dinheiro que você recebe é positivo e dinheiro que você paga é negativo. A tecla **`CHS`** (change sign) troca o sinal do número que você está digitando ou do valor de X.

O aplicativo **nunca troca um sinal por conta própria**. Se os sinais forem incompatíveis ao calcular `n` ou `i` (por exemplo, PV, PMT e FV todos positivos), aparece a mensagem: *"Entradas e saídas de caixa precisam ter sinais opostos. Use CHS para tornar as saídas negativas."*

### Exemplo: parcela de um financiamento

Financiamento de 1000 em 12 parcelas mensais a 1 % ao mês:

```
1000 CHS PV
12 n
1 i
0 FV
PMT          →  88,85
```

(`1000 CHS PV` guarda −1000 em PV. `PMT`, pressionada logo depois de `FV`, calcula o pagamento.)

### Pagamento no início ou no final

`BEG` (2ª, tecla 4) calcula com pagamentos no **início** de cada período; `END` (2ª, tecla 5), no **final**. A etiqueta **INÍCIO** aparece no visor quando o modo início está ativo. O mesmo ajuste existe em Configurações.

### Pagamentos por ano

`12×` (2ª sobre `n`) multiplica X pelos pagamentos por ano e guarda o resultado em `n`; `12÷` (2ª sobre `i`) divide X e guarda em `i`. O número de pagamentos por ano (padrão 12) muda em ⋯ → *Pagamentos por ano* ou em Configurações.

```
30 12×   →  n = 360
12 12÷   →  i = 1   (taxa anual de 12 % dividida em 12 pagamentos)
```

## Fluxo de caixa, VPL e TIR

| Tecla (2ª) | Função |
|---|---|
| `CF0` (sobre `PV`) | fluxo inicial (tempo 0) |
| `CFj` (sobre `PMT`) | adiciona o próximo fluxo (até 80) |
| `Nj` (sobre `FV`) | repete o último fluxo `Nj` vezes (1 a 99) |
| `NPV` (sobre 7) | valor presente líquido à taxa `i` |
| `IRR` (sobre 8) | taxa interna de retorno, em % por período |

Convenção temporal: CF0 não é descontado; o primeiro `CFj` ocorre no final do período 1.

```
1000 CHS CF0
300 CFj  400 CFj  500 CFj
10 i
NPV          →  −21,04
IRR          →  8,90   (%)
```

**TIR e ambiguidade.** Sem troca de sinal nos fluxos a TIR não existe (mensagem de erro). Com uma única raiz ela é mostrada; se os fluxos trocarem de sinal mais de uma vez, aparece um **aviso**. Quando existem **várias** TIRs, nenhuma é escolhida: o aplicativo informa que há mais de uma solução e lista os valores encontrados.

## Amortização

Com PV, PMT e `i` preenchidos, digite o número de períodos e pressione `AMORT` (2ª, tecla 9). O resultado é:

- **X**: juros dos períodos amortizados (com o sinal do pagamento);
- **Y**: principal amortizado;
- **Z**: saldo devedor.

Chamadas seguintes continuam a partir do próximo período. A **tabela completa** está em ⋯ → *Tabela de amortização*. Os juros de cada período são arredondados a centavos (como fazem as instituições financeiras).

## Memória

`STO` (2ª, tecla 1) seguida de um dígito 0–9 guarda X; `RCL` (2ª, tecla 2) e um dígito trazem o valor de volta. O quadro de registradores (⋯ → *Registradores e fluxo de caixa*) lista tudo.

## Segunda função (2ª)

A tecla `2nd` ativa a função escrita **acima** de cada tecla. Quando ela está ativa, aparece a etiqueta **2ª** no visor, a tecla fica destacada, os rótulos trocam de lugar (o da função ativa fica grande) e a mensagem de ajuda explica. Ela vale para **uma** tecla e depois se desativa.

## Limpar

| Ação | Efeito |
|---|---|
| `CLx` | zera X |
| `CLR` (2ª) | zera a pilha |
| `CLRƒ` (2ª sobre `CLx`) ou ⋯ → *Limpar registradores* | zera n, i, PV, PMT, FV, a lista de fluxos e o progresso da amortização (**pede confirmação**; mantém pilha e memória) |
| ⋯ → *Limpar tudo* | zera também a pilha e a memória (**pede confirmação**) |

Os registradores **são mantidos ao fechar o aplicativo**; só deixam de existir quando você os limpa.

## Taxas

⋯ → *Conversão de taxas* converte entre taxa nominal e efetiva e entre periodicidades com capitalização composta. O resultado mostra taxa e periodicidade de origem, taxa e periodicidade de destino, o método e se a taxa é nominal ou efetiva. **Dividir 12 % ao ano por 12 não dá a taxa efetiva mensal** (isso seria 1 %; a equivalente composta é 0,9489 %).
