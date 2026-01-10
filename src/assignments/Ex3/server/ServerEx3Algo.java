package assignments.Ex3.server;

import assignments.Ex3.model.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Server-side version of Ex3Algo: works directly on GameState (no PacmanGame/GhostCL).
 * Priorities:
 * 1) Escape when danger ghosts are near (maximize distance + prefer open tiles).
 * 2) Eat fast: BFS shortest path to nearest DOT/POWER.
 * 3) Break loops + avoid reverse.
 */
public class ServerEx3Algo {

    // ======= BEHAVIOR KNOBS =======
    private static final int DANGER_TRIGGER = 7;
    private static final int HARD_AVOID = 2;
    private static final int POWER_TAKE_IF_DIST_LE = 2;
    private static final int POWER_PREFER_IF_DANGER_LE = 5;

    private static final int OPENING_STEPS = 25;
    private static final int NO_POWER_FIRST_TICKS = 50; // same spirit as your original
    private static final int LOOP_MEM = 12;

    private int tick = 0;

    private int lastX = Integer.MIN_VALUE, lastY = Integer.MIN_VALUE;
    private Direction lastDir = Direction.STAY;
    private int stuckCount = 0;

    private final ArrayDeque<Long> lastPositions = new ArrayDeque<>();

    public Direction nextMove(GameState s) {
        tick++;

        int px = s.getPacmanX();
        int py = s.getPacmanY();

        if (px == lastX && py == lastY) stuckCount++;
        else stuckCount = 0;

        pushPos(px, py);

        boolean powerMode = s.isPowerMode();
        boolean blockPowerTiles = powerMode || (tick <= NO_POWER_FIRST_TICKS);

        // Opening: start moving
        if (tick <= OPENING_STEPS) {
            Direction op = openingMove(px, py, s, blockPowerTiles);
            if (op != Direction.STAY) return remember(px, py, op);
        }

        Direction chosen;

        if (powerMode) {
            // While powered: prefer DOTs (we still "block POWER tiles" like your POWER LOCK policy)
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

        // loop + stuck
        chosen = breakLoopIfNeeded(px, py, s, chosen, blockPowerTiles);
        if (stuckCount >= 3) chosen = forceDifferentLegal(px, py, s, chosen, blockPowerTiles);

        // avoid reverse if possible
        chosen = applyNoReverse(px, py, s, chosen, blockPowerTiles);

        return remember(px, py, chosen);
    }

    // ===================== ESCAPE =====================

    private Direction escapeMove(int px, int py, GameState s, boolean blockPowerTiles, int curThreat) {
        Direction[] dirs = {Direction.UP, Direction.LEFT, Direction.DOWN, Direction.RIGHT};

        Direction bestDir = Direction.STAY;
        int bestScore = Integer.MIN_VALUE;

        // Pass 1: enforce hard avoid if possible
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

        // Pass 2: relax
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

    private int safeVal(int dist) {
        if (dist == Integer.MAX_VALUE) return 50;
        return Math.min(dist, 50);
    }

    // ===================== EAT FAST =====================

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

    // for ghost-distance BFS: ignore "ghost occupancy" blocks, only walls + blocked power.
    private boolean passableGhostDist(int x, int y, GameState s, boolean blockPowerTiles) {
        if (s.isWall(x, y)) return false;
        if (blockPowerTiles && s.grid[x][y] == Tile.POWER) return false;
        return true;
    }

    // ===================== LOOP / MOVEMENT =====================

    private void pushPos(int x, int y) {
        long key = (((long) x) << 32) ^ (y & 0xffffffffL);
        lastPositions.addLast(key);
        while (lastPositions.size() > LOOP_MEM) lastPositions.removeFirst();
    }

    private boolean isRecentPos(int x, int y) {
        long key = (((long) x) << 32) ^ (y & 0xffffffffL);
        for (long k : lastPositions) if (k == key) return true;
        return false;
    }

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

    private Direction remember(int px, int py, Direction dir) {
        lastX = px;
        lastY = py;
        lastDir = dir;
        return dir;
    }

    private Direction openingMove(int px, int py, GameState s, boolean blockPowerTiles) {
        int rx = px + Direction.RIGHT.dx, ry = py + Direction.RIGHT.dy;
        if (passable(rx, ry, s, blockPowerTiles)) return Direction.RIGHT;

        int lx = px + Direction.LEFT.dx, ly = py + Direction.LEFT.dy;
        if (passable(lx, ly, s, blockPowerTiles)) return Direction.LEFT;

        return Direction.STAY;
    }

    // ===================== PASSABLE =====================

    private boolean passable(int x, int y, GameState s, boolean blockPowerTiles) {
        if (!s.inBounds(x, y)) return false;
        if (s.grid[x][y] == Tile.WALL) return false;
        if (blockPowerTiles && s.grid[x][y] == Tile.POWER) return false;

        // block stepping onto a danger ghost tile (same spirit as original)
        for (Ghost g : s.getGhosts()) {
            if (g == null) continue;
            if (g.isEatable()) continue;
            if (g.x() == x && g.y() == y) return false;
        }
        return true;
    }

    private boolean boardHasTile(GameState s, Tile target) {
        for (int x = 0; x < s.w; x++) {
            for (int y = 0; y < s.h; y++) {
                if (s.grid[x][y] == target) return true;
            }
        }
        return false;
    }

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
