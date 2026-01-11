package assignments.Ex3;

import assignments.Ex3.model.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DirectionTest {

    @Test
    public void direction_vectors_areCorrect() {
        assertEquals(0, Direction.UP.dx);
        assertEquals(1, Direction.UP.dy);

        assertEquals(0, Direction.DOWN.dx);
        assertEquals(-1, Direction.DOWN.dy);

        assertEquals(-1, Direction.LEFT.dx);
        assertEquals(0, Direction.LEFT.dy);

        assertEquals(1, Direction.RIGHT.dx);
        assertEquals(0, Direction.RIGHT.dy);
    }

    @Test
    public void stay_hasZeroMovement() {
        assertEquals(0, Direction.STAY.dx);
        assertEquals(0, Direction.STAY.dy);
    }
    @Test
    public void directionCount_isExpected() {
        assertEquals(5, Direction.values().length);
    }

    @Test
    public void directions_areUnique() {
        assertNotEquals(Direction.UP, Direction.DOWN);
        assertNotEquals(Direction.LEFT, Direction.RIGHT);
    }
}
