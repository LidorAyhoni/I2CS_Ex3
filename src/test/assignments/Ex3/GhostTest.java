package assignments.Ex3;

import assignments.Ex3.model.Ghost;
import assignments.Ex3.model.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GhostTest {

    @Test
    public void ctor_setsInitialPosition() {
        Ghost g = new Ghost(3, 4);
        assertEquals(3, g.x());
        assertEquals(4, g.y());
    }

    @Test
    public void eatable_defaultFalse_thenCanToggle() {
        Ghost g = new Ghost(1, 1);

        assertFalse(g.isEatable());
        g.setEatable(true);
        assertTrue(g.isEatable());
        g.setEatable(false);
        assertFalse(g.isEatable());
    }

    @Test
    public void canMovePosition_viaEntityAPI() {
        Ghost g = new Ghost(1, 1);

        g.setPos(2, 3);
        assertEquals(2, g.x());
        assertEquals(3, g.y());
    }

    @Test
    public void canChangeDirection_viaEntityAPI() {
        Ghost g = new Ghost(1, 1);

        g.setDir(Direction.UP);
        assertEquals(Direction.UP, g.dir());

        g.setDir(Direction.STAY);
        assertEquals(Direction.STAY, g.dir());
    }
}
