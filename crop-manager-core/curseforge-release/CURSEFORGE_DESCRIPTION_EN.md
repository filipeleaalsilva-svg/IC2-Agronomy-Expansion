# Crop Manager Core

**Crop Manager Core** is an agricultural automation and logistics suite for **Minecraft 1.12.2** and **IndustrialCraft 2 Experimental** (`ic2-2.8.222-ex112`).

It unifies crop care, harvesting, environmental diagnostics, weed removal, fertilizer production, Weed-Ex synthesis, wireless EU distribution, and high-capacity IC2 seed storage into a cohesive, industrial-grade IC2-styled system.

---

## Requirements

- **Minecraft:** `1.12.2`
- **Mod Loader:** Minecraft Forge (or Cleanroom 1.12.2)
- **Required Dependency:** **IndustrialCraft 2 Experimental** (`ic2@[2.8.222-ex112]`)
- **Java:** Java 8+ (tested with Forge/Cleanroom Java 8 bytecode target)

---

## Machines & Blocks

### 1. Advanced Crop Manager
An all-in-one IC2 crop care and harvesting machine:
- **Combined Automation:** Performs harvesting, fertilizing, hydrating, and Weed-Ex application in a single scan pass.
- **Configurable 3D Area:** Adjustable radius from `1` (`3×3`) up to server `maxRadius` (`8` by default = `17×17`) across up to 3 vertical layers (below, same level, above). Includes a local 3D area preview.
- **Specs:** Tier 2 EU sink (`100,000 EU` internal buffer), `20,000 mB` Water tank, `20,000 mB` Weed-Ex tank.
- **IC2 Upgrades:** Supports Overclocker, Energy Storage, Transformer, **Weed Killer Upgrade**, and **Wireless EU Upgrade**.
- **Diagnostics & Census:** Reports planted crops, mature counts, weed counts, unanalyzed crops, and real environmental readings (soil nutrients, hydration, air quality, and light levels).
- **Redstone Control:** Always on / with redstone signal / without redstone signal.

### 2. Agricultural Monitor
Remote control, telemetry, and wireless logistics hub:
- **Wireless Linking:** Link to an Advanced Crop Manager, IC2 Crop-Matron, or Electric Weed Harvester using the **Agricultural Probe** (base range `32` blocks, expandable with **Wireless Range Upgrades**).
- **Remote Controls:** Toggle machine power, harvest, fertilize, hydrate, Weed-Ex, and redstone mode remotely.
- **Automation Rules & Hysteresis:** Configure automatic pause thresholds for Water and Weed-Ex (empty or percentage-based). When paused due to low Weed-Ex, operation safely resumes only once Weed-Ex reaches **100%** and conditions clear.
- **Wireless Logistics:** Can transfer Water, Weed-Ex, Fertilizer, and harvested items (with **Harvest Transfer Upgrade**), plus distribute **Wireless EU** to linked machines and nearby Crop Manager support machines.
- **Redstone Output:** Off, Alarm (emits redstone 15 on faults/pauses), or Normal (emits redstone 15 when active and healthy).

### 3. Electric Weed Harvester
Dedicated EU-powered weed clearing machine:
- Scans configurable 3D areas (`4` scans/tick base) and removes IC2 weeds without harming valid crops.
- Stores harvested IC2 weeds in 4 output slots for further processing (e.g., feeding the Electric Composter).
- Tier 1 (`10,000 EU` buffer), battery slot, and 4 IC2 upgrade slots.

### 4. Electric Composter
Converts organic materials and water into **IC2 Fertilizer**:
- Processes organic items (`4` organic items + `1,000 mB` Water, or `2` IC2 weeds + `500 mB` Water) into IC2 Fertilizer.
- Tier 1 (`10,000 EU` buffer), `16,000 mB` Water tank, battery slot, and 4 IC2 upgrade slots.

### 5. Weed-Ex Mixer (3-Tank)
Automated Weed-Ex fluid synthesis machine:
- Converts **Spider Eyes** + Water into **Grin Extract** (`grin_extract`), and **Redstone** + Water into **Liquid Redstone** (`liquid_redstone`), then mixes them into **IC2 Weed-Ex** (`ic2weed_ex`).
- Supports fluid containers (buckets/cells) and Forge fluid capabilities for pipe automation.
- Tier 1 (`10,000 EU` buffer), battery slot, and 4 IC2 upgrade slots.

### 6. Crop Database Chest (500 Slots)
High-capacity storage specifically designed for **IC2 Seed Bags**:
- **500 dedicated seed slots** with pagination, instant text search, and **Favorites** filtering (`Shift+R` or middle-click).
- Displays full IC2 crop genetics (`Growth`, `Gain`, `Resistance`) and species info.
- Optional nearby seed auto-collection (`3`-block radius).

---

## Items & Upgrades

- **Portable Crop Database (500 Slots):** Handheld access to your Crop Database Chest inventory. Link by `Shift + Right-Click` on a Crop Database Chest.
- **Agricultural Probe:** Inspects IC2 crops (respecting scan/analysis levels) and links machines to the Agricultural Monitor (`Shift + Right-Click` source, then `Right-Click` Monitor).
- **Upgrade Linker:** Links and configures wireless upgrade targets.
- **Wireless EU Upgrade:** Universal IC2 `IUpgradeItem`! Works in both Crop Manager machines and compatible IC2 machines. Link via `Shift + Right-Click` on an IC2 energy storage block (`BatBox`, `CESU`, `MFE`, `MFSU`).
- **Weed Killer Upgrade:** Enables the Advanced Crop Manager to actively clear IC2 weeds using `20 mB` of Weed-Ex per weed.
- **Wireless Range Upgrade:** Extends Agricultural Monitor wireless range.
- **Harvest Transfer Upgrade:** Enables wireless transfer of harvested products from the linked Manager to the Monitor.

---

## Optional Resource Pack

An official dark-themed GUI pack (**`CropManagerCore-GUI-Dark-0.5.0-alpha9.zip`**) is available in the **Additional Files** section for players who prefer a sleek dark industrial interface.

---

## Installation

1. Install **Minecraft 1.12.2** with **Forge** (or Cleanroom) and **IndustrialCraft 2 Experimental (`2.8.222-ex112`)**.
2. Drop `CropManagerCore-0.5.0-alpha9.jar` into your `mods/` folder.
3. *(Optional)* Place `CropManagerCore-GUI-Dark-0.5.0-alpha9.zip` in `resourcepacks/` and enable it in-game.
4. *(Recommended)* Back up your world before updating alpha builds.

---

## Languages

- **English (`en_us`)**
- **Portuguese - Brazil (`pt_br`)**
