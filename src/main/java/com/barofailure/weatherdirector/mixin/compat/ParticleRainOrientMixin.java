package com.barofailure.weatherdirector.mixin.compat;

import com.barofailure.weatherdirector.client.ClientWeather;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Particle Rain tilts streaks by their speed RELATIVE to the camera, so walking or flying changes the rain's angle.
 * With our wind the rain has one real direction; draw it by world speed so it keeps its slant while you move.
 */
@Mixin(targets = "pigcart.particlerain.particle.CustomParticle", remap = false)
public abstract class ParticleRainOrientMixin {
    @Shadow public abstract void renderWorldVelocityQuad(VertexConsumer h, Camera camera, float tickPercent);

    @Inject(method = "renderRelativeVelocityQuad", at = @At("HEAD"), cancellable = true)
    private void weatherDirector$world(VertexConsumer h, Camera camera, float tickPercent, CallbackInfo ci) {
        if (!ClientWeather.active()) return;
        renderWorldVelocityQuad(h, camera, tickPercent);
        ci.cancel();
    }
}
