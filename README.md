# SM8750 common — piano R&D subset

Shared tree for Android 16 ROMs on the Xiaomi Pad 8 Pro, checked out at `device/xiaomi/sm8750-common`. Used by [`lineage-23.2`](../../tree/lineage-23.2). Other projects: [`main`](../../tree/main).

Small platform scaffold adapted for piano; not a drop-in replacement for the
complete dada phone common tree. Adds Virtual A/B and China 307 pinned first-boot
blob seeds. Generated vendor packaging is inherited once. No unconditional radio,
UDFPS, donor display density, or blanket build-check bypasses.

Use piano's offline extraction entry point to generate both vendor trees together.
The blob list is validated against piano stock; cross-device reuse is unproven.
See piano/docs/LINEAGE_RND.md for source state, completed checks and remaining gates.
