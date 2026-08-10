package controller.services;

import model.Tribe;

public class TribeService {
    public void sendGift(Tribe tribe, int resourceAmount) {
        int relationshipBoost = resourceAmount / 10;
        tribe.changeRelationship(relationshipBoost);
    }

    public void recordWar(Tribe tribe) {
        tribe.changeRelationship(-50);
    }
}
