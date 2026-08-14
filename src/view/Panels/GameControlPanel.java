package view.Panels;

import controller.GameController;
import controller.events.CombatResultEvent;
import controller.events.DisasterEvent;
import controller.events.EventBus;
import controller.events.HUDChangedEvent;
import controller.events.StarvationEvent;
import model.ResourceType;
import model.UnitType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GameControlPanel extends JPanel {
    private final GameController GC;

    private JLabel turnLabel;
    private JLabel seasonLabel;
    private JLabel unitLabel;
    private JLabel foodLabel;
    private JLabel woodLabel;
    private JLabel stoneLabel;
    private JLabel ironLabel;
    private JLabel fishLabel;
    private JLabel happinessLabel;
    private JButton nextTurnButton;


    public GameControlPanel(GameController gc) {
        this.GC = gc;

        this.setLayout(new FlowLayout(FlowLayout.CENTER, 25, 12));
        this.setBackground(new Color(45, 45, 45));
        // FlowLayout.preferredLayoutSize() always assumes a single row, even when the actual
        // available width forces it to wrap onto a second line. Since this panel sits in
        // BorderLayout.NORTH (sized via getPreferredSize()), a wrapped second row would be
        // pushed outside the reserved area and rendered behind the map. Reserve enough
        // height up front for two rows so nothing (e.g. the Pause button) goes missing.
        this.setPreferredSize(new Dimension(0, 100));

        initializeComponents();
        updateHUD();

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "openPauseMenu");
        getActionMap().put("openPauseMenu", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showPauseMenu();
            }
        });

        EventBus.subscribe(HUDChangedEvent.class, e -> updateHUD());
        EventBus.subscribe(StarvationEvent.class, e -> showStarvationAlert());
        EventBus.subscribe(DisasterEvent.class, this::showDisasterAlert);
        EventBus.subscribe(CombatResultEvent.class, this::showCombatResult);
    }

    private void showDisasterAlert(DisasterEvent event) {
        JOptionPane.showMessageDialog(this, event.getMessage(), "Natural Disaster: " + event.getType(),
                JOptionPane.WARNING_MESSAGE);
    }

    private void showCombatResult(CombatResultEvent event) {
        StringBuilder message = new StringBuilder();
        message.append("Attacker rolls: ").append(event.getAttackerRolls()).append("\n");
        message.append("Defender rolls: ").append(event.getDefenderRolls());
        if (event.isDefenderHadWall()) message.append(" (+2 each from wall)");
        message.append("\n\n");
        message.append("Hits on defender: ").append(event.getDefenderHits()).append("\n");
        message.append("Hits on attacker: ").append(event.getAttackerHits());

        JOptionPane.showMessageDialog(this, message.toString(), "Combat Result", JOptionPane.PLAIN_MESSAGE);
    }

    private JLabel createLabel(Font hudFont, Color textColor){
        JLabel label = new JLabel();
        label.setFont(hudFont);
        label.setForeground(textColor);
        this.add(label);
        return label;
    }

    private void initializeComponents() {
        Font hudFont = new Font("SansSerif", Font.BOLD, 14);
        Color textColor = Color.WHITE;

        turnLabel = new JLabel("Turn: 1");
        turnLabel.setFont(hudFont);
        turnLabel.setForeground(new Color(241, 196, 15));
        this.add(turnLabel);

        seasonLabel = new JLabel("Season: Spring");
        seasonLabel.setFont(hudFont);
        seasonLabel.setForeground(new Color(102, 204, 255));
        this.add(seasonLabel);


        unitLabel = createLabel(hudFont, textColor);
        foodLabel = createLabel(hudFont, textColor);
        woodLabel = createLabel(hudFont, textColor);
        stoneLabel = createLabel(hudFont, textColor);
        ironLabel = createLabel(hudFont, textColor);
        fishLabel = createLabel(hudFont, textColor);
        happinessLabel = createLabel(hudFont, textColor);

        nextTurnButton = new JButton("Next Turn");
        nextTurnButton.setFont(hudFont);
        nextTurnButton.setFocusable(false);
        nextTurnButton.setBackground(new Color(39, 174, 96));
        nextTurnButton.setForeground(Color.WHITE);

        nextTurnButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleNextTurnAction();
            }
        });

        this.add(nextTurnButton);

        JButton pauseButton = new JButton("Pause");
        pauseButton.setFont(hudFont);
        pauseButton.setFocusable(false);
        pauseButton.addActionListener(e -> showPauseMenu());
        this.add(pauseButton);
    }

    private void showPauseMenu() {
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(topFrame, "Paused", true);
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        dialog.setContentPane(content);

        for (int slot = 1; slot <= 3; slot++) {
            content.add(buildSlotRow(dialog, slot));
            content.add(Box.createVerticalStrut(8));
        }

        JButton resumeButton = new JButton("Resume");
        resumeButton.setFocusable(false);
        resumeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        resumeButton.addActionListener(e -> dialog.dispose());
        content.add(resumeButton);

        dialog.setResizable(true);
        dialog.setMinimumSize(new Dimension(420, 260));
        dialog.pack();
        dialog.setLocationRelativeTo(topFrame);
        dialog.setVisible(true);
    }

    private JPanel buildSlotRow(JDialog dialog, int slot) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        JLabel summaryLabel = new JLabel("Slot " + slot + ": " + GC.peekSaveSummary(slot));
        summaryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(summaryLabel);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton saveBtn = new JButton("Save");
        saveBtn.setFocusable(false);
        saveBtn.addActionListener(e -> handleSaveAction(slot, summaryLabel));
        buttonRow.add(saveBtn);

        JButton loadBtn = new JButton("Load");
        loadBtn.setFocusable(false);
        loadBtn.addActionListener(e -> {
            handleLoadAction(slot);
            dialog.dispose();
        });
        buttonRow.add(loadBtn);

        row.add(buttonRow);
        return row;
    }

    private void handleSaveAction(int slot, JLabel summaryLabel) {
        if (!GC.isSaveAllowed()) {
            JOptionPane.showMessageDialog(this,
                    "Finish or cancel your pending action (attack/build/deconstruct) before saving.",
                    "Save Unavailable", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean success = GC.saveGame(slot);
        if (success) summaryLabel.setText("Slot " + slot + ": " + GC.peekSaveSummary(slot));
        JOptionPane.showMessageDialog(this, success ? "Game saved to slot " + slot : "Save failed",
                "Save Game", success ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
    }

    private void handleLoadAction(int slot) {
        boolean success = GC.loadGame(slot);
        JOptionPane.showMessageDialog(this, success ? "Game loaded from slot " + slot : "Load failed (empty or corrupted slot)",
                "Load Game", success ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);

        if (success) {
            turnLabel.setText("Turn: " + GC.getCurrentTurn());
            updateHUD();

            JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
            if (topFrame != null) topFrame.repaint();
        }
    }

    private void handleNextTurnAction() {
        if (GC.hasUnitsWithRemainingAP()) {

            String warningMessage = "<html><div style='font-family: \"Segoe UI\", sans-serif;'>"
                    + "<b>Hold on, bro!</b> some of your units still have Action Points (AP) left.<br>"
                    + "Are you sure you want to end this turn and waste their moves?"
                    + "</div></html>";

            int response = JOptionPane.showConfirmDialog(
                    this,
                    warningMessage,
                    "Unused Action Points",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (response != JOptionPane.YES_OPTION) {
                return;
            }
        }
        GC.advanceTurn();

        turnLabel.setText("Turn: " + GC.getCurrentTurn());

        updateHUD();

        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        if (topFrame != null) {
            topFrame.repaint();
        }
    }

    public void updateHUD() {
        var economy = GC.getEconomy();
        GC.updateNetChanges();
        seasonLabel.setText("Season: " + GC.getCurrentSeason());
        unitLabel.setText("Unit: " + GC.getUnitCounts() + "/" + GC.getUnitCapacity());
        foodLabel.setText("Food: " + (economy.getNetChanges(ResourceType.CATTLE) +
                economy.getNetChanges(ResourceType.WHEAT)) + " | " +
                (economy.getResourceAmount(ResourceType.CATTLE) +
                economy.getResourceAmount(ResourceType.WHEAT)) + "/" +
                (economy.getResourceCapacityAmount(ResourceType.CATTLE) +
                        economy.getResourceCapacityAmount(ResourceType.WHEAT)));
        woodLabel.setText("Wood: " + economy.getNetChanges(ResourceType.WOOD) + " | " +
                economy.getResourceAmount(ResourceType.WOOD) + "/" +
                economy.getResourceCapacityAmount(ResourceType.WOOD));
        stoneLabel.setText("Stone: " + economy.getNetChanges(ResourceType.STONE) + " | " +
                economy.getResourceAmount(ResourceType.STONE) + "/" +
                economy.getResourceCapacityAmount(ResourceType.STONE));
        ironLabel.setText("Iron: " + economy.getNetChanges(ResourceType.IRON) + " | " +
                economy.getResourceAmount(ResourceType.IRON) + "/" +
                economy.getResourceCapacityAmount(ResourceType.IRON));
        fishLabel.setText("Fish: " + economy.getNetChanges(ResourceType.FISH) + " | " +
                economy.getResourceAmount(ResourceType.FISH) + "/" +
                economy.getResourceCapacityAmount(ResourceType.FISH));
        happinessLabel.setText("Happiness: " + GC.getHappinessManager().getHappiness());

        int explorerCounts = GC.getUnitCounts(UnitType.EXPLORER);
        int builderCounts = GC.getUnitCounts(UnitType.BUILDER);
        int workerCounts = GC.getUnitCounts(UnitType.WORKER);
        int expanderCounts = GC.getUnitCounts(UnitType.BORDER_EXPANDER);


// ساخت یک متن HTML شیک برای تول‌تیپ
        String tooltipText = "<html>" +
                "Explorers: " + explorerCounts + "<br>" +
                "Builders: " + builderCounts + "<br>" +
                "Workers: " + workerCounts + "<br>" +
                "Expanders: " + expanderCounts +
                "</html>";

        unitLabel.setToolTipText(tooltipText);
    }

// آره خلاصه اینجا احساس صمیمیت کردم
    public void showStarvationAlert() {
        String alertHtml = "<html><div style='width: 280px; text-align: left; font-family: \"Segoe UI\", sans-serif; padding: 5px;'>"
                + "<h2 style='color: #e74c3c; margin: 0 0 10px 0; font-size: 16px;'>CRITICAL CRISIS!</h2>"
                + "<p style='color: #333333; font-size: 13px; line-height: 1.6;'>"
                + "<b>Bro, we are in a total crisis!</b><br>"
                + "Food reserves hit zero and starvation is kicking in. "
                + "Fix the situation ASAP before your units start dropping dead!</p>"
                + "</div></html>";

        JLabel label = new JLabel(alertHtml);

        JOptionPane.showMessageDialog(
                this,
                label,
                "Emergency",
                JOptionPane.WARNING_MESSAGE
        );
    }
}