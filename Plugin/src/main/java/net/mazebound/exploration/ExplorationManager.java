package net.mazebound.exploration;

import net.mazebound.Mazebound;
import net.mazebound.data.MilestoneManager;
import net.mazebound.data.PlayerData;
import net.mazebound.data.PlayerDataManager;
import net.mazebound.world.MazeLayout;
import org.bukkit.Location;

import java.util.UUID;

public class ExplorationManager {

    private static final int CELL_SIZE = Mazebound.CELL_SIZE;

    private final PlayerDataManager playerDataManager;
    private final RegionManager regionManager;
    private final MilestoneManager milestoneManager;

    // Set from Mazebound.onEnable once the maze has been generated.
    private MazeLayout layout;

    public ExplorationManager(PlayerDataManager playerDataManager) {
        this.playerDataManager = playerDataManager;
        this.regionManager = new RegionManager();
        this.milestoneManager =
                new MilestoneManager(playerDataManager);
    }

    public void setLayout(MazeLayout layout) {
        this.layout = layout;
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

    /**
     * True only inside a cell that actually exists in the generated maze
     * (not in the hub, and not outside the maze bounds).
     */
    public boolean isMazeLocation(Location location) {

        return layout != null
                && layout.isMazeBlock(
                location.getBlockX(),
                location.getBlockZ()
        );
    }
}