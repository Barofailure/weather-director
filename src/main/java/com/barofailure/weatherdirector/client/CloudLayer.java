package com.barofailure.weatherdirector.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import com.barofailure.weatherdirector.Compat;

/**
 * Where the cloud deck is, so rain falls FROM it: above a thick deck there is no rain, no rain sound and no gloom
 * (sunshine over a sea of cloud), inside it you are in cloud fog. Heights follow Better Clouds' shape when it is
 * installed (base = dimension cloud height + yOffset, puffs from -0.3 * yRange to +yRange), else the vanilla layer.
 * Under a shader pack that draws its own clouds there is no deck of ours (rain falls at every height, no cloud fog),
 * unless the pack draws this deck itself (shaderDeckPacks; it gets the heights as wdCloudBottom / wdCloudTop).
 */
public final class CloudLayer {
    private static float above, aboveRain, inside;
    private static final float BAND_SHIFT = 5f;
    /** Test rig only (env BW_NO_CLOUD_FOG=1): no cloud fog, to see where Better Clouds' puffs really are. */
    private static final boolean NO_CLOUD_FOG = "1".equals(System.getenv("BW_NO_CLOUD_FOG"));
    private static float bottom = 188, top = 200;

    private CloudLayer() {}

    /** 0 below the deck .. 1 well above it (eased). */
    public static float above() {
        return above;
    }

    /** 0 outside .. 1 deep inside the deck (eased). */
    public static float inside() {
        return inside;
    }

    public static float bottom() {
        return bottom;
    }

    public static float top() {
        return top;
    }

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        boolean on = level != null && ClientWeather.active(level) && mc.gameRenderer.getMainCamera().isInitialized();
        float base = on ? level.effects().getCloudHeight() : Float.NaN;
        if (on && Float.isNaN(base)) {
            above = aboveRain = inside = 0f;
            return;
        }
        // The deck's heights are kept up to date even while it is off: a shader pack that draws it reads them
        if (on) {
            float lo = base - 1f, hi = base + 5f;
            if (Compat.on(Compat.BETTER_CLOUDS)) {
                float[] r = BetterCloudsHeights.range(base);
                lo = r[0];
                hi = r[1];
            }
            // 2026-10-12: the whole cloud band (fog in, fog out, rain stop) a bit lower, ~5 blocks.
            bottom = lo - BAND_SHIFT;
            top = hi - BAND_SHIFT;
        }
        if (!on || !ClientConfig.CLOUD_LAYER.get() || !ShaderWeather.ourCloudsDrawn()) {
            above += (0f - above) * 0.1f;
            aboveRain += (0f - aboveRain) * 0.1f;
            inside += (0f - inside) * 0.1f;
            return;
        }
        float lo = bottom, hi = top;
        double y = mc.gameRenderer.getMainCamera().getPosition().y;
        // Only a real deck clears the sky above it or wraps you in fog; scattered fair-weather clouds do not.
        float deck = Mth.clamp((ClientWeather.cover - 0.6f) / 0.3f, 0f, 1f);
        // Rain is gone 4 blocks above the deck top (it starts fading 2 below it), also while the deck is still building
        // or thinning out (light rain falls from it then, but never above it); cloud fog builds over the first 6 blocks
        // inside the deck and thins over the last 6.
        float overTop = Mth.clamp((float) (y - (hi - 2f)) / 6f, 0f, 1f);
        float aboveNow = overTop * deck;
        float depth = Math.min((float) (y - lo), (float) (hi - y));
        float insideNow = Mth.clamp(depth / 6f, 0f, 1f) * deck * Mth.clamp((ClientWeather.cover - 0.8f) / 0.2f, 0f, 1f);
        if (NO_CLOUD_FOG) insideNow = 0f; // test rig: see the real clouds without our cloud fog
        above += (aboveNow - above) * 0.1f;
        aboveRain += (overTop - aboveRain) * 0.1f;
        inside += (insideNow - inside) * 0.1f;
    }

    /** 0 below the deck top .. 1 above it, whatever the cover (eased): rain only falls below the clouds. */
    public static float aboveRain() {
        return aboveRain;
    }

    /** Client-side rain as seen from here: none above the deck. Used for every rain-level read on the client. */
    public static float visibleRain(float rainLevel) {
        return rainLevel * (1f - aboveRain);
    }
}
