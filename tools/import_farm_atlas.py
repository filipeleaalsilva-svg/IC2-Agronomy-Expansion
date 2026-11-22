"""Import the pixel atlas using IC2's actual casing and exact RGB palette.

Requires the audited IC2 dependency. Every machine face is 16x16; activity
strips contain two 16x16 frames and never use interpolation.
"""
from pathlib import Path
from zipfile import ZipFile
from io import BytesIO
import json
from PIL import Image, ImageDraw

root = Path(__file__).resolve().parents[1]
assets = root / 'src/main/resources/assets/cropmanager'
atlas = Image.open(root / 'docs/artwork/farm-atlas-ic2.png').convert('RGBA')
source_names = [
    'processing/basic/macerator_leftrightback', 'processing/basic/macerator_front',
    'processing/basic/macerator_top', 'resource/cropmatron_front_active',
    'fluid/pump_front_active', 'processing/basic/electric_furnace_front_active',
    'resource/crop_harvester_sides', 'misc/sorting_machine_front']
with ZipFile(root / 'libs/industrialcraft-2-2.8.222-ex112.jar') as jar:
    originals = []
    for name in source_names:
        supplied = root / 'textures/blocks/machine' / (name + '.png')
        data = supplied.read_bytes() if supplied.exists() else jar.read('assets/ic2/textures/blocks/machine/' + name + '.png')
        originals.append(Image.open(BytesIO(data)).convert('RGBA'))
palette = sorted(set(pixel for im in originals for pixel in im.getdata() if pixel[3] == 255))
base = originals[0].resize((16,16), Image.Resampling.NEAREST)
cache = {}
def color(pixel):
    if pixel not in cache:
        cache[pixel] = min(palette, key=lambda candidate: sum((candidate[i]-pixel[i])**2 for i in range(3)))
    return cache[pixel]
def quantize(im):
    out=Image.new('RGBA', im.size)
    out.putdata([color(p) for p in im.getdata()]); return out

def cell(column, row):
    w,h=atlas.size
    return quantize(atlas.crop((round(column*w/5),round(row*h/4),round((column+1)*w/5),round((row+1)*h/4))).resize((16,16),Image.Resampling.NEAREST))
def fascia(im, box=(3,3,13,13), lamp=False):
    out=base.copy(); out.paste(im.crop(box),box[:2])
    if lamp: out.paste(im.crop((6,1,10,3)),(6,1))
    return out

def write(name,value):
    path=assets/name; path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')

for name,row in [('advanced_crop_manager',0),('agricultural_monitor',2)]:
    front=fascia(cell(0,row),lamp=True)
    frames={'front':front,'back':fascia(cell(2 if row<2 else 1,row),(5,5,11,11)),
            'top':fascia(cell(3 if row<2 else 2,row),(4,4,12,12)),
            'side':base.copy(),'bottom':base.copy()}
    if row==1:
        frames['side']=fascia(cell(4,row),(5,6,11,10))
        frames['bottom']=fascia(cell(2,row),(5,5,11,11))
    if row<2:
        one=fascia(cell(1,row),lamp=True); two=one.copy()
        two.paste(cell(2+row,3).crop((6,1,10,3)),(6,1))
        active=Image.new('RGBA',(16,32)); active.paste(one,(0,0)); active.paste(two,(0,16))
        frames['front_active']=active
    else:
        active=front.copy(); d=ImageDraw.Draw(active); d.rectangle((6,1,9,2),fill=color((203,53,35,255))); frames['front_active']=active
    for side,image in frames.items():
        path=assets/f'textures/blocks/{name}_{side}.png'; path.parent.mkdir(parents=True,exist_ok=True); image.save(path)
        if image.height==32: write(f'textures/blocks/{name}_{side}.png.mcmeta',{'animation':{'frametime':8,'interpolate':False}})
    variants={}
    for active in (False,True):
        suffix='_active' if active else ''
        mapping={'down':'bottom','up':'top','north':'front'+suffix,'south':'back','east':'side','west':'side','particle':'side'}
        write(f'models/block/{name}{suffix}.json',{'parent':'block/cube','textures':{face:f'cropmanager:blocks/{name}_{texture}' for face,texture in mapping.items()}})
        for facing,angle in [('north',0),('east',90),('south',180),('west',270)]:
            variants[f'active={str(active).lower()},facing={facing}']={'model':f'cropmanager:{name}{suffix}','y':angle,'uvlock':True}
    write(f'blockstates/{name}.json',{'variants':variants})
    write(f'models/item/{name}.json',{'parent':f'cropmanager:block/{name}'})
# New machines use the same native casing and palette; only their central fascia differs.
for name in ['electric_composter', 'agricultural_reservoir', 'agricultural_collector']:
    front=base.copy(); d=ImageDraw.Draw(front)
    dark=color((45,47,46,255)); steel=color((142,144,143,255)); edge=color((77,79,78,255))
    d.rectangle((3,3,12,12),fill=dark)
    if name=='electric_composter':
        d.rectangle((4,4,11,10),fill=color((91,68,39,255)))
        d.rectangle((5,5,10,7),fill=color((51,69,35,255)))
        d.line((5,9,10,9),fill=color((143,105,54,255)))
        d.line((5,11,10,11),fill=steel)
    elif name=='agricultural_reservoir':
        d.rectangle((4,4,6,11),fill=color((63,121,159,255)))
        d.rectangle((9,4,11,11),fill=color((145,132,58,255)))
        d.line((4,4,6,4),fill=steel); d.line((9,4,11,4),fill=steel)
        d.line((4,11,6,11),fill=steel); d.line((9,11,11,11),fill=steel)
    elif name=='agricultural_collector':
        d.rectangle((4,5,11,10),fill=edge)
        d.rectangle((5,6,10,9),fill=dark)
        d.line((4,11,11,11),fill=steel)
        for xx in (5,7,9): d.line((xx,6,xx,9),fill=steel)
    else:
        d.rectangle((4,4,11,9),fill=color((39,79,91,255)))
        d.rectangle((5,5,6,7),fill=color((91,140,67,255)))
        d.rectangle((9,5,10,7),fill=color((117,163,72,255)))
        d.line((4,11,11,11),fill=steel)
        d.point((7,10),fill=color((70,148,184,255)))
    top=base.copy(); dt=ImageDraw.Draw(top)
    dt.rectangle((4,4,11,11),fill=edge); dt.rectangle((5,5,10,10),fill=dark)
    if name=='seed_workbench':
        dt.line((7,5,7,10),fill=color((62,135,166,255)))
    else:
        for yy in (5,7,9): dt.line((5,yy,10,yy),fill=steel)
    frames={'front':front,'top':top,'back':base.copy(),'side':base.copy(),'bottom':base.copy()}
    if name!='seed_workbench':
        frames['back']=top.copy()
        one=front.copy(); ImageDraw.Draw(one).rectangle((6,1,9,2),fill=color((92,155,65,255)))
        two=one.copy(); ImageDraw.Draw(two).rectangle((6,1,9,2),fill=color((143,183,76,255)))
        stripActive=Image.new('RGBA',(16,32)); stripActive.paste(one,(0,0)); stripActive.paste(two,(0,16)); frames['front_active']=stripActive
    else: frames['front_active']=front.copy()
    for face,im in frames.items():
        im.save(assets/f'textures/blocks/{name}_{face}.png')
        if im.height==32: write(f'textures/blocks/{name}_{face}.png.mcmeta',{'animation':{'frametime':8,'interpolate':False}})
    variants={}
    for active in (False,True):
        suffix='_active' if active else ''
        mapping={'down':'bottom','up':'top','north':'front'+suffix,'south':'back','east':'side','west':'side','particle':'side'}
        write(f'models/block/{name}{suffix}.json',{'parent':'block/cube','textures':{face:f'cropmanager:blocks/{name}_{texture}' for face,texture in mapping.items()}})
        for facing,angle in [('north',0),('east',90),('south',180),('west',270)]: variants[f'active={str(active).lower()},facing={facing}']={'model':f'cropmanager:{name}{suffix}','y':angle,'uvlock':True}
    write(f'blockstates/{name}.json',{'variants':variants})
    write(f'models/item/{name}.json',{'parent':f'cropmanager:block/{name}'})
strip=Image.open(root/'docs/artwork/item-strip.png').convert('RGBA')
for column,name in [(1,'agricultural_probe')]:
    path=assets/f'textures/items/{name}.png'; path.parent.mkdir(parents=True,exist_ok=True)
    w,h=strip.size
    strip.crop((round(column*w/2),0,round((column+1)*w/2),h)).resize((16,16),Image.Resampling.NEAREST).save(path)
    write(f'models/item/{name}.json',{'parent':'item/generated','textures':{'layer0':f'cropmanager:items/{name}'}})
(root/'docs/artwork/ic2-palette.json').write_text(json.dumps({'sources':source_names,'colors':palette},indent=2)+'\n',encoding='utf-8')
# Enforce the native pixel palette and the unchanged IC2 casing border on every face/frame.
for path in (assets/'textures/blocks').glob('*.png'):
    im=Image.open(path).convert('RGBA')
    assert im.width==16 and im.height in (16,32),path
    assert set(im.getdata()) <= set(palette),path
    for frame in range(im.height//16):
        for y in range(16):
            for x in range(16):
                if x in (0,1,14,15) or y in (0,15): assert im.getpixel((x,y+frame*16))==base.getpixel((x,y)),(path,x,y)
print('Imported native IC2 casing, exact IC2 RGB palette, 16px faces and indicator-only animations.')
