package com.barofailure.weatherdirector.mixin.client;

import com.barofailure.weatherdirector.client.ClientWeather;
import com.barofailure.weatherdirector.client.CloudLayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** On the client, rain is what falls from the clouds onto you: above the cloud deck it is gone (particles, sound, sky). */
@Mixin(Level.class)
public abstract class LevelRainMixin {
    @Inject(method = "getRainLevel", at = @At("RETURN"), cancellable = true)
    private void weatherDirector$aboveClouds(float delta, CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof ClientLevel level && CloudLayer.above() > 0.001f && ClientWeather.active(level)) {
            cir.setReturnValue(CloudLayer.visibleRain(cir.getReturnValue()));
        }
    }
}
