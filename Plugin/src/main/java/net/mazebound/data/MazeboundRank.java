package net.mazebound.data;

public enum MazeboundRank {

    WANDERER("Wanderer", 0, "&7&lWanderer"),
    PATHFINDER("Pathfinder", 10, "&a&lPathfinder"),
    EXPLORER("Explorer", 25, "&b&lExplorer"),
    TRAILBLAZER("Trailblazer", 50, "&e&lTrailblazer"),
    CARTOGRAPHER("Cartographer", 100, "&6&lCartographer"),
    WAYFINDER("Wayfinder", 200, "&d&lWayfinder"),
    MAZE_MASTER("Maze Master", 400, "&5%lMaze Master"),
    LEGEND("Legend", 800, "&c%lLegend");

    private final String displayName;
    private final int requiredCells;
    private final String prefix;

    MazeboundRank(
            String displayName,
            int requiredCells,
            String prefix
    ) {
        this.displayName = displayName;
        this.requiredCells = requiredCells;
        this.prefix = prefix;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getRequiredCells() {
        return requiredCells;
    }

    public String getPrefix() {
        return prefix;
    }
    
    public static MazeboundRank fromCells(int cells) {

        MazeboundRank current =
                MazeboundRank.WANDERER;

        for (MazeboundRank rank : values()) {

            if (cells >= rank.requiredCells) {
                current = rank;
            }
        }

        return current;
    }
}