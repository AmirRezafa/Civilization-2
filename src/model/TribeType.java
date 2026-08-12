package model;

public enum TribeType {
    FARMER("Farmer", ResourceType.WHEAT, 0.75, true),
    MOUNTAIN("Mountain", ResourceType.STONE, 0.75, true),
    TRADER("Trader", ResourceType.STONE, 0.80, true),
    COASTAL("Coastal", ResourceType.WHEAT, 0.75, true),
    WARRIOR("Warrior", null, 0.0, false);

    private final String displayName;
    private final ResourceType tradeRewardResource;
    private final double tradeRate;
    private final boolean canTrade;

    TribeType(String displayName, ResourceType tradeRewardResource, double tradeRate, boolean canTrade) {
        this.displayName = displayName;
        this.tradeRewardResource = tradeRewardResource;
        this.tradeRate = tradeRate;
        this.canTrade = canTrade;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ResourceType getTradeRewardResource() {
        return tradeRewardResource;
    }

    public double getTradeRate() {
        return tradeRate;
    }

    public boolean canTrade() {
        return canTrade;
    }
}
