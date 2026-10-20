package com.barofailure.weatherdirector.client;

import net.irisshaders.iris.api.v0.IrisApi;

/** Iris' public API. Only loaded when Iris is installed (Compat.IRIS). */
final class IrisState {
    private IrisState() {}

    static boolean shadersOn() {
        return IrisApi.getInstance().isShaderPackInUse();
    }
}
