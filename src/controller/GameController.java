package controller;

import controller.events.EventBus;
import controller.events.HUDChangedEvent;
import controller.events.StarvationEvent;
import controller.events.UnitActionsChangedEvent;
import controller.services.CombatService;
import controller.services.DisasterService;
import controller.services.FogOfWarService;
import controller.services.PersistedServices;
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
    private TradeService tradeService = new TradeService();
    private GlobalHappinessManager happinessManager = new GlobalHappinessManager();
    private TribeService tribeService = new TribeService();
    private List<Tribe> tribes;
    private DisasterService disasterService = new DisasterService();
    private final SaveLoadService saveLoadService = new SaveLoadService();

    final static int ROWS = 100, COLS = 100;

    private ArrayList<Tile> Tiles;
    private Tile[][] tileGrid;
    private ArrayList<Unit> units = new ArrayList<>();
    private ArrayList<Building> buildings = new ArrayList<>();
    private Map<HexEdge, EdgeFeature> edgeFeatures;
    private Map<HexEdge, Integer> wallHP = new HashMap<>();
    private Map<HexEdge, Integer> wallFailedUpkeep = new HashMap<>();
    private static final int WALL_MAX_HP = 100;

    private GlobalResourceManager economy;

    private Unit selectedUnit = null;
    private Tile tileUnderUnit = null;
    private EdgeFeature pendingEdgeBuild = null;
    private boolean pendingEdgeDeconstruct = false;
    private boolean pendingAttack = false;
    private CombatService combatService = new CombatService();

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

        HexEdge edge = new HexEdge(col1, row1, col2, row2);
        edgeFeatures.put(edge, feature);
        if (feature == EdgeFeature.WALL) wallHP.put(edge, WALL_MAX_HP);
        pendingEdgeBuild = null;
        EventBus.publish(new HUDChangedEvent());
        return true;
    }

    public int getWallHP(int col1, int row1, int col2, int row2) {
        return wallHP.getOrDefault(new HexEdge(col1, row1, col2, row2), 0);
    }

    public void destroyRoadsTouching(int col, int row) {
        List<HexEdge> toRemove = new ArrayList<>();
        for (Map.Entry<HexEdge, EdgeFeature> entry : edgeFeatures.entrySet()) {
            if (entry.getValue() != EdgeFeature.ROAD) continue;
            HexEdge edge = entry.getKey();
            if (touchesHex(edge, col, row)) toRemove.add(edge);
        }
        for (HexEdge edge : toRemove) edgeFeatures.remove(edge);
    }

    public boolean hasRiverEdgeTouching(int col, int row) {
        for (Map.Entry<HexEdge, EdgeFeature> entry : edgeFeatures.entrySet()) {
            if (entry.getValue() != EdgeFeature.RIVER) continue;
            if (touchesHex(entry.getKey(), col, row)) return true;
        }
        return false;
    }

    private boolean touchesHex(HexEdge edge, int col, int row) {
        return (edge.getCol1() == col && edge.getRow1() == row) ||
                (edge.getCol2() == col && edge.getRow2() == row);
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
        HexEdge edge = new HexEdge(col1, row1, col2, row2);
        edgeFeatures.remove(edge);
        wallHP.remove(edge);
        wallFailedUpkeep.remove(edge);
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

    public void startAttacking() {
        pendingAttack = true;
    }

    public boolean isPendingAttack() {
        return pendingAttack;
    }

    public boolean attackAdjacentStructure(int col, int row) {
        if (selectedUnit == null || !isMilitaryUnit(selectedUnit.getType())) return false;
        if (selectedUnit.getCurrentAP() < 1) return false;

        HexEdge edge = new HexEdge(selectedUnit.getCol(), selectedUnit.getRow(), col, row);
        if (edgeFeatures.getOrDefault(edge, EdgeFeature.NONE) == EdgeFeature.WALL) {
            int damage = combatService.calculateStructureDamage(List.of(selectedUnit));
            int remaining = wallHP.getOrDefault(edge, WALL_MAX_HP) - damage;
            if (remaining <= 0) {
                edgeFeatures.remove(edge);
                wallHP.remove(edge);
                wallFailedUpkeep.remove(edge);
            } else {
                wallHP.put(edge, remaining);
            }
            selectedUnit.setCurrentAP(selectedUnit.getCurrentAP() - 1);
            pendingAttack = false;
            EventBus.publish(new HUDChangedEvent());
            return true;
        }

        Tribe defendingTribe = getTribeAt(col, row);
        if (defendingTribe != null) {
            tribeService.recordAttack(defendingTribe, happinessManager);
        }
        if (defendingTribe != null && defendingTribe.getGuardUnitCount() > 0) {
            List<Unit> defenders = new ArrayList<>();
            for (int i = 0; i < defendingTribe.getGuardUnitCount(); i++) {
                defenders.add(new Unit(UnitType.SWORDSMAN, col, row));
            }
            combatService.resolveCombat(List.of(selectedUnit), defenders, false);

            int survivors = 0;
            for (Unit d : defenders) if (!d.isDead()) survivors++;
            int defeatedCount = defenders.size() - survivors;
            defendingTribe.setGuardUnitCount(survivors);
            recordDefeatsForNearbyQuests(col, row, defeatedCount);

            selectedUnit.setCurrentAP(selectedUnit.getCurrentAP() - 1);
            pendingAttack = false;

            if (selectedUnit.isDead()) {
                deleteUnit(selectedUnit);
                selectedUnit = null;
            }

            EventBus.publish(new HUDChangedEvent());
            return true;
        }

        Tile targetTile = tileGrid[col][row];
        Building target = targetTile.getBuilding();
        if (target == null || target.getType() == BuildingType.TOWN_HALL) return false;

        combatService.attackStructure(List.of(selectedUnit), target);
        selectedUnit.setCurrentAP(selectedUnit.getCurrentAP() - 1);
        pendingAttack = false;

        EventBus.publish(new HUDChangedEvent());
        return true;
    }

    private void recordDefeatsForNearbyQuests(int col, int row, int defeatedCount) {
        if (defeatedCount <= 0) return;

        for (Tribe tribe : tribes) {
            Quest quest = tribe.getActiveQuest();
            if (quest == null || quest.isCompleted() || quest.getType() != QuestType.WARRIOR_DEFEAT) continue;
            if (!tribeService.hexDistanceWithin(tribe.getCol(), tribe.getRow(), col, row, Tiles, 5)) continue;

            for (int i = 0; i < defeatedCount; i++) quest.recordDefeat();
        }
    }

    private Tribe getTribeAt(int col, int row) {
        for (Tribe t : tribes) {
            if (t.getCol() == col && t.getRow() == row) return t;
        }
        return null;
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
        tribeService.sendGift(tribe, resource, amount);
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

        tribeService.awardCaptureLoot(tribe, economy);
        tribeService.recordWar(tribe);

        EventBus.publish(new HUDChangedEvent());
        return true;
    }

    public boolean canDeclareWarOnTribe(Tribe tribe) {
        return tribe.getRelationship() != TribeRelationship.ENEMY;
    }

    public void declareWarOnTribe(Tribe tribe) {
        tribeService.recordAttack(tribe, happinessManager);
        EventBus.publish(new HUDChangedEvent());
    }

    public boolean canRequestPeaceWithTribe(Tribe tribe) {
        return tribeService.canRequestPeace(tribe);
    }

    public boolean requestPeaceWithTribe(Tribe tribe) {
        boolean success = tribeService.requestPeace(tribe, economy);
        if (success) EventBus.publish(new HUDChangedEvent());
        return success;
    }

    public boolean canRequestAllianceWithTribe(Tribe tribe) {
        return tribeService.canRequestAlliance(tribe);
    }

    public boolean requestAllianceWithTribe(Tribe tribe) {
        boolean success = tribeService.requestAlliance(tribe);
        if (success) EventBus.publish(new HUDChangedEvent());
        return success;
    }

    public Season getCurrentSeason() {
        return Season.fromTurn(currentTurn);
    }

    public boolean canOfferQuestToTribe(Tribe tribe) {
        return tribeService.canOfferQuest(tribe, currentTurn);
    }

    public void issueQuestToTribe(Tribe tribe) {
        tribeService.issueQuest(tribe, currentTurn);
        EventBus.publish(new HUDChangedEvent());
    }

    public void checkTribeQuests() {
        for (Tribe tribe : tribes) {
            Quest quest = tribe.getActiveQuest();
            if (quest == null || quest.isCompleted()) continue;

            if (tribeService.isQuestSatisfied(tribe, quest, economy, edgeFeatures, buildings, Tiles)) {
                tribeService.completeQuest(tribe, quest, economy);
                EventBus.publish(new HUDChangedEvent());
            } else if (quest.isExpired(currentTurn)) {
                tribeService.expireQuest(tribe, currentTurn);
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
        state.savedAtMillis = System.currentTimeMillis();
        state.tiles = Tiles;
        state.tileGrid = tileGrid;
        state.units = units;
        state.buildings = buildings;
        state.edgeFeatures = edgeFeatures;
        state.wallHP = wallHP;
        state.economy = economy;
        state.happinessManager = happinessManager;
        state.tribes = tribes;
        state.researchedTechs = researchedTechs;
        state.unitCount = unitCount;
        state.currentTurn = currentTurn;
        state.unitCapacity = unitCapacity;
        return state;
    }

    public PersistedServices captureServices() {
        PersistedServices services = new PersistedServices();
        services.combatService = combatService;
        services.disasterService = disasterService;
        services.tradeService = tradeService;
        services.tribeService = tribeService;
        return services;
    }

    public void restoreServices(PersistedServices services) {
        if (services == null) return;
        this.combatService = services.combatService;
        this.disasterService = services.disasterService;
        this.tradeService = services.tradeService;
        this.tribeService = services.tribeService;
    }

    public void restoreState(GameState state) {
        this.Tiles = state.tiles;
        this.tileGrid = state.tileGrid;
        this.units = state.units;
        this.buildings = state.buildings;
        this.edgeFeatures = state.edgeFeatures;
        this.wallHP = state.wallHP != null ? state.wallHP : new HashMap<>();
        this.wallFailedUpkeep = new HashMap<>();
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

    public boolean isSaveAllowed() {
        return !pendingAttack && pendingEdgeBuild == null && !pendingEdgeDeconstruct;
    }

    public boolean saveGame(int slot) {
        if (!isSaveAllowed()) return false;
        return saveLoadService.save(this, slot);
    }

    public boolean loadGame(int slot) {
        return saveLoadService.load(this, slot);
    }

    public void autosave() {
        saveLoadService.autosave(this);
    }

    public String peekSaveSummary(int slot) {
        return saveLoadService.peekSummary(slot);
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

    public void cancelTownHallProduction() {
        Townhall.getBuilding().clearProduction();
        EventBus.publish(new HUDChangedEvent());
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

    public boolean hasMilitaryUnitInTownHall() {
        for (Unit u : units) {
            if (isMilitaryUnit(u.getType()) && u.getCol() == Townhall.getCol() && u.getRow() == Townhall.getRow()) {
                return true;
            }
        }
        return false;
    }

    public void processWallUpkeep() {
        List<HexEdge> toRemove = new ArrayList<>();
        for (Map.Entry<HexEdge, EdgeFeature> entry : edgeFeatures.entrySet()) {
            if (entry.getValue() != EdgeFeature.WALL) continue;
            HexEdge edge = entry.getKey();
            if (economy.spendResource(ResourceType.STONE, 1)) {
                wallFailedUpkeep.remove(edge);
            } else {
                int fails = wallFailedUpkeep.getOrDefault(edge, 0) + 1;
                if (fails >= 3) {
                    toRemove.add(edge);
                } else {
                    wallFailedUpkeep.put(edge, fails);
                }
            }
        }
        for (HexEdge edge : toRemove) {
            edgeFeatures.remove(edge);
            wallHP.remove(edge);
            wallFailedUpkeep.remove(edge);
        }
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
        tribeService.resetTradeTurn();
    }

    public boolean canTradeWithTribe(Tribe tribe) {
        return tribeService.canTradeWith(tribe);
    }

    public boolean tradeWithTribe(Tribe tribe, ResourceType sellResource, int sellAmount) {
        boolean success = tribeService.tradeWithTribe(tribe, economy, sellResource, sellAmount);
        if (success) EventBus.publish(new HUDChangedEvent());
        return success;
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
            buildWallsAroundTownHall();
        }

        EventBus.publish(new HUDChangedEvent());
    }

    private void buildWallsAroundTownHall() {
        for (Tile t : Tiles) {
            if (!HexUtils.isNeighbor(Townhall.getCol(), Townhall.getRow(), t.getCol(), t.getRow())) continue;
            if (t.getTerrain() == TerrainType.SEA || t.getTerrain() == TerrainType.MOUNTAIN_RANGE) continue;

            HexEdge edge = new HexEdge(Townhall.getCol(), Townhall.getRow(), t.getCol(), t.getRow());
            edgeFeatures.put(edge, EdgeFeature.WALL);
            wallHP.put(edge, WALL_MAX_HP);
        }
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
