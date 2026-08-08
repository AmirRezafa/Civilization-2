package controller.services;

import model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class WorldGenerator {

    public static class WorldData {
        public final ArrayList<Tile> tiles;
        public final Tile[][] tileGrid;
        public final Tile townhall;
        public final Building townhallBuilding;
        public final List<Unit> initialUnits;

        public WorldData(ArrayList<Tile> tiles, Tile[][] tileGrid, Tile townhall,
                          Building townhallBuilding, List<Unit> initialUnits) {
            this.tiles = tiles;
            this.tileGrid = tileGrid;
            this.townhall = townhall;
            this.townhallBuilding = townhallBuilding;
            this.initialUnits = initialUnits;
        }
    }

    public WorldData generate(int rows, int cols, int townhallX, int townhallY) {
        Random random = new Random();
        TerrainType[] types = {TerrainType.PLAIN, TerrainType.FOREST, TerrainType.MOUNTAIN, TerrainType.MEADOW};

        int seedsCount = 150;
        int[][] seeds = new int[seedsCount + types.length + 1][2];
        TerrainType[] seedTypes = new TerrainType[seedsCount + types.length + 1];

        for (int i = 0; i < seedsCount; i++) {
            int c = random.nextInt(cols);
            int r = random.nextInt(rows);
            while(Math.pow(c - townhallX, 2) + Math.pow(r - townhallY, 2) < 16){
                c = random.nextInt(cols);
                r = random.nextInt(rows);
            }
            seeds[i][0] = c;
            seeds[i][1] = r;
            seedTypes[i] = types[random.nextInt(types.length)];
        }

        ArrayList<int[]> positions = new ArrayList<>(List.of(
                new int[]{townhallX + 5, townhallY},
                new int[]{townhallX - 5, townhallY},
                new int[]{townhallX, townhallY + 5},
                new int[]{townhallX, townhallY - 5},
                new int[]{townhallX + 4, townhallY + 4}
        ));

        Collections.shuffle(positions, random);

        for(int i = 0; i < types.length + 1; i++){
            seeds[i + seedsCount] = positions.get(i);
            seedTypes[i + seedsCount] = types[((i + 1) % types.length)];
        }

        ArrayList<Tile> tempTiles = new ArrayList<>();
        Tile[][] tileGrid = new Tile[rows][cols];
        Tile townhallTile = null;

        for (int col = 0; col < cols; col++) {
            for (int row = 0; row < rows; row++) {
                double minD = Double.MAX_VALUE;
                TerrainType finalType = types[0];

                for (int i = 0; i < seedsCount + types.length + 1; i++) {
                    double dist = Math.pow(seeds[i][0] - col, 2) + Math.pow(seeds[i][1] - row, 2);
                    dist += random.nextDouble() * 8.0;

                    if (dist < minD) {
                        minD = dist;
                        finalType = seedTypes[i];
                    }
                }

                Map<ResourceType, Integer> tileResources = new HashMap<>();

                switch (finalType) {
                    case FOREST:
                        tileResources.put(ResourceType.WOOD, 500);
                        break;

                    case MOUNTAIN:
                        tileResources.put(ResourceType.STONE, 500);

                        if (random.nextDouble() < 0.20)
                            tileResources.put(ResourceType.IRON, 150);
                        break;

                    case PLAIN:
                        if (random.nextDouble() < 0.20)
                            tileResources.put(ResourceType.CATTLE, 300);
                        break;

                    case MEADOW:
                        if (random.nextDouble() < 0.30)
                            tileResources.put(ResourceType.WHEAT, 300);
                        break;
                }
                Tile tile = new Tile(col, row, finalType, tileResources);
                tileGrid[col][row] = tile;
                tempTiles.add(tile);
                if(col == townhallX && row == townhallY) townhallTile = tile;
            }
        }

        Building townhallBuilding = new Building(BuildingType.TOWN_HALL, townhallX, townhallY);
        townhallTile.setBuilding(townhallBuilding);

        List<Unit> initialUnits = new ArrayList<>();
        initialUnits.add(new Unit(UnitType.BUILDER, townhallX, townhallY + 1));
        initialUnits.add(new Unit(UnitType.BUILDER, townhallX + 1, townhallY));
        initialUnits.add(new Unit(UnitType.WORKER, townhallX - 1, townhallY + 1));
        initialUnits.add(new Unit(UnitType.WORKER, townhallX, townhallY - 1));
        initialUnits.add(new Unit(UnitType.EXPLORER, townhallX + 1, townhallY + 1));

        return new WorldData(tempTiles, tileGrid, townhallTile, townhallBuilding, initialUnits);
    }
}
