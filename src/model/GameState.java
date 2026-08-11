package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GameState implements Serializable {
    public ArrayList<Tile> tiles;
    public Tile[][] tileGrid;
    public ArrayList<Unit> units;
    public ArrayList<Building> buildings;
    public Map<HexEdge, EdgeFeature> edgeFeatures;
    public GlobalResourceManager economy;
    public GlobalHappinessManager happinessManager;
    public List<Tribe> tribes;
    public Map<TechType, Boolean> researchedTechs;
    public Map<UnitType, Integer> unitCount;
    public int currentTurn;
    public int unitCapacity;
}
