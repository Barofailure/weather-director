package com.barofailure.weatherdirector.mixin.compat;

import com.barofailure.weatherdirector.client.CloudShape;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.qendolin.betterclouds.config.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Every settings read inside Better Clouds' renderer gets the weather-shaped copy (see CloudShape). */
@Mixin(targets = "com.qendolin.betterclouds.clouds.Renderer", remap = false)
public abstract class BetterCloudsShapeMixin {
    @WrapOperation(method = "*", require = 1, allow = 32,
            at = @At(value = "INVOKE", target = "Lcom/qendolin/betterclouds/config/ConfigManager;instance()Lcom/qendolin/betterclouds/config/Config;"))
    private Config weatherDirector$shape(Operation<Config> original) {
        return CloudShape.shape(original.call());
    }
}
