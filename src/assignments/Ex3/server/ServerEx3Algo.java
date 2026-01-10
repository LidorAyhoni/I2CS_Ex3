package assignments.Ex3.server;

import assignments.Ex3.model.*;

import java.util.ArrayDeque;
import java.util.ArrayList;

/**
 * Server-side Pac-Man decision engine inspired by the original {@code Ex3Algo},
 * but operating directly on the project's {@link GameState} model (no course
 * {@code PacmanGame}/{@code GhostCL} wrappers).
 *
 * <p><b>High-level policy (in order):</b></p>
 * <ol>
 *   <li><b>Escape:</b> If a non-eatable (danger) ghost is within a configurable range,
 *       choose a move that increases safety (maximize distance to danger) while preferring open tiles.</li>
 *   <li><b>Eat fast:</b> Otherwise, use BFS to reach the nearest DOT or (sometimes) POWER as a shortest path.</li>
 *   <li><b>Stability:</b> Apply loop-breaking, "stuck" recovery, and "no-reverse" preference to avoid jittering.</li>
 * </ol>
 *
 * <p><b>Notes:</b> This class is intentionally stateful. It keeps lightweight memory across ticks
 * (last direction, recent positions, tick counter) to improve movement smoothness and reduce loops.</p>
 */
public class ServerEx3Algo {

    // ======= BEHAVIOR KNOBS =======

    /** If a danger ghost is within this BFS distance, switch to escape behavior. */
    private static final int DANGER_TRIGGER = 7;

    /** Strong avoidance radius; moves that lead to distance <= this are rejected when possible. */
    private static final int HARD_AVOID = 2;

    /** Always consider taking POWER if it is extremely close (distance <= this). */
    private static final int POWER_TAKE_IF_DIST_LE = 2;

    /** Prefer POWER over DOT when danger is relatively near (distance <= this). */
    private static final int POWER_PREFER_IF_DANGER_LE = 5;

    /**
     * Opening phase duration (in ticks) in which the agent tries to "get moving"
     * using a simple deterministic rule before the full policy kicks in.
     */
    private static final int OPENING_STEPS = 25;

    /**
     * A "power lock" period (in ticks) at the beginning where POWER tiles can be treated as blocked.
     * This mimics the spirit of the original algorithm's early-game behavior.
     */
    private static final int NO_POWER_FIRST_TICKS = 50;

    /** Number of recent positions remembered for loop detection. */
    private static final int LOOP_MEM = 12;

    // ======= INTERNAL MEMORY (STATEFUL AI) =======

    /** Monotonic tick counter used for opening/locking heuristics. */
    private int tick = 0;

    /** Previous Pac-Man position used to detect "stuck" situations. */
    private int lastX = Integer.MIN_VALUE, lastY = Integer.MIN_VALUE;

    /** Previously chosen direction used for smoothing and no-reverse preference. */
    private Direction lastDir = Direction.STAY;

    /** Counts consecutive ticks in which Pac-Man did not change position. */
    private int stuckCount = 0;

    /** Recent positions encoded as a single long, used to detect loops/backtracking. */
    private final ArrayDeque<Long> lastPositions = new ArrayDeque<>();

    /**
     * Computes the next direction for Pac-Man based on the current {@link GameState}.
     *
     * <p>This method implements the full decision pipeline:
     * opening move → escape/eat-fast selection → loop breaking → stuck recovery → no-reverse smoothing.</p>
     *
     * @param s current server-side game state (grid, ghosts, pacman position, power mode, etc.)
     * @return the chosen direction for the next tick (may be {@link Direction#STAY} if no legal move exists)
     */
    public Direction nextMove(GameState s) {
        tick++;

        int px = s.getPacmanX();
        int py = s.getPacmanY();

        if (px == lastX && py == lastY) stuckCount++;
        else stuckCount = 0;

        pushPos(px, py);

        boolean powerMode = s.isPowerMode();

        // When blockPowerTiles=true, POWER tiles are treated as non-passable (policy knob).
        boolean blockPowerTiles = powerMode || (tick <= NO_POWER_FIRST_TICKS);

        // Opening: try to start moving deterministically.
        if (tick <= OPENING_STEPS) {
            Direction op = openingMove(px, py, s, blockPowerTiles);
            if (op != Direction.STAY) return remember(px, py, op);
        }

        Direction chosen;

        if (powerMode) {
            // While powered: prioritize DOT collection (POWER is typically irrelevant while active).
            chosen = bfsToNearestTileSmart(px, py, s, Tile.DOT, blockPowerTiles);
            if (chosen == Direction.STAY) chosen = anyLegalMove(px, py, s, blockPowerTiles);
        } else {
            int curThreat = minBfsDistToDangerGhost(px, py, s, blockPowerTiles);

            if (curThreat != Integer.MAX_VALUE && curThreat <= DANGER_TRIGGER) {
                chosen = escapeMove(px, py, s, blockPowerTiles, curThreat);
            } else {
                chosen = eatFastMove(px, py, s, blockPowerTiles, curThreat);
            }
        }

        if (chosen == Direction.STAY) chosen = anyLegalMove(px, py, s, blockPowerTiles);

        // Loop + stuck recovery.
        chosen = breakLoopIfNeeded(px, py, s, chosen, blockPowerTiles);
        if (stuckCount >= 3) chosen = forceDifferentLegal(px, py, s, chosen, blockPowerTiles);

        // Avoid immediate reversing when a safe alternative exists.
        chosen = applyNoReverse(px, py, s, chosen, blockPowerTiles);

        return remember(px, py, chosen);
    }

    // ===================== ESCAPE =====================

    /**
     * Escape behavior: selects a move that maximizes safety while preferring open tiles.
     *
     * <p>Scoring factors include:</p>
     * <ul>
     *   <li>Distance to nearest danger ghost (higher is better)</li>
     *   <li>Number of exits from the candidate tile (more open is better)</li>
     *   <li>Preference for continuing in the same direction (smoother motion)</li>
     *   <li>Penalty for stepping into recently visited positions (loop avoidance)</li>
     * </ul>
     *
     * <p>Uses two passes:
     * pass 1 enforces {@link #HARD_AVOID} if possible; pass 2 relaxes constraints when needed.</p>
     */
    private Direction escapeMove(int px, int py, GameState s, boolean blockPowerTiles, int curThreat) {
        Direction[] dirs = {Direction.UP, Direction.LEFT, Direction.DOWN, Direction.RIGHT};

        Direction bestDir = Direction.STAY;
        int bestScore = Integer.MIN_VALUE;

        // Pass 1: enforce hard avoid if possible.
        for (Direction d : dirs) {
            int nx = px + d.dx;
            int ny = py + d.dy;
            if (!passable(nx, ny, s, blockPowerTiles)) continue;

            int nt = minBfsDistToDangerGhost(nx, ny, s, blockPowerTiles);
            if (nt <= HARD_AVOID && curThreat > HARD_AVOID) continue;

            int score =
                    safeVal(nt) * 2000 +
                            countExits(nx, ny, s, blockPowerTiles) * 120 +
                            (d == lastDir ? 40 : 0) +
                            (isRecentPos(nx, ny) ? -300 : 0);

            if (score > bestScore) {
                bestScore = score;
                bestDir = d;
            }
        }
        if (bestDir != Direction.STAY) return bestDir;

        // Pass 2: relax constraints.
        for (Direction d : dirs) {
            int nx = px + d.dx;
            int ny = py + d.dy;
            if (!passable(nx, ny, s, blockPowerTiles)) continue;

            int nt = minBfsDistToDangerGhost(nx, ny, s, blockPowerTiles);
            int score =
                    safeVal(nt) * 2000 +
                            countExits(nx, ny, s, blockPowerTiles) * 120 +
                            (d == lastDir ? 40 : 0) +
                            (isRecentPos(nx, ny) ? -300 : 0);

            if (score > bestScore) {
                bestScore = score;
                bestDir = d;
            }
        }
        return bestDir;
    }

    /**
     * Normalizes a threat-distance value into a bounded "safety score".
     * {@code Integer.MAX_VALUE} (unreachable) is treated as very safe.
     */
    private int safeVal(int dist) {
        if (dist == Integer.MAX_VALUE) return 50;
        return Math.min(dist, 50);
    }

    // ===================== EAT FAST =====================

    /**
     * Dot/power collection behavior: choose between DOT and POWER based on distance and danger.
     *
     * <p>Heuristics:</p>
     * <ul>
     *   <li>If DOT unavailable → go to POWER (if allowed).</li>
     *   <li>If POWER unavailable → go to DOT.</li>
     *   <li>Prefer POWER when danger is near, POWER is extremely close, or POWER is clearly better.</li>
     * </ul>
     */
    private Direction eatFastMove(int px, int py, GameState s, boolean blockPowerTiles, int curThreat) {
        int dotDist = nearestTargetDist(px, py, s, Tile.DOT, blockPowerTiles);
        int powDist = nearestTargetDist(px, py, s, Tile.POWER, blockPowerTiles);

        boolean dangerNear = (curThreat != Integer.MAX_VALUE && curThreat <= POWER_PREFER_IF_DANGER_LE);
        boolean powerVeryClose = (powDist != Integer.MAX_VALUE && powDist <= POWER_TAKE_IF_DIST_LE);

        if (dotDist == Integer.MAX_VALUE && powDist == Integer.MAX_VALUE) return Direction.STAY;
        if (dotDist == Integer.MAX_VALUE) return bfsToNearestTileSmart(px, py, s, Tile.POWER, blockPowerTiles);
        if (powDist == Integer.MAX_VALUE) return bfsToNearestTileSmart(px, py, s, Tile.DOT, blockPowerTiles);

        boolean shouldTakePower = dangerNear || powerVeryClose || (powDist + 2 < dotDist);
        Tile target = shouldTakePower ? Tile.POWER : Tile.DOT;

        Direction dir = bfsToNearestTileSmart(px, py, s, target, blockPowerTiles);
        if (dir != Direction.STAY) return dir;

        Tile other = (target == Tile.DOT) ? Tile.POWER : Tile.DOT;
        return bfsToNearestTileSmart(px, py, s, other, blockPowerTiles);
    }

    // ===================== BFS (SMART TIE BREAK) =====================

    /**
     * Finds the first step along a shortest path from (px,py) to the nearest tile of {@code target}.
     *
     * <p>If multiple targets exist at the same minimal distance, a "smart" tie-break is applied:
     * prefer the candidate that maximizes safety (distance to danger ghosts), openness (exit count),
     * continuity (same direction), and avoids recently visited cells.</p>
     *
     * @param target tile type to seek (DOT/POWER)
     * @return the direction to take from the current position; {@link Direction#STAY} if no path/target exists
     */
    private Direction bfsToNearestTileSmart(int px, int py, GameState s, Tile target,
                                            boolean blockPowerTiles) {
        if (blockPowerTiles && target == Tile.POWER) return Direction.STAY;

        if (!boardHasTile(s, target)) return Direction.STAY;

        int w = s.w, h = s.h;

        boolean[][] vis = new boolean[w][h];
        Direction[][] firstDir = new Direction[w][h];
        int[][] dist = new int[w][h];

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                dist[x][y] = Integer.MAX_VALUE;
                firstDir[x][y] = Direction.STAY;
            }
        }

        ArrayDeque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{px, py});
        vis[px][py] = true;
        dist[px][py] = 0;

        Direction[] dirs = {Direction.UP, Direction.LEFT, Direction.DOWN, Direction.RIGHT};

        int foundDist = Integer.MAX_VALUE;
        ArrayList<int[]> candidates = new ArrayList<>();

        while (!q.isEmpty()) {
            int[] cur = q.poll();
            int cx = cur[0], cy = cur[1];
            int cd = dist[cx][cy];
            if (cd > foundDist) break;

            for (Direction d : dirs) {
                int nx = cx + d.dx;
                int ny = cy + d.dy;

                if (!s.inBounds(nx, ny)) continue;
                if (vis[nx][ny]) continue;
                if (!passable(nx, ny, s, blockPowerTiles)) continue;

                vis[nx][ny] = true;
                dist[nx][ny] = cd + 1;
                firstDir[nx][ny] = (cx == px && cy == py) ? d : firstDir[cx][cy];

                if (s.grid[nx][ny] == target) {
                    if (dist[nx][ny] < foundDist) {
                        foundDist = dist[nx][ny];
                        candidates.clear();
                        candidates.add(new int[]{nx, ny});
                    } else if (dist[nx][ny] == foundDist) {
                        candidates.add(new int[]{nx, ny});
                    }
                }

                q.add(new int[]{nx, ny});
            }
        }

        if (candidates.isEmpty()) return Direction.STAY;

        Direction bestDir = Direction.STAY;
        int bestScore = Integer.MIN_VALUE;

        for (int[] c : candidates) {
            int x = c[0], y = c[1];
            Direction dir = firstDir[x][y];
            if (dir == Direction.STAY) continue;

            int threat = minBfsDistToDangerGhost(x, y, s, blockPowerTiles);
            int exits = countExits(x, y, s, blockPowerTiles);

            int score =
                    safeVal(threat) * 1000 +
                            exits * 120 +
                            (dir == lastDir ? 40 : 0) +
                            (isRecentPos(x, y) ? -400 : 0);

            if (score > bestScore) {
                bestScore = score;
                bestDir = dir;
            }
        }

        return bestDir;
    }

    /**
     * Computes the BFS distance to the closest {@code target} tile.
     *
     * <p>Used for deciding DOT vs POWER (not for choosing the final direction).</p>
     *
     * @return minimal distance, or {@link Integer#MAX_VALUE} if unreachable/not present
     */
    private int nearestTargetDist(int sx, int sy, GameState s, Tile target, boolean blockPowerTiles) {
        if (blockPowerTiles && target == Tile.POWER) return Integer.MAX_VALUE;

        int w = s.w, h = s.h;
        boolean[][] vis = new boolean[w][h];
        ArrayDeque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{sx, sy, 0});
        vis[sx][sy] = true;

        Direction[] dirs = {Direction.UP, Direction.LEFT, Direction.DOWN, Direction.RIGHT};

        while (!q.isEmpty()) {
            int[] cur = q.poll();
            int x = cur[0], y = cur[1], d0 = cur[2];

            if (s.grid[x][y] == target && !(x == sx && y == sy)) return d0;

            for (Direction d : dirs) {
                int nx = x + d.dx;
                int ny = y + d.dy;

                if (!s.inBounds(nx, ny)) continue;
                if (vis[nx][ny]) continue;
                if (!passable(nx, ny, s, blockPowerTiles)) continue;

                vis[nx][ny] = true;
                q.add(new int[]{nx, ny, d0 + 1});
            }
        }
        return Integer.MAX_VALUE;
    }

    // ===================== GHOST DIST =====================

    /**
     * Returns the minimal BFS distance from (px,py) to any "danger" ghost (non-eatable).
     * Eatable ghosts are ignored since they do not represent immediate danger.
     */
    private int minBfsDistToDangerGhost(int px, int py, GameState s, boolean blockPowerTiles) {
        int best = Integer.MAX_VALUE;
        for (Ghost g : s.getGhosts()) {
            if (g == null) continue;
            if (g.isEatable()) continue; // danger = non-eatable
            int d = bfsDist(px, py, g.x(), g.y(), s, blockPowerTiles);
            best = Math.min(best, d);
        }
        return best;
    }

    /**
     * Standard BFS shortest path distance on the grid.
     *
     * <p>This variant is used for safety evaluation and ghost distance computations.</p>
     *
     * @return shortest path length, or {@link Integer#MAX_VALUE} if unreachable
     */
    private int bfsDist(int sx, int sy, int tx, int ty, GameState s, boolean blockPowerTiles) {
        if (sx == tx && sy == ty) return 0;

        int w = s.w, h = s.h;
        boolean[][] vis = new boolean[w][h];
        ArrayDeque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{sx, sy, 0});
        vis[sx][sy] = true;

        Direction[] dirs = {Direction.UP, Direction.LEFT, Direction.DOWN, Direction.RIGHT};

        while (!q.isEmpty()) {
            int[] cur = q.poll();
            int x = cur[0], y = cur[1], dist = cur[2];

            for (Direction d : dirs) {
                int nx = x + d.dx;
                int ny = y + d.dy;

                if (!s.inBounds(nx, ny)) continue;
                if (vis[nx][ny]) continue;
                if (!passableGhostDist(nx, ny, s, blockPowerTiles)) continue;

                if (nx == tx && ny == ty) return dist + 1;

                vis[nx][ny] = true;
                q.add(new int[]{nx, ny, dist + 1});
            }
        }
        return Integer.MAX_VALUE;
    }

    /**
     * Passability rule for "ghost distance" BFS:
     * ignores ghost occupancy and blocks only walls (and optionally POWER tiles).
     */
    private boolean passableGhostDist(int x, int y, GameState s, boolean blockPowerTiles) {
        if (s.isWall(x, y)) return false;
        if (blockPowerTiles && s.grid[x][y] == Tile.POWER) return false;
        return true;
    }

    // ===================== LOOP / MOVEMENT =====================

    /**
     * Adds current position to the loop-memory queue.
     * Positions are encoded into a single 64-bit key (x in high bits, y in low bits).
     */
    private void pushPos(int x, int y) {
        long key = (((long) x) << 32) ^ (y & 0xffffffffL);
        lastPositions.addLast(key);
        while (lastPositions.size() > LOOP_MEM) lastPositions.removeFirst();
    }

    /** @return true if the given cell is present in the recent-position memory window. */
    private boolean isRecentPos(int x, int y) {
        long key = (((long) x) << 32) ^ (y & 0xffffffffL);
        for (long k : lastPositions) if (k == key) return true;
        return false;
    }

    /**
     * If the chosen move leads to a recently visited cell, attempts to select an alternative legal move
     * that does not re-enter the loop, while keeping the original choice as a fallback.
     */
    private Direction breakLoopIfNeeded(int px, int py, GameState s, Direction chosen, boolean blockPowerTiles) {
        int nx = px + chosen.dx;
        int ny = py + chosen.dy;
        if (!isRecentPos(nx, ny)) return chosen;

        Direction[] dirs = {Direction.UP, Direction.LEFT, Direction.DOWN, Direction.RIGHT};
        for (Direction d : dirs) {
            if (d == chosen) continue;
            int tx = px + d.dx, ty = py + d.dy;
            if (!passable(tx, ty, s, blockPowerTiles)) continue;
            if (!isRecentPos(tx, ty)) return d;
        }
        return chosen;
    }

    /**
     * Counts the number of legal neighbor cells around (x,y).
     * Used as a simple proxy for "openness" of the area.
     */
    private int countExits(int x, int y, GameState s, boolean blockPowerTiles) {
        int exits = 0;
        Direction[] dirs = {Direction.UP, Direction.LEFT, Direction.DOWN, Direction.RIGHT};
        for (Direction d : dirs) {
            int nx = x + d.dx;
            int ny = y + d.dy;
            if (passable(nx, ny, s, blockPowerTiles)) exits++;
        }
        return exits;
    }

    /**
     * Picks any legal move with mild smoothing:
     * prefer continuing {@link #lastDir}, avoid immediate reverse if possible, otherwise take the first legal option.
     */
    private Direction anyLegalMove(int px, int py, GameState s, boolean blockPowerTiles) {
        Direction[] dirs = {Direction.UP, Direction.LEFT, Direction.DOWN, Direction.RIGHT};

        if (lastDir != Direction.STAY) {
            int nx = px + lastDir.dx, ny = py + lastDir.dy;
            if (passable(nx, ny, s, blockPowerTiles)) return lastDir;
        }

        Direction rev = opposite(lastDir);
        for (Direction d : dirs) {
            if (d == rev) continue;
            int nx = px + d.dx, ny = py + d.dy;
            if (passable(nx, ny, s, blockPowerTiles)) return d;
        }

        for (Direction d : dirs) {
            int nx = px + d.dx, ny = py + d.dy;
            if (passable(nx, ny, s, blockPowerTiles)) return d;
        }
        return Direction.STAY;
    }

    /**
     * Used when the agent is stuck (not moving for several ticks):
     * tries to choose a legal move different from {@code avoid} and also avoids reversing if possible.
     */
    private Direction forceDifferentLegal(int px, int py, GameState s, Direction avoid, boolean blockPowerTiles) {
        Direction[] dirs = {Direction.UP, Direction.LEFT, Direction.DOWN, Direction.RIGHT};
        Direction rev = opposite(lastDir);

        for (Direction d : dirs) {
            if (d == avoid) continue;
            if (d == rev) continue;
            int nx = px + d.dx, ny = py + d.dy;
            if (passable(nx, ny, s, blockPowerTiles)) return d;
        }
        for (Direction d : dirs) {
            if (d == avoid) continue;
            int nx = px + d.dx, ny = py + d.dy;
            if (passable(nx, ny, s, blockPowerTiles)) return d;
        }
        return avoid;
    }

    /**
     * Avoids choosing the reverse of {@link #lastDir} when an alternative legal move exists.
     * This reduces "jitter" and ping-ponging in corridors.
     */
    private Direction applyNoReverse(int px, int py, GameState s, Direction chosen, boolean blockPowerTiles) {
        if (lastDir == Direction.STAY) return chosen;

        Direction rev = opposite(lastDir);
        if (chosen != rev) return chosen;

        Direction[] dirs = {Direction.UP, Direction.LEFT, Direction.DOWN, Direction.RIGHT};
        for (Direction d : dirs) {
            if (d == rev) continue;
            int nx = px + d.dx, ny = py + d.dy;
            if (passable(nx, ny, s, blockPowerTiles)) return d;
        }
        return chosen;
    }

    /**
     * Persists the last chosen move and last position for the next tick.
     *
     * @return the same {@code dir} argument for convenient call chaining
     */
    private Direction remember(int px, int py, Direction dir) {
        lastX = px;
        lastY = py;
        lastDir = dir;
        return dir;
    }

    /**
     * Simple deterministic opening rule: try RIGHT, then LEFT, otherwise stay.
     * This helps prevent the agent from idling at the first few ticks.
     */
    private Direction openingMove(int px, int py, GameState s, boolean blockPowerTiles) {
        int rx = px + Direction.RIGHT.dx, ry = py + Direction.RIGHT.dy;
        if (passable(rx, ry, s, blockPowerTiles)) return Direction.RIGHT;

        int lx = px + Direction.LEFT.dx, ly = py + Direction.LEFT.dy;
        if (passable(lx, ly, s, blockPowerTiles)) return Direction.LEFT;

        return Direction.STAY;
    }

    // ===================== PASSABLE =====================

    /**
     * Passability rule for Pac-Man movement.
     *
     * <p>Blocks:</p>
     * <ul>
     *   <li>out-of-bounds cells</li>
     *   <li>walls</li>
     *   <li>POWER tiles when {@code blockPowerTiles} is enabled</li>
     *   <li>cells occupied by a danger ghost (non-eatable), to avoid stepping into immediate collision</li>
     * </ul>
     */
    private boolean passable(int x, int y, GameState s, boolean blockPowerTiles) {
        if (!s.inBounds(x, y)) return false;
        if (s.grid[x][y] == Tile.WALL) return false;
        if (blockPowerTiles && s.grid[x][y] == Tile.POWER) return false;

        for (Ghost g : s.getGhosts()) {
            if (g == null) continue;
            if (g.isEatable()) continue;
            if (g.x() == x && g.y() == y) return false;
        }
        return true;
    }

    /** @return true if the board contains at least one cell of the given {@code target} tile. */
    private boolean boardHasTile(GameState s, Tile target) {
        for (int x = 0; x < s.w; x++) {
            for (int y = 0; y < s.h; y++) {
                if (s.grid[x][y] == target) return true;
            }
        }
        return false;
    }

    /**
     * Returns the opposite direction (used for "no reverse" logic).
     *
     * @param d input direction (may be null)
     * @return opposite direction; {@link Direction#STAY} if {@code d} is null or {@code STAY}
     */
    private static Direction opposite(Direction d) {
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
