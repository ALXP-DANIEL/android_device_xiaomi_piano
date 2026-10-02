#!/bin/bash
# SPDX-License-Identifier: Apache-2.0
#
# Authorize this computer for adb on a piano that cannot show the "Allow USB
# debugging" prompt (stuck boot animation, or a build without the prompt), by
# adding the computer's public adb key to the device from OrangeFox/TWRP.
# The key goes to the device's /data, never into a ROM build.
#
# Usage: boot the tablet into the recovery, then
#   tools/adb-authorize-from-recovery.sh [path/to/adbkey.pub]
set -e
KEY="${1:-$HOME/.android/adbkey.pub}"
[ -r "$KEY" ] || { echo "No public key at $KEY (run 'adb devices' once to create one)"; exit 1; }

if ! adb get-state 2>/dev/null | grep -q recovery; then
    echo "Boot the tablet into the recovery first (Power + Volume Up)."
    exit 1
fi

adb shell 'mount /data 2>/dev/null; mountpoint -q /data' || {
    echo "/data is not mounted. Unlock/decrypt data in the recovery first."
    exit 1
}
adb shell 'mkdir -p /data/misc/adb && touch /data/misc/adb/adb_keys'
if adb shell 'cat /data/misc/adb/adb_keys' | grep -qF "$(cut -d' ' -f1 "$KEY")"; then
    echo "Key already present."
else
    adb push "$KEY" /data/local/tmp/adbkey.pub >/dev/null 2>&1 || adb push "$KEY" /tmp/adbkey.pub
    adb shell 'f=/data/local/tmp/adbkey.pub; [ -e $f ] || f=/tmp/adbkey.pub
        cat $f >> /data/misc/adb/adb_keys; echo >> /data/misc/adb/adb_keys'
fi
adb shell 'chown system:shell /data/misc/adb/adb_keys; chmod 0640 /data/misc/adb/adb_keys
    chown system:shell /data/misc/adb; chmod 2750 /data/misc/adb
    restorecon -R /data/misc/adb 2>/dev/null || true'
echo "Done. Reboot the tablet; adb should now be authorized."
