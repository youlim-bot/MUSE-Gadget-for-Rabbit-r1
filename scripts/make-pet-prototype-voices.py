"""Recreate local macOS speech prototype clips. Requires installed voices and ffmpeg.
These are separate from the mathematical-tone music/effect asset generators.
"""
from pathlib import Path
import subprocess,tempfile,shutil,sys,re
if sys.platform!='darwin':raise SystemExit('This optional voice prototype generator requires macOS.')
ffmpeg=shutil.which('ffmpeg')
if not ffmpeg:raise SystemExit('ffmpeg is required to encode mono PCM WAV clips.')
voices=subprocess.check_output(['say','-v','?'],text=True)
def voice(prefix,locale):
 for line in voices.splitlines():
  m=re.match(r'^(.*?)\s{2,}('+re.escape(locale)+r')\s+',line)
  if m and m.group(1).startswith(prefix):return m.group(1).strip()
 raise SystemExit('Required system voice is not installed: '+prefix)
lines={
 'ko':(voice('Yuna','ko_KR'),{'hello':'안녕! 우리 함께 자라요.','thanks':'고마워요. 정말 좋아요.','sleep':'잘 자요. 내일 또 만나요.','praise':'해냈어요! 정말 즐거워요.'}),
 'ja':(voice('Kyoko','ja_JP'),{'hello':'こんにちは。一緒に育とうね。','thanks':'ありがとう。とっても嬉しい。','sleep':'おやすみ。また明日ね。','praise':'できたね。とっても楽しい。'}),
 'en':(voice('Samantha','en_US'),{'hello':"Hello! Let's grow together.",'thanks':'Thank you. That makes me happy.','sleep':'Good night. See you tomorrow.','praise':'We did it! That was fun.'})
}
out=Path(__file__).resolve().parents[1]/'app/src/main/res/raw';out.mkdir(parents=True,exist_ok=True)
with tempfile.TemporaryDirectory(prefix='pet-voice-') as td:
 for language,(name,sentences) in lines.items():
  for event,text in sentences.items():
   aiff=Path(td)/f'{language}_{event}.aiff'
   subprocess.run(['say','-v',name,'-r','170','-o',str(aiff),text],check=True)
   subprocess.run([ffmpeg,'-v','error','-y','-i',str(aiff),'-af','afade=t=in:d=0.025','-ac','1','-ar','22050','-c:a','pcm_s16le',str(out/f'pet_voice_{language}_{event}.wav')],check=True)
print('Generated 12 local macOS voice prototype clips.')
