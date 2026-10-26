# Temas

O aplicativo tem **dois temas**, ambos escuros: **Flamengo** (vermelho e preto) e **Cleveland Cavaliers** (preto, vinho e dourado). Eles mudam **só a aparência**: os três modos (básica, científica, financeira RPN) usam as mesmas telas e os mesmos motores de cálculo. O último tema escolhido é restaurado ao abrir o aplicativo e a troca é imediata, sem reiniciar.

```
                APP
                 │
        ┌────────┼─────────┐
     BÁSICA  CIENTÍFICA  FINANCEIRA      (um motor de cálculo cada, independente do tema)
        └────────┼─────────┘
              TEMA  →  Flamengo | Cleveland Cavaliers
```

## Como funciona

1. `res/values/colors.xml` guarda as duas paletas, com os mesmos nomes de token (`flamengo_*`, `cavaliers_*`). **É o único lugar com cores.**
2. `res/values/styles.xml` tem `Theme.Calc.Flamengo` e `Theme.Calc.Cavaliers`, que ligam cada token a um **atributo de tema**.
3. Layouts, desenhos e código só leem atributos (`?attr/calc_key_number`, ...). Nenhuma tela conhece o tema ativo.
4. `Themes.kt` guarda o id do tema escolhido em `SharedPreferences`, aplica o estilo com `setTheme()` antes de inflar cada tela e, ao trocar, chama `recreate()`.

## Tokens

| Token (atributo `calc_*`) | Uso |
|---|---|
| `background` | fundo geral |
| `surface` | histórico, painéis |
| `surface_variant` | menus, diálogos |
| `display_background` | área do visor |
| `primary` | cor principal do tema (logo) |
| `secondary` | segunda cor do tema |
| `accent` | destaques: textos de categoria, "f" e "g", barra do seletor, chaves ligadas |
| `text_primary` | textos e números |
| `text_secondary` | textos de apoio (resultado em prévia, ajuda) |
| `key_number` | teclas de número |
| `key_operator` | operadores `+ − × ÷ %` e parênteses |
| `key_function` | funções (fundo das teclas de contorno) |
| `key_danger` | limpar e apagar |
| `key_financial` | teclas financeiras n, i, PV, PMT, FV |
| `key_enter` | `=` e ENTER |
| `outline` | contornos |
| `error` | mensagens de erro (vermelho legível nos dois temas) |
| `logo` | logotipo do tema (referência a um desenho) |

Os atributos herdados do OpenCalc (`foreground_color`, `button_color`, `text_color`, ...) continuam existindo e apontam para estes tokens, para as telas antigas não precisarem mudar.

## Cores de cada tema

As cores foram tiradas dos logotipos que você forneceu.

| Token | Flamengo | Cleveland Cavaliers |
|---|---|---|
| background | `#0B0B0B` | `#0A0A0A` |
| display | `#141414` | `#111111` |
| primary | `#F5281A` | `#860038` |
| accent | `#FF5447` | `#FDBB30` |
| key_number | `#262626` | `#242124` |
| key_operator | `#5A1410` | `#6E002E` |
| key_enter | `#C8180A` | `#A5003F` |
| key_danger | `#5C5C5C` | `#5C5C5C` |
| text_primary | `#F5F5F5` | `#F5F5F5` |

Todos os pares de texto e fundo têm contraste WCAG ≥ 4,5:1 (a menor razão é 4,76: cor de destaque sobre tecla de número no tema Flamengo; texto principal sobre qualquer tecla fica acima de 5,3). O teste `ThemeResourcesTest` confere isso a cada build, além de garantir que os dois temas definem todos os tokens e que nenhum layout ou código tem cor fixa.

Para ajustar um tema, mude os valores em `colors.xml`; nada mais precisa ser tocado.

## Logotipos

Os logotipos que você enviou estão no projeto como imagens transparentes:

| Arquivo | Tema |
|---|---|
| `app/src/main/res/drawable-nodpi/logo_flamengo.png` | Flamengo |
| `app/src/main/res/drawable-nodpi/logo_cavaliers.png` | Cleveland Cavaliers |

Os originais (capturas de tela recebidas, com fundo branco) ficam em `assets-originais/`. Para gerar de novo os PNGs transparentes a partir de novos originais:

```powershell
python tools\prepare_logos.py assets-originais\flamengo-original.png assets-originais\cavaliers-original.png
```

O script remove só o branco ligado à borda (as letras brancas dentro do escudo ficam) e recorta o espaço vazio. Precisa do Pillow (`pip install pillow`).

**Para trocar por arquivos melhores:** substitua os dois PNGs, mantendo os mesmos nomes. Formato recomendado: **PNG com fundo transparente (RGBA), lado maior de pelo menos 512 px**. Os atuais têm cerca de 260 a 350 px, suficientes para os tamanhos usados (30 dp na barra, 112 dp na tela Sobre, 56 dp no seletor), mas ficariam nítidos em telas de alta densidade com mais resolução. Não é preciso mudar código.

Onde o logotipo aparece, sempre discreto e nunca atrás de números: barra superior da calculadora (30 dp, ao lado do menu), seletor de tema, tela Sobre. O teste `ThemeResourcesTest` confere que os dois arquivos existem e têm canal alfa.

## Seletor de tema

Configurações → **Tema** abre um diálogo com os dois temas. Cada linha mostra o logotipo, o nome, uma prévia de cinco cores **tirada do próprio tema** (não do tema em uso) e a marca "✓ Selecionado" no atual. Tocar em um tema o aplica na hora.

## Verificação visual

Capturas dos dois temas (básica, científica, financeira, retrato e paisagem, histórico, configurações, diálogos e Sobre) estão em `docs/screenshots/`.


## Troca de tema pelos escudos

No topo da tela, ao lado do menu, ficam os **dois escudos** (Flamengo e Cleveland Cavaliers) lado a lado, separados por um espaço da largura de um escudo. O escudo do tema em uso fica nítido, com um contorno; tocar no outro troca o tema na hora. A escolha é lembrada ao reabrir.

## Vermelho do Flamengo × vinho do Cavaliers

O vermelho do Flamengo foi trocado por um vermelho vivo (teclas de operação `#D91A0B`, destaque `#F5281A`) e a tecla `ENTER`/`=` ficou clara, para que não seja confundido com o vinho (`#6E002E`/`#860038`) do Cavaliers, cuja tecla `ENTER` é dourada. `ThemeResourcesTest` garante que as cores principais dos dois temas fiquem distantes entre si e que o texto continue legível (contraste ≥ 4,5).
