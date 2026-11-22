"""Build an isolated dark IC2 GUI resource pack for the opt-in contrast QA."""
from pathlib import Path
import os
root=Path(__file__).resolve().parents[1]
os.chdir(root)
(root/'build').mkdir(exist_ok=True)
from zipfile import ZipFile,ZIP_DEFLATED
from io import BytesIO
from PIL import Image
with ZipFile('libs/industrialcraft-2-2.8.222-ex112.jar') as z: im=Image.open(BytesIO(z.read('assets/ic2/textures/gui/guicropmatron.png'))).convert('RGBA')
im.putdata([(round(r*0.24),round(g*0.24),round(b*0.24),a) for r,g,b,a in im.getdata()]);buf=BytesIO();im.save(buf,format='PNG')
with ZipFile('build/contrast-pack.zip','w',ZIP_DEFLATED) as z:
 z.writestr('pack.mcmeta','{"pack":{"pack_format":3,"description":"Isolated dark GUI contrast test"}}');z.writestr('assets/ic2/textures/gui/guicropmatron.png',buf.getvalue())
