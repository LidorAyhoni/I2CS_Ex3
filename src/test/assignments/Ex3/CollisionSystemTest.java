package assignments.Ex3;

import assignments.Ex3.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CollisionSystemTest {

    private Tile[][] open5x5() {
        Tile[][] g = new Tile[5][5];
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) g[x][y] = Tile.EMPTY;
        }
        // border walls
        for (int i = 0; i < 5; i++) {
            g[0][i] = Tile.WALL;
            g[4][i] = Tile.WALL;
            g[i][0] = Tile.WALL;
            g[i][4] = Tile.WALL;
        }
        return g;
    }

    @Test
    public void resolve_noGhosts_noCrash() {
        GameState s = new GameState(open5x5(), 2, 2);
        CollisionSystem cs = new CollisionSystem();

        assertDoesNotThrow(() -> cs.resolve(s));
        assertEquals(3, s.getLives());
        assertFalse(s.isDone());
        assertEquals(0, s.getScore());
    }

    @Test
    public void resolve_noCollision_noEffect() {
        GameState s = new GameState(open5x5(), 2, 2);
        s.addGhost(new Ghost(1, 1));
        s.addGhost(new Ghost(3, 3));

        int livesBefore = s.getLives();
        int scoreBefore = s.getScore();

        CollisionSystem cs = new CollisionSystem();
        cs.resolve(s);

        assertEquals(livesBefore, s.getLives());
        assertEquals(scoreBefore, s.getScore());
        assertFalse(s.isDone());
    }

    @Test
    public void resolve_collisionWithNonEatableGhost_losesLife() {
        GameState s = new GameState(open5x5(), 2, 2);

        Ghost g = new Ghost(2, 2); // same cell as pacman -> collision
        g.setEatable(false);
        s.addGhost(g);

        CollisionSystem cs = new CollisionSystem();
        cs.resolve(s);

        assertEquals(2, s.getLives());     // started at 3 -> now 2
        assertFalse(s.isDone());
        // score should not increase when hit
        assertEquals(0, s.getScore());
    }

    @Test
    public void resolve_collisionWithEatableGhost_addsScoreAndDoesNotLoseLife() {
        GameState s = new GameState(open5x5(), 2, 2);

        Ghost g = new Ghost(2, 2);
        s.addGhost(g);

        // easiest way to ensure eatable=true in your design: power mode
        s.activatePower(5);
        assertTrue(g.isEatable());

        int livesBefore = s.getLives();

        CollisionSystem cs = new CollisionSystem();
        cs.resolve(s);

        assertEquals(livesBefore, s.getLives());
        assertEquals(200, s.getScore());  // your GameState gives 200 on eat
    }

    @Test
    public void resolve_multipleGhosts_firstCollisionTriggersAndStops() {
        GameState s = new GameState(open5x5(), 2, 2);

        Ghost g1 = new Ghost(2, 2); // collides
        Ghost g2 = new Ghost(2, 2); // also collides, but resolve returns after first

        // make both non-eatable
        g1.setEatable(false);
        g2.setEatable(false);

        s.addGhost(g1);
        s.addGhost(g2);

        CollisionSystem cs = new CollisionSystem();
        cs.resolve(s);

        // should lose exactly 1 life (because resolve returns after first collision)
        assertEquals(2, s.getLives());
    }
}
