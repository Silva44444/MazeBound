package net.mazebound.world;

import org.bukkit.Material;

/**
 * Where the renderer reads and writes blocks. The populator wraps a
 * LimitedRegion in one of these; tests can use a plain in-memory map.
 */
public interface BlockSink {

    Material getType(int x, int y, int z);

    void setType(int x, int y, int z, Material material);

    /** Biome key at a position, e.g. "minecraft:desert", or "" if unknown. */
    default String getBiomeKey(int x, int y, int z) {
        return "";
    }

    /**
     * Places a brushable block (suspicious sand) and, where possible,
     * attaches a vanilla archaeology loot table to it.
     */
    default void setBrushable(
            int x,
            int y,
            int z,
            Material material,
            String lootTable
    ) {
        setType(x, y, z, material);
    }
}