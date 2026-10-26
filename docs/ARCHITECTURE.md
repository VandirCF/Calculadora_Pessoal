# Arquitetura

Aplicativo Android nativo (Kotlin, Views XML + ViewBinding, Material 3), derivado do OpenCalc. Módulo único `:app`; `minSdk 21`, `targetSdk 35`. Sem rede, sem conta, sem telemetria.

## Visão geral

```
CalculatorMode { BASIC, SCIENTIFIC, FINANCIAL_RPN }          util/CalculatorMode.kt
        │   persistido em MyPreferences; ViewModel mantém na rotação
MainActivity ── seletor segmentado (MaterialButtonToggleGroup) no topo
        │
        ├─ Básico / Científico  →  motor algébrico original
        │       Expression (texto) → Calculator (parser recursivo, BigDecimal) → CalcResult
        │       NumberFormatter · SignToggle · PastedNumber · CalculatorMemory
        │
        └─ Financeiro RPN  →  FinancialPanel (UI em código)  →  RpnEngine
                                  │                               ├─ pilha X/Y/Z/T, LAST X, memória 0–9
                                  │                               ├─ Tvm        (n, i, PV, PMT, FV)
                                  │                               ├─ Npv, Irr   (CashFlows, RootFinder)
                                  │                               ├─ Amortization
                                  │                               └─ RateConversions
                                  └─ FinancialStateStore (JSON versionado) → MyPreferences

Compartilhados: MyPreferences (SharedPreferences) · Themes (Flamengo | Cleveland Cavaliers) · History/HistoryAdapter · strings (pt-BR)
```

## Pacotes

| Pacote | Conteúdo |
|---|---|
| `activities` | `BaseActivity` (contexto pt-BR), `MainActivity`, `SettingsActivity`, `AboutActivity`, `CalculatorModeViewModel` |
| `calculator`, `calculator.parser` | motor algébrico (básico e científico) |
| `finance` | domínio financeiro **sem Android**: `BigMath`, `Tvm`, `CashFlows` (VPL e TIR), `Amortization`, `RateConversions`, `RootFinder`, `RpnFormatter`, resultados e erros |
| `finance.rpn` | `RpnEngine`, `RpnKey`, `RpnEvent`, `RpnSnapshot` |
| `financial` | camada de apresentação do modo financeiro: `FinancialPanel`, `FinancialViewModel`, `FinancialStateStore` |
| `history` | `History` (modelo versionado por campos opcionais), `HistoryType`, `HistoryAdapter` |
| `util` | `CalculatorMode`, `CalculatorMemory`, `SignToggle`, `PastedNumber`, `SeparatorSettings`, migração de preferências |

## Decisões

**Navegação.** Seletor segmentado no topo, sempre visível e com descrição por botão para leitores de tela. Não usa gestos laterais (conflitariam com o painel de histórico e com os gestos do sistema) nem Navigation Component. Básico e Científico compartilham a tela original; Financeiro é um painel sobreposto.

**Motor financeiro separado e independente da UI.** O domínio não conhece Android nem os botões, e a UI só traduz teclas em chamadas ao `RpnEngine`. Isso permitiu cobrir tudo com testes JVM (ver `docs/FINANCIAL_TEST_CASES.md`). O motor algébrico original **não** é usado como RPN: os dois conceitos são distintos.

**`BigDecimal` com política explícita** (34 dígitos, `HALF_EVEN`). Detalhes em [FINANCIAL_FUNCTIONS.md](FINANCIAL_FUNCTIONS.md).

**Persistência.** O modo ativo é restaurado ao abrir. O estado financeiro (registradores, pilha, memória, fluxos) é gravado como JSON com `version` em `SharedPreferences` e **persiste após fechar o aplicativo**: perder uma conta por fechar o app é pior do que ter de limpá-la. Para limpar há ação explícita e com confirmação (2ª + CLx, ou o menu ⋯). JSON ilegível ou de outra versão é ignorado e o motor começa vazio.

**Histórico unificado.** Uma única lista em JSON; cada item tem `mode`, `type` e `details` opcionais. Itens antigos (sem esses campos) são lidos como Básico. Resultados já gravados são texto e **não mudam** quando o usuário altera formatos nas configurações. O histórico financeiro tem lista própria porque o painel deslizante fica por trás do painel financeiro.

**Configurações.** Separador decimal e de milhares (automático, ponto, vírgula, espaço, nenhum; nunca coincidem), casas decimais financeiras, modo e número de pagamentos por ano, além das opções originais. "Restaurar padrões" remove só as configurações, não os dados.

**Estado de erro do motor algébrico.** O motor original usa variáveis globais de erro. `Calculator.evaluateSafe` devolve `CalcResult` (valor ou erro tipado), zera as flags antes e depois e serializa o acesso. A interface atual ainda usa `evaluate` com as flags (migração completa fica como melhoria futura, para não arriscar regressões).

**Compatibilidade com API 21.** Nenhuma API acima de 21 sem salvaguarda: a raiz quadrada de `BigDecimal` é calculada por Newton (a função nativa exige API 33), verificado pelo lint.

**Temas.** Dois temas escuros (Flamengo, Cleveland Cavaliers), só dados: `colors.xml` guarda as paletas, `styles.xml` liga cada cor a um atributo `calc_*`, e as telas leem atributos. `Themes.kt` guarda o tema escolhido e o aplica com `setTheme()` antes de inflar; ao trocar, a tela é recriada. Os motores de cálculo não conhecem o tema. Detalhes em [TEMAS.md](TEMAS.md).

**Idioma e números.** Um único idioma (pt-BR): os recursos padrão são o português e `BaseActivity` dá a cada tela um contexto pt-BR. O número interno (`BigDecimal`, texto com `.`) nunca é localizado; vírgula e ponto de milhares só aparecem na exibição ([FORMATACAO_NUMERICA.md](FORMATACAO_NUMERICA.md)).

**Distribuição.** Só APK, instalado à mão ([GERAR_APK.md](GERAR_APK.md)). Sem loja, AAB, anúncios, conta ou rede ([AUDITORIA_PRIVACIDADE.md](AUDITORIA_PRIVACIDADE.md)).

## Testes

| Tipo | Local | Quantidade aproximada |
|---|---|---|
| Unitários JVM | `app/src/test` | básico, científico, entrada, formatação, modo, histórico, separadores, recursos de string, e todo o domínio financeiro |
| Instrumentados | `app/src/androidTest` | fluxos essenciais (modos, básico, científico, RPN, TVM, registradores, histórico, tema, graus/radianos) |

Comandos: `./gradlew testDebugUnitTest`, `./gradlew lintDebug`, `./gradlew connectedDebugAndroidTest` (precisa de emulador ou aparelho).
