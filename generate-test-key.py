#!/usr/bin/env python3
"""Generate a new private Lineage AVB key outside any source repository."""
import os
from pathlib import Path
import subprocess

key = Path.home() / '.local/share/xiaomi-pad-8-pro/keys/lineage-23.2-avb.pem'
key.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
if any((parent/'.git').exists() or (parent/'.repo').exists() for parent in key.parents):
    raise SystemExit('Refusing to generate a private key inside a repository')
os.umask(0o077)
# Exclusive creation protects existing key material. Never print the key.
with key.open('xb') as stream:
    subprocess.run(['openssl', 'genpkey', '-algorithm', 'RSA',
                    '-pkeyopt', 'rsa_keygen_bits:4096'], stdout=stream, check=True)
key.chmod(0o600)
print(f'PIANO_AVB_KEY_PATH={key}')
