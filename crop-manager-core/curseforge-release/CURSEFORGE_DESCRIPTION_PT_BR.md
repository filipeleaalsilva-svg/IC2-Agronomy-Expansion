# Crop Manager Core

**Crop Manager Core** é uma suíte completa de automação e logística agrícola para **Minecraft 1.12.2** e **IndustrialCraft 2 Experimental** (`ic2-2.8.222-ex112`).

O mod reúne cuidados com lavouras, colheita, diagnóstico ambiental, remoção de ervas daninhas, produção de fertilizante, síntese de Weed-Ex, distribuição de energia EU sem fio e armazenamento de alta capacidade para sementes do IC2 em um sistema industrial coeso.

---

## Requisitos

- **Minecraft:** `1.12.2`
- **Mod Loader:** Minecraft Forge (ou Cleanroom 1.12.2)
- **Dependência Obrigatória:** **IndustrialCraft 2 Experimental** (`ic2@[2.8.222-ex112]`)
- **Java:** Java 8+

---

## Máquinas e Blocos

### 1. Advanced Crop Manager (Gerenciador Agrícola Avançado)
Máquina completa para cuidados e colheita de cultivos IC2:
- **Automação Unificada:** Executa colheita, fertilização, hidratação e aplicação de Weed-Ex em uma única varredura.
- **Área 3D Configurável:** Raio ajustável de `1` (`3×3`) até o limite do servidor (`8` por padrão = `17×17`) em até 3 camadas verticais, com visualização local da área.
- **Especificações:** Tier 2 (`100.000 EU`), tanque de `20.000 mB` de Água e `20.000 mB` de Weed-Ex.
- **Upgrades Compatíveis:** Overclocker, Energy Storage, Transformer, **Weed Killer Upgrade** e **Wireless EU Upgrade**.
- **Diagnóstico e Censo:** Exibe contagem de plantas, maduras, ervas, não analisadas e leituras reais do ambiente (nutrientes, umidade, ventilação e luz).

### 2. Agricultural Monitor (Monitor Agrícola)
Central de telemetria, controle remoto e logística sem fio:
- **Vínculo Sem Fio:** Vincule ao Advanced Crop Manager, Crop-Matron do IC2 ou Electric Weed Harvester usando a **Agricultural Probe** (alcance base de `32` blocos, expansível com **Wireless Range Upgrades**).
- **Controle Remoto e Automação:** Ligue/desligue funções remotamente e configure regras de parada automática por nível de Água e Weed-Ex (com trava de retomada em 100% de Weed-Ex).
- **Logística Sem Fio:** Transfere fluidos, fertilizante, colheitas (com **Harvest Transfer Upgrade**) e distribui **EU sem fio** para máquinas vinculadas.
- **Saída de Redstone:** Desligada, Alarme (sinal 15 em falhas/pausas) ou Normal.

### 3. Electric Weed Harvester (Capinador Elétrico)
Máquina dedicada à remoção elétrica de ervas daninhas do IC2 sem danificar cultivos válidos. Possui 4 slots de saída para as ervas coletadas, slot de bateria e 4 slots de upgrade IC2.

### 4. Electric Composter (Composteira Elétrica)
Converte matéria orgânica + água (`4` itens orgânicos + `1.000 mB` de Água, ou `2` ervas daninhas IC2 + `500 mB` de Água) em **Fertilizante IC2**.

### 5. Weed-Ex Mixer (Misturador de Weed-Ex — 3 Tanques)
Linha de síntese automatizada de **Weed-Ex**:
- Converte **Olho de Aranha** + Água em Extrato de Grin (`grin_extract`) e **Redstone** + Água em Redstone Líquida (`liquid_redstone`), misturando ambos em **IC2 Weed-Ex**.
- Compatível com recipientes (baldes/células) e tubulações via Forge Fluid Capabilities.

### 6. Crop Database Chest (Baú de Dados de Cultivos — 500 Slots)
Armazenamento especializado para **sacos de sementes do IC2**:
- **500 slots** com paginação, busca instantânea e sistema de **Favoritos** (`Shift+R` ou clique do meio).
- Exibe genética completa (`Growth`, `Gain`, `Resistance`) e espécie.

---

## Itens e Upgrades

- **Portable Crop Database (500 Slots):** Acesso portátil ao seu Baú de Dados (`Shift + Clique Direito` no baú para vincular).
- **Agricultural Probe:** Inspeciona cultivos IC2 e vincula máquinas ao Monitor Agrícola.
- **Upgrade Linker:** Vincula e configura alvos de upgrades sem fio.
- **Wireless EU Upgrade:** `IUpgradeItem` universal para máquinas do mod e máquinas compatíveis do IC2 (`Shift + Clique Direito` em `BatBox`, `CESU`, `MFE` ou `MFSU`).
- **Weed Killer Upgrade:** Permite ao Advanced Crop Manager remover ervas daninhas consumindo `20 mB` de Weed-Ex por erva.
- **Wireless Range Upgrade:** Aumenta o alcance sem fio do Monitor Agrícola.
- **Harvest Transfer Upgrade:** Habilita transferência sem fio dos itens colhidos para o Monitor.

---

## Resource Pack Opcional (Tema Escuro)

O pacote de recursos **`CropManagerCore-GUI-Dark-0.5.0-alpha9.zip`** está disponível na seção **Additional Files** para quem prefere interfaces industriais escuras.

---

## Instalação

1. Instale o **Minecraft 1.12.2** com **Forge** (ou Cleanroom) e **IndustrialCraft 2 Experimental (`2.8.222-ex112`)**.
2. Coloque `CropManagerCore-0.5.0-alpha9.jar` na pasta `mods/`.
3. *(Opcional)* Coloque `CropManagerCore-GUI-Dark-0.5.0-alpha9.zip` em `resourcepacks/` e ative no jogo.
4. *(Recomendado)* Faça backup do mundo antes de atualizar versões alpha.
