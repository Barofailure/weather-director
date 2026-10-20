package com.barofailure.weatherdirector.client;

import com.barofailure.weatherdirector.Compat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import org.joml.Vector2f;

/**
 * The weather as shader packs see it: Iris uniforms (registered by IrisUniformsMixin), so a pack can draw the same
 * cloud cover, grey sky, fog and cloud drift as Better Clouds does without shaders. A pack that does not declare them
 * is not affected; one that does gets 0 everywhere while wdActive is 0 and should then fall back to its own weather.
 * <ul>
 * <li>wdActive: 1 while Weather Director drives the sky (overworld of a Weather Director server), else 0</li>
 * <li>wdCover: how much of the sky the clouds cover, 0 clear .. 1 a closed deck (eases over minutes)</li>
 * <li>wdGloom: how grey sky, clouds and fog look, 0 .. 1 (at least rainStrength; overcast 0.55, storms 1)</li>
 * <li>wdLightGloom: how much the daylight is dimmed, 0 .. 1 (overcast dims less than rain)</li>
 * <li>wdFogEnd: blocks at which weather fog (fog, thunderstorms) is opaque, 0 = no weather fog</li>
 * <li>wdCloudOffset: how far the clouds have drifted with the wind, in blocks (x, z), wrapped every 36000 blocks</li>
 * </ul>
 */
public final class ShaderWeather {
    private static final double WRAP = 36000;
    private static final Vector2f OFFSET = new Vector2f();

    private ShaderWeather() {}

    private static ClientLevel level() {
        ClientLevel level = Minecraft.getInstance().level;
        return level != null && ClientWeather.active(level) && ClientConfig.SHADER_WEATHER.get() ? level : null;
    }

    private static float partial() {
        return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
    }

    public static float active() {
        return level() != null && ClientConfig.CONTROL_CLOUDS.get() ? 1f : 0f;
    }

    public static float cover() {
        return active() > 0f ? Mth.clamp(ClientWeather.cover, 0f, 1f) : 0f;
    }

    public static float gloom() {
        ClientLevel level = level();
        return level == null ? 0f : Mth.clamp(SkyLook.gloom(level, level.getRainLevel(partial())), 0f, 1f);
    }

    public static float lightGloom() {
        ClientLevel level = level();
        return level == null ? 0f : Mth.clamp(SkyLook.lightGloom(level, level.getRainLevel(partial())), 0f, 1f);
    }

    public static float fogEnd() {
        return level() != null && WeatherFog.active() ? WeatherFog.end() : 0f;
    }

    public static Vector2f cloudOffset() {
        if (active() <= 0f) return OFFSET.set(0f, 0f);
        float pt = partial();
        return OFFSET.set((float) wrap(ClientWeather.cloudX(pt)), (float) wrap(ClientWeather.cloudZ(pt)));
    }

    private static double wrap(double v) {
        return v - Math.floor(v / WRAP) * WRAP;
    }

    /**
     * False while a shader pack draws the sky and Better Clouds does not draw under it: the pack's clouds are where the
     * pack puts them, so there is no deck of ours to fly into or to rain from (CloudLayer stays out of the way).
     */
    public static boolean ourCloudsDrawn() {
        if (!Compat.on(Compat.IRIS) || !IrisState.shadersOn()) return true;
        return Compat.on(Compat.BETTER_CLOUDS) && BetterCloudsHeights.drawing();
    }
}
