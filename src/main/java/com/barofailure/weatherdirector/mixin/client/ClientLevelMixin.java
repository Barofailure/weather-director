package com.barofailure.weatherdirector.mixin.client;

import com.barofailure.weatherdirector.client.SkyLook;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Sky and (vanilla) cloud colour go grey with our gloom, daylight dims a little; seasonal sky tint; star brightness. */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @WrapOperation(method = {"getSkyColor", "getCloudColor"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getRainLevel(F)F"))
    private float weatherDirector$gloom(ClientLevel level, float partialTick, Operation<Float> original) {
        return SkyLook.gloom(level, original.call(level, partialTick));
    }

    @WrapOperation(method = "getSkyDarken",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getRainLevel(F)F"))
    private float weatherDirector$lightGloom(ClientLevel level, float partialTick, Operation<Float> original) {
        return SkyLook.lightGloom(level, original.call(level, partialTick));
    }

    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void weatherDirector$sky(Vec3 pos, float partialTick, CallbackInfoReturnable<Vec3> cir) {
        cir.setReturnValue(SkyLook.sky((ClientLevel) (Object) this, cir.getReturnValue(), partialTick));
    }

    @Inject(method = "getStarBrightness", at = @At("RETURN"), cancellable = true)
    private void weatherDirector$stars(float partialTick, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(SkyLook.stars((ClientLevel) (Object) this, cir.getReturnValue()));
    }
}
