package com.barofailure.weatherdirector.mixin.compat;

import com.barofailure.weatherdirector.client.SkyLook;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Better Clouds greys its clouds by rain and darkens them by thunder; with ours they also grey when overcast. At night
 * thunder darkens them less and dark-moon nights keep a faint glow, so night clouds stay dark grey instead of black.
 */
@Pseudo
@Mixin(targets = "com.qendolin.betterclouds.clouds.EffectTintProvider", remap = false)
public abstract class BetterCloudsTintMixin {
    @WrapOperation(method = "getCloudsColor",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getRainLevel(F)F"))
    private static float weatherDirector$gloom(ClientLevel level, float tickDelta, Operation<Float> original) {
        float rain = original.call(level, tickDelta);
        return com.barofailure.weatherdirector.client.ClientConfig.CONTROL_CLOUDS.get() ? SkyLook.gloom(level, rain) : rain;
    }

    @WrapOperation(method = "getCloudsColor",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getThunderLevel(F)F"))
    private static float weatherDirector$nightThunder(ClientLevel level, float tickDelta, Operation<Float> original) {
        float thunder = original.call(level, tickDelta);
        return com.barofailure.weatherdirector.client.ClientConfig.CONTROL_CLOUDS.get()
                ? thunder * SkyLook.thunderDarkening(level.getTimeOfDay(tickDelta)) : thunder;
    }

    @WrapOperation(method = "getMoonSize",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getMoonBrightness()F"))
    private static float weatherDirector$moonGlow(ClientLevel level, Operation<Float> original) {
        float moon = original.call(level);
        return com.barofailure.weatherdirector.client.ClientConfig.CONTROL_CLOUDS.get() ? Math.max(moon, SkyLook.MIN_MOON_LIGHT) : moon;
    }
}
