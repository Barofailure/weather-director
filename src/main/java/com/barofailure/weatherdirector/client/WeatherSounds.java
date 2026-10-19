package com.barofailure.weatherdirector.client;

import com.barofailure.weatherdirector.Compat;
import com.barofailure.weatherdirector.WeatherDirectorMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Weather you can hear. Wind: AmbientSounds' recorded wind (two breezes, two strong winds, a storm howl) played by us so
 * its loudness follows the server's one global wind, its gusts and the height. Each pair blends slowly from one recording
 * to the other, so no loop is heard repeating. Where the camera is (WindPlace) sets loudness and brightness: full in the
 * open, quieter and duller under a roof, quiet and muffled indoors (a real low-pass filter, WindFilter), silent in caves,
 * sealed rooms, deep underground and underwater. AmbientSounds' own generic wind is replaced by a built-in resource pack;
 * its badlands wind and wind in the leaves stay, following our wind and place too.
 * Rain: rain and snow sounds of the game, AmbientSounds and Particle Rain get quieter in drizzle and louder in heavy
 * rain (they otherwise play at one fixed volume). Rain and thunder are quieter under a roof and indoors, thunder duller,
 * both silent deep underground; thunder arrives after the flash at the speed of sound. All of it only while a Weather
 * Director server is sending.
 */
public final class WeatherSounds {
    /** Our wind events (defined by the built-in pack, files from AmbientSounds): breeze a/b, strong a/b, storm howl. */
    private static final SoundEvent[] LAYERS = {event("wind.breeze_a"), event("wind.breeze_b"), event("wind.strong_a"),
            event("wind.strong_b"), event("wind.howl")};
    /** AmbientSounds' wind flavours that stay and follow our wind (its generic wind is replaced). */
    private static final Set<ResourceLocation> FLAVOUR = Set.of(rl("ambientsounds", "wind.mesa"), rl("ambientsounds", "wind.wind-in-leaves"));
    /** Rain and snow beds whose volume follows the rain strength. */
    private static final Set<ResourceLocation> RAIN = Set.of(
            ResourceLocation.withDefaultNamespace("weather.rain"), ResourceLocation.withDefaultNamespace("weather.rain.above"),
            rl("ambientsounds", "weather.rain1"), rl("ambientsounds", "weather.rain2"),
            rl("particlerain", "weather.snow"), rl("particlerain", "weather.snow.above"),
            rl("particlerain", "weather.sandstorm"), rl("particlerain", "weather.sandstorm.above"));
    /** Lightning (vanilla, also as played by Distant Thunders). */
    private static final Set<ResourceLocation> BOLT = Set.of(ResourceLocation.withDefaultNamespace("entity.lightning_bolt.thunder"),
            ResourceLocation.withDefaultNamespace("entity.lightning_bolt.impact"));
    /** AmbientSounds' storm loops. */
    private static final Set<ResourceLocation> STORM = Set.of(rl("ambientsounds", "weather.storm-close"), rl("ambientsounds", "weather.storm-away"));
    /** Thunder already held back once (played again by the sound engine when its delay is over). */
    private static final Set<SoundInstance> delayed = Collections.newSetFromMap(new WeakHashMap<>());
    /** Blocks per tick: the speed of sound (343 m/s). */
    private static final double SOUND_SPEED = 343.0 / 20.0;

    private static final WindLoop[] loops = new WindLoop[LAYERS.length];
    /** Slow blends between the two recordings of a pair, and the howl's swell (independent, so seams never line up). */
    private static final SmoothNoise breezeBlend = new SmoothNoise(23f), strongBlend = new SmoothNoise(29f), howlSwell = new SmoothNoise(11f);
    private static float t;
    /** Wind strength here (global wind plus height) and the place values, for the flavour sounds and the debug log. */
    private static float windHere, placeGain = 1f, placeHf = 1f;
    private static long lastRainLog;
    /** Ticks until stopped loops may be started again (a game without a working sound device never keeps one). */
    private static int retry;

    private WeatherSounds() {}

    private static ResourceLocation rl(String ns, String path) {
        return ResourceLocation.fromNamespaceAndPath(ns, path);
    }

    private static SoundEvent event(String path) {
        return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(WeatherDirectorMod.MOD_ID, path));
    }

    /** The wind plays when it is switched on and AmbientSounds (whose recordings it uses) is installed. */
    public static boolean windOn() {
        return ClientConfig.WIND_SOUND.get() && Compat.on(Compat.AMBIENTSOUNDS);
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        boolean ready = mc.level != null && mc.player != null && ClientWeather.active(mc.level);
        if (ready && (ClientConfig.MUFFLE_INDOORS.get() || ClientConfig.WIND_SOUND.get() && ClientConfig.INDOOR_WIND.get())) WindPlace.tick(mc);
        if (!ready || !windOn()) {
            fadeOut();
            return;
        }
        t += 0.05f;
        double y = mc.gameRenderer.getMainCamera().getPosition().y;
        // Up on mountains and peaks it blows harder than in the valley.
        float ws = Mth.clamp(ClientWeather.windSpeed + 0.35f * smooth((float) y, 110f, 220f), 0f, 2f);
        windHere = ws;
        float vol = (float) (double) ClientConfig.WIND_VOLUME.get();
        float wave = 0.6f * Mth.sin(t * 0.31f) + 0.4f * Mth.sin(t * 0.113f + 1.7f);
        // Breeze: nothing below ~5 km/h, then it fades in. Light wind is only heard now and then (a gust gate that
        // opens more often as the wind rises, always open from ~33 km/h), like a real breeze you do not always notice.
        float strongMix = Mth.clamp((ws - 0.3f) / 0.7f, 0f, 1f);
        float openGate = smooth(ws, 0.1f, 0.55f);
        float threshold = 0.72f * (1f - openGate);
        float gate = openGate >= 1f ? 1f : smooth(gust.value(t), threshold - 0.12f, threshold + 0.12f);
        // The breeze recordings are quiet by nature (~12 dB below the strong ones), so they play near full volume.
        float softV = smooth(ws, 0.08f, 0.5f) * gate * (1f - 0.6f * strongMix) * (0.94f + 0.06f * wave);
        // Strong wind: barely in rain, clear in heavy rain, full in a thunderstorm.
        float strongV = strongMix * strongMix * 0.85f * (0.8f + 0.2f * wave);
        // Storm howl on top in the strongest winds, swelling and fading.
        float howlV = smooth(ws, 0.7f, 1.4f) * 0.55f * (0.6f + 0.4f * howlSwell.value(t));
        float[] place = place(mc, y);
        placeGain = place[0];
        placeHf = place[1];
        if (retry > 0) retry--;
        if (!keepAll(mc)) return;
        // Recordings keep their character: only a slight pitch rise with the wind, a little deeper indoors.
        float deeper = 1f - 0.08f * WindPlace.indoor;
        float softPitch = (0.97f + 0.05f * Mth.clamp(ws, 0f, 1f)) * deeper;
        float strongPitch = (0.96f + 0.06f * Mth.clamp(ws, 0f, 1.5f) + 0.02f * wave) * deeper;
        // Equal-power blends: the pair always sums to the same loudness.
        float b = breezeBlend.value(t) * Mth.HALF_PI, s = strongBlend.value(t) * Mth.HALF_PI;
        float g = place[0] * vol;
        loops[0].target(softV * Mth.cos(b) * g, softPitch, place[1]);
        loops[1].target(softV * Mth.sin(b) * g, softPitch, place[1]);
        loops[2].target(strongV * Mth.cos(s) * g, strongPitch, place[1]);
        loops[3].target(strongV * Mth.sin(s) * g, strongPitch, place[1]);
        loops[4].target(howlV * g, strongPitch, place[1]);
        if (mc.level.getGameTime() % 100 == 0) {
            WeatherDirectorMod.LOG.debug("weather sounds: wind {} (here {}) | open {} sheltered {} indoor {} (roof {} sides {} outer walls {} sky {}) | place gain {} hf {} filter {} | breeze {}/{} strong {}/{} howl {} | rain level {} -> rain sound x{} | inner {} deep {} | rain place x{} thunder place x{}",
                    ClientWeather.windSpeed, ws, WindPlace.open, WindPlace.sheltered, WindPlace.indoor, WindPlace.roof, WindPlace.sides, WindPlace.exterior, WindPlace.sky,
                    place[0], place[1], WindFilter.working(),
                    loops[0].level(), loops[1].level(), loops[2].level(), loops[3].level(), loops[4].level(),
                    mc.level.getRainLevel(1f), rainFactor(mc.level.getRainLevel(1f)), WindPlace.inner, WindPlace.deep,
                    placeScale(0.85f, 0.5f, 0.3f), placeScale(0.85f, 0.5f, 0.35f));
        }
    }

    /**
     * {loudness, brightness} for where the camera is: the open air full (a bit more up high) and bright; under a roof
     * quieter and duller; indoors quiet and muffled; caves, sealed rooms, deep underground and underwater silent.
     */
    private static float[] place(Minecraft mc, double y) {
        if (mc.gameRenderer.getMainCamera().getFluidInCamera() != FogType.NONE) return new float[] {0f, 1f};
        float height = 0.75f + 0.25f * Mth.clamp((float) (y - 62) / 90f, 0f, 1f);
        if (!ClientConfig.INDOOR_WIND.get()) return new float[] {ClientWeather.skyExposure * height, 1f};
        float gain = (WindPlace.open * height + WindPlace.sheltered * 0.7f + WindPlace.indoor * 0.35f) * (1f - WindPlace.deep);
        float heard = WindPlace.open + WindPlace.sheltered + WindPlace.indoor;
        float hf = heard < 0.01f ? 1f : (WindPlace.open + WindPlace.sheltered * 0.35f + WindPlace.indoor * 0.08f) / heard;
        return new float[] {gain < 0.005f ? 0f : gain, hf};
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

    /** Keeps all wind layers playing; if one stopped, all restart together. */
    private static boolean keepAll(Minecraft mc) {
        boolean ok = true;
        for (WindLoop l : loops) ok &= l != null && !l.isStopped() && mc.getSoundManager().isActive(l);
        if (ok) return true;
        if (retry > 0) return false;
        retry = 40;
        for (WindLoop l : loops) if (l != null) l.end();
        for (int i = 0; i < loops.length; i++) loops[i] = new WindLoop(LAYERS[i], i);
        for (WindLoop l : loops) mc.getSoundManager().play(l);
        return false;
    }

    private static void fadeOut() {
        for (WindLoop l : loops) if (l != null) l.fade();
    }

    public static void reset() {
        for (WindLoop l : loops) if (l != null) l.end();
        java.util.Arrays.fill(loops, null);
    }

    /** Volume factor for a sound the game is about to play or keep playing (1 = unchanged). */
    public static float volumeScale(SoundInstance sound) {
        ResourceLocation id = sound.getLocation();
        boolean rain = RAIN.contains(id), thunder = !rain && (BOLT.contains(id) || STORM.contains(id));
        boolean flavour = !rain && !thunder && FLAVOUR.contains(id);
        if (!rain && !thunder && !flavour) return 1f;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !ClientWeather.active(mc.level)) return 1f;
        // Badlands wind and wind in the leaves: soft in calm air, full from a strong wind, and where the wind is heard.
        if (flavour) return windOn() ? (0.3f + 0.7f * Mth.clamp(windHere, 0f, 1f)) * placeGain : 1f;
        float scale = 1f;
        // Drizzle (0.3) 0.4, rain (0.65) 0.7, heavy rain (1.0) full; while rain fades in or out it stays soft.
        if (rain && ClientConfig.RAIN_FOLLOWS_STRENGTH.get()) scale = rainFactor(mc.level.getRainLevel(1f));
        if (ClientConfig.MUFFLE_INDOORS.get()) scale *= rain ? placeScale(0.85f, 0.5f, 0.3f) : placeScale(0.85f, 0.5f, 0.35f);
        if (BOLT.contains(id)) WeatherDirectorMod.LOG.debug("weather sounds: {} plays, volume x{}", id.getPath(), scale);
        else if (rain && mc.level.getGameTime() - lastRainLog >= 100) {
            lastRainLog = mc.level.getGameTime();
            WeatherDirectorMod.LOG.debug("weather sounds: rain sound {} volume x{}", id, scale);
        }
        return scale;
    }

    /** Pitch factor: thunder indoors sounds deeper and duller (1 = unchanged). */
    public static float pitchScale(SoundInstance sound) {
        if (!ClientConfig.MUFFLE_INDOORS.get() || !BOLT.contains(sound.getLocation())) return 1f;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !ClientWeather.active(mc.level)) return 1f;
        return 1f - 0.15f * Mth.clamp(WindPlace.indoor + WindPlace.inner, 0f, 1f);
    }

    /** Ticks to hold a lightning sound back so it comes after the flash (0 = play now). */
    public static int thunderDelay(SoundInstance sound) {
        if (!ClientConfig.THUNDER_DELAY.get() || !BOLT.contains(sound.getLocation()) || sound.isRelative() || delayed.remove(sound)) return 0;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !ClientWeather.active(mc.level)) return 0;
        double distance = mc.gameRenderer.getMainCamera().getPosition().distanceTo(new Vec3(sound.getX(), sound.getY(), sound.getZ()));
        int ticks = (int) Math.round(distance / SOUND_SPEED);
        if (ticks < 1) return 0;
        delayed.add(sound);
        WeatherDirectorMod.LOG.debug("weather sounds: {} {} blocks away, {} ticks after the flash, place open {} sheltered {} indoor {} inner {} deep {}",
                sound.getLocation().getPath(), Math.round(distance), ticks, WindPlace.open, WindPlace.sheltered, WindPlace.indoor, WindPlace.inner, WindPlace.deep);
        return ticks;
    }

    /** Open air full, under a roof less, indoors and inside less again; nothing deep underground. */
    private static float placeScale(float sheltered, float indoor, float inner) {
        float w = WindPlace.open + WindPlace.sheltered * sheltered + WindPlace.indoor * indoor + WindPlace.inner * inner;
        float scale = Mth.clamp(w, 0f, 1f) * (1f - WindPlace.deep);
        return scale < 0.01f ? 0f : scale;
    }

    private static float rainFactor(float rainLevel) {
        float level = Mth.clamp(rainLevel, 0f, 1f);
        return level >= 0.3f ? 0.4f + 0.6f * (level - 0.3f) / 0.7f : 0.4f * level / 0.3f;
    }

    /** A looping wind layer that eases to the loudness and brightness the weather asks for and ends itself when silent. */
    static final class WindLoop extends AbstractTickableSoundInstance {
        private final int slot;
        private float targetVolume, targetPitch = 1f, targetHf = 1f, hf = 1f, sentHf = -1f;
        private boolean ending;

        WindLoop(SoundEvent event, int slot) {
            super(event, SoundSource.WEATHER, SoundInstance.createUnseededRandom());
            this.slot = slot;
            looping = true;
            delay = 0;
            volume = 0.001f;
            relative = true;
            attenuation = Attenuation.NONE;
        }

        void target(float v, float p, float h) {
            targetVolume = v;
            targetPitch = p;
            targetHf = h;
        }

        void end() {
            ending = true;
            targetVolume = 0f;
        }

        /** Fade to silence, pitch unchanged. Uses the raw fields: getPitch()/getVolume() read the resolved sound,
         *  which is still null when the game never played the loop (no audio device, sound engine not loaded). */
        void fade() {
            targetVolume = 0f;
            targetPitch = pitch;
        }

        float level() {
            return volume;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            volume += (targetVolume - volume) * 0.04f;
            pitch += (targetPitch - pitch) * 0.04f;
            hf += (targetHf - hf) * 0.04f;
            // The source exists from the first tick after play; send again only when the brightness really moved.
            if ((Math.abs(hf - sentHf) > 0.01f || sentHf < 0f) && WindFilter.apply(this, slot, hf)) sentHf = hf;
            if (ending && volume < 0.002f) stop();
        }
    }
}
