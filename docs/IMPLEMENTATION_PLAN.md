# Plano de Implementação — Calculadora (Básica · Científica · Financeira RPN)

> Fase 0 — auditoria e plano técnico. Nenhum código de produção foi alterado.
> Base: fork de OpenCalc (GPL-3.0), commit `0584d61`.

## 1. Resumo da arquitetura encontrada

| Item | Achado |
|---|---|
| Linguagem | Kotlin 2.0.0 (plugin `kotlin.android` + `compose.compiler`; **Compose não é usado**, só `androidx.compose.runtime` como dependência) |
| Gradle / AGP | Gradle 8.10.2 (wrapper), AGP 8.8.0, catálogo em `gradle/libs.versions.toml` |
| SDK | `minSdk 21`, `targetSdk 35`, `compileSdk 35`; JVM target 1.8 |
| UI | Views XML + ViewBinding, Material Components 1.12, `ConstraintLayout`, `AndroidSlidingUpPanel` (painel de histórico) |
| Arquitetura | **Activity única "gorda"** (`MainActivity.kt`, 1447 linhas). Sem ViewModel (a dependência `lifecycle-viewmodel-ktx` existe mas não é usada), sem navegação (Navigation/Fragments), sem DI |
| Sistema algébrico | Próprio (README explica a decisão de não usar mXparser): `Expression` (pré-processamento textual) + `Calculator` (parser recursivo descendente sobre `BigDecimal`) |
| Estado | Campos privados da Activity + **variáveis globais top-level** de erro em `Calculator.kt` (`division_by_0`, `domain_error`, `syntax_error`, `is_infinity`, `require_real_number`) |
| Persistência | `SharedPreferences` padrão via `MyPreferences`; histórico serializado em JSON (Gson) dentro de uma única chave |
| Temas | `Themes.kt` + estilos `AppTheme`, `AmoledTheme`, `MaterialYouTheme`; `forceDayNight` aplicado em `OpenCalcApp` |
| i18n | `values-*` com ~60 idiomas; já existem `values-pt` e `values-pt-rBR`; `locales_config.xml` (per-app language) |
| Permissões | `VIBRATE`, `SYSTEM_ALERT_WINDOW`, `USE_FULL_SCREEN_INTENT` (sem INTERNET) |
| Extras | Quick Settings Tile (`MyTileService`), tela de bloqueio, Fastlane metadata, `playStore/` |
| CI | Diretório `.github/` presente, sem `workflows/` |

## 2. Mapa de módulos e diretórios relevantes

Módulo único `:app`, pacote `com.darkempire78.opencalculator`:

| Caminho | Linhas | Responsabilidade |
|---|---|---|
| `activities/MainActivity.kt` | 1447 | UI, entrada de teclas, orquestra cálculo, histórico, modo científico, formatação do display |
| `calculator/Calculator.kt` | 417 | Avaliador `BigDecimal`: + − × ÷ `#`(mod) `^`, √, !, ln, log₂, log₁₀, exp, sin/cos/tan e inversas, π, e |
| `calculator/parser/Expression.kt` | 396 | Normaliza símbolos, insere `×` implícito, trata `%`, `√`, `!`, fecha parênteses |
| `calculator/parser/NumberFormatter.kt` | 156 | Formatação com separadores de milhar/decimal, sistemas de numeração |
| `history/History.kt`, `HistoryAdapter.kt` | 11 / 204 | Modelo `(calculation, result, time, id)` e RecyclerView com swipe-to-delete |
| `MyPreferences.kt` | 123 | Acesso tipado às preferências + histórico |
| `util/ScientificMode.kt`, `MyPreferenceMigrator.kt` | 28 / 101 | Enum OFF/NOT_ACTIVE/ACTIVE e **precedente de migração de preferências** |
| `Themes.kt` | 132 | Seletor e aplicação de temas |
| `activities/SettingsActivity.kt` + `res/xml/root_preferences.xml` | 235 | Configurações (PreferenceFragmentCompat) |
| `res/layout`, `layout-land`, `-small`, `-sw600dp`, `-sw720dp` | — | Um `activity_main.xml` por classe de tela, com teclado científico embutido |
| `app/src/test`, `app/src/androidTest` | — | Testes (ver §6) |

## 3. Fluxo atual de cálculo

1. Cada botão chama um handler XML (`onClick`) em `MainActivity` que insere texto no `EditText input` (`updateDisplay`, `addSymbol`).
2. Um `TextWatcher` dispara `updateResultDisplay()` a cada mudança.
3. `Expression.getCleanExpression()` converte o texto exibido (×, ÷, `log(`, `sin⁻¹`, `E`, `%`, `!`, separadores) em uma string "limpa".
4. `Calculator.evaluate(clean, isDegreeModeActivated)` avalia com parser recursivo e devolve `BigDecimal`; erros são sinalizados pelas **flags globais**.
5. `MainActivity` lê as flags, arredonda (`roundResult`, `numberPrecision`), formata (`NumberFormatter`) e mostra no `resultDisplay`; erros pintam o display (`setErrorColor`).
6. Ao `=`, grava `History` (ou auto-salva sem `=`, opção) em `SharedPreferences`.

**Observações:** pré-visualização de resultado ao vivo; `%` resolvido por reescrita de string; `sin/cos/tan/ln/log` passam por `Double` (precisão ~15 dígitos); `+ − × ÷ ^ √` usam `BigDecimal`.

## 4. Sistema de interface, histórico e temas

- **Interface:** layouts XML por classe de tela; o teclado científico é um grupo de views dentro do mesmo layout, mostrado/ocultado por `enableOrDisableScientistMode`/`hideScientificMode`. Tipo de modo guardado como `ScientificModeTypes` (OFF = só básico, NOT_ACTIVE, ACTIVE). Não há conceito de "modo" com troca de teclado inteira.
- **Histórico:** `List<History>` ⇄ JSON em uma chave de `SharedPreferences`; tamanho máximo configurável (`historySize`, padrão 50); sem campo de tipo/modo. Falha de parse devolve lista vazia (perda silenciosa).
- **Temas:** índice (Padrão/AMOLED/Material You) + day/night; troca recria a Activity.

## 5. Dependências externas

`androidx.core/appcompat/preference/constraintlayout`, `material 1.12.0`, `lifecycle-viewmodel-ktx 2.8.3`, `androidx.compose.runtime 1.6.8` (aparentemente sem uso), `gson 2.10.1`, `AndroidSlidingUpPanel 4.6.1` (**JitPack** — único repositório fora de Google/Maven Central), JUnit 4.13.2, AndroidX Test/Espresso. Nenhuma dependência de rede em runtime.

## 6. Testes existentes

- Unitários (JVM): `ExpressionUnitTest` (~14 testes) e `NumberFormatterTest` (~22 testes) — cobrem pré-processamento/formatação; **não há testes diretos de `Calculator.evaluate`** (depende de `android.os.Build` em `sqrt`, o que exigiria Robolectric ou desacoplamento).
- Instrumentados: `MainActivityTests` (lançar, rotacionar, recriar) e `ExampleInstrumentedTest`. Sem testes de fluxo de cálculo.

## 7. Resultado do baseline (antes de qualquer mudança)

Ver seção **12 — Registro do build inicial**.

## 8. Riscos técnicos

| # | Risco | Impacto | Mitigação |
|---|---|---|---|
| R1 | `MainActivity` monolítica (1447 linhas) e sem ViewModel | Alto: três modos aumentariam o acoplamento | Extrair estado e lógica de teclas para `ViewModel` + classes puras antes de adicionar modos |
| R2 | Flags de erro **globais mutáveis** em `Calculator.kt` | Alto: não são thread-safe nem testáveis em isolamento; vazam entre cálculos | Substituir por resultado selado (`CalcResult.Success/Error(type)`) mantendo API antiga como adaptador |
| R3 | `Calculator` depende de `android.Build` | Médio: impede teste JVM puro | Injetar estratégia de `sqrt` ou usar `BigDecimal.sqrt` via fallback próprio (Newton já existe) |
| R4 | Funções transcendentais via `Double` | Médio: precisão limitada e sem tolerância documentada | Documentar tolerância; testes com delta |
| R5 | Histórico em uma chave JSON de `SharedPreferences` | Médio: sem tipo, sem versão do esquema, parse falho apaga tudo | Adicionar `schemaVersion`, campo `type`, migração e backup antes de sobrescrever |
| R6 | `minSdk 21` + `java.time`/APIs modernas | Baixo | Usar `BigDecimal`/`java.util` ou habilitar desugaring |
| R7 | Layouts duplicados por classe de tela | Médio: o modo financeiro precisaria de ~8 variações | Fragmento financeiro com `GridLayout`/`ConstraintLayout` flexível e poucos qualificadores |
| R8 | Permissões `SYSTEM_ALERT_WINDOW`/`USE_FULL_SCREEN_INTENT` herdadas | Médio para a Play Store | Avaliar remoção na Fase 15 (fora do escopo da Fase 0) |
| R9 | Assinatura de release usa `signingConfigs.debug` | Médio | Release assinada via `keystore.properties` **fora do repo** (Fase 15) |
| R10 | JitPack como fonte de dependência | Baixo | Registrar em licenças/atribuições; avaliar fixar versão (já fixada) |
| R11 | `applicationId`/pacote `com.darkempire78.*` pertence ao projeto original | Alto para publicação | Definir novo `applicationId` na Fase 15 e documentar; manter `namespace` para evitar refator massivo |
| R12 | Marca: "OpenCalc" e ícone são do projeto original | Médio | Renomear e criar ícone próprio; **manter** avisos de copyright/GPL e atribuição |
| R13 | Download do Gradle/AGP exige rede na primeira execução | Baixo | Registrado no baseline |

## 9. Pontos reutilizáveis e pontos a refatorar

**Reutilizar:** `Calculator`/`Expression`/`NumberFormatter` para básico e científico; `MyPreferences` (adicionar chaves); `Themes`; `HistoryAdapter`; `MyPreferenceMigrator` como padrão de migração; infraestrutura de testes JVM; i18n existente (`values-pt-rBR`).

**Refatorar:**
1. Estado global de erro → resultado tipado (R2).
2. Lógica de entrada de teclas da Activity → classe testável (`InputController`).
3. `History` + persistência → modelo versionado com `type` (R5).
4. `ScientificModeTypes` (visibilidade do teclado) → conceito de **modo ativo** (`CalculatorMode`), mantendo a preferência atual como migração.
5. Dependência de `android.Build` no cálculo (R3).

## 10. Proposta de arquitetura para os três modos

```
CalculatorMode { BASIC, SCIENTIFIC, FINANCIAL_RPN }      // persistido; sobrevive a rotação
        │
MainActivity (host) ── ModeSwitcher (seletor segmentado no topo, visível e acessível)
        │
        ├─ Fragment Algébrico (BASIC/SCIENTIFIC)  ← reaproveita layout/lógica atuais
        │       └─ Expression + Calculator (BigDecimal)       [sem mudar semântica]
        └─ Fragment Financeiro RPN
                └─ FinancialViewModel
                      ├─ domain/rpn/RpnStack, RpnEngine         (X,Y,Z,T, LASTX, ENTER...)
                      ├─ domain/finance/TvmSolver, CashFlow, Npv, Irr, Amortization, RateConversion
                      └─ BigDecimal (MathContext/escala/arredondamento explícitos)

Compartilhado: MyPreferences, Themes, HistoryRepository (tipo + modo), NumberFormatter, strings i18n
```

Decisões:
- **Navegação:** *seletor segmentado (Material `MaterialButtonToggleGroup`) no topo* + `Fragment` por família de modo. Motivos: já se usa Material 1.12; é visível e acessível (TalkBack), não conflita com o painel deslizante de histórico (que usa gesto vertical) nem com gestos laterais do sistema; evita a complexidade de Navigation Component. Básico e Científico compartilham o fragmento atual (alternam o teclado, como hoje).
- **Motor financeiro e RPN** são Kotlin puro, **sem dependência de Android**, em pacote `domain/`, 100% testável na JVM. O motor algébrico existente **não** é reutilizado para RPN.
- **Numérica:** `BigDecimal` com `MathContext(34, HALF_EVEN)` interno, exibição configurável (padrão 2 casas para moeda); iterações (Newton/bissecção para taxa e IRR) isoladas em `Double` apenas quando necessário, documentadas, com limite de 100 iterações e tolerância 1e‑10, resultado reconvertido por `BigDecimal` controlado.
- **Persistência:** modo ativo e registradores TVM em `SharedPreferences` (JSON versionado); registradores sobrevivem a rotação (ViewModel + `SavedStateHandle`) e **persistem** após fechar o app, com ação explícita "Limpar registradores" (decisão a ser confirmada na Fase 6).
- **Histórico:** `History` ganha `type` e `schemaVersion` com valores padrão para itens antigos (compatível com o JSON atual — Gson preenche ausentes com `null`, tratado por migração).

## 11. Fases e critérios de aceite

Cada fase termina com: build verde, testes verdes, relatório, commit local. Sem push.

| Fase | Entrega | Critérios de aceite |
|---|---|---|
| 0 | Auditoria e este plano | Baseline registrado; documento criado |
| 1 | `CalculatorMode`, seletor segmentado, persistência do modo, migração de `ScientificModeTypes` | Troca de modo clara e acessível; modo sobrevive a rotação e reabertura; nenhuma função perdida; testes do enum/migração |
| 2 | Modo básico revisado | Casos de borda documentados e testados (÷0, `%`, zero à esquerda, precisão, notação científica); comportamento atual preservado salvo exceções documentadas |
| 3 | Modo científico | Testes com tolerância documentada para sin/cos/tan (graus e rad), log, ln, √, potência, fatorial, erros; `Calculator` testável em JVM (R2/R3) |
| 4 | Motor financeiro | Domínio sem Android; `BigDecimal` com política explícita; testes de entradas inválidas |
| 5 | Pilha RPN | Testes de sequências de teclas (`2 ENTER 3 +` = 5; `10 ENTER 4 −` = 6; regra de `%` documentada); elevação/descida/LASTX documentados |
| 6 | TVM | Qualquer variável resolvida; início/fim; sinais não alterados silenciosamente; testes por fórmula independente |
| 7 | CF/NPV/IRR | Casos do enunciado; IRR com limite de iterações e alerta de múltiplas raízes |
| 8 | Amortização e taxas | Tabela/resumo; nominal vs efetiva distintas e rotuladas |
| 9 | UI financeira | Pilha visível, ENTER destacado, função secundária sempre indicada, retrato/paisagem/telas pequenas, TalkBack |
| 10 | Histórico unificado | Tipos, filtro, detalhe, copiar, excluir, limpar com confirmação, migração segura com testes |
| 11 | Configurações | Todas as opções listadas; restaurar padrões; sem alterar resultados antigos |
| 12 | i18n | EN e pt-BR completos, sem texto fixo; verificação de layout com textos longos |
| 13 | Qualidade | Suíte completa + `FINANCIAL_TEST_CASES.md` + UI tests viáveis + lint |
| 14 | Documentação | README, ARCHITECTURE, RPN_GUIDE, FINANCIAL_FUNCTIONS, PRIVACY, RELEASE_CHECKLIST, CHANGELOG; atribuição ao OpenCalc |
| 15 | Distribuição | `applicationId` novo documentado, ícone/nome próprios, assinatura fora do repo, R8, AAB; **sem publicar** |

## 12. Registro do build inicial

Ambiente: Windows 11, JDK Temurin 17, Android SDK local (platforms 34/35/36), Gradle 8.10.2 (baixado na primeira execução).

Comando: `./gradlew.bat --no-daemon assembleDebug testDebugUnitTest lintDebug --continue` (5m33s)

| Etapa | Resultado |
|---|---|
| `assembleDebug` | **OK** — APK instalado e executado em emulador (API 35); ver `docs/baseline-screenshot.png` |
| `testDebugUnitTest` | **36 testes, 4 falhas** (`ExpressionUnitTest`: `pow_isCorrect`, `division_isCorrect`, `number_factorial_equal_decimal_factorial`, `decimal_factorial_shows_correct_result`). Os valores obtidos divergem dos esperados (ex.: esperado 50.0, obtido 5.0; esperado 1.38e80, obtido Infinity) — falhas **pré-existentes** no upstream, a investigar na Fase 2/3 (sem alteração de código na Fase 0) |
| `lintDebug` | **Falha: 23 erros, 202 avisos** — 21× `InvalidVectorPath`, 2× `NewApi` (`MyTileService` exige API 24 com minSdk 21). Pré-existentes. Não há `ktlint`/`detekt` configurados |

Conclusão: build compila; a suíte existente **não está verde** e o lint não passa. Antes de declarar qualquer fase concluída, será necessário corrigir as 4 falhas (ou documentá-las como comportamento esperado) e criar um baseline de lint.

## 13. Fase 1 — decisões (multimodo)

- **Modelo:** `util/CalculatorMode` (`BASIC`, `SCIENTIFIC`, `FINANCIAL_RPN`), Kotlin puro e testado em JVM.
- **Navegação escolhida:** seletor segmentado (`MaterialButtonToggleGroup`) no topo, à esquerda do menu ⋮, incluído via `view_mode_switcher.xml` nos 3 layouts existentes (retrato, paisagem, sw720dp-land). Sempre visível, com `contentDescription` por botão; não usa gestos laterais (evita conflito com o painel de histórico e gestos do sistema).
- **Estado:** `CalculatorModeViewModel` (sobrevive a rotação/tema/idioma) + `MyPreferences.calculatorMode` (último modo restaurado ao abrir).
- **Migração:** enquanto o usuário não escolher um modo, o inicial deriva da preferência antiga `scientificMode` (`ACTIVE` → `SCIENTIFIC`, demais → `BASIC`). A preferência antiga é mantida só como valor inicial. **Mudança de comportamento documentada:** o modo escolhido no seletor passa a mandar sobre a configuração antiga (que antes era reaplicada a cada `onResume` em retrato); em paisagem o básico agora oculta as linhas científicas, e o científico as mostra.
- **Histórico:** `History` ganhou o campo opcional `mode`; entradas antigas (sem o campo) são lidas como `BASIC` via `History.origin`. Nenhuma migração destrutiva.
- **Financeiro:** painel provisório "Em breve" (substituído na Fase 9); indicador DEG oculto nesse modo.
- **Lint:** criado `app/lint-baseline.xml` com os 23 erros pré-existentes, para que apenas problemas novos falhem o lint.
- **Testes:** corrigido `ExpressionUnitTest` (dependia do locale da máquina; agora `Locale.US`) e criado `CalculatorModeTest`.
