#!/usr/bin/env python3
"""Explicit USB provisioning; the user supplies their own restricted Routes API key."""
import getpass
import re
import subprocess
import sys


def main():
    key = getpass.getpass("Routes API key (hidden): ").strip()
    if not re.fullmatch(r"AIza[A-Za-z0-9_-]{35}", key):
        raise ValueError("Unexpected key format")
    # -d refuses ambiguous physical-device selection. Key travels on stdin, never argv/logs.
    result = subprocess.run(
        ["adb", "-d", "shell", 'run-as dev.cameronpak.muser1 sh -c "umask 077; cat > files/pending-google-routes-key"'],
        input=key.encode(), capture_output=True, timeout=20,
    )
    key = ""
    if result.returncode:
        raise RuntimeError("USB transfer failed; check device authorization and debug app installation")
    print("Transferred to app-private staging. Open Quick questions → Directions on r1 to encrypt/import and remove staging.")
    print("This transfers a key only; API validity must be checked by requesting a route.")


if __name__ == "__main__":
    try:
        main()
    except (ValueError, RuntimeError, subprocess.SubprocessError) as error:
        print(type(error).__name__ + ": provisioning did not complete", file=sys.stderr)
        sys.exit(1)
