package assignments.Ex3.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Chooses a legal move for a ghost.
 * Rules:
 * - never goes into walls/out of bounds
 * - prefers to keep direction if possible
 * - avoids reverse if there are other options
 */
public class GhostMovement {

    private final Random rnd = new Random();

    public Direction chooseNext(Ghost g, GameState s) {
        if (g == null) return Direction.STAY;

        List<Direction> legal = new ArrayList<>(4);

        for (Direction d : new Direction[]{Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT}) {
            int nx = g.x() + d.dx;
            int ny = g.y() + d.dy;
            if (!s.isWall(nx, ny)) {
                legal.add(d);
            }
        }

        if (legal.isEmpty()) return Direction.STAY;

        // Prefer continuing direction if still legal
        if (g.dir() != null && legal.contains(g.dir())) return g.dir();

        // Avoid reverse if possible
        Direction rev = reverse(g.dir());
        if (rev != Direction.STAY && legal.size() > 1) {
            legal.remove(rev);
        }

        return legal.get(rnd.nextInt(legal.size()));
    }

    private static Direction reverse(Direction d) {
        if (d == null) return Direction.STAY;
        return switch (d) {
            case UP -> Direction.DOWN;
            case DOWN -> Direction.UP;
            case LEFT -> Direction.RIGHT;
            case RIGHT -> Direction.LEFT;
            default -> Direction.STAY;
        };
    }
}
