package model;

public class Tribe implements java.io.Serializable {
    private final int col, row;
    private int relationshipValue = 0;
    private Quest activeQuest;

    public Tribe(int col, int row) {
        this.col = col;
        this.row = row;
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
}
