# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/). Versões ainda não publicadas.

## [0.1.0] — não publicada

Primeira versão derivada do OpenCalc 3.2.1 (commit `0584d61`), sob GPL-3.0.

### Adicionado
- **Três modos** no mesmo aplicativo (Básica, Científica, Financeira RPN) com seletor segmentado; o último modo usado é restaurado.
- **Modo básico:** inverter sinal (toque longo em `−`), colar apenas números válidos, 70 testes automatizados e o comportamento documentado em `docs/BASIC_MODE.md`.
- **Modo científico:** raiz de índice n, inverso 1/x e módulo (via INV), memória M+/M−/MR/MC, resultado anterior (Ans), `Calculator.evaluateSafe`, 67 testes (ver `docs/SCIENTIFIC_MODE.md`).
- **Calculadora financeira RPN** com pilha X/Y/Z/T, LAST X, memória 0–9, TVM (n, i, PV, PMT, FV; início/fim), fluxo de caixa, VPL, TIR (com detecção de ambiguidade), amortização (resumo e tabela) e conversão de taxas nominal/efetiva/equivalente. Motor em `BigDecimal` (34 dígitos), independente da interface, validado contra cálculos independentes (`docs/FINANCIAL_TEST_CASES.md`).
- **Histórico unificado** com tipo e modo, filtro por modo, detalhe com copiar/reutilizar/excluir, lista própria para o modo financeiro e confirmação ao limpar tudo.
- **Configurações:** separador decimal, separador de milhares, casas decimais financeiras, modo de pagamento, pagamentos por ano e *Restaurar configurações padrão*.
- Textos completos em **inglês e português do Brasil**, com testes de consistência.
- Testes instrumentados dos fluxos essenciais; teste que garante que todas as variações de layout definem as mesmas views.
- Documentação: `README.md`, `docs/ARCHITECTURE.md`, `docs/RPN_GUIDE.md`, `docs/FINANCIAL_FUNCTIONS.md`, `docs/FINANCIAL_TEST_CASES.md`, `docs/PRIVACY.md`, `docs/RELEASE_CHECKLIST.md`, `docs/THIRD_PARTY_LICENSES.md`.

### Alterado
- Novo `applicationId` provisório `io.github.vandircf.calculadorafinanceira` (o `namespace` do código continua `com.darkempire78.opencalculator`), nome *Calculadora Financeira RPN*, `versionCode 1`, `versionName 0.1.0`.
- Novo **ícone original**.
- A preferência antiga de modo científico passa a definir só o modo inicial.
- Assinatura de release lida de `keystore.properties` **fora do repositório**; sem ele, a build de release sai sem assinatura.
- Backup automático restrito às preferências (`res/xml/backup_rules.xml`, `data_extraction_rules.xml`).
- Tela *Sobre*: política de privacidade e licença/créditos agora são locais; atalhos que apontavam para o OpenCalc (avaliar, traduzir, Discord) foram ocultados e o crédito ao projeto original foi mantido.

### Corrigido
- `tan(90°)` e demais pontos indefinidos agora são erro de domínio (antes retornava um valor enorme).
- Ruído de ponto flutuante em funções trigonométricas (`sin(−180°)` dava −1,2e‑16) agora resulta em 0.
- Quatro testes do projeto original dependiam do idioma do computador e falhavam em pt-BR; passaram a usar `Locale.US`.
- Falha do teste instrumentado de exemplo no build de debug (sufixo `.debug`).

### Removido
- Permissões `SYSTEM_ALERT_WINDOW` e `USE_FULL_SCREEN_INTENT` (não usadas).
- Ativos de distribuição do OpenCalc (`fastlane/`, `playStore/`, `.github/FUNDING.yml`): descreviam outro aplicativo e a conta de apoio do autor original. O crédito e a licença permanecem.
- Imagens PNG do ícone antigo.

### Observações
- Os erros do lint que já existiam no projeto original estão em `app/lint-baseline.xml`; apenas problemas novos falham o lint.
