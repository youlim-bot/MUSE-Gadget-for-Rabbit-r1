#!/usr/bin/env python3
"""Targeted publication checks. Does not replace manual review or a secret scanner."""
from pathlib import Path
import re
import subprocess
import sys
root = Path(__file__).resolve().parents[1]
files = [root / p for p in subprocess.check_output(['git','-C',str(root),'ls-files','-z']).decode().split('\0') if p]
problems = []
for p in files:
    rel = p.relative_to(root).as_posix()
    if p.suffix.lower() in {'.apk','.aab','.jks','.keystore','.img','.bin','.log'} or p.name.startswith('.env'):
        problems.append((rel, 'private/generated artifact extension'))
    try:
        text = p.read_text()
    except (UnicodeDecodeError, OSError):
        continue
    patterns = [r'gh[pousr]_[A-Za-z0-9]{30,}',r'github_pat_[A-Za-z0-9_]{30,}',r'sk-[A-Za-z0-9]{24,}',r'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----',r'/' + 'Users' + r'/[^/\s]+/',r'/' + 'Volumes' + r'/[^/\s]+/']
    # This scanner's pattern definitions contain path prefixes but no actual user paths.
    for pattern in patterns:
        if re.search(pattern,text):problems.append((rel,'possible secret or local personal path'))
    if p.suffix == '.md':
        targets=re.findall(r'\]\(([^\s)]+)(?:\s+"[^"]*")?\)',text)
        targets += re.findall(r'<img[^>]+src="([^"]+)"',text)
        for target in targets:
            if re.match(r'^[a-z]+:',target) or target.startswith('#'):continue
            target=target.split('#')[0]
            if target and not (p.parent/target).exists():problems.append((rel,'missing link: '+target))
if problems:
    for path,reason in problems:print(path+': '+reason)
    sys.exit(1)
print(f'PASS: {len(files)} tracked files checked; local doc links and targeted artifact/secret patterns clean.')
