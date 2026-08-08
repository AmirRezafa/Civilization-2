package model;

import java.util.ArrayList;

public class Building {
    private int col, row;

    private final BuildingType type;
    private boolean isOccupied;
    private ArrayList<Unit> workers;

    private UnitType producingUnit = null;
    private int productionTurnsLeft = 0;

    private int failedCount = 0;


    public Building(BuildingType type, int col, int row) {
        this.col = col;
        this.row = row;

        this.type = type;
        this.isOccupied = false;
        workers = new ArrayList<>();
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    public BuildingType getType() {
        return type;
    }

    public boolean isOccupied() {
        return isOccupied;
    }

    public void addWorker(Unit worker){
        workers.add(worker);
        isOccupied = true;
    }

    public void removeWorker(Unit worker){
        workers.remove(worker);
        if(workers.isEmpty()) isOccupied = false;
    }

    public boolean needWorker() {
        return workers.size() < type.getMaxWorkerCapacity();
    }

    public ArrayList<Unit> getStationedWorkers() {
        return workers;
    }

    public Unit getLastWorker() {
        return workers.get(workers.size() - 1);
    }

    public void startProducing(UnitType type) {
        this.producingUnit = type;
        this.productionTurnsLeft = type.getBuildTurns();
    }

    public boolean isProducing() {
        return (producingUnit != null);
    }

    public UnitType getProducingUnit() {
        return producingUnit;
    }

    public int getProductionTurnsLeft() {
        return productionTurnsLeft;
    }

    public void decrementProductionTurns() {
        productionTurnsLeft--;
    }

    public void clearProduction() {
        producingUnit = null;
        productionTurnsLeft = 0;
    }

    public void upkeepFailed() {
        failedCount++;
    }

    public int getFailedCount() {
        return failedCount;
    }
}