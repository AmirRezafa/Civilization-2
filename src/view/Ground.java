package view;

import controller.GameController;
import model.EdgeFeature;
import model.HexEdge;
import model.HexUtils;
import model.TerrainType;
import model.Tile;
import model.Unit;
import view.components.*;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class Ground extends JPanel{
    private int width, height;
    private GameController GC;


    Ground(){
        setBackground(Color.GRAY);
    }

    public void setController(GameController controller) {
        GC = controller;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        width = getWidth();
        height = getHeight();

        g2.translate(-GC.getXOffset(), -GC.getYOffset());

        int a = GC.getA();

        int xOffset = GC.getXOffset();
        int yOffset = GC.getYOffset();

        for(Tile tile : GC.getTiles()) {

            double x = HexUtils.centerX(tile.getCol()) * a;
            double y = HexUtils.centerY(tile.getCol(), tile.getRow()) * a;

            if (x < xOffset - a * 2 || x > xOffset + width + a * 2 ||
                    y < yOffset - a * 2 || y > yOffset + height + a * 2){
                continue;
            }

            TileView.show(x, y, a, g2, getTerrainColor(tile.getTerrain()), tile);
            ResourceView.show(tile, a, x, y, g2);
            StationedWorkerView.show(tile, a, x, y, g2);
            if(tile.getBuilding() != null && tile.isVisible()){
                BuildingView.show(tile.getBuilding(), x, y, a, g2);
            }
        }

        for (Map.Entry<HexEdge, EdgeFeature> entry : GC.getEdgeFeatures().entrySet()) {
            HexEdge edge = entry.getKey();
            Tile t1 = GC.getTileAt(edge.getCol1(), edge.getRow1());
            Tile t2 = GC.getTileAt(edge.getCol2(), edge.getRow2());
            if (!t1.isVisible() || !t2.isVisible()) continue;

            int wallHP = entry.getValue() == EdgeFeature.WALL
                    ? GC.getWallHP(edge.getCol1(), edge.getRow1(), edge.getCol2(), edge.getRow2())
                    : 0;
            EdgeView.show(edge.getCol1(), edge.getRow1(), edge.getCol2(), edge.getRow2(),
                    entry.getValue(), wallHP, a, g2);
        }

        g2.setColor(Color.RED);
        for (Unit unit : GC.getUnits()) {
            if(unit.isAssigned()) continue;
            UnitView.show(unit, a, unit == GC.getSelectedUnit(), g2);
        }

        g2.translate(GC.getXOffset(), GC.getYOffset());

        if (GC.getSelectedUnit() != null) {
            Unit selectedUnit = GC.getSelectedUnit();

            g2.setColor(new Color(20, 20, 20, 220));
            g2.fillRoundRect(20, height - 140, 260, 100, 15, 15);
            g2.setColor(Color.YELLOW);
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(20, height - 140, 260, 100, 15, 15);

            g2.setColor(Color.YELLOW);
            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            g2.drawString(" UNIT SELECTED", 40, height - 115);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2.drawString("Coordinates: [ X: " + selectedUnit.getCol() + " , Y: " + selectedUnit.getRow() + " ]", 40, height - 90);

            g2.drawString("Terrain Type: " + GC.getTileUnderUnit().getTerrain().toString(), 40, height - 70);
        }

        g2.dispose();
    }

    private Color getTerrainColor(TerrainType type) {
        return switch (type) {
            case PLAIN -> new Color(180, 200, 100);
            case FOREST -> new Color(34, 139, 34);
            case MOUNTAIN -> new Color(128, 128, 128);
            case MEADOW -> new Color(144, 238, 144);
            case SEA -> new Color(65, 105, 225);
            case MOUNTAIN_RANGE -> new Color(90, 90, 90);
        };
    }

    public int getwidth() {
        return width;
    }

    public int getheight() {
        return height;
    }
}
