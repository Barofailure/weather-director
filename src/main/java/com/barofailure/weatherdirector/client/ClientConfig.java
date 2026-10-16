package com.barofailure.weatherdirector.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Per-player settings (config/weather_director-client.toml, also in Mods -> Weather Director -> Config).
 * The server decides WHAT the weather is; each player decides how much of it their game draws.
 * Vanilla settings are respected too: Particles (All / Decreased / Minimal) scales the ground fog, a manual
 * "Clouds: Off" stays off, "Hide lightning flashes" is handled by the game itself.
 */
public final class ClientConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue CONTROL_CLOUDS;
    public static final ModConfigSpec.BooleanValue SHAPE_CLOUDS;
    public static final ModConfigSpec.BooleanValue KEEP_CLOUDS_ON;
    public static final ModConfigSpec.BooleanValue GREY_SKIES;
    public static final ModConfigSpec.BooleanValue SEASONAL_SKY;
    public static final ModConfigSpec.DoubleValue SEASONAL_SKY_STRENGTH;
    public static final ModConfigSpec.BooleanValue WIND_ON_RAIN;
    public static final ModConfigSpec.DoubleValue WIND_STRENGTH;
    public static final ModConfigSpec.BooleanValue LIGHT_DRIZZLE;
    public static final ModConfigSpec.BooleanValue GROUND_FOG;
    public static final ModConfigSpec.DoubleValue GROUND_FOG_AMOUNT;
    public static final ModConfigSpec.IntValue PARTICLE_BUDGET;
    public static final ModConfigSpec.BooleanValue VIEW_FOG;
    public static final ModConfigSpec.DoubleValue VIEW_FOG_STRENGTH;
    public static final ModConfigSpec.BooleanValue CLOUD_LAYER;
    public static final ModConfigSpec.BooleanValue WIND_SOUND;
    public static final ModConfigSpec.DoubleValue WIND_VOLUME;
    public static final ModConfigSpec.BooleanValue RAIN_FOLLOWS_STRENGTH;
    public static final ModConfigSpec.BooleanValue MUTE_AMBIENTSOUNDS_WIND;
    public static final ModConfigSpec.BooleanValue INDOOR_WIND;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("clouds");
        CONTROL_CLOUDS = b.comment("Weather decides how much of the sky the clouds cover, how grey they are and where they drift (Better Clouds).")
                .define("controlClouds", true);
        SHAPE_CLOUDS = b.comment("Rain clouds sit lower and form a thicker deck.").define("shapeClouds", true);
        KEEP_CLOUDS_ON = b.comment("Distant Horizons switches the game's clouds off when it starts; turn them back on once per session. A manual 'Clouds: Off' is kept.")
                .define("keepCloudsOn", true);
        b.pop().push("sky");
        GREY_SKIES = b.comment("Overcast and rainy weather greys the sky and hides the sun and stars.").define("greySkies", true);
        SEASONAL_SKY = b.comment("Seasonal sky tint, sunrise/sunset colours and clearer winter stars.").define("seasonalSky", true);
        SEASONAL_SKY_STRENGTH = b.comment("How strong the seasonal sky effects are.").defineInRange("seasonalSkyStrength", 1.0, 0.0, 1.0);
        b.pop().push("rain");
        WIND_ON_RAIN = b.comment("Rain and snow fall at an angle with the wind (Particle Rain).").define("windOnRain", true);
        WIND_STRENGTH = b.comment("How strongly the wind pushes rain and snow.").defineInRange("windStrength", 1.0, 0.0, 2.0);
        LIGHT_DRIZZLE = b.comment("Light rain draws fewer drops than heavy rain (Particle Rain).").define("lightDrizzle", true);
        b.pop().push("fog");
        GROUND_FOG = b.comment("Mist and fog lie on the ground as soft fog (Particle Rain's fog particles).").define("groundFog", true);
        GROUND_FOG_AMOUNT = b.comment("Amount of ground fog. Also scaled by the vanilla Particles setting (Decreased = half, Minimal = none).")
                .defineInRange("groundFogAmount", 1.0, 0.0, 2.0);
        PARTICLE_BUDGET = b.comment("No new ground fog while Particle Rain already has this many particles alive (performance cap).")
                .defineInRange("particleBudget", 6000, 500, 20000);
        VIEW_FOG = b.comment("Fog, thunderstorms and being inside a cloud shorten how far you can see.").define("viewFog", true);
        VIEW_FOG_STRENGTH = b.comment("How much they shorten the view (0 = not at all).").defineInRange("viewFogStrength", 1.0, 0.0, 1.0);
        b.pop().push("cloudLayer");
        CLOUD_LAYER = b.comment("No rain above the cloud deck, fog inside it.").define("cloudLayer", true);
        b.pop().push("sound");
        WIND_SOUND = b.comment("Hear the wind: louder when it blows harder, in the open and up high, quiet indoors.").define("windSound", true);
        WIND_VOLUME = b.comment("How loud the wind is (also follows the game's Weather volume).").defineInRange("windVolume", 1.0, 0.0, 2.0);
        INDOOR_WIND = b.comment("The wind sounds different under a roof and indoors: duller, the house takes the gusts, a whistle at openings.")
                .define("indoorWind", true);
        RAIN_FOLLOWS_STRENGTH = b.comment("Rain and snow sounds are soft in drizzle and full in heavy rain (vanilla, AmbientSounds, Particle Rain).")
                .define("rainFollowsStrength", true);
        MUTE_AMBIENTSOUNDS_WIND = b.comment("With AmbientSounds installed, its own wind (which ignores the weather) is muted so the wind is only heard once. Needs a restart.")
                .define("muteAmbientSoundsWind", true);
        b.pop();
        SPEC = b.build();
    }

    private ClientConfig() {}

    /** Vanilla Particles setting: All 1, Decreased 0.5, Minimal 0. */
    public static float vanillaParticles() {
        ParticleStatus p = Minecraft.getInstance().options.particles().get();
        return p == ParticleStatus.ALL ? 1f : p == ParticleStatus.DECREASED ? 0.5f : 0f;
    }
}
