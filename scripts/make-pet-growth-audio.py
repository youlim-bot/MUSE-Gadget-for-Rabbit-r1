"""Project-composed growth music and effects; no songs, samples or external services."""
from pathlib import Path
import wave
import numpy as np
RATE=22050
OUT=Path(__file__).resolve().parents[1]/'app/src/main/res/raw'
OUT.mkdir(parents=True,exist_ok=True)
def write(name,b):
 peak=max(.6,float(np.max(np.abs(b))))
 b=b/peak*.58
 fade=np.minimum(1,np.minimum(np.arange(len(b)),np.arange(len(b))[::-1])/(RATE*.025))
 with wave.open(str(OUT/(name+'.wav')),'wb') as f:
  f.setparams((1,2,RATE,0,'NONE','not compressed'));f.writeframes((b*fade*32767).astype('<i2').tobytes())
def tone(b,at,midi,dur,amp=.18,kind=0):
 t=np.arange(int(dur*RATE))/RATE;freq=440*2**((midi-69)/12)
 env=(1-np.exp(-t*65))*np.exp(-t*(3 if kind!=1 else 1.5)/dur)
 harmonics=np.sin(2*np.pi*freq*t)+(.18 if kind==0 else .07)*np.sin(2*np.pi*freq*2*t)
 if kind==2:harmonics+=.10*np.sin(2*np.pi*freq*3*t)
 sound=harmonics*env*amp;i=int(at*RATE);n=min(len(sound),len(b)-i)
 if n>0:b[i:i+n]+=sound[:n]
def chirp(b,at,freq=1800):
 t=np.arange(int(.3*RATE))/RATE;phase=np.cumsum(freq+700*np.sin(np.pi*t/.3))/RATE
 i=int(at*RATE);v=np.sin(2*np.pi*phase)*np.sin(np.pi*t/.3)**2*.06;n=min(len(v),len(b)-i)
 if n>0:b[i:i+n]+=v[:n]
tunes={
 'lullaby':([72,76,79,76,74,77,81,77,72,79,76,74,71,74,76,72],1.5,0),
 'sprout':([72,74,76,79,76,74,72,67,69,72,74,76,74,72,69,67],1.5,1),
 'sunny':([72,79,76,79,74,81,77,81,76,79,84,79,74,77,79,72]*2,.75,2),
 'meadow':([60,64,67,72,62,65,69,74,64,67,71,76,60,67,64,60],1.5,1),
 'starlight':([72,79,83,79,74,81,84,81,76,83,86,83,72,79,76,72],1.5,0)
}
for name,(notes,step,kind) in tunes.items():
 b=np.zeros(RATE*24)
 for i,n in enumerate(notes):tone(b,i*step,n,step*1.5,.16,kind)
 for i,n in enumerate([48,53,55,48]):tone(b,i*6,n,5.8,.045,1)
 if name in ('sprout','meadow'):
  for at in (4.3,10.4,16.1,22.2):chirp(b,at)
 write('pet_'+name,b)
for name,notes in {'hatch':[72,76,79,84],'plant':[60,67],'bloom':[72,79,84,88],'learn':[67,72,76,79],'catch':[79,84,91]}.items():
 b=np.zeros(int(RATE*1.6))
 for i,n in enumerate(notes):tone(b,.06+i*.19,n,.62,.28)
 if name=='hatch':chirp(b,.9,2100)
 write('pet_'+name,b)
rng=np.random.default_rng(81008);b=np.zeros(RATE)
for i in range(5):
 at=.08+i*.13;t=np.arange(int(.18*RATE))/RATE
 freq=650+1100*t/.18;v=np.sin(2*np.pi*np.cumsum(freq)/RATE)*np.exp(-t*18)*.16
 j=int(at*RATE);b[j:j+len(v)]+=v
write('pet_water',b)
print('Created five original growth loops and six effects')
