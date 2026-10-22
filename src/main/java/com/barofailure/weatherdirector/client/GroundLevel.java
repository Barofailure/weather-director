package com.barofailure.weatherdirector.client;

import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import com.barofailure.weatherdirector.WeatherDirectorMod;

/**
 * The height of the ground around the player, where mist and ground fog lie: the median surface height (leaves, logs and
 * plants do not count, water does) of a 7 x 7 grid, 8 blocks apart, around the camera, eased so it never jumps. A shader pack gets the top
 * of that fog layer (wdFogTop), the way Particle Rain's fog particles lie on the ground without shaders.
 */
public final class GroundLevel {
    private static final int R = 3, STEP = 8;
    private static final int[] HEIGHTS = new int[(2 * R + 1) * (2 * R + 1)];
    private static float ground = Float.NaN;
    private static final BlockPos.MutableBlockPos POS = new BlockPos.MutableBlockPos();

    private GroundLevel() {}

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || !mc.gameRenderer.getMainCamera().isInitialized() || level.getGameTime() % 10 != 0) return;
        var cam = mc.gameRenderer.getMainCamera().getBlockPosition();
        int n = 0, min = level.getMinBuildHeight();
        for (int dx = -R; dx <= R; dx++)
            for (int dz = -R; dz <= R; dz++) {
                int x = cam.getX() + dx * STEP, z = cam.getZ() + dz * STEP;
                if (!level.hasChunk(x >> 4, z >> 4)) continue;
                // the client only has the heightmaps the server sends (leaves and plants count): walk down to the ground
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                POS.set(x, y - 1, z);
                for (int k = 0; k < 24 && POS.getY() > min && soft(level.getBlockState(POS)); k++) POS.move(0, -1, 0);
                if (POS.getY() > min) HEIGHTS[n++] = POS.getY() + 1;
            }
        if (n == 0) {
            if (level.getGameTime() % 1200 == 0) WeatherDirectorMod.LOG.info("Weather Director: ground fog, no loaded surface around {}", cam);
            return;
        }
        Arrays.sort(HEIGHTS, 0, n);
        float median = HEIGHTS[n / 2];
        if (Float.isNaN(ground)) WeatherDirectorMod.LOG.info("Weather Director: ground fog lies at y {} ({} samples)", median, n);
        ground = Float.isNaN(ground) ? median : ground + (median - ground) * 0.25f;
    }

    /** Not the ground: leaves, logs, plants (what the fog lies around, not on). Water is ground: fog lies on lakes. */
    private static boolean soft(BlockState state) {
        if (!state.getFluidState().isEmpty()) return false;
        return state.isAir() || state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.canBeReplaced();
    }

    /** The top of the mist / ground fog layer in blocks (mist lies about 4 blocks deep, thick fog about 9). */
    public static float fogTop() {
        if (Float.isNaN(ground)) return Float.NaN;
        return ground + 4f + 5f * ClientWeather.fogLevel;
    }
}
