package assignments.Ex3;

import exe.ex3.game.Game;
import exe.ex3.game.GhostCL;
import exe.ex3.game.PacmanGame;
import main.java.assignments.Ex3.Ex3Algo;
import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

public class Ex3AlgoTest {

    // ---- Fake minimal game/ghost objects (only what Ex3Algo uses) ----

    private static class FakeGhost implements GhostCL {
        private final String pos;
        private final int eatableTicks;

        FakeGhost(int x, int y, int eatableTicks) {
            this.pos = x + "," + y;
            this.eatableTicks = eatableTicks;
        }

        @Override
        public int getType() {
            return 0;
        }

        @Override
        public String getPos(int code) { return pos; }

        @Override
        public String getInfo() {
            return "";
        }

        @Override
        public double remainTimeAsEatable(int code) { return eatableTicks; }

        @Override
        public int getStatus() {
            return 0;
        }

        // If your GhostCL interface has more methods in your jar,
        // IntelliJ will tell you what to implement; implement them with dummy values.
    }

    private static class FakeGame implements PacmanGame {
        private final int[][] board;
        private final String pacPos;
        private final GhostCL[] ghosts;

        FakeGame(int[][] board, int px, int py, GhostCL[] ghosts) {
            this.board = board;
            this.pacPos = px + "," + py;
            this.ghosts = ghosts;
        }

        @Override
        public int[][] getGame(int code) { return board; }

        @Override
        public String move(int i) {
            return "";
        }

        @Override
        public void play() {

        }

        @Override
        public String end(int i) {
            return "";
        }

        @Override
        public String getData(int i) {
            return "";
        }

        @Override
        public int getStatus() {
            return 0;
        }

        @Override
        public boolean isCyclic() {
            return false;
        }

        @Override
        public String init(int i, String s, boolean b, long l, double v, int i1, int i2) {
            return "";
        }

        @Override
        public Character getKeyChar() {
            return null;
        }

        @Override
        public String getPos(int code) { return pacPos; }

        @Override
        public GhostCL[] getGhosts(int code) { return ghosts; }

        // If PacmanGame has more methods in your jar,
        // implement them as stubs (throw UnsupportedOperationException or return defaults).
    }

    // ---- Helpers to build boards that match Ex3Algo's color expectations ----

    private static int PINK_DOT() { return Game.getIntColor(Color.PINK, 0); }
    private static int GREEN_POWER() { return Game.getIntColor(Color.GREEN, 0); }

    private static int[][] simpleBoardWithWallsAndDots() {
        // 7x7, -1 walls border + some inside walls so "walls are common" for wall-detection
        int W = -1;
        int E = 0;
        int D = PINK_DOT();
        int G = GREEN_POWER();

        int[][] b = new int[7][7];
        for (int x = 0; x < 7; x++) for (int y = 0; y < 7; y++) b[x][y] = E;

        // border walls
        for (int i = 0; i < 7; i++) { b[i][0]=W; b[i][6]=W; b[0][i]=W; b[6][i]=W; }

        // inside walls (increase frequency of walls)
        b[3][1]=W; b[3][2]=W; b[3][3]=W; b[3][4]=W; b[3][5]=W;

        // dots
        b[1][1]=D; b[1][2]=D; b[1][3]=D;
        b[5][5]=D;

        // one GREEN somewhere
        b[2][5]=G;

        return b;
    }

    private static boolean isLegalDir(int dir) {
        return dir == Game.UP || dir == Game.DOWN || dir == Game.LEFT || dir == Game.RIGHT || dir == Game.STAY;
    }

    @Test
    public void move_returns_a_legal_direction() {
        Ex3Algo algo = new Ex3Algo();
        int[][] b = simpleBoardWithWallsAndDots();
        FakeGame g = new FakeGame(b, 1, 1, new GhostCL[0]);

        int dir = algo.move(g);
        assertTrue(isLegalDir(dir), "move() must return one of UP/DOWN/LEFT/RIGHT/STAY");
    }

    @Test
    public void avoids_wall_move_when_surrounded_by_walls() {
        Ex3Algo algo = new Ex3Algo();

        int W = -1;
        int E = 0;
        int[][] b = new int[5][5];
        for (int x = 0; x < 5; x++) for (int y = 0; y < 5; y++) b[x][y] = W;
        // only one open cell (2,2)
        b[2][2] = E;

        FakeGame g = new FakeGame(b, 2, 2, new GhostCL[0]);

        int dir = algo.move(g);
        // In this case every step is a wall; algorithm should not crash.
        // Returning STAY is acceptable.
        assertTrue(isLegalDir(dir));
    }

    @Test
    public void when_danger_ghost_is_near_prefers_escape_not_toward_ghost() {
        Ex3Algo algo = new Ex3Algo();
        int[][] b = simpleBoardWithWallsAndDots();

        // Pac at (1,1). Ghost near at (2,1) and NOT eatable -> danger.
        GhostCL[] ghosts = new GhostCL[]{ new FakeGhost(2,1,0) };
        FakeGame g = new FakeGame(b, 1, 1, ghosts);

        int dir = algo.move(g);

        // going RIGHT from (1,1) would step closer to ghost at (2,1)
        assertNotEquals(Game.RIGHT, dir, "Should avoid moving toward an adjacent danger ghost if alternatives exist");
    }

    @Test
    public void early_game_blocks_green_when_other_moves_exist() {
        Ex3Algo algo = new Ex3Algo();

        int W = -1;
        int E = 0;
        int D = PINK_DOT();
        int G = GREEN_POWER();

        // Pac at (2,2), GREEN to the right, DOT to the left -> should prefer DOT early (NO_POWER_FIRST_TICKS)
        int[][] b = new int[5][5];
        for (int x = 0; x < 5; x++) for (int y = 0; y < 5; y++) b[x][y] = E;
        for (int i = 0; i < 5; i++) { b[i][0]=W; b[i][4]=W; b[0][i]=W; b[4][i]=W; }

        b[3][2] = G; // right
        b[1][2] = D; // left

        FakeGame g = new FakeGame(b, 2, 2, new GhostCL[0]);

        int dir = algo.move(g);
        assertNotEquals(Game.RIGHT, dir, "Early ticks should avoid stepping on GREEN if another option exists");
    }
}
