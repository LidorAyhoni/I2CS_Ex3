package assignments.Ex3;

import assignments.Ex3.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GhostMovementTest {

    private Tile[][] open5x5() {
        Tile[][] g = new Tile[5][5];
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) g[x][y] = Tile.EMPTY;
        }
        for (int i = 0; i < 5; i++) {
            g[0][i] = Tile.WALL;
            g[4][i] = Tile.WALL;
            g[i][0] = Tile.WALL;
            g[i][4] = Tile.WALL;
        }
        return g;
    }

    @Test
    public void chooseNext_returnsNonNullDirection() {
        GameState s = new GameState(open5x5(), 2, 2);
        Ghost g = new Ghost(1, 1);
        s.addGhost(g);

        GhostMovement gm = new GhostMovement();
        Direction d = gm.chooseNext(g, s);

        assertNotNull(d);
    }

    @Test
    public void chooseNext_returnsValidEnumValue() {
        GameState s = new GameState(open5x5(), 2, 2);
        Ghost g = new Ghost(1, 1);
        s.addGhost(g);

        GhostMovement gm = new GhostMovement();
        Direction d = gm.chooseNext(g, s);

        // if it's not null and is Direction, it's in the enum by definition,
        // but we keep a very clear assertion:
        assertTrue(java.util.EnumSet.allOf(Direction.class).contains(d));
    }

    @Test
    public void chooseNext_inOpenArea_isNotA_wallMove() {
        GameState s = new GameState(open5x5(), 2, 2);
        Ghost g = new Ghost(2, 2);
        s.addGhost(g);

        GhostMovement gm = new GhostMovement();
        Direction d = gm.chooseNext(g, s);

        int nx = g.x() + d.dx;
        int ny = g.y() + d.dy;

        assertFalse(s.isWall(nx, ny));
    }
}
