#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Verify pinned stock images and prepare offline userspace/kernel input directories.
Does not invoke adb, fastboot, Soong, flash or modify the input firmware.
"""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import sys

HERE = Path(__file__).resolve().parent

def sha256(path):
    h = hashlib.sha256()
    with path.open('rb') as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b''):
            h.update(chunk)
    return h.hexdigest()

def verify(stock):
    lock = json.loads((HERE / 'stock-inputs.json').read_text())
    for name, expected in lock['files'].items():
        path = stock / name
        if path.stat().st_size != expected['size'] or sha256(path) != expected['sha256']:
            raise SystemExit(f'Firmware input mismatch: {path}')
    print(f"Verified {len(lock['files'])} inputs: {lock['firmware']}", flush=True)
    return lock

def run(*args, **kwargs):
    subprocess.run([str(x) for x in args], check=True, **kwargs)

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('super_unpacked', type=Path)
    parser.add_argument('--output', type=Path)
    parser.add_argument('--unpack-bootimg', type=Path,
                        help='Path to AOSP unpack_bootimg.py')
    parser.add_argument('--verify-only', action='store_true')
    args = parser.parse_args()
    source = args.super_unpacked.resolve()
    if source.name != 'super-unpacked':
        parser.error('Input must be the pinned firmware super-unpacked directory')
    lock = verify(source.parent)
    if args.verify_only:
        return
    if args.output is None or args.unpack_bootimg is None:
        parser.error('--output and --unpack-bootimg are required for preparation')
    out = args.output.resolve()
    if out.exists():
        parser.error('Output must not exist; partial or old output is never silently reused')
    for tool in ('fsck.erofs', 'lz4', 'cpio'):
        if shutil.which(tool) is None:
            parser.error(f'Missing host tool: {tool}')
    if not args.unpack_bootimg.is_file():
        parser.error('unpack_bootimg.py is missing')
    out.mkdir(parents=True)
    dump = out / 'dump'
    dump.mkdir()
    for part in ('vendor', 'odm', 'vendor_dlkm', 'system_dlkm'):
        run('fsck.erofs', f'--extract={dump / part}', '--no-preserve-owner',
            source / f'{part}_a.img')
    kernel = out / 'piano-kernel'
    kernel.mkdir()
    unpack = out / 'unpacked'
    for part in ('boot', 'vendor_boot'):
        run(sys.executable, args.unpack_bootimg, '--boot_img',
            source.parent / 'images' / f'{part}.img', '--out', unpack / part)
    shutil.copy2(unpack / 'boot/kernel', kernel / 'kernel')
    (kernel / 'dtb').mkdir()
    shutil.copy2(unpack / 'vendor_boot/dtb', kernel / 'dtb/piano.dtb')
    shutil.copy2(source.parent / 'images/dtbo.img', kernel / 'dtbo.img')
    shutil.copy2(unpack / 'vendor_boot/bootconfig', kernel / 'stock-bootconfig')
    ramdisk = unpack / 'vendor_ramdisk'
    ramdisk.mkdir()
    archive = unpack / 'vendor.cpio'
    with archive.open('wb') as stream:
        run('lz4', '-dc', unpack / 'vendor_boot/vendor_ramdisk00', stdout=stream)
    with archive.open('rb') as stream:
        names = subprocess.check_output(['cpio', '-it', '--quiet'], stdin=stream, text=True)
    for name in names.splitlines():
        if Path(name).is_absolute() or '..' in Path(name).parts:
            raise SystemExit(f'Unsafe archive member: {name}')
    with archive.open('rb') as stream:
        run('cpio', '-idm', '--quiet', stdin=stream, cwd=ramdisk)
    shutil.copytree(ramdisk / 'lib/modules', kernel / 'vendor_ramdisk', symlinks=True)
    shutil.copytree(dump / 'vendor_dlkm/lib/modules', kernel / 'vendor_dlkm', symlinks=True)
    for layout in ('lib', 'flatten'):
        shutil.copytree(dump / 'system_dlkm' / layout,
                        kernel / 'system_dlkm' / layout, symlinks=True)
    # Build-generated properties/fs_config must not be replaced by stock etc/.
    # Preserve the complete stock module tuple; no depmod/dedup/ABI rewriting.
    (out / 'verified-inputs.json').write_text(json.dumps(lock, indent=2) + '\n')
    print(f'Prepared {dump} and {kernel}. Run extract-files.py with the dump path in a Lineage checkout.')

if __name__ == '__main__':
    main()
