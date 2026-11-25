#!/usr/bin/env python3
"""Generate authentic IC2-grade 16x16 pixel-art textures, 3D JSON models, and blockstates for Crop Manager.

Strictly follows docs/TEXTURE_GUIDELINES.md:
- Machine exteriors derive from the audited IC2 bright steel casing used by Advanced Crop Manager,
  Electric Composter, Grin Grinder, and Grin Extractor.
- All block/item textures are 16x16 RGBA 8-bit (no interpolation, no anti-aliasing).
- Animated front textures are 16x64 (4 frames) with .mcmeta {"animation":{"frametime":5,"interpolate":false}}.
- Every custom crop species has its own hand-pixeled silhouette across stages 1..4.
"""

import json
from io import BytesIO
from pathlib import Path
from zipfile import ZipFile
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
IC2_JAR = ROOT / "libs" / "industrialcraft-2-2.8.222-ex112.jar"
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "cropmanager"
TEX_BLOCKS = ASSETS / "textures" / "blocks"
TEX_CROPS = TEX_BLOCKS / "crops"
TEX_ITEMS = ASSETS / "textures" / "items"
BLOCKSTATES = ASSETS / "blockstates"
MODELS_BLOCK = ASSETS / "models" / "block"
MODELS_ITEM = ASSETS / "models" / "item"

for d in (TEX_BLOCKS, TEX_CROPS, TEX_ITEMS, BLOCKSTATES, MODELS_BLOCK, MODELS_ITEM):
    d.mkdir(parents=True, exist_ok=True)

# Official Crop Manager / IC2 Palette (docs/TEXTURE_GUIDELINES.md)
PAL = {
    ".": (0, 0, 0, 0),
    # IC2 structural steel & mechanical cavity (Section 1.1)
    "0": (0x11, 0x16, 0x19, 255),  # Cavidade negra
    "1": (0x1C, 0x22, 0x26, 255),  # Sombra profunda
    "2": (0x2B, 0x32, 0x37, 255),  # Cinza escuro
    "3": (0x46, 0x50, 0x56, 255),  # Chanfro interno
    "4": (0x5B, 0x65, 0x6B, 255),  # Cinza medio
    "5": (0x70, 0x7A, 0x80, 255),  # Aco estrutural
    "6": (0xB0, 0xB8, 0xBC, 255),  # Highlight 3D
    "7": (0xDD, 0xE1, 0xE0, 255),  # Brilho de rebites
    # Semantic IC2 accents (Section 1.3)
    "b": (0x2D, 0x7E, 0xA4, 255),  # Blue telemetry
    "B": (0x61, 0xB6, 0xCF, 255),  # Blue highlight
    "c": (0x1A, 0x5A, 0x6B, 255),  # Deep cyan
    "C": (0x38, 0xB6, 0xC8, 255),  # Cyan core
    "Q": (0x8D, 0xEE, 0xF7, 255),  # Cyan specular
    "e": (0x1B, 0x45, 0x22, 255),  # Deep forest green
    "g": (0x28, 0x6B, 0x32, 255),  # Shadow green
    "G": (0x3A, 0x9B, 0x46, 255),  # Leaf green
    "h": (0x63, 0xC2, 0x52, 255),  # Bright leaf
    "H": (0x9B, 0xEC, 0x6E, 255),  # Specular sprout green
    "l": (0xB6, 0xD9, 0x57, 255),  # Lime bio-sap
    # Earth / Wood / Humus
    "k": (0x1E, 0x13, 0x0B, 255),  # Deep earth outline
    "w": (0x3B, 0x24, 0x14, 255),  # Dark wood/soil
    "W": (0x58, 0x38, 0x20, 255),  # Mid wood/soil
    "m": (0x75, 0x4D, 0x2E, 255),  # Warm wood
    "M": (0x94, 0x67, 0x41, 255),  # Wood highlight
    "t": (0xB4, 0x86, 0x56, 255),  # Light twine / straw
    # Copper & Gold (Section 1.3)
    "r": (0x66, 0x3A, 0x1A, 255),  # Deep copper
    "R": (0x94, 0x5B, 0x2A, 255),  # Copper
    "o": (0xCE, 0x7E, 0x3D, 255),  # Bright copper
    "O": (0xF5, 0xB4, 0x74, 255),  # Copper specular
    "y": (0x8F, 0x69, 0x1E, 255),  # Deep gold
    "Y": (0xC8, 0x9D, 0x41, 255),  # Gold
    "f": (0xF2, 0xC9, 0x4C, 255),  # Bright gold
    "F": (0xFF, 0xF0, 0x9E, 255),  # Gold specular
    # Amber / Nitro
    "a": (0x8F, 0x48, 0x14, 255),  # Deep amber
    "A": (0xD6, 0x7A, 0x1E, 255),  # Amber
    "n": (0xF5, 0xAD, 0x36, 255),  # Bright amber
    "N": (0xFF, 0xE6, 0x82, 255),  # Amber glow
    # Toxic Purple (Grinbloom / Venomilia style)
    "p": (0x4D, 0x1E, 0x6B, 255),  # Deep violet
    "P": (0x82, 0x35, 0xAD, 255),  # Violet
    "v": (0xB9, 0x5E, 0xE0, 255),  # Bright magenta-violet
    "V": (0xE6, 0xA8, 0xFF, 255),  # Petal highlight
    # Lapis Royal Blue (Lazuli Bloom)
    "u": (0x16, 0x2A, 0x7A, 255),  # Deep lapis
    "U": (0x24, 0x4E, 0xC4, 255),  # Royal lapis
    "z": (0x54, 0x8B, 0xFF, 255),  # Bright lapis crystal
    "Z": (0xB8, 0xDC, 0xFF, 255),  # Lapis specular
    # Pearlescent Iridium (Irid Moss)
    "i": (0x7D, 0x8A, 0x99, 255),  # Cool iridium shadow
    "I": (0xC5, 0xD2, 0xDE, 255),  # Silver iridium
    "s": (0xE4, 0xEC, 0xF5, 255),  # Pearl iridium highlight
    "S": (0xFF, 0xFF, 0xFF, 255),  # Pure crystal glint
    # Radioactive Uranium Glow (Radioactive Soil & Radbloom)
    "x": (0x2D, 0x8C, 0x24, 255),  # Uranium green shadow
    "X": (0x66, 0xE8, 0x38, 255),  # Bright uranium glow
    "q": (0xBE, 0xFF, 0x6A, 255),  # Neon radioactive core
    "J": (0xEE, 0xFF, 0xC8, 255),  # White-green glow peak
    # Lithium Silver-Pink & Sulfur Bright Yellow
    "d": (0x9C, 0x5B, 0x7A, 255),  # Deep lithium rose
    "D": (0xDE, 0x99, 0xBC, 255),  # Lithium crystal pink
    "K": (0xFF, 0xD4, 0xEA, 255),  # Lithium highlight
    "j": (0xC9, 0xA8, 0x16, 255),  # Sulfur shadow
    "E": (0xF7, 0xE2, 0x3B, 255),  # Bright sulfur yellow
}


def save_png(path: Path, im: Image.Image) -> None:
    im.convert("RGBA").save(path, format="PNG")


def save_json(path: Path, data: dict) -> None:
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def from_grid(rows: list[str], extra_pal: dict | None = None) -> Image.Image:
    assert len(rows) == 16, f"Expected 16 rows, got {len(rows)}"
    pal = PAL if extra_pal is None else {**PAL, **extra_pal}
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    for y, row in enumerate(rows):
        assert len(row) == 16, f"Row {y} length {len(row)} != 16"
        for x, ch in enumerate(row):
            px[x, y] = pal[ch]
    return im


def load_ic2_casings() -> dict[str, Image.Image]:
    """Load authentic bright IC2/CropManager machine casings so Seed Analyzer matches existing machines."""
    side = Image.open(TEX_BLOCKS / "advanced_crop_manager_side.png").convert("RGBA").crop((0, 0, 16, 16))
    bottom = Image.open(TEX_BLOCKS / "electric_composter_bottom.png").convert("RGBA").crop((0, 0, 16, 16))
    with ZipFile(IC2_JAR) as z:
        macerator_side = Image.open(
            BytesIO(z.read("assets/ic2/textures/blocks/machine/processing/basic/macerator_leftrightback.png"))
        ).convert("RGBA").resize((16, 16), Image.NEAREST)
    return {"side": side, "bottom": bottom, "macerator": macerator_side}


def generate_seed_analyzer_assets() -> None:
    casings = load_ic2_casings()
    # Base directly on the clean, bright IC2 4-quadrant casing (same as electric_composter & advanced_crop_manager)
    clean_panel = casings["side"].copy()

    # Side & Back: clean bright IC2 steel casing (no heavy black blocks)
    side = clean_panel.copy()
    sd = ImageDraw.Draw(side)
    # Subtle 1px horizontal micro-slot near lower center for sleek industrial identity
    sd.line((5, 11, 10, 11), fill=PAL["3"])
    sd.line((5, 12, 10, 12), fill=PAL["6"])
    save_png(TEX_BLOCKS / "seed_analyzer_side.png", side)
    save_png(TEX_BLOCKS / "seed_analyzer_back.png", clean_panel.copy())

    # Top: clean bright IC2 casing with a compact, delicate 6x6 optical glass aperture (4px margin all around)
    top = clean_panel.copy()
    td = ImageDraw.Draw(top)
    td.rectangle((5, 5, 10, 10), fill=PAL["3"])
    td.line((5, 10, 10, 10), fill=PAL["7"])
    td.line((10, 5, 10, 10), fill=PAL["7"])
    td.rectangle((6, 6, 9, 9), fill=PAL["c"])
    td.rectangle((6, 6, 8, 8), fill=PAL["b"])
    td.point((6, 6), fill=PAL["Q"])
    td.point((7, 6), fill=PAL["B"])
    td.point((6, 7), fill=PAL["B"])
    save_png(TEX_BLOCKS / "seed_analyzer_top.png", top)

    # Bottom: authentic IC2 reinforced base
    save_png(TEX_BLOCKS / "seed_analyzer_bottom.png", casings["bottom"].copy())

    # Front: sleek, compact inset console matching advanced_crop_manager_front & agricultural_monitor_front
    def build_front(frame_idx: int) -> Image.Image:
        im = clean_panel.copy()
        d = ImageDraw.Draw(im)

        # Delicate top status bar (x=6..9, y=2) — 1px height, matching Advanced Crop Manager
        top_bar = PAL["B"] if frame_idx >= 0 else PAL["b"]
        d.line((6, 2, 9, 2), fill=top_bar)
        d.point((6, 2), fill=PAL["Q"] if frame_idx >= 0 else PAL["B"])

        # Compact recessed OLED screen (x=4..11, y=4..9) with fine 1px metallic bevel (no heavy black slab)
        d.line((4, 4, 11, 4), fill=PAL["3"])   # top inset shadow
        d.line((4, 4, 4, 9), fill=PAL["3"])    # left inset shadow
        d.line((4, 9, 11, 9), fill=PAL["7"])   # bottom catch highlight
        d.line((11, 4, 11, 9), fill=PAL["7"])  # right catch highlight
        d.rectangle((5, 5, 10, 8), fill=PAL["0"])  # crisp 6x4 flat OLED interior

        if frame_idx < 0:
            # Idle: fine 1px muted botanical/DNA curve inside 6x4 display
            for sx, sy in ((6, 7), (7, 6), (8, 6), (9, 7)):
                d.point((sx, sy), fill=PAL["g"])
            d.point((7, 7), fill=PAL["e"])
            d.point((8, 7), fill=PAL["e"])
        else:
            # Active: subtle 1px vertical laser scanline sweeping x=5..10 across crisp 1px green seed waveform
            beam_x = [5, 6, 8, 10][frame_idx]
            # Fine 1px trace illuminated behind the beam
            for sx, sy in ((6, 7), (7, 6), (8, 6), (9, 7)):
                d.point((sx, sy), fill=PAL["G"] if sx <= beam_x else PAL["e"])
            # Crisp 1px laser sweep
            for sy in range(5, 9):
                d.point((beam_x, sy), fill=PAL["h"])
            d.point((beam_x, 6), fill=PAL["H"])
            d.point((beam_x, 7), fill=PAL["Q"])

        # Sleek 1px thin Seed Bag insertion slot (x=5..10, y=11) with fine silver lip
        d.line((5, 11, 10, 11), fill=PAL["1"])
        d.line((5, 12, 10, 12), fill=PAL["7"])

        # Minimal 1px micro-indicators (y=13, matching advanced_crop_manager bottom pips)
        d.point((5, 13), fill=PAL["3"])
        d.point((7, 13), fill=PAL["3"])
        d.point((8, 13), fill=PAL["3"])
        d.point((10, 13), fill=PAL["H"] if (frame_idx >= 0 and frame_idx % 2 == 0) else PAL["b"])
        return im

    save_png(TEX_BLOCKS / "seed_analyzer_front.png", build_front(-1))

    # Active 16x64 (4 frames) + .mcmeta (interpolate: false)
    anim = Image.new("RGBA", (16, 64), (0, 0, 0, 0))
    for f in range(4):
        anim.paste(build_front(f), (0, f * 16))
    save_png(TEX_BLOCKS / "seed_analyzer_front_active.png", anim)
    save_json(TEX_BLOCKS / "seed_analyzer_front_active.png.mcmeta", {"animation": {"frametime": 5, "interpolate": False}})

    save_json(BLOCKSTATES / "seed_analyzer.json", {
        "variants": {
            "active=false,facing=north": {"model": "cropmanager:seed_analyzer", "y": 0, "uvlock": True},
            "active=false,facing=east": {"model": "cropmanager:seed_analyzer", "y": 90, "uvlock": True},
            "active=false,facing=south": {"model": "cropmanager:seed_analyzer", "y": 180, "uvlock": True},
            "active=false,facing=west": {"model": "cropmanager:seed_analyzer", "y": 270, "uvlock": True},
            "active=true,facing=north": {"model": "cropmanager:seed_analyzer_active", "y": 0, "uvlock": True},
            "active=true,facing=east": {"model": "cropmanager:seed_analyzer_active", "y": 90, "uvlock": True},
            "active=true,facing=south": {"model": "cropmanager:seed_analyzer_active", "y": 180, "uvlock": True},
            "active=true,facing=west": {"model": "cropmanager:seed_analyzer_active", "y": 270, "uvlock": True},
        }
    })
    for suffix, front in (("", "seed_analyzer_front"), ("_active", "seed_analyzer_front_active")):
        save_json(MODELS_BLOCK / f"seed_analyzer{suffix}.json", {
            "parent": "block/cube",
            "textures": {
                "down": "cropmanager:blocks/seed_analyzer_bottom",
                "up": "cropmanager:blocks/seed_analyzer_top",
                "north": f"cropmanager:blocks/{front}",
                "south": "cropmanager:blocks/seed_analyzer_back",
                "east": "cropmanager:blocks/seed_analyzer_side",
                "west": "cropmanager:blocks/seed_analyzer_side",
                "particle": "cropmanager:blocks/seed_analyzer_side",
            },
        })
    save_json(MODELS_ITEM / "seed_analyzer.json", {"parent": "cropmanager:block/seed_analyzer"})


def generate_manual_composter_assets() -> None:
    # 1. Warm oak barrel/tub planks inspired by Actually Additions' Compost
    wood = from_grid([
        "wWWWWWWWWWWWWWWw",
        "WttMtMMttMMtMttW",
        "WMmMMmmMMmMMmmMW",
        "WmmWmmmWWmmmWmmW",
        "WWWWWWWWWWWWWWWW",
        "wttttMMttMMttttw",
        "WMmmMMmMMmMMmmMW",
        "WmmWWmmWWmmWWmmW",
        "WWWWWWWWWWWWWWWW",
        "wttMMtMttMtMMttw",
        "WMmMMmmMMmmMMmMW",
        "WmmmWmmWWmmWmmmW",
        "WWWWWWWWWWWWWWWW",
        "wtMttMMttMMttMtw",
        "WMmmWmmMMmmWmmMW",
        "wwwwwwwwwwwwwwww",
    ])
    save_png(TEX_BLOCKS / "manual_composter_wood.png", wood)

    # 2. Dark-oak / spruce wooden rim & corner posts (Actually Additions wooden trough trim, no steel posts)
    rim = from_grid([
        "kkkkkkkkkkkkkkkk",
        "kMMMMMMMMMMMMMMw",
        "kMtWWWWWWWWWWtWw",
        "kMWmmmmmmmmmmWWw",
        "kMWmWWWWWWWWmWWw",
        "kMWmWwwwwwwWmWWw",
        "kMWmWwWWWWwWmWWw",
        "kMWmWwWmmWwWmWWw",
        "kMWmWwWmmWwWmWWw",
        "kMWmWwWWWWwWmWWw",
        "kMWmWwwwwwwWmWWw",
        "kMWmWWWWWWWWmWWw",
        "kMWmmmmmmmmmmWWw",
        "kMtWWWWWWWWWWtWw",
        "kwwwwwwwwwwwwwww",
        "kkkkkkkkkkkkkkkk",
    ])
    save_png(TEX_BLOCKS / "manual_composter_rim.png", rim)
    legacy_steel = TEX_BLOCKS / "manual_composter_steel.png"
    if legacy_steel.exists():
        legacy_steel.unlink()

    # 3. Raw organic matter (fresh chopped leaves, twigs & straw - IC2 plant_ball inspired)
    raw = from_grid([
        "egGhGegWgGhGegGe",
        "gGHhGgeGGhHggGhe",
        "GhGegmWgeGgGhgGg",
        "egGgmMtmgGgGeGhg",
        "gGhgGwmGgGhGegGe",
        "GeGhgGegGHhGgGhe",
        "egGgGeGgGhGegmWg",
        "gGhGegGgeGgGmMtg",
        "GHhGgmWgeGhGgwmG",
        "gGgGmMtggGHhGgGe",
        "egGgGwmGgGgGhgGg",
        "gGheGgGeGgGeGhgG",
        "GhGgGhGegGhgGegG",
        "egGGhHggGhHgeGhe",
        "gGgGgGegGgGgGgGe",
        "egGegGgeGgGegGge",
    ])
    save_png(TEX_BLOCKS / "manual_composter_raw.png", raw)

    # 4. Fermenting moist compost (rich damp earth + decaying leaf bits - IC2 fertilizer inspired)
    fermenting = from_grid([
        "kwWWwkwWgwWWwkwW",
        "wWmWWwWgWWmWWwkW",
        "WWwkwWWwkwWWwWgw",
        "wWWwWmWWwWmWWwWW",
        "kwgWWwkwWWwkwWWw",
        "wWgWmWwkWWmWWwkw",
        "WWwwWWwWWwWWgwWW",
        "wkWWwkwWmWWwWWwk",
        "wWWmWWwWWwkwWmWW",
        "WWwkwWWgwWWwWWwW",
        "wWWwWmWWwWWmWWkw",
        "kwWWwWWwkwWWwWgw",
        "wWmWWwkwWWmWWwWW",
        "WWwWgwWWwWWwkwWW",
        "wkwWWwWmWWwWmWWw",
        "kwWWwkwWWwkwWWwk",
    ])
    save_png(TEX_BLOCKS / "manual_composter_fermenting.png", fermenting)

    # 5. Ready rich dark humus (velvety dark compost with nutrient amber & green flecks)
    ready = from_grid([
        "kwkwWWkwkwWWkwkw",
        "wWkwWwkwWWkYwWkw",
        "kwWWkwWWkwWWkwWW",
        "wkwYkwkwWWkwgkwW",
        "WWkwWWkwkwWWkwkw",
        "kwWWkwgWkwWwkwWW",
        "wkwkwWWkwYkwWWkw",
        "WWkwWWkwWWkwkwWw",
        "kwWwkWkwkwWWkYkw",
        "wkwWWkwWWkwWWkwW",
        "WWkYkwWWkwgWkwkw",
        "kwWWkwkwWWkwWWkw",
        "wkwWwkwWWkwYkwWW",
        "WWkwgWkwkwWWkwkw",
        "kwWWkwWWkwWwkwWW",
        "wkwkwWWkwkwWWkwk",
    ])
    save_png(TEX_BLOCKS / "manual_composter_ready.png", ready)

    # Half-height (8px / 0.5 block) open wooden tub inspired by Actually Additions' Compost
    def faces_all(tex, uv=None):
        f = {"texture": f"#{tex}"}
        if uv is not None:
            f["uv"] = uv
        return {side: dict(f) for side in ("up", "down", "north", "south", "east", "west")}

    def plank_ns(y0, y1, z0, z1):
        uv_side = [0, 0, 16, 8]
        return {
            "from": [3, y0, z0],
            "to": [13, y1, z1],
            "faces": {
                "north": {"texture": "#wood", "uv": uv_side},
                "south": {"texture": "#wood", "uv": uv_side},
                "up": {"texture": "#wood", "uv": [0, 0, 16, 2]},
                "down": {"texture": "#wood", "uv": [0, 14, 16, 16]},
            },
        }

    def plank_ew(y0, y1, x0, x1):
        uv_side = [0, 0, 16, 8]
        return {
            "from": [x0, y0, 3],
            "to": [x1, y1, 13],
            "faces": {
                "east": {"texture": "#wood", "uv": uv_side},
                "west": {"texture": "#wood", "uv": uv_side},
                "up": {"texture": "#wood", "uv": [0, 0, 16, 2]},
                "down": {"texture": "#wood", "uv": [0, 14, 16, 16]},
            },
        }

    base_elements = [
        # Bottom wooden floor (y=0.5..1.5)
        {"from": [2, 0.5, 2], "to": [14, 1.5, 14], "faces": faces_all("wood", [0, 0, 16, 16])},
        # 4 Low dark-oak corner posts (y=0..8, exact half-block height)
        {"from": [1, 0, 1], "to": [3, 8, 3], "faces": faces_all("rim", [0, 0, 4, 16])},
        {"from": [13, 0, 1], "to": [15, 8, 3], "faces": faces_all("rim", [12, 0, 16, 16])},
        {"from": [1, 0, 13], "to": [3, 8, 15], "faces": faces_all("rim", [0, 0, 4, 16])},
        {"from": [13, 0, 13], "to": [15, 8, 15], "faces": faces_all("rim", [12, 0, 16, 16])},
        # 2 tiers of tight oak barrel planks (y=1.0..4.0 and y=4.2..7.0)
        plank_ns(1.0, 4.0, 1.5, 2.5),
        plank_ns(1.0, 4.0, 13.5, 14.5),
        plank_ew(1.0, 4.0, 1.5, 2.5),
        plank_ew(1.0, 4.0, 13.5, 14.5),
        plank_ns(4.2, 7.0, 1.5, 2.5),
        plank_ns(4.2, 7.0, 13.5, 14.5),
        plank_ew(4.2, 7.0, 1.5, 2.5),
        plank_ew(4.2, 7.0, 13.5, 14.5),
        # Top dark-oak lip rails (y=7.0..8.0)
        {"from": [3, 7.0, 1], "to": [13, 8.0, 2.6], "faces": faces_all("rim", [2, 0, 14, 3])},
        {"from": [3, 7.0, 13.4], "to": [13, 8.0, 15], "faces": faces_all("rim", [2, 0, 14, 3])},
        {"from": [1, 7.0, 3], "to": [2.6, 8.0, 13], "faces": faces_all("rim", [0, 2, 3, 14])},
        {"from": [13.4, 7.0, 3], "to": [15, 8.0, 13], "faces": faces_all("rim", [13, 2, 16, 14])},
    ]

    fill_specs = {
        0: (None, None),
        1: (3.2, "cropmanager:blocks/manual_composter_raw"),
        2: (4.8, "cropmanager:blocks/manual_composter_raw"),
        3: (6.2, "cropmanager:blocks/manual_composter_raw"),
        4: (7.2, "cropmanager:blocks/manual_composter_fermenting"),
        5: (7.2, "cropmanager:blocks/manual_composter_ready"),
    }

    variants = {}
    for level, (top_y, fill_tex) in fill_specs.items():
        textures = {
            "wood": "cropmanager:blocks/manual_composter_wood",
            "rim": "cropmanager:blocks/manual_composter_rim",
            "particle": "cropmanager:blocks/manual_composter_wood",
        }
        elements = list(base_elements)
        if top_y is not None and fill_tex is not None:
            textures["fill"] = fill_tex
            elements.append({
                "from": [2.5, 1.5, 2.5],
                "to": [13.5, top_y, 13.5],
                "faces": {
                    "up": {"texture": "#fill", "uv": [1, 1, 15, 15]},
                    "north": {"texture": "#fill", "uv": [1, 4, 15, 12]},
                    "south": {"texture": "#fill", "uv": [1, 4, 15, 12]},
                    "east": {"texture": "#fill", "uv": [1, 4, 15, 12]},
                    "west": {"texture": "#fill", "uv": [1, 4, 15, 12]},
                },
            })
        save_json(MODELS_BLOCK / f"manual_composter_{level}.json", {
            "parent": "block/block",
            "textures": textures,
            "elements": elements,
            "display": {
                "gui": {"rotation": [30, 225, 0], "translation": [0, 1.5, 0], "scale": [0.65, 0.65, 0.65]},
                "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
                "fixed": {"rotation": [0, 0, 0], "translation": [0, 1, 0], "scale": [0.5, 0.5, 0.5]},
            },
        })
        variants[f"fill={level}"] = {"model": f"cropmanager:manual_composter_{level}"}

    save_json(BLOCKSTATES / "manual_composter.json", {"variants": variants})
    save_json(MODELS_ITEM / "manual_composter.json", {"parent": "cropmanager:block/manual_composter_5"})


def generate_item_assets() -> None:
    # 1. organic_compost.png — IC2 fertilizer mound silhouette, rich dark humus + mossy organic flecks
    organic_compost = from_grid([
        "................",
        "................",
        ".......kk.......",
        "......kWWk......",
        ".....kWmWWk.....",
        "....kWgmWWwk....",
        "....kWmWWgwk....",
        "...kWmWWwWWwk...",
        "..kWmGWWwmWwwk..",
        "..kWWgWWwWWwwk..",
        ".kWWmWWwGWwWwwk.",
        ".kWwwWWwwWwwwwk.",
        "..kwwwWWwwwwwk..",
        "...kkwwwwwwkk...",
        ".....kkkkkk.....",
        "................",
    ])
    save_png(TEX_ITEMS / "organic_compost.png", organic_compost)

    # 2. catalytic_fertilizer.png — crystalline cyan-gold catalytic mineral mound with glowing sparks
    catalytic = from_grid([
        "................",
        "........F.......",
        ".......11.......",
        "......1QC1...f..",
        ".....1QQCC1.....",
        "....1QCCFbc1....",
        "....1QCCCb1.....",
        "...1QCfCCbc1....",
        "..1QQCCbCFbc1...",
        "..1QCCbbCCbc1...",
        ".1QCFCbbCfbbc1..",
        ".1CCbbcbbCbbc1..",
        "..1cbbbccbbcc1..",
        "...11cccccc11...",
        ".....111111.....",
        "................",
    ])
    save_png(TEX_ITEMS / "catalytic_fertilizer.png", catalytic)

    # 3. nitro_essence.png — glowing amber nutrient bulb cradled in dark-emerald calyx leaves
    nitro = from_grid([
        "................",
        "........e.......",
        ".......egH......",
        "......egGg......",
        "....e.1GG1.e....",
        ".....11aa11.....",
        "....1aAnNAa1....",
        "...1aAnNNnAa1...",
        "...1ANnNNnAA1...",
        "...1ANnnAAaA1...",
        "...1aAnnAAaa1...",
        "....1aAAAAa1....",
        "....1aaAAaa1....",
        ".....1aaaa1.....",
        "......1111......",
        "................",
    ])
    save_png(TEX_ITEMS / "nitro_essence.png", nitro)

    # 4. bio_fiber.png — diagonal tied sheaf of segmented reed fibers (IC2 stickreed/plant style)
    bio_fiber = from_grid([
        "................",
        "..........eg.e..",
        ".........eGHeg..",
        "........eGHeGH..",
        ".......eGHeGHe..",
        "......egHeGHeg..",
        ".....eGHkkGHe...",
        "....eGHkMtkge...",
        "....eGkwMtk.....",
        "...eGkkwmk......",
        "..eGHeGkk.......",
        "..GHeGHeg.......",
        ".egHeGHeg.......",
        ".gHeGHe.........",
        ".egege..........",
        "................",
    ])
    save_png(TEX_ITEMS / "bio_fiber.png", bio_fiber)

    # 5. mutagenic_powder.png — crystalline violet-emerald powder mound with luminous spores
    mutagenic_powder = from_grid([
        "................",
        ".......H........",
        ".......11.......",
        "......1Vv1...q..",
        ".....1VVvv1.....",
        "....1VvhvPP1....",
        "....1VvvPPp1....",
        "...1VvHvvPPp1...",
        "..1VVvvPPvXpp1..",
        "..1VvPPpPvPpp1..",
        ".1VvXvPPpPvhpp1.",
        ".1PvPPpPPpPPpp1.",
        "..1pPppgPpppp1..",
        "...11pppppp11...",
        ".....111111.....",
        "................",
    ])
    save_png(TEX_ITEMS / "mutagenic_powder.png", mutagenic_powder)

    for item_name in ("organic_compost", "catalytic_fertilizer", "mutagenic_powder", "nitro_essence", "bio_fiber"):
        save_json(MODELS_ITEM / f"{item_name}.json", {
            "parent": "item/generated",
            "textures": {"layer0": f"cropmanager:items/{item_name}"},
        })


def generate_crop_sprites() -> None:
    """Hand-pixeled 16x16 stages 1..4 for each of the 5 custom IC2 crops, each with a unique silhouette."""
    crops = {
        # 1. GRINBLOOM: Spiky toxic twin bell-flowers (IC2 venomilia DNA)
        "grinbloom": [
            [
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                ".......e........",
                ".....e.G.e......",
                "....eGgGgGe.....",
                ".....egGge......",
                "......ege.......",
                "......ege.......",
            ],
            [
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "....p.....p.....",
                "...ePe...ePe....",
                "...eGe...eGe....",
                "..eGge...egGe...",
                "..gGe.e.e.eGg...",
                "...eGgGgGgGe....",
                "....egGeGge.....",
                ".....eg.ge......",
                ".....eg.ge......",
                ".....eg.ge......",
            ],
            [
                "................",
                "................",
                "................",
                "...pP.....Pp....",
                "..pPvp...pvPp...",
                "..pPHP...PHPp...",
                "...pP.....Pp....",
                "...eGe...eGe....",
                "..eGge...egGe...",
                ".eGgGe.e.eGgGe..",
                "..egGe.G.eGge...",
                "...eGgGgGgGe....",
                "....egGeGge.....",
                "....egG.Gge.....",
                ".....eg.ge......",
                ".....eg.ge......",
            ],
            [
                "................",
                "..pPp.....pPp...",
                ".pPVvp...pvVPp..",
                ".pPvHPp.pPHvPp..",
                ".pPvHHp.pHHvPp..",
                "..pPgp...pgPp...",
                "...eGe...eGe....",
                ".e.eGge.egGe.e..",
                "eGgGgGe.eGgGgGe.",
                ".egGgeG.GegGge..",
                "..egGeGgGeGge...",
                "...eGgGeGgGe....",
                "....egG.Gge.....",
                "....egG.Gge.....",
                ".....eg.ge......",
                ".....eg.ge......",
            ],
        ],
        # 2. BIOREED: Tall segmented bamboo/biomass stalks (IC2 stickreed DNA)
        "bioreed": [
            [
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "...H.....h......",
                "...G.....G..H...",
                "...g..h..g..G...",
                "...G..G..G..g...",
                "...g..g..g..G...",
                "...e..e..e..e...",
            ],
            [
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "......H.........",
                "...H..G....H....",
                "...G..g....G..h.",
                "..eG..e...eG..G.",
                "...h..H....h..g.",
                "...G..G....G..e.",
                "...g..g....g..H.",
                "...e..e....e..G.",
                "...G..G....G..g.",
                "...g..g....g..e.",
            ],
            [
                "................",
                "................",
                "......H...H.....",
                "..H...G...G.....",
                "..G...g...g..H..",
                "..e..eG..eG..G..",
                "..h...h...h..g..",
                "..G...G...G..e..",
                "..g..hG...g..h..",
                "..e...e...e..G..",
                "..H...H..hH..g..",
                "..G...G...G..e..",
                "..g...g...g..H..",
                "..e...e...e..G..",
                "..G...G...G..g..",
                "..g...g...g..e..",
            ],
            [
                "......H...H.....",
                "..H...G...G..H..",
                "..G..lGl..G..G..",
                "..e...e...e..e..",
                ".hH...H..lHl.H..",
                "..G...G...G..G..",
                "..g...g...g..g..",
                "..e..lGl..e..e..",
                "..H...H...H.hH..",
                "..G...G...G..G..",
                "..g...g..lgl.g..",
                "..e...e...e..e..",
                ".lHl..H...H..H..",
                "..G...G...G..G..",
                "..g...g...g..g..",
                "..e...e...e..e..",
            ],
        ],
        # 3. NITROBULB: Broad-leafed rosette cradling twin glowing amber nutrient bulbs
        "nitrobulb": [
            [
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "......e..e......",
                "....egG..Gge....",
                "...eGHgeegHGe...",
                "....egGaaGge....",
                ".....eaaaae.....",
            ],
            [
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                ".....e....e.....",
                "...egG....Gge...",
                "..eGHg.ee.gHGe..",
                "...egGgGGgGge...",
                "....eGaAAaGe....",
                "...egAaNNAaGe...",
                "....eaAAAAae....",
                ".....eaaaae.....",
                "......eeee......",
            ],
            [
                "................",
                "................",
                "................",
                "................",
                "....e......e....",
                "..egG..ee..Gge..",
                ".eGHgeGHHGeGHGe.",
                "..egGgGggGgGge..",
                "...eaAAggAAae...",
                "..eaAnNggNnAae..",
                "..eaANnggnNAae..",
                "..eaAAaggAAaae..",
                "...eaaaaaaaae...",
                "....egGaaGge....",
                ".....egegge.....",
                "......eeee......",
            ],
            [
                "................",
                "................",
                "...e........e...",
                ".egG...ee...Gge.",
                "eGHge.gHHg.egHGe",
                ".egGgeGggGeGgge.",
                "..eaAAaGGaAAae..",
                ".eaAnNAeeANnAae.",
                ".eaANNnGGnNNAae.",
                ".eaANnAeeAnNAae.",
                ".eaAAAaggAAAAae.",
                "..eaaaaeeaaaae..",
                "...egGaaaaGge...",
                "....egGggGge....",
                ".....eg..ge.....",
                "......e..e......",
            ],
        ],
        # 4. LAZULI_BLOOM (Catalyzed Tier 5): Crystalline vines blossoming into royal-blue lapis clusters
        "lazuli_bloom": [
            [
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                ".....c....c.....",
                "....cu....uc....",
                ".....u.cc.u.....",
                ".....uUccUu.....",
                "......e..e......",
                "......e..e......",
            ],
            [
                "................",
                "................",
                "................",
                "................",
                "................",
                "....c......c....",
                "...cC......Cc...",
                "....uU....Uu....",
                "...cU..cc..Uc...",
                "....uUCccCUu....",
                ".....eU..Ue.....",
                "....cU....Uc....",
                ".....eU..Ue.....",
                "......e..e......",
                ".....e....e.....",
                ".....e....e.....",
            ],
            [
                "................",
                "................",
                "...uU......Uu...",
                "..uZzU....UzZu..",
                "...uU.c..c.Uu...",
                "...cC.C..C.Cc...",
                "..cCQcU..UcQCc..",
                "...cCuU..UuCc...",
                "....uUCccCUu....",
                "...cCeU..UeCc...",
                "....eU.cc.Ue....",
                "...cC.e..e.Cc...",
                "....eU....Ue....",
                ".....e....e.....",
                ".....e....e.....",
                ".....e....e.....",
            ],
            [
                "................",
                "..uZzU....uZzU..",
                ".uZZzUu..uUzZZu.",
                ".uZzUzU..UzUzZu.",
                "..uUUzU..UzUUu..",
                "..cCQcU..UcQCc..",
                ".cCQQcU..UcQQCc.",
                "..cCcuU..UucCc..",
                "...uUzCQQCzUu...",
                "..cCceU..UecCc..",
                ".cCQceU..UecQCc.",
                "..cC..e..e..Cc..",
                ".....eU..Ue.....",
                "....eU....Ue....",
                ".....e....e.....",
                ".....e....e.....",
            ],
        ],
        # 5. IRID_MOSS (Catalyzed Tier 8): Velvety catalytic moss bed sprouting pearlescent silver-white iridium shards
        "irid_moss": [
            [
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "....eg....ge....",
                "..egGge..egGge..",
                ".egGGgGeegGGgGe.",
                ".eeeeeeeeeeeeee.",
            ],
            [
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                ".....i....i.....",
                "...egG....Gge...",
                "..eGcgG..GgCGe..",
                ".eGgGiGgeGgGiGe.",
                ".egGGgGggGgGGge.",
                "egGgGGgGGgGGgGge",
                "eeeeeeeeeeeeeeee",
            ],
            [
                "................",
                "................",
                "................",
                "................",
                "................",
                "....iI....Ii....",
                "....Is....sI....",
                ".....i....i.....",
                "...g.i.iI.i.g...",
                "..eGgGiIsGiGge..",
                ".eGcgGgGGgGgCGe.",
                ".egGGiGggGiGGge.",
                "eGgGGgGggGgGGgGe",
                "egGgGGgGGgGGgGge",
                "egGGgGGgGGgGGgGe",
                "eeeeeeeeeeeeeeee",
            ],
            [
                "................",
                "................",
                "...iSs....sSi...",
                "...ISSi..iSSI...",
                "....iI.iI.Ii....",
                ".....i.SS.i..iI.",
                "..iI.i.iI.i..sS.",
                "..sS.i..i.i..iI.",
                "...i.c..i.c..i..",
                "..eGgGiGGiGgGge.",
                ".eGcgGsIiSgGgCGe",
                "eGgGGgGggGgGGgGe",
                "egGGiGGggGGiGGge",
                "egGgGGgGGgGGgGge",
                "egGGgGGgGGgGGgGe",
                "eeeeeeeeeeeeeeee",
            ],
        ],
        # 6. RADBLOOM (Catalyzed Radioactive Tier 7): Dark mutated stalk blooming into glowing uranium-green trefoil pods
        "radbloom": [
            [
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                ".......X........",
                "......1x1.......",
                "....12.1.21.....",
                ".....12121......",
                "......121.......",
                "......111.......",
            ],
            [
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                ".......X........",
                "......xqx.......",
                "....12.x.21.....",
                "...12e111e21....",
                "....121x121.....",
                ".....12121......",
                "....1e121e1.....",
                "......121.......",
                "......121.......",
                "......111.......",
            ],
            [
                "................",
                "................",
                "................",
                "......xXx.......",
                ".....xXqxX......",
                "......xXx.......",
                "...xX.121.Xx....",
                "..xXqx121xqXx...",
                "...1211x1121....",
                "..12e21212e21...",
                "...121121121....",
                "....121x121.....",
                "...1e12121e1....",
                ".....12121......",
                "......121.......",
                "......111.......",
            ],
            [
                "................",
                "......xXx.......",
                ".....xXqJx......",
                ".....xJqJx......",
                "......xXx.......",
                "..xXx.121.xXx...",
                ".xXqJx1x1xJqXx..",
                ".xJqJx121xJqJx..",
                "..xXx11211xXx...",
                "..12e21x12e21...",
                ".12e2112112e21..",
                "...121121121....",
                "....121x121.....",
                "...1e12121e1....",
                ".....12121......",
                "......111.......",
            ],
        ],
        # 7. GLOWBLOOM (Tier 4 Glowing): Arching green stems with hanging golden glowstone lanterns
        "glowbloom": [
            [
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                ".......f........",
                "......eGe.......",
                ".....eGgGe......",
                "......egge......",
                ".......ge.......",
                ".......ge.......",
            ],
            [
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "....eGe..eGe....",
                "...efFe..eFfe...",
                "....eg....ge....",
                "...eGg....gGe...",
                "....egG..Gge....",
                ".....egGGge.....",
                "......egge......",
                "......egge......",
                ".......ge.......",
                ".......ge.......",
            ],
            [
                "................",
                "................",
                ".....eGGGGge....",
                "...eGge..egGe...",
                "..eGe......eGe..",
                "..yFf......fFy..",
                "..YFF......FFY..",
                "...yY..ee..Yy...",
                "......eGGe......",
                "....eGgGGgGe....",
                ".....egGGge.....",
                "......egge......",
                "......egge......",
                "......egge......",
                ".......ge.......",
                ".......ge.......",
            ],
            [
                "................",
                "....eGGGGGGge...",
                "..eGge.ee.egGe..",
                ".eGe..eGGe..eGe.",
                ".yFf..yFFy..fFy.",
                ".YFFy.YFFY.yFFY.",
                ".yFFy..yY..yFFy.",
                "..yY...ee...Yy..",
                "......eGGe......",
                "...eGgGggGgGe...",
                "....egGggGge....",
                ".....egGGge.....",
                "......egge......",
                "......egge......",
                ".......ge.......",
                ".......ge.......",
            ],
        ],
        # 8. QUARTZREED (Catalyzed Tier 5): Crisp white-ivory crystalline quartz spires
        "quartzreed": [
            [
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                ".......S........",
                "....s..s...S....",
                "....I..I...s....",
                "....i..I...I....",
                "....i..i...i....",
                "....3..3...3....",
            ],
            [
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                ".......S........",
                "...S...s....S...",
                "...s...I....s...",
                "...I...I....I...",
                "...I..sI....I...",
                "...i..Ii....i...",
                "...i..ii....i...",
                "...3..33....3...",
                "...3..33....3...",
                "...2..22....2...",
            ],
            [
                "................",
                "................",
                ".......S........",
                "......sS........",
                "...S..sI...S....",
                "...s..II...s....",
                "..sI..Ii..sI....",
                "..II..iI..II..S.",
                "..iI..ii..iI..s.",
                "..ii..3i..ii..I.",
                "..3i..33..3i..i.",
                "..33..33..33..i.",
                "..33..23..33..3.",
                "..23..22..23..3.",
                "..22..22..22..2.",
                "..22..22..22..2.",
            ],
            [
                ".......S........",
                "......sS........",
                "...S..sS...S....",
                "..sS..sI..sS....",
                "..sI..II..sI..S.",
                "..II..Ii..II.sS.",
                ".sIi..iI..Ii.sI.",
                ".IIi..ii..iI.II.",
                ".iIi..Qi..ii.iI.",
                ".ii3..3i..3i.ii.",
                ".3i3..33..33.3i.",
                ".333..23..33.33.",
                ".232..22..23.23.",
                ".222..22..22.22.",
                ".222..22..22.22.",
                ".121..11..12.12.",
            ],
        ],
        # 9. LITHIVINE (Catalyzed Tier 6): Metallic silver climbing vine with glowing lithium rose-silver nodes
        "lithivine": [
            [
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                ".....d....d.....",
                "....dD....Dd....",
                ".....5.ii.5.....",
                ".....5IiiI5.....",
                "......4..4......",
                "......3..3......",
            ],
            [
                "................",
                "................",
                "................",
                "................",
                "................",
                ".....d....d.....",
                "....dK....Kd....",
                ".....I....I.....",
                "...dD5.ii.5Dd...",
                "....5IiiiiI5....",
                ".....5i..i5.....",
                "....d5....5d....",
                ".....4i..i4.....",
                "......4..4......",
                ".....3....3.....",
                ".....3....3.....",
            ],
            [
                "................",
                "................",
                "...dD......Dd...",
                "..dKDd....dDKd..",
                "...dD.i..i.Dd...",
                "....I.I..I.I....",
                "..dDKi5..5iKDd..",
                "...dDi5..5idD...",
                "....5IiiiiI5....",
                "..dDi5....5idD..",
                "...dD5.ii.5Dd...",
                "....iI.44.Ii....",
                ".....4I..I4.....",
                ".....3....3.....",
                ".....3....3.....",
                ".....3....3.....",
            ],
            [
                "................",
                "..dKDd....dDKd..",
                ".dKKDdI..IdDKKd.",
                ".dKDdDI..IDdDKd.",
                "..dDDI5..5IDDd..",
                "..iIsi5..5isIi..",
                ".dDKsi5..5isKDd.",
                "..dDdi5..5idDd..",
                "...5IiKSSKiI5...",
                "..dDdi5..5idDd..",
                ".dDKdi5..5idKDd.",
                "..dD..4..4..Dd..",
                ".....4I..I4.....",
                "....3I....I3....",
                ".....3....3.....",
                ".....3....3.....",
            ],
        ],
        # 10. SULFURBUD (Tier 4 Chemical): Compact olive-green shrub bursting with vibrant yellow sulfur buds
        "sulfurbud": [
            [
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "......j..j......",
                "....egE..Ege....",
                "...eGggeegGge...",
                "....egGggGge....",
                ".....eeeeee.....",
            ],
            [
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                ".....jE..Ej.....",
                "....jEj..jEj....",
                "...egGgeegGge...",
                "..eGggGggGggGe..",
                "...egGgjGgGge...",
                "....egGggGge....",
                ".....egggge.....",
                "......eeee......",
                "......eeee......",
            ],
            [
                "................",
                "................",
                "................",
                "................",
                "....jE....Ej....",
                "...jEFj..jFEj...",
                "...jEj.jE.jEj...",
                "..egGgjEFjgGge..",
                ".eGggGgEEgGggGe.",
                "..egGggGGggGge..",
                "...eGgGggGgGe...",
                "....egGggGge....",
                ".....egggge.....",
                ".....eg..ge.....",
                "......e..e......",
                "......e..e......",
            ],
            [
                "................",
                "................",
                "...jEj....jEj...",
                "..jEFEj..jEFEj..",
                "..jEfj.jE.jfEj..",
                "...jj.jEFEj.jj..",
                ".jEj.egEFge.jEj.",
                "jEFEjeGggGejEFEj",
                ".jEjegGggGgejEj.",
                "..egGgGggGgGge..",
                "...eGgGggGgGe...",
                "....egGggGge....",
                ".....egggge.....",
                ".....eg..ge.....",
                "......e..e......",
                "......e..e......",
            ],
        ],
    }

    for crop_id, stages in crops.items():
        for idx, grid in enumerate(stages, start=1):
            save_png(TEX_CROPS / f"{crop_id}_{idx}.png", from_grid(grid))


def generate_radioactive_soil_assets() -> None:
    """Rich dark subsoil laced with glowing uranium-green mineral veins (16x16)."""
    soil = from_grid([
        "kwWWwkwkwWWwkwWw",
        "wWWwkwxXwWWwkwWW",
        "WWkwkxqJxkWWwkwW",
        "wWWwkwxXxkwWWwkW",
        "kwWWwkwkxWWwkwWW",
        "wWkwWWwkwWWkwxWw",
        "WWwkwWWwkwWkxqXw",
        "wWWwkxXwWWwkwxWW",
        "kwWkxqJxkWWwkwWw",
        "wWWwkwxXxkwWWwWW",
        "WWkwWWwkxWWwkwWw",
        "wWWwkwWWwkwWWwkW",
        "kwWWwkwkxXwWWwWW",
        "wWkwWWkxqJxkWWwW",
        "WWwkwWWwkxXwkwWW",
        "kwWWwkwWWwkwWWwk",
    ])
    save_png(TEX_BLOCKS / "radioactive_soil.png", soil)
    save_json(BLOCKSTATES / "radioactive_soil.json", {
        "variants": {"normal": {"model": "cropmanager:radioactive_soil"}}
    })
    save_json(MODELS_BLOCK / "radioactive_soil.json", {
        "parent": "block/cube_all",
        "textures": {"all": "cropmanager:blocks/radioactive_soil"},
    })
    save_json(MODELS_ITEM / "radioactive_soil.json", {
        "parent": "cropmanager:block/radioactive_soil"
    })


def generate_sprinkler_assets() -> None:
    """Generate sleek IC2-style 3D models, blockstates, and 16x16 textures for Sprinklers Mk1, Mk2, Mk3."""
    casings = load_ic2_casings()
    # Sleek bright steel base texture derived directly from IC2 casing
    base_tex = casings["side"].copy()
    save_png(TEX_BLOCKS / "sprinkler_base.png", base_tex)

    # Tier accent textures (16x16 metal collars/nozzles: Mk1 Copper, Mk2 Gold, Mk3 Pressurized Cyan-Diamond)
    accents = {
        "sprinkler_accent_mk1": (PAL["r"], PAL["R"], PAL["o"], PAL["O"]),
        "sprinkler_accent_mk2": (PAL["y"], PAL["Y"], PAL["f"], PAL["F"]),
        "sprinkler_accent_mk3": (PAL["c"], PAL["b"], PAL["B"], PAL["Q"]),
    }
    for name, (c0, c1, c2, c3) in accents.items():
        im = Image.new("RGBA", (16, 16), c1)
        d = ImageDraw.Draw(im)
        d.rectangle((0, 0, 15, 15), fill=c0)
        d.rectangle((1, 1, 14, 14), fill=c1)
        d.line((1, 1, 14, 1), fill=c3)
        d.line((1, 1, 1, 14), fill=c2)
        d.line((14, 1, 14, 14), fill=c0)
        d.line((1, 14, 14, 14), fill=c0)
        d.rectangle((3, 3, 12, 12), fill=c2)
        d.rectangle((5, 5, 10, 10), fill=c1)
        for rx, ry in ((2, 2), (13, 2), (2, 13), (13, 13)):
            d.point((rx, ry), fill=c3)
        save_png(TEX_BLOCKS / f"{name}.png", im)

    def faces(tex):
        return {s: {"texture": f"#{tex}"} for s in ("up", "down", "north", "south", "east", "west")}

    for tier in (1, 2, 3):
        name = f"sprinkler_mk{tier}"
        accent_tex = f"cropmanager:blocks/sprinkler_accent_mk{tier}"
        for active in (False, True):
            elements = [
                # Ground flange plate (y=0..2)
                {"from": [3, 0, 3], "to": [13, 2, 13], "faces": faces("base")},
                # Tier accent collar ring (y=2..4)
                {"from": [4.5, 2, 4.5], "to": [11.5, 4, 11.5], "faces": faces("accent")},
                # Slender vertical riser stem (y=4..9)
                {"from": [6.5, 4, 6.5], "to": [9.5, 9, 9.5], "faces": faces("base")},
                # Top nozzle cap (y=9..11)
                {"from": [6, 9, 6], "to": [10, 11, 10], "faces": faces("accent")},
            ]
            # Rotating spray arms: Mk1 has 2 arms (E-W), Mk2/Mk3 have 4-way cross arms!
            # Active state rotates arms visually by 45 degrees!
            rot = {"origin": [8, 9.5, 8], "axis": "y", "angle": 45 if active else 0}
            arm1 = {"from": [2, 9.2, 7.2], "to": [14, 10.4, 8.8], "faces": faces("accent")}
            if active:
                arm1["rotation"] = rot
            elements.append(arm1)
            if tier >= 2:
                arm2 = {"from": [7.2, 9.2, 2], "to": [8.8, 10.4, 14], "faces": faces("accent")}
                if active:
                    arm2["rotation"] = rot
                elements.append(arm2)
            if tier == 3:
                # Extra high-pressure top ring on Mk3
                elements.append({"from": [5.5, 5.5, 5.5], "to": [10.5, 7, 10.5], "faces": faces("accent")})

            suffix = "_active" if active else ""
            save_json(MODELS_BLOCK / f"{name}{suffix}.json", {
                "parent": "block/block",
                "textures": {
                    "base": "cropmanager:blocks/sprinkler_base",
                    "accent": accent_tex,
                    "particle": "cropmanager:blocks/sprinkler_base",
                },
                "elements": elements,
                "display": {
                    "gui": {"rotation": [30, 225, 0], "translation": [0, 1, 0], "scale": [0.75, 0.75, 0.75]},
                    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.35, 0.35, 0.35]},
                },
            })
        save_json(BLOCKSTATES / f"{name}.json", {
            "variants": {
                "active=false": {"model": f"cropmanager:{name}"},
                "active=true": {"model": f"cropmanager:{name}_active"},
            }
        })
        save_json(MODELS_ITEM / f"{name}.json", {"parent": f"cropmanager:block/{name}"})


def main() -> None:
    generate_seed_analyzer_assets()
    generate_manual_composter_assets()
    generate_radioactive_soil_assets()
    generate_sprinkler_assets()
    generate_item_assets()
    generate_crop_sprites()
    print("Regenerated all IC2-grade expansion textures, 3D models, and blockstates.")


if __name__ == "__main__":
    main()
