#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Offline extraction only. Use prepare-stock.py to verify and unpack inputs."""
import hashlib
import sys
from pathlib import Path
ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / 'tools/extract-utils'))
from extract_utils.main import ExtractUtils, ExtractUtilsModule
from extract_utils.fixups_blob import blob_fixup
# Frozen QTI composer V1/V3 definitions are equivalent after comments and
# integer literal formatting; V3 imports the stock service's Android composer V3.
blob_fixups = {
    'vendor/bin/hw/vendor.qti.hardware.display.composer-service': blob_fixup().replace_needed(
        'vendor.qti.hardware.display.composer3-V1-ndk.so',
        'vendor.qti.hardware.display.composer3-V3-ndk.so'),
}
module = ExtractUtilsModule(
    'sm8750-common', 'xiaomi', check_elf=True, blob_fixups=blob_fixups,
    # Stock display DAL links an ODM notifier supplied by piano.
    namespace_imports=['hardware/qcom-caf/sm8750',
                       'vendor/qcom/opensource/commonsys-intf/display', 'vendor/xiaomi/piano'],
)
if __name__ == '__main__':
    if len(sys.argv) != 2 or not Path(sys.argv[1]).is_dir():
        raise SystemExit('Supply exactly one offline extracted directory; device extraction is disabled')
    lists = [Path(__file__).with_name('proprietary-files.txt')]
    source = Path(sys.argv[1])
    for manifest in lists:
        for line in manifest.read_text().splitlines():
            if not line or line.startswith('#'):
                continue
            name, expected, *_ = line.split('|')
            path = source / name.split(';')[0]
            if not path.is_file() or hashlib.sha1(path.read_bytes()).hexdigest() != expected:
                raise SystemExit(f'Missing or mismatched China 307 input: {name}')
    ExtractUtils.device(module).run()
