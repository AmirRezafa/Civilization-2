package model;

public class GlobalHappinessManager {
    private int happiness = 0;

    public void addHappiness(int delta) {
        happiness += delta;
    }

    public int getHappiness() {
        return happiness;
    }

    public boolean isGoldenAge() {
        return happiness >= 3;
    }

    public boolean isDissatisfied() {
        return happiness <= -3 && happiness >= -4;
    }

    public boolean isRiot() {
        return happiness <= -5;
    }
}
