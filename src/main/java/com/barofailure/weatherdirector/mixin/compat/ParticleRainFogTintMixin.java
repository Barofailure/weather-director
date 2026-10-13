package com.barofailure.weatherdirector.mixin.compat;

import com.barofailure.weatherdirector.client.GroundFog;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import com.barofailure.weatherdirector.mixin.client.SingleQuadParticleAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pigcart.particlerain.config.ParticleData;

/** Ground fog pieces born during our mist/fog take its colour (white mist, grey fog) and, in fog, a bigger size. */
@Mixin(targets = "pigcart.particlerain.particle.CustomParticle", remap = false)
public abstract class ParticleRainFogTintMixin {
    @Shadow float baseSize;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void weatherDirector$tint(ClientLevel level, double x, double y, double z, ParticleData data, CallbackInfo ci) {
        if (GroundFog.on() && GroundFog.isFog(data)) {
            GroundFog.tint((SingleQuadParticle) (Object) this);
            baseSize *= GroundFog.sizeScale();
            ((SingleQuadParticleAccessor) this).weatherDirector$setQuadSize(baseSize);
        }
    }
}
