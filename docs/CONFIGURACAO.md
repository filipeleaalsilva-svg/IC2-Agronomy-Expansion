# Configuração e regras

O servidor gera `config/cropmanager.cfg`. Edite com o jogo/servidor fechado. Alcance, camadas e controles são persistidos por máquina. A GUI ajusta os cuidados e redstone; a área usa os padrões do servidor ou o valor legado salvo. Clientes usam os limites informados pelo servidor.

Custos de energia `NaN` no arquivo são substituídos pelo padrão e reparados ao carregar. Energia não finita em NBT, que poderia ter sido causada por essa configuração na alpha1, é recuperada como zero.

| Categoria/chave | Padrão | Intervalo |
|---|---:|---:|
| processing/baseIntervalTicks | 2 | 1–20 |
| processing/maxPositionsPerTick | 8 | 1–32 |
| area/maxRadius | 8 | 1–8 |
| area/defaultRadius | 4 | 1–maxRadius |
| area/defaultLayers | 7 | 1–7 |
| energy/scanEU | 1 | 1–1.000 |
| energy/careEU | 10 | 1–1.000 |
| energy/harvestEU | 20 | 1–1.000 |

A máscara de camadas soma 1 para abaixo, 2 para mesma altura e 4 para acima. Exemplos: 2 = camada única da máquina; 4 = camada única acima; 5 = abaixo e acima; 7 = todas. Valores iniciais se aplicam a novas máquinas. Reduzir o limite global ajusta o raio salvo ao recarregar.

## Processamento e EU

A ordem por posição é fertilizar, hidratar, aplicar Weed-Ex e colher. Só são atendidas entidades crop reais do IC2 com uma planta definida. Não são hidratados blocos vanilla de terra arada.

Sem upgrades, uma posição a cada dois ticks. O acumulador fracionário permite intervalos menores que um tick com Overclockers. Limite absoluto de oito posições por tick no padrão, incluindo posições vazias e descarregadas; cada posição realiza no máximo quatro ações. Não se acumula uma rajada de trabalho durante falta de energia ou com todas as funções desligadas.

Uma volta percorre `(2 × raio + 1)² × número de camadas` posições. A 20 TPS: 9×9×3 leva 24,3 s, e 9×9×1 leva 8,1 s. Chunks descarregados são ignorados sem consulta de entidade ou cobrança, mas ocupam uma tentativa no limite de processamento. A máquina não os carrega.

Fórmula padrão por volta/trecho:

`EU = D × (P + 10 × C + 20 × H)`

`P` é o número de posições carregadas consultadas, `C` o número de cuidados bem-sucedidos e `H` o número de colheitas realizadas. Os três custos base são configuráveis. Não há consumo ocioso; há consumo de busca em posições vazias carregadas.

Exemplo sem upgrades: nove consultas com fertilização, água, Weed-Ex e uma colheita custam `9 + 30 + 20 = 59 EU`. Os cuidados reservam EU antes da chamada original; falha devolve a reserva. Água/Weed-Ex são drenados pelos métodos nativos, não por uma segunda cobrança de fluido.

## Upgrades

São aceitos somente os módulos nativos Overclocker, Energy Storage e Transformer. Seus fatores são consultados nas interfaces do IC2 fornecido, contando todos os itens dos quatro slots.

- `T = produto(multiplicador nativo de tempo ^ quantidade)`.
- `D = produto(multiplicador nativo de demanda ^ quantidade)`.
- `posições/tick = min(teto, 1 / (intervalo base × T))`.
- `capacidade EU = 20.000 + soma(armazenamento extra nativo × quantidade)`.
- `tier = 1 + soma(tiers extras nativos × quantidade)`.

No IC2 analisado, cada Energy Storage acrescenta 10.000 EU e cada Transformer acrescenta um tier. Não confundir tier de entrada com armazenamento. Ao remover armazenamento, energia acima da nova capacidade é descartada. O novo tier é comunicado à rede EU por remoção/reentrada da fonte receptora.

O custo **por consulta/ação** recebe `D`: é o balanceamento explícito desta máquina de varredura, não a energia total de uma receita de processamento comum do IC2. Overclockers continuam aumentando o custo mesmo quando a velocidade já alcançou o teto; evite excesso. Se uma ação custa mais que toda a capacidade, ela fica bloqueada por energia.

## Segurança dos recursos

A colheita é realizada somente no ponto ótimo ou máximo, pelo método original. Seus resultados são calculados uma única vez. Havendo sobra na saída, ela fica numa fila NBT persistente; nenhuma nova colheita começa enquanto essa fila não for esvaziada. O indicador de espaço considera essa condição. Falta de insumos não impede outras funções.

Energia, slots, tanques, configurações, cursor, progresso e fila são salvos. A quebra normal entrega itens dos slots e da fila uma vez; energia e fluidos são descartados. O bloco colocado depois começa vazio. A máquina não implementa armazenamento de estado no item nem remoção especial por chave IC2.

Como outros sistemas baseados em entidades de bloco, não existe transação atômica de disco entre a planta e a máquina. Uma interrupção abrupta durante salvamentos pode produzir estados inconsistentes. Isso não é apresentado como garantia de recuperação de crashes.

## Multiplayer e coexistência

Só o servidor processa plantas e aceita configurações. O cliente envia a ação do botão; o servidor aplica essa ação ao estado mais recente, preservando cliques rápidos e alterações independentes de dois jogadores. Pedidos exigem a mesma janela e dimensão, o container dessa máquina aberto, distância de até oito blocos e entidade válida. O resultado deve respeitar o raio permitido e manter uma camada selecionada. Energia, tanques e limites exibidos vêm do servidor; slots usam a sincronização do container Forge. Clientes e servidor devem usar a mesma versão 0.4.0-alpha1.

O Crop Manager não herda de Harvester/Crop-Matron e não usa seus hooks. O CropAccelerator alpha analisado transforma essas duas classes originais; não há registro do Crop Manager nele. Máquinas distintas sobrepostas podem atuar na mesma planta em momentos diferentes, pelas regras normais do IC2. Não existe exclusividade global por planta.
