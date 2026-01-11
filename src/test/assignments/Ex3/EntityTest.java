package assignments.Ex3;

import assignments.Ex3.model.Direction;
import assignments.Ex3.model.Ghost;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EntityTest {

    @Test
    public void setPos_updatesCoordinates() {
        Ghost g = new Ghost(1, 1);

        g.setPos(3, 4);

        assertEquals(3, g.x());
        assertEquals(4, g.y());
    }

    @Test
    public void setDir_updatesDirection() {
        Ghost g = new Ghost(1, 1);

        g.setDir(Direction.LEFT);
        assertEquals(Direction.LEFT, g.dir());

        g.setDir(Direction.STAY);
        assertEquals(Direction.STAY, g.dir());
    }
}
