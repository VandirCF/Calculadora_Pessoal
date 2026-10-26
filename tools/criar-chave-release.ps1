<#
.SYNOPSIS
  Cria a chave de assinatura do APK de release e o arquivo keystore.properties.

  Execute VOCÊ MESMO, no seu computador: as senhas são digitadas por você e ficam só nos dois
  arquivos criados (a chave e keystore.properties). Nada é enviado a lugar nenhum, e o
  .gitignore impede que qualquer um dos dois entre no Git.

.EXAMPLE
  .\tools\criar-chave-release.ps1
  .\tools\criar-chave-release.ps1 -Pasta D:\chaves

Guia completo: docs/GERAR_APK.md
#>
param(
    [string]$Pasta = (Join-Path $HOME 'chaves-android')
)

$ErrorActionPreference = 'Continue' # comandos nativos escrevem avisos no stderr; o resultado vem de $LASTEXITCODE
Set-Location (Join-Path $PSScriptRoot '..')

if (Test-Path 'keystore.properties') {
    Write-Host 'Já existe um keystore.properties neste projeto. Nada foi alterado.' -ForegroundColor Yellow
    Write-Host 'Se quiser criar outra chave, apague o arquivo antes (e guarde a chave antiga: sem ela não dá para atualizar o app já instalado).'
    exit 1
}

$keytool = Get-Command keytool -ErrorAction SilentlyContinue
if (-not $keytool -and $env:JAVA_HOME) { $keytool = Get-Command (Join-Path $env:JAVA_HOME 'bin\keytool.exe') -ErrorAction SilentlyContinue }
if (-not $keytool) {
    Write-Host 'Não encontrei o keytool (vem com o JDK). Instale o JDK 17 e abra um novo terminal.' -ForegroundColor Red
    exit 1
}

New-Item -ItemType Directory -Force -Path $Pasta | Out-Null
$arquivo = Join-Path $Pasta 'calculadora-release.jks'
if (Test-Path $arquivo) {
    Write-Host "Já existe $arquivo. Escolha outra -Pasta ou mova o arquivo." -ForegroundColor Yellow
    exit 1
}

$alias = 'calculadora'
Write-Host "A chave será criada em: $arquivo"
Write-Host 'O keytool vai pedir uma senha e alguns dados (nome etc.; podem ser simples, como "Pessoal").'
Write-Host ''
& $keytool.Source -genkeypair -v -keystore $arquivo -alias $alias -keyalg RSA -keysize 4096 -validity 10000
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host ''
Write-Host 'Digite de novo a senha que você acabou de escolher (para o arquivo keystore.properties).'
$segura = Read-Host 'Senha' -AsSecureString
$senha = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($segura))

$caminhoComBarras = $arquivo -replace '\\', '/'
@(
    "storeFile=$caminhoComBarras"
    "storePassword=$senha"
    "keyAlias=$alias"
    "keyPassword=$senha"
) | Set-Content -Path 'keystore.properties' -Encoding ASCII

Write-Host ''
Write-Host 'Pronto.' -ForegroundColor Green
Write-Host "  Chave:      $arquivo"
Write-Host '  Config.:    keystore.properties (nesta pasta, ignorado pelo Git)'
Write-Host ''
Write-Host 'GUARDE UMA CÓPIA da chave e da senha em outro lugar (pendrive, gerenciador de senhas).'
Write-Host 'Sem elas você não consegue atualizar o app já instalado: terá de desinstalar e perder os dados.'
Write-Host ''
Write-Host 'Agora gere o APK:  .\tools\gerar-apk.ps1 -Tipo release'
