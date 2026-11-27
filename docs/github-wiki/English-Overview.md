# IC2: Agronomy Expansion — English Overview

![Minecraft](https://img.shields.io/badge/Minecraft-1.12.2-62B47A)
![IC2](https://img.shields.io/badge/IC2%20Experimental-2.8.222--ex112-4B7BEC)
![Version](https://img.shields.io/badge/Version-0.5.0--alpha11-orange)
![License](https://img.shields.io/badge/License-MIT-blue)

**[Voltar para a Wiki em Português (Home)](Home)**

**IC2: Agronomy Expansion** (`modid: cropmanager`) is a comprehensive agricultural automation, genetics, logistics, and wireless EU addon for **Minecraft 1.12.2** and **IndustrialCraft 2 Experimental (`2.8.222-ex112`)**.

---

## Installation & Requirements

1. **Minecraft `1.12.2`** with Forge (or Cleanroom `1.12.2`).
2. **Required:** **IndustrialCraft 2 Experimental (`2.8.222-ex112`)**.
3. **Optional:** **HEI / JEI** for recipe and custom IC2 crop lookup.
4. Place **only** `IC2-Agronomy-Expansion-0.5.0-alpha11.jar` inside `mods/`. Both client and server must run the exact same version.
5. *(Optional)* The main JAR ships with the **classic light IC2 UI** by default. For the dark `#1C1C1D` theme, place `IC2-Agronomy-Expansion-GUI-Dark-0.5.0-alpha11.zip` (`pack_format: 3`, 20 remapped GUI textures) inside `resourcepacks/` and enable it in-game — text contrast adjusts automatically.

---

## Core Systems at a Glance

### 1. Crop Automation & Remote Telemetry
* **Advanced Crop Manager (`advanced_crop_manager`):** Tier 2 (`100,000 EU`), `20,000 mB` Water & Weed-Ex tanks, `3×3` to `17×17` configurable 3D area (up to 3 vertical layers + 3D offset), integrated environmental census, genetic filtering, and persistent harvest queue. *(Note: `ITEM_HANDLER_CAPABILITY` is omitted on the `DOWN` face to prevent Ender IO 5.5.x Cleanroom conduit crashes; use the other 5 faces.)*
* **Agricultural Probe (`agricultural_probe`):** Inspects IC2 crops (respecting scan level and catalyzed charges) and binds machines (`Sneak + Right-Click` target, then `Right-Click` Monitor).
* **Agricultural Monitor (`agricultural_monitor`):** `32`-block base range (up to `512` with 4 **Wireless Range Upgrades**), remote controls, `Harvest Transfer Upgrade`, `Wireless EU Upgrade`, configurable Redstone alerts, and automation stop rules with `100%` Weed-Ex hysteresis lock.

### 2. Irrigation, Greenhouses & Hydroponics
* **Sprinklers (`sprinkler_mk1`..`mk3` & `sprinkler_diagonal`):** Zero-EU 3D sprinklers (`9×9` on Mk1/Mk2, `17×17` on Mk3; `4×` speed on Mk2/Mk3; 4 independent `9×9` quadrants on Diagonal). Toggle 3 Redstone modes via `Sneak + Right-Click`.
* **Greenhouse Glass (`greenhouse_glass`):** Boosts random-tick growth on immature crops directly underneath when light `>= 9`.
* **Hydroponic Chamber (`hydroponic_chamber`):** Tier 2 (`50,000 EU`, `16 EU/t`, `16,000 mB` water tank) that grows IC2 Seed Bags safely inside a `VirtualCropTile` (contains `Radbloom` radiation). Requires `Greenhouse Glass` above and valid subsoil below; gains `+25%` speed from a `3×3` glass dome and `+50%` speed from an adjacent `IHeatSource` (`20 HU/t`).

### 3. Chemistry, Composting & Biogas Ecosystem
* **Manual & Electric Composters:** Convert organic items (including IC2 `Weed`, `Bio-Fiber`, and `Nitro Essence`) into **Organic Compost** or **IC2 Fertilizer**.
* **Weed-Ex Production Line:** **Grin Grinder** (`Grinding Blade` + Spider Eye → `Grin Powder`) → **Grin Extractor** (`+ Water` → `Grin Extract`) → **Weed-Ex Mixer** (`+ Redstone` → `IC2 Weed-Ex`).
* **Biogas Machines:** **Biogas Weeder** (`16,000 mB` Biogas, outputs IC2 `Weed`), **Biogas Sprayer** (`9×9` hydration/fertilization), **Biogas Heat Burner** (`32 HU/t` `IHeatSource` front face), **Thermal Pipe** (up to `100 HU/t`), and **Agricultural Biogas Generator** (`3×3×3` hollow multiblock producing `32 EU/t` Tier 1).

### 4. Genetics, Seed Storage & Custom Crops
* **Seed Analyzer (`seed_analyzer`):** Tier 1 machine that analyzes Seed Bags up to `4/4` with OLED telemetry.
* **Crossing Tool (`crossing_tool`):** Links empty double crop sticks and harvests/rearms crosses in one click.
* **Crop Database Chest & Portable (`500` slots):** Search, sort, favorite (`Middle-Click` / `Shift+R`), magnetic pickup, and cross-dimensional linking via **Upgrade Linker**.
* **Pollinator Hive (`pollinator_hive`) & Mutagenic Powder:** Accelerates double crop stick cross-breeding (`3×` faster when charged).
* **15 Custom IC2 Crops:**
  * *Standard:* `Grinbloom` (Grin Powder), `Bioreed` (`Bio-Fiber`, plantable directly on sticks), `Nitrobulb` (`Nitro Essence`), `Glowbloom` (Glowstone + light 6/12), `Sulfurbud` (Sulfur), `Resincane` (Sticky Resin), `Cryolotus` (Snowballs).
  * *Catalyzed (require **Catalytic Fertilizer** past stage 2):* `Lazuli Bloom` (Lapis), `Quartzreed` (Nether Quartz), `Cupric Moss` (Copper, needs `ore:blockCopper` subsoil), `Stannum Herb` (Tin, needs `ore:blockTin` subsoil), `Lithivine` (Lithium), `Voltreed` (Redstone, needs `Redstone Block` subsoil), `Radbloom` (Uranium-238, needs `Radioactive Soil`, emits IC2 radiation), and `Irid Moss` (Iridium Shard).

### 5. `3×5×3` Multiblocks & Wireless EU Grid
* **Agricultural Reservoir (`agricultural_reservoir`):** Dual-tank `3×5×3` multiblock with Tier 1 EU auto-delivery, fluid filters, `Capacity Expansion`, `Reinforced Pump`, and `Reserve Valve`.
* **Industrial Fluid Reservoir (`fluid_tank`) & Agricultural Grain Silo (`agricultural_silo`):** Zero-EU `3×5×3` bulk storage (`128,000` base + up to 8 upgrades = `384,000`), face port config, Redstone auto-eject, and comparator output on casings.
* **Fluid Pipe (`fluid_pipe`):** Zero-EU fluid equalization and transport (`250 mB / 5 ticks`).
* **Wi-Fi EU Grid & Dish Antenna (`eu_wifi_plug`, `eu_wifi_point`, `eu_dish_antenna`):** 1,000 public/private EU channels (`100,000 EU` buffer, selectable `T1–T4` output) + area wireless EU distribution for **Wireless EU Upgrades**.
