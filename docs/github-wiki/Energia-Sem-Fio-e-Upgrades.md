# Energia Sem Fio e Upgrades

O **IC2: Agronomy Expansion** oferece distribuição de EU sem fios para máquinas do próprio mod e máquinas nativas do IndustrialCraft 2, além de upgrades exclusivos.

---

## 1. Upgrade de Energia Wireless (EU)
*Nome em inglês: **Wireless EU Upgrade** · ID: `cropmanager:wireless_eu_upgrade`*

Implementa `IUpgradeItem` do IC2, funcionando tanto em máquinas do **Crop Manager** quanto em **qualquer máquina do IC2** que possua slots de upgrade.

### Modos de Vínculo (`Shift + Clique Direito`)
1. **Armazenador IC2 (`BatBox`, `CESU`, `MFE`, `MFSU`):** a máquina que receber o upgrade puxa EU diretamente do armazenador na mesma dimensão (requer chunk carregado).
2. **Canal Wi-Fi de EU:** clique com o upgrade em um **Plug** ou **Ponto Wi-Fi de EU** (ou configure na rede) para puxar energia diretamente de um dos 1.000 canais da rede Wi-Fi.
3. **Antena Parabólica de EU:** vincule exclusivamente a uma Antena Parabólica (ou deixe o upgrade **sem vínculo** para receber energia automaticamente de qualquer Antena Parabólica no alcance).
4. **No Monitor Agrícola:** até `4` upgrades (`+50.000 EU` de reserva, `+1` Tier e `+256 EU/t` cada) permitem alimentar remotamente o Manager vinculado e máquinas de suporte próximas em raio de `16` blocos.

---

## 2. Rede Wi-Fi de EU (Canais Compartilhados)

| Bloco | ID | Papel |
|---|---|---|
| **Plug Wi-Fi de EU** (*Wi-Fi EU Plug*) | `cropmanager:eu_wifi_plug` | Conecta-se a cabos ou geradores IC2 como receptor (`Tier 4` padrão) e injeta EU em um canal Wi-Fi (`100.000 EU` de buffer por canal) |
| **Ponto Wi-Fi de EU** (*Wi-Fi EU Point*) | `cropmanager:eu_wifi_point` | Puxa EU de um canal Wi-Fi e entrega pacotes IC2 para cabos ou máquinas adjacentes, com seletor de **Tier (`T1–T4`)**, limite de `EU/t` e controle Redstone |

* **1.000 Canais (`0..999`):** cada canal possui modo **Público** ou **Privado** (canais públicos criados por outro jogador têm a privacidade protegida contra alteração).
* **Interdimensional:** funciona entre dimensões diferentes, com perda configurável em `wifi_grid.crossDimLossPercent` (`0%` padrão, até `50%`).

---

## 3. Antena Parabólica de EU
*Nome em inglês: **EU Dish Antenna** · ID: `cropmanager:eu_dish_antenna`*

* Puxa energia de `BatBox / CESU / MFE / MFSU` adjacentes ou cabos IC2 (`Tier 4` entrada, buffer `100.000 EU`).
* Distribui energia sem fio até **`512 EU/t`** (padrão) em um **raio base de `16` blocos**, somando **`+16` blocos** por cada **Upgrade de Alcance Wireless** inserido.
* Alimenta qualquer máquina no alcance que possua um **Upgrade Wireless EU** sem vínculo ou vinculado especificamente a ela.

---

## 4. Tabela Completa de Upgrades do Mod

| Upgrade | ID | Onde Usar | Efeito |
|---|---|---|---|
| **Upgrade Weed Killer** | `weed_killer_upgrade` | Crop Manager Avançado | Remove ervas daninhas IC2 reais durante a varredura (`20 mB` Weed-Ex/erva) |
| **Upgrade de Alcance Wireless** | `wireless_range_upgrade` | Monitor Agrícola / Antena Parabólica | Dobra o alcance do Monitor (`32` → `512` com 4) ou soma `+16` blocos de raio na Antena |
| **Upgrade de Transferência de Colheitas** | `harvest_transfer_upgrade` | Monitor Agrícola | Transfere colheitas do Manager vinculado para o buffer interno extraível do Monitor |
| **Upgrade de Energia Wireless (EU)** | `wireless_eu_upgrade` | Monitor, máquinas do mod e máquinas IC2 | Recebe/transmite EU sem fio de armazenadores IC2, canais Wi-Fi ou Antenas |
| **Expansão de Capacidade** | `reservoir_capacity` | Reservatório Agrícola, Tanque Industrial, Silo | `+16.000 mB` por tanque (até 4 no Reservatório) ou `+32.000` (até 8 no Tanque/Silo) |
| **Bomba Reforçada** | `reservoir_pump` | Reservatório Agrícola | Dobra a vazão do envio automático |
| **Válvula de Reserva** | `reservoir_reserve` | Reservatório Agrícola | Mantém reserva mínima configurável (`0 / 25 / 50 / 75%`) |

> **Upgrades Nativos do IC2:** todas as máquinas elétricas do mod aceitam `Overclocker`, `Energy Storage` e `Transformer`. **Atenção no Crop Manager Avançado:** `Overclockers` aumentam o custo por varredura (`D`) mesmo após atingir o teto de `8` posições/tick — evite excesso de Overclockers.
