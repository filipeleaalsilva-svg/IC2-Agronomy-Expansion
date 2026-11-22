from pathlib import Path
from PIL import Image, ImageDraw

OUT=Path('src/main/resources/assets/cropmanager/textures/blocks')
OUT.mkdir(parents=True,exist_ok=True)
P={
 'shadow':(28,34,38,255),'edge':(70,80,86,255),'steel':(112,122,128,255),
 'light':(176,184,188,255),'mid':(91,101,107,255),'dark':(43,50,55,255),
 'black':(17,22,25,255),'blue':(45,126,164,255),'blue_hi':(97,182,207,255),
 'copper':(148,91,42,255),'gold':(200,157,65,255),'white':(221,225,224,255),
 'green':(50,150,70,255),'green_hi':(120,220,86,255),'water':(42,130,191,255),
 'water_hi':(115,193,228,255),'liquid':(90,151,74,255)
}
def canvas(): return Image.new('RGBA',(16,16),P['shadow'])
def frame(d):
 d.rectangle((0,0,15,15),fill=P['shadow']); d.rectangle((1,1,14,14),fill=P['steel'])
 d.line((1,1,14,1),fill=P['light']); d.line((1,1,1,14),fill=P['light'])
 d.line((14,1,14,14),fill=P['dark']); d.line((1,14,14,14),fill=P['dark'])
 d.rectangle((2,2,13,13),fill=P['mid'])
 for x,y in ((2,2),(13,2),(2,13),(13,13)):
  d.point((x,y),fill=P['white']); d.point((x+(-1 if x>8 else 1),y),fill=P['edge'])
def save(name,img): img.save(OUT/(name+'.png'),optimize=True)
def side(name,upper=False):
 im=canvas();d=ImageDraw.Draw(im);frame(d)
 d.rectangle((3,3,12,11),fill=P['dark'])
 for y in (4,6,8,10):
  d.rectangle((4,y,11,y+1),fill=P['black']);d.line((4,y,10,y),fill=P['edge'])
 d.rectangle((2,12,13,13),fill=P['copper'] if upper else P['gold']);d.line((3,12,12,12),fill=P['light'])
 if upper:
  d.rectangle((4,3,11,3),fill=P['blue']);d.line((4,3,10,3),fill=P['blue_hi'])
 save(name,im)
def top(name,extractor=False):
 im=canvas();d=ImageDraw.Draw(im);frame(d)
 d.rectangle((3,3,12,12),fill=P['dark']);d.rectangle((4,4,11,11),fill=P['steel'])
 if extractor:
  d.rectangle((6,3,9,12),fill=P['copper']);d.rectangle((7,4,8,11),fill=P['gold'])
 else:
  d.rectangle((5,5,10,10),fill=P['black']);d.rectangle((6,6,9,9),fill=P['edge']);d.point((7,7),fill=P['gold'])
 save(name,im)
def grinder_front(name,upper=False,active=False):
 im=canvas();d=ImageDraw.Draw(im);frame(d)
 if upper:
  # Feed hopper: a visible IC2-style metal funnel above the grinding chamber.
  d.rectangle((4,3,11,5),fill=P['black']);d.line((4,3,11,3),fill=P['light'])
  d.rectangle((5,6,10,7),fill=P['gold']);d.rectangle((6,8,9,11),fill=P['dark'])
  d.rectangle((7,9,8,12),fill=P['black']);d.line((4,12,11,12),fill=P['blue'])
  d.rectangle((6,13,9,14),fill=P['steel'])
  led=P['green_hi'] if active else P['blue_hi'];d.point((12,4),fill=led)
 else:
  d.rectangle((3,3,12,11),fill=P['black']);d.rectangle((4,4,11,10),fill=P['edge'])
  d.ellipse((4,3,11,10),fill=P['dark'],outline=P['light']);d.ellipse((5,4,10,9),fill=P['steel'],outline=P['shadow'])
  d.line((6,5,9,8),fill=P['black']);d.line((9,5,6,8),fill=P['black'])
  d.rectangle((7,6,8,7),fill=P['gold']);d.rectangle((4,12,11,13),fill=P['blue'])
  led=P['green_hi'] if active else P['blue_hi'];d.point((12,4),fill=led)
 save(name,im)
def extractor_front(name,upper=False,active=False):
 im=canvas();d=ImageDraw.Draw(im);frame(d)
 if upper:
  # Tall vessel window and heat inlet.
  d.rectangle((4,2,11,13),fill=P['black']);d.rectangle((5,3,10,12),fill=P['blue'])
  d.rectangle((6,4,7,11),fill=P['water']);d.line((5,3,10,3),fill=P['water_hi'])
  d.rectangle((4,2,11,3),fill=P['copper']);d.line((5,2,10,2),fill=P['gold'])
  d.rectangle((6,13,9,14),fill=P['steel']);d.point((12,4),fill=P['green_hi'] if active else P['blue_hi'])
 else:
  d.rectangle((3,3,12,11),fill=P['dark']);d.rectangle((4,4,11,10),fill=P['black'])
  d.rectangle((5,4,10,8),fill=P['water']);d.line((5,4,10,4),fill=P['water_hi'])
  d.rectangle((5,9,10,10),fill=P['liquid']);d.rectangle((4,12,11,13),fill=P['copper'])
  d.rectangle((7,11,8,14),fill=P['gold']);d.point((12,4),fill=P['green_hi'] if active else P['blue_hi'])
 save(name,im)
for machine,fn in [('grin_grinder',grinder_front),('grin_extractor',extractor_front)]:
 for upper in (False,True):
  for active in (False,True):
   suffix=('upper' if upper else 'lower')+('_active' if active else '')+'_front'
   fn(machine+'_'+suffix,upper,active)
 for part in ('lower','upper'):
  side(machine+'_'+part+'_side',part=='upper')
 top(machine+'_top',machine=='grin_extractor')
 bottom=canvas(); db=ImageDraw.Draw(bottom);frame(db);db.rectangle((4,4,11,11),fill=P['dark']);db.rectangle((6,6,9,9),fill=P['edge']);save(machine+'_bottom',bottom)
# Plain straight heat pipe: matte steel tube with restrained copper collars and an IC2 blue heat marker.
im=Image.new('RGBA',(16,16),P['dark']);d=ImageDraw.Draw(im)
d.rectangle((0,0,15,15),fill=P['shadow']);d.rectangle((2,2,13,13),fill=P['steel']);d.rectangle((4,4,11,11),fill=P['mid'])
d.rectangle((5,0,10,15),fill=P['edge']);d.rectangle((6,0,9,15),fill=P['steel']);d.line((6,0,6,15),fill=P['light'])
d.rectangle((5,1,10,2),fill=P['copper']);d.rectangle((5,13,10,14),fill=P['copper'])
d.rectangle((6,1,9,1),fill=P['gold']);d.rectangle((6,14,9,14),fill=P['gold'])
d.rectangle((7,4,8,11),fill=P['blue']);d.line((7,5,7,10),fill=P['blue_hi'])
save('thermal_pipe',im)
im=Image.new('RGBA',(16,16),P['shadow']);d=ImageDraw.Draw(im);d.rectangle((1,1,14,14),fill=P['copper']);d.rectangle((2,2,13,13),fill=P['gold']);d.rectangle((3,3,12,12),fill=P['edge']);d.rectangle((4,4,11,11),fill=P['dark']);d.rectangle((6,6,9,9),fill=P['steel']);d.point((7,7),fill=P['blue_hi']);save('thermal_pipe_collar',im)
