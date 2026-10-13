package com.barofailure.weatherdirector.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;

/**
 * Wind on falling particles, done the way Weather2 does it (studied, reimplemented):
 * a particle is born moving with the air, then its horizontal speed relaxes towards the wind by 10% per tick.
 * Every drop therefore falls at the same slant at any moment, streaks stay parallel, nothing speeds up forever.
 * Rain gets a slant angle from the wind (3 deg calm to 28 deg in a storm) at its own fall speed; light things
 * (snow, dust) simply drift with a share of the air speed.
 * Gusts vary with time only, never with position, so neighbouring drops never disagree.
 */
public final class RainWind {
    /** Per-tick blend of a particle's horizontal speed towards the wind (Weather2: 0.1). */
    public static final double RELAX = 0.1;
    /** Particle Rain rain (gravity 0.9) falls at about 1.76 blocks per tick (0.04 * gravity, friction 0.98). */
    public static final float FALL_SPEED = 1.76f;
    /** Ticks a drop takes to fall to the player's height; rain is spawned this far upwind so it lands on you. */
    public static final float FALL_TICKS = 4f;
    public static final float MAX_UPWIND = 5f;

    private RainWind() {}

    /** 0.75 .. 1.25, slow, the same for the whole sky. */
    public static float gust(ClientLevel level, float partialTick) {
        float t = level.getGameTime() + partialTick;
        return 1f + 0.18f * Mth.sin(t * 0.031f) * Mth.sin(t * 0.0173f + 1.3f) + 0.07f * Mth.sin(t * 0.11f);
    }

    /**
     * Horizontal target speed (blocks per tick) for a particle with this Particle Rain gravity and wind share.
     * Heavy particles (rain, gravity >= 0.5) fall at about 1.96 * gravity blocks per tick (vanilla particle physics:
     * 0.04 * gravity per tick, friction 0.98), so a slant angle maps to a speed.
     */
    private static long cachedTick = Long.MIN_VALUE;
    private static float cachedWind, cachedTan;

    /** Wind and slant are the same for every drop in a tick; computed once per tick, not once per particle. */
    private static void refresh(ClientLevel level) {
        long t = level.getGameTime();
        if (t == cachedTick) return;
        cachedTick = t;
        float scale = ClientConfig.WIND_ON_RAIN.get() ? ClientConfig.WIND_STRENGTH.get().floatValue() : 0f;
        cachedWind = ClientWeather.windSpeed * ClientWeather.rainWindScale * gust(level, 0f) * scale;
        cachedTan = (float) Math.tan((3f + 25f * Mth.clamp(cachedWind, 0f, 1.2f)) * Mth.DEG_TO_RAD) * (scale > 0f ? 1f : 0f);
    }

    public static float targetSpeed(ClientLevel level, float gravity, float windShare) {
        refresh(level);
        float wind = cachedWind;
        if (gravity >= 0.5f) {
            // Calm 3 deg, drizzle ~10, rain ~15, heavy rain ~23, storm ~28 (good rain mods stay within 10-30 deg).
            return FALL_SPEED * Math.min(1f, gravity) * cachedTan;
        }
        float share = Mth.clamp(windShare * 10f, 0.2f, 1f);
        return (0.03f + 0.3f * wind) * share;
    }

    /** How far upwind (blocks) rain should spawn so it drifts onto the camera. */
    public static float upwind(ClientLevel level) {
        return Math.min(MAX_UPWIND, targetSpeed(level, 0.9f, 0.4f) * FALL_TICKS);
    }

    public static float dirX() {
        return Mth.cos(ClientWeather.windAngle);
    }

    public static float dirZ() {
        return Mth.sin(ClientWeather.windAngle);
    }
}
