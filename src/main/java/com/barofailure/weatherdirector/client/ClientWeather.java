package com.barofailure.weatherdirector.client;

import com.barofailure.weatherdirector.WeatherKind;
import com.barofailure.weatherdirector.net.WeatherSyncPayload;
import com.barofailure.weatherdirector.WeatherDirectorMod;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import com.barofailure.weatherdirector.Compat;

/**
 * What the server told us about the sky, eased smoothly on the client. Everything here is only used while
 * the player is in the overworld of a server that runs Weather Director; otherwise the game looks vanilla.
 */
public final class ClientWeather {
    private static boolean received;
    public static WeatherKind kind = WeatherKind.CLEAR;
    /** 0 spring, 1 summer, 2 autumn, 3 winter, 4 none. */
    public static int season = 4;
    private static float rampTicks = 4800;
    private static float cloudRampTicks = 9600;
    private static float serverCover = -1;

    private static float fogRampTicks = 1800;
    private static float coverTarget, gloomTarget, hazeTarget, mistTarget, fogTarget;
    public static float cover, gloom;
    /** Raw eased levels (0..1); use haze()/mist()/fog() for what the camera should see. */
    public static float hazeLevel, mistLevel, fogLevel;

    private static float windAngleServer, windSpeedServer;
    public static float windAngle, windSpeed;
    public static float rainWindScale = 1;
    /** 0 indoors/underground .. 1 under open sky, eased (Dynamic Surroundings gates weather fog the same way). */
    public static float skyExposure = 1;

    private static double serverCloudX, serverCloudZ;
    private static double cloudX, cloudZ, prevCloudX, prevCloudZ;
    private static float cloudSpeed;

    private ClientWeather() {}

    public static void accept(WeatherSyncPayload p) {
        ensureClouds();
        WeatherKind[] kinds = WeatherKind.values();
        kind = p.kind() >= 0 && p.kind() < kinds.length ? kinds[p.kind()] : WeatherKind.CLEAR;
        season = p.season();
        rampTicks = Math.max(20, p.rampSeconds() * 20);
        cloudRampTicks = Math.max(20, p.cloudRampSeconds() * 20);
        serverCover = p.coverNow();
        coverTarget = p.cover();
        gloomTarget = p.gloom();
        hazeTarget = p.haze();
        mistTarget = p.mist();
        fogTarget = p.fog();
        fogRampTicks = Math.max(20, p.fogRampSeconds() * 20);
        windAngleServer = p.windAngle();
        windSpeedServer = p.windSpeed();
        cloudSpeed = p.cloudSpeed();
        rainWindScale = p.rainWindScale();
        serverCloudX = p.cloudX();
        serverCloudZ = p.cloudZ();
        if (!received || p.snap()) {
            // First packet (or /wd set ... now): start where the server is, no easing.
            cover = serverCover >= 0 ? serverCover : coverTarget;
            gloom = gloomTarget;
            hazeLevel = kind.wet() ? hazeTarget * Mth.clamp((cover - 0.75f) / 0.17f, 0f, 1f) : hazeTarget;
            mistLevel = mistTarget;
            fogLevel = fogTarget;
            windAngle = windAngleServer;
            windSpeed = windSpeedServer;
            cloudX = prevCloudX = serverCloudX;
            cloudZ = prevCloudZ = serverCloudZ;
            received = true;
        }
    }

    /**
     * Distant Horizons switches the vanilla cloud option off at every start (to draw its own clouds), and
     * Better Clouds then draws nothing. Our weather lives in Better Clouds' clouds, so while a Weather
     * Director server is sending, clouds stay on. Better Clouds turns DH's own clouds off, so they never double.
     */
    private static long joinedAt = -1;

    private static void ensureClouds() {
        if (!Compat.on(Compat.BETTER_CLOUDS) || !ClientConfig.KEEP_CLOUDS_ON.get() || !ClientConfig.CONTROL_CLOUDS.get()) return;
        long now = System.currentTimeMillis();
        if (joinedAt < 0) joinedAt = now;
        // Distant Horizons switches clouds off during its start-up, shortly after joining; only undo that, so a player who
        // turns clouds off later keeps them off.
        if (now - joinedAt > 60_000) return;
        Options options = Minecraft.getInstance().options;
        if (options.cloudStatus().get() == CloudStatus.OFF) {
            options.cloudStatus().set(CloudStatus.FANCY);
            options.save();
            WeatherDirectorMod.LOG.info("Turned clouds back on (Distant Horizons switches them off; weather needs them)");
        }
    }

    public static void reset() {
        joinedAt = -1;
        received = false;
    }

    public static void tick() {
        if (!received) return;
        // Clouds build slowly (cloud ramp), haze with the rain (rain ramp). Cover follows the server's own eased
        // cover, which is what decides when rain may start, so clouds and rain stay in step.
        float cloudStep = 1f / cloudRampTicks, step = 1f / rampTicks;
        float coverGoal = serverCover >= 0 ? serverCover : coverTarget;
        cover += Mth.clamp(coverGoal - cover, -cloudStep * 2f, cloudStep * 2f);
        if (serverCover >= 0) serverCover += Mth.clamp(coverTarget - serverCover, -cloudStep, cloudStep);
        gloom += Mth.clamp(gloomTarget - gloom, -cloudStep, cloudStep);
        float fogStep = 1f / fogRampTicks;
        // A storm's haze comes with its clouds (like the rain, which waits for a thick deck); fog's haze does not wait.
        float hazeGoal = kind.wet() ? hazeTarget * Mth.clamp((cover - 0.75f) / 0.17f, 0f, 1f) : hazeTarget;
        hazeLevel += Mth.clamp(hazeGoal - hazeLevel, -fogStep, fogStep);
        mistLevel += Mth.clamp(mistTarget - mistLevel, -fogStep, fogStep);
        fogLevel += Mth.clamp(fogTarget - fogLevel, -fogStep, fogStep);
        CloudLayer.tick();
        Minecraft mcDbg = Minecraft.getInstance();
        if (mcDbg.level != null && mcDbg.level.getGameTime() % 100 == 0) {
            WeatherDirectorMod.LOG.debug("client weather {}: fps {} particles {} | cover {} gloom {} haze {} mist {} fog {} | sky {} above {} inside {} | layer {}..{} cam y {}",
                    kind, mcDbg.getFps(), mcDbg.particleEngine.countParticles(), cover, gloom, hazeLevel, mistLevel, fogLevel, skyExposure, CloudLayer.above(), CloudLayer.inside(),
                    CloudLayer.bottom(), CloudLayer.top(), Math.round(mcDbg.gameRenderer.getMainCamera().getPosition().y));
        }
        windSpeed += (windSpeedServer - windSpeed) * 0.05f;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.gameRenderer.getMainCamera().isInitialized()) {
            int sky = mc.level.getBrightness(net.minecraft.world.level.LightLayer.SKY, mc.gameRenderer.getMainCamera().getBlockPosition());
            skyExposure += (Mth.clamp((sky - 8) / 7f, 0f, 1f) - skyExposure) * 0.05f;
        }
        windAngle += Mth.wrapDegrees((windAngleServer - windAngle) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD * 0.05f;
        // Clouds: the server's drift and ours advance alike; small corrections fade in instead of jumping.
        double vx = Mth.cos(windAngle) * cloudSpeed, vz = Mth.sin(windAngle) * cloudSpeed;
        serverCloudX += vx;
        serverCloudZ += vz;
        prevCloudX = cloudX;
        prevCloudZ = cloudZ;
        cloudX += vx + (serverCloudX - cloudX) * 0.05;
        cloudZ += vz + (serverCloudZ - cloudZ) * 0.05;
    }

    /** True when the sky of this level is ours to change. */
    public static boolean active(Level level) {
        return received && level instanceof ClientLevel && level.dimension() == Level.OVERWORLD;
    }

    public static boolean active() {
        return active(Minecraft.getInstance().level);
    }

    public static double cloudX(float partialTick) {
        return Mth.lerp(partialTick, prevCloudX, cloudX);
    }

    public static double cloudZ(float partialTick) {
        return Mth.lerp(partialTick, prevCloudZ, cloudZ);
    }

    /** How much open sky weather reaches the camera: none indoors, underground or above the cloud deck. */
    private static float outside() {
        return skyExposure * (1f - CloudLayer.above());
    }

    /** View obscuration 0..1 (fog and thunderstorms only), under open sky and below the cloud deck. */
    public static float haze() {
        return hazeLevel * outside();
    }

    /** True while weather takes over the game's fog (only inside the cloud deck). */
    public static boolean weatherFogUp() {
        return WeatherFog.active();
    }

    /** Rain strength relative to normal rain (0.65), for how dense rain particles should be. */
    public static float precipitationScale(Level level) {
        // No floor: light rain starts with single drops, not a burst.
        return (float) Math.pow(Mth.clamp(level.getRainLevel(1f) / 0.65f, 0f, 1f), 1.3);
    }
}
