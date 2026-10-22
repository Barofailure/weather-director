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
 * <li>wdGroundFog, wdMist: how much fog / mist lies on the ground, 0 .. 1 (the ground fog setting and amount included;
 * 0 above the clouds). Without shaders this is Particle Rain's fog particles, which a pack can draw itself as low fog.</li>
 * <li>wdCloudOffset: how far the clouds have drifted with the wind, in blocks (x, z), wrapped every 36000 blocks</li>
 * <li>wdFogTop: the top of the mist / ground fog layer, in blocks above (+) or below (-) the eye; the layer lies on the
 * ground around the player (GroundLevel), so seen from above the fog lies down there, as Particle Rain's does.</li>
 * <li>wdCloudBottom, wdCloudTop: the cloud deck's heights (blocks) for a pack listed in shaderDeckPacks that draws it;
 * 0 for every other pack. Rain stops at its top, cloud fog inside it, as without shaders.</li>
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

    public static float groundFog() {
        return level() != null ? ground(ClientWeather.fogLevel) : 0f;
    }

    public static float mist() {
        return level() != null ? ground(ClientWeather.mistLevel) : 0f;
    }

    private static float ground(float level) {
        if (!ClientConfig.GROUND_FOG.get()) return 0f;
        return Mth.clamp(level * (1f - CloudLayer.above()) * ClientConfig.GROUND_FOG_AMOUNT.get().floatValue(), 0f, 1f);
    }

    public static float fogTop() {
        if (level() == null) return 0f;
        float top = GroundLevel.fogTop();
        return Float.isNaN(top) ? 0f : top - (float) Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().y;
    }

    public static float cloudBottom() {
        return level() != null && shaderDrawsDeck() ? CloudLayer.bottom() : 0f;
    }

    public static float cloudTop() {
        return level() != null && shaderDrawsDeck() ? CloudLayer.top() : 0f;
    }

    /** The active shader pack draws our cloud deck itself (named in shaderDeckPacks). */
    static boolean shaderDrawsDeck() {
        if (!Compat.on(Compat.IRIS) || !IrisState.shadersOn()) return false;
        String name = IrisState.packName().toLowerCase(java.util.Locale.ROOT);
        for (String p : ClientConfig.SHADER_DECK_PACKS.get())
            if (!p.isBlank() && name.contains(p.toLowerCase(java.util.Locale.ROOT))) return true;
        return false;
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
     * False while a shader pack draws the sky, Better Clouds does not draw under it and the pack does not draw our deck
     * (shaderDeckPacks): its clouds are where the pack puts them, so there is no deck of ours to fly into or to rain
     * from (CloudLayer stays out of the way).
     */
    public static boolean ourCloudsDrawn() {
        if (!Compat.on(Compat.IRIS) || !IrisState.shadersOn()) return true;
        return (Compat.on(Compat.BETTER_CLOUDS) && BetterCloudsHeights.drawing()) || shaderDrawsDeck();
    }
}
