package net.mazebound.exploration;

public class RegionManager {

    public static final int HUB_SIZE = 9;
    public static final int HUB_RADIUS = HUB_SIZE / 2;

    public RegionManager() {
    }

    public RegionType getRegionType(Cell cell) {
        int x = cell.x();
        int z = cell.z();

        if (Math.abs(x) <= HUB_RADIUS && Math.abs(z) <= HUB_RADIUS) {
            return RegionType.HUB;
        }

        return RegionType.MAZE;
    }
}