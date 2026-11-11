// SPDX-License-Identifier: MPL-2.0
// Biome color interception architecture adapted from Fabric Seasons mixin/BiomeMixin.java, revision e0b2dd285e5a7f0b2850871ea7a0eb5289d0937c.
// Original authors: D4rkness_King and Fabric Seasons contributors. Palette and blending are new; no upstream assets imported.
package com.projectregnum.agriculture.client;

public final class SeasonalColors {
    private static final int[] GRASS = {0x91b753, 0x78a93a, 0xc68b38, 0xb7c4c5};
    private static final int[] FOLIAGE = {0x9ecf62, 0x579631, 0xce7735, 0xa5b5b4};
    private static final float[] STRENGTH = {0.18f, 0.20f, 0.65f, 0.58f};
    private static ClientVisualConfig config = new ClientVisualConfig(true, true);
    private static volatile int renderKey = -1;
    private SeasonalColors() {}
    public static void configure(ClientVisualConfig next) { config = next; }
    private static boolean enabled() { return config.enableSeasonalColors() && ClientSeasonState.colors(); }

    // Sixteen steps across the first quarter of each season. Meshes change only when this key changes.
    public static int renderKey() { return renderKey; }
    public static void update() { renderKey = calculateRenderKey(); }
    public static void clear() { renderKey = -1; }
    private static int calculateRenderKey() {
        if (!enabled()) return -1;
        var snapshot = ClientSeasonState.snapshot();
        int step = config.smoothTransitions() ? Math.min(16, (int) (snapshot.progress() * 64)) : 16;
        return snapshot.season().ordinal() * 17 + step;
    }
    public static int tint(int base, boolean foliage) {
        int key = renderKey();
        if (key < 0) return base;
        int current = key / 17;
        int previous = (current + 3) % 4;
        float progress = (key % 17) / 16f;
        int[] palette = foliage ? FOLIAGE : GRASS;
        int from = blend(base, palette[previous], STRENGTH[previous]);
        int to = blend(base, palette[current], STRENGTH[current]);
        return blend(from, to, progress);
    }
    private static int blend(int from, int to, float amount) {
        int result = 0;
        for (int shift = 16; shift >= 0; shift -= 8) {
            int a = from >> shift & 255;
            int b = to >> shift & 255;
            result |= Math.round(a + (b - a) * amount) << shift;
        }
        return result;
    }
}
