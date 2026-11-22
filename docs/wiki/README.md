# Crop Manager — Wiki completa

**Guia em português · versão 0.3.0-alpha1 · Minecraft 1.12.2 · IC2 Experimental 2.8.222-ex112**

O Crop Manager reúne cuidados e colheita de crops do IndustrialCraft 2, com ferramentas para diagnóstico, seleção de sementes, monitoramento e reaproveitamento de vegetais. Esta wiki descreve os seis blocos e quatro itens registrados na versão atual.

![Visão geral das seis máquinas e suas interfaces](images/visao-geral.png)

*Capturas reais do cliente de desenvolvimento, organizadas em uma montagem. Os valores e filtros das imagens são exemplos, não configurações obrigatórias.*

## Índice

1. [Instalação e compatibilidade](#instalacao)
2. [Primeira fazenda: passo a passo](#inicio)
3. [Catálogo de máquinas e itens](#catalogo)
4. [Crop Manager básico](#basico)
5. [Crop Manager avançado](#avancado)
6. [Área, velocidade e energia](#energia)
7. [Upgrades](#upgrades)
8. [Filtros e Classificador de Sementes](#classificador)
9. [Monitor Agrícola e alertas](#monitor)
10. [Sonda e Cartão de Configuração](#ferramentas)
11. [Composteira e Composto Orgânico](#composteira)
12. [Bancada de Sementes](#bancada)
13. [Caderno Agrícola](#caderno)
14. [Todas as receitas](#receitas)
15. [Automação e exemplos de montagem](#automacao)
16. [Configuração do servidor](#configuracao)
17. [Salvamento, quebra e multiplayer](#persistencia)
18. [Problemas frequentes](#problemas)
19. [Limites, validação e fontes](#fontes)

<a id="instalacao"></a>
## 1. Instalação e compatibilidade

| Componente | Versão / condição |
|---|---|
| Minecraft | 1.12.2 |
| Dependência obrigatória | IndustrialCraft 2 Experimental 2.8.222-ex112 |
| Addon | CropManager-0.3.0-alpha1.jar |
| Ambiente local documentado | E2EE 1.87.3, Cleanroom 0.6.11-alpha, Java 25.0.3 |
| CropAccelerator | Opcional; coexistência verificada em ambiente isolado |
| Idiomas | Português brasileiro e inglês; inglês é o fallback |

1. Feche o jogo e faça uma cópia do mundo antes de testar a versão alpha.
2. Coloque somente o JAR de distribuição em `minecraft/mods` da instância que você utiliza.
3. Mantenha o IC2 obrigatório instalado. Não é necessário substituir o IC2 ou atualizar o Cleanroom apenas para instalar o addon.
4. Remova o JAR antigo do Crop Manager ao atualizar.
5. Em multiplayer, instale **a mesma versão no servidor e em todos os clientes**. A conexão recusa versões diferentes.
6. Inicie a instância normalmente. Não coloque os JARs `-dev`, `-sources` ou `-runtime-tests` na pasta de mods do jogo.

O bytecode do addon é Java 8, mas isso não significa que você deva mudar o Java usado pelo seu launcher para Java 8. Use o Java exigido pelo ambiente de jogo.

O ambiente E2EE acima é o alvo local do projeto. A validação disponível não equivale a um teste de toda a instância E2EE com todos os mods e scripts.

<a id="inicio"></a>
## 2. Primeira fazenda: passo a passo

1. Prepare uma plantação usando **crops do IC2**, com plantas definidas nos crop sticks. O Manager não cuida de terra arada vanilla nem de um plantio vanilla comum.
2. Fabrique o Manager básico com **Crop Harvester + Circuito Eletrônico Avançado + Crop-Matron**, em uma linha horizontal.
3. Coloque a máquina perto da plantação. A área padrão cobre nove blocos de largura por nove de comprimento, centrada na máquina, nas alturas imediatamente abaixo, na mesma altura e imediatamente acima dela.
4. Conecte uma fonte EU compatível com o tier de entrada: o básico começa no **tier 1**.
5. Abra com botão direito e abasteça fertilizante IC2, água e Weed-Ex conforme os cuidados desejados. Também é possível usar somente colheita.
6. Ative os botões **Colher**, **Adubar**, **Água** e **EX** conforme sua necessidade. As quatro funções começam habilitadas.
7. Deixe a redstone em **R0 / sempre** para o primeiro teste.
8. Clique em **Mostrar área** e feche a tela para conferir o contorno no mundo.
9. Aguarde a varredura alcançar cada planta. Na configuração padrão, uma volta do básico leva aproximadamente **24,3 segundos a 20 TPS**, sem interrupções.
10. Retire os produtos manualmente ou por automação na base. Mantenha espaço para a colheita continuar.

Não é necessário preencher todos os insumos para utilizar uma função isolada. Falta de água, por exemplo, não impede uma colheita que tenha energia e espaço disponíveis.

<a id="catalogo"></a>
## 3. Catálogo de máquinas e itens

| Máquina | Função | Reserva EU inicial | Tier inicial | Upgrades |
|---|---|---:|---:|---|
| Crop Manager | Cuidados e colheita de crops | 20.000 | 1 | Overclock, armazenamento, transformador |
| Crop Manager Avançado | Cuidados, colheita, diagnóstico e seleção informativa | 100.000 | 2 | Os mesmos do básico |
| Classificador de Sementes | Separa sementes em três categorias | 20.000 | 1 | Nenhum |
| Monitor Agrícola | Exibe recursos, diagnóstico e emite alertas | Não utiliza EU | — | Nenhum |
| Composteira Elétrica | Converte vegetais em composto | 20.000 | 1 | Nenhum |
| Bancada de Sementes | Compara duas sementes manualmente | Não utiliza EU | — | Nenhum |

“Reserva EU inicial” significa **capacidade de armazenamento sem upgrades**: colocar o bloco não fornece uma bateria cheia.

| Item | Uso |
|---|---|
| Sonda Agrícola | Consulta uma crop e vincula um monitor a um Manager |
| Cartão de Configuração | Copia funções, modo de redstone e, quando aplicável, filtros |
| Composto Orgânico | Ingrediente para fabricar fertilizante IC2 |
| Caderno Agrícola | Registra até 256 espécies analisadas por caderno |

<a id="basico"></a>
## 4. Crop Manager básico

![Interface do Crop Manager básico](images/manager-basico.png)

*A imagem mostra a interface compacta. Alguns rótulos podem aparecer abreviados conforme a revisão e o idioma.*

### Inventário e tanques

São **26 slots da máquina**, além do inventário do jogador:

| Grupo | Quantidade | Conteúdo |
|---|---:|---|
| Fertilizante | 3 | Fertilizante IC2 |
| Produtos | 15 | Resultados da colheita |
| Upgrades | 4 | Módulos compatíveis |
| Entrada de recipientes | 2 | Recipientes de abastecimento |
| Saída de recipientes | 2 | Recipientes resultantes |

Os tanques comportam **10.000 mB de água** e **10.000 mB de Weed-Ex**, separadamente. O armazenamento elétrico base é de **20.000 EU**, com entrada tier 1.

### O que cada botão faz

| Controle | Efeito |
|---|---|
| Colher | Permite colher no ponto ótimo ou máximo reconhecido pelo IC2 |
| Adubar | Permite aplicar fertilizante pelo método nativo do crop |
| Água | Permite hidratar a crop |
| EX | Permite aplicar Weed-Ex |
| R0 / R1 / R2 | Alterna o modo de redstone |
| Tema claro / escuro | Muda a preferência visual local |
| Mostrar / Ocultar área | Alterna o contorno local da área no mundo |
| Ícone azul **i** | Exibe os upgrades compatíveis |

Passe o mouse sobre barras, slots e controles para consultar detalhes. Os indicadores **EU**, **AD**, **H2O**, **EX** e **SAÍ** resumem energia, fertilizante, água, Weed-Ex e espaço de saída.

### Ordem de trabalho e colheita

Em cada posição, a ordem é **fertilizar → hidratar → aplicar Weed-Ex → colher**. O Manager utiliza os métodos originais do IC2; não acrescenta um multiplicador próprio de crescimento.

Os drops de cada colheita são calculados uma única vez. Se não couberem todos na saída, a sobra fica em uma fila persistente. Novas colheitas ficam bloqueadas até essa fila escoar. As outras funções podem continuar.

O básico interrompe a busca quando todas as quatro funções estão desligadas. A frente fica ativa durante dez ticks após uma ação bem-sucedida; esse detalhe visual não representa uma fonte de iluminação especial.

### Redstone

| Modo | Quando funciona |
|---|---|
| R0 — sempre | Independentemente do sinal; padrão |
| R1 — com sinal | Quando há sinal de redstone |
| R2 — sem sinal | Quando não há sinal de redstone |

A pausa bloqueia varredura e ações nas plantas. A máquina continua podendo receber EU, processar recipientes e escoar produtos pendentes. Não acumula trabalho para executar uma rajada ao sair da pausa.

<a id="avancado"></a>
## 5. Crop Manager Avançado

![Operação do Manager avançado](images/avancado-operacao.png)

O avançado mantém os mesmos **26 slots**, as quatro funções e a área padrão do básico. As diferenças são:

| Característica | Básico | Avançado |
|---|---:|---:|
| Capacidade EU base | 20.000 | 100.000 |
| Tier elétrico inicial | 1 | 2 — MV |
| Água | 10.000 mB | 20.000 mB |
| Weed-Ex | 10.000 mB | 20.000 mB |
| Intervalo base padrão | 2 ticks por posição | 1 tick por posição |
| Ciclo padrão 9×9×3 a 20 TPS | 24,3 s | 12,15 s |
| Diagnóstico e filtro informativo | Não | Sim |

Os tempos são teóricos, sem falta de energia, pausa por redstone ou outras interrupções. O avançado usa o mesmo custo por posição/ação e o mesmo teto de posições por tick.

### Abas e diagnóstico

![Diagnóstico da plantação](images/avancado-diagnostico.png)

- **Operação:** estado geral e controles de cuidado.
- **Diagnóstico / Dados:** plantas, maduras, ervas daninhas, não analisadas, fora dos critérios, idade do relatório e posições indisponíveis.
- **Seleção / Filtro:** limites de genética e restrição opcional de espécie.

O relatório só é publicado após um ciclo completo. Ele representa uma observação ao longo da varredura; consulte a idade antes de tratá-lo como situação atual de toda a plantação. Alterar filtros ou recarregar a máquina inicia uma nova coleta.

Não existe uma segunda varredura exclusiva para diagnóstico. O avançado continua percorrendo a área e consumindo EU de busca mesmo com os quatro cuidados desligados. Falta de energia e pausa por redstone interrompem essa coleta.

### Seleção visual

![Filtro do Manager avançado](images/avancado-filtro.png)

Ao habilitar o contorno da área, **caixas vermelhas grandes** marcam plantas fora do filtro; **caixas amarelas menores** marcam plantas não analisadas. Os marcadores refletem o último relatório recebido pela interface.

**O filtro é informativo.** Ele não altera a colheita normal, não arranca plantas, não replanta e não melhora genética. Para separar sacos de sementes fisicamente, use o Classificador.

As setas **< / >** recolhem ou expandem os painéis do avançado, classificador e monitor. O inventário permanece na mesma posição, e filtros e aba são preservados. O recolhimento é local à tela.

<a id="energia"></a>
## 6. Área, velocidade e energia

### Alcance

O raio horizontal padrão é **4**, formando um quadrado de **9×9**. As três camadas ficam nos deslocamentos verticais **−1, 0 e +1** em relação à máquina. Isso equivale a **243 posições** por volta, incluindo posições vazias.

Não há exigência de linha de visão. O contorno pode ser visualizado depois de fechar a GUI. **A interface atual não possui controles de raio ou camadas**: máquinas novas usam os padrões do servidor, e máquinas existentes preservam sua área salva, respeitando o limite global.

O addon não carrega chunks. Posições descarregadas ocupam uma tentativa no limite de processamento, mas não geram consulta de crop nem cobrança de EU de busca.

### Custos padrão

| Evento | Custo antes dos upgrades |
|---|---:|
| Consulta a uma posição carregada, mesmo vazia | 1 EU |
| Cuidado bem-sucedido: adubo, água ou Weed-Ex | 10 EU por ação |
| Colheita bem-sucedida | 20 EU |

Não existe um consumo ocioso fixo de EU/t nos Managers. O gasto depende da busca e das ações realizadas.

```text
EU = D × (P × scanEU + C × careEU + H × harvestEU)

P = posições carregadas consultadas
C = soma dos cuidados bem-sucedidos
H = colheitas realizadas
D = multiplicador de demanda dos upgrades
```

Exemplo: nove consultas, uma adubação, uma hidratação, uma aplicação de Weed-Ex e uma colheita custam **9 + 30 + 20 = 59 EU**, sem upgrades e com custos padrão. Um ciclo de 243 posições todas carregadas, sem ações, custa **243 EU**.

Os cuidados reservam EU antes da chamada nativa; se a ação falhar, a reserva é devolvida. Os fluidos são drenados pela lógica nativa, sem uma segunda cobrança artificial de água ou Weed-Ex.

### Velocidade

```text
posições por ciclo = (2 × raio + 1)² × quantidade de camadas
posições/tick = min(teto do servidor, 1 / intervalo efetivo)
segundos por ciclo = posições por ciclo / (posições/tick × TPS)
```

O teto padrão é **8 posições por tick**. Cada posição pode realizar no máximo as quatro ações. TPS abaixo de 20 aumenta o tempo real para completar a volta.

<a id="upgrades"></a>
## 7. Upgrades

Somente os dois Managers aceitam estes três tipos nativos do IC2:

| Upgrade | Efeito | Observação |
|---|---|---|
| Overclocker | Reduz o intervalo e aumenta a demanda | O aumento de custo continua mesmo quando a velocidade atinge o teto |
| Energy Storage | Aumenta a reserva EU | Cada unidade acrescenta 10.000 EU no IC2 auditado |
| Transformer | Aumenta o tier de entrada | Cada unidade acrescenta um tier no IC2 auditado |

Os fatores de tempo e demanda são obtidos das interfaces do IC2; a quantidade total de itens nos quatro slots conta para o cálculo.

```text
T = produto dos multiplicadores nativos de tempo, elevados às quantidades
D = produto dos multiplicadores nativos de demanda, elevados às quantidades
intervalo efetivo = intervalo do servidor × T × (0,5 no avançado; 1 no básico)
capacidade = capacidade base do modelo + armazenamento extra dos upgrades
tier = tier base do modelo + tiers extras dos upgrades
```

Armazenamento não aumenta tier, e transformador não aumenta a reserva EU. Remover armazenamento descarta a energia acima da nova capacidade. Se uma ação custar mais do que toda a capacidade disponível, ela não poderá ocorrer até ajustar upgrades/capacidade.

<a id="classificador"></a>
## 8. Filtros e Classificador de Sementes

![Interface do classificador](images/classificador.png)

### Configurar os critérios

O classificador e o avançado usam limites inclusivos entre **0 e 31** para **crescimento**, **ganho** e **resistência**. Inicialmente, cada atributo aceita a faixa inteira e qualquer espécie.

1. Ajuste os mínimos e máximos pelos botões **− / +**.
2. Para limitar por espécie, pegue uma semente **completamente analisada** no cursor.
3. Clique em **Usar amostra**. A amostra não é consumida; o botão copia somente a identidade da espécie, sem preencher os intervalos genéticos.
4. Use **X** para limpar apenas a restrição de espécie. Os limites numéricos permanecem.

Uma semente/crop precisa estar no **nível 4 de análise do IC2** para a comparação genética. As demais entram na categoria não analisada. A análise deve ser feita pelos recursos do IC2; nenhuma ferramenta deste addon revela genética desconhecida.

Exemplo: para aceitar uma espécie com crescimento 10–31 e ganho 20–31, defina essas faixas, mantenha a resistência na faixa desejada e use a espécie como amostra. Os valores 10 e 20 também são aceitos, pois os limites são inclusivos.

### Separação física

O Classificador possui **36 slots**: nove entradas e três grupos de nove saídas. Processa uma semente por tentativa a cada **cinco ticks**, consumindo **20 EU por semente**. Em condições ideais a 20 TPS, são até quatro sementes por segundo.

Ele aceita itens que implementam a interface de sementes `ICropSeed`. Preserva a identidade, genética e NBT de cada semente: apenas transfere uma unidade para a saída correspondente.

| Destino | Condição | Face de extração |
|---|---|---|
| Aprovadas | Analisadas e dentro de todos os critérios | Frente |
| Restantes | Analisadas, mas fora de algum critério | Traseira |
| Não analisadas | Análise incompleta | Base |

**A entrada automatizada fica no topo e nas duas laterais que não são frente/traseira.** A orientação é relativa à frente do bloco. Não conecte a alimentação de sementes à frente ou à traseira.

Sem energia ou sem espaço na saída adequada, a semente não é transferida nem consome EU. Uma categoria cheia não vira autorização para enviar sementes à categoria errada; outras entradas com destino livre ainda podem ser processadas.

<a id="monitor"></a>
## 9. Monitor Agrícola e alertas

![Monitor com painel de dados](images/monitor-dados.png)

O monitor consulta um Manager básico ou avançado, sem gastar EU. Exibe estado, EU, água e Weed-Ex, com barras e valores atuais/capacidade. Dados de crops e proporção de maduras dependem de um relatório disponível do **avançado**.

### Vincular

1. Segure a **Sonda Agrícola**.
2. Agache e clique no Manager desejado.
3. Clique no Monitor Agrícola com a mesma sonda.
4. Mantenha monitor e Manager na **mesma dimensão**, a até **32 blocos de distância**.

O vínculo fica salvo no monitor. A atualização ocorre a cada **20 ticks**, aproximadamente um segundo a 20 TPS. A distância considera a separação espacial entre os blocos, incluindo a altura.

O monitor não mantém chunks carregados. Alvo descarregado, removido, distante ou em outra dimensão deixa a conexão inválida e limpa os valores anteriores, em vez de apresentá-los como atuais.

### Alertas e redstone

![Aba de alertas do monitor](images/monitor-alertas.png)

A aba **Alertas** permite ativar ou desativar individualmente seis categorias:

| Alerta | O que observa |
|---|---|
| Pouca EU | Insuficiência de energia informada pelo Manager |
| Adubo | Falta de fertilizante com adubação habilitada |
| Água | Falta de água com hidratação habilitada |
| Weed-Ex | Falta de Weed-Ex com esse cuidado habilitado |
| Saída cheia | Condição de saída bloqueada informada pelo Manager |
| Sem vínculo | Vínculo configurado que se tornou inválido |

Todos começam habilitados, e a seleção fica salva no bloco. Quando algum alerta selecionado está ativo, o visor fica vermelho e o monitor emite **redstone fraca de intensidade 15** nas faces.

Um monitor que nunca recebeu vínculo não alarma por isso. Já um vínculo configurado, mas perdido por distância, dimensão, remoção ou chunk descarregado, pode disparar o alerta correspondente.

Use a saída em uma lâmpada ou circuito de aviso. Monte a fiação de modo que ela não alimente involuntariamente um Manager configurado para controle por redstone.

<a id="ferramentas"></a>
## 10. Sonda e Cartão de Configuração

### Sonda Agrícola

![Leitura de uma crop com a sonda](images/sonda.png)

Clique em uma crop IC2 para consultar tamanho/estágio, recursos armazenados, luz, umidade, nutrientes do terreno e qualidade do ar. Genética só aparece com análise completa; a identidade da espécie também respeita o nível de análise já existente.

A sonda não analisa, não altera a planta e não gasta carga nem durabilidade. Sua segunda função é registrar um Manager e vinculá-lo ao monitor, conforme o procedimento anterior.

### Cartão de Configuração

1. **Agachar + clicar** em um Manager copia as configurações para o cartão.
2. **Clicar sem agachar** em outro Manager aplica as configurações armazenadas.

| Campo | Copiado? |
|---|---|
| Quatro funções de cuidado/colheita | Sim |
| Modo de redstone | Sim |
| Filtro de espécie e genética | Entre Managers avançados |
| Área, raio e camadas | Não |
| Itens, upgrades, EU e fluidos | Não |
| Vínculo do monitor | Não |

Um cartão de avançado aplicado no básico transfere apenas os campos comuns e avisa no chat. Um cartão copiado do básico não contém filtro para substituir o filtro de um avançado. O cartão é para Managers; não configura o Classificador.

<a id="composteira"></a>
## 11. Composteira e Composto Orgânico

![Interface da composteira elétrica](images/composteira.png)

| Parâmetro | Valor |
|---|---:|
| Entrada por lote | 8 itens vegetais iguais no slot de entrada |
| Produto | 1 Composto Orgânico |
| Duração | 200 ticks; 10 segundos a 20 TPS |
| Consumo enquanto processa | 5 EU/t |
| EU total por lote | 1.000 EU |
| Reserva / tier | 20.000 EU / tier 1 |
| Upgrades | Nenhum |

### Materiais aceitos

Somente os itens vanilla explicitamente aceitos:

- Trigo e sementes de trigo, abóbora, melancia e beterraba.
- Cenoura, batata, batata venenosa e beterraba.
- Cana-de-açúcar, maçã e fatia de melancia.
- Folhas, mudas, vinhas, grama alta e cacto.

Sacos de sementes IC2, produtos minerais e vegetais de outros mods não entram automaticamente nessa lista. O teste é pelo item registrado; não é uma regra universal para qualquer alimento ou item vegetal.

### Operação e automação

Insira vegetais pelo topo ou pelas laterais e retire composto pela **base**. A GUI também permite manipulação manual.

Sem energia, com menos de oito itens ou sem espaço na saída, o processo pausa sem consumir o lote. O progresso e a EU são salvos ao salvar o mundo. Ao quebrar a máquina, os itens são devolvidos, mas progresso e energia são perdidos.

### Transformar composto em fertilizante

Na bancada de crafting, misture **quatro Compostos Orgânicos + uma farinha de osso** para obter **quatro fertilizantes IC2**. A receita não tem formato.

O composto é um ingrediente; para abastecer o slot de adubo do Manager, transforme-o primeiro em fertilizante IC2.

<a id="bancada"></a>
## 12. Bancada de Sementes

![Comparação entre sementes A e B](images/bancada.png)

Coloque um saco de sementes IC2 no slot **A** e outro no **B**. A tabela mostra crescimento, ganho e resistência de cada semente, com a diferença **B − A**.

Exemplo: crescimento 20 em A e 24 em B resulta em **+4**. Resistência 5 em A e 3 em B resulta em **−2**. A diferença é numérica; resistência mais alta não deve ser interpretada automaticamente como melhor.

Dados desconhecidos continuam aparecendo como **?**. A bancada não analisa, cruza, melhora nem consome sementes. Não utiliza EU, não aceita upgrades e não oferece automação de inventário.

Para registrar uma semente completamente analisada, mantenha um **Caderno Agrícola** na mão principal ou secundária e clique em **Anotar A** ou **Anotar B**.

<a id="caderno"></a>
## 13. Caderno Agrícola

![Página do caderno agrícola](images/caderno.png)

Cada caderno armazena até **256 espécies únicas**, com os registros salvos no próprio item. Ele é um catálogo de espécies, não um arquivo separado de cada combinação genética de sementes.

### Registrar uma espécie

Escolha um dos métodos:

1. Use o caderno com botão direito em uma **crop completamente analisada**.
2. Segure uma **semente completamente analisada na outra mão** e clique com o caderno no ar.
3. Na Bancada de Sementes, mantenha o caderno em uma das mãos e clique em **Anotar A/B**.

Os métodos preservam a semente. Repetir uma espécie atualiza a página existente, sem ocupar uma nova vaga.

### Consultar

Clique com o caderno no ar e use as setas para mudar de página. O registro mostra espécie, tier, estágios, atributos e descrições fornecidos pela API do crop.

A **profundidade das raízes** só é registrada ao observar uma planta real. Se a espécie for atualizada depois por uma semente, a informação de raízes previamente obtida é preservada.

Requisitos ambientais variam por espécie. O caderno não deduz limites universais nem substitui a leitura da Sonda Agrícola. Os textos de crops externos dependem do mod que os fornece.

<a id="receitas"></a>
## 14. Todas as receitas

Cada grade abaixo representa a bancada 3×3; **·** indica uma posição vazia. As receitas com formato usam os ingredientes e posições registrados pelo addon. Scripts de modpacks podem alterar as receitas disponíveis na instância.

### Crop Manager básico

```text
H C M
```

**H:** Crop Harvester IC2 · **C:** Circuito Eletrônico Avançado IC2 · **M:** Crop-Matron IC2. Receita de uma linha horizontal, com espelhamento horizontal padrão.

### Crop Manager Avançado

```text
A C A
E M E
A C A
```

**A:** liga avançada IC2, quatro unidades · **C:** circuito avançado IC2, duas unidades · **E:** cristal de energia IC2, duas unidades · **M:** Manager básico.

### Classificador de Sementes

```text
I C I
H B H
I C I
```

**I:** lingote de ferro, quatro · **C:** circuito eletrônico IC2, dois · **H:** funil, dois · **B:** baú, um.

### Monitor Agrícola

```text
G G G
C R C
I I I
```

**G:** bloco de vidro, três · **C:** circuito eletrônico IC2, dois · **R:** redstone, uma · **I:** lingote de ferro, três.

### Sonda Agrícola

```text
· G ·
I C I
· R ·
```

**G:** bloco de vidro · **I:** lingote de ferro, dois · **C:** circuito eletrônico IC2 · **R:** redstone.

### Cartão de Configuração

```text
· P ·
R C P
· P ·
```

**P:** papel, três · **R:** redstone · **C:** circuito eletrônico IC2.

### Composteira Elétrica

```text
I I I
F C F
I I I
```

**I:** lingote de ferro, seis · **F:** fornalha, duas · **C:** circuito eletrônico IC2.

### Bancada de Sementes

```text
G G G
I B I
I I I
```

**G:** bloco de vidro, três · **I:** lingote de ferro, cinco · **B:** livro.

### Receitas sem formato e processamento

| Resultado | Ingredientes / processo |
|---|---|
| 1 Caderno Agrícola | 1 livro + 1 papel + 1 bolsa de tinta preta; crafting sem formato |
| 4 fertilizantes IC2 | 4 Compostos Orgânicos + 1 farinha de osso; crafting sem formato |
| 1 Composto Orgânico | 8 vegetais iguais na Composteira; 200 ticks e 1.000 EU |

As máquinas e ferramentas fabricadas nas grades acima produzem uma unidade por receita. Os ingredientes IC2 são resolvidos pela API do mod, sem depender de IDs numéricos globais de registro.

<a id="automacao"></a>
## 15. Automação e exemplos de montagem

### Faces dos Managers

| Recurso | Entrada | Saída |
|---|---|---|
| EU | Seis faces | Nenhuma saída EU |
| Fertilizante e recipientes de abastecimento | Topo e quatro laterais | Sem extração por essas faces |
| Produtos e recipientes resultantes | Inserção proibida | Base |
| Água e Weed-Ex | Seis faces, respeitando o fluido do tanque | Retirada também nas seis faces |
| Upgrades | Interface manual | Interface manual |

O acesso de itens sem face segue a regra de entrada do topo. Os conectores desenhados na textura são elementos visuais; use as regras da tabela para planejar tubos e funis.

### Exemplo: fazenda com saída automática

```text
             Fertilizante / recipientes
                        ↓
Fonte EU ──────── [ Crop Manager ] ← água / Weed-Ex
                        ↓
                 Extração pela base
                        ↓
                 Baú de produtos
```

Instale o Manager no alcance das crops e configure o extrator do seu mod de transporte para retirar pela base. O addon expõe inventários por face; a configuração de puxar ou empurrar itens depende do transporte utilizado.

### Exemplo: seleção de sementes

```text
Sementes → topo ou lateral livre do Classificador
                         ├─ frente → aprovadas
                         ├─ trás   → fora do filtro
                         └─ base   → não analisadas
```

Deixe os três destinos com espaço. Faça a análise das sementes por meios do IC2 antes de esperar classificação genética.

### Exemplo: reaproveitamento de vegetais

```text
Vegetais iguais → Composteira → Composto
                                     + farinha de osso
                                     ↓ crafting
                                Fertilizante IC2
                                     ↓
                                  Manager
```

O crafting intermediário não é feito pela Composteira: use uma bancada ou um sistema de crafting externo.

<a id="configuracao"></a>
## 16. Configuração do servidor

O arquivo gerado é **`config/cropmanager.cfg`**, na instância/servidor. Edite com o jogo ou servidor fechado. Clientes exibem os valores informados pelo servidor.

| Categoria / chave | Padrão | Intervalo | Efeito |
|---|---:|---|---|
| processing/baseIntervalTicks | 2 | 1–20 | Intervalo base de busca do básico |
| processing/maxPositionsPerTick | 8 | 1–32 | Teto de posições por tick |
| area/maxRadius | 8 | 1–8 | Raio máximo permitido |
| area/defaultRadius | 4 | 1–maxRadius | Raio inicial de novas máquinas |
| area/defaultLayers | 7 | 1–7 | Máscara inicial de camadas |
| energy/scanEU | 1 | 1–1.000 | Custo de consulta por posição carregada |
| energy/careEU | 10 | 1–1.000 | Custo por cuidado bem-sucedido |
| energy/harvestEU | 20 | 1–1.000 | Custo por colheita bem-sucedida |

### Máscara de camadas

Some os valores das alturas desejadas:

| Valor | Altura |
|---:|---|
| 1 | Um bloco abaixo da máquina |
| 2 | Mesma altura da máquina |
| 4 | Um bloco acima da máquina |

Exemplos: **2** = somente mesma altura; **4** = somente acima; **5** = abaixo e acima; **7** = todas as três.

Alterar os padrões de área afeta máquinas novas. As existentes preservam a área salva; reduzir o limite global ajusta o raio salvo ao recarregar. O limite de raio 8 permite no máximo uma área horizontal de **17×17**.

Custos de energia não finitos são reparados ao carregar. Esta versão não expõe parâmetros próprios de configuração para mudar a receita ou a duração da Composteira.

<a id="persistencia"></a>
## 17. Salvamento, quebra e multiplayer

### Salvar e carregar

Os Managers salvam energia, slots, tanques, configurações, cursor/progresso de varredura e a fila de produtos pendentes. O relatório do avançado precisa de nova coleta após recarregar.

A Composteira salva inventário, energia e progresso. O Classificador salva inventário, energia e filtros. O monitor salva vínculo e seleção de alertas. O caderno guarda seus registros no próprio item.

### Quebrar a máquina

Na quebra normal dos Managers, itens dos slots e produtos pendentes são entregues uma vez. **Energia e fluidos são descartados**. O bloco recolocado começa vazio, sem transportar o estado anterior no item.

A Composteira devolve os itens e perde energia/progresso. O addon não oferece remoção especial de estado por chave IC2. Não planeje uma mudança de lugar supondo que tanques e bateria serão preservados.

### Multiplayer e máquinas sobrepostas

Só o servidor processa plantas e aplica configurações. As ações de GUI exigem um container válido, a mesma dimensão e proximidade da máquina. Cliente e servidor precisam usar **0.3.0-alpha1**.

Managers sobrepostos podem atender a mesma planta em momentos diferentes, seguindo as regras normais do IC2. Não há coordenação global nem exclusividade por crop. Comece com uma máquina por área para facilitar diagnóstico e comparação de consumo.

<a id="problemas"></a>
## 18. Problemas frequentes

| Sintoma | Verifique / como resolver |
|---|---|
| Máquina não faz nada | EU suficiente, funções ligadas, modo de redstone, crop IC2 válida e área correta |
| Tem EU, mas uma ação não acontece | Com upgrades, o custo dessa ação pode superar a EU disponível ou até a capacidade total |
| Apenas uma planta demora a ser atendida | A máquina percorre posições em sequência; aguarde uma volta completa |
| A terra arada continua seca | O Manager hidrata entidades crop do IC2; não irriga farmland vanilla |
| Colheita parou | Esvazie produtos e recipientes; a fila pendente bloqueia novas colheitas |
| Água ausente impede tudo? | Cada cuidado é independente; confira energia, colheita e espaço separadamente |
| Avançado gasta EU com cuidados desligados | O diagnóstico continua varrendo. Use o controle por redstone para pausar |
| Filtro não impede colheita | É o comportamento esperado: seleção do avançado é informativa |
| Sementes vão para não analisadas | Complete o nível 4 de análise do IC2 |
| Amostra não é aceita | A semente precisa estar completamente analisada e no cursor da GUI |
| Classificador não aceita entrada | Use topo ou lateral que não seja frente/trás; confira se o item implementa ICropSeed |
| Uma saída do Classificador travou | Libere espaço na categoria correta; não há redirecionamento para outra categoria |
| Monitor sem dados de plantas | Vincule a um avançado e aguarde relatório completo com energia e sem pausa |
| Monitor perdeu conexão | Confira dimensão, distância de até 32 blocos, existência do Manager e chunk carregado |
| Monitor acusa insumo que não uso | Desative esse cuidado no Manager ou a categoria de alerta no monitor |
| Composteira não trabalha | Use oito itens iguais aceitos, mantenha EU e espaço na saída |
| Composto não entra no adubo | Fabrique fertilizante IC2 usando quatro compostos e uma farinha de osso |
| Bancada mostra ? | O equipamento respeita análise incompleta e não revela dados desconhecidos |
| Caderno não registra | Exige análise completa; confira o limite de 256 espécies e o método de uso |
| GUI não cabe na janela | Reduza a escala da GUI; avançado, classificador e monitor usam 342 pixels de largura com painel aberto |
| Interface está em inglês | Selecione Português (Brasil) nas opções de idioma do Minecraft |
| Mais overclock não dá velocidade | O teto do servidor pode ter sido atingido, mas o multiplicador de custo continua aumentando |
| Servidor recusa conexão | Confira versão idêntica do addon nos dois lados e remova JARs antigos duplicados |
| Receita difere no modpack | Scripts do pack podem modificar receitas; consulte o visualizador de receitas da instância |

### Perguntas rápidas

**Adiciona crops novas?** Não. A expansão 0.3.0 acrescenta equipamentos e ferramentas.

**Substitui o Crop Harvester ou Crop-Matron?** Não. As máquinas originais continuam disponíveis e entram na receita do básico.

**Acelera crescimento diretamente?** Não. Automatiza cuidados nativos e colheita; a velocidade de varredura é diferente da velocidade de crescimento da planta.

**Faz cruzamento ou replantio?** Não. A Bancada compara e o Classificador separa sementes; nenhum deles cruza ou melhora genética.

**Remove ervas daninhas por seleção?** Não. O diagnóstico informa a condição, e o filtro não arranca plantas.

**Funciona longe com chunks descarregados?** Não. Manager e monitor não carregam chunks.

**A sonda substitui a análise do IC2?** Não. Ela respeita o conhecimento já obtido sobre a crop.

**É compatível com qualquer versão de IC2, Cleanroom e E2EE?** Não existe essa garantia. Use a dependência e o ambiente documentados e teste outras combinações separadamente.

<a id="fontes"></a>
## 19. Limites, validação e fontes

Esta wiki foi preparada a partir do **código local da 0.3.0-alpha1**, das receitas registradas, das traduções e da documentação do projeto. Em divergências com trechos históricos, a implementação atual foi usada como referência — especialmente na ausência de ajustes de área na GUI e nas faces de entrada do Classificador.

As imagens são capturas reais já existentes no projeto, com exemplos de interfaces e estados. Não foram criadas telas fictícias. Algumas capturas dos recursos anteriores foram feitas antes da expansão 0.3.0; a montagem geral mostra também os equipamentos novos.

A validação documentada da expansão 0.3.0 registra **19 testes JUnit** e **94 verificações em mundo isolado Cleanroom**, além de capturas e verificações de GUI/rede em cliente de desenvolvimento. Esses números são os resultados previamente registrados no projeto, não uma nova execução feita para escrever esta wiki.

Permanecem fora dessa garantia: toda a instância E2EE com seus scripts e mods, sessão real com dois jogadores, desempenho de fazendas grandes e compatibilidade universal futura. Salvamentos de máquina e planta não formam uma transação atômica contra encerramentos abruptos.

### Referências do projeto

- [README principal](../../README.md): instalação, recursos e compilação.
- [Configuração e regras](../CONFIGURACAO.md): detalhes técnicos e histórico de balanceamento.
- [Validação](../VALIDACAO.md): testes, evidências e limitações por revisão.
- [Registro de conteúdo e receitas](../../src/main/java/br/cropmanager/CropManager.java).
- [Lógica do Manager](../../src/main/java/br/cropmanager/ManagerTile.java).
- [Filtros](../../src/main/java/br/cropmanager/SeedFilter.java) e [Classificador](../../src/main/java/br/cropmanager/ClassifierTile.java).
- [Monitor](../../src/main/java/br/cropmanager/MonitorTile.java) e [ferramentas](../../src/main/java/br/cropmanager/FarmTool.java).
- [Composteira](../../src/main/java/br/cropmanager/ComposterTile.java), [Bancada](../../src/main/java/br/cropmanager/SeedWorkbenchTile.java) e [Caderno](../../src/main/java/br/cropmanager/NotebookItem.java).
- [Parâmetros do servidor](../../src/main/java/br/cropmanager/ManagerConfig.java).

**Referência da wiki:** 14 de novembro de 2026. Para compartilhar a versão ilustrada em Markdown, leve este arquivo junto com a pasta `images`. Os links de código e documentação exigem o restante do projeto.
