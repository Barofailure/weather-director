package com.barofailure.weatherdirector.client;

import net.irisshaders.iris.api.v0.IrisApi;

/** Iris' public API. Only loaded when Iris is installed (Compat.IRIS). */
final class IrisState {
    private IrisState() {}

    static boolean shadersOn() {
        return IrisApi.getInstance().isShaderPackInUse();
    }

    /** The active shader pack's name (Iris internals, not its API: empty if that ever changes). */
    static String packName() {
        try {
            String name = net.irisshaders.iris.Iris.getCurrentPackName();
            return name == null ? "" : name;
        } catch (Throwable t) {
            return "";
        }
    }
}
