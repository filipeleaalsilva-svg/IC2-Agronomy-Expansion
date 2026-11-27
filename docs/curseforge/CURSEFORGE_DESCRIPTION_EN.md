# IC2: Agronomy Expansion

**IC2: Agronomy Expansion** is a complete agricultural automation, genetics, logistics, and wireless EU addon for **Minecraft 1.12.2** and **IndustrialCraft 2 Experimental (`2.8.222-ex112`)**.

It turns IC2 crop breeding and farming into a cohesive industrial system: 3D crop care and harvesting, remote telemetry with hysteresis rules, closed-enclosure hydroponics, Weed-Ex chemistry, a Biogas-powered farm ecosystem, `3×5×3` multiblock storage, wireless EU distribution, 500-slot indexed seed storage, and **15 custom IC2 crop species**.

---

## Requirements

* **Minecraft:** `1.12.2`
* **Mod Loader:** Minecraft Forge (or Cleanroom `1.12.2`)
* **Required Dependency:** **IndustrialCraft 2 Experimental** (`ic2@[2.8.222-ex112]`)
* **Optional Integration:** **HEI / JEI** (searchable IC2 Seed Bags by species + custom crop recipes tab)
* **Java:** Java 8+

---

## Key Features

### 1. 3D Crop Automation & Remote Monitoring
* **Advanced Crop Manager:** Tier 2 machine (`100,000 EU`, `20,000 mB` Water & Weed-Ex tanks) that fertilizes, hydrates, applies Weed-Ex, clears weeds (with the **Weed Killer Upgrade**), and harvests IC2 crops across a configurable `3×3` to `17×17` area (up to 3 vertical layers + 3D offset). Includes a live environmental census (soil nutrients, humidity, air quality, light) and genetic filtering.
* **Agricultural Monitor & Probe:** Link machines with the **Agricultural Probe** (`32`-block base range, up to `512` with **Wireless Range Upgrades**). Control operations remotely, transfer harvests wirelessly, distribute **Wireless EU**, emit Redstone alarms, and configure automatic stop rules with a `100%` Weed-Ex hysteresis lock.

### 2. Irrigation, Greenhouses & Hydroponic Chamber
* **Agricultural Sprinklers (Mk1–Mk3) & Diagonal Sprinkler:** Zero-EU 3D sprinklers covering `9×9` (`Mk1/Mk2`), `17×17` (`Mk3`, `4×` speed), or 4 independently toggled `9×9` quadrants leaving a dry cross walkway (**Diagonal Sprinkler**).
* **Greenhouse Glass & Decor:** **Greenhouse Glass** accelerates immature IC2 crops underneath when light `>= 9`. Includes **Greenhouse Frame**, **Garden Trellis**, and 4 level-15 decorative lamps (**Farm Lantern**, **Wooden Sconce**, **Industrial Lantern**, **Glass Lamp**).
* **Hydroponic Chamber:** Tier 2 EU machine (`50,000 EU`, `16 EU/t`, `16,000 mB` water tank) that cultivates IC2 Seed Bags safely inside a `VirtualCropTile` (safely containing **Radbloom** radiation). Requires Greenhouse Glass above and valid subsoil below, with optional `3×3` glass dome (`+25%` speed) and adjacent IC2 heat (`+50%` speed) bonuses.

### 3. Chemistry, Composting & Biogas Ecosystem
* **Manual & Electric Composters:** Early-game **Manual Composter** (0 EU, `2×` speed with water) produces **Organic Compost**, while the **Electric Composter** converts organic items (including IC2 `Weed`, `Bio-Fiber`, and `Nitro Essence`) + water into **IC2 Fertilizer**.
* **Weed-Ex Production Line:** **Grin Grinder** (with reusable `256`-use **Grinding Blade**) → **Grin Extractor** → **Weed-Ex Mixer** (`IC2 Weed-Ex`).
* **Biogas Farm Machines & Heat:**
  * **Biogas Weeder:** Zero-EU weed remover (`16,000 mB` Biogas) that outputs IC2 `Weed` items.
  * **Biogas Sprayer:** Zero-EU `9×9` hydration and fertilization powered by IC2 Biogas.
  * **Biogas Heat Burner & Thermal Pipe:** `32 HU/t` `IHeatSource` burner and `100 HU/t` thermal pipes.
  * **Agricultural Biogas Generator:** `3×3×3` hollow multiblock generating `32 EU/t` (Tier 1) from IC2 Biogas.

### 4. Genetics, 500-Slot Seed Storage & 15 Custom Crops
* **Seed Analyzer & Crossing Tool:** Tier 1 batch analyzer (`4/4` scan level with OLED telemetry) and a portable **Crossing Tool** for one-click harvesting and rearming double crop sticks.
* **Pollinator Hive & Mutagenic Powder:** Accelerates double crop stick cross-breeding (`3×` faster when charged).
* **Crop Database Chest & Portable Database:** `500` IC2 Seed Bag slots with instant search, sorting (Type / Tier / Stats), Favorites (`Middle-Click` / `Shift+R`), magnetic pickup, and cross-dimensional linking via **Upgrade Linker**.
* **Agricultural Manual:** Illustrated in-game guide with search and clickable recipes.
* **15 Custom IC2 Crop Species:**
  * *Support & Resource Crops:* **Grinbloom**, **Bioreed** (`Bio-Fiber` plantable directly on sticks), **Nitrobulb**, **Glowbloom** (emits light), **Sulfurbud**, **Resincane**, **Cryolotus**.
  * *Catalyzed Crops (require **Catalytic Fertilizer** past stage 2):* **Lazuli Bloom**, **Quartzreed**, **Cupric Moss** (`ore:blockCopper`), **Stannum Herb** (`ore:blockTin`), **Lithivine**, **Voltreed** (`Redstone Block`), **Radbloom** (`Radioactive Soil` + IC2 radiation), and **Irid Moss** (`Iridium Shard`).

### 5. `3×5×3` Multiblocks & Wireless EU Grid
* **Agricultural Reservoir:** Tier 1 EU `3×5×3` dual-tank multiblock with fluid filters, face ports, `Capacity Expansion`, `Reinforced Pump`, and `Reserve Valve`.
* **Industrial Fluid Reservoir & Agricultural Grain Silo:** Zero-EU `3×5×3` bulk storage (`128,000` base up to `384,000` with 8 upgrades), configurable face ports, Redstone auto-eject, and comparator output on casings.
* **Fluid Pipe:** Zero-EU fluid transport and safe tank equalization.
* **Wi-Fi EU Grid & Dish Antenna:** `1,000` public/private EU channels (`100,000 EU` buffer, selectable `T1–T4` output) + area wireless EU distribution for **Wireless EU Upgrades** (works in both mod machines and native IC2 machines).

---

## Optional Dark Theme Resource Pack

The main mod JAR uses the classic light IC2 UI by default. Prefer a sleek dark industrial UI (`#1C1C1D`)? Download **`IC2-Agronomy-Expansion-GUI-Dark-0.5.0-alpha11.zip`** (`pack_format: 3`, `"IC2: Agronomy Expansion - Dark Theme (#1C1C1D)"`, custom `pack.png`, and 20 remapped GUI textures) from **Additional Files**, place it in `resourcepacks/`, and enable it in-game. Text colors adapt automatically to background brightness.

---

## Installation & World Migration

1. Install **Minecraft 1.12.2** with Forge (or Cleanroom) and **IC2 Experimental `2.8.222-ex112`**.
2. Drop `IC2-Agronomy-Expansion-0.5.0-alpha11.jar` into your `mods/` folder (do not install `-sources` or `-runtime-tests` JARs).
3. Client and server must run the exact same mod version.
4. **Updating from older alphas:** Back up your world first. Legacy basic Crop Managers migrate automatically to the Advanced Crop Manager on chunk load, and retired legacy blocks/items safely recover or remap via Forge `MissingMappings`.

---

## Links & Languages

* **Full Wiki (PT-BR & EN):** [GitHub Wiki](https://github.com/filipeleaalsilva-svg/IC2-Agronomy-Expansion/wiki)
* **Source Code & Issue Tracker:** [GitHub Repository](https://github.com/filipeleaalsilva-svg/IC2-Agronomy-Expansion)
* **Supported Languages:** English (`en_us`) and Portuguese - Brazil (`pt_br`)
* **License:** MIT License
