package net.mazebound.world;

import net.mazebound.exploration.Cell;
import net.mazebound.exploration.CellDirection;
import org.bukkit.Material;
import org.bukkit.World;

public class MazeCellBuilder {

    public static final int MAZE_FLOOR_Y = 50;
    public static final int WALL_HEIGHT = 6;
    private static final int CELL_SIZE = 55;

    private final World world;

    public MazeCellBuilder(World world) {
        this.world = world;
    }

    public void build(Cell cell) {

        int baseX = cell.x() * CELL_SIZE;
        int baseZ = cell.z() * CELL_SIZE;

        buildFloor(baseX, baseZ);

        buildNorthWall(cell, baseX, baseZ);
        buildSouthWall(cell, baseX, baseZ);
        buildWestWall(cell, baseX, baseZ);
        buildEastWall(cell, baseX, baseZ);
    }

    private void buildFloor(int baseX, int baseZ) {

        for (int x = 0; x < CELL_SIZE; x++) {
            for (int z = 0; z < CELL_SIZE; z++) {

                world.getBlockAt(
                        baseX + x,
                        MAZE_FLOOR_Y,
                        baseZ + z
                ).setType(Material.STONE);
            }
        }
    }

    private void buildNorthWall(
            Cell cell,
            int baseX,
            int baseZ
    ) {

        if (cell.hasLargeRoomConnection(CellDirection.NORTH)) {
            return;
        }

        boolean hasOpening =
                cell.hasOpening(CellDirection.NORTH);

        for (int i = 0; i < CELL_SIZE; i++) {

            if (hasOpening && isOpeningPosition(i)) {
                continue;
            }

            buildWall(baseX + i, baseZ);
        }
    }

    private void buildSouthWall(
            Cell cell,
            int baseX,
            int baseZ
    ) {

        if (cell.hasLargeRoomConnection(CellDirection.SOUTH)) {
            return;
        }

        boolean hasOpening =
                cell.hasOpening(CellDirection.SOUTH);

        for (int i = 0; i < CELL_SIZE; i++) {

            if (hasOpening && isOpeningPosition(i)) {
                continue;
            }

            buildWall(baseX + i, baseZ + CELL_SIZE - 1);
        }
    }

    private void buildWestWall(
            Cell cell,
            int baseX,
            int baseZ
    ) {

        if (cell.hasLargeRoomConnection(CellDirection.WEST)) {
            return;
        }

        boolean hasOpening =
                cell.hasOpening(CellDirection.WEST);

        for (int i = 0; i < CELL_SIZE; i++) {

            if (hasOpening && isOpeningPosition(i)) {
                continue;
            }

            buildWall(baseX, baseZ + i);
        }
    }

    private void buildEastWall(
            Cell cell,
            int baseX,
            int baseZ
    ) {

        if (cell.hasLargeRoomConnection(CellDirection.EAST)) {
            return;
        }

        boolean hasOpening =
                cell.hasOpening(CellDirection.EAST);

        for (int i = 0; i < CELL_SIZE; i++) {

            if (hasOpening && isOpeningPosition(i)) {
                continue;
            }

            buildWall(
                    baseX + CELL_SIZE - 1,
                    baseZ + i
            );
        }
    }

    private void buildWall(int x, int z) {

        for (int y = 1; y <= WALL_HEIGHT; y++) {

            world.getBlockAt(
                    x,
                    MAZE_FLOOR_Y + y,
                    z
            ).setType(Material.STONE_BRICKS);
        }
    }

    private boolean isOpeningPosition(int position) {

        int center = 27;
        int halfWidth = 5;

        return position >= center - halfWidth
                && position <= center + halfWidth;
    }
}