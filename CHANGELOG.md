# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/). Versões ainda não distribuídas.

## [0.1.0] — projeto pessoal, instalado por APK

### Mudança de escopo
O aplicativo é **pessoal e não será publicado em loja**. Saíram do projeto: `store/` (textos, capturas e declarações de loja), a lista de conferência de publicação, a configuração de AAB, o ícone de loja do OpenCalc (`ic_launcher-playstore.png`), os atalhos de doação, avaliação, tradução, Discord e política em página web, e a classe `DonationDialog`. Entrou `docs/GERAR_APK.md` e os scripts `tools/gerar-apk.ps1` e `tools/criar-chave-release.ps1`.

### Visual da calculadora financeira (estilo HP-12C) e temas
- **Teclado financeiro no estilo das calculadoras financeiras clássicas:** funções em **dourado acima** das teclas (`f`) e em **azul** nas teclas (`g`), teclas `f` e `g` coloridas, `ENTER` alto e claro. Paisagem: 4 linhas de 10 teclas; retrato: 8 linhas de 5. Teclas diretas `GRAVAR`, `CHAMAR`, `ÚLT X`; a antiga tecla `2ª` foi substituída por `f` e `g` (`docs/RPN_GUIDE.md`).
- **Nova tecla `%T`** (porcentagem do total: X é quantos por cento de Y), com testes.
- **Escudos lado a lado no topo** para trocar o tema com um toque (no lugar do logotipo único).
- **Flamengo com vermelho vivo**, distinto do vinho do Cavaliers; novos tokens `on_key_enter`, `shift_f`, `shift_g` e testes de contraste e de distância entre as paletas.

### Adicionado
- **Dois temas escuros, Flamengo e Cleveland Cavaliers**, por atributos de tema (`calc_*`) e uma paleta por tema em `colors.xml`; seletor com logotipo e prévia, troca imediata e tema lembrado ao reabrir. Logotipos fornecidos pelo dono do projeto em `res/drawable-nodpi`; `tools/prepare_logos.py` os torna transparentes (`docs/TEMAS.md`).
- **Formato brasileiro por padrão** (`1.234.567,89`), com testes de que os cálculos nunca usam texto localizado (`docs/FORMATACAO_NUMERICA.md`).
- **Rótulos das teclas financeiras em português** (2ª, INÍCIO, FIM, GRAVAR, CHAMAR, ÚLT X, LIMPAR, LIMP. REG), dica ao tocar e segurar cada tecla e linha de ajuda com o significado ("PV — Valor presente"). A tecla do decimal mostra a vírgula.
- `BaseActivity`: todas as telas recebem um contexto pt-BR, mesmo com o celular em inglês.
- `docs/AUDITORIA_PORTUGUES.md`, `docs/AUDITORIA_PRIVACIDADE.md`, `docs/TEMAS.md`, `docs/FORMATACAO_NUMERICA.md`, `docs/GERAR_APK.md`.
- Testes: `ThemeResourcesTest` (paletas completas, contraste WCAG ≥ 4,5, sem cor fixa no código, logotipos transparentes), `FormatoBrasileiroTest`, `LayoutConsistencyTest`, `StringResourcesTest` reescrito (um só idioma, sem texto fixo, sem inglês), e 7 testes de interface novos (tema padrão, troca, restauração, três modos nos dois temas, idioma e formato).

### Alterado
- **Idioma único: português do Brasil.** `values/strings.xml` agora é o pt-BR; os 60 outros idiomas, o seletor de idioma e `locales_config.xml` foram removidos. Sete textos que o OpenCalc não tinha em pt-BR foram traduzidos.
- Tela *Sobre* refeita (logotipo do tema, versão, crédito ao OpenCalc, licença e privacidade como texto local).
- Configurações: removidas as linhas *Idioma*, *Sistema de numeração* (indiano) e *Modo científico padrão* (conflitava com o seletor de modo); *Tema* agora abre o novo seletor.
- Mensagem "mais de uma solução" lista os valores encontrados em vez de repetir um texto de desenvolvedor em inglês.
- "Limpar Histórico" → "Limpar histórico"; "offline" → "sem internet"; rótulo da tela de configurações (aparecia como `SettingsActivity`) → "Configurações".

### Corrigido
- **Travamento ao digitar rápido**: o cursor do visor podia valer −1 e derrubar o aplicativo (`StringIndexOutOfBoundsException`); agora é sempre válido.
- Colar `1,234.5` (formato americano) era lido como `1,2345`; agora é recusado.
- Ordem de inicialização do painel financeiro (a tela não abria ao usar um mapa de rótulos declarado depois de `build()`).

### Removido
- Temas Claro, Escuro, AMOLED e Material You, `values-night` e o modo claro.
- Dependência `androidx.compose.runtime` e o plugin do compilador do Compose (sem uso): APK de release de 2,36 MB para 2,08 MB, depois de testar.

---

## Histórico anterior (fases 1 a 15)

Base: OpenCalc 3.2.1 (commit `0584d61`), sob GPL-3.0. Itens de loja descritos abaixo foram removidos pela mudança de escopo acima.

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
