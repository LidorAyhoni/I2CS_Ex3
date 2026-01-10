package assignments.Ex3.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Ghost movement: mostly random, with a slight bias to "try to eat Pac-Man".
 *
 * Rules:
 * - never goes into walls/out of bounds (relies on s.isWall)
 * - prefers to keep direction if possible
 * - avoids reverse if there are other options
 * - adds a small bias toward Pac-Man (classic Pac-Man feel, not full chasing/BFS)
 */
public class GhostMovement {

    private final Random rnd = new Random();

    // Tuning knobs (feel free to tweak)
    private static final int WEIGHT_FORWARD = 6;   // strong preference to keep direction
    private static final int WEIGHT_TOWARD_PAC = 3; // mild bias toward Pac-Man
    private static final int WEIGHT_OTHER = 1;     // base weight for any legal move

    public Direction chooseNext(Ghost g, GameState s) {
        if (g == null) return Direction.STAY;

        // Collect legal moves
        List<Direction> legal = new ArrayList<>(4);
        for (Direction d : new Direction[]{Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT}) {
            int nx = g.x() + d.dx;
            int ny = g.y() + d.dy;
            if (!s.isWall(nx, ny)) {
                legal.add(d);
            }
        }
        if (legal.isEmpty()) return Direction.STAY;

        // If only one option, take it
        if (legal.size() == 1) return legal.get(0);

        // Avoid reverse if possible
        Direction current = g.dir();
        Direction rev = reverse(current);
        if (rev != Direction.STAY && legal.size() > 1) {
            legal.remove(rev);
            // if we removed and now only one left
            if (legal.size() == 1) return legal.get(0);
        }

        // Weighted random selection
        Direction bestTowardPac = bestStepTowardPacman(g, s, legal);

        int total = 0;
        int[] weights = new int[legal.size()];

        for (int i = 0; i < legal.size(); i++) {
            Direction d = legal.get(i);

            int w = WEIGHT_OTHER;

            // prefer continuing direction
            if (current != null && d == current) w += WEIGHT_FORWARD;

            // mild bias toward pacman
            if (bestTowardPac != null && d == bestTowardPac) w += WEIGHT_TOWARD_PAC;

            weights[i] = w;
            total += w;
        }

        int r = rnd.nextInt(total);
        for (int i = 0; i < legal.size(); i++) {
            r -= weights[i];
            if (r < 0) return legal.get(i);
        }

        // fallback
        return legal.get(rnd.nextInt(legal.size()));
    }

    /**
     * Chooses which one-step move (from legal list) reduces Manhattan distance to Pac-Man the most.
     * Returns null if we can't evaluate.
     */
    private Direction bestStepTowardPacman(Ghost g, GameState s, List<Direction> legal) {
        // We assume GameState has pacX/pacY fields (your renderer uses s.pacX/s.pacY)
        int px = s.pacX;
        int py = s.pacY;

        int gx = g.x();
        int gy = g.y();

        int bestDist = Integer.MAX_VALUE;
        Direction best = null;

        for (Direction d : legal) {
            int nx = gx + d.dx;
            int ny = gy + d.dy;
            int dist = Math.abs(nx - px) + Math.abs(ny - py);
            if (dist < bestDist) {
                bestDist = dist;
                best = d;
            }
        }
        return best;
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
