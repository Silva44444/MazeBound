package net.mazebound.data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private int xp;
    private final Set<String> discoveredCells = new HashSet<>();

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
}