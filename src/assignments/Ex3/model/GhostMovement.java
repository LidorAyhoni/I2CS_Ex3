package assignments.Ex3.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GhostMovement {
    private final Random rnd = new Random();

    public Direction chooseNext(Ghost g, boolean[][] passable) {
        List<Direction> legal = new ArrayList<>();
        for (Direction d : new Direction[]{Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT}) {
            int nx = g.x() + d.dx, ny = g.y() + d.dy;
            if (inBounds(passable, nx, ny) && passable[nx][ny]) legal.add(d);
        }
        if (legal.isEmpty()) return Direction.STAY;

        // Bias: keep direction if still legal
        if (g.dir() != Direction.STAY) {
            for (Direction d : legal) {
                if (d == g.dir()) return d;
            }
        }

        // Avoid reverse if possible
        Direction rev = reverse(g.dir());
        if (rev != Direction.STAY && legal.size() > 1) {
            legal.remove(rev);
        }

        return legal.get(rnd.nextInt(legal.size()));
    }

    private static boolean inBounds(boolean[][] p, int x, int y) {
        return x >= 0 && y >= 0 && x < p.length && y < p[0].length;
    }

    private static Direction reverse(Direction d) {
        return switch (d) {
            case UP -> Direction.DOWN;
            case DOWN -> Direction.UP;
            case LEFT -> Direction.RIGHT;
            case RIGHT -> Direction.LEFT;
            default -> Direction.STAY;
        };
    }
}
