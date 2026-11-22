---
name: cropmanager-gated-item-testing
description: >-
  Enforces a strict per-item standardized testing and validation gate for the Crop Manager
  Minecraft 1.12.2 / IC2 mod. Use whenever implementing, extending, or refactoring any item,
  block, TileEntity, EU support machine, or custom IC2 crop so that each item is verified
  against its archetype contract, asset guidelines, translations, and Gradle build before
  starting the next item.
---

# Crop Manager — Standardized Per-Item Gated Testing

## Core Rule (Hard Stop Gate)

Implement **one item/feature at a time**. **NEVER** begin coding Item N+1 until Item N passes **100%** of:
1. Its dedicated JUnit test suite (`src/test/java/br/cropmanager/<Feature>Test.java`) covering every mandatory row of its **Archetype** below.
2. The **Global Gate (G1–G5)** (assets, PNGs/animations, `en_us`/`pt_br` parity, config bounds/NaN safety, and clean Gradle build).
3. **Zero cheating:** no `@Ignore`/skipped tests, no deleted assertions, no weakening thresholds to force green.

---

## Global Gate (Mandatory for EVERY Item, Block, Machine, or Crop)

| Gate | Scope | Required Verification |
| :---: | :--- | :--- |
| **G1** | **Assets & Models** | Register the new block/item/crop in `AssetIntegrityTest.java`. Verify `blockstates/*.json`, `models/block/*.json`, `models/item/*.json`, all `cropmanager:` model/texture references exist on disk, and custom 3D elements stay within `[0..16]`. |
| **G2** | **PNG & `.mcmeta`** | Follow `docs/TEXTURE_GUIDELINES.md`: every new PNG must be **8-bit RGBA** (`bitDepth=8, colorType=6`), exact resolution (`16×16` blocks/items/crops; `16×(16*N)` animated strips; `512×512` machine GUIs). Every animated texture (`h > w`) must have a `.png.mcmeta` with `"interpolate": false` and `"frametime" > 0`. Regenerate dark GUI pack via `python tools/generate_gui_textures.py` if any GUI changed. |
| **G3** | **Localization Parity** | Exact 1:1 key parity between `en_us.lang` and `pt_br.lang` (no duplicate keys, no empty values, no `\ufffd`). Include `.name`, `_help` tooltips, and every GUI/`condition`/status key used by the new code. |
| **G4** | **Config & NaN Safety** | If keys are added to `ManagerConfig.java`, add tests in `ManagerConfigTest.java` verifying defaults, custom values, clamping outside `[min, max]`, and `NaN`/`Infinity` repair on any `finiteFloat` (always restoring defaults in `finally`). |
| **G5** | **Clean Gradle Build** | Run with local Gradle cache (Java 17+ host, Java 8 bytecode target):<br>`$env:GRADLE_USER_HOME = "$PWD\.gradle-home"; .\gradlew.bat processResources --rerun test --rerun jar --rerun reobfJar --rerun --no-daemon --console=plain`<br>Must exit `0` with `verifyIc2`, `compileJava`, all JUnit suites (`0` failures, `0` errors, `0` skipped), and `reobfJar` passing. |

---

## Standardized Test Matrix by Archetype

Use `net.minecraft.init.Bootstrap.register()`, `TileEntity.register("cropmanager:<id>", ...)`, and `Mockito.mock(World.class)` (server-side `isRemote == false`) so every test runs headlessly without launching Minecraft.

### Archetype A — Consumable / Tool Item
*(e.g., Organic Compost, Catalytic Fertilizer, Mutagenic Powder, Probe, Upgrades)*

- **A1. Happy Path:** valid target accepted (`canApply`/action returns `true`) and exact state change applied.
- **A2. Guards & Caps:** rejects `ItemStack.EMPTY`, `null` target, wrong tile/block, and already-saturated targets; caps values safely (e.g., IC2 nutrients/water `0..100`, IC2 crop stats `0..31`, charges `>= 0`).
- **A3. Consumption & Side Guard:** survival consumes 1 unit/durability **only** on success; creative does not consume; `world.isRemote == true` makes no server state mutations; respects `canPlayerEdit`.
- **A4. Deterministic RNG** *(if probabilistic)*: inject/seed `Random` to test both success and failure branches and verify the target is never corrupted on failure.
- **A5. Machine/Pipeline Hooks** *(if applicable)*: verify recognition by `ManagerTile.isFertilizer`, `ComposterTile.organic`, or target machine `isItemValid`.

### Archetype B — Passive or World-Interactive Block / Tile (No EU)
*(e.g., Manual Composter, Sprinklers Mk1–3, Pollinator Hive, Greenhouse Glass)*

- **B1. Core World Rule:** validates primary mechanic (e.g., light/skylight check, scan radius, speed multiplier, weed suppression, crop growth/crossing boost).
- **B2. `update()` & Side Guard** *(if `ITickable`)*: advances cooldown/progress at configured rate, fires effect on interval, halts cleanly when missing inputs/water/conditions, and is a no-op when `world.isRemote == true`.
- **B3. Area Bounds & Idempotency:** ignores unloaded chunks (`!world.isBlockLoaded(pos, false)`), `y < 0 || y >= world.getHeight()`, incompatible tiles, and already-maxed targets.
- **B4. NBT & Corruption Clamping** *(if `TileEntity`)*: `writeToNBT` → `readFromNBT` preserves full state; invalid NBT values (out-of-range tier, `duration <= 0`, `progress > duration`, negative charges) are clamped safely.
- **B5. Lifecycle & Capabilities** *(if `TileEntity`)*: `shouldRefresh` returns `false` for same-block state updates (`ACTIVE`/`FILL`) and `true` when block changes; `dropContents()` spawns & clears items without duplication; `IItemHandler`/`IFluidHandler` filters invalid items/fluids and tolerates `EnumFacing == null`.

### Archetype C — EU-Powered Support Machine (`FarmTile` + `IUpgradableBlock`)
*(e.g., Seed Analyzer, Electric Composter, Grin Grinder/Extractor, Hydroponic Chamber)*

- **C1. Full Processing Cycle:** with valid inputs + EU (+ fluid/heat/structure if required), consumes exact `euPerTick` over `workTicks`, produces expected output, and updates `condition` & telemetry.
- **C2. Stall, Abort & Structural Gates:**
  - Low EU → `no_energy`, `progress` does not advance, inputs untouched.
  - Full/incompatible output → `no_space`, `0` EU spent, no item loss.
  - Missing input mid-cycle → `progress` resets cleanly.
  - **Structural requirements** *(if applicable, e.g., Hydroponic Chamber)*: valid structure enables work; missing/invalid required neighbor blocks work with explicit `condition` and `0` EU spent; optional bonuses scale speed without corrupting state; restoring structure resumes cleanly.
- **C3. IC2 Energy, Upgrades & NaN Recovery:** `isSupportMachine() == true`, `energySink() != null`, `applyIc2Upgrades` scales `workTicks`/`euPerTick`/`capacity`/`sinkTier`, upgrade slots reject non-IC2 items, battery slot requires `IElectricItem`, and `NaN`/`Infinity` EU in NBT recovers to `0`.
- **C4. 6-Face + `null` Automation:** test `itemsForFace` / capabilities across all `EnumFacing.values()` + `null`: only valid inputs accepted in input slots, no extraction from input/upgrade/battery slots, output slot is extract-only; tanks enforce fluid filter and `canFill`/`canDrain`.
- **C5. NBT & GUI Sync Round-Trip:** `writeToNBT` → `readFromNBT` and `guiState()` → `acceptGuiState()` round-trip faithfully with `duration >= 1` and `tier >= 1`.

### Archetype D — Custom IC2 Crop (`BaseCropCard` / `CropCard`)
*(e.g., Grinbloom, Bioreed, Radbloom, Glowbloom)*

- **D1. Registration & Metadata:** present in `CropRegistry.ALL`, `owner == "cropmanager"`, `maxSize >= 2`, `1 <= sizeAfterHarvest < maxSize`, `getTexturesLocation().size() == maxSize`, `unlocalizedName == "cropmanager.crop." + id`, and `CropRegistry.registerAll()` honors `enableCustomCrops = false`.
- **D2. Growth Gates (`canGrow`):** blocks at `size >= maxSize`, blocks below min light (`< 9`), allows at threshold, and enforces catalyst (`size >= 2`) and subsoil (`isBlockBelow`) rules.
- **D3. Harvest & Drops:** `canBeHarvested` is `false` for `size < maxSize` and `true` at `maxSize`; `previewGain()` and `getGain()` return defensive non-empty copies; catalyzed crops decrement charge by `1` (floor `0`).
- **D4. Passive Effects** *(if applicable)*: `getEmittedLight` matches stage curve; tick/collision auras (e.g., radiation) respect Hazmat/creative guards and config disable (`radius == 0`).

---

## Gate Sign-Off Checklist (Required Before Starting Next Item)

Before moving to the next item in a plan, verify and report:
- [ ] Item `<name>` matches Archetype `<A/B/C/D>` — all required test cases added in `<Feature>Test.java`.
- [ ] Global Gate G1–G4 updated (`AssetIntegrityTest`, `TEXTURE_GUIDELINES.md` check, `en_us`/`pt_br` parity, `ManagerConfigTest`).
- [ ] Global Gate G5 executed and passed (`BUILD SUCCESSFUL`, exact JUnit counts: `X suites, Y tests, 0 failures, 0 errors, 0 skipped`).
- [ ] Only after all boxes above are green: unlock Item N+1.
