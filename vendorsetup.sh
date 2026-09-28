# OrangeFox build options. OrangeFox reads these from the environment when
# build/envsetup.sh sources this file.

# AromaFM ships only a 32-bit ARM binary. SM8750 runs 64-bit code only, so it
# fails with "Exec format error" (updater error 255). Leave it out.
export FOX_DELETE_AROMAFM=1
