package com.barofailure.weatherdirector.client;

import com.barofailure.weatherdirector.Compat;

/**
 * Where weather shortens the view: fog (~130 blocks), thunderstorms (~300) and standing inside the cloud deck
 * (~36). One curve in blocks for BOTH the normal chunks (vanilla fog) and Distant Horizons' LODs, so there is
 * never a seam or "ring" where the chunks end (extinction k per block, fully opaque at 3 / k).
 * The curve is linear from 5% of that distance (vanilla and DH both draw linear fog).
 */
public final class WeatherFog {
    private static final float K_FOG = 3f / 36f;
    /** Weather view obscuration (look "haze"): 1.0 = fully opaque at ~130 blocks (fog); storms use ~0.45 (~300). */
    private static final float K_HAZE = 3f / 130f;
    /** Below this (visibility beyond 6000 blocks) the weather does not touch fog at all. */
    private static final float K_MIN = 3f / 6000f;

    private WeatherFog() {}

    private static float k() {
        if (!ClientWeather.active()) return 0f;
        float view = ClientConfig.VIEW_FOG.get() ? ClientConfig.VIEW_FOG_STRENGTH.get().floatValue() : 0f;
        return (K_HAZE * ClientWeather.haze() + K_FOG * CloudLayer.inside()) * view;
    }

    /**
     * True while weather fog is thick enough to matter; only then do we take over vanilla AND Distant Horizons fog
     * (always both together, or the chunk edge would show). With DH the weather must limit the view to well inside
     * its LOD radius; lighter haze leaves both untouched.
     */
    public static boolean active() {
        float k = k();
        return k > K_MIN && 3f / k <= limit();
    }

    static float limit() {
        return Compat.on(Compat.DISTANT_HORIZONS) ? 0.6f * DhFog.radius() : 6000f;
    }

    /** Distance (blocks) at which the fog is fully opaque. */
    public static float end() {
        return 3f / Math.max(k(), K_MIN);
    }

    /** Distance (blocks) where the fog begins. */
    public static float start() {
        return end() * 0.05f;
    }
}
