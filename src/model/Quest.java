package model;

public class Quest implements java.io.Serializable {
    private final String description;
    private boolean completed = false;

    public Quest(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void markCompleted() {
        completed = true;
    }
}
