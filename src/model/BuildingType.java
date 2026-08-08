package model;

import java.util.ArrayList;

public enum BuildingType {
    LUMBER_MILL("Lumber Mill", TerrainType.FOREST, ResourceType.WOOD,
            0, 0, 0, 2, 1, 0, true),
    STONE_MINE("Stone Mine", TerrainType.MOUNTAIN, ResourceType.STONE,
            15, 0, 0, 3, 2, 0, true) {
        @Override
        public boolean isUnlocked(boolean stoneTech, boolean ironTech, boolean settlementTech) {
            return stoneTech;
        }
    },
    IRON_MINE("Iron Mine", TerrainType.MOUNTAIN, ResourceType.IRON,
            25, 0, 0, 3, 2, 0, true) {
        @Override
        public boolean isUnlocked(boolean stoneTech, boolean ironTech, boolean settlementTech) {
            return ironTech;
        }
    },
    FARM("Farm", TerrainType.MEADOW, ResourceType.WHEAT,
            0, 0, 0, 2, 2, 0, true),
    STABLE("Stable", TerrainType.PLAIN, ResourceType.CATTLE,
            20, 0, 0, 2, 3, 0, true),
    TOWN_HALL("Town Hall", null, ResourceType.NONE,
            0, 0, 0, 0, 0, 3, false) {
        @Override
        public void produceResources(Building building, Tile tile, GlobalResourceManager economy, int ratePerWorker) {
            economy.addResource(ResourceType.WHEAT, 1);
            economy.addResource(ResourceType.WOOD, 1);
        }
    },
    SETTLEMENT("Settlement", null, ResourceType.NONE,
            25, 15, 10, 0, 2, 2, true) {
        @Override
        public boolean isUnlocked(boolean stoneTech, boolean ironTech, boolean settlementTech) {
            return settlementTech;
        }

        @Override
        public int getUnitCapacityBonus() {
            return 3;
        }

        @Override
        public void produceResources(Building building, Tile tile, GlobalResourceManager economy, int ratePerWorker) {
            // Settlements never produce resource output, even when occupied
        }
    };

    private final String displayName;
    private final TerrainType requiredTerrain;
    private final ResourceType outputResource;

    private final int woodCost;
    private final int stoneCost;
    private final int ironCost;
    private final int apCost;

    private final int maxWorkerCapacity;

    private final int visionRadius;

    private final boolean isPlayerBuildable;

    BuildingType(String displayName, TerrainType requiredTerrain, ResourceType outputResource,
                 int woodCost, int stoneCost, int ironCost, int maxWorkerCapacity, int apCost, int visionRadius,
                 boolean isPlayerBuildable) {
        this.displayName = displayName;
        this.requiredTerrain = requiredTerrain;
        this.outputResource = outputResource;
        this.woodCost = woodCost;
        this.stoneCost = stoneCost;
        this.ironCost = ironCost;
        this.maxWorkerCapacity = maxWorkerCapacity;
        this.apCost = apCost;
        this.visionRadius = visionRadius;
        this.isPlayerBuildable = isPlayerBuildable;
    }

    public String getDisplayName() {
        return displayName;
    }

    public TerrainType getRequiredTerrain() {
        return requiredTerrain;
    }

    public ResourceType getOutputResource() {
        return outputResource;
    }

    public int getWoodCost() {
        return woodCost;
    }

    public int getStoneCost() {
        return stoneCost;
    }

    public int getIronCost() {
        return ironCost;
    }

    public int getMaxWorkerCapacity() {
        return maxWorkerCapacity;
    }

    public int getApCost() {
        return apCost;
    }

    public String getCostString() {
        ArrayList<String> costs = new ArrayList<>();

        if (woodCost > 0) costs.add(woodCost + " Wood");
        if (stoneCost > 0) costs.add(stoneCost + " Stone");
        if (ironCost > 0) costs.add(ironCost + " Iron");

        if (costs.isEmpty()) return "Free";

        return String.join(", ", costs);
    }

    public int getVisionRadius() {
        return visionRadius;
    }

    public boolean isPlayerBuildable() {
        return isPlayerBuildable;
    }

    public boolean isBuildableOnTerrain(TerrainType terrain) {
        return requiredTerrain == null || requiredTerrain == terrain;
    }

    public boolean isUnlocked(boolean stoneTech, boolean ironTech, boolean settlementTech) {
        return true;
    }

    public int getUnitCapacityBonus() {
        return 0;
    }

    public void produceResources(Building building, Tile tile, GlobalResourceManager economy, int ratePerWorker) {
        if (!building.isOccupied()) return;

        ResourceType targetResource = getOutputResource();
        if (targetResource == null || targetResource == ResourceType.NONE) return;
        if (!tile.hasResource(targetResource)) return;

        economy.addResource(targetResource,
                tile.extractResource(targetResource, ratePerWorker * building.getStationedWorkers().size()));
    }
}
