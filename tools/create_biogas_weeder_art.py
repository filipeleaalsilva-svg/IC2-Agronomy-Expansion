"""Build the unregistered biogas weeder art package and preview its actual JSON model.

The AI material atlas is retained as source. All distributable textures are
quantized to the project's audited IC2 palette and are exactly 16x16 RGBA.
The preview rasterizer reads the resulting Minecraft model/texture files.
"""
from pathlib import Path
from io import BytesIO
from zipfile import ZipFile, ZIP_DEFLATED
import json, math, hashlib
import numpy as np
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / 'docs/artwork/biogas-weeder'
ASSETS = ROOT / 'src/main/resources/assets/cropmanager'
TEXTURES = ASSETS / 'textures/blocks/biogas_weeder'
TEXTURES.mkdir(parents=True, exist_ok=True)
palette = [tuple(v) for v in json.loads((ROOT/'docs/artwork/ic2-palette.json').read_text(encoding='utf-8'))['colors']]
cache = {}
def nearest(p):
    p=tuple(p)
    if p not in cache: cache[p] = min(palette,key=lambda q:sum((p[i]-q[i])**2 for i in range(3)))
    return cache[p]
def quantize(im):
    result=Image.new('RGBA', im.size);result.putdata([nearest(p) for p in im.getdata()]);return result
atlas=Image.open(ART/'generated-material-atlas.png').convert('RGBA')
with ZipFile(ROOT/'libs/industrialcraft-2-2.8.222-ex112.jar') as z:
    casing=Image.open(BytesIO(z.read('assets/ic2/textures/blocks/machine/processing/basic/macerator_leftrightback.png'))).convert('RGBA').resize((16,16),Image.Resampling.NEAREST)
names=['chassis_top','chassis_side','cutter','motor','tank_side','tank_end','mechanics','underside']
for i,name in enumerate(names):
    col,row=i%4,i//4; w,h=atlas.size
    im=quantize(atlas.crop((round(col*w/4),round(row*h/2),round((col+1)*w/4),round((row+1)*h/2))).resize((16,16),Image.Resampling.NEAREST))
    if name in ('chassis_top','chassis_side'):
        native=casing.copy();native.paste(im.crop((2,2,14,14)),(2,2));im=native
    im.save(TEXTURES/(name+'.png'))
Image.new('RGBA',(16,16),nearest((182,187,191,255))).save(TEXTURES/'plain.png')

# Elements use only vanilla 1.12.2 cuboids and supported Y rotations.
# Coplanar octagon caps use one flat material to avoid UV seams at overlaps.
elements=[]
def box(name,lo,hi,texture,faces=None,rotation=None):
    fs={}
    for face in ['down','up','north','south','west','east']:
        if faces is not None and face not in faces:continue
        tex=texture.get(face,texture.get('all','plain')) if isinstance(texture,dict) else texture
        uv=[0,0,16,16]
        if isinstance(tex,tuple):tex,uv=tex
        fs[face]={'uv':uv,'texture':'#'+tex}
    el={'name':name,'from':lo,'to':hi,'faces':fs}
    if rotation:el['rotation']=rotation
    elements.append(el)
def octagon(name,cx,cz,half,y0,y1,texture,cap='plain',gauge=False):
    b=1.0
    box(name+'_core',[cx-half+b,y0,cz-half],[cx+half-b,y1,cz+half],{'north':texture if gauge else (texture,[0,0,6,16]),'south':(texture,[0,0,6,16]),'up':cap,'down':cap},['north','south','up','down'])
    box(name+'_left',[cx-half,y0,cz-half+b],[cx-half+b,y1,cz+half-b],{'west':(texture,[0,0,6,16]),'up':cap,'down':cap},['west','up','down'])
    box(name+'_right',[cx+half-b,y0,cz-half+b],[cx+half,y1,cz+half-b],{'east':(texture,[0,0,6,16]),'up':cap,'down':cap},['east','up','down'])
    a=math.sqrt(0.5)
    for label,x,z in [('nw',cx-half+b,cz-half+b),('ne',cx+half-b,cz-half+b),('sw',cx-half+b,cz+half-b),('se',cx+half-b,cz+half-b)]:
        box(name+'_'+label,[x-a,y0,z-a],[x+a,y1,z+a],{'all':(texture,[0,0,6,16]),'up':cap,'down':cap},rotation={'origin':[x,(y0+y1)/2,z],'axis':'y','angle':45,'rescale':False})

for x in [3,11]:
    for z in [3,11]:box('foot_%d_%d'%(x,z),[x,0,z],[x+2,1,z+2],'underside')
octagon('low_chassis',8,8,6,1,4,'chassis_side')
box('motor',[4,4,3],[12,8,9],{'north':'motor','south':'chassis_side','east':'chassis_side','west':'chassis_side','up':'chassis_top','down':'underside'})
box('front_cutter',[3,1.5,0],[13,4,3],{'north':'cutter','up':'underside','all':'mechanics'})
for x in [4,7.5,11]:box('cutting_tooth_'+str(x),[x,0.5,0],[x+1,2.25,2],'plain')
box('left_guard',[2,1,1],[3,4,6],'chassis_side')
box('right_guard',[13,1,1],[14,4,6],'chassis_side')
octagon('gas_tank',8,12,3,4,13,'tank_side',gauge=True)
box('tank_cap_plate',[6,13,10],[10,13.125,14],{'up':'tank_end','all':'plain'})
box('tank_valve',[7,13,11],[9,14,13],'mechanics')
box('valve_handle',[6.5,14,11.5],[9.5,14.5,12.5],'plain')
box('gas_pipe_left',[4,5,8],[5,6,12],'mechanics')
box('gas_pipe_cross',[4,5,8],[7,6,9],'mechanics')
box('exhaust',[11,8,7],[12,11,8],'mechanics')
box('exhaust_cap',[10.75,11,6.75],[12.25,11.5,8.25],'underside')
textures={n:'cropmanager:blocks/biogas_weeder/'+n for n in names+['plain']}
textures['particle']=textures['chassis_side']
model={'credit':'Crop Manager addon - unregistered art preview, no gameplay implementation','ambientocclusion':True,'textures':textures,'elements':elements,'display':{'gui':{'rotation':[30,225,0],'translation':[0,-1,0],'scale':[0.85,0.85,0.85]},'ground':{'rotation':[0,0,0],'translation':[0,2,0],'scale':[0.5,0.5,0.5]},'fixed':{'rotation':[0,180,0],'translation':[0,0,0],'scale':[0.8,0.8,0.8]}}}
model_path=ASSETS/'models/block/biogas_weeder.json';model_path.parent.mkdir(parents=True,exist_ok=True);model_path.write_text(json.dumps(model,indent=2)+'\n',encoding='utf-8')
item_path=ASSETS/'models/item/biogas_weeder.json';item_path.write_text(json.dumps({'parent':'cropmanager:block/biogas_weeder'},indent=2)+'\n',encoding='utf-8')

# Software orthographic model render: triangle z-buffer and nearest texels.
model=json.loads(model_path.read_text(encoding='utf-8'))
loaded={k:np.asarray(Image.open(ASSETS/('textures/'+v.split(':',1)[1]+'.png')).convert('RGBA')) for k,v in model['textures'].items()}
face_points={
 'down':lambda a,b:[[a[0],a[1],a[2]],[b[0],a[1],a[2]],[b[0],a[1],b[2]],[a[0],a[1],b[2]]],
 'up':lambda a,b:[[a[0],b[1],b[2]],[b[0],b[1],b[2]],[b[0],b[1],a[2]],[a[0],b[1],a[2]]],
 'north':lambda a,b:[[b[0],a[1],a[2]],[a[0],a[1],a[2]],[a[0],b[1],a[2]],[b[0],b[1],a[2]]],
 'south':lambda a,b:[[a[0],a[1],b[2]],[b[0],a[1],b[2]],[b[0],b[1],b[2]],[a[0],b[1],b[2]]],
 'west':lambda a,b:[[a[0],a[1],a[2]],[a[0],a[1],b[2]],[a[0],b[1],b[2]],[a[0],b[1],a[2]]],
 'east':lambda a,b:[[b[0],a[1],b[2]],[b[0],a[1],a[2]],[b[0],b[1],a[2]],[b[0],b[1],b[2]]]}
def render(camera=(1,0.8,-1.4),size=180):
    cam=np.array(camera,dtype=float);cam/=np.linalg.norm(cam)
    right=np.cross(cam,[0,1,0]);right/=np.linalg.norm(right);up=np.cross(right,cam)
    quads=[]
    for el in model['elements']:
        for side,desc in el['faces'].items():
            pts=np.array(face_points[side](el['from'],el['to']),float)
            if 'rotation'in el:
                r=el['rotation'];o=np.array(r['origin']);ang=math.radians(r['angle']);c,s=math.cos(ang),math.sin(ang);matrix=np.array([[c,0,s],[0,1,0],[-s,0,c]])
                pts=(pts-o)@matrix.T+o
            normal=np.cross(pts[1]-pts[0],pts[2]-pts[0]);normal/=np.linalg.norm(normal)
            if normal@cam<=0:continue
            points=np.stack((pts@right,-pts@up,pts@cam),axis=1)
            u0,v0,u1,v1=desc['uv'];uv=np.array([[u0,v1],[u1,v1],[u1,v0],[u0,v0]],float)
            brightness=0.68+0.32*max(0,normal@np.array([0.3,0.88,-0.36]))
            quads.append([points,uv,desc['texture'][1:],brightness])
    allp=np.concatenate([q[0][:,:2] for q in quads]);mn=allp.min(axis=0);mx=allp.max(axis=0)
    scale=(size-20)/max(mx-mn);offset=(size-(mx-mn)*scale)/2-mn*scale
    out=np.zeros((size,size,4),dtype=np.uint8);depth=np.full((size,size),-1e20)
    for points,uv,name,brightness in quads:
        points=points.copy();points[:,:2]=points[:,:2]*scale+offset
        for tri in [(0,1,2),(0,2,3)]:
            v=points[list(tri)];t=uv[list(tri)]
            x0=max(0,int(math.floor(v[:,0].min())));x1=min(size-1,int(math.ceil(v[:,0].max())))
            y0=max(0,int(math.floor(v[:,1].min())));y1=min(size-1,int(math.ceil(v[:,1].max())))
            xx,yy=np.meshgrid(np.arange(x0,x1+1)+0.5,np.arange(y0,y1+1)+0.5)
            den=(v[1,1]-v[2,1])*(v[0,0]-v[2,0])+(v[2,0]-v[1,0])*(v[0,1]-v[2,1])
            if abs(den)<1e-9:continue
            w0=((v[1,1]-v[2,1])*(xx-v[2,0])+(v[2,0]-v[1,0])*(yy-v[2,1]))/den
            w1=((v[2,1]-v[0,1])*(xx-v[2,0])+(v[0,0]-v[2,0])*(yy-v[2,1]))/den;w2=1-w0-w1
            z=w0*v[0,2]+w1*v[1,2]+w2*v[2,2];mask=(w0>=-1e-8)&(w1>=-1e-8)&(w2>=-1e-8)&(z>depth[y0:y1+1,x0:x1+1]+1e-8)
            tx=np.clip((w0*t[0,0]+w1*t[1,0]+w2*t[2,0]).astype(int),0,15);ty=np.clip((w0*t[0,1]+w1*t[1,1]+w2*t[2,1]).astype(int),0,15)
            pixels=loaded[name][ty,tx].copy();pixels[:,:,:3]=(pixels[:,:,:3]*brightness).astype(np.uint8);mask &=pixels[:,:,3]>0
            block=out[y0:y1+1,x0:x1+1];block[mask]=pixels[mask];depth[y0:y1+1,x0:x1+1][mask]=z[mask]
    return Image.fromarray(out,'RGBA')

font_path='C:/Windows/Fonts/arial.ttf'
font=ImageFont.truetype(font_path,24);small=ImageFont.truetype(font_path,18);heading=ImageFont.truetype(font_path,34)
preview=Image.new('RGBA',(1280,860),(35,40,44,255));d=ImageDraw.Draw(preview)
d.text((32,22),'Capinador a Biogás',font=heading,fill='#edf0ed')
d.text((32,68),'Modelo de arte • texturas reais de 16×16 • paleta IC2',font=font,fill='#bac6c7')
main=render(size=200).resize((680,680),Image.Resampling.NEAREST);preview.alpha_composite(main,(0,118))
front=render((0.08,0.55,-1.6),size=160).resize((320,320),Image.Resampling.NEAREST);preview.alpha_composite(front,(740,102));d.text((780,420),'Frente / cabeçote',font=font,fill='#e3e8e5')
rear=render((-1,0.85,1.4),size=160).resize((320,320),Image.Resampling.NEAREST);preview.alpha_composite(rear,(740,458));d.text((780,780),'Traseira / tanque',font=font,fill='#e3e8e5')
d.text((32,820),'Prévia do JSON e PNGs finais; bloco ainda não registrado no jogo.',font=small,fill='#b7bfc1');preview.convert('RGB').save(ART/'model-preview.png')
render(size=128).save(ART/'inventory-render.png')
sheet=Image.new('RGB',(1000,1040),'#23282c');d=ImageDraw.Draw(sheet)
d.text((24,16),'Faces finais — cada PNG tem 16×16 pixels',font=heading,fill='#edf0ed')
labels={'chassis_top':'Tampa / chassi','chassis_side':'Lateral / chassi','cutter':'Cabeçote frontal','motor':'Motor / indicador','tank_side':'Cilindro / lateral','tank_end':'Cilindro / tampa','mechanics':'Mecânica / tubulação','underside':'Parte inferior','plain':'Metal de junção'}
for i,name in enumerate(names+['plain']):
 x=28+(i%3)*326;y=78+(i//3)*318
 im=Image.open(TEXTURES/(name+'.png'));sheet.paste(im.resize((256,256),Image.Resampling.NEAREST).convert('RGB'),(x,y));d.text((x,y+264),labels[name],font=small,fill='#e3e8e5');d.text((x,y+287),name+'.png',font=small,fill='#a4b4b9')
sheet.save(ART/'texture-sheet.png')
# Validation is performed on the final production assets, never on enlarged previews.
report={'texture_count':len(names)+1,'texture_size':[16,16],'model_elements':len(elements),'registered':False,'source':'built-in image_gen atlas + native IC2 casing/palette','textures':{}}
for path in sorted(TEXTURES.glob('*.png')):
 im=Image.open(path);assert im.mode=='RGBA' and im.size==(16,16),path
 assert set(im.getdata())<=set(palette),path
 report['textures'][path.name]={'size':list(im.size),'sha256':hashlib.sha256(path.read_bytes()).hexdigest()}
for el in model['elements']:
 assert all(0<=v<=16 for v in el['from']+el['to']),el['name']
 if 'rotation'in el:assert el['rotation']['angle']in(-45,-22.5,0,22.5,45)
 for face in el['faces'].values():assert face['texture'][1:]in model['textures']
(ART/'validation.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print(json.dumps(report,ensure_ascii=True))
