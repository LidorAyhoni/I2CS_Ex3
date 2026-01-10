package assignments.Ex3.model;

public class GameState {
    public final int w, h;
    public final Tile[][] grid;

    public int pacX, pacY;
    public int score = 0;
    public boolean done = false;

    public GameState(Tile[][] grid, int pacX, int pacY) {
        this.grid = grid;
        this.w = grid.length;
        this.h = grid[0].length;
        this.pacX = pacX;
        this.pacY = pacY;
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < w && y < h;
    }

    public boolean isWall(int x, int y) {
        return !inBounds(x, y) || grid[x][y] == Tile.WALL;
    }
}
