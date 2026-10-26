# Privacidade

*Calculadora Financeira RPN* é um aplicativo **pessoal, instalado por APK**. O texto abaixo também está dentro do aplicativo (Sobre → Política de privacidade).

## Resumo

O aplicativo funciona **sem internet** e **não coleta nenhum dado**. Não há conta, login, anúncios, compras, análise de uso, relatório de falhas online nem rastreamento.

## O que fica no aparelho

| Dado | Para que serve |
|---|---|
| configurações, tema e último modo usado | lembrar suas escolhas |
| histórico de cálculos | consultar e reutilizar resultados |
| memória e registradores financeiros (n, i, PV, PMT, FV, fluxos de caixa, pilha) | continuar uma conta depois de fechar o app |

Ficam no armazenamento privado do aplicativo e **nunca saem do aparelho por ação do aplicativo**: ele nem tem a permissão de internet.

## Permissões

Apenas **vibração** (retorno das teclas; pode ser desativado em Configurações). Nada de localização, contatos, câmera, microfone, armazenamento, rede ou notificações.

## Backup do Android

O Android pode copiar as preferências do aplicativo para a **sua própria** conta Google, se o backup do aparelho estiver ativado, e restaurá-las em um celular novo. Para evitar, desative o backup do aparelho ou mude `android:allowBackup` para `false` no manifesto.

## Apagar os dados

- Histórico: menu ⋮ → *Limpar histórico* (com confirmação), deslizar um item ou *Excluir* no detalhe.
- Registradores financeiros: `f` + `CLx`, ou menu ⋯ → *Limpar registradores* / *Limpar tudo* (com confirmação).
- Configurações: Configurações → *Restaurar configurações padrão*.
- Tudo: Configurações do Android → Apps → Calculadora Financeira RPN → Armazenamento → *Limpar dados*, ou desinstalar.

A verificação técnica (permissões, bibliotecas, APK) está em [AUDITORIA_PRIVACIDADE.md](AUDITORIA_PRIVACIDADE.md).
