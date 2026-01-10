package assignments.Ex3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class Index2DTest {

    @Test
    public void constructor_sets_coordinates_correctly() {
        Index2D p = new Index2D(3, 5);
        assertEquals(3, p.getX());
        assertEquals(5, p.getY());
    }

    @Test
    public void equals_same_coordinates_true() {
        Index2D a = new Index2D(2, 4);
        Index2D b = new Index2D(2, 4);
        assertEquals(a, b);
    }

    @Test
    public void equals_different_coordinates_false() {
        Index2D a = new Index2D(2, 4);
        Index2D b = new Index2D(3, 4);
        assertNotEquals(a, b);
    }

    @Test
    public void copy_constructor_creates_equal_but_distinct_object() {
        Index2D a = new Index2D(7, 8);
        Index2D b = new Index2D(a);

        assertEquals(a, b);
        assertNotSame(a, b);
    }

    @Test
    public void toString_format_is_correct() {
        Index2D p = new Index2D(1, 9);
        assertEquals("1,9", p.toString());
    }

    @Test
    public void hashCode_equal_objects_same_hash() {
        Index2D a = new Index2D(4, 6);
        Index2D b = new Index2D(4, 6);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
