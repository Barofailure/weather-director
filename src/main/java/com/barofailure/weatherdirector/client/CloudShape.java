package com.barofailure.weatherdirector.client;

import com.qendolin.betterclouds.config.Config;
import net.minecraft.util.Mth;

/**
 * Better Clouds reads its settings every frame; we hand its renderer a copy shaped by the weather, so rain clouds
 * sit lower and grow thicker and a storm becomes a heavy low deck. The player's own Better Clouds settings stay
 * untouched (we never write to them). The shape moves in only 4 steps and never touches the puff size (shader
 * parameters), so Better Clouds rebuilds rarely and never recompiles its shaders because of us.
 */
public final class CloudShape {
    private static Config lastOriginal;
    private static Config lastSnapshot;
    private static Config shaped;
    private static int lastKey = -1;
    private static long lastCheck;

    private CloudShape() {}

    /** Steps of the weather shape. Every step makes Better Clouds rebuild its whole cloud field, so keep them few. */
    private static final int STEPS = 4;

    /** 0 (dry) .. STEPS (storm). */
    private static int key() {
        float wet = Mth.clamp((ClientWeather.gloom - 0.4f) / 0.6f, 0f, 1f);
        return Math.round(wet * STEPS);
    }

    public static Config shape(Config original) {
        if (original == null || !ClientWeather.active() || !ClientConfig.SHAPE_CLOUDS.get() || !ClientConfig.CONTROL_CLOUDS.get()) return original;
        int key = key();
        if (key == 0) return original;
        // Called ~10 times a frame: compare the player's whole settings only once a second (they change rarely).
        long now = System.currentTimeMillis();
        boolean recheck = now - lastCheck > 1000;
        if (recheck) lastCheck = now;
        if (original != lastOriginal || key != lastKey || (recheck && !original.equals(lastSnapshot))) {
            float wet = key / (float) STEPS;
            Config c = new Config(original);
            // Only generation settings change. sizeXZ / sizeY are part of Better Clouds' shader parameters: changing
            // them recompiles its shaders (a stutter every step), so the puff size always stays the player's own.
            c.yOffset = original.yOffset - 36f * wet;          // cloud base comes down
            c.yRange = original.yRange * (1f + 0.7f * wet);    // and the deck grows taller
            shaped = c;
            lastOriginal = original;
            lastSnapshot = new Config(original);
            lastKey = key;
        }
        return shaped;
    }
}
