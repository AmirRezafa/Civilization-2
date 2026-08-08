package model;

public enum TerrainType {
    PLAIN("Plain", 1),
    FOREST("Forest", 2),
    MOUNTAIN("Mountain", 4),
    MEADOW("Meadow", 1);

    private final String displayName;
    private final int movementCost;

    TerrainType(String displayName, int movementCost) {
        this.displayName = displayName;
        this.movementCost = movementCost;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMovementCost() {
        return movementCost;
    }
}