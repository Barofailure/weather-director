package com.barofailure.weatherdirector.mixin.client;

import com.barofailure.weatherdirector.client.WeatherSounds;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Rain and snow sounds follow the rain strength (WeatherSounds.rainScale); runs for new and for playing sounds. */
@Mixin(SoundEngine.class)
public abstract class SoundEngineVolumeMixin {
    @Inject(method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F", at = @At("RETURN"), cancellable = true)
    private void weatherDirector$rainVolume(SoundInstance sound, CallbackInfoReturnable<Float> cir) {
        float scale = WeatherSounds.rainScale(sound);
        if (scale != 1f) cir.setReturnValue(cir.getReturnValueF() * scale);
    }
}
