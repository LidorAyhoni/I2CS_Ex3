package assignments.Ex3;

import assignments.Ex3.model.*;
import assignments.Ex3.render.Renderer;
import assignments.Ex3.server.GameLoop;
import assignments.Ex3.server.control.DirectionProvider;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;

import static org.junit.jupiter.api.Assertions.*;

public class GameLoopTest {

    // --------- Simple test doubles (no GUI / no keyboard) ---------

    private static class DummyRenderer implements Renderer {
        int renders = 0;

        @Override
        public void init(int pixels, int gridW, int gridH) {
            // not needed for tests
        }

        @Override
        public void render(GameState s) {
            renders++;
        }
    }

    private static class QueueDirectionProvider implements DirectionProvider {
        private final Deque<Direction> q = new ArrayDeque<>();

        QueueDirectionProvider(Direction... dirs) {
            for (Direction d : dirs) q.addLast(d);
        }

        @Override
        public Direction nextDirection(GameState s) {
            // If we run out, just stay
            return q.isEmpty() ? Direction.STAY : q.removeFirst();
        }
    }

    // --------- Helpers ---------

    private Tile[][] emptyWithBorderWalls(int w, int h) {
        Tile[][] g = new Tile[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) g[x][y] = Tile.EMPTY;
        }
        for (int x = 0; x < w; x++) {
            g[x][0] = Tile.WALL;
            g[x][h - 1] = Tile.WALL;
        }
        for (int y = 0; y < h; y++) {
            g[0][y] = Tile.WALL;
            g[w - 1][y] = Tile.WALL;
        }
        return g;
    }

    // --------- Tests ---------

    @Test
    public void run_eatsSingleDot_scores10_andStopsBecauseNoDotsLeft() {
        // Map: pacman at (1,1), a DOT at (2,1)
        Tile[][] g = emptyWithBorderWalls(5, 5);
        g[2][1] = Tile.DOT;

        GameState s = new GameState(g, 1, 1);

        DummyRenderer r = new DummyRenderer();
        DirectionProvider p = new QueueDirectionProvider(Direction.RIGHT);

        // dtMs = 0 so no sleep delay
        GameLoop loop = new GameLoop(s, r, p, null, 0);
        loop.run();

        assertTrue(s.isDone(), "Game should end when no DOT tiles remain");
        assertEquals(2, s.getPacmanX());
        assertEquals(1, s.getPacmanY());
        assertEquals(10, s.getScore(), "Eating DOT should give +10");
        assertTrue(r.renders > 0, "Renderer should be called at least once");
        assertEquals(Tile.EMPTY, s.grid[2][1], "DOT tile should be consumed");
    }

    @Test
    public void run_eatsPower_scores50_activatesPower_andMakesGhostsEatable() {
        // Important: GameLoop ends when there are NO DOT tiles.
        // So we create a map with POWER only (no DOT) to end quickly after first iteration.
        Tile[][] g = emptyWithBorderWalls(5, 5);
        g[2][1] = Tile.POWER;

        GameState s = new GameState(g, 1, 1);

        Ghost ghost = new Ghost(3, 1);
        s.addGhost(ghost);
        assertFalse(ghost.isEatable(), "Ghost should start non-eatable");

        DummyRenderer r = new DummyRenderer();
        DirectionProvider p = new QueueDirectionProvider(Direction.RIGHT);

        GameLoop loop = new GameLoop(s, r, p, null, 0);
        loop.run();

        assertTrue(s.isDone(), "Game should end immediately since there are no DOT tiles");
        assertEquals(2, s.getPacmanX());
        assertEquals(1, s.getPacmanY());
        assertEquals(50, s.getScore(), "Eating POWER should give +50");
        assertTrue(s.isPowerMode(), "Power mode should be active after eating POWER");
        assertTrue(ghost.isEatable(), "Ghosts should become eatable in power mode");
        assertEquals(Tile.EMPTY, s.grid[2][1], "POWER tile should be consumed");
        assertTrue(r.renders > 0, "Renderer should be called at least once");
    }
}
