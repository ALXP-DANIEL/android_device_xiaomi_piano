#!/bin/bash
# Apply piano's changes to LineageOS source projects.
# Run from the top of the LineageOS tree: device/xiaomi/piano/patches/apply-patches.sh
# Each patched project is reset to its synced state first, so the patches
# always apply in order on a clean tree (safe to rerun).
set -e
TOP=$(pwd)
DIR=$(cd "$(dirname "$0")" && pwd)
for d in "$DIR"/*/; do
  project=$(basename "$d" | tr _ /)
  git -C "$TOP/$project" checkout -- .
  for p in "$d"*.patch; do
    git -C "$TOP/$project" apply "$p"
    echo "applied: $project $(basename "$p")"
  done
done
