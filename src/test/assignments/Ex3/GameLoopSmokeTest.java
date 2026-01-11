package assignments.Ex3;

import assignments.Ex3.model.*;
import assignments.Ex3.render.Renderer;
import assignments.Ex3.server.GameLoop;
import assignments.Ex3.server.control.DirectionProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GameLoopSmokeTest {

    private static class DummyRenderer implements Renderer {
        int renders = 0;
        @Override public void init(int pixels, int gridW, int gridH) {}
        @Override public void render(GameState s) { renders++; }
    }

    private static class StayProvider implements DirectionProvider {
        @Override public Direction nextDirection(GameState s) { return Direction.STAY; }
    }

    private Tile[][] open5x5() {
        Tile[][] g = new Tile[5][5];
        for (int x = 0; x < 5; x++) for (int y = 0; y < 5; y++) g[x][y] = Tile.EMPTY;
        for (int i = 0; i < 5; i++) {
            g[0][i] = Tile.WALL; g[4][i] = Tile.WALL; g[i][0] = Tile.WALL; g[i][4] = Tile.WALL;
        }
        // add a DOT so loop doesn't instantly stop
        g[2][3] = Tile.DOT;
        return g;
    }

    @Test
    public void run_withStayProvider_doesNotCrash_andRenders() {
        GameState s = new GameState(open5x5(), 2, 2);
        DummyRenderer r = new DummyRenderer();

        GameLoop loop = new GameLoop(s, r, new StayProvider(), null, 0);

        // This will eventually stop by maxSteps (since pacman never eats the DOT),
        // but it's a valid smoke test.
        loop.run();

        assertTrue(s.isDone());
        assertTrue(r.renders > 0);
    }
}
