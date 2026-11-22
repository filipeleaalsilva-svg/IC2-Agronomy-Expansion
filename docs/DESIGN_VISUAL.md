# Padrão visual IC2 — revisão 0.5.0-alpha2

## Padrão comum

A revisão preserva a carcaça cinza, slots, pixel art e ícone azul de upgrades do IC2. Os botões passam a usar cinza com relevo discreto, altura padrão de 14px e largura medida pelo maior rótulo daquele controle, com 12px de respiro. `+`, `−` e setas usam 14×14px. Margens de 8px, intervalos de 4px entre controles e 8px entre grupos organizam as telas. As posições dos slots e a janela de rede permanecem estáveis.

Títulos usam negrito; texto normal mantém a fonte legível do Minecraft. Cores semânticas distinguem habilitado, atenção e bloqueio, acompanhadas de rótulos ou símbolos. Botões selecionados têm indicação discreta em vez de `[x]`; hover não pisca nem muda a largura. A paleta acompanha o contraste do fundo IC2 carregado.

## Dados e hierarquia

Líquidos usam notação compacta em mB, como `Água ~12,6k / 20k mB`. O separador acompanha português/inglês; `~` informa arredondamento e o tooltip apresenta os valores exatos. Fertilizante mantém ícone e quantidade. Custos, capacidade técnica, coordenadas e explicações longas vão para tooltips, sem alterar os números usados pela lógica.

O monitor usa abas de Culturas, Ambiente, Controle e Automação. O Manager reúne Culturas e Ambiente dentro de Dados. A lista de culturas tem quatro entradas por página, ícone nativo de semente, nome e quantidade plantada. Identificação respeita o nível de análise; não analisadas ficam agrupadas sem revelar espécie ou genética. Ícones são apenas informativos, não slots de retirada. Não são inventadas texturas quando o IC2 reutiliza um ícone entre espécies.

A GUI atual do monitor mostra água e Weed-Ex do alvo em tanques verticais, barras de EU e buffer local nos tooltips. Quatro slots aceitam upgrades Wireless Range e três aceitam fertilizante para envio automático; a aba de controle contém o botão ligar/desligar e a aba Auto oferece limites para água, Weed-Ex, fertilizante, energia e saída cheia.

Ambiente mostra nutrientes do terreno, umidade, ventilação e luz. O resumo usa médias das plantas da área, excluindo ervas e posições vazias. A seleção de uma cultura permite mínimo/média/máximo por grupo. Não há avaliação universal de qualidade do solo. Dados ausentes ou levantamento incompleto têm mensagens curtas próprias.

Contagens de maduras, ervas, rejeitadas, cursor e idade numérica saem da apresentação. O processamento e os alertas internos continuam usando os dados necessários. A coleta usa a varredura existente e publica somente ciclos completos; não inicia buscas adicionais nem força carregamento de chunks.

## Conferência

A revisão deve ser conferida no cliente real nos dois idiomas, escalas 1 e 2, painéis recolhidos/expandidos e fundos claros/escuros. Capturas e resultados da alpha1 abaixo são históricos e não comprovam a aprovação visual da alpha2. O estado da nova verificação fica em `VALIDACAO.md`.

---

## Histórico: revisão 0.5.0-alpha1

O monitor mostra recursos em números absolutos e fertilizante com o ícone do item. As três abas compactas separam diagnóstico, controles remotos e regras de tanque. O Manager e o capinador possuem uma aba Área com raio, camadas e prévia local. Todos mantêm o ícone azul de informação e o painel recolhível.

O capinador tem modelo vanilla 3D com 32 elementos, chassi baixo, tanque octogonal e dentes. Texturas de 16×16 usam a paleta IC2; a variante ativa acende o motor e desloca o cabeçote. A colisão é composta por quatro volumes rotacionados com a orientação. Foi conferido no cliente real. O modelo-base e as referências estão em `artwork/biogas-weeder`; a variante ativa está nos recursos do mod.

Classificador, bancada e itens aposentados não possuem modelos distribuídos. Sua recuperação em mundos antigos ocorre por migração para conteúdo funcional, sem GUI legada.

## Base visual preservada da revisão 0.4.0

A revisão atual usa somente o cinza padrão do IC2. Tema escuro e troca de tema foram removidos. Fundo, slots e ícone azul são amostrados diretamente dos recursos originais do IC2; botões usam a textura vanilla utilizada pelo IC2. A seleção é indicada por uma linha discreta, sem fundo azul permanente.

GUIs de composteira, reservatório e coletor: 176×208px, inventário abaixo, indicadores compactos e upgrades da composteira em coluna à direita. O monitor usa a mesma base, com painel recolhível de 146px e modos de saída explícitos. O Manager preserva seus slots na base de 194px com controles reduzidos.

As fachadas novas reutilizam a carcaça e a paleta auditadas: dois reservatórios visíveis na estação de fluidos e uma boca de captação no coletor. Cada face/quadro tem 16×16 pixels reais, bordas nativas e animação somente do indicador. A importação é reproduzida por `tools/import_farm_atlas.py`.

As imagens de evidência são capturas do cliente real, nos dois idiomas e escalas 1 e 2. O Crop-Matron de referência é colocado pelo método original do IC2, com inicialização de rede normal. Classificador/bancada antigos só possuem interface de recuperação de inventário; não são conteúdo novo disponível.

---

Registro das revisões anteriores:

# Padrão visual IC2 — revisão 0.2.0

A referência principal é a pasta `textures` fornecida pelo usuário. Foram inspecionadas as carcaças do Macerator, Crop-Matron, Crop Harvester, Electric Furnace, Induction Furnace e Sorting Machine. As oito referências utilizadas pelo importador são idênticas às correspondentes do IC2 2.8.222-ex112 auditado.

## Regras aplicadas

- Carcaça cinza clara fria, com painéis subdivididos, bordas discretas e pequenos pontos de fixação; sem faixas azuis grandes nem moldura futurista escura.
- Base comum extraída da lateral/traseira do Macerator, preservada nos contornos de cada face. As fachadas mantêm a identificação de cada função.
- As referências fornecidas são 32×32. O resultado é 16×16 real por face/frame, redimensionado por vizinho mais próximo. Nenhuma interpolação ou cor intermediária é criada.
- Cada pixel das máquinas deve pertencer à paleta extraída das oito referências. A lista exata está em `artwork/ic2-palette.json`. As animações alteram somente o indicador e têm frames de 16×16.
- As GUIs usam diretamente `ic2:textures/gui/guicropmatron.png` para fundo e slots e `ic2:textures/gui/infobutton.png` para o ícone azul. Os botões usam a mesma textura vanilla utilizada por `ic2.core.gui.VanillaButton`, com bordas preservadas nas alturas compactas.
- Todas as GUIs têm o ícone azul no canto superior direito. Managers listam overclocker, armazenamento e transformador; classificador, monitor e sonda informam ausência de upgrades compatíveis. O tema escuro mantém a mesma estrutura e os mesmos controles.

## Reimportação e conferência

`py tools/import_farm_atlas.py` prioriza as referências de `textures/blocks/machine`; o JAR auditado em `libs/` é a alternativa para reconstruções do ZIP de fontes. O importador valida resolução, paleta, opacidade e contornos da carcaça. Os arquivos de referência fornecidos pelo usuário não são modificados.

A comparação visual é feita na fixture de cliente de desenvolvimento, ao lado de um Crop-Matron original. O JAR publicado não contém a fixture nem as classes de teste.

## Painel compacto

O Manager avançado e o classificador foram reduzidos de 374 para 342 pixels de largura. Os filtros ocupam 136 pixels: cada atributo mostra seus valores mínimo/máximo e controles de 12×14 na mesma linha, com espaçamento vertical de 22 pixels. Abas e ações têm 14 pixels de altura. A ajuda da amostra é um tooltip. Slots, funções, rede e temas foram preservados. Capturas de todas as GUIs estão em `evidence/compact-gui`.

A revisão vertical remove o fundo vazio abaixo dos painéis laterais: avançado 188px, classificador 148px e monitor 176px, preservando o inventário principal de 240px. Diagnóstico usa linhas de 10px e rótulos abreviados; filtros usam linhas de 18px e botões de 12px de altura.

## Recolhimento e monitor visual

As setas < / > recolhem os painéis laterais sem mover o inventário ou trocar o container. A aba e os filtros permanecem selecionados. O monitor usa a mesma largura de 342px dos outros painéis expandidos, com valores junto dos rótulos e barras de recursos/maturidade. Nomes, mensagens e tooltips têm versões completas en-US (principal/fallback) e pt-BR, seguindo o idioma do jogo.

## Expansão 0.3.0

Composteira e bancada compartilham a carcaça nativa e a paleta auditada. Fachada da composteira: janela orgânica e grelha; bancada: duas amostras sob visor. O indicador da composteira tem dois quadros de 16×16, sem interpolação. O monitor usa vermelho para alarme, incluindo o visor renderizado no mundo.

As duas novas GUIs de blocos têm 194×240 pixels, sem painel lateral desnecessário. A aba Alertas do monitor tem seis controles de 12px, com painel de 148px; Dados usa 184px para acomodar a barra de abas e avisos. O caderno usa 260×200px, paginação e limite visual de linhas, com texto completo ao passar o mouse nos atributos/descrição.

Os ícones de composto e caderno foram gerados como referência raster, convertidos para 16×16 com alpha e quantizados para a paleta. A fonte está em `artwork/compost-notebook-strip.png`; `tools/import_farm_atlas.py` reproduz os recursos. Capturas reais dos dois idiomas/temas e montagens estão em `evidence/expansion-0.3.0`.
