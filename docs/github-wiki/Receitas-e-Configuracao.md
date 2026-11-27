# Receitas e Configuração

Referência completa de receitas registradas em código (`CropManager.java` e `br.cropmanager.chest.crafting.*`) e parâmetros de `config/cropmanager.cfg`.

---

## 1. Receitas de Máquinas e Blocos

| Resultado | Grade (`3×3` ou menor) | Ingredientes |
|---|---|---|
| **Crop Manager Avançado** | `HIC / CMC / III` | `H`: Crop Harvester (IC2), `I`: Iridium Plate/Reinforced Iridium (IC2 `crafting:iridium`), `C`: Advanced Circuit (IC2), `M`: Crop-Matron (IC2) |
| **Monitor Agrícola** | `IGI / CIC / IRI` | `I`: `crafting:iridium` (IC2), `G`: Vidro, `C`: Advanced Circuit, `R`: Redstone |
| **Capinador a Biogás** | `IBI / FCF / III` | `I`: Barra de Ferro, `B`: Balde, `F`: Fornalha, `C`: Electronic Circuit (IC2) |
| **Composteira Manual** | `P P / PIP / ISI` | `P`: Tábuas (`plankWood`), `I`: Barra de Ferro, `S`: Laje de Madeira (`slabWood`) |
| **Composteira Elétrica** | `III / FCF / III` | `I`: Barra de Ferro, `F`: Fornalha, `C`: Electronic Circuit |
| **Moedor de Grin** | `III / ICI / IRI` | `I`: Barra de Ferro, `C`: Advanced Circuit, `R`: Redstone |
| **Extrator de Grin** | `IGI / FCF / IRI` | `I`: Barra de Ferro, `G`: Vidro, `F`: Fornalha, `C`: Electronic Circuit, `R`: Redstone |
| **Misturador de Weed-Ex** | `IGI / GCG / IRI` | `I`: Barra de Ferro, `G`: Vidro, `C`: Advanced Circuit, `R`: Redstone |
| **Analisador de Sementes** | `IGI / CAC / IRI` | `I`: Barra de Ferro, `G`: Vidro, `C`: Electronic Circuit, `A`: Cropnalyzer (IC2), `R`: Redstone |
| **Câmara Hidropônica** | `GGG / ICI / RBR` | `G`: Vidro de Estufa, `I`: Barra de Ferro, `C`: Advanced Circuit, `R`: Redstone, `B`: Balde de Água |
| **Regador Agrícola Mk1** | ` I  / IBI / RIR` | `I`: Barra de Ferro, `B`: Balde de Água, `R`: Redstone |
| **Regador Agrícola Mk2** | `G G / BMB / RCR` | `G`: Barra de Ouro, `B`: Balde de Água, `M`: Regador Mk1, `R`: Redstone, `C`: Electronic Circuit |
| **Regador Agrícola Mk3** | `D D / BMB / RCR` | `D`: Diamante, `B`: Balde de Água, `M`: Regador Mk2, `R`: Redstone, `C`: Advanced Circuit |
| **Regador Diagonal** | ` S  / SCS /  S ` | `S`: Regador Mk1, `C`: Electronic Circuit |
| **Pulverizador a Biogás** | `WSB` | `W`: Capinador a Biogás, `S`: Regador Mk1, `B`: Balde |
| **Gerador a Biogás Agrícola** | `GCI / II ` | `G`: Semifluid Generator (IC2), `C`: Electronic Circuit, `I`: Chapa de Ferro (`plate:iron` IC2) |
| **Carcaça do Gerador a Biogás** (`×4`) | `II / II` | `I`: Chapa de Ferro (`plate:iron` IC2) |
| **Queimador Térmico a Biogás** | `IFI /  T ` | `I`: Barra de Ferro, `F`: Fornalha, `T`: Tubo Térmico |
| **Tubo Térmico** (`×8`) | `III / ICI / III` | `I`: Barra de Ferro, `C`: Heat Conductor (IC2) |
| **Tubo de Fluidos** (`×8`) | `III / GCG / III` | `I`: Barra de Ferro, `G`: Vidro, `C`: Universal Fluid Cell (IC2) ou Balde |
| **Colmeia Polinizadora** | `PPP / FFF / PPP` | `P`: Tábuas (`plankWood`), `F`: Flor Amarela (Dandelion) |
| **Vidro de Estufa** (`×8`) | `GGG / GIG / GGG` | `G`: Vidro, `I`: Barra de Ferro |
| **Terra Radioativa** (`×2`) | `DCD / CUC / DCD` | `D`: Terra, `C`: Composto Orgânico, `U`: Crushed Uranium ou Uranium-238 (IC2) |
| **Reservatório Agrícola** | `GBG / BCB / III` | `G`: Vidro, `B`: Balde, `C`: Electronic Circuit, `I`: Barra de Ferro |
| **Carcaça do Reservatório** (`×8`) | `III / PPP / III` | `I`: Barra de Ferro, `P`: Tábuas (`plankWood`) |
| **Reservatório de Fluido Industrial** | `IGI / BGB / IRI` | `I`: Barra de Ferro, `G`: Vidro, `B`: Balde, `R`: Redstone |
| **Carcaça do Tanque Industrial** (`×8`) | `IGI / I I / IGI` | `I`: Barra de Ferro, `G`: Painel de Vidro |
| **Silo Agrícola de Grãos** | `IHI / ICI / IRI` | `I`: Barra de Ferro, `H`: Funil, `C`: Baú (`chestWood`), `R`: Redstone |
| **Carcaça de Silo Agrícola** (`×8`) | `IBI / IPI / IBI` | `I`: Barra de Ferro, `B`: Barras de Ferro, `P`: Tábuas (`plankWood`) |
| **Plug Wi-Fi de EU** | `ERE / ICI / IWI` | `E`: Ender Pearl, `R`: Redstone, `I`: Barra de Ferro, `C`: Advanced Circuit, `W`: Upgrade Wireless EU |
| **Ponto Wi-Fi de EU** | `IWI / ICI / ERE` | `I`: Barra de Ferro, `W`: Upgrade Wireless EU, `C`: Advanced Circuit, `E`: Ender Pearl, `R`: Redstone |
| **Antena Parabólica de EU** | `I I / RER / ICI` | `I`: Barra de Ferro, `R`: Redstone, `E`: Ender Pearl, `C`: Advanced Circuit |
| **Baú de Dados de Cultivos** | `PMP / ACA / WLX` | `P`: Steel Plate (IC2), `M`: MFSU (IC2), `A`: Advanced Circuit, `C`: Advanced Machine Casing (IC2), `W`: Universal Cell (Água), `L`: Lapotron Crystal **100% carregado**, `X`: Universal Cell (Weed-Ex) — as células vazias retornam |
| **Armação de Estufa** (`×8`) | `ISI / S S / ISI` | `I`: Barra de Ferro, `S`: Graveto (`stickWood`) |
| **Treliça de Jardim** (`×4`) | `S S / SSS / S S` | `S`: Graveto (`stickWood`) |
| **Lanterna Agrícola** (`×4`) | ` I  / IGI /  I ` | `I`: Barra de Ferro, `G`: Glowstone Dust |
| **Arandela / Lanterna Ind. / Luminária Vidro** | `ML` | `M`: Tábuas / Barras de Ferro / Vidro, `L`: Lanterna Agrícola |

---

## 2. Receitas de Itens, Ferramentas e Upgrades

| Resultado | Receita | Ingredientes |
|---|---|---|
| **Sonda Agrícola** | ` G  / ICI /  R ` | `G`: Vidro, `I`: Barra de Ferro, `C`: Electronic Circuit, `R`: Redstone |
| **Ferramenta de Cruzamento** | `SCC` | `S`: Tesoura, `C`: Crop Sticks (IC2) |
| **Lâmina de Moagem** | `II / FS /  I` | `I`: Barra de Ferro, `F`: Sílex (Flint), `S`: Graveto |
| **Manual Agrícola** | Sem forma (*Shapeless*) | `Livro` + (`Composto Orgânico` ou `Carcaça do Reservatório`) |
| **Crop Database Portátil** | `ICI / EBE / PXP` | `I`: `crafting:iridium`, `C`: Advanced Circuit, `E`: Electronic Circuit, `B`: Baú de Dados **vazio**, `P`: Chapa de Ferro, `X`: Energy Crystal **100% carregado** |
| **Vínculo do Portátil** | Sem forma (*Shapeless*) | `Crop Database Portátil` + `Upgrade Linker` vinculado (consome o Linker e preserva sementes) |
| **Upgrade Linker** | ` E  / ICI /  R ` | `E`: Ender Pearl, `I`: Barra de Ferro, `C`: Advanced Circuit, `R`: Redstone |
| **Upgrade Weed Killer** | `RGR / CEC / RGR` | `R`: Redstone, `G`: Glowstone Dust, `C`: Advanced Circuit, `E`: Overclocker Upgrade (IC2) |
| **Upgrade de Alcance Wireless** | ` E  / ECE /  R ` | `E`: Ender Pearl, `C`: Advanced Circuit, `R`: Redstone |
| **Upgrade de Transferência de Colheitas** | `IDI / CEC / IDI` | `I`: `crafting:iridium`, `D`: Diamante, `C`: Advanced Circuit, `E`: Ender Pearl |
| **Upgrade de Energia Wireless (EU)** | `ERE / ASA / ETE` | `E`: Ender Pearl, `R`: Redstone, `A`: Advanced Circuit, `S`: Energy Storage Upgrade, `T`: Transformer Upgrade |
| **Expansão de Capacidade** | Sem forma (*Shapeless*) | `Carcaça do Reservatório` + `2× Balde` + `Electronic Circuit` |
| **Bomba Reforçada** | Sem forma (*Shapeless*) | `Pistão` + `Balde` + `Tubo Térmico` + `Electronic Circuit` |
| **Válvula de Reserva** | Sem forma (*Shapeless*) | `Carcaça do Reservatório` + `Tubo Térmico` + `Tocha de Redstone` |
| **Fertilizante Catalítico** (`×2`) | ` C  / NGN /  C ` | `C`: Composto Orgânico, `N`: Essência Nitro, `G`: Grin Powder |
| **Pó Mutagênico** (`×2`) | ` G  / NPN /  G ` | `G`: Glowstone Dust, `N`: Essência Nitro, `P`: Grin Powder |
| **Plant Ball (IC2)** | `FF / FF` | `F`: Bio-Fibra |

---

## 3. Configuração do Servidor (`config/cropmanager.cfg`)

Edite com o jogo/servidor fechado. Os clientes recebem automaticamente os limites do servidor.

| Categoria / Chave | Padrão | Intervalo | Descrição |
|---|---:|---:|---|
| `processing/baseIntervalTicks` | `2` | `1–20` | Ticks por posição sem upgrades |
| `processing/maxPositionsPerTick` | `8` | `1–32` | Limite máximo de posições varridas por tick |
| `area/maxRadius` | `8` | `1–16` | Raio máximo permitido pelo servidor |
| `area/defaultRadius` | `4` | `1–maxRadius` | Raio inicial de novas máquinas |
| `area/defaultLayers` | `7` | `1–7` | Máscara de camadas (`1` abaixo, `2` nível, `4` acima) |
| `energy/scanEU` / `careEU` / `harvestEU` | `1` / `10` / `20` | `1–1.000` | Custo base de EU por leitura, cuidado e colheita |
| `advanced_manager/advancedEnergyCapacity` | `100.000` | `1.000–10M` | Buffer base de EU do Crop Manager Avançado |
| `advanced_manager/advancedTankCapacity` | `20.000` | `1.000–1M` | Capacidade dos tanques de Água e Weed-Ex (mB) |
| `advanced_manager/weedKillerCostMB` | `20` | `0–1.000` | Custo de Weed-Ex por erva removida |
| `monitor/baseRange` | `32` | `1–512` | Alcance base do Monitor em blocos |
| `monitor/wirelessEuRatePerUpgrade` | `256` | `1–100.000` | Taxa de transferência sem fio por upgrade (EU/t) |
| `weeder/gasCapacity` / `scansPerTick` | `16.000` / `2` | `1k–1M` / `1–32` | Tanque de Biogás e varreduras por tick do Capinador |
| `composter/requiredItems` / `waterPerRecipe` | `4` / `1.000` | `1–64` / `0–16k` | Receita da Composteira Elétrica |
| `thermal_pipe/maxHeatPerTick` | `100` | `1–100.000` | Limite de transferência térmica por tick (HU/t) |
| `database_chest/autoCollectRadius` | `3` | `1–16` | Raio de coleta magnética do Baú de Dados |
| `crops/enableCustomCrops` | `true` | `true/false` | Ativa o registro das 15 espécies de crops do mod |
| `crops/radbloomRadiationRadius` | `3.5` | `0–16` | Raio de radiação da Erva-Rádio madura |
| `hydroponic_chamber/durationTicks` / `euPerTick` | `200` / `16` | `10–72k` / `1–10k` | Duração base e consumo em EU/t da Câmara Hidropônica |
| `wifi_grid/channelBufferEU` / `crossDimLossPercent` | `100.000` / `0.0` | `1k–10M` / `0–50` | Buffer por canal Wi-Fi e perda interdimensional (%) |
| `dish_antenna/baseRadius` / `maxTransferPerTick` | `16` / `512` | `4–64` / `32–16.384` | Raio base e taxa máxima da Antena Parabólica |

---

## 4. Persistência, Quebra de Blocos e FAQ

* **Quebra de Máquinas:** inventários e filas de colheita pendentes são dropados no chão ao quebrar normalmente; energia e fluidos nos tanques internos são descartados.
* **Multiplayer:** toda a lógica roda no servidor. Clientes e servidor devem possuir exatamente a versão `0.5.0-alpha11`.
* **Por que o Crop Manager Avançado não aceita conduítes por baixo (`DOWN`)?** Para prevenir crashes no **Ender IO 5.5.x Cleanroom**, a capability de itens foi omitida na face inferior; use qualquer uma das outras 5 faces.
* **Como evitar desperdício de Weed-Ex?** Use o **Capinador a Biogás** separado ou configure as regras de parada com trava de histerese `100%` no **Monitor Agrícola**.
