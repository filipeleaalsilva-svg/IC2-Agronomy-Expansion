"""Inspect the current distributable and all registered assets without launching Minecraft."""
import hashlib,json,struct,zipfile
from pathlib import Path
project=Path(__file__).resolve().parents[1]
artifact=project/'build/libs/CropManager-0.5.0-alpha8.jar'
resources=project/'src/main/resources'
active={'advanced_crop_manager','agricultural_monitor','electric_composter','biogas_weeder','weed_ex_fabricator','grin_grinder','grin_extractor','thermal_pipe'}
retired={'crop_manager','seed_classifier','seed_workbench','configuration_card','agricultural_notebook','compost'}
with zipfile.ZipFile(artifact) as jar:
 names=set(jar.namelist());classes=[n for n in names if n.endswith('.class')]
 assert classes and all(n.startswith('br/cropmanager/') for n in classes)
 assert not any('RuntimeChecks' in n or 'ClientChecks' in n for n in classes)
 for name in classes:assert struct.unpack('>H',jar.read(name)[6:8])[0]==52,name
 metadata=json.loads(jar.read('mcmod.info'));assert metadata[0]['version']=='0.5.0-alpha8'
 assert 'assets/cropmanager/models/item/wireless_eu_upgrade.json' in names
 assert 'assets/cropmanager/textures/items/wireless_eu_upgrade.png' in names
 states={Path(n).stem for n in names if n.startswith('assets/cropmanager/blockstates/') and n.endswith('.json')};assert states==active,states
 for block in active:
  data=json.loads(jar.read(f'assets/cropmanager/blockstates/{block}.json'));assert len(data['variants'])==8,block
  for variant in data['variants'].values():assert 'assets/cropmanager/models/block/'+variant['model'].split(':')[1]+'.json' in names
 for block in retired:
  assert f'assets/cropmanager/models/item/{block}.json' not in names,block
 modelpaths=[n for n in names if n.startswith('assets/cropmanager/models/') and n.endswith('.json')]
 for path in modelpaths:
  data=json.loads(jar.read(path));parent=data.get('parent','')
  if parent.startswith('cropmanager:'):assert 'assets/cropmanager/models/'+parent.split(':')[1]+'.json' in names,parent
  for texture in data.get('textures',{}).values():
   if texture.startswith('cropmanager:'):assert 'assets/cropmanager/textures/'+texture.split(':')[1]+'.png' in names,texture
  for part in data.get('elements',[]):
   assert all(0<=v<=16 for v in part['from']+part['to']),path
   if 'rotation' in part:assert part['rotation']['angle'] in (-45,-22.5,0,22.5,45),path
   for face in part['faces'].values():assert face['texture'].lstrip('#') in data['textures'],path
 sprites=[n for n in names if n.startswith('assets/cropmanager/textures/') and n.endswith('.png')]
 for name in sprites:
  png=jar.read(name);w,h,bits,mode=struct.unpack('>IIBB',png[16:26]);gui='/textures/gui/' in name
  assert (w==512 and h==512) if gui else (w==16 and h in (16,32)),name
  assert (bits,mode)==(8,6),name
  assert png==(resources/name).read_bytes(),name
  if h==32:assert json.loads(jar.read(name+'.mcmeta'))['animation']['frametime']>0
 locales={}
 for lang in ('en_us','pt_br'):
  rows=[s.split('=',1) for s in jar.read('assets/cropmanager/lang/'+lang+'.lang').decode('utf-8').splitlines() if '=' in s and not s.lstrip().startswith('#')]
  locales[lang]=dict(rows);assert len(rows)==len(locales[lang]),lang
  assert all(value.strip() and '\ufffd' not in value for value in locales[lang].values()),lang
 assert locales['en_us'].keys()==locales['pt_br'].keys()
 print('Verified',len(classes),'Java 8 classes;',len(states),'active blockstates;',len(modelpaths),'models;',len(sprites),'native 16px sprites;',len(locales['en_us']),'matching translations.')
 print('Retired block/item assets absent; process-machine geometry and texture references resolved.')
print('SHA256',hashlib.sha256(artifact.read_bytes()).hexdigest())
