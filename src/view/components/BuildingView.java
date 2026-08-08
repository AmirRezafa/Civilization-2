package view.components;

import model.Building;
import model.BuildingType;
import java.awt.*;

public class BuildingView {

    public static void show(Building building, double x, double y, int a, Graphics2D g2) {
        if (building == null) return;

        int cx = (int) x;
        int cy = (int) y;

        int size = (int) (a * 0.55);
        int half = size / 2;

        BuildingType type = building.getType();

        Color bgColor;
        String label;

        switch (type) {
            case FARM:
                bgColor = new Color(241, 196, 15);
                label = "F";
                break;
            case STONE_MINE, IRON_MINE:
                bgColor = new Color(169, 169, 169);
                label = "M";
                break;
            case LUMBER_MILL:
                bgColor = new Color(139, 69, 19);
                label = "L";
                break;
            case STABLE:
                bgColor = new Color(210, 105, 30);
                label = "S";
                break;
            case TOWN_HALL:
                bgColor = new Color(41, 128, 185);
                label = "TH";
                break;
            case SETTLEMENT:
                bgColor = new Color(46, 204, 113);
                label = "C";
                break;
            default:
                bgColor = Color.WHITE;
                label = "?";
                break;
        }

        g2.setColor(bgColor);
        g2.fillRoundRect(cx - half, cy - half, size, size, 8, 8);

        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(cx - half, cy - half, size, size, 8, 8);

        g2.setFont(new Font("SansSerif", Font.BOLD, size / 2));
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(label);

        int textAscent = fm.getAscent();
        int textDescent = fm.getDescent();

        g2.drawString(label, cx - (textWidth / 2), cy + (textAscent - textDescent) / 2);
    }
}