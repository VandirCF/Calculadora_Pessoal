# Licenças de terceiros e atribuições

## Projeto original

Este aplicativo é um **trabalho derivado do OpenCalc**, de Darkempire78 e colaboradores
(<https://github.com/Darkempire78/OpenCalc>), distribuído sob a **GNU General Public License, versão 3**. O texto da licença está em [`LICENSE`](../LICENSE) e os avisos de direitos autorais e a atribuição originais foram mantidos (README, tela *Sobre*, este arquivo).

Como obra derivada, este projeto também é licenciado sob a **GPL-3.0**: quem receber o aplicativo, em qualquer forma, tem direito ao código-fonte correspondente e às mesmas liberdades. Alterações feitas aqui estão descritas em [CHANGELOG.md](../CHANGELOG.md).

## Bibliotecas incluídas no aplicativo

| Biblioteca | Versão | Licença | Uso |
|---|---|---|---|
| AndroidX (core-ktx, appcompat, preference, constraintlayout, lifecycle, recyclerview) | ver `gradle/libs.versions.toml` | Apache License 2.0 | base da interface |
| Material Components for Android | 1.12.0 | Apache License 2.0 | componentes visuais |
| Gson | 2.10.1 | Apache License 2.0 | gravar o histórico e o estado financeiro em JSON |
| AndroidSlidingUpPanel (fork `hannesa2`) | 4.6.1 | Apache License 2.0 | painel deslizante do histórico |
| Kotlin standard library e kotlinx.coroutines | 2.0.0 | Apache License 2.0 | linguagem |

Apache License 2.0 é compatível com a GPL-3.0 (o conjunto é distribuído sob GPL-3.0). Texto: <https://www.apache.org/licenses/LICENSE-2.0>.

A biblioteca do painel deslizante é baixada do JitPack (<https://jitpack.io>) durante o build; nenhuma biblioteca faz chamadas de rede em tempo de execução.

## Ferramentas de teste (não distribuídas no aplicativo)

JUnit 4 (EPL 1.0), AndroidX Test e Espresso (Apache 2.0).

## Marcas e identidade

O nome *Calculadora Financeira RPN*, o ícone e a interface financeira são **originais**. O aplicativo **não** usa nome, logotipo, imagens ou desenho industrial de nenhum fabricante de calculadoras, e não é afiliado a nenhum deles. A notação RPN é um método de cálculo, não uma marca. O nome e o ícone "OpenCalc" pertencem ao projeto original e não são usados aqui.


## Logotipos dos temas

Os logotipos do **Flamengo** e do **Cleveland Cavaliers** foram fornecidos pelo dono do projeto e são marcas de seus titulares. São usados apenas como identidade visual pessoal em um aplicativo **que não é publicado nem distribuído**; não fazem parte da licença GPL-3.0 do código. Se o aplicativo vier a ser compartilhado, remova `logo_flamengo.png` e `logo_cavaliers.png` (ou substitua por arquivos próprios) e as cores dos temas continuam funcionando.
