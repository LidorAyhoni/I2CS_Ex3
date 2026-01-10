package assignments.Ex3.levels;

import assignments.Ex3.model.GameState;
import assignments.Ex3.model.Ghost;
import assignments.Ex3.model.Tile;

import java.util.ArrayList;
import java.util.List;

public class LevelLoader {

    // 0 empty, 1 wall, 2 dot, 3 power, 4 ghost
    public static GameState level0() {
        int[][] m = {
                {1,1,1,1,1,1,1,1,1,1,1,1,1},
                {1,3,2,2,2,1,2,2,2,1,2,3,1},
                {1,2,1,1,2,1,2,1,2,1,2,2,1},
                {1,2,1,1,2,2,2,1,2,2,2,2,1},
                {1,2,2,2,2,1,1,4,1,1,2,2,1},
                {1,2,1,1,2,1,0,0,0,1,2,1,1},
                {1,2,2,2,2,2,2,2,2,2,2,2,1},
                {1,2,1,1,2,1,1,1,1,1,2,1,1},
                {1,3,2,2,2,2,2,2,2,2,2,3,1},
                {1,1,1,1,1,1,1,1,1,1,1,1,1},
        };


        int H = m.length;
        int W = m[0].length;

        Tile[][] g = new Tile[W][H];
        List<int[]> ghostSpawns = new ArrayList<>();

        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int v = m[y][x];

                int gx = x;
                int gy = H - 1 - y; // keep your coordinate flip

                if (v == 4) {
                    ghostSpawns.add(new int[]{gx, gy});
                    g[gx][gy] = Tile.EMPTY;
                    continue;
                }

                g[gx][gy] = switch (v) {
                    case 1 -> Tile.WALL;
                    case 2 -> Tile.DOT;
                    case 3 -> Tile.POWER;
                    default -> Tile.EMPTY;
                };
            }
        }

        GameState s = new GameState(g, 1, 1);

        // create ghosts from spawns
        for (int[] p : ghostSpawns) {
            s.addGhost(new Ghost(p[0], p[1]));
        }

        return s;
    }
    public static GameState level1() {

        int[][] m = {
                {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
                {1,3,2,2,2,1,2,2,2,1,2,2,2,1,2,2,2,2,2,3,1},
                {1,2,1,1,2,1,2,1,2,1,2,1,2,1,2,1,2,1,1,2,1},
                {1,2,1,1,2,2,2,1,2,2,2,1,2,2,2,1,2,2,2,2,1},
                {1,2,2,2,2,1,1,1,1,1,2,1,1,1,1,1,2,1,2,2,1},
                {1,1,1,1,2,1,2,2,2,1,2,1,2,2,2,1,2,1,2,1,1},
                {1,2,2,2,2,1,2,1,2,1,2,2,2,2,2,1,2,2,2,2,1},
                {1,2,1,1,2,1,2,2,2,2,2,2,2,2,2,1,2,1,1,2,1},
                {1,2,2,2,2,2,2,2,2,2,1,1,1,1,2,2,2,2,2,2,1},
                {1,1,1,1,2,1,1,1,2,2,1,4,4,1,2,1,2,1,1,1,1},
                {1,2,2,2,2,2,2,1,2,2,1,4,4,1,2,1,2,2,2,2,1},
                {1,2,1,1,2,2,2,1,2,2,1,0,0,1,2,1,2,2,2,2,1},
                {1,2,2,2,2,1,2,1,2,2,2,2,2,2,2,1,2,1,1,2,1},
                {1,2,1,1,2,1,2,1,2,1,2,1,2,1,2,1,2,1,1,2,1},
                {1,2,2,2,2,1,2,1,2,1,2,1,2,1,2,1,2,2,2,2,1},
                {1,1,1,1,2,1,2,1,2,1,2,1,2,1,2,1,2,1,2,1,1},
                {1,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,1},
                {1,2,1,1,1,1,2,2,2,2,1,2,2,2,2,1,1,1,1,2,1},
                {1,2,2,2,2,2,2,2,2,2,1,2,2,2,2,2,2,2,2,2,1},
                {1,3,1,1,1,1,2,1,2,2,2,2,2,1,2,1,1,1,1,3,1},
                {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
        };

        int H = m.length;
        int W = m[0].length;

        Tile[][] g = new Tile[W][H];
        java.util.List<int[]> ghostSpawns = new java.util.ArrayList<>();

        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int v = m[y][x];

                int gx = x;
                int gy = H - 1 - y; // keep your coordinate flip

                if (v == 4) {
                    ghostSpawns.add(new int[]{gx, gy});
                    g[gx][gy] = Tile.EMPTY;
                    continue;
                }

                g[gx][gy] = switch (v) {
                    case 1 -> Tile.WALL;
                    case 2 -> Tile.DOT;
                    case 3 -> Tile.POWER;
                    default -> Tile.EMPTY;
                };
            }
        }

        // ✅ Start not on POWER (was (1,1) which is a POWER in this map)
        GameState s = new GameState(g, 1, 2);

        // create ghosts from spawns
        for (int[] p : ghostSpawns) {
            s.addGhost(new Ghost(p[0], p[1]));
        }

        return s;
    }
    public static GameState level2() {

        int[][] m = {
                {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
                {1,3,2,2,2,1,2,2,2,2,1,2,2,2,1,2,2,2,2,1,2,2,2,3,1},
                {1,2,1,1,2,1,2,1,1,2,1,2,1,2,1,2,1,1,2,1,2,1,1,2,1},
                {1,2,1,1,2,2,2,2,2,2,2,2,1,2,2,2,2,2,2,2,2,1,1,2,1},
                {1,2,2,2,2,1,1,1,1,1,1,2,1,2,1,1,1,1,1,1,2,2,2,2,1},
                {1,1,1,1,2,1,2,2,2,2,1,2,2,2,1,2,2,2,2,1,2,1,1,1,1},
                {1,2,2,2,2,1,2,1,1,2,1,2,1,2,1,2,1,1,2,1,2,2,2,2,1},
                {1,2,1,1,2,1,2,1,1,2,1,2,1,2,1,2,1,1,2,1,2,1,1,2,1},
                {1,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,1},
                {1,1,1,1,2,2,2,2,1,2,1,0,0,0,1,2,1,2,2,2,2,1,1,1,1},

                // --- Ghost house area (0 = empty, 4 = ghost spawn) ---
                {1,2,2,2,2,2,2,2,1,2,1,4,4,4,1,2,1,2,2,2,2,2,2,2,1},
                {1,2,1,1,2,1,1,1,1,2,1,4,4,4,1,2,1,1,1,1,2,1,1,2,1},
                {1,2,2,2,2,2,2,2,1,2,1,1,1,1,1,2,1,2,2,2,2,2,2,2,1},
                // -----------------------------------------------

                {1,2,1,1,2,2,2,2,1,2,2,2,2,2,2,2,1,2,2,2,2,1,1,2,1},
                {1,2,2,2,2,2,2,2,1,2,2,2,2,2,2,2,1,2,2,2,2,2,2,2,1},
                {1,1,1,1,2,1,2,2,2,2,1,2,2,2,1,2,2,2,2,1,2,1,1,1,1},
                {1,2,2,2,2,1,2,1,1,2,1,2,1,2,1,2,1,1,2,1,2,2,2,2,1},
                {1,2,1,1,2,1,2,1,1,2,1,2,1,2,1,2,1,1,2,1,2,1,1,2,1},

                {1,3,2,2,2,1,2,2,2,2,1,2,2,2,1,2,2,2,2,1,2,2,2,3,1},
                {1,2,1,1,2,1,2,1,1,2,1,2,1,2,1,2,1,1,2,1,2,1,1,2,1},

                {1,2,2,2,2,2,2,2,2,2,1,2,2,2,1,2,2,2,2,2,2,2,2,2,1},
                {1,2,1,1,1,1,1,1,1,2,1,2,1,2,1,2,1,1,1,1,1,1,1,2,1},
                {1,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,1},

                {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
        };

        int H = m.length;
        int W = m[0].length;

        Tile[][] g = new Tile[W][H];
        java.util.List<int[]> ghostSpawns = new java.util.ArrayList<>();

        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int v = m[y][x];

                int gx = x;
                int gy = H - 1 - y; // keep your coordinate flip

                if (v == 4) {
                    ghostSpawns.add(new int[]{gx, gy});
                    g[gx][gy] = Tile.EMPTY;
                    continue;
                }

                g[gx][gy] = switch (v) {
                    case 1 -> Tile.WALL;
                    case 2 -> Tile.DOT;
                    case 3 -> Tile.POWER;
                    default -> Tile.EMPTY;
                };
            }
        }

        // Start not on a POWER
        GameState s = new GameState(g, 1, 2);

        for (int[] p : ghostSpawns) {
            s.addGhost(new Ghost(p[0], p[1]));
        }

        return s;
    }

}
