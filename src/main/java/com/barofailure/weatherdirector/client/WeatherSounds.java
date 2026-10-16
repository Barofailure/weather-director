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
import net.minecraft.world.phys.Vec3;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Weather you can hear. Wind: a soft breeze and a strong gusty wind whose loudness follows the server's one global wind.
 * Each comes in three versions that play together, in step, and blend by where the camera is (WindPlace): in the open,
 * sheltered under a roof (duller, the roof drums in gusts) and indoors (muffled pressure on the walls, thumps and a thin
 * whistle with the gusts, coming from the nearest opening).
 * Rain: rain and snow sounds of the game, AmbientSounds and Particle Rain get quieter in drizzle and louder in heavy
 * rain (they otherwise play at one fixed volume). Rain and thunder are quieter under a roof and indoors, thunder duller,
 * both silent deep underground; thunder arrives after the flash at the speed of sound. All of it only while a Weather
 * Director server is sending.
 */
public final class WeatherSounds {
    private static final String[] PLACES = {"", "_sheltered", "_indoor"};
    private static final SoundEvent[] SOFT = events("wind.soft"), STRONG = events("wind.strong");
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

    /** [place][layer]: place 0 open, 1 sheltered, 2 indoors; layer 0 breeze, 1 strong. Started together so gusts line up. */
    private static final WindLoop[][] loops = new WindLoop[3][2];
    private static float t;
    private static long lastRainLog;
    /** Ticks until stopped loops may be started again (a game without a working sound device never keeps one). */
    private static int retry;

    private WeatherSounds() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(WeatherDirectorMod.MOD_ID, path);
    }

    private static ResourceLocation rl(String ns, String path) {
        return ResourceLocation.fromNamespaceAndPath(ns, path);
    }

    private static SoundEvent[] events(String base) {
        SoundEvent[] e = new SoundEvent[3];
        for (int i = 0; i < 3; i++) e[i] = SoundEvent.createVariableRangeEvent(id(base + PLACES[i]));
        return e;
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        boolean ready = mc.level != null && mc.player != null && ClientWeather.active(mc.level);
        if (ready && (ClientConfig.MUFFLE_INDOORS.get() || ClientConfig.WIND_SOUND.get() && ClientConfig.INDOOR_WIND.get())) WindPlace.tick(mc);
        boolean on = ready && ClientConfig.WIND_SOUND.get();
        if (!on) {
            fadeOut();
            return;
        }
        t += 0.05f;
        float ws = Mth.clamp(ClientWeather.windSpeed, 0f, 2f);
        float vol = (float) (double) ClientConfig.WIND_VOLUME.get();
        float wave = 0.6f * Mth.sin(t * 0.31f) + 0.4f * Mth.sin(t * 0.113f + 1.7f);
        // Breeze: nothing below ~5 km/h, then it fades in. Light wind is only heard now and then (a gust gate that
        // opens more often as the wind rises, always open from ~33 km/h), like a real breeze you do not always notice.
        float strongMix = Mth.clamp((ws - 0.3f) / 0.7f, 0f, 1f);
        float openGate = smooth(ws, 0.1f, 0.55f);
        float threshold = 0.72f * (1f - openGate);
        float gate = openGate >= 1f ? 1f : smooth(gust.value(t), threshold - 0.12f, threshold + 0.12f);
        float softV = smooth(ws, 0.08f, 0.5f) * 0.3f * gate * (1f - 0.6f * strongMix) * (0.94f + 0.06f * wave);
        // Strong wind: barely in rain, clear in heavy rain, full in a thunderstorm.
        float strongV = strongMix * strongMix * 0.85f * (0.8f + 0.2f * wave);
        float[] place = placeWeights(mc);
        if (retry > 0) retry--;
        if (!keepAll(mc)) return;
        float softPitch = 0.95f + 0.1f * ws, strongPitch = 0.9f + 0.15f * ws + 0.03f * wave;
        Vec3 from = WindPlace.from;
        for (int i = 0; i < 3; i++) {
            loops[i][0].target(softV * place[i] * vol, softPitch);
            loops[i][1].target(strongV * place[i] * vol, strongPitch);
            if (i == 2) {
                loops[i][0].at(from);
                loops[i][1].at(from);
            }
        }
        if (mc.level.getGameTime() % 100 == 0) {
            WeatherDirectorMod.LOG.debug("weather sounds: wind {} | open {} sheltered {} indoor {} (roof {} sides {} outer walls {} sky {}) | breeze {}/{}/{} strong {}/{}/{} | rain level {} -> rain sound x{} | inner {} deep {} | rain place x{} thunder place x{}",
                    ws, place[0], place[1], place[2], WindPlace.roof, WindPlace.sides, WindPlace.exterior, WindPlace.sky,
                    loops[0][0].level(), loops[1][0].level(), loops[2][0].level(),
                    loops[0][1].level(), loops[1][1].level(), loops[2][1].level(),
                    mc.level.getRainLevel(1f), rainFactor(mc.level.getRainLevel(1f)), WindPlace.inner, WindPlace.deep,
                    placeScale(0.85f, 0.5f, 0.3f), placeScale(0.85f, 0.5f, 0.35f));
        }
    }

    /** Loudness of the open, sheltered and indoor versions; nothing underwater or in lava, a bit more up high. */
    private static float[] placeWeights(Minecraft mc) {
        if (mc.gameRenderer.getMainCamera().getFluidInCamera() != FogType.NONE) return new float[] {0f, 0f, 0f};
        double y = mc.gameRenderer.getMainCamera().getPosition().y;
        float height = 0.75f + 0.25f * Mth.clamp((float) (y - 62) / 90f, 0f, 1f);
        if (!ClientConfig.INDOOR_WIND.get()) return new float[] {ClientWeather.skyExposure * height, 0f, 0f};
        return new float[] {WindPlace.open * height, WindPlace.sheltered * 0.9f, WindPlace.indoor};
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

    /** Keeps all six loops playing; if one stopped, all restart together so their gusts stay in step. */
    private static boolean keepAll(Minecraft mc) {
        boolean ok = true;
        for (WindLoop[] row : loops) for (WindLoop l : row) ok &= l != null && !l.isStopped() && mc.getSoundManager().isActive(l);
        if (ok) return true;
        if (retry > 0) return false;
        retry = 40;
        for (WindLoop[] row : loops) for (WindLoop l : row) if (l != null) l.end();
        for (int i = 0; i < 3; i++) {
            loops[i][0] = new WindLoop(SOFT[i], i == 2);
            loops[i][1] = new WindLoop(STRONG[i], i == 2);
        }
        for (WindLoop[] row : loops) for (WindLoop l : row) mc.getSoundManager().play(l);
        return false;
    }

    private static void fadeOut() {
        for (WindLoop[] row : loops) for (WindLoop l : row) if (l != null) l.fade();
    }

    public static void reset() {
        for (WindLoop[] row : loops) for (WindLoop l : row) if (l != null) l.end();
        for (WindLoop[] row : loops) java.util.Arrays.fill(row, null);
    }

    /** Volume factor for a sound the game is about to play or keep playing (1 = unchanged). */
    public static float volumeScale(SoundInstance sound) {
        ResourceLocation id = sound.getLocation();
        boolean rain = RAIN.contains(id), thunder = !rain && (BOLT.contains(id) || STORM.contains(id));
        if (!rain && !thunder) return 1f;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !ClientWeather.active(mc.level)) return 1f;
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

    /** A looping wind layer that eases to the loudness the weather asks for and ends itself when silent. */
    static final class WindLoop extends AbstractTickableSoundInstance {
        private float targetVolume, targetPitch = 1f;
        private boolean ending;

        /** placed: the indoor versions sit at a point in the room (towards the opening) instead of all around. */
        WindLoop(SoundEvent event, boolean placed) {
            super(event, SoundSource.WEATHER, SoundInstance.createUnseededRandom());
            looping = true;
            delay = 0;
            volume = 0.001f;
            relative = !placed;
            attenuation = Attenuation.NONE;
        }

        /** Where a placed loop plays from; null = right at the listener (no direction). */
        void at(Vec3 p) {
            if (relative) return;
            Vec3 pos = p != null ? p : Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            x = pos.x;
            y = pos.y;
            z = pos.z;
        }

        void target(float v, float p) {
            targetVolume = v;
            targetPitch = p;
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
            if (ending && volume < 0.002f) stop();
        }
    }
}
