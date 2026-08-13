package controller.services;

import model.Building;
import model.Unit;
import model.UnitType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class CombatService implements java.io.Serializable {
    private final Random random = new Random();

    private List<Integer> rollDice(List<Unit> units) {
        int swordsmenCount = 0, cavalryCount = 0, archerCount = 0;
        for (Unit u : units) {
            if (u.getType() == UnitType.SWORDSMAN) swordsmenCount++;
            else if (u.getType() == UnitType.CAVALRY) cavalryCount++;
            else if (u.getType() == UnitType.ARCHER) archerCount++;
        }

        List<Integer> rolls = new ArrayList<>();
        for (int i = 0; i < swordsmenCount; i++) rolls.add(random.nextInt(6) + 1);
        for (int i = 0; i < cavalryCount; i++) rolls.add(random.nextInt(6) + 1);
        if (archerCount > 0) rolls.add(random.nextInt(6) + 1);

        return rolls;
    }

    private void applyHits(List<Unit> units, int hitCount) {
        List<Unit> ordered = new ArrayList<>();
        for (Unit u : units) if (u.getType() == UnitType.SWORDSMAN) ordered.add(u);
        for (Unit u : units) if (u.getType() == UnitType.ARCHER) ordered.add(u);
        for (Unit u : units) if (u.getType() == UnitType.CAVALRY) ordered.add(u);

        int remainingHits = hitCount;
        for (Unit u : ordered) {
            while (remainingHits > 0 && !u.isDead()) {
                u.takeHit();
                remainingHits--;
            }
            if (remainingHits <= 0) break;
        }
    }

    public void resolveCombat(List<Unit> attackers, List<Unit> defenders, boolean defenderHasWall) {
        List<Integer> attackerRolls = rollDice(attackers);
        List<Integer> defenderRolls = rollDice(defenders);

        if (defenderHasWall) {
            for (int i = 0; i < defenderRolls.size(); i++) {
                defenderRolls.set(i, Math.min(6, defenderRolls.get(i) + 2));
            }
        }

        attackerRolls.sort(Collections.reverseOrder());
        defenderRolls.sort(Collections.reverseOrder());

        int pairs = Math.min(attackerRolls.size(), defenderRolls.size());
        int defenderHits = 0, attackerHits = 0;

        for (int i = 0; i < pairs; i++) {
            if (attackerRolls.get(i) > defenderRolls.get(i)) {
                defenderHits++;
            } else {
                attackerHits++;
            }
        }

        applyHits(defenders, defenderHits);
        applyHits(attackers, attackerHits);
    }

    public int calculateStructureDamage(List<Unit> attackers) {
        int totalDamage = 0;
        for (Unit u : attackers) {
            totalDamage += u.getType().getAttackPower();
        }
        return totalDamage;
    }

    public void attackStructure(List<Unit> attackers, Building building) {
        building.takeDamage(calculateStructureDamage(attackers));
    }
}
