package assignments.Ex3;

import assignments.Ex3.levels.LevelLoader;
import assignments.Ex3.model.GameState;
import assignments.Ex3.model.Ghost;
import assignments.Ex3.model.Tile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LevelLoaderTest {

    @Test
    public void level0_isValid() {
        assertLevelValid(LevelLoader.level0(), 0);
    }

    @Test
    public void level1_isValid() {
        assertLevelValid(LevelLoader.level1(), 1);
    }

    @Test
    public void level2_isValid() {
        assertLevelValid(LevelLoader.level2(), 2);
    }

    // -------- helpers --------

    private void assertLevelValid(GameState s, int levelIdx) {
        assertNotNull(s, "level " + levelIdx + ": GameState is null");
        assertNotNull(s.grid, "level " + levelIdx + ": grid is null");

        // dimensions
        assertTrue(s.w > 0 && s.h > 0, "level " + levelIdx + ": invalid dimensions");
        assertEquals(s.w, s.grid.length, "level " + levelIdx + ": grid width mismatch");

        for (int x = 0; x < s.w; x++) {
            assertNotNull(s.grid[x], "level " + levelIdx + ": grid column " + x + " is null");
            assertEquals(s.h, s.grid[x].length, "level " + levelIdx + ": grid height mismatch at x=" + x);
        }

        // no null tiles + count dots/power
        int foodCount = 0;
        for (int x = 0; x < s.w; x++) {
            for (int y = 0; y < s.h; y++) {
                Tile t = s.grid[x][y];
                assertNotNull(t, "level " + levelIdx + ": null tile at (" + x + "," + y + ")");
                if (t == Tile.DOT || t == Tile.POWER) foodCount++;
            }
        }
        assertTrue(foodCount > 0, "level " + levelIdx + ": no DOT/POWER found (no goal)");

        // pacman spawn valid
        assertTrue(s.inBounds(s.getPacmanX(), s.getPacmanY()),
                "level " + levelIdx + ": pacman spawn out of bounds");
        assertFalse(s.isWall(s.getPacmanX(), s.getPacmanY()),
                "level " + levelIdx + ": pacman starts on a wall");

        // ghosts valid
        for (Ghost g : s.getGhosts()) {
            assertTrue(s.inBounds(g.x(), g.y()),
                    "level " + levelIdx + ": ghost out of bounds at (" + g.x() + "," + g.y() + ")");
            assertFalse(s.isWall(g.x(), g.y()),
                    "level " + levelIdx + ": ghost on wall at (" + g.x() + "," + g.y() + ")");
        }
    }
}
