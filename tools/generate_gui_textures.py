#!/usr/bin/env python3
"""Generate authentic IC2 Experimental GUI textures (light & dark themes) and upgrade icons for Crop Manager."""

import io
import json
import struct
import sys
import zipfile
from pathlib import Path
from PIL import Image, ImageDraw

PROJECT_ROOT = Path(__file__).resolve().parents[1]
IC2_JAR = PROJECT_ROOT / "libs" / "industrialcraft-2-2.8.222-ex112.jar"
DESIGN_KIT_WIDGETS = PROJECT_ROOT / "design_kit" / "widgets"
GUI_LIGHT_DIR = PROJECT_ROOT / "src" / "main" / "resources" / "assets" / "cropmanager" / "textures" / "gui"
ITEMS_DIR = PROJECT_ROOT / "src" / "main" / "resources" / "assets" / "cropmanager" / "textures" / "items"
DEFAULT_DARK_PACK = PROJECT_ROOT / "dist" / "CropManager-GUI-Dark-0.5.0-alpha10"

LIGHT_PALETTE = {
    "body": (0xC6, 0xC6, 0xC6, 255),
    "panel_dark": (0xB4, 0xB4, 0xB4, 255),
    "panel_mid": (0xBC, 0xBC, 0xBC, 255),
    "subpanel": (0xB0, 0xB5, 0xB8, 255),
    "rivet": (0x37, 0x37, 0x37, 255),
    "ghost": (0x6E, 0x6E, 0x6E, 255),
    "accent_water": (0x2E, 0x86, 0xC1, 255),
    "accent_weed": (0x58, 0xD6, 0x8D, 255),
    "edge": (0xFF, 0xFF, 0xFF, 255),
    "shadow": (0x55, 0x55, 0x55, 255),
    "shadow_deep": (0x37, 0x37, 0x37, 255),
    "seam": (0x8B, 0x8B, 0x8B, 255),
    "engrave_dark": (0x55, 0x55, 0x55, 255),
    "engrave_fill": (0x8B, 0x8B, 0x8B, 255),
    "engrave_light": (0xFF, 0xFF, 0xFF, 255),
    "slot_bg": (0x8B, 0x8B, 0x8B, 255),
    "slot_icon": (0x55, 0x55, 0x55, 255),
    "slot_icon_hi": (0xA8, 0xA8, 0xA8, 255),
    "gauge_well": (0x26, 0x26, 0x26, 255),
    "gauge_border": (0x37, 0x37, 0x37, 255),
    "gauge_tick": (0x55, 0x55, 0x55, 255),
    "gauge_tick_red": (0x8E, 0x28, 0x20, 255),
}

DARK_PALETTE = {
    "body": (0x1C, 0x1C, 0x1D, 255),
    "panel_dark": (0x15, 0x15, 0x16, 255),
    "panel_mid": (0x18, 0x18, 0x19, 255),
    "subpanel": (0x18, 0x18, 0x19, 255),
    "rivet": (0x08, 0x08, 0x09, 255),
    "ghost": (0x3A, 0x3A, 0x3D, 255),
    "accent_water": (0x2E, 0x86, 0xC1, 255),
    "accent_weed": (0x58, 0xD6, 0x8D, 255),
    "edge": (0x4E, 0x4E, 0x52, 255),
    "shadow": (0x0D, 0x0D, 0x0E, 255),
    "shadow_deep": (0x05, 0x05, 0x06, 255),
    "seam": (0x2E, 0x2E, 0x31, 255),
    "engrave_dark": (0x0D, 0x0D, 0x0E, 255),
    "engrave_fill": (0x14, 0x14, 0x15, 255),
    "engrave_light": (0x4E, 0x4E, 0x52, 255),
    "slot_bg": (0x12, 0x12, 0x13, 255),
    "slot_icon": (0x32, 0x32, 0x35, 255),
    "slot_icon_hi": (0x0B, 0x0B, 0x0C, 255),
    "gauge_well": (0x05, 0x05, 0x06, 255),
    "gauge_border": (0x08, 0x08, 0x09, 255),
    "gauge_tick": (0x28, 0x28, 0x2B, 255),
    "gauge_tick_red": (0x7A, 0x26, 0x20, 255),
}


def remap_sprite_to_dark(img: Image.Image) -> Image.Image:
    """Remap an authentic IC2 light-theme sprite to the dark theme palette."""
    out = img.copy().convert("RGBA")
    px = out.load()
    w, h = out.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            # Check if grayscale / neutral IC2 tone
            if max(r, g, b) - min(r, g, b) <= 6:
                v = r
                if v >= 230:
                    px[x, y] = (0x62, 0x6B, 0x74, a)  # #ffffff -> #626b74
                elif v >= 180:
                    px[x, y] = (0x25, 0x2A, 0x30, a)  # #c6c6c6 -> #252a30
                elif v >= 135:
                    px[x, y] = (0x11, 0x17, 0x1D, a)  # #8b8b8b -> #11171d
                elif v >= 110:
                    px[x, y] = (0x18, 0x1F, 0x26, a)  # #7e7d7d -> mid recess
                elif v >= 70:
                    px[x, y] = (0x08, 0x0B, 0x0F, a)  # #555555 -> #080b0f
                elif v >= 30:
                    px[x, y] = (0x05, 0x08, 0x0C, a)  # #373737 -> #05080c
                else:
                    px[x, y] = (0x04, 0x06, 0x09, a)  # #000000 border
            else:
                # Preserve colored pixels (green scale, red bolt, etc.)
                px[x, y] = (r, g, b, a)
    return out


def load_clean_widget(rel_path: str, fallback: Image.Image) -> Image.Image:
    path = DESIGN_KIT_WIDGETS / rel_path
    if path.is_file():
        return Image.open(path).convert("RGBA")
    return fallback


def load_ic2_sprites(dark: bool = False) -> dict[str, Image.Image]:
    """Load clean verified IC2 widgets from design_kit/widgets/ (falling back to the IC2 JAR)."""
    with zipfile.ZipFile(IC2_JAR, "r") as zf:
        common = Image.open(io.BytesIO(zf.read("assets/ic2/textures/gui/common.png"))).convert("RGBA")
        matron = Image.open(io.BytesIO(zf.read("assets/ic2/textures/gui/guicropmatron.png"))).convert("RGBA")

    clean_slot = load_clean_widget("slots/common_slot_020_u103_v7_18x18.png", common.crop((103, 7, 121, 25)))
    assert clean_slot.size == (18, 18), f"Unexpected slot size: {clean_slot.size}"
    assert clean_slot.getpixel((0, 0))[:3] == (0x37, 0x37, 0x37), "Slot top-left border must be #373737"
    assert clean_slot.getpixel((17, 17))[:3] == (0xFF, 0xFF, 0xFF), "Slot bottom-right border must be #FFFFFF"
    assert clean_slot.getpixel((9, 9))[:3] == (0x8B, 0x8B, 0x8B), "Slot center must be clean #8B8B8B (no 'F' filter)"

    sprites = {
        "panel_9slice": load_clean_widget("symbols_and_icons/common_symbols_and_icon_009_u16_v16_64x64.png", common.crop((16, 16, 80, 80))),
        "slot": clean_slot,                                  # 18x18 clean IC2 slot
        "bolt_off": load_clean_widget("symbols_and_icons/common_symbols_and_icon_018_u100_v65_7x13.png", common.crop((100, 65, 107, 78))),
        "bolt_on": load_clean_widget("symbols_and_icons/common_symbols_and_icon_024_u116_v65_7x13.png", common.crop((116, 65, 123, 78))),
        "energy_box": load_clean_widget("bars_and_tanks/common_bars_and_tank_027_u128_v7_32x17.png", common.crop((128, 7, 160, 24))),
        "energy_fill": load_clean_widget("symbols_and_icons/common_symbols_and_icon_028_u132_v43_24x9.png", common.crop((132, 43, 156, 52))),
        "recycle_off": common.crop((133, 64, 151, 79)),      # 18x15 recycling/composting icon
        "arrow_right": load_clean_widget("symbols_and_icons/common_symbols_and_icon_033_u165_v0_22x15.png", common.crop((165, 0, 187, 15))),
        "crush_off": common.crop((165, 35, 186, 46)),        # 21x11 grinder/crusher icon
        "drop_off": common.crop((165, 96, 187, 111)),        # 22x15 fluid droplet/extraction icon
        "tank_light": load_clean_widget("bars_and_tanks/common_bars_and_tank_007_u6_v100_20x55.png", common.crop((7, 100, 27, 155))),
        "tank_dark": load_clean_widget("bars_and_tanks/common_bars_and_tank_012_u70_v100_20x55.png", common.crop((70, 100, 90, 155))),
        "tank_scale": common.crop((46, 109, 54, 146)),       # 8x37 green tank scale
        "slot_fert": matron.crop((10, 56, 28, 74)),           # 18x18 Crop-Matron fertilizer slot
        "slot_cell": matron.crop((48, 26, 66, 44)),          # 18x18 Crop-Matron fluid capsule slot
    }

    if dark:
        sprites = {k: remap_sprite_to_dark(v) for k, v in sprites.items()}
    return sprites


def fill_rect(draw: ImageDraw.ImageDraw, x: int, y: int, w: int, h: int, color: tuple) -> None:
    if w <= 0 or h <= 0:
        return
    draw.rectangle([x, y, x + w - 1, y + h - 1], fill=color)


def draw_bevel_box(
    draw: ImageDraw.ImageDraw,
    x: int,
    y: int,
    w: int,
    h: int,
    top_left: tuple,
    bottom_right: tuple,
    corner: tuple,
    fill: tuple | None = None,
) -> None:
    """Draw a 1px bevelled rectangle over [x..x+w-1, y..y+h-1]."""
    if fill is not None and w > 2 and h > 2:
        fill_rect(draw, x + 1, y + 1, w - 2, h - 2, fill)
    fill_rect(draw, x, y, w - 1, 1, top_left)
    fill_rect(draw, x, y, 1, h - 1, top_left)
    fill_rect(draw, x + 1, y + h - 1, w - 1, 1, bottom_right)
    fill_rect(draw, x + w - 1, y + 1, 1, h - 1, bottom_right)
    draw.point((x + w - 1, y), fill=corner)
    draw.point((x, y + h - 1), fill=corner)


def draw_nine_slice_panel(
    im: Image.Image,
    panel_sprite: Image.Image,
    x0: int,
    y0: int,
    w: int,
    h: int,
    body_color: tuple,
) -> None:
    """Stamp the 64x64 IC2 nine-slice panel onto (x0, y0, w, h)."""
    draw = ImageDraw.Draw(im)
    c = 8  # 8x8 corners
    fill_rect(draw, x0 + 4, y0 + 4, w - 8, h - 8, body_color)

    # Top and bottom edges
    top_strip = panel_sprite.crop((c, 0, 64 - c, c))
    bot_strip = panel_sprite.crop((c, 64 - c, 64 - c, 64))
    for tx in range(x0 + c, x0 + w - c, top_strip.width):
        seg_w = min(top_strip.width, (x0 + w - c) - tx)
        im.alpha_composite(top_strip.crop((0, 0, seg_w, c)), (tx, y0))
        im.alpha_composite(bot_strip.crop((0, 0, seg_w, c)), (tx, y0 + h - c))

    # Left and right edges
    left_strip = panel_sprite.crop((0, c, c, 64 - c))
    right_strip = panel_sprite.crop((64 - c, c, 64, 64 - c))
    for ty in range(y0 + c, y0 + h - c, left_strip.height):
        seg_h = min(left_strip.height, (y0 + h - c) - ty)
        im.alpha_composite(left_strip.crop((0, 0, c, seg_h)), (x0, ty))
        im.alpha_composite(right_strip.crop((0, 0, c, seg_h)), (x0 + w - c, ty))

    # Four rounded corners
    im.alpha_composite(panel_sprite.crop((0, 0, c, c)), (x0, y0))
    im.alpha_composite(panel_sprite.crop((64 - c, 0, 64, c)), (x0 + w - c, y0))
    im.alpha_composite(panel_sprite.crop((0, 64 - c, c, 64)), (x0, y0 + h - c))
    im.alpha_composite(panel_sprite.crop((64 - c, 64 - c, 64, 64)), (x0 + w - c, y0 + h - c))


def draw_outer_panel(
    im: Image.Image,
    sprites: dict[str, Image.Image],
    w: int,
    h: int,
    sidebar: bool,
    sx: int,
    p: dict,
) -> None:
    """
    Stamp the authentic IC2 nine-slice window for the main panel and sidebar.
    When sidebar=True, main panel width is sx+2 (176 or 194) and the vertical divider sits at x=sx..sx+2.
    """
    if not sidebar:
        draw_nine_slice_panel(im, sprites["panel_9slice"], 0, 0, w, h, p["body"])
    else:
        compact_w = sx + 2
        draw_nine_slice_panel(im, sprites["panel_9slice"], 0, 0, compact_w, h, p["body"])
        draw_nine_slice_panel(im, sprites["panel_9slice"], sx + 1, 0, w - (sx + 1), h, p["body"])


def draw_custom_slot_icon(draw: ImageDraw.ImageDraw, x: int, y: int, kind: str, p: dict) -> None:
    """Draw a subtle 1px pixel-art icon inside the 16x16 slot area (x..x+15, y..y+15) for non-Matron slots."""
    c = p["slot_icon"]
    hi = p["slot_icon_hi"]
    pts: list[tuple[int, int]] = []

    if kind == "antenna":
        pts = [
            (7, 5), (8, 5),
            (7, 7), (8, 7), (7, 8), (8, 8), (7, 9), (8, 9), (7, 10), (8, 10), (6, 11), (7, 11), (8, 11), (9, 11),
            (5, 5), (4, 6), (4, 7), (5, 8),
            (10, 5), (11, 6), (11, 7), (10, 8),
            (3, 4), (2, 5), (2, 6), (2, 7), (2, 8), (3, 9),
            (12, 4), (13, 5), (13, 6), (13, 7), (13, 8), (12, 9),
        ]
    elif kind == "bolt":
        pts = [
            (8, 3), (9, 3), (7, 4), (8, 4), (6, 5), (7, 5),
            (5, 6), (6, 6), (7, 6), (8, 6), (9, 6), (10, 6),
            (7, 7), (8, 7), (9, 7), (7, 8), (8, 8), (6, 9), (7, 9), (6, 10), (5, 11),
        ]
    elif kind == "transfer":
        pts = [
            (3, 5), (4, 5), (5, 5), (6, 5), (7, 5),
            (3, 6), (7, 6), (3, 7), (7, 7), (3, 8), (7, 8), (3, 9), (7, 9),
            (3, 10), (4, 10), (5, 10), (6, 10), (7, 10),
            (8, 7), (9, 7), (10, 7), (11, 7), (12, 7),
            (8, 8), (9, 8), (10, 8), (11, 8), (12, 8),
            (10, 5), (11, 6), (11, 9), (10, 10),
        ]
    elif kind == "upgrade":
        pts = [
            (7, 4), (8, 4), (6, 5), (7, 5), (8, 5), (9, 5),
            (5, 6), (6, 6), (7, 6), (8, 6), (9, 6), (10, 6),
            (7, 7), (8, 7), (7, 8), (8, 8), (7, 9), (8, 9),
            (5, 11), (6, 11), (7, 11), (8, 11), (9, 11), (10, 11),
        ]
    elif kind == "blade":
        pts = [
            (7, 3), (8, 3), (4, 4), (11, 4),
            (5, 5), (6, 5), (7, 5), (8, 5), (9, 5), (10, 5),
            (5, 6), (6, 6), (9, 6), (10, 6),
            (3, 7), (4, 7), (5, 7), (10, 7), (11, 7), (12, 7),
            (3, 8), (4, 8), (5, 8), (10, 8), (11, 8), (12, 8),
            (5, 9), (6, 9), (9, 9), (10, 9),
            (5, 10), (6, 10), (7, 10), (8, 10), (9, 10), (10, 10),
            (4, 11), (11, 11), (7, 12), (8, 12),
        ]
    elif kind == "powder":
        pts = [
            (7, 5), (8, 5), (6, 6), (7, 6), (8, 6), (9, 6),
            (5, 7), (6, 7), (8, 7), (9, 7), (10, 7),
            (4, 8), (5, 8), (6, 8), (7, 8), (8, 8), (9, 8), (10, 8), (11, 8),
            (3, 9), (4, 9), (5, 9), (6, 9), (7, 9), (8, 9), (9, 9), (10, 9), (11, 9), (12, 9),
            (3, 10), (4, 10), (5, 10), (6, 10), (7, 10), (8, 10), (9, 10), (10, 10), (11, 9), (12, 10),
        ]
    elif kind == "redstone":
        pts = [
            (7, 4), (8, 4), (6, 5), (7, 5), (8, 5), (9, 5),
            (5, 6), (6, 6), (8, 6), (9, 6), (10, 6),
            (4, 7), (5, 7), (6, 7), (7, 7), (8, 7), (9, 7), (10, 7), (11, 7),
            (5, 8), (6, 8), (7, 8), (8, 8), (9, 8), (10, 8),
            (6, 9), (7, 9), (8, 9), (9, 9), (7, 10), (8, 10),
        ]

    for px, py in pts:
        if px + 1 < 15 and py + 1 < 15:
            draw.point((x + px + 1, y + py + 1), fill=hi)
    for px, py in pts:
        draw.point((x + px, y + py), fill=c)


def draw_slot(
    im: Image.Image,
    draw: ImageDraw.ImageDraw,
    sprites: dict[str, Image.Image],
    x: int,
    y: int,
    p: dict,
    icon: str | None = None,
) -> None:
    """
    Stamp the authentic 18x18 IC2 slot at (x-1, y-1)..(x+16, y+16) so the 16x16 item area is (x, y)..(x+15, y+15).
    Uses Crop-Matron fertilizer and fluid capsule slots directly when requested.
    """
    if icon == "fert":
        im.alpha_composite(sprites["slot_fert"], (x - 1, y - 1))
    elif icon == "cell":
        im.alpha_composite(sprites["slot_cell"], (x - 1, y - 1))
    else:
        im.alpha_composite(sprites["slot"], (x - 1, y - 1))
        if icon:
            draw_custom_slot_icon(draw, x, y, icon, p)


def draw_gauge(
    im: Image.Image,
    draw: ImageDraw.ImageDraw,
    sprites: dict[str, Image.Image],
    x: int,
    y: int,
    w: int,
    h: int,
    p: dict,
    kind: str = "fluid",
) -> None:
    """
    Draw a recessed IC2 gauge:
    - For fluid tanks, uses the authentic IC2 common.png fluid tank sprite (tank_dark) scaled via 9-slice
      to preserve exact 2px bevel borders and green graduation lines.
    - For EU / progress bars, draws an inset cavity with IC2 striped ticks.
    """
    if h > w and kind == "fluid":
        tank = sprites["tank_dark"]  # 20x55
        tw, th = tank.size
        # 9-slice with 3px border to preserve exact IC2 bevel and inner dark glass
        b = 3
        core_w, core_h = max(1, w - 2 * b), max(1, h - 2 * b)
        # Corners
        im.alpha_composite(tank.crop((0, 0, b, b)), (x, y))
        im.alpha_composite(tank.crop((tw - b, 0, tw, b)), (x + w - b, y))
        im.alpha_composite(tank.crop((0, th - b, b, th)), (x, y + h - b))
        im.alpha_composite(tank.crop((tw - b, th - b, tw, th)), (x + w - b, y + h - b))
        # Top & Bottom edges
        top_e = tank.crop((b, 0, tw - b, b)).resize((core_w, b), Image.NEAREST)
        bot_e = tank.crop((b, th - b, tw - b, th)).resize((core_w, b), Image.NEAREST)
        im.alpha_composite(top_e, (x + b, y))
        im.alpha_composite(bot_e, (x + b, y + h - b))
        # Left & Right edges
        left_e = tank.crop((0, b, b, th - b)).resize((b, core_h), Image.NEAREST)
        right_e = tank.crop((tw - b, b, tw, th - b)).resize((b, core_h), Image.NEAREST)
        im.alpha_composite(left_e, (x, y + b))
        im.alpha_composite(right_e, (x + w - b, y + b))
        # Center well (dark glass + IC2 green scale)
        center = tank.crop((b, b, tw - b, th - b)).resize((core_w, core_h), Image.NEAREST)
        im.alpha_composite(center, (x + b, y + b))
        return

    draw_bevel_box(
        draw,
        x - 1,
        y - 1,
        w + 2,
        h + 2,
        top_left=p["shadow_deep"],
        bottom_right=p["edge"],
        corner=p["seam"],
        fill=None,
    )
    fill_rect(draw, x, y, w, h, p["gauge_border"])
    if w > 2 and h > 2:
        fill_rect(draw, x + 1, y + 1, w - 2, h - 2, p["gauge_well"])

    if h > w:
        # Vertical IC2 energy bar: striped horizontal segments with subtle red/grey ticks
        for idx, gy in enumerate(range(y + 4, y + h - 2, 4)):
            fill_rect(draw, x + 2, gy, w - 4, 1, p["gauge_tick"])
            if idx % 2 == 0:
                fill_rect(draw, x + 2, gy, 2, 1, p["gauge_tick_red"])
    else:
        # Horizontal energy or progress bar: IC2 striped vertical segments
        step = 4 if kind == "energy_h" else 6
        for idx, gx in enumerate(range(x + step, x + w - 2, step)):
            col = p["gauge_tick_red"] if (kind == "energy_h" and idx % 3 == 0) else p["gauge_tick"]
            fill_rect(draw, gx, y + 1, 1, h - 2, col)


def draw_engraved_connector(draw: ImageDraw.ImageDraw, x1: int, x2: int, y_mid: int, p: dict) -> None:
    """Draw a compact IC2 engraved arrow/connector between adjacent slots."""
    shaft_end = x2 - 4
    fill_rect(draw, x1 + 1, y_mid, max(1, shaft_end - x1), 3, p["engrave_light"])
    for i in range(4):
        fill_rect(draw, shaft_end + i + 1, y_mid - (3 - i) + 1, 1, 2 * (3 - i) + 1, p["engrave_light"])
    fill_rect(draw, x1, y_mid - 1, max(1, shaft_end - x1), 3, p["engrave_dark"])
    for i in range(4):
        fill_rect(draw, shaft_end + i, y_mid - (3 - i), 1, 2 * (3 - i) + 1, p["engrave_dark"])
    fill_rect(draw, x1 + 1, y_mid, max(1, shaft_end - x1 - 1), 1, p["engrave_fill"])


def draw_player_inventory(
    im: Image.Image,
    draw: ImageDraw.ImageDraw,
    sprites: dict[str, Image.Image],
    player_top: int,
    hotbar_y: int,
    p: dict,
    divider_line: bool = True,
    left_x: int = 8,
) -> None:
    """Draw the 3x9 player inventory, 1x9 hotbar, and subtle top divider line at playerTop - 5."""
    if divider_line:
        div_y = player_top - 5
        fill_rect(draw, left_x - 1, div_y, 162, 1, p["shadow"])
        fill_rect(draw, left_x - 1, div_y + 1, 162, 1, p["edge"])

    for row in range(3):
        for col in range(9):
            draw_slot(im, draw, sprites, left_x + 18 * col, player_top + 18 * row, p)
    for col in range(9):
        draw_slot(im, draw, sprites, left_x + 18 * col, hotbar_y, p)


def draw_recessed_subpanel(draw: ImageDraw.ImageDraw, x: int, y: int, w: int, h: int, p: dict) -> None:
    """Draw a recessed technical sub-panel (#B0B5B8) with inverted bevel (#555555/#FFFFFF) and #373737 corner rivets."""
    draw_bevel_box(draw, x, y, w, h, p["shadow"], p["edge"], p["seam"], fill=p["subpanel"])
    for rx, ry in ((x + 2, y + 2), (x + w - 3, y + 2), (x + 2, y + h - 3), (x + w - 3, y + h - 3)):
        draw.point((rx, ry), fill=p["rivet"])


def draw_oled_bezel(draw: ImageDraw.ImageDraw, x: int, y: int, w: int, h: int, p: dict) -> None:
    """Draw a flat #000000 OLED screen framed only by a 1px #373737 chassis bezel (no internal bevel)."""
    fill_rect(draw, x, y, w, h, p["shadow_deep"])
    fill_rect(draw, x + 1, y + 1, w - 2, h - 2, (0, 0, 0, 255))


def draw_engraved_divider(draw: ImageDraw.ImageDraw, x: int, y: int, w: int, p: dict) -> None:
    """Draw a crisp 2px horizontal engraved divider (#555555 top, #FFFFFF bottom)."""
    fill_rect(draw, x, y, w, 1, p["shadow"])
    fill_rect(draw, x, y + 1, w, 1, p["edge"])


def draw_wireless_arrows(draw: ImageDraw.ImageDraw, x: int, y: int, p: dict) -> None:
    """Draw three subtle engraved right-pointing arrows indicating wireless resource transfer."""
    for idx in range(3):
        ax = x + idx * 8
        for dx, dy in ((0, 0), (1, 1), (2, 2), (1, 3), (0, 4)):
            draw.point((ax + dx + 1, y + dy + 1), fill=p["engrave_light"])
            draw.point((ax + dx, y + dy), fill=p["engrave_dark"])


def draw_industrial_tank(
    im: Image.Image,
    draw: ImageDraw.ImageDraw,
    sprites: dict[str, Image.Image],
    x: int,
    y: int,
    w: int,
    h: int,
    p: dict,
    accent: tuple,
) -> None:
    """Draw a vertical IC2 fluid tank from design_kit with a color-coded frame accent and 1px #373737 ticks every 5px."""
    draw_gauge(im, draw, sprites, x, y, w, h, p, kind="fluid")
    # Color-coded top fillet & corner markers
    fill_rect(draw, x, y - 2, w, 2, accent)
    draw.point((x, y), fill=accent)
    draw.point((x + w - 1, y), fill=accent)
    # 1px #373737 scale ticks every 5px inside glass
    for gy in range(y + 5, y + h - 3, 5):
        tick_w = 5 if ((gy - y) % 10 == 0) else 3
        fill_rect(draw, x + w - 2 - tick_w, gy, tick_w, 1, p["shadow_deep"])


def draw_ghost_slot(
    im: Image.Image,
    draw: ImageDraw.ImageDraw,
    sprites: dict[str, Image.Image],
    x: int,
    y: int,
    p: dict,
    icon: str | None = None,
) -> None:
    """Stamp a clean 18x18 IC2 slot at (x-1, y-1) and optional monochrome #6E6E6E 16x16 ghost silhouette."""
    im.alpha_composite(sprites["slot"], (x - 1, y - 1))
    if not icon:
        return
    c = p["ghost"]
    pts: list[tuple[int, int]] = []
    if icon == "fert":
        # Fertilizer sack silhouette (16x16)
        pts = [
            (6, 2), (7, 2), (8, 2), (9, 2),
            (7, 3), (8, 3),
            (5, 4), (6, 4), (7, 4), (8, 4), (9, 4), (10, 4),
            (4, 5), (5, 5), (6, 5), (7, 5), (8, 5), (9, 5), (10, 5), (11, 5),
            (3, 6), (4, 6), (5, 6), (6, 6), (9, 6), (10, 6), (11, 6), (12, 6),
            (3, 7), (4, 7), (5, 7), (10, 7), (11, 7), (12, 7),
            (3, 8), (4, 8), (5, 8), (7, 8), (8, 8), (10, 8), (11, 8), (12, 8),
            (3, 9), (4, 9), (5, 9), (6, 9), (9, 9), (10, 9), (11, 9), (12, 9),
            (4, 10), (5, 10), (6, 10), (7, 10), (8, 10), (9, 10), (10, 10), (11, 10),
            (4, 11), (5, 11), (6, 11), (7, 11), (8, 11), (9, 11), (10, 11), (11, 11),
            (5, 12), (6, 12), (7, 12), (8, 12), (9, 12), (10, 12),
        ]
    elif icon == "cell":
        # Fluid cell/capsule silhouette
        pts = [
            (6, 2), (7, 2), (8, 2), (9, 2),
            (5, 3), (10, 3),
            (5, 4), (10, 4),
            (4, 5), (5, 5), (10, 5), (11, 5),
            (4, 6), (11, 6), (4, 7), (11, 7),
            (4, 8), (6, 8), (7, 8), (8, 8), (9, 8), (11, 8),
            (4, 9), (6, 9), (7, 9), (8, 9), (9, 9), (11, 9),
            (4, 10), (6, 10), (7, 10), (8, 10), (9, 10), (11, 10),
            (4, 11), (5, 11), (10, 11), (11, 11),
            (5, 12), (6, 12), (7, 12), (8, 12), (9, 12), (10, 12),
        ]
    elif icon == "bolt":
        # Battery / lightning bolt silhouette
        pts = [
            (8, 2), (9, 2), (7, 3), (8, 3), (6, 4), (7, 4), (5, 5), (6, 5),
            (4, 6), (5, 6), (6, 6), (7, 6), (8, 6), (9, 6), (10, 6), (11, 6),
            (7, 7), (8, 7), (9, 7), (10, 7),
            (7, 8), (8, 8), (9, 8),
            (6, 9), (7, 9), (8, 9),
            (6, 10), (7, 10), (5, 11), (6, 11), (5, 12),
        ]
    elif icon == "upgrade":
        # IC2 upgrade chip with pins silhouette
        for gx in range(4, 12):
            pts.append((gx, 4))
            pts.append((gx, 11))
        for gy in range(4, 12):
            pts.append((4, gy))
            pts.append((11, gy))
        for gx in (6, 7, 8, 9):
            pts.append((gx, 6))
            pts.append((gx, 9))
        for gx in (5, 8, 10):
            pts.append((gx, 2))
            pts.append((gx, 3))
            pts.append((gx, 12))
            pts.append((gx, 13))
        for gy in (6, 9):
            pts.append((2, gy))
            pts.append((3, gy))
            pts.append((12, gy))
            pts.append((13, gy))
    elif icon == "antenna":
        # Probe / wireless antenna silhouette
        pts = [
            (7, 3), (8, 3), (7, 4), (8, 4),
            (7, 6), (8, 6), (7, 7), (8, 7), (7, 8), (8, 8),
            (7, 9), (8, 9), (7, 10), (8, 10),
            (5, 11), (6, 11), (7, 11), (8, 11), (9, 11), (10, 11),
            (4, 12), (5, 12), (10, 12), (11, 12),
            (5, 4), (4, 5), (4, 6), (5, 7),
            (10, 4), (11, 5), (11, 6), (10, 7),
            (3, 3), (2, 4), (2, 5), (2, 6), (2, 7), (3, 8),
            (12, 3), (13, 4), (13, 5), (13, 6), (13, 7), (12, 8),
        ]
    elif icon == "transfer":
        # Harvest crate + transfer arrow silhouette
        pts = [
            (2, 4), (3, 4), (4, 4), (5, 4), (6, 4), (7, 4),
            (2, 5), (7, 5), (2, 6), (7, 6), (2, 7), (7, 7), (2, 8), (7, 8), (2, 9), (7, 9),
            (2, 10), (3, 10), (4, 10), (5, 10), (6, 10), (7, 10),
            (8, 7), (9, 7), (10, 7), (11, 7), (12, 7), (13, 7),
            (8, 8), (9, 8), (10, 8), (11, 8), (12, 8), (13, 8),
            (10, 5), (11, 6), (11, 9), (10, 10),
        ]
    for px, py in pts:
        draw.point((x + px, y + py), fill=c)


def update_icons_sheet(icons_path: Path) -> None:
    """Populate the 3 unused 16x16 cells (1,3), (2,3), (3,3) of icons.png with crisp industrial pixel-art icons."""
    im = Image.open(icons_path).convert("RGBA")
    assert im.size == (64, 64), f"icons.png must remain 64x64, got {im.size}"
    draw = ImageDraw.Draw(im)

    # Clear (1,3), (2,3), (3,3)
    for col in (1, 2, 3):
        fill_rect(draw, col * 16, 48, 16, 16, (0, 0, 0, 0))

    # (1,3) Automation Rules: 3 horizontal sliders with colored knobs
    ox, oy = 16, 48
    rail = (0x37, 0x37, 0x37, 255)
    knob_g = (0x44, 0xCC, 0x55, 255)
    knob_b = (0x3A, 0x9B, 0xDD, 255)
    knob_y = (0xEE, 0xBB, 0x33, 255)
    for ry, kx, kc in ((oy + 4, ox + 9, knob_b), (oy + 8, ox + 5, knob_g), (oy + 12, ox + 10, knob_y)):
        fill_rect(draw, ox + 2, ry, 12, 2, rail)
        fill_rect(draw, kx, ry - 1, 3, 4, kc)

    # (2,3) Redstone Output / Alarm signal beacon
    ox, oy = 32, 48
    fill_rect(draw, ox + 6, oy + 3, 4, 2, (0xDD, 0x33, 0x33, 255))
    fill_rect(draw, ox + 5, oy + 5, 6, 5, (0xEE, 0x44, 0x44, 255))
    fill_rect(draw, ox + 7, oy + 6, 2, 2, (0xFF, 0xCC, 0x66, 255))
    fill_rect(draw, ox + 4, oy + 10, 8, 2, (0x55, 0x55, 0x55, 255))
    fill_rect(draw, ox + 6, oy + 12, 4, 2, (0x37, 0x37, 0x37, 255))
    # Signal waves
    for px, py in ((3, 5), (2, 6), (2, 7), (3, 8), (12, 5), (13, 6), (13, 7), (12, 8)):
        draw.point((ox + px, oy + py), fill=(0xFF, 0xAA, 0x33, 255))

    # (3,3) Test 5s Pulse waveform
    ox, oy = 48, 48
    pulse = (0xFF, 0x55, 0x33, 255)
    hi = (0xFF, 0xCC, 0x55, 255)
    fill_rect(draw, ox + 2, oy + 10, 3, 2, pulse)
    fill_rect(draw, ox + 5, oy + 4, 2, 8, pulse)
    fill_rect(draw, ox + 5, oy + 4, 6, 2, hi)
    fill_rect(draw, ox + 9, oy + 4, 2, 8, pulse)
    fill_rect(draw, ox + 11, oy + 10, 3, 2, pulse)

    im.save(icons_path, format="PNG")


def build_screen(
    name: str,
    w: int,
    h: int,
    sidebar: bool,
    sx: int,
    machine: str,
    p: dict,
    sprites: dict[str, Image.Image],
    out_dir: Path,
) -> None:
    im = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    draw_outer_panel(im, sprites, w, h, sidebar, sx, p)
    draw = ImageDraw.Draw(im)

    if machine == "monitor":
        draw_oled_bezel(draw, 7, 17, 162, 16, p)
        draw_gauge(im, draw, sprites, 8, 42, 16, 36, p, kind="fluid")
        draw_gauge(im, draw, sprites, 30, 42, 16, 36, p, kind="fluid")
        draw_recessed_subpanel(draw, 73, 33, 96, 58, p)
        for i in range(15):
            draw_ghost_slot(im, draw, sprites, 76 + 18 * (i % 5), 36 + 18 * (i // 5), p)
        for i in range(4):
            draw_ghost_slot(im, draw, sprites, 98 + 18 * i, 92, p)
            draw_slot(im, draw, sprites, 8 + 18 * i, 127, p, icon="antenna")
        for i in range(3):
            draw_slot(im, draw, sprites, 98 + 18 * i, 127, p, icon="fert")
        draw_slot(im, draw, sprites, 8, 151, p, icon="bolt")
        draw_slot(im, draw, sprites, 98, 151, p, icon="transfer")
        if sidebar:
            draw_recessed_subpanel(draw, 179, 41, 139, 189, p)
        draw_player_inventory(im, draw, sprites, 173, 231, p)

    elif machine == "weeder":
        player_top, hotbar_y = 126, 184
        # Gauge
        draw_gauge(im, draw, sprites, 8, 36, 12, 28, p, kind="fluid")
        # Authentic IC2 right process arrow between (44,64) and (104,64) at x=68..96, y=68..76
        im.alpha_composite(sprites["arrow_right"], (71, 65))
        # Slots
        draw_slot(im, draw, sprites, 44, 64, p, icon="cell")
        draw_slot(im, draw, sprites, 104, 64, p)
        draw_slot(im, draw, sprites, 134, 64, p)

        if sidebar:
            # Framed content panel at x=180..316, y=26..198
            draw_bevel_box(draw, 180, 26, 137, 173, p["shadow"], p["edge"], p["seam"], fill=p["panel_dark"])

        draw_player_inventory(im, draw, sprites, player_top, hotbar_y, p, divider_line=True)

    elif machine == "composter":
        player_top, hotbar_y = 126, 184
        # Frame grouping the 4 upgrade slots at x=145..167, y=19..95
        draw_bevel_box(draw, 145, 19, 23, 77, p["shadow"], p["edge"], p["seam"], fill=p["panel_dark"])
        # Energy bolt above battery slot (8, 62)
        im.alpha_composite(sprites["bolt_off"], (13, 44))
        # Water tank (26, 28, 20, 50) + progress bar (74, 62, 30, 5)
        draw_gauge(im, draw, sprites, 26, 28, 20, 50, p, kind="fluid")
        draw_gauge(im, draw, sprites, 74, 62, 30, 5, p, kind="progress")
        # Authentic IC2 arrow_right centered between (54, 42) and (112, 42)
        im.alpha_composite(sprites["arrow_right"], (80, 42))
        # Slots
        draw_slot(im, draw, sprites, 54, 42, p, icon="fert")
        draw_slot(im, draw, sprites, 112, 42, p, icon="fert")

        draw_player_inventory(im, draw, sprites, player_top, hotbar_y, p, divider_line=True)

    elif machine == "grinder":
        player_top, hotbar_y = 126, 184
        # Frame grouping the 4 upgrade slots at x=145..167, y=19..95
        draw_bevel_box(draw, 145, 19, 23, 77, p["shadow"], p["edge"], p["seam"], fill=p["panel_dark"])
        # Energy bolt above battery slot (8, 62)
        im.alpha_composite(sprites["bolt_off"], (13, 44))
        # Progress bar
        draw_gauge(im, draw, sprites, 64, 80, 48, 5, p, kind="progress")
        # Authentic IC2 crusher/grinder icon above the middle blade slot
        im.alpha_composite(sprites["crush_off"], (78, 35))
        # Connectors between slots (38->80->122 at y=52)
        draw_engraved_connector(draw, 57, 77, 59, p)
        draw_engraved_connector(draw, 99, 119, 59, p)
        # Slots
        draw_slot(im, draw, sprites, 38, 52, p)
        draw_slot(im, draw, sprites, 80, 52, p, icon="blade")
        draw_slot(im, draw, sprites, 122, 52, p, icon="powder")

        draw_player_inventory(im, draw, sprites, player_top, hotbar_y, p, divider_line=True)

    elif machine == "analyzer":
        player_top, hotbar_y = 126, 184
        # Frame grouping the 4 upgrade slots at x=145..167, y=19..95
        draw_bevel_box(draw, 145, 19, 23, 77, p["shadow"], p["edge"], p["seam"], fill=p["panel_dark"])
        # Energy bolt above battery slot (8, 62)
        im.alpha_composite(sprites["bolt_off"], (13, 44))
        # Seed Bag input (34, 24) -> arrow (68, 20) + progress bar (58, 36, 42, 5) -> Seed Bag output (110, 24)
        draw_slot(im, draw, sprites, 34, 24, p)
        im.alpha_composite(sprites["arrow_right"], (68, 20))
        draw_gauge(im, draw, sprites, 58, 36, 42, 5, p, kind="progress")
        draw_slot(im, draw, sprites, 110, 24, p)
        # OLED genetic telemetry readout panel (30, 50, 110, 48)
        draw_oled_bezel(draw, 30, 50, 110, 48, p)
        # Upgrade column

        draw_player_inventory(im, draw, sprites, player_top, hotbar_y, p, divider_line=True)

    elif machine == "extractor":
        player_top, hotbar_y = 126, 184
        # Frame grouping the 4 upgrade slots at x=145..167, y=19..95
        draw_bevel_box(draw, 145, 19, 23, 77, p["shadow"], p["edge"], p["seam"], fill=p["panel_dark"])
        # Energy bolt above battery slot (8, 62)
        im.alpha_composite(sprites["bolt_off"], (13, 44))
        # Gauges
        draw_gauge(im, draw, sprites, 28, 28, 20, 50, p, kind="fluid")
        draw_gauge(im, draw, sprites, 112, 28, 20, 50, p, kind="fluid")
        draw_gauge(im, draw, sprites, 56, 59, 48, 5, p, kind="progress")
        # Slots
        draw_slot(im, draw, sprites, 24, 84, p, icon="powder")
        draw_slot(im, draw, sprites, 54, 84, p, icon="cell")
        draw_slot(im, draw, sprites, 84, 84, p, icon="cell")
        draw_slot(im, draw, sprites, 114, 84, p)

        draw_player_inventory(im, draw, sprites, player_top, hotbar_y, p, divider_line=True)

    elif machine == "mixer":
        player_top, hotbar_y = 126, 184
        # Frame grouping the 4 upgrade slots at x=145..167, y=19..95
        draw_bevel_box(draw, 145, 19, 23, 77, p["shadow"], p["edge"], p["seam"], fill=p["panel_dark"])
        # Energy bolt above battery slot (8, 62)
        im.alpha_composite(sprites["bolt_off"], (13, 44))
        # Gauges
        draw_gauge(im, draw, sprites, 28, 28, 20, 50, p, kind="fluid")
        draw_gauge(im, draw, sprites, 112, 28, 20, 50, p, kind="fluid")
        draw_gauge(im, draw, sprites, 56, 59, 48, 5, p, kind="progress")
        # Subtle vertical conduit from Redstone slot (80,32) to progress bar (64,59,48,5)
        fill_rect(draw, 79, 50, 2, 8, p["engrave_dark"])
        fill_rect(draw, 81, 50, 1, 8, p["engrave_light"])
        # Clean symmetric 4-slot layout (no overlapping arrow behind progress text)
        draw_slot(im, draw, sprites, 72, 32, p, icon="redstone")
        draw_slot(im, draw, sprites, 28, 84, p, icon="cell")
        draw_slot(im, draw, sprites, 84, 84, p, icon="cell")
        draw_slot(im, draw, sprites, 116, 84, p)

        draw_player_inventory(im, draw, sprites, player_top, hotbar_y, p, divider_line=True)

    elif machine == "manager":
        draw_gauge(im, draw, sprites, 10, 22, 18, 52, p, kind="fluid")
        draw_gauge(im, draw, sprites, 74, 22, 18, 52, p, kind="fluid")
        for x in (33, 51):
            draw_slot(im, draw, sprites, x, 24, p, icon="cell")
            draw_slot(im, draw, sprites, x, 42, p)
        for i in range(3):
            draw_slot(im, draw, sprites, 10 + 18 * i, 85, p, icon="fert")
        draw_recessed_subpanel(draw, 95, 19, 93, 59, p)
        for i in range(15):
            draw_slot(im, draw, sprites, 98 + 18 * (i % 5), 22 + 18 * (i // 5), p)
        for i in range(4):
            draw_slot(im, draw, sprites, 98 + 18 * i, 85, p, icon="upgrade")
        if sidebar:
            draw_recessed_subpanel(draw, 198, 32, 108, 170, p)
        draw_player_inventory(im, draw, sprites, 126, 184, p, left_x=16)

    if machine in ("composter", "grinder", "extractor", "mixer", "analyzer"):
        draw_slot(im, draw, sprites, 8, 62, p, icon="bolt")
        for y in (22, 40, 58, 76):
            draw_slot(im, draw, sprites, 148, y, p, icon="upgrade")

    out_path = out_dir / f"{name}.png"
    im.save(out_path, format="PNG")


def build_chest_textures(p: dict, sprites: dict[str, Image.Image], out_dir: Path, dark: bool) -> None:
    """Generate IC2-styled Crop Database Chest GUI (256x256) and side drawer (128x128)."""
    im = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    draw_nine_slice_panel(im, sprites["panel_9slice"], 0, 0, 195, 216, p["body"])
    draw = ImageDraw.Draw(im)

    lcd_bg = (0x0A, 0x0E, 0x12, 255) if dark else (0x12, 0x18, 0x1C, 255)
    lcd_hdr = (0x12, 0x18, 0x1F, 255) if dark else (0x1B, 0x24, 0x2A, 255)
    lcd_box = (0x0E, 0x14, 0x1A, 255) if dark else (0x17, 0x20, 0x26, 255)
    lcd_line = (0x24, 0x30, 0x3A, 255) if dark else (0x2B, 0x39, 0x42, 255)

    # Match the readout, 5x5 seed grid, toolbar and inventory in CropGuiLayout.
    draw_bevel_box(draw, 7, 11, 86, 91, p["shadow"], p["edge"], p["seam"], fill=lcd_bg)
    fill_rect(draw, 8, 12, 84, 12, lcd_hdr)
    fill_rect(draw, 8, 43, 84, 1, lcd_line)
    fill_rect(draw, 8, 74, 84, 1, lcd_line)
    draw_bevel_box(draw, 8, 82, 84, 19, p["shadow"], lcd_line, lcd_bg, fill=lcd_box)
    draw_bevel_box(draw, 96, 12, 94, 94, p["shadow"], p["edge"], p["seam"], fill=p["panel_dark"])
    for row in range(5):
        for col in range(5):
            draw_slot(im, draw, sprites, 99 + col * 18, 15 + row * 18, p)
    draw_bevel_box(draw, 7, 107, 181, 18, p["shadow"], p["edge"], p["seam"], fill=p["panel_mid"])
    draw_player_inventory(im, draw, sprites, 133, 191, p, left_x=17)

    chest_path = out_dir / "crop_database_chest.png"
    im.save(chest_path, format="PNG")
    verify_png(chest_path, 256, 256)

    # Side drawer (100x112) + tab (22x24) in 128x128
    dr = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    draw_nine_slice_panel(dr, sprites["panel_9slice"], 0, 0, 100, 112, p["body"])
    ddraw = ImageDraw.Draw(dr)
    draw_bevel_box(ddraw, 6, 6, 88, 16, p["shadow"], p["edge"], p["seam"], fill=p["panel_mid"])
    draw_bevel_box(ddraw, 6, 76, 88, 30, p["shadow"], p["edge"], p["seam"], fill=p["panel_dark"])
    draw_nine_slice_panel(dr, sprites["panel_9slice"], 104, 0, 22, 24, p["body"])
    drawer_path = out_dir / "crop_database_drawer.png"
    dr.save(drawer_path, format="PNG")
    verify_png(drawer_path, 128, 128)


def build_wireless_eu_upgrade_icon(out_path: Path) -> None:
    """Generate the 16x16 wireless_eu_upgrade.png item icon with crisp IC2 chip casing and lightning core."""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)

    fill_rect(draw, 2, 2, 12, 11, (0x25, 0x2B, 0x30, 255))
    fill_rect(draw, 3, 3, 10, 9, (0x71, 0x82, 0x8B, 255))
    fill_rect(draw, 4, 4, 8, 7, (0x14, 0x19, 0x1E, 255))
    # Red energy base + crisp gold lightning bolt in the center
    fill_rect(draw, 5, 5, 6, 5, (0xC9, 0x34, 0x24, 255))
    for px, py in ((8, 5), (7, 6), (8, 6), (6, 7), (7, 7), (8, 7), (9, 7), (7, 8), (8, 8), (7, 9)):
        draw.point((px, py), fill=(0xFF, 0xD8, 0x4A, 255))
    # Gold contact pins
    pin = (0xB5, 0xA0, 0x5C, 255)
    for px in (4, 7, 10):
        fill_rect(draw, px, 13, 2, 2, pin)
    for py in (5, 9):
        fill_rect(draw, 0, py, 2, 2, pin)
        fill_rect(draw, 14, py, 2, 2, pin)

    out_path.parent.mkdir(parents=True, exist_ok=True)
    im.save(out_path, format="PNG")


def verify_png(path: Path, expected_w: int, expected_h: int) -> None:
    raw = path.read_bytes()
    w, h, bits, mode = struct.unpack(">IIBB", raw[16:26])
    assert (w, h) == (expected_w, expected_h), f"{path}: size {(w, h)} != {(expected_w, expected_h)}"
    assert (bits, mode) == (8, 6), f"{path}: (bits, mode) {(bits, mode)} != (8, 6)"


def main() -> None:
    dark_pack_dir = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else DEFAULT_DARK_PACK
    dark_gui_dir = dark_pack_dir / "assets" / "cropmanager" / "textures" / "gui"

    GUI_LIGHT_DIR.mkdir(parents=True, exist_ok=True)
    dark_gui_dir.mkdir(parents=True, exist_ok=True)

    light_sprites = load_ic2_sprites(dark=False)
    dark_sprites = load_ic2_sprites(dark=True)

    screens = [
        ("equipment_monitor", 322, 256, True, 174, "monitor"),
        ("equipment_monitor_compact", 176, 256, False, 174, "monitor"),
        ("equipment_weeder_expanded", 322, 208, True, 174, "weeder"),
        ("equipment_weeder_compact", 176, 208, False, 174, "weeder"),
        ("equipment_composter", 176, 208, False, 174, "composter"),
        ("equipment_grinder", 176, 208, False, 174, "grinder"),
        ("equipment_extractor", 176, 208, False, 174, "extractor"),
        ("equipment_mixer", 176, 208, False, 174, "mixer"),
        ("equipment_analyzer", 176, 208, False, 174, "analyzer"),
        ("equipment_legacy", 176, 208, False, 174, "composter"),
        ("manager_advanced", 310, 208, True, 192, "manager"),
        ("manager_advanced_compact", 194, 208, False, 192, "manager"),
        ("manager_basic", 194, 208, False, 192, "manager"),
    ]

    for name, w, h, side, sx, machine in screens:
        build_screen(name, w, h, side, sx, machine, LIGHT_PALETTE, light_sprites, GUI_LIGHT_DIR)
        build_screen(name, w, h, side, sx, machine, DARK_PALETTE, dark_sprites, dark_gui_dir)
        verify_png(GUI_LIGHT_DIR / f"{name}.png", 512, 512)
        verify_png(dark_gui_dir / f"{name}.png", 512, 512)

    update_icons_sheet(GUI_LIGHT_DIR / "icons.png")
    verify_png(GUI_LIGHT_DIR / "icons.png", 64, 64)

    build_chest_textures(LIGHT_PALETTE, light_sprites, GUI_LIGHT_DIR, dark=False)
    build_chest_textures(DARK_PALETTE, dark_sprites, dark_gui_dir, dark=True)

    # Verify (4..15, 4..15) body color on equipment_monitor.png
    for theme_dir, expected_rgb in ((GUI_LIGHT_DIR, (0xC6, 0xC6, 0xC6, 255)), (dark_gui_dir, (0x25, 0x2A, 0x30, 255))):
        mon = Image.open(theme_dir / "equipment_monitor.png")
        for y in range(4, 16):
            for x in range(4, 16):
                assert mon.getpixel((x, y)) == expected_rgb, f"Mismatch at ({x},{y}) in {theme_dir}"
        assert mon.getpixel((500, 500)) == (0, 0, 0, 0)

    # Generate wireless_eu_upgrade.png
    icon_path = ITEMS_DIR / "wireless_eu_upgrade.png"
    build_wireless_eu_upgrade_icon(icon_path)
    verify_png(icon_path, 16, 16)

    # Write pack.mcmeta and zip dark resource pack
    pack_meta = {"pack": {"pack_format": 3, "description": "Crop Manager - interfaces IC2 em tema escuro"}}
    meta_path = dark_pack_dir / "pack.mcmeta"
    meta_path.write_text(json.dumps(pack_meta, ensure_ascii=False), encoding="utf-8")

    zip_path = dark_pack_dir.parent / f"{dark_pack_dir.name}.zip"
    with zipfile.ZipFile(zip_path, "w", compression=zipfile.ZIP_DEFLATED) as zf:
        for file_path in sorted(dark_pack_dir.rglob("*")):
            if file_path.is_file():
                zf.write(file_path, file_path.relative_to(dark_pack_dir).as_posix())

    print(f"Generated {len(screens)} light GUIs, {len(screens)} dark GUIs, wireless_eu_upgrade.png, and {zip_path.name}.")


if __name__ == "__main__":
    main()
