# Auditoria do português

Regra: **todo texto visível ao usuário está em português do Brasil**; o código pode ficar em inglês. Não há seletor de idioma e não existe outro idioma no projeto.

## Como o idioma é garantido

| Mecanismo | O que faz |
|---|---|
| `res/values/strings.xml` é português | o recurso padrão **é** pt-BR; os 60 outros idiomas do OpenCalc e o `locales_config.xml` foram removidos |
| `BaseActivity` (pai de `MainActivity`, `SettingsActivity`, `AboutActivity`) | dá a cada tela um contexto pt-BR; assim os textos das bibliotecas (botões de diálogo, acessibilidade), datas ("Hoje") e símbolos numéricos também saem em português, mesmo com o celular em inglês |
| `OpenCalcApp` | define `Locale.setDefault(pt-BR)` |
| Manifesto sem `localeConfig` | o Android não oferece "idioma do app" |
| Configurações | a linha *Idioma* foi removida |

## Verificações automáticas

| Teste | O que garante |
|---|---|
| `StringResourcesTest.noOtherLanguageFolderExists` | nenhuma pasta `values-xx` (idioma) e nenhum `locales_config.xml` |
| `StringResourcesTest.noLayoutOrMenuHasHardCodedVisibleText` | layouts, menus e preferências só usam `@string` (exceções técnicas: `DEG`, `RAD`, `45`, `00`, `000`) |
| `StringResourcesTest.noVisibleStringContainsEnglishWords` | nenhum texto de `strings.xml` contém palavras como *the, and, your, settings, cancel, history, payment, interest...* |
| `StringResourcesTest.brazilianTerminologyFollowsTheSpecification` | termos exigidos: Valor presente, Valor futuro, Pagamento, Número de períodos, Taxa por período, Valor presente líquido, Taxa interna de retorno, Fluxos de caixa, Limpar registradores, início/final do período, Tabela de amortização |
| `StringResourcesTest.everyStringUsedByTheFinancialPanelIsDefined` | o painel financeiro não usa texto inexistente |
| `EssentialFlowsTest.theInterfaceIsInBrazilianPortuguese` | a configuração real da tela é `pt` e os textos saem em português (no emulador, com o sistema em inglês) |

## Classificação dos textos encontrados

### 1. Visíveis ao usuário: todos em português

- **339 strings** em `strings.xml` (46 marcadas como não traduzíveis: dígitos, operadores, símbolos matemáticos, o nome do aplicativo). As 7 que o OpenCalc não tinha traduzido para pt-BR foram traduzidas; três textos em inglês fixos nas Configurações (`Numbering System`, resumos `APP THEME` e `APP LANGUAGE`) foram corrigidos.
- **Rótulos das teclas financeiras:** `2ª`, `INÍCIO`, `FIM`, `GRAVAR`, `CHAMAR`, `ÚLT X`, `LIMPAR`, `LIMP. REG` (antes em inglês: 2nd, BEG, END, STO, RCL, LSTx, CLR).
- **Textos derivados de erros:** o motor financeiro devolve uma mensagem de desenvolvedor em inglês (`detail`); a interface **nunca a mostra**. Para "mais de uma solução" ela lista os números encontrados (`errorCandidates`), formatados em pt-BR. Corrigido: antes, o texto em inglês vazava na mensagem.
- **Rótulo da tela de configurações** (aparece nos apps recentes): era `SettingsActivity`, agora "Configurações".
- **Seletor de numeração:** a opção (sistema indiano) foi removida da interface por não fazer sentido aqui; as descrições internas foram traduzidas.
- **Menus, diálogos, histórico, ajuda, estados vazios, confirmações e descrições de acessibilidade:** todos por `strings.xml`.
- Padronização: "Limpar Histórico" → "Limpar histórico"; "offline" → "sem internet".

### Notação técnica mantida no botão, explicada em português

| Botão | Explicação em português (toque e segure a tecla, ou veja a linha de ajuda) |
|---|---|
| `n` | Número de períodos |
| `i` | Taxa por período |
| `PV` | Valor presente |
| `PMT` | Pagamento |
| `FV` | Valor futuro |
| `NPV` | Valor presente líquido |
| `IRR` | Taxa interna de retorno |
| `CF0`, `CFj`, `Nj` | Fluxo inicial, fluxo seguinte, repetir o último fluxo |
| `AMORT` | Amortizar períodos |
| `CHS` | Trocar sinal |
| `ENTER` | Enter (separa dois números) |
| `RPN` | notação polonesa reversa (explicada na Ajuda) |
| `sin`, `cos`, `tan`, `log`, `ln`, `INV` | funções matemáticas (descrições de acessibilidade em português) |
| `RAD` / `DEG` | radianos / graus |
| `AC` | limpar tudo |

Cada tecla tem **descrição para leitor de tela em português** e **dica ao tocar e segurar**; ao pressionar uma tecla técnica a linha de ajuda mostra, por exemplo, "PV — Valor presente". No visor, os registradores mostram `n`, `i`, `PV`, `PMT`, `FV` com os valores.

### 2. Apenas código interno (não visível)

Nomes de classes, métodos e atributos (`FinancialCalculatorViewModel`...), chaves de preferências, tags de teste (`key:ENTER`), mensagens de exceção do domínio, comentários. **Não foram renomeados.**

### 3. Documentação técnica

Todo o `docs/` está em português; o texto das licenças (`LICENSE`, GPL-3.0) permanece no original.

### 4. Nomes que não devem ser traduzidos

*OpenCalc* (nome do projeto de origem), *GPL-3.0*, *Gson*, *AndroidX*, *Material Components* (nomes de bibliotecas), *Flamengo* e *Cleveland Cavaliers* (nomes dos temas), os símbolos matemáticos (`π`, `e`, `√`, `Δ%`).

## Limite conhecido

O sistema Android continua mostrando em inglês o que **ele** desenha fora do aplicativo (por exemplo, a tela "instalar apps desconhecidos" no idioma do celular).
