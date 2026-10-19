package com.barofailure.weatherdirector.client;

import com.barofailure.weatherdirector.WeatherDirectorMod;
import com.barofailure.weatherdirector.mixin.client.ChannelAccessor;
import com.barofailure.weatherdirector.mixin.client.SoundEngineAccessor;
import com.barofailure.weatherdirector.mixin.client.SoundManagerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.EXTEfx;

/**
 * Muffles the wind under a roof and indoors with a real low-pass filter (OpenAL EFX) on the wind's own sound sources.
 * One filter per wind layer, made once and moved to the layer's current source; the game makes a fresh source for every
 * sound, so nothing else ever plays through it. Sound Physics leaves these sources alone (they have no distance
 * attenuation). Without EFX the wind is only quieter indoors, not duller.
 */
final class WindFilter {
    /** Filter ids per layer; touched only on the sound thread. */
    private static final int[] ids = new int[8];
    private static volatile boolean disabled;
    private static boolean checked;

    private WindFilter() {}

    /** Client thread: hands the brightness (1 = open air, lower = duller) to the layer's source; false = no source yet. */
    static boolean apply(SoundInstance sound, int slot, float hf) {
        if (disabled) return true;
        ChannelAccess.ChannelHandle handle = ((SoundEngineAccessor) ((SoundManagerAccessor) Minecraft.getInstance().getSoundManager())
                .weatherDirector$soundEngine()).weatherDirector$instanceToChannel().get(sound);
        if (handle == null) return false;
        handle.execute(channel -> set(((ChannelAccessor) channel).weatherDirector$source(), slot, hf));
        return true;
    }

    /** Sound thread. */
    private static void set(int source, int slot, float hf) {
        if (disabled) return;
        try {
            if (!checked) {
                checked = true;
                if (!AL.getCapabilities().ALC_EXT_EFX) {
                    disabled = true;
                    WeatherDirectorMod.LOG.info("Weather Director: no OpenAL EFX, indoor wind is quieter but not muffled");
                    return;
                }
            }
            AL10.alGetError(); // clear errors left by other code on this thread
            if (ids[slot] == 0) {
                ids[slot] = EXTEfx.alGenFilters();
                EXTEfx.alFilteri(ids[slot], EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
                EXTEfx.alFilterf(ids[slot], EXTEfx.AL_LOWPASS_GAIN, 1f);
            }
            EXTEfx.alFilterf(ids[slot], EXTEfx.AL_LOWPASS_GAINHF, Math.max(0f, Math.min(1f, hf)));
            // A changed filter only takes effect when it is attached again.
            AL10.alSourcei(source, EXTEfx.AL_DIRECT_FILTER, ids[slot]);
            int error = AL10.alGetError();
            if (error != AL10.AL_NO_ERROR) {
                disabled = true;
                WeatherDirectorMod.LOG.warn("Weather Director: OpenAL error {} on the wind filter, indoor wind is quieter but not muffled", error);
            }
        } catch (Throwable t) {
            disabled = true;
            WeatherDirectorMod.LOG.warn("Weather Director: wind filter off ({}), indoor wind is quieter but not muffled", t.toString());
        }
    }

    /** For the debug log: a filter has been made and EFX works. */
    static boolean working() {
        return !disabled && ids[0] != 0;
    }
}
