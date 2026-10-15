package com.barofailure.weatherdirector.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import com.barofailure.weatherdirector.Compat;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client wiring: easing tick, fog distance and colour, Distant Horizons far fog. */
public final class ClientEvents {

    private ClientEvents() {}

    /** NeoForge's built-in settings screen for our client config (Mods -> Weather Director -> Config). */
    public static void registerConfigScreen(net.neoforged.fml.ModContainer container) {
        container.registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                (c, parent) -> new net.neoforged.neoforge.client.gui.ConfigurationScreen(c, parent));
    }

    /**
     * AmbientSounds plays its own wind by height and open sky only, which would talk over the weather's wind. A built-in
     * resource pack (always on, only when AmbientSounds is installed) sets that one AmbientSounds category to volume 0.
     */
    public static void addPacks(net.neoforged.neoforge.event.AddPackFindersEvent e) {
        if (e.getPackType() != net.minecraft.server.packs.PackType.CLIENT_RESOURCES || !Compat.on(Compat.AMBIENTSOUNDS)) return;
        boolean mute = !ClientConfig.SPEC.isLoaded() || (ClientConfig.WIND_SOUND.get() && ClientConfig.MUTE_AMBIENTSOUNDS_WIND.get());
        if (!mute) return;
        e.addPackFinders(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.barofailure.weatherdirector.WeatherDirectorMod.MOD_ID, "resourcepacks/ambientsounds_wind"),
                net.minecraft.server.packs.PackType.CLIENT_RESOURCES, net.minecraft.network.chat.Component.literal("Weather Director: one wind"),
                net.minecraft.server.packs.repository.PackSource.BUILT_IN, true, net.minecraft.server.packs.repository.Pack.Position.TOP);
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> { ClientWeather.tick(); WeatherSounds.tick(); });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> { ClientWeather.reset(); WeatherSounds.reset(); });
        NeoForge.EVENT_BUS.addListener(ClientEvents::renderFog);
        NeoForge.EVENT_BUS.addListener(ClientEvents::fogColor);
        if (Compat.on(Compat.DISTANT_HORIZONS)) DhFog.register();
    }

    private static boolean plainAir(FogType type, Entity camera) {
        if (type != FogType.NONE) return false;
        return !(camera instanceof LivingEntity living)
                || !(living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS));
    }

    /**
     * Only fog, thunderstorms and the inside of the cloud deck shorten the view, all through the one WeatherFog curve
     * (vanilla chunks and DH LODs alike, no ring). Mist and the fog layer itself are ground fog (GroundFog).
     */
    private static void renderFog(ViewportEvent.RenderFog e) {
        Minecraft mc = Minecraft.getInstance();
        if (!ClientWeather.active(mc.level) || !plainAir(e.getType(), e.getCamera().getEntity())) return;
        if (WeatherFog.active()) {
            float start = WeatherFog.start(), end = WeatherFog.end();
            if (e.getMode() == FogRenderer.FogMode.FOG_SKY) {
                // The sky's horizon greys out together with the land under it (no blue band below the grey).
                e.setNearPlaneDistance(0f);
                e.setFarPlaneDistance(e.getFarPlaneDistance() * Math.min(1f, end / WeatherFog.limit()));
            } else {
                e.setNearPlaneDistance(start);
                e.setFarPlaneDistance(end);
            }
            e.setCanceled(true);
        }
    }

    /** Grey weather washes the fog colour out; inside the cloud deck it turns cloud grey. */
    private static void fogColor(ViewportEvent.ComputeFogColor e) {
        Minecraft mc = Minecraft.getInstance();
        if (!ClientWeather.active(mc.level) || !plainAir(e.getCamera().getFluidInCamera(), e.getCamera().getEntity())) return;
        // Horizon under grey weather: same colour wash as the sky (SkyLook.sky), so no blue band below the clouds.
        float grey = !ClientConfig.GREY_SKIES.get() ? 0f : Mth.clamp((ClientWeather.gloom - 0.15f) / 0.6f, 0f, 1f) * 0.75f * (1f - CloudLayer.above());
        if (grey > 0.001f) {
            float l = e.getRed() * 0.3f + e.getGreen() * 0.59f + e.getBlue() * 0.11f;
            e.setRed(Mth.lerp(grey, e.getRed(), l * 1.02f));
            e.setGreen(Mth.lerp(grey, e.getGreen(), l * 1.03f));
            e.setBlue(Mth.lerp(grey, e.getBlue(), l * 1.08f));
        }
        // Night fog and mist: the distance-fog wall would be the near-black night sky (smog); fade it to moonlit grey.
        float t = mc.level.getTimeOfDay((float) e.getPartialTick());
        float m = GroundFog.mist(), f = GroundFog.fog();
        float nf = (1f - SkyLook.day(t)) * Math.max(f * 0.85f, m * 0.45f);
        if (nf > 0.002f) {
            float share = f / Math.max(0.001f, m + f);
            float[] mc1 = SkyLook.mistColor(t), fc = SkyLook.denseFogColor(t);
            e.setRed(Mth.lerp(nf, e.getRed(), Mth.lerp(share, mc1[0], fc[0])));
            e.setGreen(Mth.lerp(nf, e.getGreen(), Mth.lerp(share, mc1[1], fc[1])));
            e.setBlue(Mth.lerp(nf, e.getBlue(), Mth.lerp(share, mc1[2], fc[2])));
        }
        float wf = CloudLayer.inside() * 0.85f;
        if (wf > 0.002f) {
            float[] c = SkyLook.denseFogColor(mc.level.getTimeOfDay((float) e.getPartialTick()));
            e.setRed(Mth.lerp(wf, e.getRed(), c[0]));
            e.setGreen(Mth.lerp(wf, e.getGreen(), c[1]));
            e.setBlue(Mth.lerp(wf, e.getBlue(), c[2]));
        }
    }
}
