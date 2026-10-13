package com.barofailure.weatherdirector.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import pigcart.particlerain.ParticleLoader;
import pigcart.particlerain.config.ParticleData;

/**
 * Mist and fog as ground fog: Particle Rain's own low fog particles ("fog" lying flat, "vertical_fog" standing up,
 * soft 2-block pieces layered in depth), which it normally shows only in wet/spooky biomes. While our weather has mist
 * or fog, they may appear in every biome, as many as the level calls for: light and white in mist (on land and over
 * water), dense and grey in fog (on land only). Particle Rain's own biome fog stays as the baseline.
 * Only loaded when Particle Rain is installed.
 */
public final class GroundFog {
    /** Particle Rain spawn chance per surface sample at full mist / full fog (its own swamp fog uses 0.1). */
    private static final float MIST_DENSITY = 0.025f;
    private static final float FOG_DENSITY = 0.45f;

    private static Map<String, ParticleData> seen;
    private static final List<ParticleData> FOG_DATA = new ArrayList<>();

    private GroundFog() {}

    public static float mist() {
        return ClientWeather.active() ? ClientWeather.mistLevel * (1f - CloudLayer.above()) : 0f;
    }

    public static float fog() {
        return ClientWeather.active() ? ClientWeather.fogLevel * (1f - CloudLayer.above()) : 0f;
    }

    public static boolean on() {
        return ClientConfig.GROUND_FOG.get() && (mist() > 0.01f || fog() > 0.01f);
    }

    /**
     * Player amount x vanilla Particles setting x performance: no extra fog while Particle Rain is at the particle
     * budget, and less of it while the frame rate is under 30.
     */
    private static float scale() {
        if (pigcart.particlerain.ParticleSpawner.particleCount >= ClientConfig.PARTICLE_BUDGET.get()) return 0f;
        float fps = Math.max(1, Minecraft.getInstance().getFps());
        return ClientConfig.GROUND_FOG_AMOUNT.get().floatValue() * ClientConfig.vanillaParticles() * Math.min(1f, fps / 30f);
    }

    /** Particle Rain's two ground-fog particle definitions (looked up again when its packs reload). */
    private static List<ParticleData> fogData() {
        Map<String, ParticleData> all = ParticleLoader.particles;
        if (all != seen) {
            seen = all;
            FOG_DATA.clear();
            if (all != null) {
                for (String id : new String[] {"fog", "vertical_fog"}) {
                    ParticleData d = all.get(id);
                    if (d != null) FOG_DATA.add(d);
                }
            }
        }
        return FOG_DATA;
    }

    public static boolean isFog(ParticleData data) {
        return data != null && fogData().contains(data);
    }

    public static boolean isFogBiomeList(Object list) {
        for (ParticleData d : fogData()) if (d.biomeList == list) return true;
        return false;
    }

    public static boolean isFogBlockList(Object list) {
        for (ParticleData d : fogData()) if (d.blockList == list) return true;
        return false;
    }

    public static boolean isFogPrecipitation(Object list) {
        for (ParticleData d : fogData()) if (d.precipitation == list) return true;
        return false;
    }

    public static float density(float original) {
        return Math.max(original, (MIST_DENSITY * mist() + FOG_DENSITY * fog()) * scale());
    }

    /** Mist lies mostly over water (land gets this share of it); fog is thick on land and never on water. */
    private static final float MIST_ON_LAND = 0.2f;
    /** Fog layer: pieces spread up to this many blocks above the ground (Particle Rain: within 1) and drawn bigger. */
    private static final float FOG_EXTRA_HEIGHT = 2.2f;
    private static final float FOG_SIZE = 1.5f;

    /** Ground block check: water only for mist; land for fog fully, for mist only partly. */
    public static boolean ground(Holder<Block> block, boolean original) {
        float m = mist(), f = fog();
        var random = Minecraft.getInstance().level.random;
        if (block.value() == Blocks.WATER) {
            return m > 0.01f && random.nextFloat() < m / (m + f);
        }
        if (!original) return false;
        return random.nextFloat() < (m * MIST_ON_LAND + f) / Math.max(0.001f, m + f);
    }

    /** Share of the ground fog that is dense fog (0 = all mist, 1 = all fog). */
    public static float fogShare() {
        float m = mist(), f = fog();
        return f / Math.max(0.001f, m + f);
    }

    /** Extra spawn height for a new fog piece: the dense fog layer is deeper than mist. */
    public static double extraHeight() {
        return Minecraft.getInstance().level.random.nextDouble() * FOG_EXTRA_HEIGHT * fogShare();
    }

    /** Size multiplier for a new fog piece. */
    public static float sizeScale() {
        return 1f + (FOG_SIZE - 1f) * fogShare();
    }

    /** White in mist, grey in fog; the world lighting dims them at night (see SkyLook.particleColor). */
    public static void tint(SingleQuadParticle p) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        float m = mist(), f = fog();
        float share = f / Math.max(0.001f, m + f);
        float t = level.getTimeOfDay(1f);
        float[] white = SkyLook.particleColor(t, false), grey = SkyLook.particleColor(t, true);
        float v = 0.92f + level.random.nextFloat() * 0.08f;
        p.setColor(Mth.lerp(share, white[0], grey[0]) * v, Mth.lerp(share, white[1], grey[1]) * v, Mth.lerp(share, white[2], grey[2]) * v);
    }

    /** Ground fog creeps with the wind, much slower than the wind itself. Blocks per tick. */
    public static float drift() {
        return 0.008f + 0.035f * ClientWeather.windSpeed;
    }
}
