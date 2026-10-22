package com.barofailure.weatherdirector.mixin.compat;

import com.barofailure.weatherdirector.client.ShaderWeather;
import net.irisshaders.iris.gl.uniform.UniformHolder;
import net.irisshaders.iris.gl.uniform.UniformUpdateFrequency;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Iris: every shader program also gets the weather uniforms (ShaderWeather lists them). */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.uniforms.IrisExclusiveUniforms", remap = false)
public abstract class IrisUniformsMixin {
    @Inject(method = "addIrisExclusiveUniforms", at = @At("TAIL"))
    private static void weatherDirector$uniforms(UniformHolder uniforms, CallbackInfo ci) {
        uniforms.uniform1f(UniformUpdateFrequency.PER_FRAME, "wdActive", ShaderWeather::active)
                .uniform1f(UniformUpdateFrequency.PER_FRAME, "wdCover", ShaderWeather::cover)
                .uniform1f(UniformUpdateFrequency.PER_FRAME, "wdGloom", ShaderWeather::gloom)
                .uniform1f(UniformUpdateFrequency.PER_FRAME, "wdLightGloom", ShaderWeather::lightGloom)
                .uniform1f(UniformUpdateFrequency.PER_FRAME, "wdFogEnd", ShaderWeather::fogEnd)
                .uniform1f(UniformUpdateFrequency.PER_FRAME, "wdGroundFog", ShaderWeather::groundFog)
                .uniform1f(UniformUpdateFrequency.PER_FRAME, "wdMist", ShaderWeather::mist)
                .uniform1f(UniformUpdateFrequency.PER_FRAME, "wdFogTop", ShaderWeather::fogTop)
                .uniform1f(UniformUpdateFrequency.PER_FRAME, "wdCloudBottom", ShaderWeather::cloudBottom)
                .uniform1f(UniformUpdateFrequency.PER_FRAME, "wdCloudTop", ShaderWeather::cloudTop)
                .uniform2f(UniformUpdateFrequency.PER_FRAME, "wdCloudOffset", ShaderWeather::cloudOffset);
    }
}
