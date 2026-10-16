package com.barofailure.weatherdirector.mixin.client;

import com.barofailure.weatherdirector.client.WeatherSounds;
import net.minecraft.client.resources.sounds.SoundInstance;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Rain and snow sounds follow the rain strength, rain and thunder get quieter indoors and silent deep underground
 * (WeatherSounds.volumeScale: for sounds starting in play(), which uses the volume-and-category overload, and for playing
 * tickable sounds); thunder indoors is deeper (pitchScale) and comes
 * after the flash (thunderDelay).
 */
@Mixin(SoundEngine.class)
public abstract class SoundEngineVolumeMixin {
    @Shadow
    public abstract void playDelayed(SoundInstance sound, int delay);

    @Inject(method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F", at = @At("RETURN"), cancellable = true)
    private void weatherDirector$weatherVolume(SoundInstance sound, CallbackInfoReturnable<Float> cir) {
        float scale = WeatherSounds.volumeScale(sound);
        if (scale != 1f) cir.setReturnValue(cir.getReturnValueF() * scale);
    }

    @ModifyExpressionValue(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundEngine;calculateVolume(FLnet/minecraft/sounds/SoundSource;)F"))
    private float weatherDirector$startVolume(float volume, @Local(argsOnly = true) SoundInstance sound) {
        float scale = WeatherSounds.volumeScale(sound);
        return scale != 1f ? volume * scale : volume;
    }

    @Inject(method = "calculatePitch(Lnet/minecraft/client/resources/sounds/SoundInstance;)F", at = @At("RETURN"), cancellable = true)
    private void weatherDirector$thunderPitch(SoundInstance sound, CallbackInfoReturnable<Float> cir) {
        float scale = WeatherSounds.pitchScale(sound);
        if (scale != 1f) cir.setReturnValue(Mth.clamp(cir.getReturnValueF() * scale, 0.5f, 2f));
    }

    @Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)V", at = @At("HEAD"), cancellable = true)
    private void weatherDirector$thunderAfterFlash(SoundInstance sound, CallbackInfo ci) {
        int ticks = WeatherSounds.thunderDelay(sound);
        if (ticks > 0) {
            playDelayed(sound, ticks);
            ci.cancel();
        }
    }
}
