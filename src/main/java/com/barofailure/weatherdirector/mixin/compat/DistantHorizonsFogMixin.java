package com.barofailure.weatherdirector.mixin.compat;

import com.barofailure.weatherdirector.client.ClientWeather;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Distant Horizons cancels the game's terrain fog while it draws LODs (its enableVanillaFog defaults to false), which
 * also wipes out weather fog around the player. While our fog, mist or rain haze is up, DH keeps vanilla fog;
 * DhFog fogs the LODs beyond to match.
 */
@Mixin(targets = "com.seibel.distanthorizons.common.commonMixins.MixinVanillaFogCommon_neoforge", remap = false)
public abstract class DistantHorizonsFogMixin {
    @Inject(method = "cancelFog", at = @At("HEAD"), cancellable = true)
    private static void weatherDirector$keepWeatherFog(Camera camera, FogRenderer.FogMode mode, CallbackInfoReturnable<Boolean> cir) {
        if (ClientWeather.weatherFogUp()) cir.setReturnValue(false);
    }
}
