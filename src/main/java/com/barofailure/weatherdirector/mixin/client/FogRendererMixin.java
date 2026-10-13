package com.barofailure.weatherdirector.mixin.client;

import com.barofailure.weatherdirector.client.SkyLook;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Fog colour darkens with our gloom like it does with rain. */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    @WrapOperation(method = "setupColor",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getRainLevel(F)F"))
    private static float weatherDirector$gloom(ClientLevel level, float partialTick, Operation<Float> original) {
        return SkyLook.gloom(level, original.call(level, partialTick));
    }
}
