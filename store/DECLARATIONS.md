# Declarações para a loja (rascunho para o responsável conferir)

Estas respostas descrevem o aplicativo **como está no código**. Quem publica deve conferir cada item no formulário da loja; nada foi enviado.

## Dados do aplicativo

| Item | Valor |
|---|---|
| Nome | Calculadora Financeira RPN (EN: Financial RPN Calculator) |
| `applicationId` | `io.github.vandircf.calculadorafinanceira` (provisório; **não pode mudar após a primeira publicação**) |
| Versão | 0.1.0 (`versionCode` 1) |
| Categoria sugerida | Ferramentas, ou Finanças |
| Idiomas com texto completo | inglês (EUA) e português (Brasil); os demais idiomas herdados do OpenCalc usam o inglês nos textos novos |
| Preço | gratuito |
| Licença | GPL-3.0 (código-fonte obrigatório) |

## Anúncios

**Não contém anúncios.**

## Compras no aplicativo

Não há.

## Coleta e compartilhamento de dados (formulário "Segurança dos dados")

- Coleta algum dado do usuário? **Não.**
- Compartilha dados com terceiros? **Não.**
- Os dados são criptografados em trânsito? **Não se aplica** (não há transmissão de dados; o aplicativo não tem a permissão de internet).
- O usuário pode pedir a exclusão dos dados? Os dados ficam só no aparelho e podem ser apagados no próprio aplicativo ou limpando o armazenamento (ver `docs/PRIVACY.md`).
- Observação: o backup automático do Android pode incluir as preferências do aplicativo na conta Google **do próprio usuário**, sem acesso do desenvolvedor.

## Permissões declaradas

Somente `VIBRATE`.

## Classificação indicativa (questionário IARC)

Sem violência, conteúdo sexual, linguagem imprópria, drogas, jogos de azar, compras, interação entre usuários, localização compartilhada ou conteúdo gerado pelo usuário. Resultado esperado: **livre para todos os públicos (Livre / PEGI 3 / Everyone)**. Confirmar no questionário oficial.

## Aviso de conteúdo financeiro

O aplicativo é uma calculadora e **não oferece aconselhamento financeiro**. Sugere-se incluir na descrição da loja: *"Os resultados são cálculos matemáticos e não substituem a orientação de um profissional."*

## Política de privacidade

Texto em `docs/PRIVACY.md` e dentro do aplicativo. As lojas pedem uma **URL pública**: publique o texto (por exemplo, no repositório ou em uma página) antes de enviar.

## Materiais

- Títulos e descrições: `store/listing/en-US` e `store/listing/pt-BR`.
- Capturas de tela: `store/screenshots/`.
- Ícone 512×512 e imagem de destaque 1024×500: `store/graphics/` (gerados por `generate_graphics.py` a partir da arte original do ícone).
