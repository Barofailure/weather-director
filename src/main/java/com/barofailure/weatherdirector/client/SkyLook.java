package com.barofailure.weatherdirector.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The colour side: grey skies and clouds without rain, sun and stars hidden by cloud, fog colour,
 * and the seasonal touches (sky tint, sunrise/sunset palette, clearer winter stars).
 * Ecliptic Seasons already shifts each biome's base sky colour by season; ours sits on top, gently.
 */
public final class SkyLook {
    private SkyLook() {}

    // Per season (spring, summer, autumn, winter, none).
    private static final float[][] SKY_TINT = {
            {1.00f, 1.02f, 1.03f}, {0.93f, 0.98f, 1.08f}, {1.04f, 1.00f, 0.94f}, {0.97f, 1.00f, 1.05f}, {1, 1, 1}};
    private static final float[] SKY_PALE = {0.00f, 0.00f, 0.04f, 0.12f, 0f};
    private static final float[][] SUNSET_TINT = {
            {1.00f, 0.97f, 1.08f}, {1.06f, 0.88f, 0.80f}, {1.06f, 0.84f, 0.78f}, {0.96f, 0.86f, 1.18f}, {1, 1, 1}};
    private static final float[] SUNSET_STRENGTH = {1.05f, 1.30f, 1.20f, 1.00f, 1f};
    private static final float[] STARS = {1.10f, 1.00f, 1.30f, 1.70f, 1f};

    private static float seasonal() {
        return ClientConfig.SEASONAL_SKY.get() ? ClientConfig.SEASONAL_SKY_STRENGTH.get().floatValue() : 0f;
    }

    /** How grey the sky, fog and clouds look: the rain level, or our cloud gloom if that is more. */
    public static float gloom(Level level, float rainLevel) {
        return ClientWeather.active(level) && ClientConfig.GREY_SKIES.get() ? Math.max(rainLevel, ClientWeather.gloom * (1f - CloudLayer.above())) : rainLevel;
    }

    /** Same for daylight: overcast dims the world a little, never as much as rain. */
    public static float lightGloom(Level level, float rainLevel) {
        return ClientWeather.active(level) && ClientConfig.GREY_SKIES.get() ? Math.max(rainLevel, ClientWeather.gloom * 0.6f * (1f - CloudLayer.above())) : rainLevel;
    }

    /** How much sun, moon and stars are hidden (vanilla: by rain only). */
    public static float sunHidden(Level level, float rainLevel) {
        if (!ClientWeather.active(level)) return rainLevel;
        float byCloud = !ClientConfig.GREY_SKIES.get() ? 0f : Mth.clamp((ClientWeather.cover - 0.7f) / 0.3f, 0f, 1f) * Math.max(ClientWeather.gloom, 0.5f) * (1f - CloudLayer.above());
        return Mth.clamp(Math.max(Math.max(rainLevel, byCloud), CloudLayer.inside() * 0.95f), 0f, 1f);
    }

    public static Vec3 sky(ClientLevel level, Vec3 c, float partialTick) {
        if (!ClientWeather.active(level)) return c;
        int s = ClientWeather.season;
        float k = seasonal();
        float[] t = SKY_TINT[s];
        double r = c.x * Mth.lerp(k, 1f, t[0]), g = c.y * Mth.lerp(k, 1f, t[1]), b = c.z * Mth.lerp(k, 1f, t[2]);
        double luma = r * 0.3 + g * 0.59 + b * 0.11;
        float pale = SKY_PALE[s] * k;
        r = Mth.lerp(pale, r, luma);
        g = Mth.lerp(pale, g, luma);
        b = Mth.lerp(pale, b, luma);
        // Grey weather is grey, not dark blue: wash the colour out (vanilla only darkens it towards grey).
        float grey = !ClientConfig.GREY_SKIES.get() ? 0f : Mth.clamp((ClientWeather.gloom - 0.15f) / 0.6f, 0f, 1f) * 0.75f * (1f - CloudLayer.above());
        if (grey > 0.001f) {
            double l = r * 0.3 + g * 0.59 + b * 0.11;
            r = Mth.lerp(grey, r, l * 1.02);
            g = Mth.lerp(grey, g, l * 1.03);
            b = Mth.lerp(grey, b, l * 1.08);
        }
        float fog = CloudLayer.inside();
        if (fog > 0.001f) {
            float[] f = denseFogColor(level.getTimeOfDay(partialTick));
            float w = fog * 0.9f;
            r = Mth.lerp(w, r, f[0]);
            g = Mth.lerp(w, g, f[1]);
            b = Mth.lerp(w, b, f[2]);
        }
        return new Vec3(r, g, b);
    }

    /** Sunrise/sunset glow: seasonal colour and strength, fading behind cloud and fog. Changes the array in place. */
    public static float[] sunrise(float[] col) {
        ClientLevel level = Minecraft.getInstance().level;
        if (col == null || !ClientWeather.active(level)) return col;
        int s = ClientWeather.season;
        float k = seasonal();
        float[] t = SUNSET_TINT[s];
        col[0] = Math.min(1f, col[0] * Mth.lerp(k, 1f, t[0]));
        col[1] = Math.min(1f, col[1] * Mth.lerp(k, 1f, t[1]));
        col[2] = Math.min(1f, col[2] * Mth.lerp(k, 1f, t[2]));
        float hidden = Math.max(sunHidden(level, level.getRainLevel(1f)), ClientWeather.gloom * 0.7f);
        col[3] = Mth.clamp(col[3] * Mth.lerp(k, 1f, SUNSET_STRENGTH[s]) * (1f - hidden * 0.9f) * (1f - CloudLayer.inside() * 0.9f), 0f, 1f);
        return col;
    }

    public static float stars(Level level, float brightness) {
        if (!ClientWeather.active(level)) return brightness;
        return Math.min(1f, brightness * Mth.lerp(seasonal(), 1f, STARS[ClientWeather.season]));
    }

    private static float daylight(float timeOfDay) {
        float day = Mth.clamp(Mth.cos(timeOfDay * Mth.TWO_PI) * 2f + 0.5f, 0f, 1f);
        return 0.07f + 0.93f * day;
    }

    /** Light mist: near-white by day, dark by night. */
    public static float[] mistColor(float timeOfDay) {
        float k = daylight(timeOfDay);
        return new float[] {0.88f * k, 0.90f * k, 0.93f * k};
    }

    /** Dense fog: a heavier, darker grey. */
    public static float[] denseFogColor(float timeOfDay) {
        float k = daylight(timeOfDay);
        return new float[] {0.66f * k, 0.68f * k, 0.71f * k};
    }
}
