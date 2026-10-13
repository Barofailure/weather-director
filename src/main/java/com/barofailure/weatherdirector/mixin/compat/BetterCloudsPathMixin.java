package com.barofailure.weatherdirector.mixin.compat;

import com.barofailure.weatherdirector.client.ClientWeather;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Better Clouds wanders its clouds along a fixed random path; ours drift with the wind, the same for everyone. */
@Pseudo
@Mixin(targets = "com.qendolin.betterclouds.clouds.RandomPath", remap = false)
public abstract class BetterCloudsPathMixin {
    @Inject(method = "getPathX", at = @At("HEAD"), cancellable = true)
    private static void weatherDirector$x(long ticks, float tickDelta, double travelSpeed, CallbackInfoReturnable<Double> cir) {
        if (ClientWeather.active() && com.barofailure.weatherdirector.client.ClientConfig.CONTROL_CLOUDS.get()) cir.setReturnValue(ClientWeather.cloudX(partial()));
    }

    @Inject(method = "getPathZ", at = @At("HEAD"), cancellable = true)
    private static void weatherDirector$z(long ticks, float tickDelta, double travelSpeed, CallbackInfoReturnable<Double> cir) {
        if (ClientWeather.active() && com.barofailure.weatherdirector.client.ClientConfig.CONTROL_CLOUDS.get()) cir.setReturnValue(ClientWeather.cloudZ(partial()));
    }

    private static float partial() {
        return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
    }
}
