package com.barofailure.weatherdirector.net;

import com.barofailure.weatherdirector.WeatherDirectorMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server -> client, every 5 s and on every change: what the sky should look like (targets the client eases
 * towards), the wind and the shared cloud drift. Optional channel: clients without the mod still join.
 */
public record WeatherSyncPayload(
        int kind, int season, float rampSeconds, float cloudRampSeconds, float fogRampSeconds, float coverNow,
        float cover, float gloom, float haze, float mist, float fog,
        float windAngle, float windSpeed, double cloudX, double cloudZ, float cloudSpeed,
        float rainWindScale, boolean snap) implements CustomPacketPayload {

    public static final Type<WeatherSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WeatherDirectorMod.MOD_ID, "sync"));

    public static final StreamCodec<FriendlyByteBuf, WeatherSyncPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeByte(p.kind);
                buf.writeByte(p.season);
                buf.writeFloat(p.rampSeconds);
                buf.writeFloat(p.cloudRampSeconds);
                buf.writeFloat(p.fogRampSeconds);
                buf.writeFloat(p.coverNow);
                buf.writeFloat(p.cover);
                buf.writeFloat(p.gloom);
                buf.writeFloat(p.haze);
                buf.writeFloat(p.mist);
                buf.writeFloat(p.fog);
                buf.writeFloat(p.windAngle);
                buf.writeFloat(p.windSpeed);
                buf.writeDouble(p.cloudX);
                buf.writeDouble(p.cloudZ);
                buf.writeFloat(p.cloudSpeed);
                buf.writeFloat(p.rainWindScale);
                buf.writeBoolean(p.snap);
            },
            buf -> new WeatherSyncPayload(buf.readByte(), buf.readByte(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                    buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                    buf.readFloat(), buf.readFloat(), buf.readDouble(), buf.readDouble(), buf.readFloat(),
                    buf.readFloat(), buf.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
