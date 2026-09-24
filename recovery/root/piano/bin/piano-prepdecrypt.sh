#!/system/bin/sh
#
# Align the recovery's reported security patch levels with the ROM that is
# actually installed, before any secure service starts.
#
# KeyMint caches the OS version and the system and vendor patch levels when it
# starts, and the TEE then refuses to upgrade a key blob whose bound patch
# level does not match. Stamping the values at build time works, but ties one
# recovery image to one firmware build: 307 needed system 2026-07-01 with
# vendor 2026-02-01, 308 needs 2026-08-01 for both, and a mismatch in either
# direction leaves user 0 undecryptable.
#
# Reading them from the installed ROM instead makes a single image correct on
# any firmware. This runs from the stock-runtime setup action in
# init.recovery.qcom.rc, after the stock vendor view is mounted and before
# qseecomd and KeyMint are started, which is the only window where the values
# are both readable and not yet cached.
#
# On any failure the build-time values are left in place, which is no worse
# than not running at all.
#
# KeyMint is the only secure service that reads these values, so it is the
# only one init leaves stopped; this script starts it once the values are in
# place. That start happens from an EXIT trap, so every path out of the script
# - success, failure, or an unexpected error - still brings KeyMint up.
# keystore2 blocks until KeyMint registers and nothing decrypts without it, so
# a KeyMint on the fallback values is always better than no KeyMint.

SLOT_SUFFIX="$(getprop ro.boot.slot_suffix)"
SYSTEM_MNT=/piano/system-stock
VENDOR_MNT=/piano/vendor-stock
LOG_TAG="piano-prepdecrypt"

log_msg() {
    echo "${LOG_TAG}: $1"
    log -t "${LOG_TAG}" "$1" 2>/dev/null
}

# Start KeyMint and wait for init to report it running.
#
# The start is retried, not issued once. Its binary and the binder symlink it
# links through both live under the recovery's own /vendor, which TWRP
# briefly mounts over while it processes the fstab. A ctl.start that lands in
# that window is refused outright -
#   Unable to set property "ctl.start" to "vendor.keymint":
#     PROP_ERROR_HANDLE_CONTROL_MESSAGE (0x20)
# - and init does not retry it, so a single attempt left KeyMint unstarted and
# decryption failed. The same call succeeds once the window has passed.
#
# Bounded: if it never comes up, TWRP's own KeyMint wait reports the failure
# with more context than this script could.
start_keymint() {
    i=0
    while [ "${i}" -lt 30 ]; do
        if [ "$(getprop init.svc.vendor.keymint)" = "running" ]; then
            log_msg "KeyMint running after ${i} retries"
            return 0
        fi
        setprop ctl.start vendor.keymint 2>/dev/null
        sleep 1
        i=$((i + 1))
    done
    log_msg "KeyMint did not reach running within 30s"
    return 1
}

finish() {
    start_keymint
    setprop vendor.piano.prepdecrypt.done 1
}
trap finish EXIT

# Read a property assignment out of a build.prop-style file. Only the first
# match is used: build.prop may carry the same key more than once and init
# itself keeps the first.
read_prop() {
    prop_file="$1"
    prop_name="$2"
    [ -f "${prop_file}" ] || return 1
    value="$(grep -m1 "^${prop_name}=" "${prop_file}" 2>/dev/null)"
    [ -n "${value}" ] || return 1
    echo "${value#*=}"
}

apply_prop() {
    prop_name="$1"
    new_value="$2"
    old_value="$(getprop "${prop_name}")"

    if [ -z "${new_value}" ]; then
        log_msg "no value discovered for ${prop_name}; keeping ${old_value}"
        return 1
    fi
    if [ "${old_value}" = "${new_value}" ]; then
        log_msg "${prop_name} already ${new_value}"
        return 0
    fi
    if ! resetprop "${prop_name}" "${new_value}"; then
        log_msg "resetprop failed for ${prop_name}"
        return 1
    fi
    if [ "$(getprop "${prop_name}")" != "${new_value}" ]; then
        log_msg "${prop_name} did not take ${new_value}"
        return 1
    fi
    log_msg "${prop_name}: ${old_value} -> ${new_value}"
    return 0
}

# The stock vendor view is mounted by the calling action. The system view is
# not, because nothing else in recovery needs it this early.
if [ ! -d "${SYSTEM_MNT}" ]; then
    mkdir -p "${SYSTEM_MNT}" || log_msg "cannot create ${SYSTEM_MNT}"
    chmod 0755 "${SYSTEM_MNT}"
fi

SYSTEM_DEV="/dev/block/mapper/system${SLOT_SUFFIX}"
SYSTEM_MOUNTED=0
if [ ! -f "${SYSTEM_MNT}/system/build.prop" ]; then
    if [ -b "${SYSTEM_DEV}" ] || [ -L "${SYSTEM_DEV}" ]; then
        if mount -t erofs -o ro "${SYSTEM_DEV}" "${SYSTEM_MNT}" 2>/dev/null; then
            SYSTEM_MOUNTED=1
        else
            log_msg "cannot mount ${SYSTEM_DEV}"
        fi
    else
        log_msg "missing ${SYSTEM_DEV}"
    fi
fi

SYSTEM_SPL="$(read_prop "${SYSTEM_MNT}/system/build.prop" ro.build.version.security_patch)"
SYSTEM_REL="$(read_prop "${SYSTEM_MNT}/system/build.prop" ro.build.version.release)"
VENDOR_SPL="$(read_prop "${VENDOR_MNT}/build.prop" ro.vendor.build.security_patch)"

apply_prop ro.build.version.security_patch "${SYSTEM_SPL}"
apply_prop ro.build.version.release "${SYSTEM_REL}"
apply_prop ro.vendor.build.security_patch "${VENDOR_SPL}"

# The system view exists only to be read. Leaving it mounted would give TWRP a
# second, stale mount of a partition it manages itself.
if [ "${SYSTEM_MOUNTED}" = "1" ]; then
    umount "${SYSTEM_MNT}" 2>/dev/null || log_msg "cannot unmount ${SYSTEM_MNT}"
fi

log_msg "done: system=$(getprop ro.build.version.security_patch) vendor=$(getprop ro.vendor.build.security_patch) release=$(getprop ro.build.version.release)"

# The trap starts KeyMint and publishes vendor.piano.prepdecrypt.done.
exit 0
