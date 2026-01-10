package assignments.Ex3.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GhostMovement {
    private final Random rnd = new Random();

    public Direction chooseNext(Ghost g, GameState s) {
        List<Direction> legal = new ArrayList<>();

        for (Direction d : new Direction[]{Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT}) {
            int nx = g.x() + d.dx;
            int ny = g.y() + d.dy;
            if (!s.isWall(nx, ny)) legal.add(d);
        }

        if (legal.isEmpty()) return Direction.STAY;

        // Prefer continuing direction
        if (legal.contains(g.dir())) return g.dir();

        // Avoid reverse if possible
        Direction rev = reverse(g.dir());
        if (rev != Direction.STAY && legal.size() > 1) legal.remove(rev);

        return legal.get(rnd.nextInt(legal.size()));
    }

    private Direction reverse(Direction d) {
        return switch (d) {
            case UP -> Direction.DOWN;
            case DOWN -> Direction.UP;
            case LEFT -> Direction.RIGHT;
            case RIGHT -> Direction.LEFT;
            default -> Direction.STAY;
        };
    }
}
