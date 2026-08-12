package controller.services;

import controller.GameController;
import controller.events.EventBus;
import controller.events.StarvationEvent;
import model.*;

import java.util.List;

public class TurnProcessor {
    private static final int FOOD_REQUIREMENT = 1;
    private static final int BASE_PRODUCTION_RATE = 2;

    private final GameController gc;

    public TurnProcessor(GameController gc) {
        this.gc = gc;
    }

    public void processTurnProduction(Tile tile, GlobalResourceManager economy) {
        Building building = tile.getBuilding();
        BuildingType type = building.getType();

        boolean spended = true;
        if(!economy.spendResource(ResourceType.WOOD, type.getWoodCost() / 10)) spended = false;
        if(!economy.spendResource(ResourceType.STONE, type.getStoneCost() / 10)) spended = false;
        if(!economy.spendResource(ResourceType.IRON, type.getIronCost() / 10)) spended = false;

        if(!spended) building.upkeepFailed();
        if(building.getFailedCount() == 3){
            gc.getBuildings().remove(building);
            return;
        }

        GlobalHappinessManager happiness = gc.getHappinessManager();

        double multiplier = gc.hasProToolsTech() ? 1.5 : 1.0;
        if (happiness.isGoldenAge()) multiplier *= 1.10;

        int ratePerWorker = (int)(multiplier * BASE_PRODUCTION_RATE);
        if (happiness.isDissatisfied() || happiness.isRiot()) {
            ratePerWorker = Math.max(0, ratePerWorker - 1);
        }

        if ((type == BuildingType.STONE_MINE || type == BuildingType.IRON_MINE) && gc.hasTech(TechType.STEEL_TOOLS)) {
            ratePerWorker = (int)(ratePerWorker * 1.5);
        }

        type.produceResources(building, tile, economy, ratePerWorker, gc.getTiles());
        applyAdjacencyBonus(building, tile, economy, gc.getTiles());
        applySeasonalBonus(type, economy);
    }

    private void applySeasonalBonus(BuildingType type, GlobalResourceManager economy) {
        Season season = gc.getCurrentSeason();

        if (type == BuildingType.FARM) {
            int bonus = season.getFarmFoodBonus();
            if (bonus > 0) economy.addResource(ResourceType.WHEAT, bonus);
            else if (bonus < 0) economy.spendResource(ResourceType.WHEAT, -bonus);
        } else if (type == BuildingType.STABLE && season.getFarmFoodBonus() > 0) {
            economy.addResource(ResourceType.CATTLE, season.getFarmFoodBonus());
        }
    }

    private void applyAdjacencyBonus(Building building, Tile tile, GlobalResourceManager economy, List<Tile> allTiles) {
        BuildingType type = building.getType();

        if (type == BuildingType.FARM) {
            for (Tile other : allTiles) {
                if (other.getBuilding() != null && other.getBuilding().getType() == BuildingType.FARM &&
                        HexUtils.isNeighbor(tile.getCol(), tile.getRow(), other.getCol(), other.getRow())) {
                    economy.addResource(ResourceType.WHEAT, 1);
                    break;
                }
            }
        } else if (type == BuildingType.LUMBER_MILL) {
            for (Tile other : allTiles) {
                if (other.getTerrain() == TerrainType.SEA &&
                        HexUtils.isNeighbor(tile.getCol(), tile.getRow(), other.getCol(), other.getRow())) {
                    economy.addResource(ResourceType.WOOD, 2);
                    break;
                }
            }
        } else if (type == BuildingType.STONE_MINE || type == BuildingType.IRON_MINE) {
            int mountainCount = 0;
            for (Tile other : allTiles) {
                if (other.getTerrain() == TerrainType.MOUNTAIN &&
                        HexUtils.isNeighbor(tile.getCol(), tile.getRow(), other.getCol(), other.getRow())) {
                    mountainCount++;
                }
            }
            if (mountainCount >= 2) {
                economy.addResource(type.getOutputResource(), 1);
            }
        }
    }

    public void advanceTurn(){
        gc.incrementTurn();
        gc.resetTradeTurn();

        DisasterType disaster = gc.rollForDisaster();
        if (disaster != null) {
            System.out.println("A disaster struck: " + disaster);
        }
        for(Tile tile: gc.getTiles()){
            Building building = tile.getBuilding();
            if(building != null){
                processTurnProduction(tile, gc.getEconomy());
            }
        }
        boolean isStarvation = false;
        boolean isRiot = gc.getHappinessManager().isRiot();
        for(Unit unit: gc.getUnits()){
            boolean hasFed = gc.getEconomy().spendFood(FOOD_REQUIREMENT);
            if(!hasFed){
                isStarvation = true;
            };
            unit.resetActionPoints(!hasFed || unit.isAssigned());
            if (isRiot) {
                unit.setCurrentAP(Math.max(0, unit.getCurrentAP() - 1));
            }
        }
        if(isStarvation) EventBus.publish(new StarvationEvent());

        Tile townhall = gc.getTownhall();
        Building townHallBuilding = townhall.getBuilding();
        if(townHallBuilding.isProducing()){
            townHallBuilding.decrementProductionTurns();
            if (townHallBuilding.getProductionTurnsLeft() <= 0) {
                if (townHallBuilding.getProducingUnit() != null) {
                    Unit newUnit = new Unit(townHallBuilding.getProducingUnit(), townhall.getCol(), townhall.getRow());
                    gc.addUnit(newUnit);
                } else if (townHallBuilding.getUpgradingToLevel() != null) {
                    townHallBuilding.applyLevelUpgrade();
                    gc.applyTownHallStorage(townHallBuilding.getTownHallLevel());
                } else if (townHallBuilding.getResearchingTech() != null) {
                    gc.completeTechResearch(townHallBuilding.getResearchingTech());
                }

                townHallBuilding.clearProduction();
            }
        }

        gc.checkTribeQuests();
        gc.autosave();
    }
}
