# Changelog

Todas as mudanças notáveis neste projeto serão documentadas neste arquivo.

O formato é baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/)
e este projeto adere ao [Versionamento Semântico](https://semver.org/lang/pt-BR/).

## [0.5.0-alpha.11] - 2026-11-25

### Adicionado
- **Câmara Hidropônica (`hydroponic_chamber`)** — máquina Tier 2 EU (`50.000 EU`, `16 EU/t`, tanque de `16.000 mB` de água) que cultiva Sacos de Sementes IC2 em ambiente fechado via `VirtualCropTile` (contendo radiação de `radbloom`). Exige `greenhouse_glass` diretamente acima e subsolo válido abaixo (`dirt`/`farmland`/`grass`, ou `radioactive_soil` para `radbloom`), com bônus opcionais de cúpula `3×3` (`+25%`) e fonte de calor IC2 (`+50%`), automação segura (preserva a Seed Bag) e suporte a Fertilizante Catalítico e upgrades IC2.
- **Gerador a Biogás Agrícola (`biogas_generator` + `biogas_generator_casing`)** — estrutura multibloco oca `3×3×3` que queima Biogás IC2 (`16.000 mB`) para gerar e emitir até `32 EU/t` pela face frontal.
- **Queimador Térmico a Biogás (`biogas_heat_burner`)** — fonte térmica (`IHeatSource`) alimentada por Biogás IC2 entregando até `32 HU/t` sob demanda pela face frontal.
- **Pulverizador Agrícola a Biogás (`biogas_sprayer`)** — aplica água (`25 mB` + `1 mB` de biogás) e fertilizantes em área `9×9` ao redor usando Biogás IC2.
- **Regador Diagonal (`sprinkler_diagonal`)** — regador `19×19` com 4 quadrantes `9×9` independentes (NO, NE, SO, SE) selecionáveis e 3 modos de redstone.
- **Ferramenta de Cruzamento (`crossing_tool`)** — vincula estacas duplas vazias, armazena estoque interno em 3 slots e colhe/rearma o cruzamento automaticamente com 1 clique direito.
- **Rede Wi-Fi de EU (`eu_wifi_plug`, `eu_wifi_point`) e Antena Parabólica de EU (`eu_dish_antenna`)** — transmissão sem fio por canais compartilhados (públicos/privados com perda configurável entre dimensões) e transmissão em área para máquinas com `wireless_eu_upgrade`.
- **5 Novos Crops IC2 (`resincane`, `cryolotus`, `cupric_moss`, `stannum_herb`, `voltreed`)** — expandindo a linha para `15` espécies customizadas integradas ao HEI/JEI e ao Manual Agrícola.
- **Blocos Decorativos e Luminárias (`greenhouse_frame`, `garden_trellis`, `farm_lantern`, `wooden_sconce`, `industrial_lantern`, `glass_lamp`)** — iluminação nível `15` montável em chão, parede ou teto e acabamentos para estufas.
- **Reservatório Agrícola (`agricultural_reservoir`) e Carcaça (`reservoir_casing`)** — controlador de estrutura multibloco `3×5×3` com dois tanques independentes, filtros por fluido, suporte a recipientes e transferência automática com energia IC2.
- **Tanque Industrial de Fluido (`fluid_tank`) e Carcaça (`fluid_tank_casing`)** — multibloco `3×5×3` sem consumo de EU para armazenamento em massa (`128.000 mB` base + até 8 upgrades de capacidade), portas por face e auto-extração por redstone.
- **Silo Agrícola (`agricultural_silo`) e Carcaça (`silo_casing`)** — multibloco `3×5×3` sem consumo de EU para armazenamento em massa de um tipo de item (`128.000` base + até 8 upgrades de capacidade), trava de item e auto-extração por redstone.
- **Tubos de Fluido (`fluid_pipe`)** — logística de fluidos sem EU com equalização entre tanques e distribuição round-robin.
- **Colmeia Polinizadora (`pollinator_hive`) e Vidro de Estufa (`greenhouse_glass`)** — aceleração de cruzamentos em estacas duplas (com carga de **Pó Mutagênico**) e bônus de crescimento por random tick sob vidro de estufa.
- **Manual Agrícola (`agricultural_manual`)** — guia ilustrado in-game com busca, navegação por categorias e páginas detalhadas.

### Alterado
- **Identidade do Mod:** nome de exibição atualizado para **IC2: Agronomy Expansion** (`CropManager.NAME`) com aba criativa dedicada (`CropManager.TAB`) e artefato `IC2-Agronomy-Expansion-0.5.0-alpha11.jar`.
- **Regadores Agrícolas (`sprinkler_mk1..3`)** — adição de 3 modos de redstone alternáveis via *Agachar + Clique Direito*.
- **Capinador a Biogás** — cada erva daninha removida rende um item **Weed** do IC2 em uma saída dedicada, acessível por conduítes. Se a saída estiver cheia, a erva permanece na estaca de cultivo.
- **Composteiras Manual e Elétrica** — aceitam **Weed** do IC2 como insumo orgânico.

### Corrigido
- **Manual Agrícola (`ManualRegistry`)** — corrigido texto duplicado em `biogas_heat_burner`, `biogas_generator`, `biogas_generator_casing`, `biogas_sprayer` e `sprinkler_diagonal`, movendo-os para as categorias corretas (`Máquinas` e `Irrigação`).
- **Empacotamento do JAR** — removido `items.zip` residual dos recursos do mod (`src/main/resources/assets/cropmanager/textures/`).
- **Baú de Dados de Cultivos** — usa texturas próprias nas seis faces.
- **Interface da Ferramenta de Cruzamento (`CrossingToolGui`)** — atualizada para o tema escuro padrão `#1C1C1D`.
- **`EuDishContainer` e `EuWifiContainer`:** registro de `EuDishContainer` em `FarmTile.openMachine` para sincronização de estado e botão redstone na GUI da Antena Parabólica; transferência Shift-Clique no slot de fertilizante do Pulverizador a Biogás.
- **Plugin HEI/JEI (`CropManagerJeiPlugin` / `CropRecipeCategory`):** lookup corrigido para `IC2Items.getItem("crop_stick")` e nome do mod sincronizado com `CropManager.NAME`.
- **Multibloco do Gerador a Biogás (`BiogasGeneratorTile` / `AgriculturalMachineBlock`) e Queimador Térmico (`BiogasHeatBurnerTile`):** desvinculação imediata das cascas em `casingBroken` e `breakBlock`, preservação do estado `formed` em chunks descarregados (`DEFER`), validação de carcaça ao posicionar o bloco, preservação de `BlockEntityTag` no drop e sinal de comparador redstone proporcional à energia/gás.
- **Multiblocos de Armazenamento e Logística (`ReservoirCasingBlock`, `StorageMultiblockSupport`, `IndustrialTankTile`):** delegação de sinal de comparador nas carcaças do Reservatório Agrícola e saneamento de tags NBT de tanques (`readFluidTank`) prevenindo perda de fluidos após mesclagem de itens.
- **Rede Wi-Fi de EU (`WirelessEuGridData`, `WirelessEuUpgrade`, `EuWifiPointTile`):** bloqueio de alteração de privacidade de canais públicos pertencentes a outro jogador, conservação de EU e preservação de `lastSourceDim` ao atender máquinas cheias em outra dimensão, atualização de `EnergyNet` ao trocar Tier, ciclagem completa de taxa e guarda contra `NaN` em `drawEnergy`.
- **Renderers (`TESR`) e Traduções:** verificação do bloco antes de ler `FACING` em `AgriculturalSiloRenderer`, `IndustrialTankRenderer`, `BiogasGeneratorRenderer`, `MonitorRenderer` e `CropChestRenderer`; adição das 6 chaves de localização faltantes do Silo Agrícola em `en_us.lang` e `pt_br.lang`.
- **Testes de Regressão:** expansão abrangente das suítes de teste para o Reservatório Agrícola, Silo Agrícola, Gerador a Biogás, Tubos de Fluido e Tanque Industrial.

## [0.5.0-alpha.10] - 2026-11-21

### Adicionado
- **Composteira Manual (`manual_composter`)**: bloco 3D de early-game sem consumo de EU, construído com ripas de madeira e cantoneiras de aço IC2 com frestas de ventilação e 6 estados visuais dinâmicos (`fill=0..5`, de matéria verde fresca até húmus pronto). Interação direta com clique direito no mundo (`Shift` preenche em lote), suporte a hidratação com balde/frasco de água para dobrar a velocidade (2×) e produção de **Composto Orgânico**.
- **Novos Fertilizantes (`organic_compost` e `catalytic_fertilizer`)**:
  - **Composto Orgânico**: fertilizante inicial obtido na Composteira Manual; repõe nutrientes (`+60`) e umidade (`+40`) em crops do IC2.
  - **Fertilizante Catalítico**: produzido a partir de Composto Orgânico, Essência Nitro e Grin Powder; repõe nutrientes (`+100`), concede bônus de crescimento (`+40` growth points) e aplica cargas de **Solo Catalisado** (`cm_catalyzed`).
- **Analisador de Sementes (`seed_analyzer`)**: máquina Tier 1 EU (20.000 EU, 5 EU/t, 100 ticks) com 4 slots de upgrades IC2 (`Overclocker`, `Energy Storage`, `Transformer`, `Wireless EU`), slot de bateria, animação frontal em 4 frames e interface com visor OLED de telemetria genética (espécie, `GR / GA / RE`, Tier, selo **Catalisado** e atributos). Analisa Seed Bags em lote até o nível `4/4` e expõe `IItemHandler` seguro para automação direta com o Baú de Dados de Cultivos.
- **Regadores Agrícolas em 3 Níveis (`sprinkler_mk1`, `sprinkler_mk2`, `sprinkler_mk3`)**: blocos 3D sem consumo de EU que funcionam automaticamente sobre um bloco de água fonte ou abastecidos por balde/tubos (`CapabilityFluidHandler`), umedecendo terra arada (`moisture=7`), hidratando estacas IC2 e acelerando o crescimento (`growthPoints`):
  - **Mk1**: área `9×9` (raio 4), velocidade `1×`, tanque `4.000 mB`.
  - **Mk2**: área `9×9` (raio 4), velocidade `4×`, tanque `8.000 mB`.
  - **Mk3**: área `17×17` (raio 8 — 4× a área), velocidade `4×`, tanque `16.000 mB`.
- **Plugin Opcional de HEI / JEI (`CropManagerJeiPlugin`)**: separa todas as `ic2:crop_seed_bag` por espécie (`owner:id`) quando nenhum outro addon registra o intérprete, tornando todas as sementes pesquisáveis no HEI, e adiciona a categoria **Crop Manager: Cultivos** exibindo drops, Tier, exigência de Fertilizante Catalítico e subsolo obrigatório.
- **10 Novos Crops IC2 (Suporte Agrícola, Catalisados, Luminosos e Radioativo)** registrados via `Crops.CropRegisterEvent` (controláveis por `crops.enableCustomCrops` na configuração):
  - **Erva-Grin (`grinbloom`)**: alternativa agrícola para produção de Grin Powder IC2.
  - **Cana-Biomassa (`bioreed`)**: produz **Bio-Fibra** (`bio_fiber`), aceita nas composteiras, plantável como semente base e conversível em *Plant Ball* IC2.
  - **Nitro-Bulbo (`nitrobulb`)**: produz **Essência Nitro** (`nitro_essence`) para síntese do Fertilizante Catalítico.
  - **Lúmen-Flor (`glowbloom`)**: produz `Glowstone Dust` e emite luz natural (`6` no estágio 3, `12` no estágio 4).
  - **Erva-Enxofre (`sulfurbud`)**: produz pó pequeno de enxofre (`small_sulfur`) do IC2.
  - **Flor Lazúli (`lazuli_bloom`)**, **Quartzo-Cana (`quartzreed`)**, **Lítio-Vinha (`lithivine`)** e **Musgo de Irídio (`irid_moss`)**: **Crops Catalisados** (Tiers 5–8) que exigem Fertilizante Catalítico a partir do estágio 2 (consumindo 1 carga por colheita) para produzir `small_lapis`, `Nether Quartz`, `small_lithium` e `iridium_shard`.
  - **Erva-Rádio (`radbloom`)** + **Terra Radioativa (`radioactive_soil`)**: Crop Catalisado radioativo (Tier 7) que exige o bloco **Terra Radioativa** abaixo da terra arada, emite brilho verde e **Radiação real do IC2** quando madura (respeitando *Hazmat Suit*) e dropa `Tiny Pile of Uranium-238` (`small_uranium_238`).
- Novas interfaces `equipment_analyzer.png` (512×512 nos temas claro e escuro) e script gerador `tools/generate_expansion_assets.py` para texturas 16×16, modelos 3D e 20 sprites de estágios de crops.
- Novas seções de configuração em `cropmanager.cfg` (`manual_composter`, `fertilizers`, `seed_analyzer` e `crops`).
- Novas suítes de testes unitários (`ManualComposterTest`, `FertilizerAndCropTest`, `SeedAnalyzerTest`) e expansão do `AssetIntegrityTest` (totalizando 25 suítes, 135 testes e paridade estrita de 393 chaves entre `en_us.lang` e `pt_br.lang`).

### Alterado
- **Advanced Crop Manager e Monitor Agrícola**: `ManagerTile.isFertilizer()` e `visit()` passaram a aceitar e aplicar automaticamente **Composto Orgânico** e **Fertilizante Catalítico**.
- **Composteira Elétrica**: `ComposterTile.organic()` agora aceita **Bio-Fibra** e **Essência Nitro** como insumos orgânicos.
- **Sonda Agrícola (`agricultural_probe`)**: painel de inspeção atualizado para exibir também o estado e as cargas restantes de **Solo Catalisado**.

## [0.5.0-alpha.9] - 2026-11-18

### Adicionado
- Upgrade Wireless EU universal para IC2: implementa `IUpgradeItem` e opera em máquinas do IC2 que aceitam upgrades e nas máquinas do próprio mod. Vinculação via Shift + Clique Direito em armazenadores IC2 (`BatBox`, `CESU`, `MFE`, `MFSU`).
- Quatro slots de upgrade IC2 (`Overclocker`, `Energy Storage`, `Transformer`, `Wireless EU`) no Moedor de Grin, Extrator de Grin e Misturador de Weed-Ex.
- Slots de bateria IC2 com descarregamento automático nas interfaces gráficas da Composteira Elétrica, Moedor de Grin, Extrator de Grin e Misturador de Weed-Ex.
- Sistema de favoritos no Baú de Dados de Cultivos e Database Portátil, com botão de estrela para filtragem e atalhos Shift+R ou clique do meio sobre a semente.
- Vinculação direta do Database Portátil via Shift + Clique Direito no Baú de Dados de Cultivos.
- Documento de diretrizes visuais `docs/TEXTURE_GUIDELINES.md`, atlas de widgets `ic2_widgets.png` e kit de extração de assets em `design_kit/`.
- Testes de regressão automatizados para verificação de capabilities nas 6 faces, bloqueio de duplicação em slots de semente e paridade estrita de 362 chaves entre `en_us.lang` e `pt_br.lang`.
- Documentação formal em Javadoc das regras de vínculo wireless (dimensão, alcance, chunk carregado e desvinculação segura).

### Alterado
- Composteira Elétrica: receita ajustada para 4 itens orgânicos + 1.000 mB de água produzindo 1 fertilizante IC2, com tanque de água na interface e medidor de progresso dinâmico.
- Moedor de Grin e Extrator de Grin: convertidos para blocos cúbicos únicos (1 bloco), com animações frontais `.mcmeta` sem interpolação durante operação ativa.
- Tubo Térmico (`thermal_pipe`): modelo tridimensional reformulado em formato multipart conectado de cobre com conexões automáticas ortogonais.
- Advanced Crop Manager e Monitor Agrícola: remoção de filtros genéticos obsoletos e simplificação da botonaria para ícones padronizados (Ligado/Desligado, Redstone e Colheita) com tooltips detalhados e tela digital simplificada.
- Baú de Dados de Cultivos e Database Portátil: interface compacta 5×5 com campo de busca integrado e alinhamento visual refinado.
- Upgrades customizados (`weed_killer_upgrade`, `wireless_range_upgrade`, `harvest_transfer_upgrade`, `wireless_eu_upgrade`): texturas redesenhadas com códigos cromáticos e simbologia distintos.

### Corrigido
- Recepção térmica no Extrator de Grin: verificação corrigida para aceitar calor de fontes `IHeatSource` em todas as 6 faces (Gerador de Calor Elétrico ou Tubo Térmico).
- Registro do sprite de fluido `grin_extract` no evento `TextureStitchEvent.Pre`, eliminando textura quadriculada roxa/preta em integrações visuais.
- Bloqueio de divisão de pilhas com clique direito e clonagem com clique do meio no modo criativo nos slots do Baú de Dados de Cultivos.
- Correção de posicionamento horizontal do painel lateral retrátil no Advanced Crop Manager (`guiLeft` corrigido para utilizar `xSize`).

## [0.5.0-alpha.7] - 2026-11-17

### Adicionado
- Baú de Dados de Cultivos com capacidade para 500 sacos de sementes IC2 e Database Portátil.
- Visualização de genes (`growth`, `gain`, `resistance`), busca textual instantânea e paginação.

### Corrigido
- Tratamento de calor nas 6 faces do Extrator de Grin.

## [0.5.0-alpha.6] - 2026-11-16

### Corrigido
- Mitigação de incompatibilidade com conduítes de itens do Ender IO (série 5.5.x Cleanroom): desativação da exposição de `ITEM_HANDLER_CAPABILITY` na face inferior (`EnumFacing.DOWN`) do Advanced Crop Manager, prevenindo exceção `ClassCastException`. As demais 5 faces permanecem disponíveis para automação.

## [0.5.0-alpha.5] - 2026-11-15

### Alterado
- Mecanismo de migração segura para mundos antigos com blocos legados descontinuados. A substituição ocorre adiada no tick do servidor (`LegacyMigration`), descarregando inventários no mundo sem corrupção de chunks.

## [0.5.0-alpha.4] - 2026-11-15

### Adicionado
- Interfaces estáticas em PNG com estética industrial clássica cinza do IC2 para todas as máquinas.
- Pacote de recursos opcional para tema escuro (`CropManager-GUI-Dark`).
- Integração de molduras de tanques de fluido e indicadores nos fundos estáticos.

## [0.5.0-alpha.3] - 2026-11-14

### Adicionado
- Linha completa de síntese de Weed-Ex: Moedor de Grin, Lâmina de Moagem com desgaste (256 usos), Extrator de Grin, Misturador de Weed-Ex e Tubo Térmico.
- Capacidades Forge de fluidos e itens para automação da linha de produção via funis e tubulações externas.

## [0.5.0-alpha.2] - 2026-11-14

### Alterado
- Censo ambiental com leitura de dados reais de lavoura (nutrientes do solo, umidade, ventilação e iluminação).
- Separação estrita entre sementes analisadas e desconhecidas.

### Removido
- Coletor Agrícola e Reservatório Agrícola, consolidados na arquitetura do Crop Manager e Monitor Agrícola.

## [0.5.0-alpha.1] - 2026-11-14

### Adicionado
- Capinador a Biogás com modelo 3D dedicado e consumo de Biogás IC2 (16.000 mB de capacidade, 1 mB por varredura e 20 mB por erva removida).
- Controle remoto do Advanced Crop Manager pelo Monitor Agrícola, incluindo regras de automação por condições e trava de histerese de 100% de Weed-Ex.
- Área de operação tridimensional configurável de 3×3 a 17×17 em até 3 camadas verticais.

### Removido
- Classificador de Sementes, Bancada de Sementes, Caderno Agrícola e Cartão de Configuração (substituídos por recursos internos das máquinas avançadas).

## [0.4.0] - 2026-11-14

### Adicionado
- Versões preliminares de Coletor Agrícola e Reservatório Agrícola (32.000 mB).
- Algoritmo de contraste dinâmico adaptativo para tipografia sobre texturas escuras.

### Alterado
- Migração automática das versões iniciais básicas do Crop Manager para modelos avançados.

## [0.3.0] - 2026-11-14

### Adicionado
- Primeira versão da Composteira Elétrica para produção de fertilizante IC2.
- Seis modos operacionais de alarme redstone no Monitor Agrícola.

## [0.2.0] - 2026-11-14

### Adicionado
- Advanced Crop Manager (Tier 2, 100.000 EU de capacidade).
- Monitor Agrícola com display de dados no mundo.
- Sonda Agrícola para inspeção de cultivos.
- Suporte a internacionalização com pacotes de idioma em inglês (`en_us`) e português do Brasil (`pt_br`).

## [0.1.0] - 2026-11-14

### Adicionado
- Primeiro bloco Crop Manager integrado (20.000 EU, tanques de 20.000 mB para água e Weed-Ex, 26 slots de inventário).
- Suporte a upgrades IC2 clássicos: Overclocker, Transformer e Energy Storage.

### Corrigido
- Resolução de condições de corrida em cliques rápidos nas interfaces gráficas.
- Tratamento para valores não finitos (`NaN` e infinito) salvos em tags de energia no NBT.
