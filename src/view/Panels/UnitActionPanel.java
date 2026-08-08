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
        String msg = "Producing: " + building.getProducingUnit().getDisplayName() +
                " (" + building.getProductionTurnsLeft() + " Turns Left)";
        JLabel producingLabel = new JLabel(msg);
        producingLabel.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        producingLabel.setForeground(new Color(241, 196, 15));

        buttonContainer.add(producingLabel);
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
            trainBtn.setEnabled(canAfford);

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

            boolean isValidTerrain = bType.isBuildableOnTerrain(currentTile.getTerrain())
                    && bType.isUnlocked(GC.hasStoneTech(), GC.hasIronTech(), GC.hasSettlementTech());
            boolean isTileEmpty = (currentTile.getBuilding() == null);
            boolean inTerritory = currentTile.isOwned();

            buildBtn.setEnabled(isValidTerrain && isTileEmpty && inTerritory);

            buildBtn.addActionListener(e -> {
                GC.constructBuilding(bType);
                updateActions();
            });

            buttonContainer.add(buildBtn);
        }
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

    private void showStorageUpgradeButtons() {
        int storageLevel = GC.getStorageLevel();
        JButton storageBtn = new JButton();
        storageBtn.setFocusable(false);
        storageBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));

        if (storageLevel == 0) {
            storageBtn.setText("Upgrade Storage Lvl 1 (100 Wood)");
            storageBtn.setEnabled(GC.hasEnoughWood(100));
            storageBtn.addActionListener(e -> {
                GC.upgradeStorage();
                updateActions();
            });
            buttonContainer.add(storageBtn);
        } else if (storageLevel == 1) {
            storageBtn.setText("Upgrade Storage Lvl 2 (200 Wood, 100 Stone)");
            storageBtn.setEnabled(GC.hasEnoughWood(200) && GC.hasEnoughStone(100));
            storageBtn.addActionListener(e -> {
                GC.upgradeStorage();
                updateActions();
            });
            buttonContainer.add(storageBtn);
        } else {
            storageBtn.setText("Storage Maxed Out (Lvl 2)");
            storageBtn.setEnabled(false);
            buttonContainer.add(storageBtn);
        }
    }

    private void showResearchButtons() {
        JButton stoneTechBtn = new JButton("Stone Mining Tech (50 Wood)");
        stoneTechBtn.setFocusable(false);
        stoneTechBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));

        if (GC.hasStoneTech()) {
            stoneTechBtn.setText("Stone Mining ✅");
            stoneTechBtn.setEnabled(false);
        } else {
            stoneTechBtn.setEnabled(GC.hasEnoughWood(50));
            stoneTechBtn.addActionListener(e -> {
                GC.researchStoneTech();
                updateActions();
            });
        }
        buttonContainer.add(stoneTechBtn);

        JButton ironTechBtn = new JButton("Iron Mining Tech (100 Stone)");
        ironTechBtn.setFocusable(false);
        ironTechBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));

        if (GC.hasIronTech()) {
            ironTechBtn.setText("Iron Mining ✅");
            ironTechBtn.setEnabled(false);
        } else {
            ironTechBtn.setEnabled(GC.hasEnoughStone(100));
            ironTechBtn.addActionListener(e -> {
                GC.researchIronTech();
                updateActions();
            });
        }
        buttonContainer.add(ironTechBtn);

        JButton settlementTechBtn = new JButton("Settlement Tech (150 Wood)");
        settlementTechBtn.setFocusable(false);
        settlementTechBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));

        if (GC.hasSettlementTech()) {
            settlementTechBtn.setText("Settlement ✅");
            settlementTechBtn.setEnabled(false);
        } else {
            settlementTechBtn.setEnabled(GC.hasEnoughWood(150));
            settlementTechBtn.addActionListener(e -> {
                GC.researchSettlementTech();
                updateActions();
            });
        }
        buttonContainer.add(settlementTechBtn);

        JButton toolsTechBtn = new JButton("Pro Tools Tech (100 Iron)");
        toolsTechBtn.setFocusable(false);
        toolsTechBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));

        if (GC.hasProToolsTech()) {
            toolsTechBtn.setText("Pro Tools (1.5x) ✅");
            toolsTechBtn.setEnabled(false);
        } else {
            toolsTechBtn.setEnabled(GC.hasEnoughIron(100));
            toolsTechBtn.addActionListener(e -> {
                GC.researchProToolsTech();
                updateActions();
            });
        }
        buttonContainer.add(toolsTechBtn);
    }

    private void showTownHallMainMenu() {
        JButton trainMenuBtn = new JButton("Train Units");
        trainMenuBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        trainMenuBtn.setFocusable(false);
        trainMenuBtn.addActionListener(e -> {
            currentSubMenu = SubMenu.TRAIN;
            updateActions();
        });

        JButton storageMenuBtn = new JButton("Storage Upgrades");
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
                                case STORAGE -> showStorageUpgradeButtons();
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
        }

        refreshUI();
    }

    private void refreshUI() {
        buttonContainer.revalidate();
        buttonContainer.repaint();
    }
}