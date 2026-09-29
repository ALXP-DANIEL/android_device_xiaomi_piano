#!/bin/bash
# Apply piano's changes to LineageOS source projects.
# Run from the top of the LineageOS tree: device/xiaomi/piano/patches/apply-patches.sh
set -e
TOP=$(pwd)
DIR=$(cd "$(dirname "$0")" && pwd)
for d in "$DIR"/*/; do
  project=$(basename "$d" | tr _ /)
  for p in "$d"*.patch; do
    if git -C "$TOP/$project" apply --reverse --check "$p" 2>/dev/null; then
      echo "already applied: $project $(basename "$p")"
    else
      git -C "$TOP/$project" apply "$p"
      echo "applied: $project $(basename "$p")"
    fi
  done
done
