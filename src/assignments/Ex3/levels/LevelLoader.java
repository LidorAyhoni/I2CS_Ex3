package assignments.Ex3.levels;

import assignments.Ex3.model.GameState;
import assignments.Ex3.model.Tile;

public class LevelLoader {

    // 0 empty, 1 wall, 2 dot, 3 power
    public static GameState level0() {
        int[][] m = {
                {1,1,1,1,1,1,1,1,1,1},
                {1,2,2,2,2,2,2,2,2,1},
                {1,2,1,1,2,1,1,1,2,1},
                {1,2,2,2,2,2,2,1,2,1},
                {1,1,1,1,1,1,2,1,2,1},
                {1,2,2,2,2,2,2,2,2,1},
                {1,1,1,1,1,1,1,1,1,1},
        };

        int H = m.length;
        int W = m[0].length;

        Tile[][] g = new Tile[W][H];

        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int v = m[y][x];
                g[x][H - 1 - y] = switch (v) {
                    case 1 -> Tile.WALL;
                    case 2 -> Tile.DOT;
                    case 3 -> Tile.POWER;
                    default -> Tile.EMPTY;
                };
            }
        }

        return new GameState(g, 1, 1);
    }
}
