package com.barofailure.weatherdirector.mixin.compat;

import com.barofailure.weatherdirector.client.SkyLook;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** Better Clouds greys its clouds by rain and darkens them by thunder; with ours they also grey when overcast. */
@Pseudo
@Mixin(targets = "com.qendolin.betterclouds.clouds.EffectTintProvider", remap = false)
public abstract class BetterCloudsTintMixin {
    @WrapOperation(method = "getCloudsColor",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getRainLevel(F)F"))
    private static float weatherDirector$gloom(ClientLevel level, float tickDelta, Operation<Float> original) {
        float rain = original.call(level, tickDelta);
        return com.barofailure.weatherdirector.client.ClientConfig.CONTROL_CLOUDS.get() ? SkyLook.gloom(level, rain) : rain;
    }
}
