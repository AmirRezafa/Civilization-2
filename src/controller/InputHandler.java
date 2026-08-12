package controller;

import controller.events.EventBus;
import controller.events.UnitActionsChangedEvent;
import model.EdgeFeature;
import model.HexUtils;
import model.Season;
import model.TechType;
import model.TerrainType;
import model.Tile;
import model.Unit;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class InputHandler extends MouseAdapter {
    private final GameController gc;

    public InputHandler(GameController gc) {
        this.gc = gc;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        handleMouseClick(e);
    }

    private Unit getUnitAt(int col, int row) {
        for (Unit u : gc.getUnits()) {
            if (u.isAssigned()) continue;
            if (u.getCol() == col && u.getRow() == row) return u;
        }
        return null;
    }

    private Tile getTileAtPixel(int pixelX, int pixelY) {
        int worldX = pixelX + gc.getXOffset();
        int worldY = pixelY + gc.getYOffset();
        int a = gc.getA();

        Tile closestTile = null;
        double minDistance = Double.MAX_VALUE;

        for (Tile tile : gc.getTiles()) {
            double x = HexUtils.centerX(tile.getCol()) * a;
            double y = HexUtils.centerY(tile.getCol(), tile.getRow()) * a;

            double distance = Math.pow(worldX - x, 2) + Math.pow(worldY - y, 2);
            if (distance < minDistance) {
                minDistance = distance;
                closestTile = tile;
            }
        }

        if (minDistance <= a * a * 1.5) return closestTile;
        return null;
    }

    private void handleMouseClick(MouseEvent e) {
        Tile clickedTile = getTileAtPixel(e.getX(), e.getY());
        if (clickedTile == null) return;

        if (SwingUtilities.isLeftMouseButton(e)) {
            Unit unitOnTile = getUnitAt(clickedTile.getCol(), clickedTile.getRow());
            if (unitOnTile != null) {
                gc.setSelectedUnit(unitOnTile);
                gc.setTileUnderUnit(clickedTile);
            } else {
                gc.setSelectedUnit(null);
                gc.setTileUnderUnit(clickedTile);
            }
            EventBus.publish(new UnitActionsChangedEvent());
        } else if (SwingUtilities.isRightMouseButton(e)) {
            Unit selectedUnit = gc.getSelectedUnit();
            if (selectedUnit != null) {
                if (HexUtils.isNeighbor(selectedUnit.getCol(), selectedUnit.getRow(), clickedTile.getCol(), clickedTile.getRow())) {
                    EdgeFeature pending = gc.getPendingEdgeBuild();
                    if (pending != null) {
                        if (gc.buildEdgeFeature(selectedUnit.getCol(), selectedUnit.getRow(),
                                clickedTile.getCol(), clickedTile.getRow(), pending)) {
                            EventBus.publish(new UnitActionsChangedEvent());
                        }
                        return;
                    }

                    if (gc.isPendingEdgeDeconstruct()) {
                        if (gc.deconstructEdge(selectedUnit.getCol(), selectedUnit.getRow(),
                                clickedTile.getCol(), clickedTile.getRow())) {
                            EventBus.publish(new UnitActionsChangedEvent());
                        }
                        return;
                    }

                    if (gc.isPendingAttack()) {
                        if (gc.attackAdjacentStructure(clickedTile.getCol(), clickedTile.getRow())) {
                            EventBus.publish(new UnitActionsChangedEvent());
                        }
                        return;
                    }

                    if (!gc.canStackAt(clickedTile.getCol(), clickedTile.getRow(), selectedUnit.getType())) {
                        return;
                    }

                    TerrainType targetTerrain = clickedTile.getTerrain();
                    if (!targetTerrain.isPassable()) {
                        return;
                    }
                    if (targetTerrain == TerrainType.SEA && !gc.hasTech(TechType.SAILING)) {
                        return;
                    }

                    EdgeFeature edge = gc.getEdgeFeature(selectedUnit.getCol(), selectedUnit.getRow(),
                            clickedTile.getCol(), clickedTile.getRow());

                    Season season = gc.getCurrentSeason();
                    int movementCost;
                    if (targetTerrain == TerrainType.SEA) {
                        movementCost = targetTerrain.getMovementCost() + season.getWaterMovementPenalty();
                    } else if (edge == EdgeFeature.ROAD) {
                        movementCost = 1;
                    } else {
                        movementCost = targetTerrain.getMovementCost();
                        if (edge == EdgeFeature.RIVER) movementCost += 2;
                        movementCost += season.getLandMovementPenalty();
                    }

                    if(selectedUnit.move(clickedTile.getCol(), clickedTile.getRow(), movementCost)){
                        gc.setTileUnderUnit(clickedTile);
                        gc.updateFog();
                        EventBus.publish(new UnitActionsChangedEvent());
                    }
                }
            }
        }
    }
}
