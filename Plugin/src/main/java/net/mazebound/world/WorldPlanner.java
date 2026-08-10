package net.mazebound.world;

import net.mazebound.exploration.Cell;
import net.mazebound.exploration.RegionManager;
import net.mazebound.exploration.RegionType;

import java.util.Map;

public class WorldPlanner {

    private final RegionManager regionManager;

    public WorldPlanner(RegionManager regionManager) {
        this.regionManager = regionManager;
    }

    public void plan(Map<String, Cell> cells) {
        for (Cell cell : cells.values()) {
            RegionType regionType = regionManager.getRegionType(cell);

            // The hub is handled by RegionManager.
            // Other region types will be assigned here as the
            // world-generation system becomes more advanced.
            if (regionType == RegionType.HUB) {
                continue;
            }
        }
    }

    public RegionType getRegionType(Cell cell) {
        return regionManager.getRegionType(cell);
    }
}