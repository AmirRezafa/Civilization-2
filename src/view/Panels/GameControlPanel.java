package view.Panels;

import controller.GameController;
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
    private JLabel unitLabel;
    private JLabel foodLabel;
    private JLabel woodLabel;
    private JLabel stoneLabel;
    private JLabel ironLabel;
    private JButton nextTurnButton;


    public GameControlPanel(GameController gc) {
        this.GC = gc;

        this.setLayout(new FlowLayout(FlowLayout.CENTER, 25, 12));
        this.setBackground(new Color(45, 45, 45));

        initializeComponents();
        updateHUD();

        EventBus.subscribe(HUDChangedEvent.class, e -> updateHUD());
        EventBus.subscribe(StarvationEvent.class, e -> showStarvationAlert());
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


        unitLabel = createLabel(hudFont, textColor);
        foodLabel = createLabel(hudFont, textColor);
        woodLabel = createLabel(hudFont, textColor);
        stoneLabel = createLabel(hudFont, textColor);
        ironLabel = createLabel(hudFont, textColor);

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