package controller.events;

import model.DisasterType;

public class DisasterEvent {
    private final DisasterType type;
    private final String message;

    public DisasterEvent(DisasterType type, String message) {
        this.type = type;
        this.message = message;
    }

    public DisasterType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }
}
