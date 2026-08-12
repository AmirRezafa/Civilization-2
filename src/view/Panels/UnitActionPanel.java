package view.Panels;

import controller.GameController;
import controller.events.EventBus;
import controller.events.UnitActionsChangedEvent;
import model.*;

import javax.swing.*;
import java.awt.*;

public class UnitActionPanel extends JPanel {
    private final GameController GC;
    private final JPanel buttonContainer;

    private enum SubMenu {
        MAIN, TRAIN, STORAGE, TECH
    }

    private SubMenu currentSubMenu = SubMenu.MAIN;

    private int a;

    public UnitActionPanel(GameController gc) {
        this.GC = gc;
        setVisible(false);

        this.setLayout(new BorderLayout());
        this.setBackground(new Color(40, 40, 40));
        this.setPreferredSize(new Dimension(0, 80));

        buttonContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 25));
        buttonContainer.setOpaque(false);
        this.add(buttonContainer, BorderLayout.CENTER);
        setOpaque(false);

        EventBus.subscribe(UnitActionsChangedEvent.class, e -> updateActions());
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));

        g2.setColor(new Color(40, 40, 40));
        g2.fillRect(0, 0, getWidth(), getHeight());

        g2.dispose();

        super.paintComponent(g);
    }

    public void showProducingMSG(Building building){
        String msg;
        if (building.getProducingUnit() != null) {
            msg = "Producing: " + building.getProducingUnit().getDisplayName() +
                    " (" + building.getProductionTurnsLeft() + " Turns Left)";
        } else if (building.getUpgradingToLevel() != null) {
            msg = "Upgrading to: " + building.getUpgradingToLevel().getDisplayName() +
                    " (" + building.getProductionTurnsLeft() + " Turns Left)";
        } else {
            msg = "Researching: " + building.getResearchingTech().getDisplayName() +
                    " (" + building.getProductionTurnsLeft() + " Turns Left)";
        }
        JLabel producingLabel = new JLabel(msg);
        producingLabel.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        producingLabel.setForeground(new Color(241, 196, 15));

        buttonContainer.add(producingLabel);

        JButton cancelBtn = new JButton("Cancel (no refund)");
        cancelBtn.setFocusable(false);
        cancelBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        cancelBtn.setBackground(new Color(149, 165, 166));
        cancelBtn.setForeground(Color.WHITE);
        cancelBtn.addActionListener(e -> {
            GC.cancelTownHallProduction();
            updateActions();
        });
        buttonContainer.add(cancelBtn);
    }

    public void showProduceButtons(){
        for (UnitType uType : UnitType.values()) {
            String btnText = "Train " + uType.getDisplayName() +
                    " (" + uType.getFoodCost() + " Food, " + uType.getBuildTurns() + " Turns)";

            JButton trainBtn = new JButton(btnText);
            trainBtn.setFocusable(false);
            trainBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));

            boolean canAfford = GC.hasEnoughFood(uType.getFoodCost()) &&
                    GC.checkUnitCap();
            boolean militaryCapOk = !GC.isMilitaryUnit(uType) || GC.checkMilitaryUnitCap();
            boolean stableOk = uType != UnitType.CAVALRY || GC.hasStable();
            boolean archerLevelOk = uType != UnitType.ARCHER || GC.getTownHallLevel().getLevelNumber() >= 2;
            trainBtn.setEnabled(canAfford && militaryCapOk && stableOk && archerLevelOk);

            trainBtn.addActionListener(e -> {
                GC.startProducingUnitInTownHall(uType);
                updateActions();
            });

            buttonContainer.add(trainBtn);
        }
    }

    private void showUnassignButton(int workerCount){
        JButton unassignBtn = new JButton("Unassign Worker (" + workerCount + ")");
        unassignBtn.setFocusable(false);
        unassignBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        unassignBtn.setBackground(new Color(231, 76, 60));
        unassignBtn.setForeground(Color.WHITE);

        unassignBtn.addActionListener(e -> {
            GC.removeWorker();
            updateActions();
        });

        buttonContainer.add(unassignBtn);
    }

    private void showBuildButtons(Tile currentTile){
        for (BuildingType bType : BuildingType.values()) {
            if (!bType.isPlayerBuildable()) {
                continue;
            }

            JButton buildBtn = new JButton("Build " + bType.getDisplayName()
                    + " (" + bType.getCostString() + ")");
            buildBtn.setFocusable(false);
            buildBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));

            boolean isValidTerrain = bType.isBuildableAt(currentTile, GC.getTiles())
                    && bType.isUnlocked(GC.hasStoneTech(), GC.hasIronTech(), GC.hasSettlementTech());
            boolean isTileEmpty = (currentTile.getBuilding() == null);
            boolean inTerritory = currentTile.isOwned();
            boolean levelMet = GC.getTownHallLevel().getLevelNumber() >= bType.getRequiredTownHallLevel();

            buildBtn.setEnabled(isValidTerrain && isTileEmpty && inTerritory && levelMet);

            buildBtn.addActionListener(e -> {
                GC.constructBuilding(bType);
                updateActions();
            });

            buttonContainer.add(buildBtn);
        }
    }

    private void showAttackButton() {
        JButton attackBtn = new JButton("Attack - then right-click an adjacent structure");
        attackBtn.setFocusable(false);
        attackBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        attackBtn.setBackground(new Color(192, 57, 43));
        attackBtn.setForeground(Color.WHITE);
        attackBtn.addActionListener(e -> {
            GC.startAttacking();
            updateActions();
        });
        buttonContainer.add(attackBtn);
    }

    private void showEdgeBuildButtons() {
        JButton roadBtn = new JButton("Build Road (10 Wood) - then right-click a neighbor");
        roadBtn.setFocusable(false);
        roadBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        roadBtn.setEnabled(GC.hasEnoughWood(10));
        roadBtn.addActionListener(e -> {
            GC.startBuildingEdge(EdgeFeature.ROAD);
            updateActions();
        });
        buttonContainer.add(roadBtn);

        JButton wallBtn = new JButton("Build Wall (30 Stone) - then right-click a neighbor");
        wallBtn.setFocusable(false);
        wallBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        wallBtn.setEnabled(GC.hasEnoughStone(30));
        wallBtn.addActionListener(e -> {
            GC.startBuildingEdge(EdgeFeature.WALL);
            updateActions();
        });
        buttonContainer.add(wallBtn);
    }

    private void showBazaarButtons() {
        int[] tiers = {10, 100, 500};
        for (int tier : tiers) {
            double rate = GC.bazaarRateForTier(tier);
            JButton tradeBtn = new JButton("Trade " + tier + " Wood -> Stone (" + (int) (rate * 100) + "%)");
            tradeBtn.setFocusable(false);
            tradeBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
            tradeBtn.setEnabled(GC.canUseBazaar() && GC.hasEnoughWood(tier));
            tradeBtn.addActionListener(e -> {
                GC.tradeAtBazaar(ResourceType.WOOD, ResourceType.STONE, tier);
                updateActions();
            });
            buttonContainer.add(tradeBtn);
        }
    }

    private void showTradingPostButtons(Tile currentTile) {
        if (!currentTile.isOwned()) {
            JLabel label = new JLabel("Trading Post is outside your territory");
            label.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
            label.setForeground(Color.WHITE);
            buttonContainer.add(label);
            return;
        }

        int amount = 50;
        JButton tradeBtn = new JButton("Trade " + amount + " Wood -> Stone (80%)");
        tradeBtn.setFocusable(false);
        tradeBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        tradeBtn.setEnabled(GC.canUseTradingPost() && GC.hasEnoughWood(amount));
        tradeBtn.addActionListener(e -> {
            GC.tradeAtTradingPost(ResourceType.WOOD, ResourceType.STONE, amount);
            updateActions();
        });
        buttonContainer.add(tradeBtn);
    }

    private void showTribeCampButtons(Tile currentTile) {
        Tribe tribe = null;
        for (Tribe t : GC.getTribes()) {
            if (t.getCol() == currentTile.getCol() && t.getRow() == currentTile.getRow()) {
                tribe = t;
                break;
            }
        }
        if (tribe == null) return;

        JLabel infoLabel = new JLabel("Type: " + tribe.getType().getDisplayName() +
                " | Relationship: " + tribe.getRelationship() +
                " (" + tribe.getRelationshipValue() + ") | Camp HP: " + currentTile.getBuilding().getHP() +
                " | Guards: " + tribe.getGuardUnitCount());
        infoLabel.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        infoLabel.setForeground(Color.WHITE);
        buttonContainer.add(infoLabel);

        Tribe finalTribe = tribe;

        if (currentTile.getBuilding().isDestroyed()) {
            JButton captureBtn = new JButton("Capture Camp (turns into Outpost)");
            captureBtn.setFocusable(false);
            captureBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
            captureBtn.setBackground(new Color(46, 204, 113));
            captureBtn.setForeground(Color.WHITE);
            captureBtn.addActionListener(e -> {
                GC.captureTribeCamp(finalTribe);
                updateActions();
            });
            buttonContainer.add(captureBtn);
            return;
        }

        if (tribe.getType().canTrade()) {
            int sellAmount = 20;
            boolean canTrade = GC.canTradeWithTribe(tribe);
            JButton tradeBtn = new JButton("Trade " + sellAmount + " Wood -> " +
                    (int) (sellAmount * tribe.getType().getTradeRate()) + " " +
                    tribe.getType().getTradeRewardResource().name());
            tradeBtn.setFocusable(false);
            tradeBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
            tradeBtn.setEnabled(canTrade && GC.hasEnoughWood(sellAmount));
            tradeBtn.setToolTipText(canTrade ? null :
                    "Requires Friendly/Allied relationship (>= 20) and one trade per turn");
            tradeBtn.addActionListener(e -> {
                GC.tradeWithTribe(finalTribe, ResourceType.WOOD, sellAmount);
                updateActions();
            });
            buttonContainer.add(tradeBtn);
        }

        addGiftButton(finalTribe, ResourceType.WOOD, 50);
        addGiftButton(finalTribe, ResourceType.WHEAT, 50);
        addGiftButton(finalTribe, ResourceType.STONE, 30);
        addGiftButton(finalTribe, ResourceType.IRON, 30);

        if (tribe.getActiveQuest() == null) {
            boolean canOffer = GC.canOfferQuestToTribe(tribe);
            JButton questBtn = new JButton("Ask for a Quest");
            questBtn.setFocusable(false);
            questBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
            questBtn.setEnabled(canOffer);
            questBtn.setToolTipText(canOffer ? null : "This tribe needs more time before offering a new quest");
            questBtn.addActionListener(e -> {
                GC.issueQuestToTribe(finalTribe);
                updateActions();
            });
            buttonContainer.add(questBtn);
        } else {
            JLabel questLabel = new JLabel("Quest: " + tribe.getActiveQuest().getDescription() +
                    " | Deadline: Turn " + tribe.getActiveQuest().getDeadlineTurn() +
                    (tribe.getActiveQuest().isCompleted() ? " (Completed)" : " (In Progress)"));
            questLabel.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
            questLabel.setForeground(Color.WHITE);
            buttonContainer.add(questLabel);
        }

        JButton rewardsBtn = new JButton("View Tribe Rewards");
        rewardsBtn.setFocusable(false);
        rewardsBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        rewardsBtn.addActionListener(e -> {
            String info = finalTribe.getType().canTrade()
                    ? "Trade rate: " + (int) (finalTribe.getType().getTradeRate() * 100) + "% into " +
                    finalTribe.getType().getTradeRewardResource().name()
                    : "This tribe type does not offer trade.";
            JOptionPane.showMessageDialog(this, info, "Tribe Rewards", JOptionPane.INFORMATION_MESSAGE);
        });
        buttonContainer.add(rewardsBtn);

        boolean canDeclareWar = GC.canDeclareWarOnTribe(tribe);
        JButton declareWarBtn = new JButton("Declare War");
        declareWarBtn.setFocusable(false);
        declareWarBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        declareWarBtn.setBackground(new Color(192, 57, 43));
        declareWarBtn.setForeground(Color.WHITE);
        declareWarBtn.setEnabled(canDeclareWar);
        declareWarBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Declaring war cannot be undone and will drop relationship to -100. Continue?",
                    "Declare War", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                GC.declareWarOnTribe(finalTribe);
                updateActions();
            }
        });
        buttonContainer.add(declareWarBtn);

        boolean canPeace = GC.canRequestPeaceWithTribe(tribe);
        JButton peaceBtn = new JButton("Request Peace (30 Food, 30 Wood, 30 Iron)");
        peaceBtn.setFocusable(false);
        peaceBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        peaceBtn.setEnabled(canPeace);
        peaceBtn.setToolTipText(canPeace ? null : "Only available while at war with this tribe");
        peaceBtn.addActionListener(e -> {
            GC.requestPeaceWithTribe(finalTribe);
            updateActions();
        });
        buttonContainer.add(peaceBtn);

        boolean canAlliance = GC.canRequestAllianceWithTribe(tribe);
        JButton allianceBtn = new JButton("Request Alliance");
        allianceBtn.setFocusable(false);
        allianceBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        allianceBtn.setEnabled(canAlliance);
        allianceBtn.setToolTipText(canAlliance ? null : "Relationship must be at least 70");
        allianceBtn.addActionListener(e -> {
            GC.requestAllianceWithTribe(finalTribe);
            updateActions();
        });
        buttonContainer.add(allianceBtn);
    }

    private void addGiftButton(Tribe tribe, ResourceType resource, int amount) {
        JButton giftBtn = new JButton("Send Gift (" + amount + " " + resource.name() + ")");
        giftBtn.setFocusable(false);
        giftBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        giftBtn.setEnabled(GC.hasEnoughResource(resource, amount));
        giftBtn.addActionListener(e -> {
            GC.sendGiftToTribe(tribe, resource, amount);
            updateActions();
        });
        buttonContainer.add(giftBtn);
    }

    private void showDeconstructButtons(Tile currentTile) {
        if (currentTile.getBuilding() != null && currentTile.getBuilding().getType() != BuildingType.TOWN_HALL) {
            JButton deconstructBtn = new JButton("Deconstruct " + currentTile.getBuilding().getType().getDisplayName() + " (1 AP)");
            deconstructBtn.setFocusable(false);
            deconstructBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
            deconstructBtn.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Deconstruct " + currentTile.getBuilding().getType().getDisplayName() +
                                "? This cannot be undone.",
                        "Confirm Deconstruction", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    GC.deconstructBuilding();
                    updateActions();
                }
            });
            buttonContainer.add(deconstructBtn);
        }

        JButton deconstructEdgeBtn = new JButton("Deconstruct Road/Wall (1 AP) - then right-click a neighbor");
        deconstructEdgeBtn.setFocusable(false);
        deconstructEdgeBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        deconstructEdgeBtn.addActionListener(e -> {
            GC.startDeconstructingEdge();
            updateActions();
        });
        buttonContainer.add(deconstructEdgeBtn);
    }

    private void showWorkHereButton(){
        JButton workBtn = new JButton("Work Here");
        workBtn.setFocusable(false);
        workBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        workBtn.setBackground(new Color(46, 204, 113));
        workBtn.setForeground(Color.WHITE);

        workBtn.addActionListener(e -> {
            GC.assignWorkerToBuilding();
            updateActions();
        });

        buttonContainer.add(workBtn);
    }

    private void showExpandBorderHereButton(){
        JButton expandBtn = new JButton("Expand Borders Here");
        expandBtn.setFocusable(false);
        expandBtn.setBackground(new Color(155, 89, 182));
        expandBtn.setForeground(Color.WHITE);
        expandBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));

        expandBtn.addActionListener(e -> {
            GC.expandTerritory();
            updateActions();
        });

        buttonContainer.add(expandBtn);
    }

    private void showTownHallUpgradeButton() {
        TownHallLevel currentLevel = GC.getTownHallLevel();
        TownHallLevel nextLevel = currentLevel.getNextLevel();

        JButton upgradeBtn = new JButton();
        upgradeBtn.setFocusable(false);
        upgradeBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));

        if (nextLevel == null) {
            upgradeBtn.setText("Town Hall Maxed Out (" + currentLevel.getDisplayName() + ")");
            upgradeBtn.setEnabled(false);
            buttonContainer.add(upgradeBtn);
            return;
        }

        upgradeBtn.setText("Upgrade to " + nextLevel.getDisplayName()
                + " (" + nextLevel.getUpgradeWoodCost() + " Wood, "
                + nextLevel.getUpgradeStoneCost() + " Stone, "
                + nextLevel.getUpgradeIronCost() + " Iron)");
        upgradeBtn.setEnabled(GC.hasEnoughWood(nextLevel.getUpgradeWoodCost()) &&
                GC.hasEnoughStone(nextLevel.getUpgradeStoneCost()) &&
                GC.hasEnoughIron(nextLevel.getUpgradeIronCost()));

        upgradeBtn.addActionListener(e -> {
            GC.upgradeTownHall();
            updateActions();
        });

        buttonContainer.add(upgradeBtn);
    }

    private void showResearchButtons() {
        for (TechType tech : TechType.values()) {
            JButton techBtn = new JButton(tech.getDisplayName() + " (" + tech.getCostString() + ")");
            techBtn.setFocusable(false);
            techBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));

            if (GC.hasTech(tech)) {
                techBtn.setText(tech.getDisplayName() + " ✅");
                techBtn.setEnabled(false);
            } else {
                boolean levelMet = GC.getTownHallLevel().getLevelNumber() >= tech.getRequiredLevel().getLevelNumber();
                boolean canAfford = GC.hasEnoughResource(tech.getCostResource(), tech.getCostAmount());
                techBtn.setEnabled(levelMet && canAfford);

                techBtn.addActionListener(e -> {
                    GC.researchTech(tech);
                    updateActions();
                });
            }

            buttonContainer.add(techBtn);
        }
    }

    private void showTownHallMainMenu() {
        JButton trainMenuBtn = new JButton("Train Units");
        trainMenuBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        trainMenuBtn.setFocusable(false);
        trainMenuBtn.addActionListener(e -> {
            currentSubMenu = SubMenu.TRAIN;
            updateActions();
        });

        JButton storageMenuBtn = new JButton("Upgrade Town Hall");
        storageMenuBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        storageMenuBtn.setFocusable(false);
        storageMenuBtn.addActionListener(e -> {
            currentSubMenu = SubMenu.STORAGE;
            updateActions();
        });

        JButton techMenuBtn = new JButton("Research Tech");
        techMenuBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        techMenuBtn.setFocusable(false);
        techMenuBtn.addActionListener(e -> {
            currentSubMenu = SubMenu.TECH;
            updateActions();
        });

        buttonContainer.add(trainMenuBtn);
        buttonContainer.add(storageMenuBtn);
        buttonContainer.add(techMenuBtn);
    }

    private void showBackButton() {
        JButton backBtn = new JButton("Back");
        backBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        backBtn.setFocusable(false);
        backBtn.setBackground(new Color(149, 165, 166));
        backBtn.setForeground(Color.WHITE);
        backBtn.addActionListener(e -> {
            currentSubMenu = SubMenu.MAIN;
            updateActions();
        });
        buttonContainer.add(backBtn);
    }

    public void updateActions() {
        buttonContainer.removeAll();

        a = GC.getB();
        int hGap = (int) (a * 0.6);
        int vGap = (int) (a * 0.8);
        buttonContainer.setLayout(new FlowLayout(FlowLayout.LEFT, hGap, vGap));

        Unit selectedUnit = GC.getSelectedUnit();
        Tile currentTile = GC.getTileUnderUnit();

        setVisible(false);
        if (selectedUnit == null) {
            if(currentTile == null) return;
            Building building = currentTile.getBuilding();
            if(building == null) return;
            int workerCount = building.getStationedWorkers().size();

            if (building.getType() == BuildingType.BAZAAR) {
                showBazaarButtons();
                setVisible(true);
                return;
            }

            if (building.getType() == BuildingType.TRADING_POST) {
                showTradingPostButtons(currentTile);
                setVisible(true);
                return;
            }

            if (building.getType() == BuildingType.TRIBE_CAMP) {
                showTribeCampButtons(currentTile);
                setVisible(true);
                return;
            }

            if(workerCount == 0 && building.getType() == BuildingType.TOWN_HALL){
                if (building.isProducing()) {
                    showProducingMSG(building);
                } else {
                    switch (currentSubMenu) {
                        case MAIN -> showTownHallMainMenu();
                        default -> {
                            showBackButton();
                            switch (currentSubMenu) {
                                case TRAIN -> showProduceButtons();
                                case STORAGE -> showTownHallUpgradeButton();
                                case TECH -> showResearchButtons();
                            }
                        }
                    }
                }
                setVisible(true);
            return;
            }

            if(workerCount != 0){
                showUnassignButton(workerCount);
                setVisible(true);
            }
            refreshUI();
            return;
        }

        if (selectedUnit.getType() == UnitType.BUILDER) {
            showBuildButtons(currentTile);
            showEdgeBuildButtons();
            showDeconstructButtons(currentTile);
            setVisible(true);
        }else if(selectedUnit.getType() == UnitType.WORKER){
            Building build = GC.getTileUnderUnit().getBuilding();
            if(build == null) return;

            if(!build.needWorker()) return;

            showWorkHereButton();

            setVisible(true);
        } else if (selectedUnit.getType() == UnitType.BORDER_EXPANDER) {
            showExpandBorderHereButton();
            setVisible(true);
        } else if (GC.isMilitaryUnit(selectedUnit.getType())) {
            showAttackButton();
            setVisible(true);
        }

        refreshUI();
    }

    private void refreshUI() {
        buttonContainer.revalidate();
        buttonContainer.repaint();
    }
}