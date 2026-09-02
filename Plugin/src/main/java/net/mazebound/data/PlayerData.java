package net.mazebound.data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private int xp;

    private final Set<String> discoveredCells =
            new HashSet<>();

    private final Set<Integer> unlockedMilestones =
            new HashSet<>();

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.xp = 0;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getXp() {
        return xp;
    }

    public void addXp(int amount) {
        xp += amount;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }

    public Set<String> getDiscoveredCells() {
        return discoveredCells;
    }

    public boolean discoverCell(String cellId) {
        return discoveredCells.add(cellId);
    }

    public int getCellsDiscovered() {
        return discoveredCells.size();
    }

    public Set<Integer> getUnlockedMilestones() {
        return unlockedMilestones;
    }

    public boolean unlockMilestone(int milestone) {
        return unlockedMilestones.add(milestone);
    }

    public boolean hasMilestone(int milestone) {
        return unlockedMilestones.contains(milestone);
    }
    public MazeboundRank getRank() {

    MazeboundRank currentRank =
            MazeboundRank.WANDERER;

    for (MazeboundRank rank : MazeboundRank.values()) {

        if (getCellsDiscovered() >= rank.getRequiredCells()) {
            currentRank = rank;
        }
    }

    return currentRank;
}
}