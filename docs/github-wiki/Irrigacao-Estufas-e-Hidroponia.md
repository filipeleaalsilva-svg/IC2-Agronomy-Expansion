# Irrigação, Estufas e Hidroponia

Esta página detalha os sistemas de hidratação passiva, aceleração de crescimento em estufas, cultivo fechado na **Câmara Hidropônica** e blocos decorativos com iluminação.

---

## 1. Regadores Agrícolas (Mk1, Mk2, Mk3)
*Nomes em inglês: **Sprinkler Mk1 / Mk2 / Mk3** · IDs: `cropmanager:sprinkler_mk1`, `sprinkler_mk2`, `sprinkler_mk3`*

Blocos 3D que **não consomem EU**. Hidratam terra arada (`moisture = 7`), hidratam estacas de cultivo IC2 (`+25` de hidratação por pulso) e aceleram o crescimento de plantas jovens (`+15` pontos base).

| Modelo | Área Horizontal | Camadas | Velocidade | Tanque Interno |
|---|---:|---:|---:|---:|
| **Regador Agrícola Mk1** | `9×9` (raio `4`) | `3` (abaixo, nível, acima) | `1×` (a cada `80` ticks) | `4.000 mB` |
| **Regador Agrícola Mk2** | `9×9` (raio `4`) | `3` (abaixo, nível, acima) | `4×` (4× mais frequente e 4× bônus) | `8.000 mB` |
| **Regador Agrícola Mk3** | `17×17` (raio `8`) | `3` (abaixo, nível, acima) | `4×` (4× mais frequente e 4× bônus) | `16.000 mB` |

* **Abastecimento:** posicione diretamente sobre um **bloco de água fonte** para funcionamento contínuo sem gastar o tanque, ou abasteça por balde / **Tubo de Fluidos** (`CapabilityFluidHandler`).
* **Controle Redstone:** por padrão, pausa com sinal de Redstone. Use **`Shift + Clique Direito`** no bloco para alternar entre os 3 modos de Redstone.

---

## 2. Regador Diagonal
*Nome em inglês: **Diagonal Sprinkler** · ID: `cropmanager:sprinkler_diagonal`*

Regador **sem EU** projetado para fazendas com caminhos em cruz:
* Cobre até **4 quadrantes `9×9` independentes** (**NO**, **NE**, **SO**, **SE**), deixando um corredor seco de 1 bloco em formato de cruz ao redor do regador (área total abrangida `19×19`).
* Na interface, o topo aponta para o **Norte**. Você pode ativar/desativar cada quadrante individualmente e escolher o modo de Redstone (`Sempre`, `Sinal`, `Sem sinal`).

---

## 3. Vidro de Estufa
*Nome em inglês: **Greenhouse Glass** · ID: `cropmanager:greenhouse_glass`*

* Posicione diretamente **acima** de um cultivo jovem do IC2.
* Exige nível de luz **`>= 9`** no bloco logo acima do vidro.
* A cada random tick recebido pelo vidro, concede **`+20` pontos de crescimento** (padrão) ao cultivo diretamente abaixo (ignora estacas vazias, ervas daninhas e plantas já maduras).
* Construir uma estufa fechada completa é opcional para crops no mundo, mas obrigatório para o bônus de cúpula da Câmara Hidropônica.

---

## 4. Câmara Hidropônica
*Nome em inglês: **Hydroponic Chamber** · ID: `cropmanager:hydroponic_chamber`*

Máquina **Tier 2 EU** que cultiva um **Saco de Sementes IC2** (`IC2 Seed Bag`) dentro de um ambiente fechado simulado (`VirtualCropTile`), sem precisar de estacas no mundo.

### Especificações
| Parâmetro | Valor Padrão |
|---|---|
| **Tier / Energia** | Tier 2 · `50.000 EU` de buffer · `16 EU/t` |
| **Ciclo Base** | `200` ticks |
| **Tanque de Água** | `16.000 mB` (consome `500 mB` de água por ciclo) |
| **Upgrades** | 4 slots de upgrades IC2 (`Overclocker`, `Energy Storage`, `Transformer`, `Wireless EU`) + 1 slot de bateria |
| **Automação Segura** | A `Seed Bag` inserida é **preservada** e protegida contra extração externa; apenas os 4 slots de saída são extraíveis por tubos/funis, e apenas **Fertilizante Catalítico** entra via automação no slot de catalisador |

### Requisitos de Estrutura e Bônus
1. **Obrigatório acima:** `1` bloco de **Vidro de Estufa** (`greenhouse_glass`) diretamente acima da máquina.
2. **Obrigatório abaixo (subsolo):** `Dirt`, `Farmland` ou `Grass` para crops comuns; para **Erva-Rádio** (`radbloom`), exige **Terra Radioativa** (`radioactive_soil`); para `Musgo de Cobre`, `Erva de Estanho` e `Cana-Volt`, exige respectivamente `ore:blockCopper`, `ore:blockTin` e `Bloco de Redstone`.
3. **Bônus de Cúpula (`+25%` velocidade):** complete uma camada `3×3` de **Vidro de Estufa** acima da câmara.
4. **Bônus Térmico (`+50%` velocidade):** conecte uma fonte de calor `IHeatSource` adjacente (como o **Queimador Térmico a Biogás**, um **Tubo Térmico** ou um aquecedor IC2), consumindo `20 HU/t`.
5. **Segurança Nuclear:** como a planta cresce em `VirtualCropTile` interno, cultivos de **Erva-Rádio** dentro da Câmara Hidropônica **contêm a radiação** e não irradiam o jogador ao redor.

---

## 5. Decorativos e Iluminação de Estufa (Luz 15)

| Bloco | ID | Uso |
|---|---|---|
| **Armação de Estufa** (*Greenhouse Frame*) | `cropmanager:greenhouse_frame` | Estrutura decorativa de ferro e madeira para combinar com Vidro de Estufa |
| **Treliça de Jardim** (*Garden Trellis*) | `cropmanager:garden_trellis` | Gradeado decorativo de madeira para cercas e bordas |
| **Lanterna Agrícola** (*Farm Lantern*) | `cropmanager:farm_lantern` | Lanterna em gaiola de ferro com luz nível `15` |
| **Arandela de Madeira** (*Wooden Sconce*) | `cropmanager:wooden_sconce` | Luminária nível `15` montável em chão, parede ou teto |
| **Lanterna Industrial** (*Industrial Lantern*) | `cropmanager:industrial_lantern` | Luminária metálica nível `15` montável em chão, parede ou teto |
| **Luminária de Vidro** (*Glass Lamp*) | `cropmanager:glass_lamp` | Luminária de vidro nível `15` montável em chão, parede ou teto |

> As luminárias montáveis podem ser fixadas em faces sólidas, no **Vidro de Estufa** ou na **Armação de Estufa**, e caem caso o bloco de apoio seja removido.
