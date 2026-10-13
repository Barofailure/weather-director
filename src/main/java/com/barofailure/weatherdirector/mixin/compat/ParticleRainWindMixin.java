package com.barofailure.weatherdirector.mixin.compat;

import com.barofailure.weatherdirector.client.ClientWeather;
import com.barofailure.weatherdirector.client.GroundFog;
import com.barofailure.weatherdirector.client.RainWind;
import com.barofailure.weatherdirector.mixin.client.ParticleAccessor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pigcart.particlerain.config.ParticleData;

/**
 * Particle Rain adds a little wind force every tick (made for its own tiny wind). With our wind we replace both of
 * its wind steps: at spawn the particle moves with the air, every tick after it relaxes towards it (see RainWind).
 */
@Mixin(targets = "pigcart.particlerain.WindManager", remap = false)
public abstract class ParticleRainWindMixin {
    @Inject(method = "applySpawnWind", at = @At("HEAD"), cancellable = true)
    private static void weatherDirector$spawn(Particle particle, ParticleData data, ClientLevel level, CallbackInfo ci) {
        if (!ClientWeather.active(level)) return;
        boolean fog = GroundFog.isFog(data);
        if (data.windStrength == 0 && !fog) return;
        ParticleAccessor p = (ParticleAccessor) particle;
        float v = fog ? GroundFog.drift() : RainWind.targetSpeed(level, data.gravity, data.windStrength);
        p.weatherDirector$setXd(RainWind.dirX() * v);
        p.weatherDirector$setZd(RainWind.dirZ() * v);
        ci.cancel();
    }

    @Inject(method = "applyWind", at = @At("HEAD"), cancellable = true)
    private static void weatherDirector$tick(Particle particle, ParticleData data, ClientLevel level, CallbackInfo ci) {
        if (!ClientWeather.active(level)) return;
        boolean fog = GroundFog.isFog(data);
        if (data.windStrength == 0 && !fog) return;
        ParticleAccessor p = (ParticleAccessor) particle;
        float v = fog ? GroundFog.drift() : RainWind.targetSpeed(level, data.gravity, data.windStrength);
        double tx = RainWind.dirX() * v, tz = RainWind.dirZ() * v;
        p.weatherDirector$setXd(p.weatherDirector$xd() + (tx - p.weatherDirector$xd()) * RainWind.RELAX);
        p.weatherDirector$setZd(p.weatherDirector$zd() + (tz - p.weatherDirector$zd()) * RainWind.RELAX);
        ci.cancel();
    }
}
