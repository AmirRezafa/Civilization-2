package model;

public class Tribe implements java.io.Serializable {
    private final int col, row;
    private final TribeType type;
    private int relationshipValue = 0;
    private Quest activeQuest;
    private int questCooldownUntilTurn = 0;
    private int guardUnitCount = 0;

    public Tribe(int col, int row, TribeType type) {
        this.col = col;
        this.row = row;
        this.type = type;
    }

    public TribeType getType() {
        return type;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    public int getRelationshipValue() {
        return relationshipValue;
    }

    public void changeRelationship(int delta) {
        relationshipValue = Math.max(-100, Math.min(100, relationshipValue + delta));
    }

    public TribeRelationship getRelationship() {
        return TribeRelationship.fromValue(relationshipValue);
    }

    public Quest getActiveQuest() {
        return activeQuest;
    }

    public void setActiveQuest(Quest quest) {
        this.activeQuest = quest;
    }

    public int getQuestCooldownUntilTurn() {
        return questCooldownUntilTurn;
    }

    public void setQuestCooldownUntilTurn(int turn) {
        this.questCooldownUntilTurn = turn;
    }

    public int getGuardUnitCount() {
        return guardUnitCount;
    }

    public void setGuardUnitCount(int count) {
        this.guardUnitCount = Math.max(0, count);
    }

    public int getGuardUnitCap() {
        return type == TribeType.WARRIOR ? 5 : 3;
    }
}
