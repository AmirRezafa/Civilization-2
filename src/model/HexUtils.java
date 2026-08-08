package model;

public class HexUtils {
    public static double centerX(int col) {
        return (col + 1) * 1.5;
    }

    public static double centerY(int col, int row) {
        return (row + 1) * Math.sqrt(3) +
                (col % 2 == 0 ? Math.sqrt(3) / 2 : 0);
    }

    public static boolean isNeighbor(int col1, int row1, int col2, int row2) {
        if (col1 == col2 && Math.abs(row1 - row2) == 1) return true;
        if (Math.abs(col1 - col2) == 1) {
            if (col1 % 2 == 0) {
                return (row2 == row1 || row2 == row1 + 1);
            } else {
                return (row2 == row1 || row2 == row1 - 1);
            }
        }
        return false;
    }
}
