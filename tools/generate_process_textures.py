import argparse
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


MIXER = 'weed_ex_fabricator'
GRINDER = 'grin_grinder'
EXTRACTOR = 'grin_extractor'
ACCENTS = {MIXER: '#B9A247', GRINDER: '#76A74B', EXTRACTOR: '#945B2A'}


def chassis():
    image = canvas()
    draw = ImageDraw.Draw(image)
    frame(draw)
    draw.line((2, 12, 13, 12), fill=P['edge'])
    draw.line((3, 13, 12, 13), fill=P['steel'])
    return image, draw


def panel(draw, bounds, inside):
    left, top, right, bottom = bounds
    draw.rectangle(bounds, fill=P['dark'], outline=P['shadow'])
    draw.line((left + 1, top, right - 1, top), fill=P['light'])
    draw.rectangle((left + 1, top + 1, right - 1, bottom - 1), fill=inside)


def port(draw, x, y, color):
    draw.rectangle((x, y, x + 3, y + 3), fill=P['shadow'])
    draw.line((x, y, x + 2, y), fill=color)
    draw.rectangle((x + 1, y + 1, x + 2, y + 2), fill=P['black'])


def machine_face(machine, face):
    image, draw = chassis()
    accent = ACCENTS[machine]
    if face == 'top':
        panel(draw, (3, 3, 12, 12), P['steel'])
        if machine == GRINDER:
            draw.polygon([(4, 5), (11, 5), (9, 10), (6, 10)], fill=P['edge'])
            draw.rectangle((6, 6, 9, 8), fill=P['black'])
            draw.line((5, 5, 10, 5), fill=P['light'])
        else:
            port(draw, 5, 5, accent)
            draw.line((5, 10, 10, 10), fill=P['edge'])
            draw.point((10, 6), fill=P['light'])
    elif face == 'bottom':
        panel(draw, (4, 4, 11, 11), P['edge'])
        for x in (3, 10):
            for y in (3, 10):
                draw.rectangle((x, y, x + 2, y + 2), fill=P['dark'])
                draw.point((x, y), fill=P['light'])
    elif face == 'back':
        panel(draw, (3, 4, 12, 11), P['edge'])
        port(draw, 4, 6, P['water_hi'] if machine == MIXER else P['light'])
        port(draw, 8, 6, accent)
    else:
        panel(draw, (3, 4, 12, 11), P['dark'])
        for y in (5, 7, 9):
            draw.line((4, y, 10, y), fill=P['black'])
            draw.point((4, y + 1), fill=P['steel'])
        draw.line((11, 5, 11, 10), fill=accent)
    return image


def machine_front(machine, phase=None):
    image, draw = chassis()
    active = phase is not None
    draw.line((4, 3, 10, 3), fill=ACCENTS[machine])
    draw.point((12, 3), fill=P['green_hi'] if active else P['dark'])

    if machine == GRINDER:
        # A feed chute over two interlocking toothed rollers.
        panel(draw, (3, 5, 12, 12), P['black'])
        draw.polygon([(4, 6), (11, 6), (9, 8), (6, 8)], fill=P['edge'])
        draw.line((5, 6, 10, 6), fill=P['steel'])
        tooth_offset = phase if active else 0
        for x in range(4, 12):
            tooth = (x + tooth_offset) % 4 < 2
            draw.point((x, 9), fill=P['light'] if tooth else P['edge'])
            draw.point((x, 10), fill=P['edge'] if tooth else P['light'])
        draw.line((6, 12, 9, 12), fill=ACCENTS[machine])
    elif machine == EXTRACTOR:
        # Copper jacket, sight glass and a warm coil beneath the vessel.
        panel(draw, (4, 4, 11, 11), P['black'])
        draw.line((4, 5, 4, 10), fill=P['copper'])
        draw.line((11, 5, 11, 10), fill=P['copper'])
        draw.rectangle((5, 8, 10, 10), fill=P['liquid'] if active else '#365943')
        draw.line((5, 8, 10, 8), fill='#83AA55' if active else '#54724E')
        if active:
            for x, y in (((6, 9), (8, 7)), ((7, 8), (9, 6)),
                         ((8, 9), (6, 6)), ((9, 8), (7, 5)))[phase]:
                draw.point((x, y), fill='#B6CE82')
        for x in (5, 7, 9):
            draw.line((x, 12, x + 1, 12), fill=P['gold'] if active else P['copper'])
    else:
        # Two feeds meet above the amber batch vessel.
        panel(draw, (3, 5, 12, 12), P['black'])
        for x, color in ((5, P['water']), (10, P['liquid'])):
            draw.line((x, 5, x, 7), fill=color)
            draw.line((x, 7, 7 if x == 5 else 8, 7), fill=color)
        draw.rectangle((5, 8, 10, 11), fill='#DDCC55' if active else '#766532')
        draw.line((5, 8, 10, 8), fill=ACCENTS[machine])
        draw.line((7, 7, 7, 10), fill=P['light'])
        draw.line((6, 10, 9, 10), fill=P['edge'])
        if active:
            draw.point((9, 9), fill='#FFF0A0')
            draw.point((6, 11), fill='#FFF0A0')
    return image


def generate_machine(machine):
    prefix = machine + ('/' if machine == MIXER else '_')
    for face in ('top', 'bottom', 'side', 'back'):
        save(prefix + face, machine_face(machine, face))
    save(prefix + 'front', machine_front(machine))
    if machine == MIXER:
        save(prefix + 'front_active', machine_front(machine, 0))
    else:
        strip = Image.new('RGBA', (16, 64))
        for phase in range(4):
            strip.paste(machine_front(machine, phase), (0, 16 * phase))
        save(prefix + 'front_active', strip)


parser = argparse.ArgumentParser(description='Generate Crop Manager machine textures.')
parser.add_argument('--machine', choices=ACCENTS, help='Generate only one machine.')
args = parser.parse_args()
if args.machine:
    generate_machine(args.machine)
    raise SystemExit

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
for machine in ACCENTS:
    generate_machine(machine)
