package controller;

import controller.events.EventBus;
import controller.events.HUDChangedEvent;
import controller.events.StarvationEvent;
import controller.events.UnitActionsChangedEvent;
import controller.services.DisasterService;
import controller.services.FogOfWarService;
import controller.services.SaveLoadService;
import controller.services.TradeService;
import controller.services.TribeService;
import controller.services.TurnProcessor;
import controller.services.WorldGenerator;
import model.*;
import view.Ground;

import javax.swing.*;
import javax.swing.Timer;
import java.util.*;

public class GameController {
    private final static int BUILD_COST = 2;
    private final static int FOOD_REQUIREMENT = 1;
    private static final int BASE_PRODUCTION_RATE = 2;

    private AnimationController animationController;
    private Ground ground;
    private Camera camera;
    private FogOfWarService fogOfWarService;
    private TurnProcessor turnProcessor;
    private final TradeService tradeService = new TradeService();
    private GlobalHappinessManager happinessManager = new GlobalHappinessManager();
    private final TribeService tribeService = new TribeService();
    private List<Tribe> tribes;
    private final DisasterService disasterService = new DisasterService();
    private final SaveLoadService saveLoadService = new SaveLoadService();

    final static int ROWS = 100, COLS = 100;

    private ArrayList<Tile> Tiles;
    private Tile[][] tileGrid;
    private ArrayList<Unit> units = new ArrayList<>();
    private ArrayList<Building> buildings = new ArrayList<>();
    private Map<HexEdge, EdgeFeature> edgeFeatures;

    private GlobalResourceManager economy;

    private Unit selectedUnit = null;
    private Tile tileUnderUnit = null;
    private EdgeFeature pendingEdgeBuild = null;
    private boolean pendingEdgeDeconstruct = false;

    private int currentTurn = 1;

    private Tile Townhall;

    private int TownhallX = 10, TownhallY = 10;

    private int unitCapacity = 9;
    private Map<UnitType, Integer> unitCount = new HashMap<>();

    private Map<TechType, Boolean> researchedTechs = new HashMap<>();

    public GameController(Ground ground) {
        this.ground = ground;
        camera = new Camera(ground);
        ground.addMouseMotionListener(camera);
        ground.addMouseListener(camera);
        ground.addMouseWheelListener(camera);

        ground.addMouseListener(new InputHandler(this));

        WorldGenerator.WorldData worldData = new WorldGenerator().generate(ROWS, COLS, TownhallX, TownhallY);
        this.Tiles = worldData.tiles;
        this.tileGrid = worldData.tileGrid;
        this.Townhall = worldData.townhall;
        this.edgeFeatures = worldData.edgeFeatures;
        this.buildings.add(worldData.townhallBuilding);
        this.buildings.addAll(worldData.neutralBuildings);
        this.tribes = worldData.tribes;
        for (Unit unit : worldData.initialUnits) {
            addUnit(unit);
        }
        markTilesOwned(Townhall);

        fogOfWarService = new FogOfWarService(ROWS, COLS, tileGrid, Tiles, units, buildings);
        updateFog();

        animationController = new AnimationController(this);
        economy = new GlobalResourceManager();
        applyTownHallStorage(Townhall.getBuilding().getTownHallLevel());
        turnProcessor = new TurnProcessor(this);

        Timer timer = new Timer(
                8,
                e -> frameGenerator()
        );
        timer.start();
    }

    public void advanceTurn(){
        turnProcessor.advanceTurn();
    }

    public int getA() {
        return camera.getA();
    }

    public int getB() {
        return camera.getB();
    }

    public void updateFog() {
        fogOfWarService.updateFog();
    }

    private void frameGenerator() {
        animationController.run();
        camera.run();
//        updateFog();
        ground.repaint();
    }

    public int getXOffset(){
        return camera.getXOffset();
    }

    public int getYOffset(){
        return camera.getYOffset();
    }

    public ArrayList<Tile> getTiles() {
        return Tiles;
    }

    public ArrayList<Unit> getUnits() {
        return units;
    }

    public ArrayList<Building> getBuildings() {
        return buildings;
    }

    public EdgeFeature getEdgeFeature(int col1, int row1, int col2, int row2) {
        return edgeFeatures.getOrDefault(new HexEdge(col1, row1, col2, row2), EdgeFeature.NONE);
    }

    public void startBuildingEdge(EdgeFeature feature) {
        pendingEdgeBuild = feature;
    }

    public EdgeFeature getPendingEdgeBuild() {
        return pendingEdgeBuild;
    }

    public boolean buildEdgeFeature(int col1, int row1, int col2, int row2, EdgeFeature feature) {
        int woodCost = feature == EdgeFeature.ROAD ? 10 : 0;
        int stoneCost = feature == EdgeFeature.WALL ? 30 : 0;
        int apCost = feature == EdgeFeature.ROAD ? 1 : 2;

        if (selectedUnit == null || selectedUnit.getCurrentAP() < apCost) return false;
        if (!(economy.hasEnough(ResourceType.WOOD, woodCost) && economy.hasEnough(ResourceType.STONE, stoneCost)))
            return false;

        economy.spendResource(ResourceType.WOOD, woodCost);
        economy.spendResource(ResourceType.STONE, stoneCost);
        selectedUnit.setCurrentAP(selectedUnit.getCurrentAP() - apCost);

        edgeFeatures.put(new HexEdge(col1, row1, col2, row2), feature);
        pendingEdgeBuild = null;
        EventBus.publish(new HUDChangedEvent());
        return true;
    }

    public void startDeconstructingEdge() {
        pendingEdgeDeconstruct = true;
    }

    public boolean isPendingEdgeDeconstruct() {
        return pendingEdgeDeconstruct;
    }

    public boolean deconstructEdge(int col1, int row1, int col2, int row2) {
        EdgeFeature feature = getEdgeFeature(col1, row1, col2, row2);
        if (feature != EdgeFeature.ROAD && feature != EdgeFeature.WALL) return false;
        if (selectedUnit == null || selectedUnit.getCurrentAP() < 1) return false;

        selectedUnit.setCurrentAP(selectedUnit.getCurrentAP() - 1);
        edgeFeatures.remove(new HexEdge(col1, row1, col2, row2));
        pendingEdgeDeconstruct = false;
        EventBus.publish(new HUDChangedEvent());
        return true;
    }

    public boolean deconstructBuilding() {
        if (selectedUnit == null || selectedUnit.getType() != UnitType.BUILDER) return false;
        if (tileUnderUnit == null || tileUnderUnit.getBuilding() == null) return false;
        if (tileUnderUnit.getBuilding().getType() == BuildingType.TOWN_HALL) return false;
        if (selectedUnit.getCurrentAP() < 1) return false;

        selectedUnit.setCurrentAP(selectedUnit.getCurrentAP() - 1);
        buildings.remove(tileUnderUnit.getBuilding());
        tileUnderUnit.setBuilding(null);
        EventBus.publish(new HUDChangedEvent());
        return true;
    }

    public Tile getTownhall() {
        return Townhall;
    }

    public void incrementTurn() {
        currentTurn++;
    }

    public void addUnit(Unit unit){
        units.add(unit);
        unitCount.put(unit.getType(), unitCount.getOrDefault(unit.getType(), 0) + 1);
        if (units.size() == unitCapacity) {
            happinessManager.addHappiness(-1);
        }
    }

    public void deleteUnit(Unit unit){
        units.remove(unit);
        unitCount.put(unit.getType(), unitCount.getOrDefault(unit.getType(), 0) - 1);
    }

    public Unit getSelectedUnit() {
        return selectedUnit;
    }

    public void setSelectedUnit(Unit unit) {
        selectedUnit = unit;
    }

    public Tile getTileUnderUnit() {
        return tileUnderUnit;
    }

    public void setTileUnderUnit(Tile tile) {
        tileUnderUnit = tile;
    }

    public GlobalResourceManager getEconomy() {
        return economy;
    }

    public GlobalHappinessManager getHappinessManager() {
        return happinessManager;
    }

    public List<Tribe> getTribes() {
        return tribes;
    }

    public boolean sendGiftToTribe(Tribe tribe, ResourceType resource, int amount) {
        if (!economy.hasEnough(resource, amount)) return false;

        economy.spendResource(resource, amount);
        tribeService.sendGift(tribe, amount);
        EventBus.publish(new HUDChangedEvent());
        return true;
    }

    public boolean captureTribeCamp(Tribe tribe) {
        Tile tile = tileGrid[tribe.getCol()][tribe.getRow()];
        Building camp = tile.getBuilding();
        if (camp == null || camp.getType() != BuildingType.TRIBE_CAMP) return false;
        if (!camp.isDestroyed()) return false;

        buildings.remove(camp);
        Building outpost = new Building(BuildingType.OUTPOST, tribe.getCol(), tribe.getRow());
        buildings.add(outpost);
        tile.setBuilding(outpost);

        economy.addResource(ResourceType.WOOD, 50);
        tribeService.recordWar(tribe);

        EventBus.publish(new HUDChangedEvent());
        return true;
    }

    public Season getCurrentSeason() {
        return Season.fromTurn(currentTurn);
    }

    public void issueRoadQuestToTribe(Tribe tribe) {
        tribeService.issueRoadQuest(tribe);
        EventBus.publish(new HUDChangedEvent());
    }

    public void checkTribeQuests() {
        for (Tribe tribe : tribes) {
            Quest quest = tribe.getActiveQuest();
            if (quest != null && !quest.isCompleted() && tribeService.isRoadQuestSatisfied(tribe, edgeFeatures)) {
                tribeService.completeQuest(tribe);
                EventBus.publish(new HUDChangedEvent());
            }
        }
    }

    public void removeDestroyedBuilding(Building building) {
        buildings.remove(building);
        Tile tile = tileGrid[building.getCol()][building.getRow()];
        if (tile.getBuilding() == building) {
            tile.setBuilding(null);
        }
    }

    public DisasterType rollForDisaster() {
        DisasterType disaster = disasterService.rollForDisaster(this);
        if (disaster != null) EventBus.publish(new HUDChangedEvent());
        return disaster;
    }

    public GameState captureState() {
        GameState state = new GameState();
        state.tiles = Tiles;
        state.tileGrid = tileGrid;
        state.units = units;
        state.buildings = buildings;
        state.edgeFeatures = edgeFeatures;
        state.economy = economy;
        state.happinessManager = happinessManager;
        state.tribes = tribes;
        state.researchedTechs = researchedTechs;
        state.unitCount = unitCount;
        state.currentTurn = currentTurn;
        state.unitCapacity = unitCapacity;
        return state;
    }

    public void restoreState(GameState state) {
        this.Tiles = state.tiles;
        this.tileGrid = state.tileGrid;
        this.units = state.units;
        this.buildings = state.buildings;
        this.edgeFeatures = state.edgeFeatures;
        this.economy = state.economy;
        this.happinessManager = state.happinessManager;
        this.tribes = state.tribes;
        this.researchedTechs = state.researchedTechs;
        this.unitCount = state.unitCount;
        this.currentTurn = state.currentTurn;
        this.unitCapacity = state.unitCapacity;

        this.Townhall = tileGrid[TownhallX][TownhallY];
        this.selectedUnit = null;
        this.tileUnderUnit = null;

        fogOfWarService = new FogOfWarService(ROWS, COLS, tileGrid, Tiles, units, buildings);
        updateFog();

        EventBus.publish(new HUDChangedEvent());
        EventBus.publish(new UnitActionsChangedEvent());
    }

    public boolean saveGame(int slot) {
        return saveLoadService.save(this, slot);
    }

    public boolean loadGame(int slot) {
        return saveLoadService.load(this, slot);
    }

    public void autosave() {
        saveLoadService.autosave(this);
    }

    public int getCurrentTurn() {
        return currentTurn;
    }

    public boolean constructBuilding(BuildingType bType) {
        if(selectedUnit.getCurrentAP() < bType.getApCost()) return false;
        selectedUnit.setCurrentAP(selectedUnit.getCurrentAP() - BUILD_COST);

        if(!(economy.hasEnough(ResourceType.WOOD, bType.getWoodCost()) &&
            economy.hasEnough(ResourceType.STONE, bType.getStoneCost()) &&
            economy.hasEnough(ResourceType.IRON, bType.getIronCost())))
            return false;

        economy.spendResource(ResourceType.WOOD, bType.getWoodCost());
        economy.spendResource(ResourceType.STONE, bType.getStoneCost());
        economy.spendResource(ResourceType.IRON, bType.getIronCost());

        unitCapacity += bType.getUnitCapacityBonus();

        selectedUnit.useCharge();
        if(selectedUnit.getCharge() == 0){
            deleteUnit(selectedUnit);
            selectedUnit = null;
        }

        Building building = new Building(bType, tileUnderUnit.getCol(), tileUnderUnit.getRow());
        buildings.add(building);

        tileUnderUnit.setBuilding(building);
        EventBus.publish(new HUDChangedEvent());
        return true;
    }

    public void assignWorkerToBuilding() {
        tileUnderUnit.getBuilding().addWorker(selectedUnit);
        selectedUnit.setAssigned(true);

        selectedUnit = null;
        tileUnderUnit = null;
        EventBus.publish(new HUDChangedEvent());
    }

    public void removeWorker() {
        Building building = tileUnderUnit.getBuilding();
        Unit worker = building.getLastWorker();
        building.removeWorker(worker);
        worker.setAssigned(false);
    }

    public boolean hasEnoughFood(int foodCost) {
        return(economy.hasEnoughFood(foodCost));
    }
    public boolean hasEnoughWood(int woodCost) {
        return(economy.hasEnough(ResourceType.WOOD, woodCost));
    }
    public boolean hasEnoughStone(int stoneCost) {
        return(economy.hasEnough(ResourceType.STONE, stoneCost));
    }
    public boolean hasEnoughIron(int ironCost) {
        return(economy.hasEnough(ResourceType.IRON, ironCost));
    }

    public void startProducingUnitInTownHall(UnitType uType) {
        if(Townhall.getBuilding().isProducing()){
            System.out.println("Townhall is busy :((");
            return;
        }
        int cost = uType.getFoodCost();
        boolean isPaid = economy.spendFood(cost);
        EventBus.publish(new HUDChangedEvent());

        if(isPaid){
            Townhall.getBuilding().startProducing(uType);
            System.out.println("Producing Started :)))");
        }
    }

    public boolean checkUnitCap(){
        return (units.size() < unitCapacity);
    }

    public boolean canStackAt(int col, int row, UnitType type) {
        if (type != UnitType.SWORDSMAN && type != UnitType.ARCHER && type != UnitType.CAVALRY) return true;

        int cap = (type == UnitType.CAVALRY) ? 1 : 2;
        int count = 0;
        for (Unit u : units) {
            if (u.getCol() == col && u.getRow() == row && u.getType() == type) count++;
        }
        return count < cap;
    }

    public int getMilitaryUnitCount() {
        int count = 0;
        for (Unit u : units) {
            if (u.getType() == UnitType.SWORDSMAN || u.getType() == UnitType.ARCHER || u.getType() == UnitType.CAVALRY) {
                count++;
            }
        }
        return count;
    }

    public boolean checkMilitaryUnitCap() {
        return getMilitaryUnitCount() < getTownHallLevel().getMilitaryUnitCap();
    }

    public boolean isMilitaryUnit(UnitType type) {
        return type == UnitType.SWORDSMAN || type == UnitType.ARCHER || type == UnitType.CAVALRY;
    }

    public boolean hasStable() {
        for (Building b : buildings) {
            if (b.getType() == BuildingType.STABLE) return true;
        }
        return false;
    }

    public boolean hasBuildingType(BuildingType type) {
        for (Building b : buildings) {
            if (b.getType() == type) return true;
        }
        return false;
    }

    public boolean canUseBazaar() {
        return tradeService.canUseBazaar();
    }

    public boolean canUseTradingPost() {
        return tradeService.canUseTradingPost();
    }

    public double bazaarRateForTier(int amount) {
        return tradeService.bazaarRateForTier(amount);
    }

    public boolean tradeAtBazaar(ResourceType from, ResourceType to, int tierAmount) {
        boolean success = tradeService.tradeAtBazaar(economy, from, to, tierAmount);
        if (success) EventBus.publish(new HUDChangedEvent());
        return success;
    }

    public boolean tradeAtTradingPost(ResourceType from, ResourceType to, int amount) {
        boolean success = tradeService.tradeAtTradingPost(economy, from, to, amount);
        if (success) EventBus.publish(new HUDChangedEvent());
        return success;
    }

    public void resetTradeTurn() {
        tradeService.resetTurn();
    }


    private void markTilesOwned(Tile center) {
        center.setOwned();
        for (Tile tile: Tiles) {
            if (HexUtils.isNeighbor(tile.getCol(), tile.getRow(), center.getCol(), center.getRow())) {
                tile.setOwned();
            }
        }
    }

    public void expandTerritory() {
        markTilesOwned(tileUnderUnit);
        happinessManager.addHappiness(-1);
        if(selectedUnit == null) return;
        deleteUnit(selectedUnit);
        selectedUnit = null;
    }

    public TownHallLevel getTownHallLevel() {
        return Townhall.getBuilding().getTownHallLevel();
    }

    public void applyTownHallStorage(TownHallLevel level) {
        economy.updateStorage(level.getCattleCapacity(), level.getWheatCapacity(), level.getWoodCapacity(),
                level.getStoneCapacity(), level.getIronCapacity(), level.getFishCapacity());
    }

    public boolean upgradeTownHall() {
        Building townHallBuilding = Townhall.getBuilding();
        if (townHallBuilding.isProducing()) return false;

        TownHallLevel nextLevel = townHallBuilding.getTownHallLevel().getNextLevel();
        if (nextLevel == null) return false;

        if(!(economy.hasEnough(ResourceType.WOOD, nextLevel.getUpgradeWoodCost()) &&
            economy.hasEnough(ResourceType.STONE, nextLevel.getUpgradeStoneCost()) &&
            economy.hasEnough(ResourceType.IRON, nextLevel.getUpgradeIronCost())))
            return false;

        economy.spendResource(ResourceType.WOOD, nextLevel.getUpgradeWoodCost());
        economy.spendResource(ResourceType.STONE, nextLevel.getUpgradeStoneCost());
        economy.spendResource(ResourceType.IRON, nextLevel.getUpgradeIronCost());

        townHallBuilding.startUpgrading(nextLevel);
        EventBus.publish(new HUDChangedEvent());
        return true;
    }

    // "is" ha ro "has" kardam ke tamiz tar beshe yeho nagid ai e :((

    public boolean hasStoneTech() {
        return hasTech(TechType.STONE_MINING);
    }

    public boolean hasIronTech() {
        return hasTech(TechType.IRON_MINING);
    }

    public boolean hasSettlementTech() {
        return hasTech(TechType.SETTLEMENT_TECH);
    }

    public boolean hasProToolsTech() {
        return hasTech(TechType.PRO_TOOLS);
    }

    public boolean hasTech(TechType tech) {
        return researchedTechs.getOrDefault(tech, false);
    }

    public boolean hasEnoughResource(ResourceType type, int amount) {
        return economy.hasEnough(type, amount);
    }

    public boolean researchTech(TechType tech) {
        if (hasTech(tech)) return false;
        if (getTownHallLevel().getLevelNumber() < tech.getRequiredLevel().getLevelNumber()) return false;
        if (!economy.hasEnough(tech.getCostResource(), tech.getCostAmount())) return false;

        if (tech.isInstant()) {
            if (tech.getCostAmount() > 0) economy.spendResource(tech.getCostResource(), tech.getCostAmount());
            completeTechResearch(tech);
            return true;
        }

        Building townHallBuilding = Townhall.getBuilding();
        if (townHallBuilding.isProducing()) return false;

        if (tech.getCostAmount() > 0) economy.spendResource(tech.getCostResource(), tech.getCostAmount());
        townHallBuilding.startResearching(tech);
        EventBus.publish(new HUDChangedEvent());
        return true;
    }

    public void completeTechResearch(TechType tech) {
        researchedTechs.put(tech, true);

        if (tech == TechType.DEFENSIVE_ARCHITECTURE) {
            Building townHallBuilding = Townhall.getBuilding();
            int hpGain = 350 - townHallBuilding.getMaxHP();
            townHallBuilding.setMaxHP(350);
            if (hpGain > 0) townHallBuilding.heal(hpGain);
        }

        EventBus.publish(new HUDChangedEvent());
    }


    public void updateNetChanges(){
        economy.resetNetChanges();

        economy.addNetChanges(ResourceType.WHEAT, 1);
        economy.addNetChanges(ResourceType.WOOD, 1);

        economy.addNetChanges(ResourceType.WHEAT, -1 * FOOD_REQUIREMENT * units.size());


        int wood = 0, stone = 0, iron = 0;
        for(Building building: buildings){
            wood -= building.getType().getWoodCost() / 10;
            stone -= building.getType().getStoneCost() / 10;
            iron -= building.getType().getIronCost() / 10;

            ResourceType source = building.getType().getOutputResource();
            if(source != null && source != ResourceType.NONE){
                economy.addNetChanges(source, (int)((hasProToolsTech() ? 1.5 : 1) *
                        BASE_PRODUCTION_RATE) * building.getStationedWorkers().size());
            }
        }
        economy.addNetChanges(ResourceType.WOOD, wood);
        economy.addNetChanges(ResourceType.STONE, stone);
        economy.addNetChanges(ResourceType.IRON, iron);

    }

    public int getUnitCounts(){
        return units.size();
    }

    public int getUnitCounts(UnitType type){
        return unitCount.getOrDefault(type, 0);
    }

    public int getUnitCapacity(){
        return unitCapacity;
    }

    public boolean hasUnitsWithRemainingAP() {
        for(Unit unit: units){
            if(unit.getCurrentAP() > 0) return true;
        }
        return false;
    }
}
