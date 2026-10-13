package com.barofailure.weatherdirector.mixin;

import com.barofailure.weatherdirector.WeatherHooks;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * advanceWeatherCycle ends with thunderLevel = clamp(...) (ordinal 0) and rainLevel = clamp(...) (ordinal 1)
 * after stepping them by +-0.01. In the overworld we replace both results with our own ramp; the vanilla
 * broadcasts right after send the new levels to the clients.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @WrapOperation(method = "advanceWeatherCycle", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(FFF)F", ordinal = 0))
    private float weatherDirector$thunder(float value, float min, float max, Operation<Float> original) {
        ServerLevel self = (ServerLevel) (Object) this;
        if (!WeatherHooks.controls(self)) return original.call(value, min, max);
        return WeatherHooks.step(self, ((LevelAccessor) self).weatherDirector$oThunderLevel(), true);
    }

    @WrapOperation(method = "advanceWeatherCycle", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(FFF)F", ordinal = 1))
    private float weatherDirector$rain(float value, float min, float max, Operation<Float> original) {
        ServerLevel self = (ServerLevel) (Object) this;
        if (!WeatherHooks.controls(self)) return original.call(value, min, max);
        return WeatherHooks.step(self, ((LevelAccessor) self).weatherDirector$oRainLevel(), false);
    }
}
