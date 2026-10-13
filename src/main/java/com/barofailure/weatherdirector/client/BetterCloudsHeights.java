package com.barofailure.weatherdirector.client;

import com.qendolin.betterclouds.config.Config;
import com.qendolin.betterclouds.config.ConfigManager;

/** Better Clouds' deck heights for the current weather shape. Only loaded when Better Clouds is installed. */
final class BetterCloudsHeights {
    private BetterCloudsHeights() {}

    static float[] range(float base) {
        Config c = CloudShape.shape(ConfigManager.instance());
        if (c == null) return new float[] {base - 1f, base + 5f};
        float half = c.sizeY / 2f;
        // Measured on the test rig with our cloud fog off (rain deck, base 192, yOffset -18, yRange 86): the first visible
        // puffs at ~y190 (= base + yOffset + 0.2 yRange; the lowest puffs are tiny and faded), blue sky from ~y225
        // (= base + yOffset + 0.5 yRange + half a puff).
        return new float[] {base + c.yOffset + 0.2f * c.yRange, base + c.yOffset + 0.5f * c.yRange + half};
    }
}
