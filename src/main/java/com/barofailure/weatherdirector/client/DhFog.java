package com.barofailure.weatherdirector.client;

import com.seibel.distanthorizons.api.methods.events.DhApiEventRegister;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeFogRenderEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiCancelableEventParam;
import com.seibel.distanthorizons.api.enums.rendering.EDhApiFogFalloff;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiFogRenderParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiMutableFogRenderParam;

/** Distant Horizons' LOD fog: same curve as the chunks inside the cloud deck, a slightly earlier far fade in storms. Only loaded with DH. */
final class DhFog extends DhApiBeforeFogRenderEvent {
    private DhFog() {}

    static void register() {
        DhApiEventRegister.on(DhApiBeforeFogRenderEvent.class, new DhFog());
    }

    /** DH's LOD radius in blocks: its far fog is measured against it (uFogScale = 1 / (lodChunkRenderDistanceRadius * 16)). */
    static float radius() {
        try {
            var configs = com.seibel.distanthorizons.api.DhApi.Delayed.configs;
            Integer chunks = configs == null ? null : configs.graphics().chunkRenderDistance().getValue();
            return Math.max(256f, (chunks == null ? 128 : chunks) * 16f);
        } catch (RuntimeException e) {
            return 2048f;
        }
    }

    @Override
    public void beforeRender(DhApiCancelableEventParam<EventParam> event) {
        if (!ClientWeather.active()) return;
        DhApiMutableFogRenderParam fog = event.value.getFogRenderParam();
        if (WeatherFog.active()) {
            float radius = radius();
            fog.setFarFogFalloff(EDhApiFogFalloff.LINEAR);
            fog.setFarFogStartPercent(WeatherFog.start() / radius);
            fog.setFarFogEndPercent(WeatherFog.end() / radius);
            fog.setFarFogMinThickness(0f);
            fog.setFarFogMaxThickness(1f);
            return;
        }
    }
}
