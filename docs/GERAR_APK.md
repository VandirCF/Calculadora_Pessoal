# Como gerar e instalar o APK

O aplicativo é de uso pessoal: **não há loja, AAB, Play Console nem publicação**. A distribuição é só o arquivo `.apk`, instalado à mão no seu celular. Tudo abaixo vale para a configuração Gradle que existe neste repositório (Gradle 8.10.2, AGP 8.8.0, Kotlin 2.0.0, `compileSdk 35`, `minSdk 21`).

## O que você precisa

- **JDK 17** (`java -version` deve mostrar 17).
- **Android SDK** com a plataforma 35 e as build-tools 35 (o Android Studio instala). O caminho do SDK vai em `local.properties`, na raiz do projeto (o arquivo é ignorado pelo Git):
  ```properties
  sdk.dir=C:/Users/SEU_USUARIO/AppData/Local/Android/Sdk
  ```
- Internet **só na primeira compilação** (o Gradle baixa as bibliotecas; uma delas vem do JitPack). Depois disso compila offline.

## APK de depuração (para testar)

```powershell
.\tools\gerar-apk.ps1
```

ou direto com o Gradle:

```powershell
.\gradlew.bat assembleDebug
```

O arquivo sai em `app\build\outputs\apk\debug\app-debug.apk`. Ele é assinado automaticamente com a chave de depuração do Android, então **instala sem nenhuma configuração**. Tem o sufixo `.debug` no identificador, por isso **convive com o APK de release** no mesmo celular (são dois apps separados, cada um com seus dados).

## APK de release (para uso definitivo)

O Android **só instala APK assinado**. O release precisa de uma chave sua, criada **uma vez** e guardada **fora do Git**.

### 1. Criar a chave (uma vez)

```powershell
.\tools\criar-chave-release.ps1
```

O script usa o `keytool` do JDK: **você** digita a senha e alguns dados simples (pode ser "Pessoal"). Ele cria:

- a chave em `C:\Users\SEU_USUARIO\chaves-android\calculadora-release.jks` (ou na pasta que você indicar com `-Pasta`);
- o arquivo `keystore.properties` na raiz do projeto, com o caminho e as senhas.

Os dois ficam **fora do controle de versão**: o `.gitignore` bloqueia `keystore.properties`, `*.jks`, `*.keystore`, `*.p12` e `*.pem`. Confira com `git status` antes de cada commit.

Prefere fazer à mão? É o mesmo:

```powershell
keytool -genkeypair -v -keystore C:\chaves-android\calculadora-release.jks -alias calculadora -keyalg RSA -keysize 4096 -validity 10000
```

e crie `keystore.properties` assim (barras `/`, mesmo no Windows):

```properties
storeFile=C:/chaves-android/calculadora-release.jks
storePassword=SUA_SENHA
keyAlias=calculadora
keyPassword=SUA_SENHA
```

Se preferir guardar o arquivo fora da pasta do projeto, defina a variável de ambiente `KEYSTORE_PROPERTIES` com o caminho dele.

> **Guarde uma cópia da chave e da senha** em outro lugar (pendrive, gerenciador de senhas). **Sem a mesma chave não dá para atualizar o app já instalado**: seria preciso desinstalar e perder o histórico e os registradores.

### 2. Gerar

```powershell
.\tools\gerar-apk.ps1 -Tipo release
```

ou:

```powershell
.\gradlew.bat assembleRelease
```

O arquivo sai em `app\build\outputs\apk\release\app-release.apk`. O script confere a assinatura com o `apksigner` do SDK e mostra o certificado.

Sem `keystore.properties`, o Gradle gera `app-release-unsigned.apk`, que o Android **recusa**. O script avisa antes disso.

O release usa **R8** (reduz e ofusca o código) e `shrinkResources`. Ele foi testado no emulador, mas **instale e confira os três modos** depois de gerar.

## Instalar no celular

1. Copie o `.apk` para o celular (cabo USB, Drive, e-mail ou WhatsApp para si mesmo).
2. Abra o arquivo no celular. Na primeira vez o Android pede para **permitir instalar apps desconhecidos** para o aplicativo que você usou (Arquivos, Chrome...). Em Configurações → Segurança (o caminho muda conforme a marca).
3. Toque em **Instalar**.

Com o celular ligado ao computador, depuração USB ativa e o `adb` do SDK:

```powershell
adb install -r app\build\outputs\apk\release\app-release.apk
```

`-r` atualiza mantendo os dados (só funciona com a **mesma chave** da instalação anterior).

## Nova versão

Edite `versionCode` (sempre aumentando) e `versionName` em `app/build.gradle.kts`, gere o APK com a mesma chave e instale por cima.

## Outros comandos úteis

```powershell
.\gradlew.bat testDebugUnitTest              # testes unitários
.\gradlew.bat lintDebug                      # análise estática (Android Lint)
.\gradlew.bat connectedDebugAndroidTest      # testes de interface (precisa de emulador ou celular)
```

## Identificador do aplicativo

`io.github.vandircf.calculadorafinanceira` (`defaultConfig.applicationId`). Como o app não será publicado, ele pode ser trocado livremente; só lembre que **mudar o identificador cria um app diferente**, sem os dados do anterior.
