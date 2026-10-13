package com.barofailure.weatherdirector.mixin.compat;

import com.barofailure.weatherdirector.client.GroundFog;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.ArrayList;
import net.minecraft.core.Holder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pigcart.particlerain.config.ParticleData;
import pigcart.particlerain.config.Whitelist;

/**
 * Particle Rain's surface spawn loop decides per particle type: density, rain-type, biome, ground block. For its two
 * ground-fog types we widen those while our mist/fog is up (see GroundFog); everything else is untouched.
 */
@Mixin(targets = "pigcart.particlerain.ParticleSpawner", remap = false)
public abstract class ParticleRainGroundFogMixin {
    @WrapOperation(method = "tickSurfaceFX", at = @At(value = "INVOKE",
            target = "Lpigcart/particlerain/ParticleSpawner;density(Lpigcart/particlerain/config/ParticleData;)F"))
    private static float weatherDirector$density(ParticleData data, Operation<Float> original) {
        float d = original.call(data);
        return GroundFog.on() && GroundFog.isFog(data) ? GroundFog.density(d) : d;
    }

    /**
     * Particle Rain starts a random 0-5 minute "after weather" period whenever rain stops and then spreads fog in most
     * biomes, whatever the weather. Our weather has its own after-rain mist (visible in /wd), so while it runs,
     * that random period is off. Particle Rain's permanent swamp/jungle/dark-forest fog stays.
     */
    @WrapOperation(method = "tickSurfaceFX", at = @At(value = "INVOKE",
            target = "Lpigcart/particlerain/config/ParticleData$Weather;isCurrent(Lnet/minecraft/client/multiplayer/ClientLevel;)Ljava/lang/Boolean;"))
    private static Boolean weatherDirector$noRandomAfterRainFog(ParticleData.Weather weather, net.minecraft.client.multiplayer.ClientLevel level,
                                                               Operation<Boolean> original) {
        if (weather == ParticleData.Weather.AFTER_WEATHER && com.barofailure.weatherdirector.client.ClientWeather.active(level)) return false;
        return original.call(weather, level);
    }

    @WrapOperation(method = "tickSurfaceFX", at = @At(value = "INVOKE", target = "Ljava/util/ArrayList;contains(Ljava/lang/Object;)Z"))
    private static boolean weatherDirector$precipitation(ArrayList<?> list, Object value, Operation<Boolean> original) {
        // Fog also forms where it would snow (winter fog), not only where it would rain.
        return original.call(list, value) || (GroundFog.on() && GroundFog.isFogPrecipitation(list));
    }

    @WrapOperation(method = "tickSurfaceFX", at = @At(value = "INVOKE",
            target = "Lpigcart/particlerain/config/Whitelist$BiomeList;contains(Lnet/minecraft/core/Holder;)Z"))
    private static boolean weatherDirector$biome(Whitelist.BiomeList list, Holder<?> biome, Operation<Boolean> original) {
        return original.call(list, biome) || (GroundFog.on() && GroundFog.isFogBiomeList(list));
    }

    @WrapOperation(method = "tickSurfaceFX", at = @At(value = "INVOKE",
            target = "Lpigcart/particlerain/config/ParticleData$ParticleStyle;spawn(Lnet/minecraft/client/multiplayer/ClientLevel;DDDLpigcart/particlerain/config/ParticleData;)V"))
    private static void weatherDirector$taller(ParticleData.ParticleStyle style, net.minecraft.client.multiplayer.ClientLevel level,
                                                double x, double y, double z, ParticleData data, Operation<Void> original) {
        double lift = GroundFog.on() && GroundFog.isFog(data) ? GroundFog.extraHeight() : 0;
        original.call(style, level, x, y + lift, z, data);
    }

    @SuppressWarnings("unchecked")
    @WrapOperation(method = "tickSurfaceFX", at = @At(value = "INVOKE",
            target = "Lpigcart/particlerain/config/Whitelist$BlockList;contains(Lnet/minecraft/core/Holder;)Z"))
    private static boolean weatherDirector$ground(Whitelist.BlockList list, Holder<?> block, Operation<Boolean> original) {
        boolean ok = original.call(list, block);
        if (!GroundFog.on() || !GroundFog.isFogBlockList(list)) return ok;
        return GroundFog.ground((Holder<net.minecraft.world.level.block.Block>) block, ok);
    }
}
