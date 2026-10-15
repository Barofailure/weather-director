package com.barofailure.weatherdirector.client;

import com.barofailure.weatherdirector.WeatherDirectorMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import java.util.Set;

/**
 * Weather you can hear. Wind: two loops (a soft breeze and a strong gusty wind) whose loudness follows the server's one
 * global wind, how open the sky is at the camera (quiet indoors and underground, none underwater) and the height.
 * Rain: rain and snow sounds of the game, AmbientSounds and Particle Rain get quieter in drizzle and louder in heavy
 * rain (they otherwise play at one fixed volume). All of it only while a Weather Director server is sending.
 */
public final class WeatherSounds {
    private static final SoundEvent SOFT = SoundEvent.createVariableRangeEvent(id("wind.soft"));
    private static final SoundEvent STRONG = SoundEvent.createVariableRangeEvent(id("wind.strong"));
    /** Rain and snow beds whose volume follows the rain strength. */
    private static final Set<ResourceLocation> RAIN = Set.of(
            ResourceLocation.withDefaultNamespace("weather.rain"), ResourceLocation.withDefaultNamespace("weather.rain.above"),
            rl("ambientsounds", "weather.rain1"), rl("ambientsounds", "weather.rain2"),
            rl("particlerain", "weather.snow"), rl("particlerain", "weather.snow.above"),
            rl("particlerain", "weather.sandstorm"), rl("particlerain", "weather.sandstorm.above"));

    private static WindLoop soft, strong;
    private static float t;
    /** Ticks until a stopped loop may be started again (a game without a working sound device never keeps one). */
    private static int retry;

    private WeatherSounds() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(WeatherDirectorMod.MOD_ID, path);
    }

    private static ResourceLocation rl(String ns, String path) {
        return ResourceLocation.fromNamespaceAndPath(ns, path);
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        boolean on = ClientConfig.WIND_SOUND.get() && mc.level != null && mc.player != null && ClientWeather.active(mc.level);
        if (!on) {
            fadeOut();
            return;
        }
        t += 0.05f;
        float ws = Mth.clamp(ClientWeather.windSpeed, 0f, 2f);
        float exposure = exposure(mc);
        float vol = (float) (double) ClientConfig.WIND_VOLUME.get();
        float wave = 0.6f * Mth.sin(t * 0.31f) + 0.4f * Mth.sin(t * 0.113f + 1.7f);
        // Breeze: nothing below ~5 km/h, then it fades in. Light wind is only heard now and then (a gust gate that
        // opens more often as the wind rises, always open from ~33 km/h), like a real breeze you do not always notice.
        float strongMix = Mth.clamp((ws - 0.3f) / 0.7f, 0f, 1f);
        float open = smooth(ws, 0.1f, 0.55f);
        float threshold = 0.72f * (1f - open);
        float gate = open >= 1f ? 1f : smooth(gust.value(t), threshold - 0.12f, threshold + 0.12f);
        float softV = smooth(ws, 0.08f, 0.5f) * 0.3f * gate * (1f - 0.6f * strongMix) * (0.94f + 0.06f * wave);
        // Strong wind: barely in rain, clear in heavy rain, full in a thunderstorm.
        float strongV = strongMix * strongMix * 0.85f * (0.8f + 0.2f * wave);
        if (retry > 0) retry--;
        soft = keep(mc, soft, SOFT);
        strong = keep(mc, strong, STRONG);
        if (soft == null || strong == null) return;
        soft.target(softV * exposure * vol, 0.95f + 0.1f * ws);
        strong.target(strongV * exposure * vol, 0.9f + 0.15f * ws + 0.03f * wave);
        if (mc.level.getGameTime() % 100 == 0) {
            WeatherDirectorMod.LOG.debug("weather sounds: wind {} exposure {} | breeze {} strong {} | rain level {} -> rain sound x{}",
                    ws, exposure, soft.getVolume(), strong.getVolume(), mc.level.getRainLevel(1f), rainFactor(mc.level.getRainLevel(1f)));
        }
    }

    /** Slow random swells 0..1 for the breeze's gust gate (features ~8 s apart). */
    private static final SmoothNoise gust = new SmoothNoise(8f);

    private static float smooth(float x, float from, float to) {
        float u = Mth.clamp((x - from) / (to - from), 0f, 1f);
        return u * u * (3f - 2f * u);
    }

    static final class SmoothNoise {
        private final float period;
        private final java.util.Random random = new java.util.Random();
        private int cell = Integer.MIN_VALUE;
        private float a, b;

        SmoothNoise(float period) {
            this.period = period;
        }

        float value(float time) {
            int c = Mth.floor(time / period);
            if (c != cell) {
                a = cell == c - 1 ? b : random.nextFloat();
                b = random.nextFloat();
                cell = c;
            }
            float u = time / period - c;
            return Mth.lerp(u * u * (3f - 2f * u), a, b);
        }
    }

    /** 0 inside and underground .. 1 under open sky; a bit more up high; nothing underwater or in lava. */
    private static float exposure(Minecraft mc) {
        if (mc.gameRenderer.getMainCamera().getFluidInCamera() != FogType.NONE) return 0f;
        double y = mc.gameRenderer.getMainCamera().getPosition().y;
        float height = 0.75f + 0.25f * Mth.clamp((float) (y - 62) / 90f, 0f, 1f);
        return ClientWeather.skyExposure * height;
    }

    private static WindLoop keep(Minecraft mc, WindLoop loop, SoundEvent event) {
        if (loop != null && !loop.isStopped() && mc.getSoundManager().isActive(loop)) return loop;
        if (retry > 0) return loop;
        retry = 40;
        WindLoop fresh = new WindLoop(event);
        mc.getSoundManager().play(fresh);
        return fresh;
    }

    private static void fadeOut() {
        if (soft != null) soft.target(0f, soft.getPitch());
        if (strong != null) strong.target(0f, strong.getPitch());
    }

    public static void reset() {
        if (soft != null) soft.end();
        if (strong != null) strong.end();
        soft = strong = null;
    }

    /** Volume factor for a sound the game is about to play or keep playing (1 = unchanged). */
    public static float rainScale(SoundInstance sound) {
        if (!ClientConfig.RAIN_FOLLOWS_STRENGTH.get() || !RAIN.contains(sound.getLocation())) return 1f;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !ClientWeather.active(mc.level)) return 1f;
        // Drizzle (0.3) 0.4, rain (0.65) 0.7, heavy rain (1.0) full; while rain fades in or out it stays soft.
        return rainFactor(mc.level.getRainLevel(1f));
    }

    private static float rainFactor(float rainLevel) {
        float level = Mth.clamp(rainLevel, 0f, 1f);
        return level >= 0.3f ? 0.4f + 0.6f * (level - 0.3f) / 0.7f : 0.4f * level / 0.3f;
    }

    /** A looping wind layer that eases to the loudness the weather asks for and ends itself when silent. */
    static final class WindLoop extends AbstractTickableSoundInstance {
        private float targetVolume, targetPitch = 1f;
        private boolean ending;

        WindLoop(SoundEvent event) {
            super(event, SoundSource.WEATHER, SoundInstance.createUnseededRandom());
            looping = true;
            delay = 0;
            volume = 0.001f;
            relative = true;
            attenuation = Attenuation.NONE;
        }

        void target(float v, float p) {
            targetVolume = v;
            targetPitch = p;
        }

        void end() {
            ending = true;
            targetVolume = 0f;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            volume += (targetVolume - volume) * 0.04f;
            pitch += (targetPitch - pitch) * 0.04f;
            if (ending && volume < 0.002f) stop();
        }
    }
}
