package view.components;

import model.Unit;
import java.awt.*;

public class UnitView {
    public static void show(Unit unit, int a, boolean selected, Graphics2D g2) {
        double x = unit.getX() * a;
        double y = unit.getY() * a;

        Color unitColor;
        String label;

        switch (unit.getType()) {
            case EXPLORER:
                unitColor = new Color(52, 152, 219);
                label = "E";
                break;
            case WORKER:
                unitColor = new Color(230, 126, 34);
                label = "W";
                break;
            case BUILDER:
                unitColor = new Color(155, 89, 182);
                label = "B";
                break;
            case BORDER_EXPANDER:
                unitColor = new Color(26, 188, 156);
                label = "BE"; //Momkene bezane biroon choon 2 harfi e
                break;
            default:
                unitColor = Color.RED;
                label = "U";
                break;
        }

        if (selected) {
            g2.setColor(Color.YELLOW);
            g2.setStroke(new BasicStroke(3));
            g2.drawOval((int)x - a/2, (int)y - a/2, a, a);
        }

        g2.setColor(unitColor);
        g2.fillOval((int)x - a/4, (int)y - a/4, a/2, a/2);

        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval((int)x - a/4, (int)y - a/4, a/2, a/2);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, a / 4));

        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(label);
        int textHeight = fm.getAscent() - fm.getDescent();

        g2.drawString(label, (int)x - (textWidth / 2), (int)y + (textHeight / 2));
    }
}