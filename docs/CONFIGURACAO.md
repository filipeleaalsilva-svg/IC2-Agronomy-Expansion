# Configuração e regras

O servidor gera `config/cropmanager.cfg`. Edite com o jogo/servidor fechado. Alcance, camadas e controles são persistidos por máquina. A GUI ajusta os cuidados e redstone; a área usa os padrões do servidor ou o valor legado salvo. Clientes usam os limites informados pelo servidor.

Custos de energia `NaN` no arquivo são substituídos pelo padrão e reparados ao carregar. Energia não finita em NBT, que poderia ter sido causada por essa configuração na alpha1, é recuperada como zero.

| Categoria/chave | Padrão | Intervalo |
|---|---:|---:|
| `processing/baseIntervalTicks` | `2` | `1–20` |
| `processing/maxPositionsPerTick` | `8` | `1–32` |
| `area/maxRadius` | `8` | `1–16` |
| `area/defaultRadius` | `4` | `1–maxRadius` |
| `area/defaultLayers` | `7` | `1–7` |
| `energy/scanEU` | `1` | `1–1.000` |
| `energy/careEU` | `10` | `1–1.000` |
| `energy/harvestEU` | `20` | `1–1.000` |
| `advanced_manager/basicEnergyCapacity` | `20.000` | `1.000–10M` |
| `advanced_manager/advancedEnergyCapacity` | `100.000` | `1.000–10M` |
| `advanced_manager/basicSinkTier` / `advancedSinkTier` | `1` / `2` | `1–8` |
| `advanced_manager/advancedTimeMultiplier` | `0.5` | `0.05–10.0` |
| `advanced_manager/basicTankCapacity` / `advancedTankCapacity` | `10.000` / `20.000` | `1.000–1M` |
| `advanced_manager/weedKillerCostMB` | `20` | `0–1.000` |
| `advanced_manager/flushPendingPerTick` | `15` | `1–64` |
| `monitor/baseRange` | `32` | `1–512` |
| `monitor/maxRangeUpgrades` | `4` | `0–4` |
| `monitor/tankCapacity` | `16.000` | `1.000–1M` |
| `monitor/fluidTransferPerSecond` | `1.000` | `1–100.000` |
| `monitor/fertilizerTransferPerSecond` | `16` | `1–192` |
| `monitor/harvestTransferPerSecond` | `64` | `1–960` |
| `monitor/wirelessEuCapacityPerUpgrade` | `50.000` | `1.000–10M` |
| `monitor/wirelessEuRatePerUpgrade` | `256` | `1–100.000` |
| `monitor/supportWirelessRadius` | `16` | `1–64` |
| `monitor/testSignalTicks` | `100` | `1–1.200` |
| `weeder/gasCapacity` | `16.000` | `1.000–1M` |
| `weeder/scansPerTick` | `2` | `1–32` |
| `weeder/scanGasCostMB` / `removeWeedGasCostMB` | `1` / `20` | `0–1.000` |
| `composter/requiredItems` / `waterPerRecipe` | `4` / `1.000` | `1–64` / `0–16.000` |
| `composter/tankCapacity` / `energyCapacity` | `16.000` / `20.000` | `1.000–1M` / `1.000–10M` |
| `composter/durationTicks` / `euPerTick` / `sinkTier` | `200` / `5` / `1` | `1–72.000` / `1–10.000` / `1–8` |
| `grin_grinder/durationTicks` / `euPerTick` / `energyCapacity` | `100` / `5` / `20.000` | `1–72.000` / `1–10.000` / `1.000–10M` |
| `grin_grinder/sinkTier` / `bladeDurability` | `1` / `256` | `1–8` / `1–100.000` |
| `grin_extractor/tankCapacity` / `waterPerRecipe` / `extractPerRecipe` | `16.000` / `1.000` / `1.000` | `1.000–1M` / `1–tank` |
| `grin_extractor/durationTicks` / `euPerTick` / `energyCapacity` / `sinkTier` | `100` / `5` / `20.000` / `1` | — |
| `weed_ex_fabricator/extractTankCapacity` / `weedTankCapacity` | `16.000` / `16.000` | `1.000–1M` |
| `weed_ex_fabricator/extractPerRecipe` / `weedExPerRecipe` | `1.000` / `500` | `1–tank` |
| `weed_ex_fabricator/durationTicks` / `euPerTick` / `energyCapacity` / `sinkTier` | `100` / `5` / `20.000` / `1` | — |
| `thermal_pipe/maxHeatPerTick` | `100` | `1–100.000` |
| `database_chest/autoCollectRadius` / `autoCollectIntervalTicks` / `autoCollectMaxItemsPerScan` | `3` / `10` / `32` | `1–16` / `1–200` / `1–256` |
| `manual_composter/requiredItems` / `durationTicks` / `waterSpeedMultiplier` / `outputCount` | `8` / `1.200` / `2.0` / `2` | `1–64` / `20–72.000` / `1–10` / `1–16` |
| `fertilizers/organicNutrients` / `organicWater` | `60` / `40` | `1–100` / `0–100` |
| `fertilizers/catalyticNutrients` / `catalyticGrowthBoost` / `catalyticCharges` | `100` / `40` / `3` | `1–100` / `0–500` / `1–16` |
| `fertilizers/mutagenicPowderBurstTicks` / `mutagenicStatBonusChance` | `12` / `0.5` | `1–64` / `0.0–1.0` |
| `seed_analyzer/durationTicks` / `euPerTick` / `energyCapacity` / `sinkTier` | `100` / `5` / `20.000` / `1` | — |
| `crops/enableCustomCrops` / `radbloomRadiationRadius` / `radbloomRadiationDuration` | `true` / `3.5` / `200` | `bool` / `0–16` / `0–2.400` |
| `sprinklers/mk1AndMk2Radius` / `mk3Radius` / `mk2AndMk3SpeedMultiplier` | `4` / `8` / `4` | `1–12` / `1–16` / `1–10` |
| `sprinklers/baseIntervalTicks` / `hydrationBonus` / `growthBonus` | `80` / `25` / `15` | `10–1.200` / `1–100` / `0–200` |
| `pollinator_hive/radius` / `baseIntervalTicks` / `chargesPerPowder` / `maxCharges` | `2` / `100` / `16` / `64` | `1–8` / `10–1.200` / `1–64` / `1–64` |
| `greenhouse/growthBonus` | `20` | `0–200` |
| `industrial_tank/baseCapacity` / `capacityPerUpgrade` / `autoEjectRateMB` | `128.000` / `32.000` / `2.000` | `16k–10M` / `1k–1M` / `100–100k` |
| `agricultural_silo/baseCapacity` / `capacityPerUpgrade` / `autoEjectRate` | `128.000` / `32.000` / `64` | `1k–10M` / `100–1M` / `1–10.000` |
| `fluid_pipe/transferMbPerPulse` / `intervalTicks` | `250` / `5` | `10–10.000` / `1–200` |
| `wifi_grid/channelBufferEU` / `plugSinkTier` / `pointMaxOutputTier` / `crossDimLossPercent` | `100.000` / `4` / `4` / `0.0` | `1k–10M` / `1–8` / `1–4` / `0–50` |
| `dish_antenna/baseRadius` / `radiusPerUpgrade` / `maxTransferPerTick` | `16` / `16` / `512` | `4–64` / `4–32` / `32–16.384` |
| `dish_antenna/energyCapacity` / `sinkTier` / `scanIntervalTicks` | `100.000` / `4` / `20` | `1k–10M` / `1–8` / `5–200` |
| `hydroponic_chamber/durationTicks` / `euPerTick` / `energyCapacity` / `sinkTier` | `200` / `16` / `50.000` / `2` | `10–72k` / `1–10k` / `1k–10M` / `1–8` |
| `hydroponic_chamber/tankCapacity` / `waterPerCycle` / `heatPerTick` | `16.000` / `500` / `20` | `1k–1M` / `0–tank` / `1–1.000` |

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

## Rede Wi-Fi de EU

- `wifi_grid.channelBufferEU` controla a capacidade de cada canal. O padrão é 100.000 EU.
- `wifi_grid.plugSinkTier` controla o Tier de entrada do Plug. O padrão é 4.
- `wifi_grid.pointMaxOutputTier` limita o Tier selecionável no Ponto de 1 a 4. O padrão é 4 e o limite também se aplica a Pontos já instalados.
- `wifi_grid.crossDimLossPercent` controla a perda entre dimensões, de 0% a 50%. O padrão é 0%.
- Uma Antena Parabólica pode alimentar máquinas no alcance com Upgrade Wireless EU sem vínculo. Agache e clique com o botão direito na antena com o upgrade para dedicá-lo a ela.

## Segurança dos recursos

A colheita é realizada somente no ponto ótimo ou máximo, pelo método original. Seus resultados são calculados uma única vez. Havendo sobra na saída, ela fica numa fila NBT persistente; nenhuma nova colheita começa enquanto essa fila não for esvaziada. O indicador de espaço considera essa condição. Falta de insumos não impede outras funções.

Energia, slots, tanques, configurações, cursor, progresso e fila são salvos. A quebra normal entrega itens dos slots e da fila uma vez; energia e fluidos são descartados. O bloco colocado depois começa vazio. A máquina não implementa armazenamento de estado no item nem remoção especial por chave IC2.

Como outros sistemas baseados em entidades de bloco, não existe transação atômica de disco entre a planta e a máquina. Uma interrupção abrupta durante salvamentos pode produzir estados inconsistentes. Isso não é apresentado como garantia de recuperação de crashes.

## Multiplayer e coexistência

Só o servidor processa plantas e aceita configurações. O cliente envia a ação do botão; o servidor aplica essa ação ao estado mais recente, preservando cliques rápidos e alterações independentes de dois jogadores. Pedidos exigem a mesma janela e dimensão, o container dessa máquina aberto, distância de até oito blocos e entidade válida. O resultado deve respeitar o raio permitido e manter uma camada selecionada. Energia, tanques e limites exibidos vêm do servidor; slots usam a sincronização do container Forge. Clientes e servidor devem usar exatamente a mesma versão (`0.5.0-alpha11`).

O Crop Manager não herda de Harvester/Crop-Matron e não usa seus hooks. O CropAccelerator alpha analisado transforma essas duas classes originais; não há registro do Crop Manager nele. Máquinas distintas sobrepostas podem atuar na mesma planta em momentos diferentes, pelas regras normais do IC2. Não existe exclusividade global por planta.
