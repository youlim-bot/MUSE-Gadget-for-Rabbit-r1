"""Original synthesized pet ambience and cues. No samples or third-party recordings.
Run with Python + NumPy to deterministically reproduce the bundled WAV assets.
"""
from pathlib import Path
import wave
import numpy as np
RATE=22050
OUT=Path(__file__).resolve().parents[1]/'app/src/main/res/raw'
OUT.mkdir(parents=True,exist_ok=True)
def write(name,samples):
    samples=np.tanh(samples)*.7
    with wave.open(str(OUT/(name+'.wav')),'wb') as f:
        f.setnchannels(1);f.setsampwidth(2);f.setframerate(RATE)
        f.writeframes((samples*32767).astype('<i2').tobytes())
def note(buf,at,freq,dur,amp=.2):
    t=np.arange(int(dur*RATE))/RATE
    env=(1-np.exp(-t*70))*np.exp(-t*4/dur)
    tone=(np.sin(2*np.pi*freq*t)+.24*np.sin(2*np.pi*freq*2.003*t))*env*amp
    i=int(at*RATE);n=min(len(tone),len(buf)-i)
    if n>0:buf[i:i+n]+=tone[:n]
def bird(buf,at,base=1800):
    t=np.arange(int(.34*RATE))/RATE
    freq=base+750*np.sin(np.pi*t/.34)+140*np.sin(2*np.pi*14*t)
    tone=np.sin(2*np.pi*np.cumsum(freq)/RATE)*np.sin(np.pi*t/.34)**2*.08
    i=int(at*RATE);n=min(len(tone),len(buf)-i)
    if n>0:buf[i:i+n]+=tone[:n]
for name in ('musicbox','forest','garden'):
    b=np.zeros(RATE*24)
    if name=='musicbox':
        for i,f in enumerate([523.25,659.25,783.99,659.25,587.33,698.46,880,698.46]*2):note(b,.5+i*1.4,f,1.8,.16)
    elif name=='forest':
        for at in [1,1.6,4.5,7,7.6,11,14,14.7,18,21]:bird(b,at,1650+(int(at)%3)*200)
        for i,f in enumerate([261.63,329.63,392,329.63,293.66,349.23,440,349.23]):note(b,i*3,f,2.8,.035)
    else:
        for i,f in enumerate([261.63,392,329.63,440,293.66,392,349.23,329.63]):note(b,i*3,f,2.6,.11)
        t=np.arange(len(b))/RATE
        b+=.007*np.sin(2*np.pi*2600*t)*(np.sin(2*np.pi*1.5*t)**16)
    # Silence at the seam prevents loop clicks.
    fade=np.minimum(1,np.minimum(np.arange(len(b)),np.arange(len(b))[::-1])/(RATE*.15))
    write('pet_'+name,b*fade)
for name,notes in {'feed':[523.25,659.25],'wash':[587.33,783.99],'win':[523.25,659.25,783.99,1046.5],'sleep':[659.25,523.25,392],'level':[523.25,783.99,1046.5,1318.5]}.items():
    b=np.zeros(RATE*2)
    for i,f in enumerate(notes):note(b,i*.18,f,.65,.20)
    write('pet_'+name,b)
b=np.zeros(RATE);bird(b,.05);bird(b,.45,2100);write('pet_chirp',b)
print('Generated original pet ambience and effects')
