# Lista de conferência de release

Nada aqui publica o aplicativo. Cada item marcado como **[feito]** foi verificado neste repositório; os demais dependem de decisão ou ação do responsável.

## Identidade

- [x] **Nome:** "Calculadora Financeira RPN" (`@string/app_label`, não traduzível). Sem nome, logotipo, imagem ou desenho de terceiros.
- [x] **Ícone original:** `res/drawable/ic_launcher_foreground.xml` (+ fundo `#0F4C5C`), adaptativo no Android 8+ e com versão em camadas para Android 5–7; ícone monocromático e do bloco de Configurações Rápidas atualizados.
- [ ] **`applicationId` definitivo.** Hoje: `io.github.vandircf.calculadorafinanceira` (provisório, derivado do usuário do GitHub). **Depois da primeira publicação ele não pode mais mudar.** Para trocar, edite só `defaultConfig.applicationId` em `app/build.gradle.kts` (o `namespace` do código permanece `com.darkempire78.opencalculator`, o que evita uma refatoração de pacotes). Registre a decisão no CHANGELOG.
- [x] **Versão:** `versionCode 1`, `versionName 0.1.0` (`app/build.gradle.kts`). A cada envio à loja o `versionCode` precisa aumentar.

## Assinatura (fora do repositório)

- [x] O repositório não contém keystore, senhas nem tokens. `.gitignore` bloqueia `keystore.properties`, `*.jks`, `*.keystore`, `*.p12`, `*.pem`, `local.properties` e `*.aab`.
- [x] `app/build.gradle.kts` lê `keystore.properties` (na raiz do projeto) ou o arquivo apontado por `KEYSTORE_PROPERTIES`. Sem ele, o release sai sem assinatura.
- [ ] **Criar a chave de upload** (uma vez, fora do repositório) e guardar cópia segura:
  ```bash
  keytool -genkeypair -v -keystore C:/chaves/calculadora-release.jks -alias upload -keyalg RSA -keysize 4096 -validity 10000
  ```
  e criar `keystore.properties` com `storeFile`, `storePassword`, `keyAlias`, `keyPassword`. Na Google Play, prefira a **assinatura de apps pela Play** (a chave acima vira só a chave de upload).

## Build de release

- [x] `./gradlew assembleRelease bundleRelease` passa. Saídas: `app/build/outputs/apk/release/` e `app/build/outputs/bundle/release/app-release.aab`.
- [x] **R8 ligado** (`isMinifyEnabled`, `isShrinkResources`) com regras que mantêm os modelos lidos por Gson (`proguard-rules.pro`) e `@SerializedName` em todos eles. A build de release (assinada localmente com a chave de depuração, só para o teste) foi instalada num emulador e verificada: cálculo TVM, fechar e reabrir mantendo os registradores.
- [x] `bundle { language { enableSplit = false } }` mantém todos os idiomas no módulo base, para o seletor de idioma do aplicativo funcionar.
- [ ] Gerar o **AAB assinado** com a chave de upload e conferir com `bundletool` ou o *pre-launch report* da Play Console.
- [ ] Testar o release num aparelho real (instalar o APK assinado) antes de enviar.

## Verificações automáticas (todas passando em 2026-10-26)

- [x] `./gradlew testDebugUnitTest` — testes unitários (JVM).
- [x] `./gradlew lintDebug` e `lintVitalRelease` (rodado pelo `assembleRelease`) — só problemas novos falham; os herdados do OpenCalc estão em `app/lint-baseline.xml`.
- [x] `./gradlew connectedDebugAndroidTest` — fluxos essenciais no emulador (API 35).
- [ ] Repetir os testes instrumentados em aparelhos físicos e em Android 5/6 (API 21–23), ainda não testados. O lint confirma que nenhuma API acima de 21 é usada sem salvaguarda.
- Verificação de formatação: **o projeto não tem ktlint/spotless configurado**; nenhuma foi adicionada.

## Dados, privacidade e permissões

- [x] Única permissão: `VIBRATE` (conferido com `aapt2 dump badging`). `SYSTEM_ALERT_WINDOW` e `USE_FULL_SCREEN_INTENT`, herdadas e sem uso, foram removidas.
- [x] Sem acesso à internet, sem anúncios, sem compras, sem telemetria.
- [x] **Backup:** restrito às preferências (`backup_rules.xml`, `data_extraction_rules.xml`); decisão e texto em `docs/PRIVACY.md`.
- [x] Política de privacidade: `docs/PRIVACY.md` e dentro do aplicativo (Sobre).
- [ ] **Publicar o texto da política em uma URL pública** — a Play Console exige uma URL.

## Licenças e atribuições

- [x] `LICENSE` (GPL-3.0) mantida; atribuição ao OpenCalc no README, na tela Sobre e em `docs/THIRD_PARTY_LICENSES.md`.
- [x] Licenças das bibliotecas listadas (Apache 2.0).
- [ ] **Oferecer o código-fonte** correspondente a cada versão distribuída (GPL-3.0): manter o repositório público com a tag da versão e indicar o endereço na página da loja.
- [ ] Se for distribuir fora da Play (F-Droid, IzzyOnDroid), eles usam metadados próprios; os arquivos `fastlane/` e `playStore/` herdados do OpenCalc foram **removidos** porque descreviam outro aplicativo.

## Loja

- [x] Títulos e descrições (curta e completa) em inglês e português do Brasil: `store/listing/`. Limites respeitados (título ≤ 30, curta ≤ 80, completa ≤ 4000).
- [x] Capturas de tela (1080×2400) em inglês e português: `store/screenshots/`.
- [x] Ícone 512×512 e imagem de destaque 1024×500: `store/graphics/` (geradas por `generate_graphics.py` a partir da arte original).
- [x] Respostas sobre anúncios, coleta de dados, permissões e classificação indicativa: `store/DECLARATIONS.md` (**rascunho**: confirmar no formulário oficial).
- [ ] Capturas de tablet (7"/10") se a loja pedir.
- [ ] Revisar os textos com alguém que use o aplicativo no dia a dia.

## Antes de apertar "publicar"

1. Confirmar `applicationId`, nome e ícone.
2. Gerar o AAB assinado com a chave de upload.
3. Testar em aparelho real.
4. Publicar a política de privacidade e o código-fonte da versão.
5. **Só então** enviar, com autorização expressa do responsável. O repositório e estes documentos não fazem nenhum envio.
