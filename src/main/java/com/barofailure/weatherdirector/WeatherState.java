package com.barofailure.weatherdirector;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

/** What the director is doing, saved with the overworld (data/weather_director.dat). */
public final class WeatherState extends SavedData {
    static final String NAME = "weather_director";

    WeatherKind kind = WeatherKind.CLEAR;
    long ticksLeft = 0;
    /** The kind a dawn fog interrupted, resumed when the fog lifts. */
    WeatherKind resumeKind = null;
    long resumeTicks = 0;
    /** Wind: direction it blows towards (radians, 0 = +X), where it is turning to, strength 0..1. */
    float windAngle = 0;
    float windTarget = 0;
    float windSpeed = 0;
    /** Accumulated cloud drift in blocks, shared by all clients so everyone sees the same clouds. */
    double cloudX = 0;
    double cloudZ = 0;

    static final SavedData.Factory<WeatherState> FACTORY = new SavedData.Factory<>(WeatherState::new, WeatherState::load);

    static WeatherState load(CompoundTag tag, HolderLookup.Provider registries) {
        WeatherState s = new WeatherState();
        WeatherKind k = WeatherKind.parse(tag.getString("kind"));
        if (k != null) s.kind = k;
        s.ticksLeft = tag.getLong("ticksLeft");
        s.resumeKind = WeatherKind.parse(tag.getString("resumeKind"));
        s.resumeTicks = tag.getLong("resumeTicks");
        s.windAngle = tag.getFloat("windAngle");
        s.windTarget = tag.getFloat("windTarget");
        s.windSpeed = tag.getFloat("windSpeed");
        s.cloudX = tag.getDouble("cloudX");
        s.cloudZ = tag.getDouble("cloudZ");
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString("kind", kind.name());
        tag.putLong("ticksLeft", ticksLeft);
        tag.putFloat("windAngle", windAngle);
        tag.putFloat("windTarget", windTarget);
        tag.putFloat("windSpeed", windSpeed);
        tag.putDouble("cloudX", cloudX);
        tag.putDouble("cloudZ", cloudZ);
        if (resumeKind != null) {
            tag.putString("resumeKind", resumeKind.name());
            tag.putLong("resumeTicks", resumeTicks);
        }
        return tag;
    }
}
