package assignments.Ex3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class Map2DTest {

    private Map2D map3x3() {
        int[][] data = {
                {0, 0, 0},
                {0, -1, 0},
                {0, 0, 0}
        };
        return new Map(data);
    }

    @Test
    public void width_height_correct() {
        Map2D m = map3x3();
        assertEquals(3, m.getWidth());
        assertEquals(3, m.getHeight());
    }

    @Test
    public void getPixel_setPixel_by_xy() {
        Map2D m = map3x3();
        assertEquals(-1, m.getPixel(1, 1));
        m.setPixel(0, 0, 7);
        assertEquals(7, m.getPixel(0, 0));
    }

    @Test
    public void getPixel_setPixel_by_Pixel2D() {
        Map2D m = map3x3();
        Pixel2D p = new Index2D(2, 2);
        assertEquals(0, m.getPixel(p));
        m.setPixel(p, 9);
        assertEquals(9, m.getPixel(2, 2));
    }

    @Test
    public void isInside_bounds() {
        Map2D m = map3x3();
        assertTrue(m.isInside(new Index2D(0, 0)));
        assertTrue(m.isInside(new Index2D(2, 2)));
        assertFalse(m.isInside(new Index2D(-1, 0)));
        assertFalse(m.isInside(new Index2D(3, 1)));
        assertFalse(m.isInside(null));
    }

    @Test
    public void getMap_is_deep_copy() {
        Map2D m = map3x3();
        int[][] copy = m.getMap();
        copy[0][0] = 12345;
        assertNotEquals(copy[0][0], m.getPixel(0, 0));
    }

    @Test
    public void fill_connected_component_counts_correctly() {
        // map3x3 has center -1, all others 0
        Map2D m = map3x3();
        int filled = m.fill(new Index2D(0, 0), 5);
        assertEquals(8, filled);              // all zeros except the wall
        assertEquals(5, m.getPixel(0, 0));
        assertEquals(-1, m.getPixel(1, 1));   // wall unchanged
    }

    @Test
    public void shortestPath_basic_non_null() {
        Map2D m = map3x3();
        m.setCyclic(false);
        Pixel2D[] path = m.shortestPath(new Index2D(0,0), new Index2D(2,2), -1);
        assertNotNull(path);
        assertEquals(new Index2D(0,0), path[0]);
        assertEquals(new Index2D(2,2), path[path.length-1]);
    }

    @Test
    public void allDistance_marks_unreachable_as_minus_one() {
        Map2D m = map3x3();
        m.setCyclic(false);
        Map2D dist = m.allDistance(new Index2D(0,0), -1);

        assertEquals(0, dist.getPixel(0,0));
        assertEquals(-1, dist.getPixel(1,1)); // obstacle itself remains -1
        assertTrue(dist.getPixel(2,2) >= 0);  // reachable
    }
}
