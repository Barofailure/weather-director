package com.barofailure.weatherdirector.mixin;

import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Level.class)
public interface LevelAccessor {
    @Accessor("oRainLevel")
    float weatherDirector$oRainLevel();

    @Accessor("oThunderLevel")
    float weatherDirector$oThunderLevel();
}
