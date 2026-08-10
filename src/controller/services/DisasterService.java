package controller.services;

import controller.GameController;
import model.Building;
import model.BuildingType;
import model.DisasterType;
import model.Unit;

import java.util.List;
import java.util.Random;

public class DisasterService {
    private static final double DISASTER_CHANCE = 0.05;
    private static final int BUILDING_DAMAGE = 15;

    private final Random random = new Random();

    public DisasterType rollForDisaster(GameController gc) {
        if (random.nextDouble() >= DISASTER_CHANCE) return null;

        DisasterType[] types = DisasterType.values();
        DisasterType disaster = types[random.nextInt(types.length)];
        applyDisaster(gc);
        return disaster;
    }

    private void applyDisaster(GameController gc) {
        List<Building> buildings = gc.getBuildings();
        if (!buildings.isEmpty()) {
            Building target = buildings.get(random.nextInt(buildings.size()));
            if (target.getType() != BuildingType.TOWN_HALL) {
                target.takeDamage(BUILDING_DAMAGE);
                if (target.isDestroyed()) {
                    gc.removeDestroyedBuilding(target);
                }
            }
        }

        List<Unit> units = gc.getUnits();
        if (!units.isEmpty()) {
            Unit target = units.get(random.nextInt(units.size()));
            target.takeHit();
            if (target.isDead()) {
                gc.deleteUnit(target);
            }
        }
    }
}
