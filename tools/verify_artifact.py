"""Inspect the current distributable and all registered assets without launching Minecraft."""
import hashlib
import json
import struct
import zipfile
from pathlib import Path

project = Path(__file__).resolve().parents[1]
version = "0.5.0-alpha11"
artifact = project / f"build/libs/IC2-Agronomy-Expansion-{version}.jar"
resources = project / "src/main/resources"
retired = {"crop_manager", "seed_classifier", "seed_workbench", "configuration_card", "agricultural_notebook", "compost"}

expected_states = {
    p.stem for p in (resources / "assets/cropmanager/blockstates").glob("*.json")
}

with zipfile.ZipFile(artifact) as jar:
    names = set(jar.namelist())
    classes = [n for n in names if n.endswith(".class")]
    assert classes and all(n.startswith("br/cropmanager/") for n in classes)
    assert not any("RuntimeChecks" in n or "ClientChecks" in n for n in classes)
    for name in classes:
        assert struct.unpack(">H", jar.read(name)[6:8])[0] == 52, name

    metadata = json.loads(jar.read("mcmod.info"))
    assert metadata[0]["version"] == version, metadata[0]["version"]
    assert metadata[0]["name"] == "IC2: Agronomy Expansion", metadata[0]["name"]
    assert "assets/cropmanager/models/item/wireless_eu_upgrade.json" in names
    assert "assets/cropmanager/textures/items/wireless_eu_upgrade.png" in names

    states = {Path(n).stem for n in names if n.startswith("assets/cropmanager/blockstates/") and n.endswith(".json")}
    assert states == expected_states, (states ^ expected_states)

    for block in retired:
        assert f"assets/cropmanager/models/item/{block}.json" not in names, block

    modelpaths = [n for n in names if n.startswith("assets/cropmanager/models/") and n.endswith(".json")]
    for path in modelpaths:
        data = json.loads(jar.read(path))
        parent = data.get("parent", "")
        if parent.startswith("cropmanager:"):
            assert "assets/cropmanager/models/" + parent.split(":")[1] + ".json" in names, parent
        for texture in data.get("textures", {}).values():
            if texture.startswith("cropmanager:"):
                assert "assets/cropmanager/textures/" + texture.split(":")[1] + ".png" in names, texture
        for part in data.get("elements", []):
            assert all(-16 <= v <= 32 for v in part["from"] + part["to"]), path
            if "rotation" in part:
                assert part["rotation"]["angle"] in (-45, -22.5, 0, 22.5, 45), path
            for face in part["faces"].values():
                assert face["texture"].lstrip("#") in data["textures"], path

    sprites = [n for n in names if n.startswith("assets/cropmanager/textures/") and n.endswith(".png")]
    for name in sprites:
        png = jar.read(name)
        w, h, bits, mode = struct.unpack(">IIBB", png[16:26])
        assert w > 0 and h > 0, name
        assert png == (resources / name).read_bytes(), name
        if "/textures/blocks/" in name and h > w and h % w == 0:
            assert json.loads(jar.read(name + ".mcmeta"))["animation"]["frametime"] > 0, name

    locales = {}
    for lang in ("en_us", "pt_br"):
        rows = [
            s.split("=", 1)
            for s in jar.read("assets/cropmanager/lang/" + lang + ".lang").decode("utf-8").splitlines()
            if "=" in s and not s.lstrip().startswith("#")
        ]
        locales[lang] = dict(rows)
        assert len(rows) == len(locales[lang]), lang
        assert all(value.strip() and "\ufffd" not in value for value in locales[lang].values()), lang
    assert locales["en_us"].keys() == locales["pt_br"].keys()

    print(
        f"Verified {len(classes)} Java 8 classes; {len(states)} active blockstates; "
        f"{len(modelpaths)} models; {len(sprites)} sprites; {len(locales['en_us'])} matching translations."
    )
print("SHA256", hashlib.sha256(artifact.read_bytes()).hexdigest())
