package net.mazebound.exploration;

import net.mazebound.data.PlayerDataManager;
import org.bukkit.Location;

import java.util.UUID;

public class ExplorationManager {

    private static final int CELL_SIZE = 55;

    private final PlayerDataManager playerDataManager;
    private final RegionManager regionManager;

    public ExplorationManager(PlayerDataManager playerDataManager) {
        this.playerDataManager = playerDataManager;
        this.regionManager = new RegionManager();
    }

    public boolean discoverCell(UUID playerId, String cellId) {

        return playerDataManager
                .getPlayerData(playerId)
                .discoverCell(cellId);
    }

    public Cell getCellFromLocation(Location location) {

        int cellX = Math.floorDiv(
                location.getBlockX(),
                CELL_SIZE
        );

        int cellZ = Math.floorDiv(
                location.getBlockZ(),
                CELL_SIZE
        );

        return new Cell(cellX, cellZ);
    }

    public RegionType getRegionFromLocation(Location location) {

        Cell cell = getCellFromLocation(location);

        return regionManager.getRegionType(cell);
    }

    public boolean isMazeLocation(Location location) {

        return getRegionFromLocation(location)
                == RegionType.MAZE;
    }
}