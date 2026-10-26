<#
.SYNOPSIS
  Gera o APK do aplicativo (debug ou release) e confere a assinatura.

.EXAMPLE
  .\tools\gerar-apk.ps1               # APK de depuração
  .\tools\gerar-apk.ps1 -Tipo release # APK de release (precisa de keystore.properties)

Guia completo: docs/GERAR_APK.md
#>
param(
    [ValidateSet('debug', 'release')]
    [string]$Tipo = 'debug'
)

$ErrorActionPreference = 'Continue' # comandos nativos escrevem avisos no stderr; o resultado vem de $LASTEXITCODE
Set-Location (Join-Path $PSScriptRoot '..')

if ($Tipo -eq 'release') {
    $temChave = (Test-Path 'keystore.properties') -or ($env:KEYSTORE_PROPERTIES -and (Test-Path $env:KEYSTORE_PROPERTIES))
    if (-not $temChave) {
        Write-Host 'Não encontrei keystore.properties.' -ForegroundColor Yellow
        Write-Host 'Sem ele o APK de release sai SEM assinatura e o Android não o instala.'
        Write-Host 'Crie a chave uma vez com:  .\tools\criar-chave-release.ps1'
        Write-Host 'Veja docs/GERAR_APK.md.'
        exit 1
    }
    & .\gradlew.bat assembleRelease
    $apk = 'app\build\outputs\apk\release\app-release.apk'
} else {
    & .\gradlew.bat assembleDebug
    $apk = 'app\build\outputs\apk\debug\app-debug.apk'
}
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if (-not (Test-Path $apk)) {
    Write-Host "Esperava encontrar $apk e não encontrei." -ForegroundColor Red
    exit 1
}
$caminho = (Resolve-Path $apk).Path
Write-Host ''
Write-Host "APK gerado: $caminho" -ForegroundColor Green
Write-Host ('Tamanho: {0:N1} MB' -f ((Get-Item $caminho).Length / 1MB))

# confere a assinatura com o apksigner do Android SDK, se existir
$sdk = $env:ANDROID_HOME
if (-not $sdk -and (Test-Path 'local.properties')) {
    $linha = Select-String -Path 'local.properties' -Pattern '^sdk\.dir=(.+)$' | Select-Object -First 1
    if ($linha) { $sdk = $linha.Matches[0].Groups[1].Value -replace '\\\\', '\' -replace '\\:', ':' }
}
if ($sdk -and (Test-Path (Join-Path $sdk 'build-tools'))) {
    $ferramenta = Get-ChildItem (Join-Path $sdk 'build-tools') -Directory | Sort-Object Name -Descending |
        ForEach-Object { Join-Path $_.FullName 'apksigner.bat' } | Where-Object { Test-Path $_ } | Select-Object -First 1
    if ($ferramenta) {
        Write-Host ''
        Write-Host 'Assinatura:'
        & $ferramenta verify --print-certs $caminho 2>&1 | Where-Object { $_ -notmatch 'not protected by signature' }
    }
}

Write-Host ''
Write-Host 'Para instalar no celular: copie o arquivo (cabo USB, Drive, e-mail...) e abra-o no aparelho,'
Write-Host "ou com o celular conectado e a depuração USB ativa:  adb install -r `"$caminho`""
