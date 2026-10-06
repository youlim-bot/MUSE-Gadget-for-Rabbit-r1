#!/usr/bin/env python3
"""Compose demo screenshots into showcase sheets. Requires Pillow."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import shutil,zipfile
repo=Path(__file__).resolve().parents[1];src=repo/'docs/images';out=repo/'build/discord-r1-v1.5';out.mkdir(parents=True,exist_ok=True)
font=next((str(p) for p in [Path('/System/Library/Fonts/Supplemental/Arial.ttf'),Path('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf')] if p.exists()), 'Arial.ttf')
def f(n):return ImageFont.truetype(font,n)
def sheet(filename,items,subtitle):
 canvas=Image.new('RGB',(1560,1660),'#0d100f');d=ImageDraw.Draw(canvas)
 d.text((42,27),'MUSE GADGET',font=f(43),fill='#f6f5ef');d.text((1160,32),'RABBIT r1 / V1.5',font=f(29),fill='#ff984d')
 d.text((42,86),subtitle,font=f(23),fill='#b5bdb8')
 for i,(name,label) in enumerate(items):
  x=42+(i%3)*504;y=165+(i//3)*710
  d.text((x,y-34),label,font=f(22),fill='#ff984d')
  im=Image.open(src/(name+'.png')).convert('RGB');assert im.size==(480,640);canvas.paste(im,(x,y))
 d.text((42,1579),'Real Android UI · Offline demo · Fictional data · Original Muse avatar',font=f(22),fill='#d0d7d2')
 d.text((42,1614),'github.com/youlim-bot/MUSE-Gadget-for-Rabbit-r1',font=f(22),fill='#929b95')
 canvas.save(src/filename)
sheet('discord-overview.png',[('en-home','01  CHAT + HOME STATUS'),('en-clock','02  WEATHER CLOCK'),('en-alarms','03  LOCAL REMINDERS'),('ko-translate','04  KO / JA / EN'),('en-photo','05  PHOTO QUESTIONS'),('en-tasks','06  MEMOS + CHECKLISTS')],'Customized for everyday conversations, reminders and a charging desk clock.')
sheet('discord-clock-reminders.png',[('en-clock','CLOCK / ENGLISH'),('ko-clock','CLOCK / KOREAN'),('ja-clock','CLOCK / JAPANESE'),('en-home','BATTERY + NEXT REMINDER'),('en-alarms','LOCAL ALARMS + TIMERS'),('en-reminder','IN-MUSE REMINDER EDITOR')],'V1.5 daily controls · Weather icons, humidity and hourly UV forecast')
for p in src.glob('*.png'):shutil.copy2(p,out/p.name)
shutil.copy2(repo/'docs/DISCORD-V1.5.md',out/'Discord-post-English.md')
(out/'README.txt').write_text('Upload discord-overview.png to Discord and paste Discord-post-English.md. Optional: add discord-clock-reminders.png or individual screenshots. All images use fictional offline sample data. Do not describe them as live AI results.\n')
with zipfile.ZipFile(repo/'build/MUSE-r1-V1.5-Discord-kit.zip','w',zipfile.ZIP_DEFLATED) as z:
 for p in sorted(out.iterdir()):z.write(p,'MUSE-r1-V1.5-Discord-kit/'+p.name)
# Local inspection contact sheet; not a public asset.
files=sorted(src.glob('*.png'));files=[p for p in files if not p.name.startswith('discord')]
im=Image.new('RGB',(1200,((len(files)+4)//5)*350),'#252827');d=ImageDraw.Draw(im)
for i,p in enumerate(files):
 x=i%5*240;y=i//5*350;im.paste(Image.open(p).convert('RGB').resize((240,320)),(x,y+30));d.text((x+4,y+5),p.stem,font=f(17),fill='white')
im.save(repo/'build/v15-contact.png')
