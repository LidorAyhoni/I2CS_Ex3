package assignments.Ex3;


import org.junit.jupiter.api.Test;
import assignments.Ex3.model.*;

import static org.junit.jupiter.api.Assertions.*;

public class GameStateTest {

    private Tile[][] grid3x3CenterDot() {
        return new Tile[][]{
                {Tile.WALL, Tile.WALL, Tile.WALL},
                {Tile.WALL, Tile.DOT,  Tile.WALL},
                {Tile.WALL, Tile.WALL, Tile.WALL},
        };
    }

    private Tile[][] grid5x5Open() {
        Tile[][] g = new Tile[5][5];
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) g[x][y] = Tile.EMPTY;
        }
        // add border walls
        for (int i = 0; i < 5; i++) {
            g[0][i] = Tile.WALL;
            g[4][i] = Tile.WALL;
            g[i][0] = Tile.WALL;
            g[i][4] = Tile.WALL;
        }
        return g;
    }

    // ---------- ctor / basic getters ----------

    @Test
    public void ctor_valid_initializesFields() {
        Tile[][] g = grid3x3CenterDot();
        GameState s = new GameState(g, 1, 1);

        assertEquals(3, s.w);
        assertEquals(3, s.h);
        assertSame(g, s.grid);

        assertEquals(1, s.getPacmanX());
        assertEquals(1, s.getPacmanY());

        assertEquals(0, s.getScore());
        assertFalse(s.isDone());
        assertEquals(3, s.getLives());
    }

    @Test
    public void ctor_invalidGrid_throws() {
        assertThrows(IllegalArgumentException.class, () -> new GameState(null, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new GameState(new Tile[][]{}, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new GameState(new Tile[][]{ {} }, 0, 0));
    }

    // ---------- bounds / walls ----------

    @Test
    public void inBounds_checksCorrectly() {
        GameState s = new GameState(grid3x3CenterDot(), 1, 1);

        assertTrue(s.inBounds(0, 0));
        assertTrue(s.inBounds(2, 2));
        assertFalse(s.inBounds(-1, 0));
        assertFalse(s.inBounds(0, -1));
        assertFalse(s.inBounds(3, 0));
        assertFalse(s.inBounds(0, 3));
    }

    @Test
    public void isWall_trueForWallsAndOutOfBounds_falseForOpen() {
        Tile[][] g = grid3x3CenterDot();
        GameState s = new GameState(g, 1, 1);

        assertTrue(s.isWall(0, 0));     // wall
        assertTrue(s.isWall(-1, 0));    // OOB
        assertTrue(s.isWall(3, 3));     // OOB
        assertFalse(s.isWall(1, 1));    // DOT is not WALL
    }

    // ---------- score / lives ----------

    @Test
    public void addScore_addsOnlyPositive() {
        GameState s = new GameState(grid3x3CenterDot(), 1, 1);

        s.addScore(10);
        assertEquals(10, s.getScore());

        s.addScore(0);
        assertEquals(10, s.getScore());

        s.addScore(-5);
        assertEquals(10, s.getScore());
    }

    @Test
    public void loseLife_decrements_untilDoneAtZero() {
        GameState s = new GameState(grid3x3CenterDot(), 1, 1);

        assertEquals(3, s.getLives());
        assertFalse(s.isDone());

        s.loseLife();
        assertEquals(2, s.getLives());
        assertFalse(s.isDone());

        s.loseLife();
        assertEquals(1, s.getLives());
        assertFalse(s.isDone());

        s.loseLife();
        assertEquals(0, s.getLives());
        assertTrue(s.isDone());

        // once done, further loseLife should not go negative / change
        s.loseLife();
        assertEquals(0, s.getLives());
        assertTrue(s.isDone());
    }

    // ---------- ghosts registration ----------

    @Test
    public void addGhost_null_throws() {
        GameState s = new GameState(grid5x5Open(), 2, 2);
        assertThrows(IllegalArgumentException.class, () -> s.addGhost(null));
    }

    @Test
    public void addGhost_onWallOrOutOfBounds_throws() {
        GameState s = new GameState(grid5x5Open(), 2, 2);

        // border is WALL
        assertThrows(IllegalArgumentException.class, () -> s.addGhost(new Ghost(0, 0)));
        // OOB
        assertThrows(IllegalArgumentException.class, () -> s.addGhost(new Ghost(10, 10)));
    }

    @Test
    public void addGhost_onFreeCell_addedAndListIsUnmodifiable() {
        GameState s = new GameState(grid5x5Open(), 2, 2);

        Ghost g = new Ghost(1, 1);
        s.addGhost(g);

        assertEquals(1, s.getGhosts().size());
        assertSame(g, s.getGhosts().get(0));

        // unmodifiable list
        assertThrows(UnsupportedOperationException.class, () -> s.getGhosts().add(new Ghost(2, 2)));
    }

    // ---------- power mode ----------

    @Test
    public void activatePower_setsPowerModeAndMakesGhostsEatable() {
        GameState s = new GameState(grid5x5Open(), 2, 2);
        Ghost g1 = new Ghost(1, 1);
        Ghost g2 = new Ghost(3, 3);
        s.addGhost(g1);
        s.addGhost(g2);

        assertFalse(s.isPowerMode());
        assertFalse(g1.isEatable());
        assertFalse(g2.isEatable());

        s.activatePower(5);

        assertTrue(s.isPowerMode());
        assertTrue(g1.isEatable());
        assertTrue(g2.isEatable());
    }

    @Test
    public void tickPower_countsDownAndTurnsOffEatableAtZero() {
        GameState s = new GameState(grid5x5Open(), 2, 2);
        Ghost g = new Ghost(1, 1);
        s.addGhost(g);

        s.activatePower(2);
        assertTrue(s.isPowerMode());
        assertTrue(g.isEatable());

        s.tickPower(); // left 1
        assertTrue(s.isPowerMode());
        assertTrue(g.isEatable());

        s.tickPower(); // left 0
        assertFalse(s.isPowerMode());
        assertFalse(g.isEatable());

        // extra ticks do nothing
        s.tickPower();
        assertFalse(s.isPowerMode());
        assertFalse(g.isEatable());
    }

    @Test
    public void activatePower_extendsByMaxNotOverrideWithSmaller() {
        GameState s = new GameState(grid5x5Open(), 2, 2);
        s.activatePower(5);
        s.tickPower(); // now 4

        s.activatePower(2); // should keep 4 (max)
        // Tick 4 times -> should turn off
        for (int i = 0; i < 4; i++) s.tickPower();
        assertFalse(s.isPowerMode());
    }

    // ---------- collisions ----------

    @Test
    public void collision_whenGhostEatable_addsScoreAndRespawnsGhost() {
        GameState s = new GameState(grid5x5Open(), 2, 2);
        Ghost g = new Ghost(1, 1);
        s.addGhost(g);

        int spawnX = g.x();
        int spawnY = g.y();

        // move ghost away then make eatable
        g.setPos(2, 1);
        s.activatePower(3);
        assertTrue(g.isEatable());

        s.onPacmanGhostCollision(g);

        assertEquals(200, s.getScore());
        // respawnGhost should bring back to spawn
        assertEquals(spawnX, g.x());
        assertEquals(spawnY, g.y());
        assertEquals(Direction.STAY, g.dir());
    }

    @Test
    public void collision_whenGhostNotEatable_loseLifeAndResetPositions() {
        Tile[][] g = grid5x5Open();
        GameState s = new GameState(g, 2, 2);

        Ghost ghost = new Ghost(1, 1);
        s.addGhost(ghost);

        // move pacman and ghost away from spawn
        s.pacX = 3; s.pacY = 3;
        ghost.setPos(2, 1);
        ghost.setDir(Direction.RIGHT);
        ghost.setEatable(false);

        int livesBefore = s.getLives();
        s.onPacmanGhostCollision(ghost);

        assertEquals(livesBefore - 1, s.getLives());
        assertFalse(s.isDone());

        // resetPositions should return pacman to spawn (2,2)
        assertEquals(2, s.getPacmanX());
        assertEquals(2, s.getPacmanY());

        // and ghost to its spawn (1,1), dir stay
        assertEquals(1, ghost.x());
        assertEquals(1, ghost.y());
        assertEquals(Direction.STAY, ghost.dir());
    }

    @Test
    public void collision_whenLastLife_gameDoneAndNoResetNeeded() {
        GameState s = new GameState(grid5x5Open(), 2, 2);
        Ghost ghost = new Ghost(1, 1);
        s.addGhost(ghost);

        // drop lives to 1
        s.loseLife();
        s.loseLife();
        assertEquals(1, s.getLives());
        assertFalse(s.isDone());

        // move pacman away to see if it resets or not after game over
        s.pacX = 3; s.pacY = 3;

        s.onPacmanGhostCollision(ghost);

        assertEquals(0, s.getLives());
        assertTrue(s.isDone());
        // after done, resetPositions() is guarded; pacman might remain where it was set
        // (we assert only the "done" condition which is the correct game rule)
    }

    // ---------- reset / respawn behavior ----------

    @Test
    public void resetPositions_putsEntitiesBackToSpawns_andGhostEatableMatchesPower() {
        GameState s = new GameState(grid5x5Open(), 2, 2);
        Ghost g1 = new Ghost(1, 1);
        Ghost g2 = new Ghost(3, 3);
        s.addGhost(g1);
        s.addGhost(g2);

        // set power ON, so after reset ghosts should be eatable
        s.activatePower(5);

        // move stuff
        s.pacX = 1; s.pacY = 2;
        g1.setPos(2, 2);
        g2.setPos(2, 3);
        g1.setEatable(false);
        g2.setEatable(false);
        g1.setDir(Direction.UP);
        g2.setDir(Direction.LEFT);

        s.resetPositions();

        assertEquals(2, s.getPacmanX());
        assertEquals(2, s.getPacmanY());

        assertEquals(1, g1.x());
        assertEquals(1, g1.y());
        assertEquals(3, g2.x());
        assertEquals(3, g2.y());

        assertTrue(g1.isEatable());
        assertTrue(g2.isEatable());
        assertEquals(Direction.STAY, g1.dir());
        assertEquals(Direction.STAY, g2.dir());
    }

    @Test
    public void respawnGhost_unregisteredGhost_doesNotThrow_setsDirStay_andEatableMatchesPower() {
        GameState s = new GameState(grid5x5Open(), 2, 2);

        Ghost g = new Ghost(2, 1); // not added
        s.activatePower(3);

        g.setDir(Direction.RIGHT);
        g.setEatable(false);

        assertDoesNotThrow(() -> s.respawnGhost(g));
        assertEquals(Direction.STAY, g.dir());
        assertTrue(g.isEatable());
    }
}
