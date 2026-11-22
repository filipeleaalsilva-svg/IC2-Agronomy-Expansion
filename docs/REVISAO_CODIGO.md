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
   - **Arquivos:** [ThermalPipeTile.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/ThermalPipeTile.java#L13-L38) e [GrinExtractorTile.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/GrinExtractorTile.java#L89-L93)  
   - **Problema:** Em `ThermalPipeTile.transfer(...)`, para cada vizinho `IHeatSource`, o tubo chama `source.requestHeat(sourceSide, remaining)` (linha 28) e imediatamente `source.drawHeat(sourceSide, available, simulate)` (linha 30). Como ambos `requestHeat` e `drawHeat` delegam para `transfer`, cada salto de tubo duplica o número de chamadas recursivas para o nó anterior. Em uma linha reta de $N$ tubos térmicos, um único pedido gera $\ge 2^N$ chamadas recursivas (com 20–25 tubos são milhões de `world.getTileEntity` por tick a partir do `GrinExtractorTile`), congelando o tick do servidor (DoS).  
   - **Efeito colateral em loops:** `visited.remove(pos.toLong())` no bloco `finally` (linha 36) remove o nó ao retroceder, permitindo que caminhos alternativos em malhas consultem a mesma fonte duas vezes durante `requestHeat` (contando calor dobrado). Com isso, `GrinExtractorTile` (linha 92) pode passar em `findReadyHeatSide`, drenar calor parcial em `drawHeatFrom` (`< heatCost`) e descartá-lo sem avançar o progresso.  
   - **Correção recomendada:** Propagar uma única travessia por consulta (ou usar `drawHeat(..., true)` apenas se não houver camada dupla de `requestHeat`), mantendo o conjunto de visitados pela duração inteira da busca raiz quando o objetivo for apenas sondagem sem drenagem.

2. **Critical: Ausência de `@SideOnly(Side.CLIENT)` em métodos de tooltip em classes comuns (crash em servidor dedicado)**  
   - **Arquivos:** [CropManager.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/CropManager.java#L57-L70), [FarmTool.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/FarmTool.java#L23-L30) e [FertilizerItem.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/FertilizerItem.java#L7-L8) ([L43-L46](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/FertilizerItem.java#L43-L46))  
   - **Problema:** `CropManager.infoItem` e `CropManager.materialItem` criam subclasses anônimas de `Item` (instanciadas estaticamente na inicialização da classe principal do mod) sobrescrevendo `addInformation(..., ITooltipFlag)` e chamando `net.minecraft.client.resources.I18n.format(...)` **sem** `@SideOnly(Side.CLIENT)`. O mesmo ocorre em `FarmTool.addInformation` e `FertilizerItem.addInformation` (enquanto `WirelessEuUpgrade`, `ItemUpgradeLinker` e `ItemPortableCropDatabase` declaram `@SideOnly(Side.CLIENT)` corretamente). No Forge 1.12.2 em servidor dedicado, métodos sem `@SideOnly(Side.CLIENT)` não são removidos pelo `SideTransformer`, provocando `NoClassDefFoundError` (`ITooltipFlag` / `I18n`) durante o carregamento/inspeção de classes no servidor.  
   - **Correção recomendada:** Anotar todos os `addInformation` com `@SideOnly(Side.CLIENT)` (e evitar imports diretos de `net.minecraft.client.*` fora de métodos anotados).

---

### Obrigatórios (Devem ser corrigidos antes de merge/release)

3. **Trava do slot portador ignorada em `ClickType.SWAP` no modo vinculado remoto (GUI 13)**  
   - **Arquivo:** [ContainerCropDatabaseChest.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/chest/inventory/ContainerCropDatabaseChest.java#L262-L266)  
   - **Problema:** As linhas 262 e 265 verificam `chest.isCarrierSlot(...)` em vez de `isLockedCarrierSlot(...)` (usado no restante da classe: linhas 96, 99, 100, 297, 300, 306). Ao abrir um baú remotamente via Database Portátil vinculado (GUI 13), `chest` é um `TileCropDatabaseChest` (cujo `isCarrierSlot` sempre retorna `false`), permitindo que teclas numéricas da hotbar (`ClickType.SWAP`) troquem o item vinculado de lugar dentro do inventário do jogador.  
   - **Correção recomendada:** Substituir `chest.isCarrierSlot(...)` por `isLockedCarrierSlot(...)` nas linhas 262 e 265.

4. **Tooltip de favorito sobrepõe o tooltip da semente e aparece em slots vazios**  
   - **Arquivo:** [GuiCropDatabaseChest.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/chest/client/GuiCropDatabaseChest.java#L263-L265)  
   - **Problema:** Logo após `renderHoveredToolTip(mouseX, mouseY)` (linha 254), o bloco das linhas 263–265 chama `drawHoveringText(I18n.format("cropdatabasechest.favorite.tip"), mouseX, mouseY)` sempre que o cursor está sobre qualquer slot da grade do baú (mesmo vazio!) e apenas quando `hasManagement()` é verdadeiro. Com isso, sobrepõe o tooltip da semente ocupada, aparece em slots vazios e deixa de exibir a dica no modo portátil (onde `toggleFavorite` funciona via linha 75 de `ContainerCropDatabaseChest`).  
   - **Correção recomendada:** Anexar a dica em `ItemTooltipEvent` ou apenas quando `hovered.getHasStack()` (combinando com as linhas existentes em vez de desenhar uma segunda caixa por cima) e incluir `container.isPortable()`.

5. **`NullPointerException` em `canFillFluidType(null)` nos tanques expostos via Capability**  
   - **Arquivos:** [ManagerTile.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/ManagerTile.java#L87-L96), [WeederTile.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/WeederTile.java#L25) e [MonitorTile.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/MonitorTile.java#L76-L82) ([L121](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/MonitorTile.java#L121))  
   - **Problema:** Os `FluidTank` de água, Weed-Ex e biogás chamam `fluid.getFluid() == ...` sem validar `fluid != null`. Como `ManagerTile` expõe `water.getTankProperties()[0]` / `weed.getTankProperties()[0]` diretamente, `WeederTile` expõe `gas` diretamente e `MonitorTile.properties()` delega direto para `tank.canFillFluidType(fluid)`, qualquer mod que consulte `IFluidTankProperties.canFillFluidType(null)` provocará NPE.  
   - **Correção recomendada:** Adicionar `fluid != null && ...` em todos os `canFillFluidType` (alinhando com `ComposterTile.java:36` e `FarmTile.java:85`).

6. **Pacote de rede `Settings` permite desabilitar bits de cuidado em `ManagerTile.functions`**  
   - **Arquivos:** [ManagerNetwork.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/ManagerNetwork.java#L42-L43) e [ManagerTile.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/ManagerTile.java#L317-L323)  
   - **Problema:** O mod removeu os botões individuais de fertilizante, água e Weed-Ex (mantendo os bits 1–3 sempre ligados via `| 14` em `readFromNBT` linha 380 e `control(201)` linha 429). Porém, `SettingsHandler` repassa `message.control` direto para `ManagerSettings.applyControl` e `tile.settings(...)` sem restringir `control == 0` nem preservar `| 14`, permitindo que um cliente adulterado desligue alertas de status (`NO_FERTILIZER`, `NO_WATER`, `NO_WEED`) ou zere `functions`.  
   - **Correção recomendada:** Restringir `SettingsHandler` ao controle de colheita (`control == 0`) e impor `| 14` dentro de `ManagerTile.settings(...)`.

---

### Consider (Arquitetura, Desempenho e Robustez)

7. **Consider: Varredura e cópia de `world.loadedTileEntityList` todo tick no Monitor**  
   - **Arquivo:** [MonitorTile.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/MonitorTile.java#L234-L245)  
   - **Problema:** Quando o Monitor tem Upgrade Wireless EU e energia no buffer não consumida pelo alvo principal, `transferWirelessEnergy()` copia todo `world.loadedTileEntityList` (`new ArrayList<>(...)`) e percorre todas as TileEntities da dimensão **todo tick** (20×/s por Monitor). Em servidores com muitas máquinas carregadas, gera pressão de GC e custo de CPU desnecessários.  
   - **Sugestão:** Iterar apenas `Chunk`s dentro de `SUPPORT_WIRELESS_RADIUS` ou manter cache atualizado a cada 20 ticks.

8. **Consider: Envio de 506 pacotes `SPacketWindowProperty` a cada atualização do Baú de Dados**  
   - **Arquivo:** [ContainerCropDatabaseChest.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/chest/inventory/ContainerCropDatabaseChest.java#L148-L159)  
   - **Problema:** `sendView` envia todos os 500 elementos de `positions` mais 6 propriedades de cabeçalho/rodapé via `sendWindowProperty` a cada tecla na busca, troca de página, ordenação ou inserção via funil.  
   - **Sugestão:** Consolidar o mapeamento de visão em um único `IMessage` compacto (ou enviar somente slots visíveis/alterados).

9. **Consider: Reenvio de `guiState()` todo tick para jogadores com GUI do Manager aberta**  
   - **Arquivos:** [ManagerContainer.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/ManagerContainer.java#L44-L49) e [ManagerTile.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/ManagerTile.java#L404)  
   - **Problema:** `guiState()` inclui `"ReportAge"`, que incrementa a cada tick após o primeiro censo completo. Assim `!state.equals(lastState)` é verdadeiro todo tick, reenviando todo o `NBTTagCompound Report` (incluindo a lista `Species`) 20×/s na rede enquanto a tela estiver aberta, embora `ManagerGui` não exiba `ReportAge`.  
   - **Sugestão:** Remover `ReportAge` de `guiState()` (ou arredondar em segundos / enviar apenas sob alteração de conteúdo).

10. **Consider: `markDirty()` chamado todo tick em varreduras ociosas**  
    - **Arquivos:** [ManagerTile.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/ManagerTile.java#L191) ([L199](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/ManagerTile.java#L199)), [WeederTile.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/WeederTile.java#L40)  
    - **Problema:** `ManagerTile` marca o chunk como sujo (`world.markChunkDirty`) todo tick enquanto está ligado varrendo (mesmo sem plantas) e `WeederTile` marca até 2×/tick. Isso força salvamentos frequentes de chunk.  
    - **Sugestão:** Usar throttling periódico (ex.: a cada 20 ticks, como em [ManualComposterTile.java:99](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/ManualComposterTile.java#L99)), mantendo `markDirty()` imediato apenas em mudanças de inventário/tanques/estado.

11. **Consider: Bloqueio da face inferior (`EnumFacing.DOWN`) no `AdvancedManagerTile`**  
    - **Arquivo:** [ManagerTile.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/ManagerTile.java#L328-L335)  
    - **Contexto:** Mitigação intencional da 0.5.0-alpha6 para evitar `ClassCastException` com conduítes de item do Ender IO 5.5.1 (já registrada em `CHANGELOG.md` e `VERIFICACAO.md`), mas impede extração inferior por funis e outros mods. Investigar a incompatibilidade específica do wrapper de `IItemHandler` continua recomendado para liberar as 6 faces.

12. **Consider: Duplicação entre máquinas de suporte e `EquipmentContainer`**  
    - **Arquivos:** [ComposterTile.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/ComposterTile.java), [GrinGrinderTile.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/GrinGrinderTile.java), [GrinExtractorTile.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/GrinExtractorTile.java), [WeedExFabricatorTile.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/WeedExFabricatorTile.java), [SeedAnalyzerTile.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/SeedAnalyzerTile.java) e [EquipmentContainer.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/EquipmentContainer.java#L17-L62)  
    - **Problema:** As cinco máquinas elétricas de suporte duplicam a lógica de `recalculateUpgrades()`, `exposed()`/`itemsForFace()` e serialização NBT de `EU`/`Work`/`Duration`, além de `EquipmentContainer` empilhar cadeias `instanceof` para layout de slots, validação e `transferStackInSlot`. Essa duplicação já gerou pequenas divergências (ex.: `ComposterTile.readFromNBT:124` não limita `progress` a `workTicks - 1`, e `ComposterTile.inventory` não sobrescreve `insertItem` para filtrar `isItemValid`).  
    - **Sugestão:** Extrair uma subclasse comum (ex.: `PoweredMachineTile` estendendo `FarmTile`) e delegar definição de slots/transferência para descritores por máquina.

13. **Consider: Upgrades do Monitor instanciados como subclasses anônimas**  
    - **Arquivo:** [CropManager.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/CropManager.java#L52-L70)  
    - **Problema:** `WEED_KILLER`, `WIRELESS_RANGE` e `HARVEST_TRANSFER` compartilham classe anônima inline em `CropManager.java` (observação pré-existente de `VERIFICACAO.md`), dificultando isolamento e contribuindo para o problema #2.

---

### Nit & FYI

- **Nit:** [EquipmentGui.java:340](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/EquipmentGui.java#L340) — Linha inalcançável `if(items.tagCount()==0)` após o retorno antecipado nas linhas 322–326.
- **Nit:** [ManagerBlock.java:100-119](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/ManagerBlock.java#L100-L119) — Usar wrench (sem agachar) no `THERMAL_PIPE` (`kind == 12`) altera a propriedade `FACING` (ignorada pelo modelo conectado do tubo) e consome 100 EU ou 1 de durabilidade da ferramenta. Além disso, máquinas com `kind < 10` (`ADVANCED`, `MONITOR`, `COMPOSTER`, `WEEDER`, `WEED_EX_FABRICATOR`) não suportam rotação/desmontagem por wrench.
- **Nit:** [WirelessEuUpgrade.java:104](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/WirelessEuUpgrade.java#L104) — `-(int) Math.ceil(accepted)` desconta 1 EU inteiro mesmo quando o receptor aceita frações `< 1.0 EU` ao completar o buffer.
- **Nit:** [ManagerTile.java:160](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/ManagerTile.java#L160) — Em `migrate()`, validar `old.getBlock() instanceof ManagerBlock` antes de `old.getValue(ManagerBlock.FACING)` evita `IllegalArgumentException` caso o bloco já tenha sido substituído externamente.
- **Nit:** [CropManager.java:198](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/CropManager.java#L198) — `required(...)` registra `INFO` para cada item IC2 consultado nas receitas.
- **FYI:** [ComposterTile.java:26](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/ComposterTile.java#L26), [GrinGrinderTile.java:21](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/GrinGrinderTile.java#L21), [GrinExtractorTile.java:28](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/GrinExtractorTile.java#L28), [WeedExFabricatorTile.java:30-31](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/WeedExFabricatorTile.java#L30-L31) e [MonitorTile.java:31-33](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/MonitorTile.java#L31-L33) mantêm constantes estáticas legadas (`DURATION`, `EU_PER_TICK`, `CAPACITY`, etc.) enquanto o comportamento real vem de `ManagerConfig.*` (apenas `GrinGrinderTile.DURATION` ainda é referenciada por `RuntimeChecks`).
- **FYI:** [ItemPortableCropDatabase.java:56](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/chest/item/ItemPortableCropDatabase.java#L56) e [TileCropDatabaseChest.java:99](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/chest/inventory/TileCropDatabaseChest.java#L99) usam `isBlockLoaded(pos)` (1 argumento) enquanto os demais arquivos usam `isBlockLoaded(pos, false)`.

---

## Código Morto Identificado (`DEAD CODE IDENTIFIED`)

Os elementos abaixo estão inalcançáveis ou sem uso após a remoção de funcionalidades antigas (como filtro genético e máquinas 2×1 de altura). **Nenhuma remoção foi feita**; confirme antes de excluir:

- [FarmDisplay.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/FarmDisplay.java) — classe utilitária de diagnóstico sem nenhum chamador em `src/`.
- [AreaPreview.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/AreaPreview.java#L35-L44) (e linhas [L62-L63](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/AreaPreview.java#L62-L63)) — `markers()`, `marker()` e leitura de `"Rejected"` / `"Unknown"` de `report` (chaves nunca mais geradas em `ManagerTile`).
- [ManagerBlock.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/ManagerBlock.java#L25) (`HALF`, `MachineHalf`, `isTwoHigh() { return false; }`, `isUpper`, `hasRoomForUpper`, `basePos` nas linhas 25, 32, 44, 50–52, 72, 75, 79, 81, 85, 89–90, 112–115, 139–142; além dos casos `kind == 2, 5, 6, 7` na linha 123 e `kind != 5` na linha 39) — restos de blocos antigos removidos ou convertidos para 1 bloco de altura.
- [MonitorTile.java:383](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/MonitorTile.java#L383) — encaminha ações `202..204` que não têm mais tratamento em `ManagerTile.control`.

*(Nota: `LegacyMigration`, `LegacyRecoveryTile`, `LegacyModularChestTile`, `TileCropDatabaseChest125`, `TileCropDatabaseChest250` e o modo básico de `ManagerTile` são código ativo de migração/compatibilidade de mundos antigos e não são código morto.)*

→ **Posso remover os elementos de código morto listados acima em um próximo passo?**

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
   - [ThermalPipeTile.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/ThermalPipeTile.java): `transfer(...)` agora controla a consulta raiz com `boolean root = visited.isEmpty()`, remove a chamada duplicada a `source.requestHeat(...)` (propagando apenas `source.drawHeat(..., simulate)`) e limpa `visited` somente ao retornar da chamada raiz (`if (root) visited.clear()`), evitando re-visita em malhas e explosão exponencial.
2. **Anotação `@SideOnly(Side.CLIENT)` em tooltips de classes comuns — Critical #2:**  
   - [CropManager.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/CropManager.java), [FarmTool.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/FarmTool.java) e [FertilizerItem.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/FertilizerItem.java): `addInformation` anotado com `@SideOnly(Side.CLIENT)`.
3. **Bloqueio de `ClickType.SWAP` para slot portador no modo remoto (GUI 13) — Obrigatório #3:**  
   - [ContainerCropDatabaseChest.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/chest/inventory/ContainerCropDatabaseChest.java): `slotClick` atualizado para checar `isLockedCarrierSlot(...)` em vez de `chest.isCarrierSlot(...)`.
4. **Tooltip de favorito sem sobreposição nem exibição em slots vazios — Obrigatório #4:**  
   - [GuiCropDatabaseChest.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/chest/client/GuiCropDatabaseChest.java): dica `cropdatabasechest.favorite.tip` exibida apenas quando o slot contém item (`hovered.getHasStack()`), com `Shift` pressionado (`isShiftKeyDown()`), abrangendo também o modo portátil (`container.isPortable()`).
5. **Proteção contra `null` em `canFillFluidType(FluidStack)` — Obrigatório #5:**  
   - [ManagerTile.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/ManagerTile.java), [MonitorTile.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/MonitorTile.java), [WeederTile.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/WeederTile.java), [GrinExtractorTile.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/GrinExtractorTile.java) e [WeedExFabricatorTile.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/WeedExFabricatorTile.java): adicionada checagem `fluid != null && ...`.
6. **Validação estrita do pacote `Settings` e máscara de `functions` — Obrigatório #6:**  
   - [ManagerNetwork.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/ManagerNetwork.java) (`SettingsHandler` aceita apenas `message.control == 0`) e [ManagerTile.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/ManagerTile.java) (`settings(...)` força `(newFunctions & 1) | 14`).
7. **Nits aplicados:**  
   - [EquipmentGui.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/EquipmentGui.java): removida linha inalcançável `if(items.tagCount()==0)` em `drawHarvestList`.  
   - [ManagerTile.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/ManagerTile.java): `migrate()` verifica `old.getBlock() instanceof ManagerBlock` antes de ler `FACING`.  
   - [ManagerBlock.java](file:///c:/Users/Leal/Documents/Leal/Antigravity%20Projects/Crop%20Manager/src/main/java/br/cropmanager/ManagerBlock.java): `THERMAL_PIPE` (`kind == 12`) sem agachar ignora rotação com wrench sem gastar energia/durabilidade.

### Pendente (Não aplicado)
- Itens **Consider #7–#13** (otimizações de varredura/rede/NBT e refatorações estruturais de máquinas de suporte).
- Remoção do **código morto** listado acima (`FarmDisplay`, marcadores `"Rejected"`/`"Unknown"` em `AreaPreview`, resquícios `isTwoHigh`/`MachineHalf` em `ManagerBlock`, ações `202..204` em `MonitorTile`).

### Verificação Pós-Correção
- Executado `$env:GRADLE_USER_HOME = "$PWD\.gradle-home"; .\gradlew.bat test --offline -x downloadFernflower`: **`BUILD SUCCESSFUL`**, **25 suítes / 135 testes**, **0 falhas**, **0 erros**, **0 ignorados**.
