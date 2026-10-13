package com.barofailure.weatherdirector.mixin.compat;

import com.barofailure.weatherdirector.client.ClientWeather;
import com.barofailure.weatherdirector.client.RainWind;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import pigcart.particlerain.config.ParticleData;
import org.spongepowered.asm.mixin.injection.At;

/** Particles spawned upwind start further from the camera; keep them alive for that extra distance. */
@Mixin(targets = "pigcart.particlerain.particle.CustomParticle", remap = false)
public abstract class ParticleRainRangeMixin {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lpigcart/particlerain/config/ParticleData$SpawnPos;renderDistance()I"))
    private int weatherDirector$range(ParticleData.SpawnPos spawnPos, Operation<Integer> original) {
        int d = original.call(spawnPos);
        return ClientWeather.active() ? d + (int) Math.ceil(RainWind.MAX_UPWIND) + 2 : d;
    }
}
