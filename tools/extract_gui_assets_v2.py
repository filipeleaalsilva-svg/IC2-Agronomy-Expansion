#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
extract_gui_assets_v2.py (Corrigido para IC2 Experimental 1.12.2)
Extrator automatizado de widgets de GUI, removedor de fundo (Alpha Transparent)
e gerador do banco de dados visual (design_kit/asset_database.json) para mods de Minecraft.
Detecta automaticamente se a entrada é uma pasta comum, um arquivo .jar (mod) ou .zip (resource pack).

Requisitos:
    pip install Pillow
"""

import io
import re
import json
import zipfile
import argparse
from collections import Counter, deque
from pathlib import Path
from PIL import Image

BORDER_COLORS_HEX = {"#FFFFFF", "#555555", "#373737"}

DEFAULT_LIGHT_TO_DARK_MAP = {
    "#C6C6C6": "#2D2D30",  # Fundo principal do painel
    "#FFFFFF": "#505057",  # Borda 3D iluminada (topo/esquerda)
    "#555555": "#1B1B1E",  # Borda 3D sombreada (baixo/direita)
    "#373737": "#141416",  # Contorno externo / sombra de slot
    "#8B8B8B": "#222225",  # Fundo interno do slot 18x18
}


def rgba_to_hex(rgba):
    return f"#{rgba[0]:02X}{rgba[1]:02X}{rgba[2]:02X}"


def hex_to_rgba(hex_code, alpha=255):
    hex_code = hex_code.lstrip("#")
    return (
        int(hex_code[0:2], 16),
        int(hex_code[2:4], 16),
        int(hex_code[4:6], 16),
        alpha,
    )


def extract_palette(img: Image.Image, max_colors=32):
    rgba_img = img.convert("RGBA")
    pixels = list(rgba_img.getdata())
    opaque_pixels = [rgba_to_hex(p) for p in pixels if p[3] > 0]
    counts = Counter(opaque_pixels)
    total = max(len(opaque_pixels), 1)

    palette = []
    for hex_color, count in counts.most_common(max_colors):
        role = "custom/semantic"
        if hex_color == "#C6C6C6":
            role = "panel_background"
        elif hex_color == "#8B8B8B":
            role = "slot_background"
        elif hex_color in BORDER_COLORS_HEX:
            role = "3d_border"
        elif hex_color == "#000000":
            role = "digital_display_or_outline"

        palette.append({
            "hex": hex_color,
            "pixel_count": count,
            "percentage": round((count / total) * 100, 2),
            "inferred_role": role
        })
    return palette


def remove_background_color(crop_img: Image.Image, bg_hex_list):
    rgba = crop_img.convert("RGBA")
    width, height = rgba.size
    pixels = rgba.load()
    target_rgbs = {hex_to_rgba(h)[:3] for h in bg_hex_list}

    visited = set()
    queue = deque()

    for x in range(width):
        queue.append((x, 0))
        queue.append((x, height - 1))
    for y in range(height):
        queue.append((0, y))
        queue.append((width - 1, y))

    while queue:
        cx, cy = queue.popleft()
        if (cx, cy) in visited:
            continue
        if cx < 0 or cx >= width or cy < 0 or cy >= height:
            continue
        visited.add((cx, cy))

        r, g, b, a = pixels[cx, cy]
        if a == 0 or (r, g, b) in target_rgbs:
            pixels[cx, cy] = (0, 0, 0, 0)
            for nx, ny in ((cx + 1, cy), (cx - 1, cy), (cx, cy + 1), (cx, cy - 1)):
                if (nx, ny) not in visited:
                    queue.append((nx, ny))

    return rgba


def classify_widget(u, v, w, h):
    if (w == 18 and h == 18) or (w == 26 and h == 26):
        return "slots", "Slot padrão de inventário/máquina"
    if w in (16, 18, 20, 22) and h in (16, 18, 20, 22) and u >= 176:
        return "buttons", "Botão interativo ou ícone de controle lateral"
    if (w <= 24 and h >= 32) or (h <= 24 and w >= 32):
        return "bars_and_tanks", "Barra de energia/progresso ou medidor de tanque"
    if w >= 120 and h >= 100:
        return "chassis_base", "Painel principal da GUI"
    return "symbols_and_icons", "Símbolo, chama, raio, seta ou indicador de estado"


def detect_connected_widgets(img: Image.Image, bg_hex_list, start_u=176):
    rgba = img.convert("RGBA")
    width, height = rgba.size
    pixels = rgba.load()
    bg_rgbs = {hex_to_rgba(h)[:3] for h in bg_hex_list}

    visited = [[False] * height for _ in range(width)]
    boxes = []
    scan_start_x = start_u if width >= 256 else 0

    for x in range(scan_start_x, width):
        for y in range(height):
            if visited[x][y]:
                continue
            r, g, b, a = pixels[x, y]
            if a == 0 or (r, g, b) in bg_rgbs:
                visited[x][y] = True
                continue

            min_x, max_x = x, x
            min_y, max_y = y, y
            q = deque([(x, y)])
            visited[x][y] = True

            while q:
                cx, cy = q.popleft()
                min_x = min(min_x, cx)
                max_x = max(max_x, cx)
                min_y = min(min_y, cy)
                max_y = max(max_y, cy)

                for nx in range(max(scan_start_x, cx - 1), min(width, cx + 2)):
                    for ny in range(max(0, cy - 1), min(height, cy + 2)):
                        if not visited[nx][ny]:
                            nr, ng, nb, na = pixels[nx, ny]
                            if na > 0 and (nr, ng, nb) not in bg_rgbs:
                                visited[nx][ny] = True
                                q.append((nx, ny))

            w = (max_x - min_x) + 1
            h = (max_y - min_y) + 1
            if w >= 3 and h >= 3:
                boxes.append((min_x, min_y, w, h))

    return boxes


def iter_png_sources(input_path: Path):
    """
    Detecta automaticamente o tipo de entrada:
    1. Se for um arquivo .jar ou .zip, lê os .png direto de dentro do arquivo compactado em memória.
    2. Se for uma pasta, varre todos os .png soltos E também abre quaisquer .jar / .zip que estejam dentro dela.
    """
    if input_path.is_file() and input_path.suffix.lower() in (".jar", ".zip"):
        with zipfile.ZipFile(input_path, "r") as zf:
            for entry in zf.namelist():
                if entry.lower().endswith(".png") and not entry.endswith("/"):
                    try:
                        data = zf.read(entry)
                        if not data:
                            continue
                        yield f"{input_path.name}!/{entry}", entry, Path(entry).stem, Image.open(io.BytesIO(data)).convert("RGBA")
                    except Exception as e:
                        print(f"[AVISO] Ignorando imagem inválida {entry}: {e}")
        return

    if input_path.is_dir():
        for png_path in input_path.rglob("*.png"):
            if "design_kit" in png_path.parts:
                continue
            try:
                rel = str(png_path.relative_to(input_path)).replace("\\", "/")
                yield rel, rel, png_path.stem, Image.open(png_path).convert("RGBA")
            except Exception as e:
                print(f"[AVISO] Ignorando imagem inválida {png_path}: {e}")

        for archive_path in list(input_path.rglob("*.jar")) + list(input_path.rglob("*.zip")):
            try:
                with zipfile.ZipFile(archive_path, "r") as zf:
                    for entry in zf.namelist():
                        if entry.lower().endswith(".png") and not entry.endswith("/"):
                            try:
                                data = zf.read(entry)
                                if not data:
                                    continue
                                rel = f"{archive_path.name}!/{entry}"
                                yield rel, entry, Path(entry).stem, Image.open(io.BytesIO(data)).convert("RGBA")
                            except Exception as e:
                                print(f"[AVISO] Ignorando imagem inválida {entry}: {e}")
            except Exception as e:
                print(f"[AVISO] Ignorando arquivo compactado inválido {archive_path}: {e}")


def process_project_assets(input_path: Path, output_dir: Path, bg_colors=None):
    if bg_colors is None:
        bg_colors = ["#C6C6C6"]

    categories = ["buttons", "slots", "bars_and_tanks", "symbols_and_icons", "chassis_base", "upgrades_16x16"]
    for cat in categories:
        (output_dir / "widgets" / cat).mkdir(parents=True, exist_ok=True)

    database = {
        "project_info": {
            "source_target": str(input_path),
        },
        "light_to_dark_map": DEFAULT_LIGHT_TO_DARK_MAP,
        "palettes_by_file": {},
        "widgets": []
    }

    widget_counter = 1
    for rel_path, internal_path, stem, img in iter_png_sources(input_path):
        width, height = img.size
        database["palettes_by_file"][rel_path] = {
            "dimensions": f"{width}x{height}",
            "colors": extract_palette(img)
        }

        # Copia upgrades 16x16 e chassis de máquinas para servirem de molde
        norm_internal = internal_path.lower().replace("\\", "/")
        if width == 16 and height == 16:
            if "/items/upgrade/" in norm_internal or "/items/tool/electric/" in norm_internal:
                out_up = output_dir / "widgets" / "upgrades_16x16" / f"{stem}.png"
                img.save(out_up, "PNG")
            elif "/blocks/machine/" in norm_internal or "/blocks/resource/machine" in norm_internal:
                out_ch = output_dir / "widgets" / "chassis_base" / f"block_{stem}.png"
                img.save(out_ch, "PNG")

        if width in (256, 512) and height in (256, 512):
            is_pure_widget_sheet = any(k in stem.lower() for k in ("widget", "icon", "element", "jei", "tab", "common", "overlay", "gui"))
            start_u = 0 if is_pure_widget_sheet else 176
            boxes = detect_connected_widgets(img, bg_colors, start_u=start_u)

            for (u, v, w, h) in boxes:
                crop = img.crop((u, v, u + w, v + h))
                clean_crop = remove_background_color(crop, bg_colors)

                category, usage_rule = classify_widget(u, v, w, h)
                widget_id = f"{stem}_{category[:-1]}_{widget_counter:03d}"
                out_filename = f"{widget_id}_u{u}_v{v}_{w}x{h}.png"
                out_path = output_dir / "widgets" / category / out_filename

                clean_crop.save(out_path, "PNG")

                database["widgets"].append({
                    "id": widget_id,
                    "category": category,
                    "file_path": str(out_path).replace("\\", "/"),
                    "original_texture": rel_path,
                    "uv_coords": {"u": u, "v": v, "width": w, "height": h},
                    "has_active_state": category in ("buttons", "bars_and_tanks", "symbols_and_icons"),
                    "usage_rule": usage_rule
                })
                widget_counter += 1

    db_file = output_dir / "asset_database.json"
    db_file.write_text(json.dumps(database, indent=2, ensure_ascii=False), encoding="utf-8")
    print(f"[OK] Extração concluída! PNGs lidos: {len(database['palettes_by_file'])} | Widgets: {len(database['widgets'])}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Extrator universal de assets (Pastas, .JAR e .ZIP)")
    parser.add_argument("--input", "-i", type=str, default="industrialcraft-2-2.8.222-ex112.jar", help="Pasta, arquivo .jar ou arquivo .zip")
    parser.add_argument("--output", "-o", type=str, default="design_kit", help="Pasta de saída")
    parser.add_argument("--bg-hex", nargs="+", default=["#C6C6C6"], help="Cores Hex de fundo para remover")
    args = parser.parse_args()
    process_project_assets(Path(args.input), Path(args.output), args.bg_hex)
