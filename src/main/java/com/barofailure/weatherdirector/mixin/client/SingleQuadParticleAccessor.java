package com.barofailure.weatherdirector.mixin.client;

import net.minecraft.client.particle.SingleQuadParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SingleQuadParticle.class)
public interface SingleQuadParticleAccessor {
    @Accessor("quadSize")
    void weatherDirector$setQuadSize(float size);

    @Accessor("quadSize")
    float weatherDirector$quadSize();
}
