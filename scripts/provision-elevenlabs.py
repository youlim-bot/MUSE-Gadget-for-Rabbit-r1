#!/usr/bin/env python3
"""Provision one USB-connected r1 without writing the key to host files or argv."""
import getpass
import json
import os
import re
import subprocess
import time
import urllib.error
import urllib.request

PACKAGE = "dev.cameronpak.muser1"
ADB = [os.environ.get("ADB", "adb"), "-d"]

def adb(*args, data=None):
    p = subprocess.run(ADB + list(args), input=data, capture_output=True, timeout=25)
    if p.returncode:
        raise RuntimeError("ADB failed. Check the single USB device, authorization and debug app installation.")
    return p.stdout

def query(path, key):
    request = urllib.request.Request("https://api.elevenlabs.io" + path,
        headers={"xi-api-key": key, "Accept": "application/json"})
    # Do not forward the API key to a redirect destination.
    class NoRedirect(urllib.request.HTTPRedirectHandler):
        def redirect_request(self, req, fp, code, msg, headers, newurl):
            return None
    try:
        with urllib.request.build_opener(NoRedirect).open(request, timeout=20) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        code = "unknown"
        try:
            detail = json.loads(error.read(16384)).get("detail", {})
            candidate = detail.get("code", detail.get("status", "")) if isinstance(detail, dict) else ""
            allowed = {"invalid_api_key", "missing_permissions", "voice_not_found", "quota_exceeded", "workspace_not_found"}
            if candidate in allowed:
                code = candidate
        except (ValueError, AttributeError):
            pass
        raise RuntimeError(f"ElevenLabs HTTP {error.code} / {code}. Check model/voice availability and permissions; HTTP 400 is not always a permission error.") from None
    except Exception:
        raise RuntimeError("ElevenLabs connection failed; no key or response body was printed.") from None

def main():
    key = None
    pending = False
    try:
        if adb("get-state").strip() != b"device":
            raise RuntimeError("Connect and authorize one r1 over USB.")
        print("Uses your ElevenLabs account for read-only model/voice validation. No speech is generated.")
        print("Enable STT/TTS generation and Models/Voices read permissions. Add your selected library voice to your account first.")
        key = getpass.getpass("ElevenLabs API key (hidden): ").strip()
        voice = input("Voice ID: ").strip()
        if not key or any(c.isspace() for c in key) or not re.fullmatch(r"[A-Za-z0-9_-]{1,128}", voice):
            raise RuntimeError("Invalid key or voice ID format.")
        models = query("/v1/models", key)
        if not any(m.get("model_id") == "eleven_v4" for m in models):
            raise RuntimeError("eleven_v4 is not available in this account's model list. No settings changed.")
        selected = query("/v1/voices/" + voice, key)
        if selected.get("voice_id") != voice:
            raise RuntimeError("Voice could not be validated. No settings changed.")
        adb("shell", "run-as", PACKAGE, "rm", "-f", "files/elevenlabs-import-ok")
        payload = json.dumps({"api_key": key, "voice_id": voice}).encode()
        adb("shell", f'run-as {PACKAGE} sh -c "umask 077; mkdir -p files; cat > files/pending-elevenlabs.json"', data=payload)
        pending = True
        key = None
        payload = None
        adb("shell", "am", "start", "-a", "android.settings.SETTINGS")
        adb("shell", "am", "start", "-n", PACKAGE + "/.MainActivity")
        for _ in range(15):
            p = subprocess.run(ADB + ["shell", f'run-as {PACKAGE} sh -c "test ! -e files/pending-elevenlabs.json && test -s files/elevenlabs-import-ok"'], capture_output=True, timeout=5)
            if p.returncode == 0:
                pending = False
                print("Encrypted import confirmed. Test recognition/playback on the r1; usage can incur charges.")
                return
            time.sleep(1)
        raise RuntimeError("Import not confirmed. Unlock the r1 and retry; any remaining plaintext import will be removed.")
    finally:
        key = None
        if pending:
            subprocess.run(ADB + ["shell", "run-as", PACKAGE, "rm", "-f", "files/pending-elevenlabs.json"], capture_output=True, timeout=10)

if __name__ == "__main__":
    try:
        main()
    except (KeyboardInterrupt, EOFError):
        print("Cancelled.")
        raise SystemExit(1)
    except RuntimeError as error:
        print(str(error))
        raise SystemExit(1)
    except Exception:
        print("Setup failed. Check connectivity; no credentials were printed.")
        raise SystemExit(1)
