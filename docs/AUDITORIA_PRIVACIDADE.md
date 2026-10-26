# Auditoria de conectividade, privacidade e dependências

Objetivo: confirmar que o aplicativo é **completamente offline**, sem anúncios, login, conta, monetização, telemetria, analytics ou coleta de dados. Feita sobre o código e sobre o **APK de release** (R8 ligado).

## Resultado

**Nenhuma biblioteca do aplicativo se comunica com a rede, e o aplicativo não pede nenhuma permissão de rede.**

## 1. Permissões

Lidas do APK de release com `aapt2 dump badging`:

| Permissão | Para quê |
|---|---|
| `VIBRATE` | retorno tátil nas teclas (pode ser desativado em Configurações) |

Não há `INTERNET`, `ACCESS_NETWORK_STATE`, localização, câmera, microfone, contatos, armazenamento, notificações nem sobreposição de tela. Duas permissões herdadas do OpenCalc (`SYSTEM_ALERT_WINDOW` e `USE_FULL_SCREEN_INTENT`) não eram usadas e foram removidas.

O teste `StringResourcesTest.theManifestHasNoLanguageListAndNoNetworkAccess` falha se `INTERNET` ou `ACCESS_NETWORK_STATE` aparecerem no manifesto.

## 2. Dependências de execução

Resolvidas por `gradlew :app:dependencies --configuration releaseRuntimeClasspath`:

| Grupo | Para quê | Rede? |
|---|---|---|
| `androidx.*` (appcompat, core, preference, lifecycle, constraintlayout, recyclerview, fragment, activity, ...) | base da interface e preferências | não |
| `com.google.android.material:material` | componentes visuais | não |
| `com.google.code.gson:gson` | gravar histórico e estado financeiro em JSON local | não (ver nota abaixo) |
| `com.github.hannesa2:AndroidSlidingUpPanel` | painel deslizante do histórico (uma `View`) | não |
| `org.jetbrains.kotlin:kotlin-stdlib*`, `kotlinx-coroutines-*` | linguagem | não |
| `androidx.startup`, `androidx.profileinstaller`, `androidx.emoji2` | inicialização local, perfil de desempenho, emojis | não |

Ausentes (confirmado): Firebase, Google Play Services, Crashlytics, AdMob, Billing, OkHttp, Retrofit, WebView, SDKs de análise.

### Verificação no APK

Busca por texto nas classes (`classes.dex`) do APK de release:

| Procurado | Ocorrências |
|---|---|
| `okhttp`, `Retrofit`, `HttpURLConnection`, `HttpsURLConnection`, `java/net/Socket`, `WebView` | 0 |
| `firebase`, `crashlytics`, `com/google/android/gms`, `admob`, `analytics`, `com/android/billingclient` | 0 |
| `java/net/URL` | 1 |
| endereços `http(s)://` | apenas os dois namespaces XML do Android |

A única ocorrência de `java/net/URL` é um adaptador de tipos do Gson (que sabe converter o *texto* de uma URL); ele não abre conexões e o aplicativo não usa URLs.

### Dependência removida

`androidx.compose.runtime` e o plugin do compilador do Compose estavam no projeto original sem uso (não há `@Composable` no código). Foram removidos **depois de testar**: build, testes unitários e instrumentados continuaram passando e o APK de release caiu de 2,36 MB para 2,08 MB.

## 3. Código do aplicativo

Busca em `app/src/main` por `INTERNET`, `openConnection`, `HttpURL`, `ACTION_VIEW`, `http://` e `https://`: **sem ocorrências** além de comentários e de um link de documentação dentro de um XML. Os atalhos externos do OpenCalc (avaliar, traduzir, comunidade, doação, política em página web) foram removidos; a política de privacidade e a licença são texto dentro do aplicativo.

## 4. Rede durante a compilação

Só o **build** usa rede, e só na primeira vez: o Gradle baixa o próprio Gradle, o plugin do Android e as bibliotecas (Google, Maven Central e **JitPack**, de onde vem o `AndroidSlidingUpPanel`, com versão fixa). Nada disso vai para o APK.

## 5. Dados no aparelho

| Dado | Onde | Observação |
|---|---|---|
| configurações, tema, modo ativo | `SharedPreferences` do aplicativo | |
| histórico | idem, JSON | texto já formatado |
| memória, registradores financeiros, pilha | idem, JSON com versão | persistem ao fechar o app; apagam com 2ª + CLx ou em Limpar tudo |

`allowBackup` continua ligado (padrão do Android). Se o backup do aparelho estiver ativado, o Android pode copiar essas preferências para **a sua própria conta Google** e restaurá-las num celular novo. Se preferir que nada saia do aparelho, desative o backup do Android ou mude `android:allowBackup` para `false` no manifesto. As regras estão em `res/xml/backup_rules.xml` e `res/xml/data_extraction_rules.xml`: só preferências.

## 6. O que não existe (por decisão)

Anúncios, compras, assinaturas, Google Play Billing, login, conta, sincronização, backend, API externa, relatório de falhas online, telemetria, analytics, identificadores de usuário ou de aparelho.
