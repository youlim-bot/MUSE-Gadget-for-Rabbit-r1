#!/usr/bin/env python3
import subprocess,time,xml.etree.ElementTree as E,re,os,argparse
from pathlib import Path
adb=[os.environ.get('ADB','adb'),'-d'];pkg='dev.cameronpak.muser1.demo'
parser=argparse.ArgumentParser(description='Capture only the separate offline demo app on one authorized USB device.')
parser.add_argument('--output',default=str(Path(__file__).resolve().parents[1]/'docs/images'))
out=Path(parser.parse_args().output);out.mkdir(parents=True,exist_ok=True)
def shell(*a):return subprocess.check_output(adb+['shell',*a],timeout=25)
def nodes():
 for attempt in range(3):
  shell('input','keyevent','224')
  shell('uiautomator','dump','/data/local/tmp/muse-demo.xml')
  result=subprocess.run(adb+['shell','cat','/data/local/tmp/muse-demo.xml'],capture_output=True,timeout=25)
  shell('rm','-f','/data/local/tmp/muse-demo.xml')
  if result.returncode==0:
   rows=list(E.fromstring(result.stdout).iter('node'))
   if any(n.get('package')==pkg for n in rows):return rows
  time.sleep(.5)
 raise RuntimeError('Demo is not foreground; refusing capture')
def tap(label):
 n=None
 for _ in range(4):
  n=next((n for n in nodes() if n.get('text')==label),None)
  if n is not None:break
  time.sleep(.5)
 assert n is not None,label
 x,y,z,w=map(int,re.findall(r'\d+',n.get('bounds')));shell('input','tap',str((x+z)//2),str((y+w)//2));time.sleep(.5)
def capture(name):
 nodes()
 time.sleep(.7)
 image=subprocess.check_output(adb+['exec-out','screencap','-p'],timeout=20)
 assert image.startswith(b'\x89PNG')
 (out/(name+'.png')).write_bytes(image)
 print('Saved',name,flush=True)
for lang,scene in [('EN','chat'),('KO','chat'),('JA','chat'),('EN','translate'),('KO','translate'),('EN','photo'),('EN','languages'),('EN','tasks'),('KO','tasks'),('EN','settings')]:
 shell('input','keyevent','224')
 shell('am','force-stop',pkg)
 shell('am','start','-n',pkg+'/dev.cameronpak.muser1.MainActivity','--es','demo_language',lang,'--es','demo_scene',scene)
 time.sleep(1)
 if scene=='tasks':tap('Weekend checklist' if lang=='EN' else '주말 할 일')
 capture(lang.lower()+'-'+scene)
 if scene=='settings':tap('Voice speed');capture('en-voice-speed')
print('All captures contain offline fixture data only.',flush=True)
