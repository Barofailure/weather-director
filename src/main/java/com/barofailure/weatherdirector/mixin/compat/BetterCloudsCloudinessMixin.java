package com.barofailure.weatherdirector.mixin.compat;

import com.barofailure.weatherdirector.client.ClientWeather;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Better Clouds: our cover decides how much sky the clouds fill (0 = none, 1 = solid). */
@Pseudo
@Mixin(targets = "com.qendolin.betterclouds.clouds.CloudinessProvider", remap = false)
public abstract class BetterCloudsCloudinessMixin {
    @Inject(method = "getCloudiness", at = @At("HEAD"), cancellable = true)
    private static void weatherDirector$cover(ClientLevel level, float tickDelta, CallbackInfoReturnable<Float> cir) {
        // Better Clouds rebuilds its whole cloud field whenever cloudiness changes by 1%; our cover eases slowly over
        // minutes, so hand it over in 3% steps (a rebuild every ~15 s during a change instead of every ~5 s).
        if (ClientWeather.active(level) && com.barofailure.weatherdirector.client.ClientConfig.CONTROL_CLOUDS.get()) cir.setReturnValue(Mth.clamp(Math.round(ClientWeather.cover / 0.03f) * 0.03f, 0.03f, 1f));
    }
}
