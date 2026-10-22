package net.mazebound.world;

import org.bukkit.Material;

/**
 * Where the renderer reads and writes blocks. The populator wraps a
 * LimitedRegion in one of these; tests can use a plain in-memory map.
 */
public interface BlockSink {

    Material getType(int x, int y, int z);

    void setType(int x, int y, int z, Material material);
}