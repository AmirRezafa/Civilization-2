package model;

public enum UnitType {
    EXPLORER("Explorer", 6, 3, 20, 1, 3),
    BUILDER("Builder", 4, 2, 20, 3, 1),
    WORKER("Worker", 4, 2, 10, 1, 0),
    BORDER_EXPANDER("Border Expander", 4, 4, 15, 1, 1);

    private final String displayName;
    private final int maxAP;
    private final int buildTurns;
    private final int foodCost;
    private final int chargesCount;
    private final int visionRadius;

    UnitType(String displayName, int maxAP, int buildTurns, int foodCost, int chargesCount, int visionRadius) {
        this.displayName = displayName;
        this.maxAP = maxAP;
        this.buildTurns = buildTurns;
        this.foodCost = foodCost;
        this.chargesCount = chargesCount;
        this.visionRadius = visionRadius;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxAP() {
        return maxAP;
    }

    public int getChargesCount() {
        return chargesCount;
    }

    public int getBuildTurns() {
        return buildTurns;
    }

    public int getFoodCost() {
        return foodCost;
    }

    public int getVisionRadius() {
        return visionRadius;
    }
}