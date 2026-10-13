package com.barofailure.weatherdirector.mixin.compat;

import com.barofailure.weatherdirector.client.ClientWeather;
import com.barofailure.weatherdirector.client.RainWind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Particle Rain spawns sky particles on a sphere around the camera. In wind, half of them would blow straight out of
 * it; move the sphere upwind by the drift a drop makes while falling, so the rain lands on the player.
 */
@Mixin(targets = "pigcart.particlerain.ParticleSpawner", remap = false)
public abstract class ParticleRainSpawnMixin {
    @ModifyVariable(method = "tickSkyFX", at = @At("HEAD"), argsOnly = true)
    private static Vec3 weatherDirector$upwind(Vec3 cameraPos) {
        ClientLevel level = Minecraft.getInstance().level;
        if (!ClientWeather.active(level)) return cameraPos;
        float d = RainWind.upwind(level);
        return cameraPos.subtract(RainWind.dirX() * d, 0, RainWind.dirZ() * d);
    }
}
