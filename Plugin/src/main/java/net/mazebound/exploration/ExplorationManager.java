package net.mazebound.exploration;

import net.mazebound.data.PlayerData;
import net.mazebound.data.PlayerDataManager;
import net.mazebound.data.PlayerData;
import net.mazebound.data.MilestoneManager;
import org.bukkit.Location;

import java.util.UUID;

public class ExplorationManager {

    private static final int CELL_SIZE = 55;

    private final PlayerDataManager playerDataManager;
    private final RegionManager regionManager;
    private final MilestoneManager milestoneManager;

    public ExplorationManager(PlayerDataManager playerDataManager) {
    this.playerDataManager = playerDataManager;
    this.regionManager = new RegionManager();
    this.milestoneManager =
            new MilestoneManager(playerDataManager);
}
   public boolean discoverCell(UUID playerId, Location location) {

    if (!isMazeLocation(location)) {
        return false;
    }

    Cell cell = getCellFromLocation(location);
    String cellId = cell.getId();

    PlayerData data =
            playerDataManager.getPlayerData(playerId);

    boolean discovered =
            data.discoverCell(cellId);

    if (!discovered) {
        return false;
    }

    data.addXp(10);

milestoneManager.checkMilestones(playerId);

return true;

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
