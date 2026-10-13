package com.barofailure.weatherdirector;

import java.lang.reflect.Method;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

/** Current season from Ecliptic Seasons, read by reflection so the mod also runs without it ("NONE"). */
final class Seasons {
    private static boolean looked;
    private static Method getInstance;
    private static Method getSeason;

    static String current(Level level) {
        if (!looked) {
            looked = true;
            if (ModList.get().isLoaded("eclipticseasons")) {
                try {
                    Class<?> api = Class.forName("com.teamtea.eclipticseasons.api.EclipticSeasonsApi");
                    getInstance = api.getMethod("getInstance");
                    getSeason = api.getMethod("getSeason", Level.class);
                } catch (ReflectiveOperationException e) {
                    WeatherDirectorMod.LOG.warn("Ecliptic Seasons API not found, weather ignores seasons", e);
                }
            }
        }
        if (getSeason == null) return "NONE";
        try {
            return ((Enum<?>) getSeason.invoke(getInstance.invoke(null), level)).name();
        } catch (ReflectiveOperationException | RuntimeException e) {
            WeatherDirectorMod.LOG.warn("Reading the season failed, weather ignores seasons from now on", e);
            getSeason = null;
            return "NONE";
        }
    }
}
