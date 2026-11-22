# Relatório de Verificação e Auditoria Técnica: Crop Manager

**Data da Verificação:** 18 de novembro de 2026  
**Versão Auditada:** `0.5.0-alpha.9`  
**Escopo:** Verificação direta contra o código-fonte Java, assets, arquivos de build e suíte de testes unitários do repositório.

---

## 1. Fase 1: Verificação dos Achados da Auditoria

| ID | Item Auditado | Veredito | Evidência no Código-Fonte |
| :-: | :--- | :---: | :--- |
| **E3** | Classes que implementam `wireless_range_upgrade`, `weed_killer_upgrade` e `harvest_transfer_upgrade`. Relatório original apontava `CropManager.java` para os três. | **Confirmado** | `src/main/java/br/cropmanager/CropManager.java` (L46-57). Os três itens são instanciados como subclasses anônimas de `net.minecraft.item.Item` por meio do método auxiliar `infoItem(name, maxStack, tipKey)`. Responsabilidade atual de `CropManager.java`: registro central de blocos, itens, TileEntities, receitas de crafting vanilla/IC2, remapeamento de IDs legados (`MissingMappings`) e inicialização de rede (`ManagerNetwork`). |
| **E7** | `WirelessEuUpgrade` e conformidade com `IUpgradeItem`. Declaração de `UpgradableProperty`, máquinas IC2 suportadas e blocos armazenadores aceitos. | **Confirmado** *(com detalhamento)* | `src/main/java/br/cropmanager/WirelessEuUpgrade.java` (L35-120). O item implementa `IUpgradeItem`. Em `isSuitableFor` (L67-69), retorna `types != null && !types.isEmpty()`, atuando como curinga para qualquer máquina IC2 com slots de upgrade. Em `onTick`/`injectEnergy` (L72-120), alimenta máquinas IC2 com o componente `Energy.class` (`TileEntityBlock`) ou máquinas Crop Manager com `BasicSink` (`ManagerTile`, `ComposterTile`, `GrinGrinderTile`, `GrinExtractorTile`, `WeedExFabricatorTile`). Em `onItemUseFirst` (L46-64), aceita exclusivamente vínculo com blocos que implementem `IEnergyStorage` (`BatBox`, `CESU`, `MFE`, `MFSU`). |
| **E8** | Causa do `ClassCastException` com Ender IO. Coerência entre `hasCapability` e `getCapability` e impacto da remoção da capability na face inferior. | **Confirmado** | `src/main/java/br/cropmanager/ManagerTile.java` (L351-367). Na versão `0.5.0-alpha6`, a alteração fez com que `hasCapability` retorne `false` e `getCapability` retorne `null` para `ITEM_HANDLER_CAPABILITY` quando `face == EnumFacing.DOWN`. O comentário no código (L352) documenta que conduítes do Ender IO 5.5.1 quebravam ao consultar a face inferior. Trata-se de uma mitigação que mascara a causa raiz, com o efeito colateral de bloquear qualquer extração de itens pela face inferior por tubos, funis ou conduítes. |
| **E9** | Consumo em EU/t, buffer, tier de entrada, tempo de processo e slots de Moedor, Extrator, Misturador, Composteira e Monitor. | **Confirmado** | `src/main/java/br/cropmanager/ManagerConfig.java` e classes TileEntity:<br>• **Composteira:** 5 EU/t, buffer 20.000 EU, Tier 1, 200 ticks (1.000 EU total), 4 slots de upgrade + slot de bateria (L54-57).<br>• **Moedor de Grin:** 5 EU/t, buffer 20.000 EU, Tier 1, 100 ticks (500 EU total), lâmina com 256 usos, 4 slots de upgrade + slot de bateria (L60-64).<br>• **Extrator de Grin:** 5 EU/t + 20 HU/t calor, buffer 20.000 EU, Tier 1, 100 ticks (500 EU total), tanques de 16.000 mB água/extrato, 4 slots de upgrade + slot de bateria (L67-75).<br>• **Misturador:** 5 EU/t, buffer 20.000 EU, Tier 1, 100 ticks (500 EU total), tanques de 16.000 mB extrato/Weed-Ex, 4 slots de upgrade + slot de bateria (L77-85).<br>• **Monitor:** 0 EU/t base; buffer dinâmico de 50.000 EU por Upgrade Wireless EU (até 200.000 EU / 1.024 EU/t taxa, Tier 2), 4 slots alcance, 3 slots fertilizante (até 192 un.), 1 slot transferência de colheita, 1 slot Wireless EU e inventário de colheita de 15 slots (`MonitorTile.java` L30-74). |
| **E13** | Independência entre os 20 mB do Upgrade Weed Killer e os 20 mB do Capinador a Biogás. | **Confirmado** | `src/main/java/br/cropmanager/ManagerConfig.java` (L29, 48, 118, 135). São propriedades completamente separadas: `managerWeedKillerCostMB = 20` (fluido Weed-Ex consumido pelo Manager) e `weederRemoveGasCostMB = 20` (fluido Biogás IC2 consumido pelo Capinador). A igualdade no valor padrão (20) foi coincidência de parametrização. |
| **E14** | Semântica da "trava de 100% de Weed-Ex" no Monitor. | **Confirmado** | `src/main/java/br/cropmanager/AutomationRules.java` (L31-38). Trata-se de uma histerese de segurança (`weedLatched`). Se a parada for acionada por Weed-Ex abaixo da meta (`weedPercent >= 0 && below(...)`), a máquina para e ativa a trava (`weedLatched = true`). Ela só volta a funcionar quando o tanque de Weed-Ex atinge 100% da capacidade (`weed >= weedCapacity`), prevenindo oscilações e reinícios repetitivos por adições parciais de fluido. |
| **L3** | Lista de vegetais aceitos na Composteira Elétrica (vanilla e OreDictionary). | **Confirmado** | `src/main/java/br/cropmanager/ComposterTile.java` (L47-63).<br>• **Itens Vanilla:** `Items.WHEAT`, `WHEAT_SEEDS`, `PUMPKIN_SEEDS`, `MELON_SEEDS`, `CARROT`, `POTATO`, `POISONOUS_POTATO`, `BEETROOT`, `BEETROOT_SEEDS`, `REEDS`, `APPLE`, `MELON`, `Blocks.LEAVES`, `LEAVES2`, `SAPLING`, `VINE`, `TALLGRASS`, `CACTUS`.<br>• **OreDictionary:** `treeSapling`, `treeLeaves`, `listAllseed`, `listAllveggie`, `listAllfruit`, `cropWheat`, `cropCarrot`, `cropPotato`, `cropBeetroot`. |
| **Q4** | Proteções contra duplicação de sementes no Baú de Dados de Cultivos. | **Confirmado** | `ContainerCropDatabaseChest.java` (L278-280, 288-310), `TileCropDatabaseChest.java` (L28-33, 73-82) e `SeedInventory.java` (L21-38). O baú bloqueia divisão por clique direito e clone no criativo em slots de semente; limita cada slot a 1 saco de semente (`getSlotLimit` = 1, `accepts` valida `ICropSeed`); `transferStackInSlot` move sementes respeitando limite unitário; automação e hoppers passam por `controlledAutomation` e `canInsertItem` com validação de tipo e limite unitário. |

---

## 2. Investigação de Riscos Técnicos

1. **Custo e Orçamento de Varredura:**
   - **Comportamento real:** O volume varia de 27 (área 3×3×3) até 867 posições (17×17×3). A varredura é distribuída entre ticks usando o acumulador `ScanLogic.credit(progress, interval, maxScans)`. O servidor processa no máximo `ManagerConfig.maxScans` posições por tick (padrão de 8 posições).
   - **Cache e carregamento:** As coordenadas são calculadas dinamicamente (`ScanLogic.offset`). Não há cache de posições, mas cada bloco é protegido por `world.isBlockLoaded(target, false)`, impedindo carregamento acidental de chunks.
   - **Configurabilidade:** O intervalo base (`baseIntervalTicks`, padrão 2) e o limite por tick (`maxPositionsPerTick`, padrão 8) são configuráveis via `.cfg`.

2. **Sincronização do Baú de 500 Slots:**
   - **Comportamento real:** O método `TileCropDatabaseChest.getUpdateTag()` (L113-118) serializa apenas as coordenadas (`x`, `y`, `z`) e o número de visualizadores (`Viewers`). Os 500 slots de sementes **não são enviados** em pacotes de chunk nem em `getUpdateTag()`. A sincronização dos itens ocorre estritamente sob demanda via Container quando o jogador abre a interface. O risco de inflação de pacotes de mundo foi **refutado**.

3. **Vínculos Wireless:**
   - **Wireless EU Upgrade:** Grava a dimensão (`Dim`), exige mesma dimensão (`link.getInteger("Dim") == world.provider.getDimension()`), valida se o chunk está carregado (`world.isBlockLoaded(sourcePos, false)`) e trata remoção do bloco de energia com encerramento gracioso (retorna 0 transferido sem exceção). Alcance é ilimitado na mesma dimensão.
   - **Monitor Agrícola ↔ Manager:** Grava a dimensão (`Dimension`), exige mesma dimensão (caso contrário reporta status `"dimension"`), valida chunk carregado (reporta `"unloaded"`) e limita a distância de 32 a 512 blocos dependendo dos Upgrades de Alcance (caso contrário reporta `"distance"`). Se o bloco vinculado for quebrado, reporta `"unlinked"` sem exceções.
   - **Database Portátil:** Grava a dimensão (`Dim`), suporta conexão entre dimensões diferentes via `MinecraftServer.getWorld(dim)`, valida chunk carregado no mundo de destino (`targetWorld.isBlockLoaded(pos)`) e trata baú quebrado ou ausente com mensagem no chat e encerramento seguro. Alcance é ilimitado.

4. **Migração de Versões Legadas (alpha.5):**
   - **Comportamento real:** Não há campo formal de versão no NBT (como `DataVersion`). A migração se baseia na contagem de slots no NBT (ex.: `Size == 5` em `WeedExFabricatorTile`) e no evento de remapeamento `MissingMappings` do Forge combinado com `LegacyMigration` e `LegacyRecoveryTile`. Há teste unitário para o NBT do mixer em `PersistenceTest.java`, mas inexiste fixture de teste com arquivos de região `.mca` de mundos legados.

5. **Paridade de Internacionalização (i18n):**
   - **Comportamento real:** `src/main/resources/assets/cropmanager/lang/en_us.lang` e `pt_br.lang` possuem exatas **362 chaves** cada, com paridade de 100% (zero chaves ausentes em qualquer dos arquivos).

---

## 3. Fase 2: Conferência de Ambiente e Build

- **Versão do Mod:** `0.5.0-alpha.9` (declarada em `build.gradle`, `CropManager.java` e `mcmod.info`).
- **Minecraft:** `1.12.2`.
- **Forge / Cleanroom:** Forge `14.23.5.2860+` / Cleanroom runtime `0.6.11-alpha` (compatível com a série `0.6.13-alpha`).
- **IC2 Experimental:** `2.8.222-ex112` (verificado via hash SHA-256 `f96b1886b6d060b308fb811029d5c5cc76deb17b835aa368dcb6071f2de290d4` na task `verifyIc2`).
- **Versão do Gradle Wrapper:** `9.7.0` (definida em `gradle/wrapper/gradle-wrapper.properties`: `gradle-9.7.0-bin.zip`).
- **RetroFuturaGradle:** `2.0.3`.
- **Bytecode Alvo:** Java 8 (`JavaLanguageVersion.of(8)`, fornecedor Azul Systems).
- **JVM de Execução do Daemon:** Azul Zulu JRE 25.0.3 (`25.0.3+9-LTS`).
- **Versão do Ender IO Citada no Histórico:** O comentário em `ManagerTile.java` (L352) cita expressamente `Ender IO 5.5.1`. Na série oficial de 1.12.2, a última versão do desenvolvedor original foi a 5.3.72; a versão 5.5.1 corresponde a um fork comunitário voltado a modpacks modernos de Cleanroom.
- **Ambiente Git e Commit:** O ambiente local do projeto não possui pasta `.git` inicializada nem o executável `git` instalado no PATH do sistema. O commit é registrado como `[N/A - ambiente sem repositório .git]`.
- **Resultado dos Testes:**
  - Testes pré-existentes: **121 testes**, 100% de sucesso.
  - Testes com adições da Fase 3: **124 testes**, 100% de sucesso (tempo de execução: 17s).
  - Distribuição por pacote:
    - `br.cropmanager.AlarmLogicTest`: 2
    - `br.cropmanager.AssetIntegrityTest`: 4 (+1 teste de paridade de chaves i18n)
    - `br.cropmanager.AutomationRulesTest`: 5
    - `br.cropmanager.CropSurveyTest`: 5
    - `br.cropmanager.EnergyWarningTest`: 2
    - `br.cropmanager.ExpansionTest`: 4
    - `br.cropmanager.ManagerConfigTest`: 2
    - `br.cropmanager.ManagerSettingsTest`: 3
    - `br.cropmanager.PersistenceTest`: 10 (+1 teste de consulta de capability nas 6 faces)
    - `br.cropmanager.ScanLogicTest`: 3
    - `br.cropmanager.UiNumbersTest`: 2
    - `br.cropmanager.chest.ChestSettingsTest`: 5
    - `br.cropmanager.chest.ContainerTest`: 18 (+1 teste de bloqueio de duplicação por clique direito/clone criativo)
    - `br.cropmanager.chest.CropGuiLayoutTest`: 6
    - `br.cropmanager.chest.PackCropProductsTest`: 3
    - `br.cropmanager.chest.PortableInventoryTest`: 11
    - `br.cropmanager.chest.PortableSeedPickupTest`: 8
    - `br.cropmanager.chest.SeedInventoryTest`: 13
    - `br.cropmanager.chest.SortTest`: 6
    - `br.cropmanager.chest.UpgradeLinkerTest`: 3
    - `br.cropmanager.chest.crafting.CropChestRecipeTest`: 8
    - `br.cropmanager.chest.crafting.PortableCropRecipeTest`: 1

---

## 4. Recomendações e Propostas Não Aplicadas

As seguintes melhorias foram identificadas durante a auditoria e são submetidas para decisão do mantenedor:

### 1. Refatoração dos Upgrades do Monitor (Achado E3)
- **Situação Atual:** Os itens `wireless_range_upgrade`, `weed_killer_upgrade` e `harvest_transfer_upgrade` são instanciados como classes anônimas dentro de `CropManager.java`.
- **Proposta:** Extrair cada item para sua própria classe pública (`ItemWirelessRangeUpgrade.java`, `ItemWeedKillerUpgrade.java`, `ItemHarvestTransferUpgrade.java`) em um pacote dedicado como `br.cropmanager.item`.
- **Justificativa:** Separação de responsabilidades e facilitação de testes unitários isolados de cada upgrade.
- **Esforço Estimado:** Baixo (1 a 2 horas).
- **Risco:** Baixo (requer manter os mesmos nomes de registro Forge para não quebrar mundos existentes).

### 2. Resolução da Causa Raiz da Face Inferior com Ender IO (Achado E8)
- **Situação Atual:** A face inferior do `AdvancedManagerTile` simplesmente desliga `ITEM_HANDLER_CAPABILITY`, impedindo qualquer automação por baixo.
- **Proposta:** Identificar qual método de chamada do conduíte do Ender IO dispara `ClassCastException` (geralmente expectativa de métodos específicos de `ISidedInventory` legados em combinação com Capability) e fornecer uma implementação segura de wrapper de `IItemHandler` compatível com a face inferior.
- **Justificativa:** Restaura a automação plena pelas 6 faces do bloco, permitindo que jogadores usem conduítes por baixo.
- **Esforço Estimado:** Médio (3 a 5 horas, requer teste com o JAR do Ender IO Cleanroom).
- **Risco:** Médio (risco de regressão do crash se a distribuição do Ender IO for incompatível).

### 3. Balanceamento e Limitações do Upgrade Wireless EU
- **Situação Atual:** O upgrade drena energia de armazenadores IC2 a qualquer distância dentro da dimensão, sem perda energética e alimentando qualquer máquina IC2 com slots de upgrade.
- **Proposta:** Introduzir opções configuráveis no `.cfg` para taxa percentual de perda por transferência sem fio (ex.: 5% a 10%) e opção de alcance máximo em blocos.
- **Justificativa:** Evita desvalorizar completamente a infraestrutura tradicional de cabos de alta tensão do IC2.
- **Esforço Estimado:** Baixo (2 horas).
- **Risco:** Baixo (pode ser tornado opcional e configurável sem quebrar compatibilidade).
