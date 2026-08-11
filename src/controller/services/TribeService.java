package controller.services;

import model.EdgeFeature;
import model.HexEdge;
import model.Quest;
import model.Tribe;

import java.util.Map;

public class TribeService {
    private static final int QUEST_REWARD = 20;

    public void sendGift(Tribe tribe, int resourceAmount) {
        int relationshipBoost = resourceAmount / 10;
        tribe.changeRelationship(relationshipBoost);
    }

    public void recordWar(Tribe tribe) {
        tribe.changeRelationship(-50);
    }

    public void issueRoadQuest(Tribe tribe) {
        tribe.setActiveQuest(new Quest("Build a road to our camp"));
    }

    public boolean isRoadQuestSatisfied(Tribe tribe, Map<HexEdge, EdgeFeature> edgeFeatures) {
        for (Map.Entry<HexEdge, EdgeFeature> entry : edgeFeatures.entrySet()) {
            if (entry.getValue() != EdgeFeature.ROAD) continue;

            HexEdge edge = entry.getKey();
            boolean touchesCamp = (edge.getCol1() == tribe.getCol() && edge.getRow1() == tribe.getRow()) ||
                    (edge.getCol2() == tribe.getCol() && edge.getRow2() == tribe.getRow());
            if (touchesCamp) return true;
        }
        return false;
    }

    public void completeQuest(Tribe tribe) {
        Quest quest = tribe.getActiveQuest();
        if (quest == null || quest.isCompleted()) return;

        quest.markCompleted();
        tribe.changeRelationship(QUEST_REWARD);
    }
}
