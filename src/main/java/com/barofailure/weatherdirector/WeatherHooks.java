package com.barofailure.weatherdirector;

import net.minecraft.server.level.ServerLevel;

/** The only entry points the mixins use (mixin classes must not be referenced, so the logic stays here). */
public final class WeatherHooks {
    private WeatherHooks() {}

    public static boolean controls(ServerLevel level) {
        return WeatherDirector.controls(level);
    }

    public static float step(ServerLevel level, float prev, boolean thunder) {
        return WeatherDirector.step(level, prev, thunder);
    }
}
