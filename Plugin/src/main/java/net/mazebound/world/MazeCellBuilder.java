package net.mazebound.world;

import net.mazebound.exploration.Cell;
import net.mazebound.exploration.CellDirection;
import org.bukkit.Material;
import org.bukkit.World;

public class MazeCellBuilder {

    public static final int MAZE_FLOOR_Y = 50;
    public static final int WALL_HEIGHT = 6;

    private final World world;

    public MazeCellBuilder(World world) {
        this.world = world;
    }

    public void build(Cell cell) {

        int baseX = cell.x() * 55;
        int baseZ = cell.z() * 55;

        buildFloor(baseX, baseZ);

        buildNorthWall(cell, baseX, baseZ);
        buildSouthWall(cell, baseX, baseZ);
        buildWestWall(cell, baseX, baseZ);
        buildEastWall(cell, baseX, baseZ);
    }

    private void buildFloor(int baseX, int baseZ) {

        for (int x = 0; x < 55; x++) {
            for (int z = 0; z < 55; z++) {

                world.getBlockAt(
                        baseX + x,
                        MAZE_FLOOR_Y,
                        baseZ + z
                ).setType(Material.STONE);
            }
        }
    }

    private void buildNorthWall(Cell cell, int baseX, int baseZ) {

        boolean hasOpening =
                cell.hasOpening(CellDirection.NORTH);

        for (int i = 0; i < 55; i++) {

            if (hasOpening && isOpeningPosition(i)) {
                continue;
            }

            buildWall(
                    baseX + i,
                    baseZ
            );
        }
    }

    private void buildSouthWall(Cell cell, int baseX, int baseZ) {

        boolean hasOpening =
                cell.hasOpening(CellDirection.SOUTH);

        for (int i = 0; i < 55; i++) {

            if (hasOpening && isOpeningPosition(i)) {
                continue;
            }

            buildWall(
                    baseX + i,
                    baseZ + 54
            );
        }
    }

    private void buildWestWall(Cell cell, int baseX, int baseZ) {

        boolean hasOpening =
                cell.hasOpening(CellDirection.WEST);

        for (int i = 0; i < 55; i++) {

            if (hasOpening && isOpeningPosition(i)) {
                continue;
            }

            buildWall(
                    baseX,
                    baseZ + i
            );
        }
    }

    private void buildEastWall(Cell cell, int baseX, int baseZ) {

        boolean hasOpening =
                cell.hasOpening(CellDirection.EAST);

        for (int i = 0; i < 55; i++) {

            if (hasOpening && isOpeningPosition(i)) {
                continue;
            }

            buildWall(
                    baseX + 54,
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