package com.barofailure.weatherdirector.mixin.client;

import com.barofailure.weatherdirector.client.SkyLook;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Seasonal sunrise/sunset colours, faded behind cloud and fog. */
@Mixin(DimensionSpecialEffects.class)
public abstract class DimensionSpecialEffectsMixin {
    @Inject(method = "getSunriseColor", at = @At("RETURN"), cancellable = true)
    private void weatherDirector$sunrise(float timeOfDay, float partialTicks, CallbackInfoReturnable<float[]> cir) {
        cir.setReturnValue(SkyLook.sunrise(cir.getReturnValue()));
    }
}
