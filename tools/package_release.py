"""Package only the mod, its source project, and documentation (no dependencies)."""
import hashlib
import shutil
import zipfile
from pathlib import Path

project = Path(__file__).resolve().parents[1]
dist = project / "dist"
dist.mkdir(exist_ok=True)
name = "CropManager-0.5.0-alpha2"
shutil.copyfile(project / f"build/libs/{name}.jar", dist / f"{name}.jar")
files = []
for directory in ("src", "gradle", "tools", "docs"):
    files.extend(path for path in (project / directory).rglob("*") if path.is_file() and "__pycache__" not in path.parts)
files.extend(project / file for file in ("README.md", ".gitignore", "build.gradle", "settings.gradle", "gradle.properties", "gradlew", "gradlew.bat"))
with zipfile.ZipFile(dist / f"{name}-source.zip", "w", zipfile.ZIP_DEFLATED) as archive:
    for path in sorted(files):
        relative = path.relative_to(project).as_posix()
        info = zipfile.ZipInfo(f"{name}/{relative}", date_time=(2026, 11, 14, 0, 0, 0))
        info.compress_type = zipfile.ZIP_DEFLATED
        info.external_attr = (0o755 if relative == "gradlew" else 0o644) << 16
        archive.writestr(info, path.read_bytes())
shutil.copyfile(project / "README.md", dist / "README.md")
with (dist / "SHA256SUMS.txt").open("w", encoding="utf-8") as output:
    for suffix in (".jar", "-source.zip"):
        path = dist / f"{name}{suffix}"
        output.write(hashlib.sha256(path.read_bytes()).hexdigest() + "  " + path.name + "\n")
print(f"Packaged release in {dist}")
