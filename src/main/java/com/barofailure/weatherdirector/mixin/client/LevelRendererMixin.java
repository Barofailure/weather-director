package com.barofailure.weatherdirector.mixin.client;

import com.barofailure.weatherdirector.client.SkyLook;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** renderSky fades sun, moon and stars by 1 - rain; we fade them behind thick cloud and fog too. */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @WrapOperation(method = "renderSky",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getRainLevel(F)F"))
    private float weatherDirector$sunHidden(ClientLevel level, float partialTick, Operation<Float> original) {
        return SkyLook.sunHidden(level, original.call(level, partialTick));
    }
}
