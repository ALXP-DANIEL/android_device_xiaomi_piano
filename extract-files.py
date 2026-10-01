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
# V2 -> V3 frozen sensor API changes only add SensorType.MOISTURE_INTRUSION.
# Source sensorservice V1 imports V3; keep selected clients on that same ABI.
sensor_clients = ('odm/bin/hw/vendor.xiaomi.hw.touchfeature-service', 'odm/lib64/hw/displayfeature.default.so', 'odm/lib64/libadaptivehdr.so', 'odm/lib64/libcolortempmode.so', 'odm/lib64/libdither.so', 'odm/lib64/libflatmode.so', 'odm/lib64/libhistprocess.so', 'odm/lib64/libmiBrightness.so', 'odm/lib64/libmiSensorCtrl.so', 'odm/lib64/libpaperMode.so', 'odm/lib64/librhytheyecare.so', 'odm/lib64/libsdr2hdr.so', 'odm/lib64/libsre.so', 'odm/lib64/libtruetone.so', 'odm/lib64/libvideomode.so')
# These blobs were built against the Android 14 tinyxml2 ABI; the current
# libtinyxml2 crashes them (e.g. DisplayFeature in ParamManager::getParam).
tinyxml2_clients = tuple(
    [f'odm/lib64/camera/plugins/com.xiaomi.plugin.{p}.so' for p in (
        'anchor', 'offlineawbideal', 'offlineb2y', 'offlineformatconvertor',
        'offlinehdrraw2y', 'offlineheic', 'offlinei2y', 'offlinejpeg',
        'offlinemfnr', 'offlinemlawb', 'offlinetintlesshdr', 'offlinetintless',
        'offlineyuvreprocess', 'offlineyuvsplit')] +
    ['odm/lib64/libmiXmlParser.so',
     'vendor/bin/hw/vendor.qti.camera.provider-service_64',
     'vendor/bin/hw/vendor.xiaomi.hardware.miperf2-service',
     'vendor/lib64/libcamxcoreutils.so', 'vendor/lib64/libcamxods.so',
     'vendor/lib64/libmicamera_aidl_provider.so',
     'vendor/lib64/libmicamera_hal_core.so', 'vendor/lib64/libsimulation.so'])
blob_fixups = {
    tuple(c for c in sensor_clients if c != 'odm/lib64/hw/displayfeature.default.so'):
        blob_fixup().replace_needed(
            'android.hardware.sensors-V2-ndk.so', 'android.hardware.sensors-V3-ndk.so'),
    ('odm/lib64/hw/displayfeature.default.so',): blob_fixup()
        .replace_needed('android.hardware.sensors-V2-ndk.so', 'android.hardware.sensors-V3-ndk.so')
        .replace_needed('libtinyxml2.so', 'libtinyxml2-v34.so'),
    tinyxml2_clients: blob_fixup().replace_needed('libtinyxml2.so', 'libtinyxml2-v34.so'),
}
module = ExtractUtilsModule(
    'piano', 'xiaomi', check_elf=True, blob_fixups=blob_fixups,
    namespace_imports=['hardware/qcom-caf/sm8750',
                       'vendor/qcom/opensource/commonsys-intf/display',
                       'vendor/xiaomi/sm8750-common'],
)
if __name__ == '__main__':
    if len(sys.argv) != 2 or not Path(sys.argv[1]).is_dir():
        raise SystemExit('Supply exactly one offline extracted directory; device extraction is disabled')
    lists = [Path(__file__).with_name('proprietary-files.txt')]
    lists.append(Path(__file__).parent.parent / 'sm8750-common/proprietary-files.txt')
    source = Path(sys.argv[1])
    for manifest in lists:
        for line in manifest.read_text().splitlines():
            if not line or line.startswith('#'):
                continue
            name, expected, *_ = line.split('|')
            path = source / name.lstrip('-').split(';')[0].split(':')[0]
            if not path.is_file() or hashlib.sha1(path.read_bytes()).hexdigest() != expected:
                raise SystemExit(f'Missing or mismatched Global 304 input: {name}')
    ExtractUtils.device_with_common(module, 'sm8750-common', module.vendor).run()
