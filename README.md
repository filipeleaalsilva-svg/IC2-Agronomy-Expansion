# Crop Manager 0.5.0-alpha9

Addon para **Minecraft 1.12.2** e **IndustrialCraft 2 Experimental (`2.8.222-ex112`)**. Reúne automação agrícola avançada, monitoramento remoto, processamento de Weed-Ex e compostagem, análise e armazenamento indexado de sementes IC2 e uma linha própria de cultivos e fertilizantes.

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

1. Copie **somente** `CropManager-0.5.0-alpha9.jar` para a pasta `mods/` (não instale o JAR de `runtime-tests`).
2. Cliente e servidor devem usar exatamente a mesma versão (`acceptableRemoteVersions = [0.5.0-alpha9]`).
3. **IndustrialCraft 2 Experimental (`2.8.222-ex112`)** é obrigatório.
4. Opcionalmente, ative o resource pack `CropManager-GUI-Dark-0.5.0-alpha9.zip` no cliente para interface em tema escuro (a cor do texto se adapta automaticamente à luminosidade do fundo carregado).

### Migração de Mundos Antigos
- Faça backup do mundo antes de atualizar versões alpha.
- O **Crop Manager básico** antigo migra automaticamente para o **Advanced Crop Manager** após o carregamento do chunk (até 32 migrações por tick via `LegacyMigration`), preservando inventário, EU, tanques, área e controles.
- Blocos legados removidos (`agricultural_collector`, `agricultural_reservoir`, `seed_classifier`, `seed_workbench`) são retirados de forma adiada via `LegacyRecoveryTile`, devolvendo itens ao chão; itens guardados em inventário são remapeados conforme registro do Forge (`MissingMappings`).
- O Crop-Matron e o Crop Harvester nativos do IC2 permanecem disponíveis.

---

## Blocos e Funcionalidades

### Crop Manager Avançado (`advanced_crop_manager`)
- **Receita:** `HIC / CMC / III` (Crop Harvester, Iridium, Circuito Avançado, Crop-Matron).
- **Especificações:** Tier 2, buffer base de `100.000 EU`, tanques de `20.000 mB` (água e Weed-Ex), 26 slots internos (fertilizantes, 15 saídas, 4 upgrades IC2 e recipientes de fluido).
- **Operação:** Varre área de `3×3` até `17×17` (raio `1..8`, configurável no servidor) em até 3 camadas verticais (`abaixo / nível / acima`), nunca forçando carregamento de chunks.
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

### Composteiras (Manual e Elétrica)
- **Composteira Manual (`manual_composter`):** Bloco de início de jogo sem energia; recebe `8` itens orgânicos por interação direta no mundo, acelera com água (`2x`) e gera **Adubo Orgânico** (`organic_compost`).
- **Composteira Elétrica (`electric_composter`):** Tier 1, buffer `20.000 EU`, tanque `16.000 mB` de água. Converte **`4` itens orgânicos + `1.000 mB` de água → `1` fertilizante IC2** em `200` ticks a `5 EU/t`, aceitando 4 upgrades IC2 e slot de bateria.

### Linha de Produção de Weed-Ex e Tubo Térmico
- **Moedor de Grin (`grin_grinder`):** Tier 1, `100` ticks a `5 EU/t`; tritura `1` olho de aranha em `1` Grin Powder utilizando uma **Lâmina de Moagem** reutilizável (`256` usos).
- **Extrator de Grin (`grin_extractor`):** Tier 1, `100` ticks a `5 EU/t` + `20 HU/t` contínuos via `IHeatSource` em qualquer uma das 6 faces; combina `1` Grin Powder + `1.000 mB` de água → `1.000 mB` de **Extrato de Grin**.
- **Misturador de Weed-Ex (`weed_ex_fabricator`):** Tier 1, `100` ticks a `5 EU/t`; combina `1.000 mB` de Extrato de Grin + `1` redstone → `500 mB` de **Weed-Ex**.
- **Tubo Térmico (`thermal_pipe`):** Conecta fontes `IHeatSource` ao Extrator de Grin (até `100 HU/t`). Aceita wrench IC2 para desmontagem com *Agachar + Clique Direito*.

### Analisador, Armazenamento e Cultivos Customizados
- **Analisador de Sementes (`seed_analyzer`):** Tier 1, `100` ticks a `5 EU/t`; analisa sacos de sementes até nível 4 e exibe genética e atributos na interface.
- **Baú de Dados de Cultivos (`crop_database_chest`) e Database Portátil (`portable_crop_database`):** Armazenamento de `500` sacos de sementes em grade paginada `5×5` com busca textual, ordenação (Tipo / Tier / Atributos), favoritos (sem alterar o NBT das sementes), coleta magnética configurável, coleta automática no inventário e vínculo remoto interdimensional via **Vinculador de Upgrade (`upgrade_linker`)**.
- **Upgrade Wireless EU (`wireless_eu_upgrade`):** Implementa `IUpgradeItem` do IC2; vincula-se via *Agachar + Clique Direito* em armazenadores `IEnergyStorage` (`BatBox`, `CESU`, `MFE`, `MFSU`) para alimentar qualquer máquina IC2 com slots de upgrade na mesma dimensão.
- **Cultivos e Fertilizantes:** Registra 5 espécies IC2 (`grinbloom`, `bioreed`, `nitrobulb`, `cupric_bloom` e `auric_moss`), materiais (`bio_fiber`, `nitro_essence`) e fertilizantes (`organic_compost`, `catalytic_fertilizer`).

---

## Visão Geral da Arquitetura

| Pacote | Responsabilidade |
|---|---|
| `br.cropmanager` | Registro central (`CropManager`), configuração (`ManagerConfig`), rede (`ManagerNetwork`), proxies (`CommonProxy`/`ClientProxy`), máquinas e TileEntities (`FarmTile`, `ManagerTile`, `MonitorTile`, `WeederTile`, `ComposterTile`, `GrinGrinderTile`, `GrinExtractorTile`, `WeedExFabricatorTile`, `SeedAnalyzerTile`, `ManualComposterTile`, `ThermalPipeTile`), lógica pura (`ScanLogic`, `AutomationRules`, `CropSurvey`, `ManagerSettings`, `AlarmLogic`, `EnergyWarning`) e GUIs/renderers. |
| `br.cropmanager.crop` | Registro (`CropRegistry`) e classe base (`BaseCropCard`) das espécies customizadas de cultivos IC2 e mecânica de solo catalisado. |
| `br.cropmanager.chest.block` | Bloco animado do Baú de Dados de Cultivos (`BlockCropDatabaseChest`). |
| `br.cropmanager.chest.inventory` | Inventário unitário de sementes (`SeedInventory`), TileEntity do baú (`TileCropDatabaseChest` + variantes legadas), inventário portátil em NBT (`PortableCropInventory`), configurações (`ChestSettings`), ordenação (`SeedSortMode`) e container sincronizado por revisão (`ContainerCropDatabaseChest`). |
| `br.cropmanager.chest.item` | Database Portátil (`ItemPortableCropDatabase`), Vinculador (`ItemUpgradeLinker`) e coleta automática de sementes (`PortableSeedPickup`). |
| `br.cropmanager.chest.network` | Pacotes do baú (`ChestMessage` para busca/favoritos/ack de revisão e `ChestSettingsMessage`). |
| `br.cropmanager.chest.crafting` | Receitas customizadas (`CropChestRecipe`, `PortableCropRecipe`, `PortableLinkRecipe`). |
| `br.cropmanager.chest.integration` | Leitura segura de genética (`CropInformation`), produção (`ProductionInformation`, `PackCropProducts`) e busca (`SeedSearch`). |
| `br.cropmanager.chest.client` | Interface (`GuiCropDatabaseChest`), geometria compartilhada (`CropGuiLayout`) e renderer (`CropChestRenderer`). |

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
| `grin_extractor` | `heatPerTick` | `20` | `0–10000` | Calor (HU/t) exigido durante a extração |
| `thermal_pipe` | `maxHeatPerTick` | `100` | `1–100000` | Limite de transferência térmica por tick (HU/t) |
| `database_chest` | `autoCollectRadius` | `3` | `1–16` | Raio de coleta magnética do Baú de Dados |
| `crops` | `enableCustomCrops` | `true` | `true/false` | Ativa o registro das 5 espécies IC2 do mod |

Mais detalhes de cálculo em [docs/CONFIGURACAO.md](docs/CONFIGURACAO.md).

---

## Testes e Qualidade

- **`src/test/java/br/cropmanager/**`:** **135 testes unitários** distribuídos em **25 suítes** cobrindo lógica de varredura, regras de automação, persistência NBT, capabilities, integridade de assets/i18n (`362` chaves `en_us`/`pt_br`), inventário do Baú de Dados, receitas e proteção contra duplicação.
- **`src/runtimeTest/java/br/cropmanager/**`:** Addon separado (`cropmanager_tests`, nunca empacotado no JAR final) para validação em runtime real no Cleanroom (`RuntimeChecks.java` e `ClientChecks.java`).

---

## Documentação Adicional

- [Relatório de Revisão de Código (`docs/REVISAO_CODIGO.md`)](docs/REVISAO_CODIGO.md) — auditoria detalhada nos 5 eixos (correção, legibilidade, arquitetura, segurança e desempenho) e achados por severidade.
- [Histórico de Versões (`CHANGELOG.md`)](CHANGELOG.md)
- [Configuração e Fórmulas (`docs/CONFIGURACAO.md`)](docs/CONFIGURACAO.md)
- [Guia de Validação Cleanroom (`docs/VALIDACAO.md`)](docs/VALIDACAO.md)
- [Verificação Técnica Anterior (`VERIFICACAO.md`)](VERIFICACAO.md)
