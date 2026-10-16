package com.barofailure.weatherdirector.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Where the camera stands for the wind: in the open, sheltered (a roof but open sides: porch, shed, canopy) or indoors
 * (roof and walls). Twice a second a handful of short rays look up and around: what blocks them, whether a wall has open
 * sky right behind it (a house wall, not a cave wall) and where the nearest opening is.
 * The three weights ease smoothly so walking through a door blends the sounds.
 */
public final class WindPlace {
    public static float open = 1f, sheltered, indoor;
    private static float openGoal = 1f, shelteredGoal, indoorGoal;
    /** Diagnostics for the debug log: roof cover, open sides, exterior walls. */
    static float roof, sides, exterior;
    /** Where the indoor wind should seem to come from (an opening or the outer walls), null = all around. */
    public static Vec3 from;
    private static int clock;

    private static final int SIDE_RAYS = 12, UP_RAYS = 8;
    private static final double SIDE_LEN = 12, ROOF_LEN = 24;

    private WindPlace() {}

    public static void tick(Minecraft mc) {
        if (++clock % 10 == 0) probe(mc);
        open += (openGoal - open) * 0.08f;
        sheltered += (shelteredGoal - sheltered) * 0.08f;
        indoor += (indoorGoal - indoor) * 0.08f;
    }

    private static void probe(Minecraft mc) {
        Level level = mc.level;
        Vec3 eye = mc.gameRenderer.getMainCamera().getPosition();
        // Roof: straight up and four rays tilted 20 degrees.
        int roofHits = 0;
        Vec3[] up = {new Vec3(0, 1, 0), dir(0, 70), dir(90, 70), dir(180, 70), dir(270, 70)};
        for (Vec3 d : up) if (cast(mc, eye, d, ROOF_LEN).getType() != HitResult.Type.MISS) roofHits++;
        roof = roofHits / (float) up.length;
        // Sides: a ring around and a ring tilted up 35 degrees.
        int openRays = 0, hits = 0, ext = 0;
        Vec3 openSum = Vec3.ZERO, extSum = Vec3.ZERO;
        for (int i = 0; i < SIDE_RAYS + UP_RAYS; i++) {
            Vec3 d = i < SIDE_RAYS ? dir(i * 360.0 / SIDE_RAYS, 0) : dir((i - SIDE_RAYS) * 360.0 / UP_RAYS + 22.5, 35);
            BlockHitResult h = cast(mc, eye, d, SIDE_LEN);
            if (h.getType() == HitResult.Type.MISS) {
                openRays++;
                openSum = openSum.add(d);
                continue;
            }
            hits++;
            if (exteriorBehind(level, h.getBlockPos(), d)) {
                ext++;
                extSum = extSum.add(d);
            }
        }
        sides = openRays / (float) (SIDE_RAYS + UP_RAYS);
        exterior = hits == 0 ? 1f : ext / (float) hits;
        int skyLight = level.getBrightness(LightLayer.SKY, BlockPos.containing(eye));
        // Open sky above: open. A roof over open sides: sheltered. A roof with walls: indoors, but only near the outside
        // (outer walls with sky behind them, or sky light coming in); deep in a cave or a sealed bunker it is silent.
        float sideOpen = smooth(sides, 0.2f, 0.55f);
        float nearOutside = Math.max(smooth(exterior, 0.15f, 0.6f), smooth(skyLight, 3f, 11f));
        openGoal = 1f - roof;
        shelteredGoal = roof * sideOpen;
        indoorGoal = roof * (1f - sideOpen) * nearOutside;
        Vec3 toward = openRays > 0 ? openSum : extSum;
        from = toward.lengthSqr() > 0.01 ? eye.add(toward.normalize().scale(3)) : null;
    }

    private static BlockHitResult cast(Minecraft mc, Vec3 from, Vec3 dir, double len) {
        return mc.level.clip(new ClipContext(from, from.add(dir.scale(len)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
    }

    /** True when open sky is right behind a wall (at most 3 blocks thick): a building's outer wall, not rock. */
    private static boolean exteriorBehind(Level level, BlockPos hit, Vec3 d) {
        Vec3 c = Vec3.atCenterOf(hit);
        for (int i = 1; i <= 4; i++) {
            BlockPos p = BlockPos.containing(c.add(d.scale(i)));
            if (level.getBlockState(p).isAir()) return level.canSeeSky(p);
        }
        return false;
    }

    private static Vec3 dir(double yawDeg, double pitchUpDeg) {
        double y = Math.toRadians(yawDeg), p = Math.toRadians(pitchUpDeg);
        return new Vec3(Math.cos(y) * Math.cos(p), Math.sin(p), Math.sin(y) * Math.cos(p));
    }

    static float smooth(float x, float from, float to) {
        float u = Mth.clamp((x - from) / (to - from), 0f, 1f);
        return u * u * (3f - 2f * u);
    }
}
