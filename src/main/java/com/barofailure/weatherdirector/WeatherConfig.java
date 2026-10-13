package com.barofailure.weatherdirector;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.neoforged.fml.loading.FMLPaths;

/**
 * config/weather_director.json. Written with the defaults below on first start; /wd reload
 * re-reads it. Durations are real minutes (one MC day = 20 minutes).
 */
public final class WeatherConfig {
    public static final class Kind {
        public double minMinutes;
        public double maxMinutes;
        /** Which kinds can follow this one, with base weights (fronts: clear -> clouds -> rain -> clearing). */
        public Map<String, Double> next = new LinkedHashMap<>();

        Kind() {}

        Kind(double min, double max, Object... pairs) {
            minMinutes = min;
            maxMinutes = max;
            for (int i = 0; i < pairs.length; i += 2) next.put((String) pairs[i], ((Number) pairs[i + 1]).doubleValue());
        }
    }

    /**
     * How a kind looks on the clients (0..1 each): cloud cover, grey sky/clouds, haze (view obscuration: 1 = opaque at
     * ~130 blocks; only fog and thunderstorms), mist (light white ground fog, mostly over water), fog (dense grey ground
     * fog, land only, deeper layer), wind strength.
     */
    public static final class Look {
        public float cover;
        public float gloom;
        public float haze;
        public float mist;
        public float fog;
        public float wind;

        Look() {}

        Look(double cover, double gloom, double haze, double mist, double fog, double wind) {
            this.cover = (float) cover;
            this.gloom = (float) gloom;
            this.haze = (float) haze;
            this.mist = (float) mist;
            this.fog = (float) fog;
            this.wind = (float) wind;
        }
    }

    /** Files older than this are replaced by the defaults (the old one is kept as .bak). */
    public static final int CURRENT_VERSION = 7;
    public int configVersion = CURRENT_VERSION;

    /** Seconds the rain level needs to go from 0 to 1 (vanilla: 5). */
    public double rampSeconds = 240;
    /** Seconds clouds (cover, grey) need to go from nothing to a full deck. Rain only starts once the deck is thick. */
    public double cloudRampSeconds = 480;
    /** Rain begins when the (eased) cloud cover passes rainStartCover and is full from rainFullCover on. */
    public double rainStartCover = 0.75;
    public double rainFullCover = 0.92;
    /** Seconds fog and mist need to roll in fully (and to lift). */
    public double fogRampSeconds = 90;
    /** Day-time tick of "dawn". Fog or mist form here (after a clear night) and lift after their duration. */
    public int dawnTime = 23000;
    /** Chance per season that a dawn turns foggy (dense), if the weather at dawn is one of dawnFogFrom. */
    public Map<String, Double> dawnFogChance = new LinkedHashMap<>();
    /** Chance per season that a dawn turns misty (light), if it did not turn foggy. */
    public Map<String, Double> dawnMistChance = new LinkedHashMap<>();
    public List<String> dawnFogFrom = new ArrayList<>();
    /** Chance that mist rises when rain ends (wet ground, humid air). */
    public double afterRainMistChance = 0.35;
    public Map<String, Kind> kinds = new LinkedHashMap<>();
    public Map<String, Look> look = new LinkedHashMap<>();
    /** Wind blows mostly TOWARDS this compass angle (0 = east/+X, 90 = south/+Z, 180 = west, 270 = north). */
    public double prevailingWindTowardDegrees = 340;
    /** How far a new weather spell may turn the wind away from the prevailing direction (degrees, one sigma). */
    public double windVariationDegrees = 60;
    /** How fast the wind direction may turn, degrees per real minute. */
    public double windTurnDegreesPerMinute = 25;
    /** Cloud drift in blocks per second at wind 0 and wind 1. */
    public double cloudDriftMin = 0.4;
    public double cloudDriftMax = 4.5;
    /** Multiplier on how hard wind pushes rain and snow sideways (Particle Rain). */
    public double rainWindScale = 1.0;
    /** Per season (Ecliptic Seasons SPRING/SUMMER/AUTUMN/WINTER/NONE): multiplier on the chance to move INTO a kind. */
    public Map<String, Map<String, Double>> seasons = new LinkedHashMap<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static WeatherConfig defaults() {
        WeatherConfig c = new WeatherConfig();
        c.kinds.put("CLEAR", new Kind(20, 60, "FAIR", 3, "CLOUDY", 0.5));
        c.kinds.put("FAIR", new Kind(15, 45, "CLEAR", 2, "CLOUDY", 2));
        c.kinds.put("CLOUDY", new Kind(10, 30, "FAIR", 1.5, "OVERCAST", 2, "THUNDERSTORM", 0.3));
        c.kinds.put("OVERCAST", new Kind(10, 30, "CLOUDY", 1.5, "DRIZZLE", 1.5, "RAIN", 1.5));
        // FOG normally hands back to the weather it interrupted; "next" only applies after /wd set fog.
        c.kinds.put("FOG", new Kind(8, 16, "FAIR", 2, "CLEAR", 1, "CLOUDY", 1, "OVERCAST", 0.7));
        c.kinds.put("MIST", new Kind(6, 14, "FAIR", 2, "CLEAR", 2, "CLOUDY", 1));
        c.kinds.put("DRIZZLE", new Kind(5, 15, "OVERCAST", 1.5, "RAIN", 1.5));
        c.kinds.put("RAIN", new Kind(8, 20, "DRIZZLE", 1.5, "HEAVY_RAIN", 1, "OVERCAST", 1, "THUNDERSTORM", 0.5));
        c.kinds.put("HEAVY_RAIN", new Kind(4, 10, "RAIN", 2, "THUNDERSTORM", 1));
        c.kinds.put("THUNDERSTORM", new Kind(4, 10, "HEAVY_RAIN", 1.5, "RAIN", 1, "CLOUDY", 1));
        //                                cover gloom haze  mist  fog   wind
        c.look.put("CLEAR", new Look(0.05, 0.0, 0.0, 0.0, 0.0, 0.15));
        c.look.put("FAIR", new Look(0.5, 0.0, 0.0, 0.0, 0.0, 0.25));
        c.look.put("CLOUDY", new Look(0.66, 0.15, 0.0, 0.0, 0.0, 0.35));
        c.look.put("OVERCAST", new Look(0.9, 0.55, 0.0, 0.0, 0.0, 0.35));
        c.look.put("FOG", new Look(0.8, 0.5, 1.0, 0.0, 1.0, 0.05));
        c.look.put("MIST", new Look(0.3, 0.05, 0.0, 1.0, 0.0, 0.08));
        c.look.put("DRIZZLE", new Look(0.92, 0.6, 0.0, 0.0, 0.0, 0.3));
        c.look.put("RAIN", new Look(0.95, 0.7, 0.0, 0.0, 0.0, 0.5));
        c.look.put("HEAVY_RAIN", new Look(1.0, 0.85, 0.0, 0.0, 0.0, 0.8));
        c.look.put("THUNDERSTORM", new Look(1.0, 1.0, 0.45, 0.0, 0.0, 1.0));
        //                               clear fair cloudy overcast fog mist drizzle rain heavy storm
        c.seasons.put("SPRING", factors(1.0, 1.3, 1.1, 0.8, 1.0, 1.0, 1.4, 1.3, 0.8, 0.6));
        c.seasons.put("SUMMER", factors(1.6, 1.3, 1.0, 0.6, 1.0, 1.0, 0.5, 0.7, 1.0, 2.0));
        c.seasons.put("AUTUMN", factors(1.1, 1.2, 1.1, 1.1, 1.0, 1.0, 1.4, 1.3, 1.0, 0.4));
        c.seasons.put("WINTER", factors(1.5, 1.0, 1.0, 1.2, 1.0, 1.0, 0.6, 1.2, 0.8, 0.05));
        // Dense fog: autumn and winter. Mist: spring to autumn mornings. (Rolled in that order at dawn.)
        c.dawnFogChance.put("SPRING", 0.08);
        c.dawnFogChance.put("SUMMER", 0.03);
        c.dawnFogChance.put("AUTUMN", 0.35);
        c.dawnFogChance.put("WINTER", 0.3);
        c.dawnFogChance.put("NONE", 0.1);
        c.dawnMistChance.put("SPRING", 0.45);
        c.dawnMistChance.put("SUMMER", 0.3);
        c.dawnMistChance.put("AUTUMN", 0.45);
        c.dawnMistChance.put("WINTER", 0.2);
        c.dawnMistChance.put("NONE", 0.25);
        c.dawnFogFrom.addAll(List.of("CLEAR", "FAIR", "CLOUDY"));
        return c;
    }

    private static Map<String, Double> factors(double... f) {
        Map<String, Double> m = new LinkedHashMap<>();
        WeatherKind[] k = WeatherKind.values();
        for (int i = 0; i < k.length; i++) m.put(k[i].name(), f[i]);
        return m;
    }

    static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("weather_director.json");
    }

    /** Reads the file (writing the defaults if it is missing). Throws on a broken file so the old config stays. */
    static WeatherConfig load() throws IOException {
        Path p = path();
        if (!Files.exists(p)) {
            WeatherConfig d = defaults();
            Files.writeString(p, GSON.toJson(d));
            return d;
        }
        JsonObject json = JsonParser.parseString(Files.readString(p)).getAsJsonObject();
        if (!json.has("configVersion") || json.get("configVersion").getAsInt() < CURRENT_VERSION) {
            // The meaning of the look values changed; an old file would mean different weather. Start over.
            Files.move(p, p.resolveSibling(p.getFileName() + ".bak"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            WeatherConfig d = defaults();
            Files.writeString(p, GSON.toJson(d));
            WeatherDirectorMod.LOG.warn("{} was from an older version: replaced with the defaults (old file kept as .bak)", p);
            return d;
        }
        WeatherConfig c = GSON.fromJson(json, WeatherConfig.class);
        // Files from older versions miss newer entries: take those from the defaults.
        WeatherConfig d = defaults();
        d.kinds.forEach(c.kinds::putIfAbsent);
        d.look.forEach(c.look::putIfAbsent);
        d.seasons.forEach(c.seasons::putIfAbsent);
        d.dawnFogChance.forEach(c.dawnFogChance::putIfAbsent);
        d.dawnMistChance.forEach(c.dawnMistChance::putIfAbsent);
        if (c.dawnFogFrom.isEmpty()) c.dawnFogFrom.addAll(d.dawnFogFrom);
        for (String k : c.kinds.keySet()) {
            if (WeatherKind.parse(k) == null) WeatherDirectorMod.LOG.warn("Unknown weather kind '{}' in {}", k, p);
        }
        return c;
    }

    Look look(WeatherKind k) {
        Look l = look.get(k.name());
        return l != null ? l : new Look(0.5, 0, 0, 0, 0, 0.3);
    }

    Kind kind(WeatherKind k) {
        return kinds.get(k.name());
    }

    Map<WeatherKind, Double> seasonFactors(String season) {
        Map<WeatherKind, Double> out = new EnumMap<>(WeatherKind.class);
        Map<String, Double> m = seasons.get(season);
        if (m != null) m.forEach((k, v) -> {
            WeatherKind w = WeatherKind.parse(k);
            if (w != null) out.put(w, v);
        });
        return out;
    }
}
