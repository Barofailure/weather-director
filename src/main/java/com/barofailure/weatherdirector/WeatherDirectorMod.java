package com.barofailure.weatherdirector;

import com.barofailure.weatherdirector.client.ClientEvents;
import com.barofailure.weatherdirector.client.ClientWeather;
import com.barofailure.weatherdirector.net.WeatherSyncPayload;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

/**
 * Weather Director: one weather for the whole overworld, picked by season, with real in-between kinds
 * (drizzle, heavy rain, overcast, fog...). Server: rain/thunder strength via vanilla packets, wind, cloud drift.
 * Client (optional): cloud cover and colour, fog, wind-driven rain, seasonal skies, played out by
 * Better Clouds, Particle Rain and Distant Horizons when they are installed (see Compat).
 */
@Mod(WeatherDirectorMod.MOD_ID)
public class WeatherDirectorMod {
    public static final String MOD_ID = "weather_director";
    public static final Logger LOG = LogUtils.getLogger();

    public WeatherDirectorMod(IEventBus modBus, Dist dist, net.neoforged.fml.ModContainer container) {
        modBus.addListener(WeatherDirectorMod::registerPayloads);
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> WeatherDirector.serverStarted(e.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> WeatherDirector.serverStopped());
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> WeatherCommand.register(e.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Pre e) -> {
            if (e.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) WeatherDirector.tick(level);
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> loggedIn(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> syncTo(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> syncTo(e.getEntity()));
        if (dist.isClient()) {
            container.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT, com.barofailure.weatherdirector.client.ClientConfig.SPEC);
            com.barofailure.weatherdirector.client.ClientEvents.registerConfigScreen(container);
            ClientEvents.init();
            modBus.addListener(ClientEvents::addPacks);
        }
    }

    private static void syncTo(net.minecraft.world.entity.player.Player player) {
        if (player instanceof ServerPlayer sp) WeatherDirector.sync(sp);
    }

    private static void loggedIn(net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        LOG.info("{} joined: weather client part {}", sp.getGameProfile().getName(),
                WeatherDirector.hasClientPart(sp) ? "present" : "missing (vanilla skies for this player)");
        WeatherDirector.sync(sp);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent e) {
        // Optional: players without the mod can still join, they just see vanilla skies.
        e.registrar("1").optional().playToClient(WeatherSyncPayload.TYPE, WeatherSyncPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientWeather.accept(payload)));
    }
}
