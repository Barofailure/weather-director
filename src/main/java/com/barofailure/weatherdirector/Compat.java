package com.barofailure.weatherdirector;

import java.util.HashMap;
import java.util.Map;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Which optional mods we play the weather through. A mod counts only when it is installed AND its version is in the
 * range we tested (same major version). Outside that range its hooks stay off and the log says why, so an update of
 * Better Clouds, Particle Rain, Distant Horizons or Iris can never crash the game; the weather just looks plainer.
 * Also read by the compat mixin plugin, so it only uses the loading-time mod list and no game classes.
 */
public final class Compat {
    public static final String BETTER_CLOUDS = "betterclouds";
    public static final String PARTICLE_RAIN = "particlerain";
    public static final String DISTANT_HORIZONS = "distanthorizons";
    public static final String AMBIENTSOUNDS = "ambientsounds";
    public static final String IRIS = "iris";

    /** modId -> {lowest tested version, first untested major}. */
    private static final Map<String, String[]> TESTED = Map.of(
            BETTER_CLOUDS, new String[] {"1.13.11", "2"},
            PARTICLE_RAIN, new String[] {"4.0.2", "5"},
            DISTANT_HORIZONS, new String[] {"3.3.0", "4"},
            AMBIENTSOUNDS, new String[] {"6.3.0", "7"},
            // Our shader uniforms hook into Iris internals, so only the tested minor version.
            IRIS, new String[] {"1.8.14", "1.9"});

    private static final Logger LOG = LoggerFactory.getLogger("weather_director");
    private static final Map<String, Boolean> CACHE = new HashMap<>();

    private Compat() {}

    public static synchronized boolean on(String modId) {
        return CACHE.computeIfAbsent(modId, Compat::check);
    }

    private static boolean check(String modId) {
        LoadingModList list = LoadingModList.get();
        ModFileInfo file = list == null ? null : list.getModFileById(modId);
        if (file == null) return false;
        String found = file.getMods().get(0).getVersion().toString();
        String[] range = TESTED.get(modId);
        if (range == null) return true;
        ArtifactVersion v = plain(found);
        boolean ok = v.compareTo(plain(range[0])) >= 0 && v.compareTo(plain(range[1])) < 0;
        if (ok) LOG.info("Weather Director: {} {} found, hooks on", modId, found);
        else LOG.warn("Weather Director: {} {} is outside the tested range {} up to {}, its hooks stay OFF"
                + " (the weather still works, just without that mod's part)", modId, found, range[0], range[1]);
        return ok;
    }

    /** "1.13.11+1.21.1-neoforge" -> 1.13.11 */
    private static ArtifactVersion plain(String version) {
        return new DefaultArtifactVersion(version.split("[+\\-]", 2)[0]);
    }
}
