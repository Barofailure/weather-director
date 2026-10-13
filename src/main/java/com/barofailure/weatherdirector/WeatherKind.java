package com.barofailure.weatherdirector;

/**
 * The kinds of weather the director can pick. One kind holds for the whole overworld.
 * rain/thunder are the vanilla levels the server ramps towards. Vanilla counts the world as raining
 * above rain 0.2 and as thundering above rain*thunder 0.9, so DRIZZLE stays above 0.2 and HEAVY_RAIN
 * below 0.9 thunder (no lightning, but Particle Rain draws its storm density from the thunder level).
 * Cloud cover, fog and sky are drawn by the client part; without it the dry kinds look alike.
 */
public enum WeatherKind {
    CLEAR(0f, 0f),
    FAIR(0f, 0f),
    CLOUDY(0f, 0f),
    OVERCAST(0f, 0f),
    FOG(0f, 0f),
    MIST(0f, 0f),
    DRIZZLE(0.3f, 0f),
    RAIN(0.65f, 0f),
    HEAVY_RAIN(1f, 0.6f),
    THUNDERSTORM(1f, 1f);

    public final float rain;
    public final float thunder;

    WeatherKind(float rain, float thunder) {
        this.rain = rain;
        this.thunder = thunder;
    }

    /** Morning fog and mist are short interruptions; the weather they replaced comes back when they lift. */
    public boolean lifts() {
        return this == FOG || this == MIST;
    }

    public boolean wet() {
        return rain > 0.2f;
    }

    public static WeatherKind parse(String name) {
        try {
            return valueOf(name.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
