package com.barofailure.weatherdirector.mixin.compat;

import com.barofailure.weatherdirector.Compat;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Applies the Better Clouds / Particle Rain / Distant Horizons mixins only for installed, tested versions (Compat). */
public final class CompatMixinPlugin implements IMixinConfigPlugin {
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String simple = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
        if (simple.startsWith("BetterClouds")) return Compat.on(Compat.BETTER_CLOUDS);
        if (simple.startsWith("ParticleRain")) return Compat.on(Compat.PARTICLE_RAIN);
        if (simple.startsWith("DistantHorizons")) return Compat.on(Compat.DISTANT_HORIZONS);
        return true;
    }

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
