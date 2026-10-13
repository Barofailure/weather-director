package com.barofailure.weatherdirector.mixin.client;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface ParticleAccessor {
    @Accessor("xd") double weatherDirector$xd();
    @Accessor("xd") void weatherDirector$setXd(double v);
    @Accessor("zd") double weatherDirector$zd();
    @Accessor("zd") void weatherDirector$setZd(double v);
    @Accessor("gravity") float weatherDirector$gravity();
}
