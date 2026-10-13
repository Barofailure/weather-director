package com.barofailure.weatherdirector.mixin.compat;

import com.barofailure.weatherdirector.client.ClientWeather;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** Particle Rain spawns the same amount of rain at any rain level; scale it so drizzle is light. */
@Pseudo
@Mixin(targets = "pigcart.particlerain.ParticleSpawner", remap = false)
public abstract class ParticleRainDensityMixin {
    /**
     * Particle Rain shows its thick storm streaks as soon as the thunder level is above zero, i.e. already with the
     * first drops of a building storm. Only once the storm is about a third built up: thin drops first, then rain,
     * then the heavy streaks.
     */
    @WrapOperation(method = "tickSkyFX", at = @At(value = "INVOKE",
            target = "Lpigcart/particlerain/config/ParticleData$Weather;isCurrent(Lnet/minecraft/client/multiplayer/ClientLevel;)Ljava/lang/Boolean;"))
    private static Boolean weatherDirector$heavyLater(pigcart.particlerain.config.ParticleData.Weather weather, ClientLevel level,
                                                       Operation<Boolean> original) {
        if (weather == pigcart.particlerain.config.ParticleData.Weather.ONLY_DURING_STORMY_WEATHER && ClientWeather.active(level)) {
            return level.getThunderLevel(1f) >= 0.3f;
        }
        return original.call(weather, level);
    }

    @WrapOperation(method = "tickSkyFX", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerpInt(FII)I"))
    private static int weatherDirector$density(float delta, int a, int b, Operation<Integer> original) {
        ClientLevel level = Minecraft.getInstance().level;
        int density = original.call(delta, a, b);
        if (!ClientWeather.active(level) || !com.barofailure.weatherdirector.client.ClientConfig.LIGHT_DRIZZLE.get()) return density;
        // Fractional counts are rounded at random, so very light rain is a few single drops, never bunches.
        float n = density * ClientWeather.precipitationScale(level);
        int whole = (int) n;
        return whole + (level.random.nextFloat() < n - whole ? 1 : 0);
    }
}
