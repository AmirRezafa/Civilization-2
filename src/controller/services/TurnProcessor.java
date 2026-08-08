package controller.services;

import controller.GameController;
import controller.events.EventBus;
import controller.events.StarvationEvent;
import model.*;

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

        int ratePerWorker = (int)((gc.hasProToolsTech() ? 1.5 : 1) * BASE_PRODUCTION_RATE);
        type.produceResources(building, tile, economy, ratePerWorker);
    }

    public void advanceTurn(){
        gc.incrementTurn();
        for(Tile tile: gc.getTiles()){
            Building building = tile.getBuilding();
            if(building != null){
                processTurnProduction(tile, gc.getEconomy());
            }
        }
        boolean isStarvation = false;
        for(Unit unit: gc.getUnits()){
            boolean hasFed = gc.getEconomy().spendFood(FOOD_REQUIREMENT);
            if(!hasFed){
                isStarvation = true;
            };
            unit.resetActionPoints(!hasFed || unit.isAssigned());
        }
        if(isStarvation) EventBus.publish(new StarvationEvent());

        Tile townhall = gc.getTownhall();
        if(townhall.getBuilding().isProducing()){
            townhall.getBuilding().decrementProductionTurns();
            if (townhall.getBuilding().getProductionTurnsLeft() <= 0) {
                Unit newUnit = new Unit(townhall.getBuilding().getProducingUnit(), townhall.getCol(), townhall.getRow());
                gc.addUnit(newUnit);

                townhall.getBuilding().clearProduction();
            }
        }
    }
}
