"""Import the approved PNGs verbatim and generate Minecraft 1.12.2 models."""
import json
import struct
import sys
import zipfile
from pathlib import Path

root = Path(__file__).resolve().parents[1] / "src/main/resources"
assets = root / "assets/cropmanager"
names = ["back", "bottom", "front", "front_active", "left", "right", "service", "top"]
with zipfile.ZipFile(sys.argv[1]) as archive:
    for name in names:
        relative = f"assets/cropmanager/textures/blocks/crop_manager_{name}.png"
        data = archive.read(relative)
        assert data[:8] == b"\x89PNG\r\n\x1a\n"
        assert struct.unpack(">IIBB", data[16:26]) == (16, 16, 8, 6), relative
        target = root / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)

def write(relative, value):
    target = assets / relative
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")

for active in (False, True):
    suffix = "_active" if active else ""
    textures = {side: f"cropmanager:blocks/crop_manager_{name}" for side, name in
                {"down": "bottom", "up": "top", "north": "front" + suffix,
                 "south": "back", "west": "left", "east": "right", "particle": "back"}.items()}
    write(f"models/block/crop_manager{suffix}.json", {"parent": "block/cube", "textures": textures})
write("models/item/crop_manager.json", {"parent": "cropmanager:block/crop_manager"})
variants = {}
for facing, rotation in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
    for active in (False, True):
        variants[f"active={str(active).lower()},facing={facing}"] = {
            "model": "cropmanager:crop_manager" + ("_active" if active else ""), "y": rotation, "uvlock": True}
write("blockstates/crop_manager.json", {"variants": variants})
print("8 original PNGs imported; 8 blockstate variants and 3 models generated.")
