#!/usr/bin/env python3
"""Reproduce the original MIT-licensed geometric public avatar. Requires Pillow."""
from pathlib import Path
from PIL import Image, ImageDraw
out = Path(__file__).resolve().parents[1] / 'app/src/main/res/drawable-nodpi/muse_public_avatar.png'
im = Image.new('RGBA', (512, 512))
d = ImageDraw.Draw(im)
d.rounded_rectangle((102, 110, 410, 395), radius=120, fill='#ff9a55')
d.ellipse((156, 201, 198, 243), fill='#171817')
d.ellipse((314, 201, 356, 243), fill='#171817')
d.arc((205, 229, 307, 302), 0, 180, fill='#171817', width=12)
d.ellipse((109, 246, 161, 278), fill='#ffbb88')
d.ellipse((351, 246, 403, 278), fill='#ffbb88')
d.rounded_rectangle((235, 57, 277, 134), radius=20, fill='#ff9a55')
im.save(out)
