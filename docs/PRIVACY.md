# Política de privacidade

*Calculadora Financeira RPN* — versão 0.1.0. O mesmo texto está dentro do aplicativo (Sobre → Política de privacidade).

## Resumo

O aplicativo **funciona totalmente offline** e **não coleta nenhum dado**. Não há conta, anúncios, compras, análise de uso, rastreamento nem acesso à internet.

## O que fica no seu aparelho

| Dado | Onde | Para que serve |
|---|---|---|
| Configurações (tema, formatos, opções) | armazenamento privado do aplicativo | lembrar suas preferências |
| Histórico de cálculos | idem | consultar e reutilizar resultados |
| Memória e registradores financeiros (n, i, PV, PMT, FV, fluxos de caixa, pilha) | idem | continuar uma conta depois de fechar o app |
| Último modo usado | idem | abrir no modo em que você parou |

Esses dados **nunca saem do aparelho por ação do aplicativo**: ele não tem permissão de internet.

## Permissões

Apenas **`VIBRATE`** (vibração ao tocar nas teclas; pode ser desativada em Configurações). O aplicativo **não** pede localização, contatos, câmera, microfone, armazenamento, rede, sobreposição de tela nem notificações. (O projeto original pedia `SYSTEM_ALERT_WINDOW` e `USE_FULL_SCREEN_INTENT`, que não eram usadas e foram removidas.)

## Backup

O aplicativo permite o backup automático do Android **somente das preferências** (que incluem configurações, histórico e registradores financeiros). Se o backup do aparelho estiver ativado, o Android grava esses dados na **sua** conta Google e os restaura em um aparelho novo; o desenvolvedor não tem acesso a eles. Para impedir, desative o backup do aparelho ou desmarque o aplicativo nas configurações de backup do Android. As regras estão em `res/xml/backup_rules.xml` e `res/xml/data_extraction_rules.xml`.

## Como apagar seus dados

- **Histórico:** menu ⋮ → *Limpar histórico* (com confirmação), ou deslizar um item, ou *Excluir* no detalhe do item.
- **Registradores financeiros:** `2ª` + `CLx` ou menu ⋯ → *Limpar registradores* (com confirmação); *Limpar tudo* também apaga a pilha e a memória.
- **Configurações:** Configurações → *Restaurar configurações padrão*.
- **Tudo:** Configurações do Android → Apps → Calculadora Financeira RPN → Armazenamento → *Limpar dados*, ou desinstalar.

## Crianças e serviços de terceiros

Não há coleta de dados de ninguém, inclusive de menores. O aplicativo não usa SDKs de terceiros de anúncio, análise ou rastreamento. As bibliotecas incluídas estão em [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md).

## Alterações

Mudanças nesta política acompanham a versão do aplicativo e ficam registradas em [CHANGELOG.md](../CHANGELOG.md).
