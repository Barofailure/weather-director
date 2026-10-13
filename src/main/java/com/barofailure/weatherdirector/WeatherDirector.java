package com.barofailure.weatherdirector;

import com.barofailure.weatherdirector.net.WeatherSyncPayload;
import java.util.Map;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

/**
 * Owns the overworld weather. Every tick it counts down the current kind and rolls the next one from
 * the config (base weights x season factors); fog is a dawn event (see dawnFog). The vanilla rain/thunder levels are
 * steered towards the kind's targets by ServerLevelMixin; the vanilla raining/thundering flags are kept
 * in line here so sleep, /weather and the vanilla timers never take over.
 */
public final class WeatherDirector {
    static final int TICKS_PER_MINUTE = 1200;
    /** Levels move every STEP_TICKS ticks, so clients get a packet twice a second while ramping, none while steady. */
    static final int STEP_TICKS = 10;
    /** Clients get the look, wind and cloud drift this often (and on every change). */
    static final int SYNC_TICKS = 100;

    static WeatherConfig config = WeatherConfig.defaults();
    private static WeatherState state;
    private static long lastTimeOfDay = -1;
    private static String lastSeason = "NONE";

    private WeatherDirector() {}

    static String reloadConfig() {
        try {
            config = WeatherConfig.load();
            return "Loaded " + WeatherConfig.path().getFileName();
        } catch (Exception e) {
            WeatherDirectorMod.LOG.error("Could not read {}, keeping the previous config", WeatherConfig.path(), e);
            return "Could not read " + WeatherConfig.path().getFileName() + ": " + e.getMessage() + " (kept the previous config)";
        }
    }

    static void serverStarted(MinecraftServer server) {
        reloadConfig();
        ServerLevel overworld = server.overworld();
        state = overworld.getDataStorage().computeIfAbsent(WeatherState.FACTORY, WeatherState.NAME);
        if (state.ticksLeft <= 0) {
            begin(overworld, overworld.isRaining() ? WeatherKind.RAIN : WeatherKind.CLEAR, -1, "first start");
        }
        if (state.windSpeed == 0 && state.windAngle == 0 && state.windTarget == 0) {
            state.windAngle = state.windTarget = newWindTarget(overworld);
            state.windSpeed = config.look(state.kind).wind;
        }
        // Start at the saved kind instead of ramping from whatever vanilla loaded.
        cover = config.look(state.kind).cover;
        overworld.setRainLevel(state.kind.rain);
        overworld.setThunderLevel(state.kind.thunder);
        syncFlags(overworld);
        WeatherDirectorMod.LOG.info("Weather: {} for {} more minutes", state.kind, state.ticksLeft / TICKS_PER_MINUTE);
    }

    static void serverStopped() {
        state = null;
        cover = -1;
        lastTimeOfDay = -1;
    }

    static boolean controls(ServerLevel level) {
        return state != null && level.dimension() == Level.OVERWORLD;
    }

    static void tick(ServerLevel overworld) {
        if (!controls(overworld)) return;
        long timeOfDay = overworld.getDayTime() % 24000L;
        boolean dawn = lastTimeOfDay >= 0 && lastTimeOfDay < config.dawnTime
                && (timeOfDay >= config.dawnTime || timeOfDay < lastTimeOfDay); // second case: sleep skipped to morning
        lastTimeOfDay = timeOfDay;
        if (overworld.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE)) {
            if (dawn) dawnFog(overworld);
            if (--state.ticksLeft <= 0) {
                if (state.kind.lifts() && state.resumeKind != null) {
                    WeatherKind resume = state.resumeKind;
                    begin(overworld, resume, Math.max(1, state.resumeTicks) / (double) TICKS_PER_MINUTE, "fog lifted");
                } else {
                    WeatherKind before = state.kind;
                    WeatherKind next = pickNext(overworld);
                    begin(overworld, next, -1, "season " + Seasons.current(overworld));
                    // Rain just ended: wet ground and humid air, often mist for a while.
                    if (before.wet() && !next.wet() && overworld.random.nextDouble() < config.afterRainMistChance) {
                        interrupt(overworld, WeatherKind.MIST, "after rain");
                    }
                }
            }
        }
        tickWind();
        tickCover();
        if (overworld.getGameTime() % 20 == 0) {
            syncFlags(overworld);
            state.setDirty();
        }
        if (overworld.getGameTime() % SYNC_TICKS == 0) sync(overworld.getServer());
    }

    /** Wind strength eases to the kind's, the direction turns slowly towards its target, the clouds drift with it. */
    private static void tickCover() {
        float target = config.look(state.kind).cover;
        if (cover < 0) cover = target;
        float rate = (float) (1.0 / (Math.max(1.0, config.cloudRampSeconds) * 20.0));
        cover += Mth.clamp(target - cover, -rate, rate);
    }

    private static void tickWind() {
        float speedTarget = config.look(state.kind).wind;
        float rate = (float) (1.0 / (Math.max(1.0, config.rampSeconds) * 20.0));
        state.windSpeed += Mth.clamp(speedTarget - state.windSpeed, -rate, rate);
        float maxTurn = (float) Math.toRadians(config.windTurnDegreesPerMinute) / TICKS_PER_MINUTE;
        state.windAngle += Mth.clamp(Mth.wrapDegrees((float) Math.toDegrees(state.windTarget - state.windAngle)) * Mth.DEG_TO_RAD, -maxTurn, maxTurn);
        state.windAngle = (float) Math.IEEEremainder(state.windAngle, Math.PI * 2);
        float v = cloudSpeed();
        state.cloudX += Mth.cos(state.windAngle) * v;
        state.cloudZ += Mth.sin(state.windAngle) * v;
    }

    /** Cloud drift, blocks per tick. */
    static float cloudSpeed() {
        return (float) ((config.cloudDriftMin + (config.cloudDriftMax - config.cloudDriftMin) * state.windSpeed) / 20.0);
    }

    private static float newWindTarget(ServerLevel overworld) {
        double prevailing = Math.toRadians(config.prevailingWindTowardDegrees);
        return (float) (prevailing + overworld.random.nextGaussian() * Math.toRadians(config.windVariationDegrees));
    }

    static WeatherSyncPayload payload() {
        WeatherConfig.Look look = config.look(state.kind);
        return new WeatherSyncPayload(state.kind.ordinal(), seasonIndex(lastSeason), (float) config.rampSeconds, (float) config.cloudRampSeconds,
                (float) config.fogRampSeconds, cover,
                look.cover, look.gloom, look.haze, look.mist, look.fog, state.windAngle, state.windSpeed, state.cloudX, state.cloudZ,
                cloudSpeed(), (float) config.rainWindScale, snapNext);
    }

    static void sync(MinecraftServer server) {
        if (state == null) return;
        lastSeason = Seasons.current(server.overworld());
        WeatherSyncPayload p = payload();
        snapNext = false;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) send(player, p);
    }

    /** True for the next sync only: clients jump to the new look instead of easing (/wd set ... now). */
    private static boolean snapNext;

    /** Skip the build-up: clouds, rain, thunder and wind are at the current kind's values right away. For testing. */
    static void snap(ServerLevel overworld) {
        WeatherConfig.Look look = config.look(state.kind);
        cover = look.cover;
        boolean wasRaining = overworld.isRaining();
        overworld.setRainLevel(state.kind.rain * rainGate());
        overworld.setThunderLevel(state.kind.thunder * rainGate());
        syncFlags(overworld);
        // setRainLevel bypasses the vanilla change broadcast (it only fires when the level steps), so tell clients.
        var players = overworld.getServer().getPlayerList();
        if (wasRaining != overworld.isRaining()) {
            players.broadcastAll(new ClientboundGameEventPacket(overworld.isRaining()
                    ? ClientboundGameEventPacket.START_RAINING : ClientboundGameEventPacket.STOP_RAINING, 0f), overworld.dimension());
        }
        players.broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, overworld.getRainLevel(1f)), overworld.dimension());
        players.broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, state.kind.thunder * rainGate()), overworld.dimension());
        state.windSpeed = look.wind;
        state.windAngle = state.windTarget;
        snapNext = true;
        sync(overworld.getServer());
    }

    /** Whether this player's game has the client part (clouds, fog, wind-driven rain). */
    static boolean hasClientPart(ServerPlayer player) {
        return player.connection.hasChannel(WeatherSyncPayload.TYPE);
    }

    static void sync(ServerPlayer player) {
        if (state == null) return;
        lastSeason = Seasons.current(player.server.overworld());
        send(player, payload());
    }

    private static void send(ServerPlayer player, WeatherSyncPayload p) {
        if (player.connection.hasChannel(WeatherSyncPayload.TYPE)) PacketDistributor.sendToPlayer(player, p);
    }

    private static int seasonIndex(String season) {
        return switch (season) {
            case "SPRING" -> 0;
            case "SUMMER" -> 1;
            case "AUTUMN" -> 2;
            case "WINTER" -> 3;
            default -> 4;
        };
    }

    /**
     * Radiation fog and mist: after a clear or lightly clouded night, some dawns turn foggy (dense, autumn/winter)
     * or misty (light, spring to autumn). The weather they interrupted comes back when they lift.
     */
    private static void dawnFog(ServerLevel overworld) {
        if (!config.dawnFogFrom.contains(state.kind.name())) return;
        String season = Seasons.current(overworld);
        if (overworld.random.nextDouble() < config.dawnFogChance.getOrDefault(season, 0.0)) {
            interrupt(overworld, WeatherKind.FOG, "dawn, season " + season);
        } else if (overworld.random.nextDouble() < config.dawnMistChance.getOrDefault(season, 0.0)) {
            interrupt(overworld, WeatherKind.MIST, "dawn, season " + season);
        }
    }

    /** Start a fog/mist spell that hands back to the current weather (with its remaining time) when it lifts. */
    private static void interrupt(ServerLevel overworld, WeatherKind kind, String why) {
        WeatherKind interrupted = state.kind;
        long left = state.ticksLeft;
        begin(overworld, kind, -1, why);
        state.resumeKind = interrupted;
        state.resumeTicks = left;
    }

    /** Server copy of the clients' eased cloud cover: rain falls from clouds, so it waits for a thick deck. */
    private static float cover = -1;

    /** 0..1: how much of the kind's rain the clouds allow right now. */
    static float rainGate() {
        double span = Math.max(0.01, config.rainFullCover - config.rainStartCover);
        return (float) Mth.clamp((cover - config.rainStartCover) / span, 0, 1);
    }

    /** Called by the mixin in place of vanilla's clamp: move prev towards the target in small steps. */
    static float step(ServerLevel level, float prev, boolean thunder) {
        if (level.getGameTime() % STEP_TICKS != 0) return prev;
        float target = (thunder ? state.kind.thunder : state.kind.rain) * rainGate();
        float delta = (float) (STEP_TICKS / (Math.max(1.0, config.rampSeconds) * 20.0));
        return prev < target ? Math.min(target, prev + delta) : Math.max(target, prev - delta);
    }

    /** Switch to kind. minutes < 0 = roll a duration from the config. */
    static void begin(ServerLevel overworld, WeatherKind kind, double minutes, String why) {
        WeatherKind old = state.kind;
        if (minutes < 0) {
            WeatherConfig.Kind k = config.kind(kind);
            double min = k != null ? k.minMinutes : 10, max = k != null ? Math.max(k.minMinutes, k.maxMinutes) : 20;
            minutes = min + overworld.random.nextDouble() * (max - min);
        }
        state.kind = kind;
        state.resumeKind = null;
        state.resumeTicks = 0;
        state.ticksLeft = Math.max(1, Math.round(minutes * TICKS_PER_MINUTE));
        if (!kind.lifts() && !(old.lifts() && why.equals("fog lifted"))) {
            // A new spell brings its own wind; storms swing it further.
            float target = newWindTarget(overworld);
            if (kind == WeatherKind.THUNDERSTORM) target += (float) (overworld.random.nextGaussian() * Math.toRadians(45));
            state.windTarget = target;
        }
        state.setDirty();
        syncFlags(overworld);
        sync(overworld.getServer());
        WeatherDirectorMod.LOG.info("Weather {} -> {} for {} min ({})", old, kind, Math.round(minutes), why);
    }

    static WeatherKind pickNext(ServerLevel overworld) {
        WeatherConfig.Kind current = config.kind(state.kind);
        if (current == null || current.next.isEmpty()) return WeatherKind.CLEAR;
        Map<WeatherKind, Double> season = config.seasonFactors(Seasons.current(overworld));
        double total = 0;
        double[] weights = new double[WeatherKind.values().length];
        for (Map.Entry<String, Double> e : current.next.entrySet()) {
            WeatherKind k = WeatherKind.parse(e.getKey());
            if (k == null) continue;
            double w = Math.max(0, e.getValue()) * season.getOrDefault(k, 1.0);
            weights[k.ordinal()] += w;
            total += w;
        }
        if (total <= 0) return state.kind == WeatherKind.CLEAR ? WeatherKind.FAIR : WeatherKind.CLEAR;
        double r = overworld.random.nextDouble() * total;
        for (WeatherKind k : WeatherKind.values()) {
            r -= weights[k.ordinal()];
            if (weights[k.ordinal()] > 0 && r <= 0) return k;
        }
        return WeatherKind.CLEAR;
    }

    /** Keep the vanilla flags matching the kind; the timers stay ahead of us so vanilla never flips them. */
    private static void syncFlags(ServerLevel overworld) {
        int hold = (int) Math.min(Integer.MAX_VALUE - 1, state.ticksLeft + 6000);
        overworld.setWeatherParameters(0, hold, state.kind.wet(), state.kind == WeatherKind.THUNDERSTORM);
    }

    static float coverNow() {
        return cover;
    }

    static WeatherState state() {
        return state;
    }
}
