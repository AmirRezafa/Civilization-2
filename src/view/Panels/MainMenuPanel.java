package view.Panels;

import view.MainFrame;

import javax.swing.*;
import java.awt.*;

public class MainMenuPanel extends JPanel {
    private static final Color backGroundColor = new Color(25, 25, 25);
    private static final Dimension ButtonSize = new Dimension(220, 50);

    private final JLabel label = new JLabel("Civilization");

    private static MainFrame MF;

    private enum Button {
        Start("Start Game") {
            @Override
            public void clicked() {
                MF.showGame();
            }
        },
        Setting("Settings") {
            @Override
            public void clicked() {
                MF.showSettings();
            }
        },
        Exit("Exit") {
            @Override
            public void clicked() {
                System.out.println("Game Closed Safely.");
                System.exit(0);
            }
        };

        private final String name;
        Button(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public abstract void clicked();
    }

    public MainMenuPanel(MainFrame MF) {
        MainMenuPanel.MF = MF;
        setLayout(new GridBagLayout());
        setBackground(backGroundColor);

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setBackground(backGroundColor);
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));

        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("SansSerif", Font.BOLD, 45));
        buttonsPanel.add(label);
        buttonsPanel.add(Box.createVerticalStrut(25));

        for (Button button : Button.values()) {
            JButton temp = new JButton(button.getName());
            temp.setAlignmentX(Component.CENTER_ALIGNMENT);
            temp.setPreferredSize(ButtonSize);
            temp.setMinimumSize(ButtonSize);
            temp.setMaximumSize(ButtonSize);
            temp.setFocusPainted(false);

            temp.setFont(new Font("SansSerif", Font.BOLD, 14));
            temp.setBackground(new Color(52, 73, 94));
            temp.setForeground(Color.WHITE);

            temp.addActionListener(e -> button.clicked());

            buttonsPanel.add(temp);
            buttonsPanel.add(Box.createVerticalStrut(15));
        }
        add(buttonsPanel);
    }
}