package net.mazebound.exploration;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public class Cell {

    private final int x;
    private final int z;

    private final Set<CellDirection> openings =
            EnumSet.noneOf(CellDirection.class);

    public Cell(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public int x() {
        return x;
    }

    public int z() {
        return z;
    }

    public String getId() {
        return x + "," + z;
    }

    public void addOpening(CellDirection direction) {
        openings.add(direction);
    }

    public boolean hasOpening(CellDirection direction) {
        return openings.contains(direction);
    }

    public Set<CellDirection> getOpenings() {
        return Set.copyOf(openings);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof Cell other)) {
            return false;
        }

        return x == other.x && z == other.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }
}