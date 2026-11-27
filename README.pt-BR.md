[English](README.md) | **Português (PT-BR)** | [Wiki Oficial](https://github.com/filipeleaalsilva-svg/IC2-Agronomy-Expansion/wiki)

# IC2: Agronomy Expansion 0.5.0-alpha11

Addon para **Minecraft 1.12.2** e **IndustrialCraft 2 Experimental (`2.8.222-ex112`)**. Reúne automação agrícola avançada, monitoramento remoto, processamento de Weed-Ex e compostagem, análise e armazenamento indexado de sementes IC2, logística de fluidos/grãos em multibloco, polinização e uma linha própria de cultivos e fertilizantes.

---

## Início Rápido (Desenvolvimento e Testes)

O projeto usa **Gradle 9.7.0** com **RetroFuturaGradle 2.0.3** (alvo de bytecode Java 8). O Gradle 9.7 exige **Java 17 ou superior** para executar o daemon, enquanto o código do mod é compilado para Java 8.

Coloque o JAR auditado do IC2 em `libs/industrialcraft-2-2.8.222-ex112.jar` (o checksum SHA-256 é verificado pela task `verifyIc2`) e execute no PowerShell:

```powershell
# Aponte GRADLE_USER_HOME para o cache local se trabalhar offline
$env:GRADLE_USER_HOME = "$PWD\.gradle-home"

# Rodar a suíte de testes unitários offline
.\gradlew.bat test --offline -x downloadFernflower

# Compilar JAR principal e JAR de testes de runtime (Cleanroom)
.\gradlew.bat build reobfRuntimeTests
py tools\verify_artifact.py
```

---

## Instalação e Atualização

1. Copie **somente** `IC2-Agronomy-Expansion-0.5.0-alpha11.jar` para a pasta `mods/` (não instale o JAR de `runtime-tests`).
2. Cliente e servidor devem usar exatamente a mesma versão (`acceptableRemoteVersions = [0.5.0-alpha11]`).
3. **IndustrialCraft 2 Experimental (`2.8.222-ex112`)** é obrigatório (integração opcional com **HEI/JEI**).
4. A interface clara clássica do IC2 é o padrão do JAR. Para usar o tema escuro (`#1C1C1D`), coloque o Resource Pack opcional `IC2-Agronomy-Expansion-GUI-Dark-0.5.0-alpha11.zip` (`pack_format: 3`, 20 texturas de interface remapeadas) na pasta `resourcepacks/` — a cor do texto se adapta automaticamente à luminosidade do fundo carregado.

### Migração de Mundos Antigos
- Faça backup do mundo antes de atualizar versões alpha.
- O **Crop Manager básico** antigo migra automaticamente para o **Advanced Crop Manager** após o carregamento do chunk (até 32 migrações por tick via `LegacyMigration`), preservando inventário, EU, tanques, área e controles.
- Blocos legados removidos (`agricultural_collector`, `seed_classifier`, `seed_workbench`) são retirados de forma adiada via `LegacyRecoveryTile`, devolvendo itens ao chão; itens guardados em inventário são remapeados conforme registro do Forge (`MissingMappings`).
- O Crop-Matron e o Crop Harvester nativos do IC2 permanecem disponíveis.

---

## Blocos e Funcionalidades

### Crop Manager Avançado (`advanced_crop_manager`)
- **Receita:** `HIC / CMC / III` (Crop Harvester, Iridium, Circuito Avançado, Crop-Matron).
- **Especificações:** Tier 2, buffer base de `100.000 EU`, tanques de `20.000 mB` (água e Weed-Ex), 26 slots internos (fertilizantes, 15 saídas, 4 upgrades IC2 e recipientes de fluido).
- **Operação:** Varre área de `3×3` até `17×17` (raio `1..8`, configurável no servidor) em até 3 camadas verticais (`abaixo / nível / acima`) com deslocamento 3D configurável (`offsetX`, `offsetY`, `offsetZ`), nunca forçando carregamento de chunks.
- **Censo integrado:** A aba **Dados** consolida espécies plantadas (respeitando o nível de análise do IC2) e leituras reais de nutrientes, umidade, ventilação e luz ao fim de cada volta completa.
- **Automação:** Suporta entrada/saída via Capability (observação: na 0.5.0-alpha6+, `ITEM_HANDLER_CAPABILITY` é omitida na face inferior `DOWN` para evitar crash com conduítes do Ender IO 5.5.x Cleanroom).

### Sonda e Monitor Agrícola (`agricultural_probe`, `agricultural_monitor`)
- **Sonda:** Inspeciona cultivos IC2 (respeitando nível de análise) e vincula máquinas (`ManagerTile`, `WeederTile` ou `TileEntityCropmatron`) ao Monitor via *Agachar + Clique Direito*.
- **Monitor:** Alcance base de `32` blocos na mesma dimensão, dobrando a cada *Upgrade de Alcance Sem Fio* (até `512` blocos com 4 upgrades).
- **Recursos e Rede:** Buffer de `16.000 mB` de água e Weed-Ex, 3 slots de fertilizante, 1 slot de *Transferência de Colheita*, 1 slot de *Upgrade Wireless EU* (alimentando o alvo vinculado e máquinas de suporte em até `16` blocos) e saída redstone configurável (Desligada / Alarme / Normal / Teste 5s).
- **Regras de Parada (Aba Auto.):** Desliga o Manager remotamente por limites de água, Weed-Ex, fertilizante, energia ou saída cheia; disparos por Weed-Ex ativam trava de histerese até o tanque atingir `100%`.

### Capinador a Biogás (`biogas_weeder`)
- Bloco 3D dedicado sem consumo de EU; utiliza tanque de `16.000 mB` de **Biogás IC2**.
- Varre `2` posições por tick (`1 mB` por posição carregada examinada + `20 mB` por erva removida), removendo exclusivamente ervas daninhas reais (`Crops.weed`) sem destruir estacas ou cultivos de crescimento alto.
- Cada erva removida gera o item **Weed** do IC2 na saída dedicada da máquina. Se a saída estiver cheia, a erva fica no Crop Stick; itens Weed podem ser extraídos por conduítes e usados nas composteiras.

### Composteiras (Manual e Elétrica)
- **Composteira Manual (`manual_composter`):** Bloco de início de jogo sem energia; recebe `8` itens orgânicos por interação direta no mundo, acelera com água (`2x`) e gera **Adubo Orgânico** (`organic_compost`).
- **Composteira Elétrica (`electric_composter`):** Tier 1, buffer `20.000 EU`, tanque `16.000 mB` de água. Converte **`4` itens orgânicos + `1.000 mB` de água → `1` fertilizante IC2** em `200` ticks a `5 EU/t`, aceitando 4 upgrades IC2 e slot de bateria.

### Linha de Produção de Weed-Ex e Tubo Térmico
- **Moedor de Grin (`grin_grinder`):** Tier 1, `100` ticks a `5 EU/t`; tritura `1` olho de aranha em `1` Grin Powder utilizando uma **Lâmina de Moagem** reutilizável (`256` usos).
- **Extrator de Grin (`grin_extractor`):** Tier 1, `100` ticks a `5 EU/t`; combina `1` Grin Powder + `1.000 mB` de água → `1.000 mB` de **Extrato de Grin**.
- **Misturador de Weed-Ex (`weed_ex_fabricator`):** Tier 1, `100` ticks a `5 EU/t`; combina `1.000 mB` de Extrato de Grin + `1` redstone → `500 mB` de **Weed-Ex**.
- **Tubo Térmico (`thermal_pipe`):** Conecta fontes `IHeatSource` a máquinas compatíveis (até `100 HU/t`). Aceita wrench IC2 para desmontagem com *Agachar + Clique Direito*.

### Analisador, Armazenamento, Multiblocos e Cultivos Customizados
- **Analisador de Sementes (`seed_analyzer`):** Tier 1, `100` ticks a `5 EU/t`; analisa sacos de sementes até nível 4 e exibe genética e atributos na interface.
- **Baú de Dados de Cultivos (`crop_database_chest`) e Database Portátil (`portable_crop_database`):** Armazenamento de `500` sacos de sementes em grade paginada `5×5` com busca textual, ordenação (Tipo / Tier / Atributos), favoritos (sem alterar o NBT das sementes), coleta magnética configurável, coleta automática no inventário e vínculo remoto interdimensional via **Vinculador de Upgrade (`upgrade_linker`)**.
- **Regadores Agrícolas (`sprinkler_mk1`, `sprinkler_mk2`, `sprinkler_mk3`):** Blocos 3D sem EU que umedecem terra arada, hidratam estacas IC2 e aceleram crescimento (`9×9` em Mk1/Mk2, `17×17` em Mk3; velocidade `4×` em Mk2/Mk3), com 3 modos de redstone via *Agachar + Clique Direito*.
- **Reservatório Agrícola (`agricultural_reservoir` + `reservoir_casing`):** Multibloco `3×5×3` de dois tanques independentes com energia IC2, filtros de fluido, módulos de bomba/reserva e upgrades de capacidade.
- **Tanque Industrial de Fluido (`fluid_tank` + `fluid_tank_casing`):** Multibloco `3×5×3` sem EU para armazenamento em massa de um fluido (`128.000 mB` base + até 8 upgrades de `+32.000 mB`), portas configuráveis por face e auto-extração por redstone.
- **Silo Agrícola (`agricultural_silo` + `silo_casing`):** Multibloco `3×5×3` sem EU para armazenamento em massa de um tipo de item (`128.000` base + até 8 upgrades de `+32.000`), trava de template, portas configuráveis e auto-extração por redstone.
- **Tubo de Fluido (`fluid_pipe`):** Logística de fluidos sem energia com equalização automática entre tanques e distribuição round-robin entre vizinhos.
- **Colmeia Polinizadora (`pollinator_hive`) e Vidro de Estufa (`greenhouse_glass`):** A Colmeia acelera cruzamentos em estacas duplas próximas (com carga opcional de **Pó Mutagênico** para triplicar a cadência); o Vidro de Estufa concede bônus de crescimento por random tick ao cultivo diretamente abaixo sob luz adequada.
- **Câmara Hidropônica (`hydroponic_chamber`):** Máquina Tier 2 EU (`50.000 EU`, `16 EU/t`, tanque de `16.000 mB` de água) que cultiva Sacos de Sementes IC2 em ambiente fechado (`VirtualCropTile`). Exige `greenhouse_glass` acima e subsolo válido abaixo (`dirt`/`farmland`/`grass`, ou `radioactive_soil` para `radbloom`), com bônus opcionais de cúpula `3×3` (`+25%`) e calor IC2 adjacente (`+50%`).
- **Rede Wi-Fi de EU e Antena Parabólica (`eu_wifi_plug`, `eu_wifi_point`, `eu_dish_antenna`):** 1.000 canais públicos/privados (`100.000 EU` de buffer, saída selecionável `T1–T4` e controle redstone) e distribuição sem fio por área integrada ao `WirelessEuUpgrade`.
- **Ecossistema a Biogás (`biogas_generator` + `biogas_generator_casing`, `biogas_sprayer`, `biogas_heat_burner`):** Gerador multibloco `3×3×3` com modelo 3D OBJ/TESR, pulverizador agrícola sem EU e queimador térmico `IHeatSource` (`32 HU/t`).
- **Regador Diagonal (`sprinkler_diagonal`) e Ferramenta de Cruzamento (`crossing_tool`):** Regador de 4 quadrantes `9×9` diagonais selecionáveis com corredor seco em cruz, além de ferramenta portátil para colher e rearmar estacas duplas.
- **Decorativos e Iluminação (`greenhouse_frame`, `garden_trellis`, `farm_lantern`, `wooden_sconce`, `industrial_lantern`, `glass_lamp`):** Molduras de estufa, treliças e luminárias nível 15 para chão, parede ou teto.
- **Manual Agrícola (`agricultural_manual`) e Plugin HEI/JEI:** Guia ilustrado in-game com busca e índice por categorias, além de integração opcional com HEI/JEI para sementes IC2 e cultivos do mod.
- **Upgrade Wireless EU (`wireless_eu_upgrade`):** Implementa `IUpgradeItem` do IC2; vincula-se via *Agachar + Clique Direito* em armazenadores `IEnergyStorage` (`BatBox`, `CESU`, `MFE`, `MFSU`), canais Wi-Fi ou Antenas Parabólicas para alimentar qualquer máquina IC2 com slots de upgrade.
- **Cultivos e Fertilizantes:** Registra `15` espécies IC2 (`grinbloom`, `bioreed`, `nitrobulb`, `glowbloom`, `sulfurbud`, `lazuli_bloom`, `quartzreed`, `lithivine`, `irid_moss`, `radbloom` com `radioactive_soil`, além de `resincane`, `cryolotus`, `cupric_moss`, `stannum_herb` e `voltreed`), materiais (`bio_fiber`, `nitro_essence`, `mutagenic_powder`) e fertilizantes (`organic_compost`, `catalytic_fertilizer`).

---

## Visão Geral da Arquitetura

| Pacote | Responsabilidade |
|---|---|
| `br.cropmanager` | Registro central (`CropManager`), configuração (`ManagerConfig`), rede (`ManagerNetwork`), proxies (`CommonProxy`/`ClientProxy`), bases compartilhadas (`FarmTile`, `PoweredMachineTile`, `EquipmentLayout`, `StorageMultiblockSupport`), máquinas e TileEntities (`ManagerTile`, `MonitorTile`, `WeederTile`, `ComposterTile`, `GrinGrinderTile`, `GrinExtractorTile`, `WeedExFabricatorTile`, `SeedAnalyzerTile`, `HydroponicChamberTile`, `VirtualCropTile`, `ManualComposterTile`, `ThermalPipeTile`, `SprinklerTile`, `DiagonalSprinklerTile`, `PollinatorHiveTile`, `FluidPipeTile`, `AgriculturalReservoirTile`, `IndustrialTankTile`, `AgriculturalSiloTile`, `BiogasGeneratorTile`, `BiogasSprayerTile`, `BiogasHeatBurnerTile`, `EuWifiPlugTile`, `EuWifiPointTile`, `EuDishAntennaTile`, `WirelessEuGridData`), ferramentas (`CrossingTool`, `FarmTool`, `WirelessEuUpgrade`), lógica pura (`ScanLogic`, `AutomationRules`, `CropSurvey`, `ManagerSettings`, `AlarmLogic`, `EnergyWarning`, `CrossBreedingHelper`, `StorageMultiblock`) e GUIs/renderers. |
| `br.cropmanager.crop` | Registro (`CropRegistry`), classe base (`BaseCropCard`), `RadbloomCropCard` e mecânica de solo catalisado. |
| `br.cropmanager.chest.*` | Bloco, inventário unitário, item portátil, linker, rede, receitas, integração de busca/genética e interface do Baú de Dados de Cultivos (`500` slots). |
| `br.cropmanager.guide` | Manual Agrícola in-game (`ItemAgriculturalManual`, `GuiAgriculturalManual`, `ManualRegistry`, `ManualCategory`, `ManualEntry`). |
| `br.cropmanager.jei` | Integração opcional com HEI/JEI (`CropManagerJeiPlugin`, `CropRecipeCategory`, `CropRecipeWrapper`). |

---

## Configuração (`config/cropmanager.cfg`)

Principais chaves carregadas por `ManagerConfig`:

| Categoria | Chave | Padrão | Intervalo | Descrição |
|---|---|---:|---:|---|
| `processing` | `baseIntervalTicks` | `2` | `1–20` | Ticks por posição sem upgrades |
| `processing` | `maxPositionsPerTick` | `8` | `1–32` | Teto de posições varridas por tick |
| `area` | `maxRadius` | `8` | `1–16` | Raio horizontal máximo permitido pelo servidor |
| `area` | `defaultRadius` | `4` | `1–maxRadius` | Raio inicial de novas máquinas |
| `area` | `defaultLayers` | `7` | `1–7` | Máscara de camadas (`1` abaixo, `2` nível, `4` acima) |
| `energy` | `scanEU` / `careEU` / `harvestEU` | `1` / `10` / `20` | `1–1000` | Custo base de EU por leitura, cuidado e colheita |
| `advanced_manager` | `advancedEnergyCapacity` | `100000` | `1000–10M` | Buffer base de EU do Advanced Crop Manager |
| `advanced_manager` | `advancedTankCapacity` | `20000` | `1000–1M` | Capacidade dos tanques de água e Weed-Ex (mB) |
| `monitor` | `baseRange` | `32` | `1–512` | Alcance base do Monitor em blocos |
| `monitor` | `wirelessEuRatePerUpgrade` | `256` | `1–100000` | Taxa (EU/t) por upgrade Wireless EU |
| `weeder` | `gasCapacity` | `16000` | `1000–1M` | Capacidade do tanque de Biogás (mB) |
| `composter` | `requiredItems` / `waterPerRecipe` | `4` / `1000` | `1–64` / `0–16000` | Insumos orgânicos e água por fertilizante |
| `thermal_pipe` | `maxHeatPerTick` | `100` | `1–100000` | Limite de transferência térmica por tick (HU/t) |
| `database_chest` | `autoCollectRadius` | `3` | `1–16` | Raio de coleta magnética do Baú de Dados |
| `crops` | `enableCustomCrops` | `true` | `true/false` | Ativa o registro das espécies IC2 do mod |
| `hydroponic_chamber` | `durationTicks` / `euPerTick` | `200` / `16` | `10–72k` / `1–10k` | Duração base do ciclo e consumo em EU/t da Câmara Hidropônica |
| `wifi_grid` | `channelBufferEU` | `100000` | `1k–10M` | Buffer de suavização por canal Wi-Fi de EU |
| `dish_antenna` | `baseRadius` / `maxTransferPerTick` | `16` / `512` | `4–64` / `32–16k` | Raio base e taxa máxima de EU/t da Antena Parabólica |

Mais detalhes de cálculo em [docs/CONFIGURACAO.md](docs/CONFIGURACAO.md).

---

## Testes e Qualidade

- **`src/test/java/br/cropmanager/**`:** **290 testes unitários** distribuídos em **45 suítes** cobrindo lógica de varredura, regras de automação, persistência NBT, capabilities, integridade de assets/i18n (`796` chaves `en_us`/`pt_br`), Câmara Hidropônica, rede Wi-Fi de EU, Antena Parabólica, máquinas a Biogás, multiblocos, tubos de fluido, polinização, inventário do Baú de Dados, receitas e proteção contra duplicação.
- **`src/runtimeTest/java/br/cropmanager/**`:** Addon separado (`cropmanager_tests`, nunca empacotado no JAR final) para validação em runtime real no Cleanroom (`RuntimeChecks.java` e `ClientChecks.java`).

---

## Documentação Adicional

- [Relatório de Revisão de Código (`docs/REVISAO_CODIGO.md`)](docs/REVISAO_CODIGO.md) — auditoria detalhada nos 5 eixos (correção, legibilidade, arquitetura, segurança e desempenho) e achados por severidade.
- [Histórico de Versões (`CHANGELOG.md`)](CHANGELOG.md)
- [Configuração e Fórmulas (`docs/CONFIGURACAO.md`)](docs/CONFIGURACAO.md)
- [Guia de Validação Cleanroom (`docs/VALIDACAO.md`)](docs/VALIDACAO.md)
- [Verificação Técnica Anterior (`VERIFICACAO.md`)](VERIFICACAO.md)

---

## Licença

Distribuído sob a [Licença MIT](LICENSE).
