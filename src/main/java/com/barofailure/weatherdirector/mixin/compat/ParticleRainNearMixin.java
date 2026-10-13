package com.barofailure.weatherdirector.mixin.compat;

import com.barofailure.weatherdirector.client.ClientWeather;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pigcart.particlerain.config.ParticleData;

/**
 * A falling streak that passes right in front of the camera is drawn as a huge flat panel. Vanilla rain never draws
 * inside the player's own column either; drop sky particles closer than 2 blocks to the camera.
 */
@Mixin(targets = "pigcart.particlerain.particle.CustomParticle", remap = false)
public abstract class ParticleRainNearMixin {
    @Shadow float distance;
    @Shadow ParticleData data;

    @Inject(method = "tick", at = @At("TAIL"))
    private void weatherDirector$near(CallbackInfo ci) {
        if (distance < 2f && data != null && data.spawnPos == ParticleData.SpawnPos.SKY && ClientWeather.active()) {
            ((Particle) (Object) this).remove();
        }
    }
}
