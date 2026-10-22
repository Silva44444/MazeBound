package net.mazebound.world;

import org.bukkit.Material;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Runs after vanilla (and Tectonic) terrain generation for each NEW chunk
 * and stamps the maze on top of it. Only chunks generated after this
 * populator was attached are affected.
 *
 * Runs on the world-generation thread: it only touches the LimitedRegion
 * it is given, never Bukkit worlds, players, or entities.
 */
public class MazePopulator extends BlockPopulator {

    private static final int DEBUG_LOG_LIMIT = 20;

    private final MazeRenderer renderer;
    private final Logger logger;
    private final boolean debug;

    private final AtomicInteger debugLogged = new AtomicInteger();
    private final AtomicBoolean warnedAboutHeight = new AtomicBoolean();

    public MazePopulator(MazeRenderer renderer, Logger logger, boolean debug) {
        this.renderer = renderer;
        this.logger = logger;
        this.debug = debug;
    }

    @Override
    public void populate(
            WorldInfo worldInfo,
            Random random,
            int chunkX,
            int chunkZ,
            LimitedRegion region
    ) {
        int minY = worldInfo.getMinHeight();
        int maxY = worldInfo.getMaxHeight();

        if (!renderer.fitsWorld(minY, maxY)) {

            if (warnedAboutHeight.compareAndSet(false, true)) {
                logger.severe(
                        "maze.floor-y (" + renderer.floorY()
                                + ") does not fit this world's height range ("
                                + minY + " to " + maxY
                                + "). The maze will NOT be generated."
                );
            }

            return;
        }

        boolean touched = renderer.renderChunk(
                chunkX,
                chunkZ,
                minY,
                maxY,
                new RegionSink(region)
        );

        if (debug && touched
                && debugLogged.incrementAndGet() <= DEBUG_LOG_LIMIT) {

            logger.info("Maze populated chunk " + chunkX + "," + chunkZ);
        }
    }

    private static final class RegionSink implements BlockSink {

        private final LimitedRegion region;

        private RegionSink(LimitedRegion region) {
            this.region = region;
        }

        @Override
        public Material getType(int x, int y, int z) {
            return region.getType(x, y, z);
        }

        @Override
        public void setType(int x, int y, int z, Material material) {
            region.setType(x, y, z, material);
        }
    }
}