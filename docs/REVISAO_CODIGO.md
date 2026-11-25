# Revisão de Código e Qualidade — Crop Manager (`0.5.0-alpha9`)

**Escopo revisado:** `src/main/java/br/cropmanager/**` (~7.000 linhas em código principal, pacotes `br.cropmanager`, `br.cropmanager.crop`, `br.cropmanager.chest.*`)  
**Referência / Ambiente:** Sem repositório Git inicializado (`[sem repositório .git]`); código verificado contra a suíte de testes unitários em `src/test/java`.  
**Modo de revisão:** Auditoria inicial seguida de aplicação cirúrgica dos 2 achados **Critical**, 4 **Obrigatórios** e 3 **Nits** triviais (seção ao final); itens **Consider** (#7–#13) e código morto permanecem pendentes de decisão.

---

## Contexto e Verificação Executada

O projeto é um addon para Minecraft 1.12.2 + IndustrialCraft 2 Experimental (`2.8.222-ex112`) utilizando RetroFuturaGradle `2.0.3` e compilado para bytecode Java 8.

Comando de verificação executado após a revisão (usando cache local `.gradle-home` e ignorando `downloadFernflower` no modo offline):

```powershell
$env:GRADLE_USER_HOME = "$PWD\.gradle-home"
.\gradlew.bat test --rerun-tasks --offline -x downloadFernflower
```

- **Resultado:** `BUILD SUCCESSFUL`
- **Cobertura observada:** **135 testes** em **25 suítes** (`build/test-results/test/TEST-*.xml`), **0 falhas**, **0 erros**, **0 ignorados**.
- **Nota de ambiente:** A execução sem `-x downloadFernflower` falha offline na etapa `:downloadFernflower` do RetroFuturaGradle (`Could not evaluate onlyIf predicate`); `gradlew_local.bat` aponta `JAVA_HOME` para JDK 8, embora o Gradle 9.7 exija Java 17+ (executado aqui com OpenJDK 25).

---

## Achados por Severidade

### Critical (Bloqueadores)

1. **Critical: Explosão exponencial $O(2^N)$ em dutos térmicos e desperdício de calor em malhas**  
   - **Arquivos:** [ThermalPipeTile.java](../src/main/java/br/cropmanager/ThermalPipeTile.java#L13-L38) e [GrinExtractorTile.java](../src/main/java/br/cropmanager/GrinExtractorTile.java#L89-L93)  
   - **Problema:** Em `ThermalPipeTile.transfer(...)`, para cada vizinho `IHeatSource`, o tubo chama `source.requestHeat(sourceSide, remaining)` (linha 28) e imediatamente `source.drawHeat(sourceSide, available, simulate)` (linha 30). Como ambos `requestHeat` e `drawHeat` delegam para `transfer`, cada salto de tubo duplica o número de chamadas recursivas para o nó anterior. Em uma linha reta de $N$ tubos térmicos, um único pedido gera $\ge 2^N$ chamadas recursivas (com 20–25 tubos são milhões de `world.getTileEntity` por tick a partir do `GrinExtractorTile`), congelando o tick do servidor (DoS).  
   - **Efeito colateral em loops:** `visited.remove(pos.toLong())` no bloco `finally` (linha 36) remove o nó ao retroceder, permitindo que caminhos alternativos em malhas consultem a mesma fonte duas vezes durante `requestHeat` (contando calor dobrado). Com isso, `GrinExtractorTile` (linha 92) pode passar em `findReadyHeatSide`, drenar calor parcial em `drawHeatFrom` (`< heatCost`) e descartá-lo sem avançar o progresso.  
   - **Correção recomendada:** Propagar uma única travessia por consulta (ou usar `drawHeat(..., true)` apenas se não houver camada dupla de `requestHeat`), mantendo o conjunto de visitados pela duração inteira da busca raiz quando o objetivo for apenas sondagem sem drenagem.

2. **Critical: Ausência de `@SideOnly(Side.CLIENT)` em métodos de tooltip em classes comuns (crash em servidor dedicado)**  
   - **Arquivos:** [CropManager.java](../src/main/java/br/cropmanager/CropManager.java#L57-L70), [FarmTool.java](../src/main/java/br/cropmanager/FarmTool.java#L23-L30) e [FertilizerItem.java](../src/main/java/br/cropmanager/FertilizerItem.java#L7-L8) ([L43-L46](../src/main/java/br/cropmanager/FertilizerItem.java#L43-L46))  
   - **Problema:** `CropManager.infoItem` e `CropManager.materialItem` criam subclasses anônimas de `Item` (instanciadas estaticamente na inicialização da classe principal do mod) sobrescrevendo `addInformation(..., ITooltipFlag)` e chamando `net.minecraft.client.resources.I18n.format(...)` **sem** `@SideOnly(Side.CLIENT)`. O mesmo ocorre em `FarmTool.addInformation` e `FertilizerItem.addInformation` (enquanto `WirelessEuUpgrade`, `ItemUpgradeLinker` e `ItemPortableCropDatabase` declaram `@SideOnly(Side.CLIENT)` corretamente). No Forge 1.12.2 em servidor dedicado, métodos sem `@SideOnly(Side.CLIENT)` não são removidos pelo `SideTransformer`, provocando `NoClassDefFoundError` (`ITooltipFlag` / `I18n`) durante o carregamento/inspeção de classes no servidor.  
   - **Correção recomendada:** Anotar todos os `addInformation` com `@SideOnly(Side.CLIENT)` (e evitar imports diretos de `net.minecraft.client.*` fora de métodos anotados).

---

### Obrigatórios (Devem ser corrigidos antes de merge/release)

3. **Trava do slot portador ignorada em `ClickType.SWAP` no modo vinculado remoto (GUI 13)**  
   - **Arquivo:** [ContainerCropDatabaseChest.java](../src/main/java/br/cropmanager/chest/inventory/ContainerCropDatabaseChest.java#L262-L266)  
   - **Problema:** As linhas 262 e 265 verificam `chest.isCarrierSlot(...)` em vez de `isLockedCarrierSlot(...)` (usado no restante da classe: linhas 96, 99, 100, 297, 300, 306). Ao abrir um baú remotamente via Database Portátil vinculado (GUI 13), `chest` é um `TileCropDatabaseChest` (cujo `isCarrierSlot` sempre retorna `false`), permitindo que teclas numéricas da hotbar (`ClickType.SWAP`) troquem o item vinculado de lugar dentro do inventário do jogador.  
   - **Correção recomendada:** Substituir `chest.isCarrierSlot(...)` por `isLockedCarrierSlot(...)` nas linhas 262 e 265.

4. **Tooltip de favorito sobrepõe o tooltip da semente e aparece em slots vazios**  
   - **Arquivo:** [GuiCropDatabaseChest.java](../src/main/java/br/cropmanager/chest/client/GuiCropDatabaseChest.java#L263-L265)  
   - **Problema:** Logo após `renderHoveredToolTip(mouseX, mouseY)` (linha 254), o bloco das linhas 263–265 chama `drawHoveringText(I18n.format("cropdatabasechest.favorite.tip"), mouseX, mouseY)` sempre que o cursor está sobre qualquer slot da grade do baú (mesmo vazio!) e apenas quando `hasManagement()` é verdadeiro. Com isso, sobrepõe o tooltip da semente ocupada, aparece em slots vazios e deixa de exibir a dica no modo portátil (onde `toggleFavorite` funciona via linha 75 de `ContainerCropDatabaseChest`).  
   - **Correção recomendada:** Anexar a dica em `ItemTooltipEvent` ou apenas quando `hovered.getHasStack()` (combinando com as linhas existentes em vez de desenhar uma segunda caixa por cima) e incluir `container.isPortable()`.

5. **`NullPointerException` em `canFillFluidType(null)` nos tanques expostos via Capability**  
   - **Arquivos:** [ManagerTile.java](../src/main/java/br/cropmanager/ManagerTile.java#L87-L96), [WeederTile.java](../src/main/java/br/cropmanager/WeederTile.java#L25) e [MonitorTile.java](../src/main/java/br/cropmanager/MonitorTile.java#L76-L82) ([L121](../src/main/java/br/cropmanager/MonitorTile.java#L121))  
   - **Problema:** Os `FluidTank` de água, Weed-Ex e biogás chamam `fluid.getFluid() == ...` sem validar `fluid != null`. Como `ManagerTile` expõe `water.getTankProperties()[0]` / `weed.getTankProperties()[0]` diretamente, `WeederTile` expõe `gas` diretamente e `MonitorTile.properties()` delega direto para `tank.canFillFluidType(fluid)`, qualquer mod que consulte `IFluidTankProperties.canFillFluidType(null)` provocará NPE.  
   - **Correção recomendada:** Adicionar `fluid != null && ...` em todos os `canFillFluidType` (alinhando com `ComposterTile.java:36` e `FarmTile.java:85`).

6. **Pacote de rede `Settings` permite desabilitar bits de cuidado em `ManagerTile.functions`**  
   - **Arquivos:** [ManagerNetwork.java](../src/main/java/br/cropmanager/ManagerNetwork.java#L42-L43) e [ManagerTile.java](../src/main/java/br/cropmanager/ManagerTile.java#L317-L323)  
   - **Problema:** O mod removeu os botões individuais de fertilizante, água e Weed-Ex (mantendo os bits 1–3 sempre ligados via `| 14` em `readFromNBT` linha 380 e `control(201)` linha 429). Porém, `SettingsHandler` repassa `message.control` direto para `ManagerSettings.applyControl` e `tile.settings(...)` sem restringir `control == 0` nem preservar `| 14`, permitindo que um cliente adulterado desligue alertas de status (`NO_FERTILIZER`, `NO_WATER`, `NO_WEED`) ou zere `functions`.  
   - **Correção recomendada:** Restringir `SettingsHandler` ao controle de colheita (`control == 0`) e impor `| 14` dentro de `ManagerTile.settings(...)`.

---

### Consider (Arquitetura, Desempenho e Robustez)

7. **Consider: Varredura e cópia de `world.loadedTileEntityList` todo tick no Monitor**  
   - **Arquivo:** [MonitorTile.java](../src/main/java/br/cropmanager/MonitorTile.java#L234-L245)  
   - **Problema:** Quando o Monitor tem Upgrade Wireless EU e energia no buffer não consumida pelo alvo principal, `transferWirelessEnergy()` copia todo `world.loadedTileEntityList` (`new ArrayList<>(...)`) e percorre todas as TileEntities da dimensão **todo tick** (20×/s por Monitor). Em servidores com muitas máquinas carregadas, gera pressão de GC e custo de CPU desnecessários.  
   - **Sugestão:** Iterar apenas `Chunk`s dentro de `SUPPORT_WIRELESS_RADIUS` ou manter cache atualizado a cada 20 ticks.

8. **Consider: Envio de 506 pacotes `SPacketWindowProperty` a cada atualização do Baú de Dados**  
   - **Arquivo:** [ContainerCropDatabaseChest.java](../src/main/java/br/cropmanager/chest/inventory/ContainerCropDatabaseChest.java#L148-L159)  
   - **Problema:** `sendView` envia todos os 500 elementos de `positions` mais 6 propriedades de cabeçalho/rodapé via `sendWindowProperty` a cada tecla na busca, troca de página, ordenação ou inserção via funil.  
   - **Sugestão:** Consolidar o mapeamento de visão em um único `IMessage` compacto (ou enviar somente slots visíveis/alterados).

9. **Consider: Reenvio de `guiState()` todo tick para jogadores com GUI do Manager aberta**  
   - **Arquivos:** [ManagerContainer.java](../src/main/java/br/cropmanager/ManagerContainer.java#L44-L49) e [ManagerTile.java](../src/main/java/br/cropmanager/ManagerTile.java#L404)  
   - **Problema:** `guiState()` inclui `"ReportAge"`, que incrementa a cada tick após o primeiro censo completo. Assim `!state.equals(lastState)` é verdadeiro todo tick, reenviando todo o `NBTTagCompound Report` (incluindo a lista `Species`) 20×/s na rede enquanto a tela estiver aberta, embora `ManagerGui` não exiba `ReportAge`.  
   - **Sugestão:** Remover `ReportAge` de `guiState()` (ou arredondar em segundos / enviar apenas sob alteração de conteúdo).

10. **Consider: `markDirty()` chamado todo tick em varreduras ociosas**  
    - **Arquivos:** [ManagerTile.java](../src/main/java/br/cropmanager/ManagerTile.java#L191) ([L199](../src/main/java/br/cropmanager/ManagerTile.java#L199)), [WeederTile.java](../src/main/java/br/cropmanager/WeederTile.java#L40)  
    - **Problema:** `ManagerTile` marca o chunk como sujo (`world.markChunkDirty`) todo tick enquanto está ligado varrendo (mesmo sem plantas) e `WeederTile` marca até 2×/tick. Isso força salvamentos frequentes de chunk.  
    - **Sugestão:** Usar throttling periódico (ex.: a cada 20 ticks, como em [ManualComposterTile.java:99](../src/main/java/br/cropmanager/ManualComposterTile.java#L99)), mantendo `markDirty()` imediato apenas em mudanças de inventário/tanques/estado.

11. **Consider: Bloqueio da face inferior (`EnumFacing.DOWN`) no `AdvancedManagerTile`**  
    - **Arquivo:** [ManagerTile.java](../src/main/java/br/cropmanager/ManagerTile.java#L328-L335)  
    - **Contexto:** Mitigação intencional da 0.5.0-alpha6 para evitar `ClassCastException` com conduítes de item do Ender IO 5.5.1 (já registrada em `CHANGELOG.md` e `VERIFICACAO.md`), mas impede extração inferior por funis e outros mods. Investigar a incompatibilidade específica do wrapper de `IItemHandler` continua recomendado para liberar as 6 faces.

12. **Consider: Duplicação entre máquinas de suporte e `EquipmentContainer`**  
    - **Arquivos:** [ComposterTile.java](../src/main/java/br/cropmanager/ComposterTile.java), [GrinGrinderTile.java](../src/main/java/br/cropmanager/GrinGrinderTile.java), [GrinExtractorTile.java](../src/main/java/br/cropmanager/GrinExtractorTile.java), [WeedExFabricatorTile.java](../src/main/java/br/cropmanager/WeedExFabricatorTile.java), [SeedAnalyzerTile.java](../src/main/java/br/cropmanager/SeedAnalyzerTile.java) e [EquipmentContainer.java](../src/main/java/br/cropmanager/EquipmentContainer.java#L17-L62)  
    - **Problema:** As cinco máquinas elétricas de suporte duplicam a lógica de `recalculateUpgrades()`, `exposed()`/`itemsForFace()` e serialização NBT de `EU`/`Work`/`Duration`, além de `EquipmentContainer` empilhar cadeias `instanceof` para layout de slots, validação e `transferStackInSlot`. Essa duplicação já gerou pequenas divergências (ex.: `ComposterTile.readFromNBT:124` não limita `progress` a `workTicks - 1`, e `ComposterTile.inventory` não sobrescreve `insertItem` para filtrar `isItemValid`).  
    - **Sugestão:** Extrair uma subclasse comum (ex.: `PoweredMachineTile` estendendo `FarmTile`) e delegar definição de slots/transferência para descritores por máquina.

13. **Consider: Upgrades do Monitor instanciados como subclasses anônimas**  
    - **Arquivo:** [CropManager.java](../src/main/java/br/cropmanager/CropManager.java#L52-L70)  
    - **Problema:** `WEED_KILLER`, `WIRELESS_RANGE` e `HARVEST_TRANSFER` compartilham classe anônima inline em `CropManager.java` (observação pré-existente de `VERIFICACAO.md`), dificultando isolamento e contribuindo para o problema #2.

---

### Nit & FYI

- **Nit:** [EquipmentGui.java:340](../src/main/java/br/cropmanager/EquipmentGui.java#L340) — Linha inalcançável `if(items.tagCount()==0)` após o retorno antecipado nas linhas 322–326.
- **Nit:** [ManagerBlock.java:100-119](../src/main/java/br/cropmanager/ManagerBlock.java#L100-L119) — Usar wrench (sem agachar) no `THERMAL_PIPE` (`kind == 12`) altera a propriedade `FACING` (ignorada pelo modelo conectado do tubo) e consome 100 EU ou 1 de durabilidade da ferramenta. Além disso, máquinas com `kind < 10` (`ADVANCED`, `MONITOR`, `COMPOSTER`, `WEEDER`, `WEED_EX_FABRICATOR`) não suportam rotação/desmontagem por wrench.
- **Nit:** [WirelessEuUpgrade.java:104](../src/main/java/br/cropmanager/WirelessEuUpgrade.java#L104) — `-(int) Math.ceil(accepted)` desconta 1 EU inteiro mesmo quando o receptor aceita frações `< 1.0 EU` ao completar o buffer.
- **Nit:** [ManagerTile.java:160](../src/main/java/br/cropmanager/ManagerTile.java#L160) — Em `migrate()`, validar `old.getBlock() instanceof ManagerBlock` antes de `old.getValue(ManagerBlock.FACING)` evita `IllegalArgumentException` caso o bloco já tenha sido substituído externamente.
- **Nit:** [CropManager.java:198](../src/main/java/br/cropmanager/CropManager.java#L198) — `required(...)` registra `INFO` para cada item IC2 consultado nas receitas.
- **FYI:** [ComposterTile.java:26](../src/main/java/br/cropmanager/ComposterTile.java#L26), [GrinGrinderTile.java:21](../src/main/java/br/cropmanager/GrinGrinderTile.java#L21), [GrinExtractorTile.java:28](../src/main/java/br/cropmanager/GrinExtractorTile.java#L28), [WeedExFabricatorTile.java:30-31](../src/main/java/br/cropmanager/WeedExFabricatorTile.java#L30-L31) e [MonitorTile.java:31-33](../src/main/java/br/cropmanager/MonitorTile.java#L31-L33) mantêm constantes estáticas legadas (`DURATION`, `EU_PER_TICK`, `CAPACITY`, etc.) enquanto o comportamento real vem de `ManagerConfig.*` (apenas `GrinGrinderTile.DURATION` ainda é referenciada por `RuntimeChecks`).
- **FYI:** [ItemPortableCropDatabase.java:56](../src/main/java/br/cropmanager/chest/item/ItemPortableCropDatabase.java#L56) e [TileCropDatabaseChest.java:99](../src/main/java/br/cropmanager/chest/inventory/TileCropDatabaseChest.java#L99) usam `isBlockLoaded(pos)` (1 argumento) enquanto os demais arquivos usam `isBlockLoaded(pos, false)`.

---

## Código Morto Identificado na Revisão Inicial (`0.5.0-alpha9` — **REMOVIDO em 2026-11-23**)

- ~~`FarmDisplay.java`~~ — removida (`src/main/java/br/cropmanager/FarmDisplay.java`).
- ~~[AreaPreview.java](../src/main/java/br/cropmanager/AreaPreview.java)~~ — removidos `markers()`, `marker()` e leitura de `"Rejected"` / `"Unknown"`.
- ~~[ManagerBlock.java](../src/main/java/br/cropmanager/ManagerBlock.java) e [ClientProxy.java](../src/main/java/br/cropmanager/ClientProxy.java)~~ — removidos `HALF`, `MachineHalf`, `isTwoHigh()`, `isUpper()`, `basePos()`, `hasRoomForUpper()` e casos mortos `kind == 2, 5, 6, 7`.
- ~~[MonitorTile.java](../src/main/java/br/cropmanager/MonitorTile.java)~~ — removidas ações inoperantes `202..204` em `control(...)`.

*(Nota: `LegacyMigration`, `LegacyRecoveryTile`, `LegacyModularChestTile`, `TileCropDatabaseChest125`, `TileCropDatabaseChest250` e o modo básico de `ManagerTile` são código ativo de migração/compatibilidade de mundos antigos e não são código morto.)*

---

## Checklist da Revisão

- [x] **Contexto:** Arquitetura, configurações, changelog e suíte de testes compreendidos.
- [ ] **Correção:** Encontrados problemas em `ThermalPipeTile`/`GrinExtractorTile`, trava de `SWAP` no baú remoto, tooltip no `GuiCropDatabaseChest` e `canFillFluidType(null)`.
- [x] **Legibilidade:** Código direto na maior parte; melhorias sugeridas para cadeias `instanceof` em `EquipmentContainer` e classes anônimas de upgrade.
- [ ] **Arquitetura:** Falta `@SideOnly(Side.CLIENT)` em 3 pontos de classes comuns; duplicação entre máquinas de suporte.
- [ ] **Segurança:** Validação server-side sólida na maior parte (distância, chunk carregado, limites de busca em `ChestMessage`), com lacuna apenas em `SettingsHandler` (`functions` bits 1–3) e `SWAP` no slot vinculado.
- [ ] **Desempenho:** Bloqueadores/gargalos relevantes em `ThermalPipeTile` ($O(2^N)$), `MonitorTile.loadedTileEntityList` por tick, 506 pacotes em `sendView` e `ReportAge`/`markDirty()` contínuos.
- [x] **Verificação:** 135/135 testes unitários executados e passando.

### Veredito
- [x] **Aprovado após correções críticas e obrigatórias** — Todos os 2 itens **Critical**, os 4 itens **Obrigatórios** e os 3 **Nits** triviais foram corrigidos e validados pela suíte de testes (**135/135**). Restam apenas melhorias estruturais opcionais (**Consider #7–#13**) e eventual remoção de código morto mediante confirmação.

---

## Correções aplicadas (2026-11-21)

1. **Dutos térmicos lineares ($O(N)$ em vez de $O(2^N)$) — Critical #1:**  
   - [ThermalPipeTile.java](../src/main/java/br/cropmanager/ThermalPipeTile.java): `transfer(...)` agora controla a consulta raiz com `boolean root = visited.isEmpty()`, remove a chamada duplicada a `source.requestHeat(...)` (propagando apenas `source.drawHeat(..., simulate)`) e limpa `visited` somente ao retornar da chamada raiz (`if (root) visited.clear()`), evitando re-visita em malhas e explosão exponencial.
2. **Anotação `@SideOnly(Side.CLIENT)` em tooltips de classes comuns — Critical #2:**  
   - [CropManager.java](../src/main/java/br/cropmanager/CropManager.java), [FarmTool.java](../src/main/java/br/cropmanager/FarmTool.java) e [FertilizerItem.java](../src/main/java/br/cropmanager/FertilizerItem.java): `addInformation` anotado com `@SideOnly(Side.CLIENT)`.
3. **Bloqueio de `ClickType.SWAP` para slot portador no modo remoto (GUI 13) — Obrigatório #3:**  
   - [ContainerCropDatabaseChest.java](../src/main/java/br/cropmanager/chest/inventory/ContainerCropDatabaseChest.java): `slotClick` atualizado para checar `isLockedCarrierSlot(...)` em vez de `chest.isCarrierSlot(...)`.
4. **Tooltip de favorito sem sobreposição nem exibição em slots vazios — Obrigatório #4:**  
   - [GuiCropDatabaseChest.java](../src/main/java/br/cropmanager/chest/client/GuiCropDatabaseChest.java): dica `cropdatabasechest.favorite.tip` exibida apenas quando o slot contém item (`hovered.getHasStack()`), com `Shift` pressionado (`isShiftKeyDown()`), abrangendo também o modo portátil (`container.isPortable()`).
5. **Proteção contra `null` em `canFillFluidType(FluidStack)` — Obrigatório #5:**  
   - [ManagerTile.java](../src/main/java/br/cropmanager/ManagerTile.java), [MonitorTile.java](../src/main/java/br/cropmanager/MonitorTile.java), [WeederTile.java](../src/main/java/br/cropmanager/WeederTile.java), [GrinExtractorTile.java](../src/main/java/br/cropmanager/GrinExtractorTile.java) e [WeedExFabricatorTile.java](../src/main/java/br/cropmanager/WeedExFabricatorTile.java): adicionada checagem `fluid != null && ...`.
6. **Validação estrita do pacote `Settings` e máscara de `functions` — Obrigatório #6:**  
   - [ManagerNetwork.java](../src/main/java/br/cropmanager/ManagerNetwork.java) (`SettingsHandler` aceita apenas `message.control == 0`) e [ManagerTile.java](../src/main/java/br/cropmanager/ManagerTile.java) (`settings(...)` força `(newFunctions & 1) | 14`).
7. **Nits aplicados:**  
   - [EquipmentGui.java](../src/main/java/br/cropmanager/EquipmentGui.java): removida linha inalcançável `if(items.tagCount()==0)` em `drawHarvestList`.  
   - [ManagerTile.java](../src/main/java/br/cropmanager/ManagerTile.java): `migrate()` verifica `old.getBlock() instanceof ManagerBlock` antes de ler `FACING`.  
   - [ManagerBlock.java](../src/main/java/br/cropmanager/ManagerBlock.java): `THERMAL_PIPE` (`kind == 12`) sem agachar ignora rotação com wrench sem gastar energia/durabilidade.

### Otimizações e testes de regressão aplicados (2026-11-23)

1. **Desbloqueio de compilação em `br/cropmanager/guide`:**  
   - [ManualEntry.java](../src/main/java/br/cropmanager/guide/ManualEntry.java): implementado `getRecipes()` consultando `ForgeRegistries.RECIPES` com fallback para o grid manual configurado.  
   - [ManualRegistry.java](../src/main/java/br/cropmanager/guide/ManualRegistry.java): implementado `findEntry(ItemStack)` buscando a entrada correspondente ao item exibido.
2. **Refino em malhas térmicas — [ThermalPipeTile.java](../src/main/java/br/cropmanager/ThermalPipeTile.java):**  
   - Impede contagem dupla quando uma fonte `IHeatSource` não-tubo é alcançada por múltiplos caminhos na mesma consulta raiz (`!(neighbor instanceof ThermalPipeTile) && !visited.add(neighborPos.toLong())`).
3. **Consider #7 — Varredura wireless restrita a chunks carregados:**  
   - [MonitorTile.java](../src/main/java/br/cropmanager/MonitorTile.java): `transferWirelessEnergy()` deixou de copiar `world.loadedTileEntityList` e agora consulta apenas `IChunkProvider.getLoadedChunk(cx, cz)` dentro de `monitorSupportWirelessRadius`.
4. **Consider #9 — Filtro de estado visível em `ManagerContainer`:**  
   - [ManagerContainer.java](../src/main/java/br/cropmanager/ManagerContainer.java): `sameVisibleState(...)` ignora variações exclusivas de `ReportAge` em `detectAndSendChanges()`, evitando reenvio do pacote `State` a cada tick quando a GUI está aberta.
5. **Consider #10 — Throttling de `markDirty()` em varreduras ociosas:**  
   - [ManagerTile.java](../src/main/java/br/cropmanager/ManagerTile.java) e [WeederTile.java](../src/main/java/br/cropmanager/WeederTile.java): marcam o chunk como sujo apenas a cada 20 ciclos durante varreduras sem alterações de inventário/tanques/crops (`dirtyTicks`).
6. **Testes de regressão adicionados:**  
   - [ReviewFixesRegressionTest.java](../src/test/java/br/cropmanager/ReviewFixesRegressionTest.java): 6 testes cobrindo linearidade/diamante de tubos térmicos, `canFillFluidType(null)`, bits obrigatórios em `ManagerTile.settings`, `MonitorTile.transferWirelessEnergy` por chunks carregados, `sameVisibleState` e throttling de `markDirty()`.  
   - [ContainerTest.java](../src/test/java/br/cropmanager/chest/ContainerTest.java): adicionado `remoteLinkedChestBlocksHotbarSwapOfLockedCarrierSlot` cobrindo `ClickType.SWAP` na GUI 13.

### Pendente (Não aplicado — escopo `0.5.0-alpha9`)
- Itens **Consider #8, #11, #12 e #13** (compactação dos pacotes `SPacketWindowProperty` no Baú de Dados, investigação da face `DOWN` com Ender IO 5.5.1 e refatorações estruturais de máquinas/itens de upgrade).

---

## Revisão dos Módulos Novos (`0.5.0-alpha10` — 2026-11-23)

**Escopo revisado (sem alterações de lógica):** Reservatório Agrícola (`AgriculturalReservoir*`, `Reservoir*`), Regadores (`SprinklerBlock`, `SprinklerTile`), Vidro de Estufa (`GreenhouseGlassBlock`), Colmeia Polinizadora (`PollinatorHiveBlock`, `PollinatorHiveTile`), Cruzamento/Mutagênico/Solo Radioativo (`CrossBreedingHelper`, `MutagenicPowderItem`, `RadioactiveSoilBlock`), Cultivos novos (`RadbloomCropCard`, `CropRegistry`, `BaseCropCard`), Guia Agrícola (`br/cropmanager/guide/*`) e Plugin JEI (`br/cropmanager/jei/*`).

### Achados por Severidade

1. **Critical: `POLLINATOR_HIVE` e `GREENHOUSE_GLASS` registrados sem receita de crafting**  
   - **Arquivo:** [CropManager.java](../src/main/java/br/cropmanager/CropManager.java#L49-L50) ([L203-L266](../src/main/java/br/cropmanager/CropManager.java#L203-L266))  
   - **Problema:** Ambos os blocos são registrados e constam no Guia Agrícola (`ManualRegistry`), mas nenhuma receita em `CropManager.recipes(...)` fabrica `POLLINATOR_HIVE` nem `GREENHOUSE_GLASS`, tornando-os inacessíveis no Survival.

2. **Critical: `ManualRegistry.search(String)` chama `I18n` cliente sem `@SideOnly(Side.CLIENT)`**  
   - **Arquivo:** [ManualRegistry.java](../src/main/java/br/cropmanager/guide/ManualRegistry.java#L460)  
   - **Problema:** `ManualRegistry` é uma classe comum sem `@SideOnly(Side.CLIENT)`, e o método `search(String)` referencia `net.minecraft.client.resources.I18n.format(...)` diretamente. Se inspecionado ou carregado no servidor dedicado, pode lançar `NoClassDefFoundError`.

3. **Obrigatório: Catalyst de estacas incorreto no plugin JEI**  
   - **Arquivo:** [CropManagerJeiPlugin.java](../src/main/java/br/cropmanager/jei/CropManagerJeiPlugin.java#L51)  
   - **Problema:** Chama `IC2Items.getItem("crop_stick")`, enquanto o IC2 Experimental `2.8.222-ex112` registra como `IC2Items.getItem("crop_res", "crop_stick")` (como feito em `ManualRegistry.java:25`). Como resultado, `sticks` fica vazio e o catalyst não é registrado.

4. **Obrigatório: `AgriculturalReservoirBlock.breakBlock` não desfaz `formed` nem notifica as cascas**  
   - **Arquivo:** [AgriculturalReservoirBlock.java](../src/main/java/br/cropmanager/AgriculturalReservoirBlock.java#L116-L123)  
   - **Problema:** Quando o bloco controlador é quebrado (incluindo via `harvestBlock`/`removedByPlayer`), `breakBlock` não limpa `formed` nem notifica os blocos `ReservoirCasingTile` da estrutura, podendo deixar blocos de casca presos visualmente no estado `TOWER` (invisíveis) no cliente.

5. **Obrigatório: `SprinklerBlock.onBlockActivated` não valida permissões de edição**  
   - **Arquivo:** [SprinklerBlock.java](../src/main/java/br/cropmanager/SprinklerBlock.java#L81)  
   - **Problema:** Permite esvaziar baldes/recipientes de água no regador sem verificar `player.canPlayerEdit(pos, facing, held)` e `world.isBlockModifiable(player, pos)`.

6. **Obrigatório: `ReservoirRenderer` lê `FACING` sem checar tipo do bloco**  
   - **Arquivo:** [ReservoirRenderer.java](../src/main/java/br/cropmanager/ReservoirRenderer.java#L34) ([L112](../src/main/java/br/cropmanager/ReservoirRenderer.java#L112))  
   - **Problema:** Chama `tile.getWorld().getBlockState(tile.getPos()).getValue(AgriculturalReservoirBlock.FACING)` sem verificar `state.getBlock() == CropManager.AGRICULTURAL_RESERVOIR`, podendo lançar `IllegalArgumentException` no cliente durante a remoção do bloco.

7. **Consider: Varredura síncrona `17×17×3` (867 posições) por pulso no `SprinklerTile` Mk3**  
   - **Arquivo:** [SprinklerTile.java](../src/main/java/br/cropmanager/SprinklerTile.java#L87-L111)  
   - **Problema:** Aloca 867 `BlockPos` em um único tick e chama `world.markChunkDirty(target, te)` para todo `TileEntityCrop` na área mesmo quando `hydrateCrop` não alterou água nem crescimento.

8. **Consider: Complexidade do `AgriculturalReservoirTile` (805 linhas) e aba em `ReservoirContainer`**  
   - **Arquivos:** [AgriculturalReservoirTile.java](../src/main/java/br/cropmanager/AgriculturalReservoirTile.java) e [ReservoirContainer.java](../src/main/java/br/cropmanager/ReservoirContainer.java#L56-L60)  
   - **Problema:** `transferToFarmTargets()` e `redstoneAllows()` reconstroem listas `shellPositions(...)` repetidamente; `ReservoirContainer.enchantItem(...)` permite alternar abas mesmo com item preso no cursor (`!player.inventory.getItemStack().isEmpty()`).

9. **Consider: Documentação e script de verificação desatualizados para `0.5.0-alpha10`**  
   - **Arquivos:** [README.md](../README.md) e [verify_artifact.py](../tools/verify_artifact.py#L5-L17)  
   - **Problema:** `README.md` ainda descreve `0.5.0-alpha9`, e `tools/verify_artifact.py` ainda aponta hardcoded para `CropManager-0.5.0-alpha8.jar` e apenas os 8 blockstates antigos.

- **Nit:** [ReservoirRenderer.java:116](../src/main/java/br/cropmanager/ReservoirRenderer.java#L116) — `GlStateManager.translate(tile.getPos().getX() - tile.getPos().getX(), ...)` resulta sempre em `(0, 0, 0)`.
- **Nit:** [PollinatorHiveBlock.java:80](../src/main/java/br/cropmanager/PollinatorHiveBlock.java#L80) — `randomDisplayTick` sem `@SideOnly(Side.CLIENT)`.

### Novo Código Morto Identificado (aguardando confirmação antes de remover)
- [ReservoirGuide.java](../src/main/java/br/cropmanager/ReservoirGuide.java) — livro estático antigo sem chamadores (substituído por `ItemAgriculturalManual`).
- [AgriculturalReservoirTile.java:414-417](../src/main/java/br/cropmanager/AgriculturalReservoirTile.java#L414-L417) — método privado `validateStructureAtCachedPosition()` sem chamadores.
- [LegacyRecoveryTile.java:16](../src/main/java/br/cropmanager/LegacyRecoveryTile.java#L16) — subclasse `LegacyRecoveryTile.Reservoir` não mais registrada em `CropManager.preInit`.

---

## Verificação Pós-Remoção de Código Morto e Build Final (`0.5.0-alpha10`)
- **Build + Testes:** `$env:GRADLE_USER_HOME = "$PWD\.gradle-home"; .\gradlew.bat build --offline -x downloadFernflower` → **`BUILD SUCCESSFUL`**
- **Testes unitários:** **32 suítes / 188 testes**, **0 falhas**, **0 erros**, **0 ignorados**.
- **Artefato gerado:** `build/libs/CropManager-0.5.0-alpha10.jar` (**674.165 bytes**)
  - **SHA-256:** `3c67ff7cc5a5b9029789108dad12b619c44f471b511c81560c7606f1a5c8f670`
  - **Inspeção:** **178 classes** (todas em bytecode Java 8 / major `52`), `mcmod.info` com `version: "0.5.0-alpha10"`, e ausência confirmada de `FarmDisplay.class`, `RuntimeChecks.class` e `ClientChecks.class`.

---

## Auditoria e Consolidação (`0.5.0-alpha10` — 2026-11-24)

### Correções Aplicadas
1. **Desempenho em `PollinatorHiveTile.update()`:** removido `world.markChunkDirty(pos, this)` por tick; substituído por `markDirty()` periódico a cada 20 ticks e ao concluir pulsos/consumir carga, evitando marcação contínua de chunk como sujo em colmeias ociosas.
2. **Isolamento `@SideOnly(Side.CLIENT)`:**
   - [GreenhouseGlassBlock.java](../src/main/java/br/cropmanager/GreenhouseGlassBlock.java): adicionado `@SideOnly(Side.CLIENT)` em `shouldSideBeRendered(...)`.
   - [UiNumbers.java](../src/main/java/br/cropmanager/UiNumbers.java): adicionada anotação `@SideOnly(Side.CLIENT)` nos métodos dependentes de `Minecraft.getMinecraft()` (`locale()`, `compact(double)`, `exact(double)`, `resource(...)`, `exactResource(...)`), preservando as sobrecargas com `Locale` explícito seguras para testes/servidor.
3. **Verificação de pendências anteriores (`0.5.0-alpha9` #7, #9, #10):** confirmado no código atual que `MonitorTile.transferWirelessEnergy()` já usa `getLoadedChunk(cx, cz)`, `ManagerContainer` já ignora `ReportAge` no diff de sincronização e `ManagerTile`/`WeederTile` já aplicam throttling de 20 ticks em `markDirty()`.

### Documentação Atualizada
- [README.md](../README.md): atualizado para `0.5.0-alpha10` com todos os novos blocos/itens (multiblocos de fluido/silo, `fluid_pipe`, `pollinator_hive`, `greenhouse_glass`, `agricultural_manual`, `sprinkler_mk1..3`, 10 cultivos e plugin HEI/JEI) e contagem de **35 suítes / 220 testes / 677 chaves i18n**.
- [CHANGELOG.md](../CHANGELOG.md): seção `[Não lançado]` consolidada com todos os novos sistemas do workspace.
- [docs/CONFIGURACAO.md](../docs/CONFIGURACAO.md): atualizado de `0.4.0-alpha1` para `0.5.0-alpha10` com todas as 22 categorias de `ManagerConfig.java`.
