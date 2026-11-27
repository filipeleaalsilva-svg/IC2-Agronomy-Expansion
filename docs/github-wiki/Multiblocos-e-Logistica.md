# Multiblocos e Logística

O mod adiciona **3 torres multibloco `3×5×3`** (base `3×3`, altura `5` blocos) para armazenamento em massa e **Tubos de Fluidos** sem gasto de energia.

---

## Padrão de Montagem das Torres `3×5×3`

Todas as três estruturas abaixo seguem exatamente a mesma geometria:
1. **Base e Topo:** camadas `Y=0` e `Y=4` totalmente fechadas (`3×3`, `9` carcaças cada).
2. **Paredes Intermediárias (`Y=1..3`):** `8` blocos de borda por camada com o **núcleo interno `1×3×1` vazio (oco)**.
3. **Controlador:** substitua **1 carcaça** do centro de uma das paredes laterais intermediárias pelo bloco controlador voltado para fora (`41` carcaças + `1` controlador = `42` blocos).
4. **Acesso e Automação:** clique com o botão direito em **qualquer carcaça** formada para abrir a interface. Tubos, funis e **Comparadores de Redstone** funcionam nas faces externas das carcaças conforme a configuração de portas.

---

## 1. Reservatório Agrícola
*Nome em inglês: **Agricultural Reservoir** · IDs: `cropmanager:agricultural_reservoir` + `cropmanager:reservoir_casing`*

Multibloco com **energia IC2 (Tier 1)** e **dois tanques independentes** (ideal para armazenar Água e Weed-Ex na mesma torre).

* **Capacidade:** `32.000 mB` iniciais por tanque (expansível até `96.000 mB` cada usando até `4` **Expansões de Capacidade**, `+16.000 mB` por expansão).
* **Filtros:** insira um recipiente cheio no slot de filtro para travar o tanque àquele fluido sem consumir o recipiente.
* **Envio Automático Ativo:** com EU conectado ao controlador, envia fluido para máquinas agrícolas vizinhas ao custo de **`1 EU` por `50 mB`**.
* **Módulos Dedicados:**
  * **Expansão de Capacidade** (*Capacity Expansion* · `reservoir_capacity`): aumenta o volume máximo dos tanques.
  * **Bomba Reforçada** (*Reinforced Pump* · `reservoir_pump`): dobra a vazão de envio automático.
  * **Válvula de Reserva** (*Reserve Valve* · `reservoir_reserve`): mantém reserva mínima configurável (`0%`, `25%`, `50%` ou `75%`) em cada tanque.

---

## 2. Reservatório de Fluido Industrial
*Nome em inglês: **Industrial Fluid Reservoir** · IDs: `cropmanager:fluid_tank` + `cropmanager:fluid_tank_casing`*

Torre `3×5×3` **sem consumo de EU** para armazenamento em massa de um único fluido.

* **Capacidade:** `128.000 mB` base + até `8` **Expansões de Capacidade** (`+32.000 mB` cada = **`384.000 mB`** máximo).
* **Portas e Redstone:** clique esquerdo nas faces do diagrama na GUI para alternar entre `Fechada`, `Entrada` e `Saída`. As faces configuradas como `Saída` empurram passivamente até **`2.000 mB/s`** (0 EU) respeitando a válvula Redstone (`Ignorar`, `Sinal alto`, `Sinal baixo`).
* **Comparador:** emite sinal `0..15` no controlador e em todas as carcaças conforme o nível do tanque.

---

## 3. Silo Agrícola de Grãos
*Nome em inglês: **Agricultural Grain Silo** · IDs: `cropmanager:agricultural_silo` + `cropmanager:silo_casing`*

Torre `3×5×3` **sem consumo de EU** para armazenamento em massa de um único tipo de item (sementes, fertilizantes, produtos colhidos, etc.).

* **Capacidade:** `128.000` itens base + até `8` **Expansões de Capacidade** (`+32.000` cada = **`384.000` itens** máximo).
* **Trava de Modelo:** permite travar o tipo de item aceito mesmo quando o silo ficar vazio.
* **Portas e Auto-Extração:** configure faces `Fechada / Entrada / Saída`; faces de `Saída` empurram passivamente até **`64 itens/s`** (0 EU) conforme o modo Redstone.
* **Comparador:** emite sinal `0..15` proporcional à lotação do silo no controlador e nas carcaças.

---

## 4. Tubo de Fluidos
*Nome em inglês: **Fluid Pipe** · ID: `cropmanager:fluid_pipe`*

Logística de fluidos **sem consumo de EU**.
* Conecta tanques, reservatórios, regadores e máquinas agrícolas do mod.
* Puxa fluidos de saídas conectadas e distribui pela rede até **`250 mB` a cada `5` ticks** (padrão).
* **Equalização Segura:** equilibra níveis entre tanques bidirecionais conectados sem gerar loops infinitos.
