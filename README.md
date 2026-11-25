**English** | [Português (PT-BR)](README.pt-BR.md)

# IC2: Agronomy Expansion 0.5.0-alpha11

![Minecraft](https://img.shields.io/badge/Minecraft-1.12.2-62B47A)
![IC2](https://img.shields.io/badge/IC2%20Experimental-2.8.222--ex112-4B7BEC)
![Java](https://img.shields.io/badge/Java-8%20target-ED8B00)
![License](https://img.shields.io/badge/License-MIT-blue)

Addon for **Minecraft 1.12.2** and **IndustrialCraft 2 Experimental (`2.8.222-ex112`)**. It brings together advanced crop automation, remote monitoring, Weed-Ex processing and composting, IC2 seed analysis and indexed storage, multiblock fluid/item logistics, pollination, and a custom line of IC2 crops and fertilizers.

---

## Quick Start (Development & Testing)

The project uses **Gradle 9.7.0** with **RetroFuturaGradle 2.0.3** (targeting Java 8 bytecode). Gradle 9.7 requires **Java 17 or newer** to run the daemon, while the mod code compiles to Java 8.

Place the audited IC2 JAR at `libs/industrialcraft-2-2.8.222-ex112.jar` (its SHA-256 checksum is verified by the `verifyIc2` task) and run in PowerShell:

```powershell
# Point GRADLE_USER_HOME to the local cache when working offline
$env:GRADLE_USER_HOME = "$PWD\.gradle-home"

# Run the unit test suite offline
.\gradlew.bat test --offline -x downloadFernflower

# Build the main JAR and Cleanroom runtime-test JAR
.\gradlew.bat build reobfRuntimeTests
py tools\verify_artifact.py
```

---

## Installation & Upgrading

1. Copy **only** `IC2-Agronomy-Expansion-0.5.0-alpha11.jar` into your `mods/` folder (do not install the `runtime-tests` JAR).
2. Client and server must run the exact same version (`acceptableRemoteVersions = [0.5.0-alpha11]`).
3. **IndustrialCraft 2 Experimental (`2.8.222-ex112`)** is required (**HEI/JEI** integration is optional).
4. The dark theme UI (`#1C1C1D`) is the mod default (text color automatically adapts to the brightness of the loaded background).

### Legacy World Migration
- Back up your world before upgrading between alpha versions.
- The legacy **basic Crop Manager** automatically migrates to the **Advanced Crop Manager** after chunk load (up to 32 migrations per tick via `LegacyMigration`), preserving inventory, EU, tanks, area, and controls.
- Removed legacy blocks (`agricultural_collector`, `agricultural_reservoir`, `seed_classifier`, `seed_workbench`) are retired lazily via `LegacyRecoveryTile`, dropping their items on the ground; stored items in inventories are remapped via Forge `MissingMappings`.
- Native IC2 Crop-Matron and Crop Harvester remain available.

---

## Blocks & Features

### Advanced Crop Manager (`advanced_crop_manager`)
- **Recipe:** `HIC / CMC / III` (Crop Harvester, Iridium, Advanced Circuit, Crop-Matron).
- **Specs:** Tier 2, `100,000 EU` base buffer, `20,000 mB` tanks (water and Weed-Ex), 26 internal slots (fertilizers, 15 outputs, 4 IC2 upgrades, and fluid containers).
- **Operation:** Scans from `3×3` up to `17×17` (`1..8` radius, server-configurable) across up to 3 vertical layers (`below / level / above`) with configurable 3D area offset (`offsetX`, `offsetY`, `offsetZ`), never force-loading chunks.
- **Integrated Survey:** The **Data** tab consolidates planted species (respecting IC2 scan level) and real nutrient, hydration, air quality, and light readings at the end of each full sweep.
- **Automation:** Supports Capability input/output (note: in 0.5.0-alpha6+, `ITEM_HANDLER_CAPABILITY` is omitted on the `DOWN` face to avoid crashes with Ender IO 5.5.x Cleanroom conduits).

### Agricultural Probe & Monitor (`agricultural_probe`, `agricultural_monitor`)
- **Probe:** Inspects IC2 crops (respecting scan level) and links machines (`ManagerTile`, `WeederTile`, or `TileEntityCropmatron`) to a Monitor via *Sneak + Right-Click*.
- **Monitor:** `32`-block base range in the same dimension, doubling per *Wireless Range Upgrade* (up to `512` blocks with 4 upgrades).
- **Resources & Network:** `16,000 mB` water and Weed-Ex buffers, 3 fertilizer slots, 1 *Harvest Transfer* slot, 1 *Wireless EU Upgrade* slot (powering the linked target and nearby support machines within `16` blocks), and configurable redstone output (Off / Alarm / Normal / 5s Test).
- **Stop Rules (Auto. Tab):** Remotely pauses the Manager based on water, Weed-Ex, fertilizer, energy, or full-output thresholds; Weed-Ex triggers enable a hysteresis lock until the tank refills to `100%`.

### Biogas Weeder (`biogas_weeder`)
- Dedicated 3D machine with zero EU consumption; uses a `16,000 mB` **IC2 Biogas** tank.
- Scans `2` positions per tick (`1 mB` per loaded position checked + `20 mB` per weed removed), removing only real weeds (`Crops.weed`) without breaking crop sticks or tall crops.
- Each removed weed outputs an IC2 **Weed** item into the machine's dedicated output slot (if full, the weed remains on the Crop Stick).

### Composters (Manual & Electric)
- **Manual Composter (`manual_composter`):** Early-game zero-energy block; accepts `8` organic items via direct world interaction, accelerates with water (`2x`), and produces **Organic Compost** (`organic_compost`).
- **Electric Composter (`electric_composter`):** Tier 1, `20,000 EU` buffer, `16,000 mB` water tank. Converts **`4` organic items + `1,000 mB` water → `1` IC2 Fertilizer** in `200` ticks at `5 EU/t`, supporting 4 IC2 upgrades and a battery slot.

### Weed-Ex Production Line & Thermal Pipe
- **Grin Grinder (`grin_grinder`):** Tier 1, `100` ticks at `5 EU/t`; grinds `1` spider eye into `1` Grin Powder using a reusable **Grinding Blade** (`256` uses).
- **Grin Extractor (`grin_extractor`):** Tier 1, `100` ticks at `5 EU/t`; combines `1` Grin Powder + `1,000 mB` water → `1,000 mB` **Grin Extract**.
- **Weed-Ex Fabricator (`weed_ex_fabricator`):** Tier 1, `100` ticks at `5 EU/t`; combines `1,000 mB` Grin Extract + `1` redstone → `500 mB` **Weed-Ex**.
- **Thermal Pipe (`thermal_pipe`):** Connects `IHeatSource` blocks to compatible machines (up to `100 HU/t`). Supports IC2 wrench dismantling via *Sneak + Right-Click*.

### Seed Analyzer, Storage, Multiblocks & Custom Crops
- **Seed Analyzer (`seed_analyzer`):** Tier 1, `100` ticks at `5 EU/t`; analyzes seed bags up to level 4 and displays genetics and stats in the GUI.
- **Crop Database Chest (`crop_database_chest`) & Portable Database (`portable_crop_database`):** Stores `500` seed bags in a paginated `5×5` grid with text search, sorting (Type / Tier / Stats), favorites (without mutating seed NBT), configurable magnetic pickup, inventory auto-collect, and interdimensional remote linking via **Upgrade Linker (`upgrade_linker`)**.
- **Agricultural Sprinklers (`sprinkler_mk1`, `sprinkler_mk2`, `sprinkler_mk3`):** Zero-EU 3D blocks that hydrate farmland and IC2 crop sticks and accelerate growth (`9×9` on Mk1/Mk2, `17×17` on Mk3; `4×` speed on Mk2/Mk3), with 3 redstone modes via *Sneak + Right-Click*.
- **Agricultural Reservoir (`agricultural_reservoir` + `reservoir_casing`):** `3×5×3` dual-tank multiblock with IC2 power, fluid filters, pump/reserve modules, and capacity upgrades.
- **Industrial Fluid Tank (`fluid_tank` + `fluid_tank_casing`):** Zero-EU `3×5×3` multiblock for bulk single-fluid storage (`128,000 mB` base + up to 8 `+32,000 mB` upgrades), configurable face ports, and redstone auto-extract.
- **Agricultural Silo (`agricultural_silo` + `silo_casing`):** Zero-EU `3×5×3` multiblock for bulk single-item storage (`128,000` base + up to 8 `+32,000` upgrades), template lock, configurable ports, and redstone auto-extract.
- **Fluid Pipes (`fluid_pipe` — Pipe, Redstone Restrictor, and 6-Way Router):** Zero-EU fluid logistics with automatic tank equalization and directional routing.
- **Pollinator Hive (`pollinator_hive`) & Greenhouse Glass (`greenhouse_glass`):** The Hive speeds up cross-breeding on nearby double crop sticks (optionally consuming **Mutagenic Powder** to triple cadence); Greenhouse Glass grants a random-tick growth bonus to the crop directly below under adequate light.
- **Hydroponic Chamber (`hydroponic_chamber`):** Tier 2 EU machine (`50,000 EU`, `16 EU/t`, `16,000 mB` water tank) that grows IC2 Seed Bags in a controlled enclosure (`VirtualCropTile`). Requires `greenhouse_glass` above and valid subsoil below (`dirt`/`farmland`/`grass`, or `radioactive_soil` for `radbloom`), with optional `3×3` glass dome (`+25%` speed) and adjacent IC2 heat (`+50%` speed) bonuses.
- **Wi-Fi EU Grid & Dish Antenna (`eu_wifi_plug`, `eu_wifi_point`, `eu_dish_antenna`):** 1,000 public/private EU channels (`100,000 EU` smoothing buffer, selectable `T1–T4` output and redstone control) plus area wireless power distribution integrated with `WirelessEuUpgrade`.
- **Biogas Ecosystem (`biogas_generator` + `biogas_generator_casing`, `biogas_sprayer`, `biogas_heat_burner`):** `3×3×3` multiblock EU generator with 3D OBJ/TESR model, EU-free crop sprayer, and IC2 `IHeatSource` burner (`32 HU/t`).
- **Diagonal Sprinkler (`sprinkler_diagonal`) & Crossing Tool (`crossing_tool`):** Independently switched 4-quadrant `9×9` sprinkler with a dry cross walkway, plus a portable tool for harvesting and rearming double crop sticks.
- **Greenhouse Decor & Lighting (`greenhouse_frame`, `garden_trellis`, `farm_lantern`, `wooden_sconce`, `industrial_lantern`, `glass_lamp`):** Structural frames, trellises, and light-level-15 wall/floor/ceiling lamps.
- **Agricultural Manual (`agricultural_manual`) & HEI/JEI Plugin:** In-game illustrated guide with search and category index, plus optional HEI/JEI integration for IC2 seeds and mod crops.
- **Wireless EU Upgrade (`wireless_eu_upgrade`):** Implements IC2 `IUpgradeItem`; binds via *Sneak + Right-Click* to `IEnergyStorage` blocks (`BatBox`, `CESU`, `MFE`, `MFSU`), Wi-Fi EU channels, or Dish Antennas to power any IC2 machine with upgrade slots.
- **Custom Crops & Fertilizers:** Registers `15` IC2 crop species (`grinbloom`, `bioreed`, `nitrobulb`, `glowbloom`, `sulfurbud`, `lazuli_bloom`, `quartzreed`, `lithivine`, `irid_moss`, `radbloom` with `radioactive_soil`, plus `resincane`, `cryolotus`, `cupric_moss`, `stannum_herb`, and `voltreed`), materials (`bio_fiber`, `nitro_essence`, `mutagenic_powder`), and fertilizers (`organic_compost`, `catalytic_fertilizer`).

---

## Architecture Overview

| Package | Responsibility |
|---|---|
| `br.cropmanager` | Central registry (`CropManager`), configuration (`ManagerConfig`), networking (`ManagerNetwork`), proxies (`CommonProxy`/`ClientProxy`), shared bases (`FarmTile`, `PoweredMachineTile`, `EquipmentLayout`, `StorageMultiblockSupport`), machines and TileEntities (`ManagerTile`, `MonitorTile`, `WeederTile`, `ComposterTile`, `GrinGrinderTile`, `GrinExtractorTile`, `WeedExFabricatorTile`, `SeedAnalyzerTile`, `HydroponicChamberTile`, `VirtualCropTile`, `ManualComposterTile`, `ThermalPipeTile`, `SprinklerTile`, `DiagonalSprinklerTile`, `PollinatorHiveTile`, `FluidPipeTile`, `AgriculturalReservoirTile`, `IndustrialTankTile`, `AgriculturalSiloTile`, `BiogasGeneratorTile`, `BiogasSprayerTile`, `BiogasHeatBurnerTile`, `EuWifiPlugTile`, `EuWifiPointTile`, `EuDishAntennaTile`, `WirelessEuGridData`), tools (`CrossingTool`, `FarmTool`, `WirelessEuUpgrade`), pure logic (`ScanLogic`, `AutomationRules`, `CropSurvey`, `ManagerSettings`, `AlarmLogic`, `EnergyWarning`, `CrossBreedingHelper`, `StorageMultiblock`), and GUIs/renderers. |
| `br.cropmanager.crop` | Registry (`CropRegistry`), base card (`BaseCropCard`), `RadbloomCropCard`, and catalyzed soil mechanics. |
| `br.cropmanager.chest.*` | Block, unit inventory, portable item, linker, networking, recipes, search/genetics integration, and Crop Database Chest GUI (`500` slots). |
| `br.cropmanager.guide` | In-game Agricultural Manual (`ItemAgriculturalManual`, `GuiAgriculturalManual`, `ManualRegistry`, `ManualCategory`, `ManualEntry`). |
| `br.cropmanager.jei` | Optional HEI/JEI integration (`CropManagerJeiPlugin`, `CropRecipeCategory`, `CropRecipeWrapper`). |

---

## Configuration (`config/cropmanager.cfg`)

Main keys loaded by `ManagerConfig`:

| Category | Key | Default | Range | Description |
|---|---|---:|---:|---|
| `processing` | `baseIntervalTicks` | `2` | `1–20` | Ticks per position without upgrades |
| `processing` | `maxPositionsPerTick` | `8` | `1–32` | Cap on positions scanned per tick |
| `area` | `maxRadius` | `8` | `1–16` | Maximum horizontal radius allowed by server |
| `area` | `defaultRadius` | `4` | `1–maxRadius` | Default radius for newly placed machines |
| `area` | `defaultLayers` | `7` | `1–7` | Layer bitmask (`1` below, `2` level, `4` above) |
| `energy` | `scanEU` / `careEU` / `harvestEU` | `1` / `10` / `20` | `1–1000` | Base EU cost per scan, care, and harvest |
| `advanced_manager` | `advancedEnergyCapacity` | `100000` | `1000–10M` | Advanced Crop Manager base EU buffer |
| `advanced_manager` | `advancedTankCapacity` | `20000` | `1000–1M` | Water and Weed-Ex tank capacity (mB) |
| `monitor` | `baseRange` | `32` | `1–512` | Base Monitor range in blocks |
| `monitor` | `wirelessEuRatePerUpgrade` | `256` | `1–100000` | Transfer rate (EU/t) per Wireless EU upgrade |
| `weeder` | `gasCapacity` | `16000` | `1000–1M` | Biogas tank capacity (mB) |
| `composter` | `requiredItems` / `waterPerRecipe` | `4` / `1000` | `1–64` / `0–16000` | Organic inputs and water per fertilizer |
| `thermal_pipe` | `maxHeatPerTick` | `100` | `1–100000` | Heat transfer cap per tick (HU/t) |
| `database_chest` | `autoCollectRadius` | `3` | `1–16` | Magnetic pickup radius of the Database Chest |
| `crops` | `enableCustomCrops` | `true` | `true/false` | Enables registration of custom IC2 crop species |
| `hydroponic_chamber` | `durationTicks` / `euPerTick` | `200` / `16` | `10–72k` / `1–10k` | Base cycle duration and EU/t of the Hydroponic Chamber |
| `wifi_grid` | `channelBufferEU` | `100000` | `1k–10M` | Smoothing EU buffer per Wi-Fi EU channel |
| `dish_antenna` | `baseRadius` / `maxTransferPerTick` | `16` / `512` | `4–64` / `32–16k` | Dish Antenna base radius and max wireless EU/t |

See [docs/CONFIGURACAO.md](docs/CONFIGURACAO.md) for detailed formulas.

---

## Tests & Quality

- **`src/test/java/br/cropmanager/**`:** **269 unit tests** across **45 suites** covering scan logic, automation rules, NBT persistence, capabilities, asset/i18n integrity (`796` `en_us`/`pt_br` keys), Hydroponic Chamber, Wi-Fi EU grid, Dish Antenna, Biogas machines, multiblocks, fluid pipes, pollination, Database Chest inventory, recipes, and dupe protection.
- **`src/runtimeTest/java/br/cropmanager/**`:** Separate addon (`cropmanager_tests`, never packaged in the release JAR) for live Cleanroom runtime validation (`RuntimeChecks.java` and `ClientChecks.java`).

---

## Additional Documentation

- [Code Review Report (`docs/REVISAO_CODIGO.md`)](docs/REVISAO_CODIGO.md)
- [Changelog (`CHANGELOG.md`)](CHANGELOG.md)
- [Configuration & Formulas (`docs/CONFIGURACAO.md`)](docs/CONFIGURACAO.md)
- [Cleanroom Validation Guide (`docs/VALIDACAO.md`)](docs/VALIDACAO.md)
- [Previous Technical Verification (`VERIFICACAO.md`)](VERIFICACAO.md)

---

## License

Released under the [MIT License](LICENSE).
