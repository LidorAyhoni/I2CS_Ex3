package assignments.Ex3;

import exe.ex3.game.Game;
import exe.ex3.game.GhostCL;
import exe.ex3.game.PacManAlgo;
import exe.ex3.game.PacmanGame;

import java.awt.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * PAC-MAN v11 - SAFETY FIRST, EAT FAST (NEAREST), SMART POWER + POWER LOCK + NO POWER FIRST 5s
 *
 * Improvement:
 * - Better "nearest target" selection:
 *   BFS finds MIN distance, but if multiple targets share same min distance, choose best by:
 *   (1) safer from danger ghosts, (2) more exits, (3) less looping.
 *
 * Rules:
 * 1) POWER LOCK: while ghosts are eatable (powerMode==true), Pac-Man will NOT step on GREEN.
 * 2) NO POWER FIRST 5s: during the first ~5 seconds (first 50 ticks), Pac-Man will NOT step on GREEN.
 *
 * Priorities:
 * 1) Maximize distance from danger (non-eatable) ghosts when they are near.
 * 2) Eat pellets: prefer nearest pellet (BFS shortest path) with smart tie-break.
 * 3) Do it as fast as possible.
 */
public class Ex3Algo implements PacManAlgo {
	private int _count;

	private boolean _inited = false;
	private int DOT, POWER;

	private int baseWallValue = Integer.MIN_VALUE;

	private int lastX = Integer.MIN_VALUE, lastY = Integer.MIN_VALUE;
	private int lastDir = Game.STAY;

	private int stuckCount = 0;

	// ======= BEHAVIOR KNOBS =======
	private static final int DANGER_TRIGGER = 6;
	private static final int HARD_AVOID = 2;

	// When NOT in power mode, take a POWER only if it's "worth it"
	private static final int POWER_TAKE_IF_DIST_LE = 2;      // very close -> ok
	private static final int POWER_PREFER_IF_DANGER_LE = 5;  // danger near -> power becomes valuable

	// Opening
	private static final int OPENING_STEPS = 25;

	// ======= no green in first 5 seconds =======
	// Assumption: ~10 ticks per second => 5s ~= 50 ticks
	private static final int NO_POWER_FIRST_TICKS = 100;

	// Loop memory
	private static final int LOOP_MEM = 12;
	private final ArrayDeque<Long> lastPositions = new ArrayDeque<>();

	private static final boolean DEBUG = false;

	public Ex3Algo() { _count = 0; }

	@Override
	public String getInfo() {
		return "PacMan v11: escape-first, eat nearest fast (better nearest), smart power + POWER LOCK + NO GREEN first 5s.";
	}

	@Override
	public int move(PacmanGame game) {
		_count++;
		int code = 0;

		int[][] b = game.getGame(code);
		if (!_inited) initColors(code);

		if (baseWallValue == Integer.MIN_VALUE) {
			baseWallValue = detectWallValueStable(b);
		}

		int[] pac = parseXY(game.getPos(code));
		int px = wrapX(pac[0], b), py = wrapY(pac[1], b);

		if (px == lastX && py == lastY) stuckCount++;
		else stuckCount = 0;

		GhostCL[] ghosts = game.getGhosts(code);

		pushPos(px, py);

		// power mode = at least one ghost has remainTimeAsEatable > 0
		boolean powerMode = false;
		if (ghosts != null) {
			for (GhostCL g : ghosts) {
				if (g == null) continue;
				if (g.remainTimeAsEatable(code) > 0) { powerMode = true; break; }
			}
		}

		// Block green if:
		// 1) powerMode is active (POWER LOCK), OR
		// 2) first 5 seconds of the game
		boolean blockPowerTiles = powerMode || (_count <= NO_POWER_FIRST_TICKS);

		// Opening move: just start moving (but still legal)
		if (_count <= OPENING_STEPS) {
			int op = openingMove(px, py, b, blockPowerTiles, ghosts, code);
			if (op != Game.STAY) {
				remember(px, py, op);
				return op;
			}
		}

		int chosen;

		if (powerMode) {
			// While protected: eat DOT fast (and POWER tiles are blocked)
			chosen = bfsToNearestValueSmart(px, py, b, DOT, blockPowerTiles, ghosts, code);
			if (chosen == Game.STAY) chosen = anyLegalMove(px, py, b, blockPowerTiles, ghosts, code);
		} else {
			int curThreat = minBfsDistToDangerGhost(px, py, b, ghosts, code, blockPowerTiles);

			if (curThreat != Integer.MAX_VALUE && curThreat <= DANGER_TRIGGER) {
				chosen = escapeMove(px, py, b, blockPowerTiles, ghosts, code, curThreat);
			} else {
				chosen = eatFastMove(px, py, b, blockPowerTiles, ghosts, code, curThreat);
			}
		}

		if (chosen == Game.STAY) chosen = anyLegalMove(px, py, b, blockPowerTiles, ghosts, code);

		chosen = breakLoopIfNeeded(px, py, b, chosen, blockPowerTiles, ghosts, code);

		if (stuckCount >= 3) {
			chosen = forceDifferentLegal(px, py, b, chosen, blockPowerTiles, ghosts, code);
		}

		chosen = applyNoReverse(px, py, b, chosen, blockPowerTiles, ghosts, code);

		remember(px, py, chosen);
		return chosen;
	}

	// ===================== PRIORITY 1: ESCAPE =====================

	private int escapeMove(int px, int py, int[][] b, boolean blockPowerTiles,
						   GhostCL[] ghosts, int code, int curThreat) {
		int[] dirs = {Game.UP, Game.LEFT, Game.DOWN, Game.RIGHT};

		int bestDir = Game.STAY;
		int bestScore = Integer.MIN_VALUE;

		// Pass 1: enforce hard avoid if possible
		for (int d : dirs) {
			int nx = stepX(px, d, b);
			int ny = stepY(py, d, b);

			if (!passable(nx, ny, b, blockPowerTiles, ghosts, code)) continue;

			int nt = minBfsDistToDangerGhost(nx, ny, b, ghosts, code, blockPowerTiles);
			if (nt <= HARD_AVOID && curThreat > HARD_AVOID) continue;

			int score = 0;
			score += safeVal(nt) * 2000;
			score += countExits(nx, ny, b, blockPowerTiles, ghosts, code) * 120;
			if (isRecentPos(nx, ny)) score -= 300;
			if (d == lastDir) score += 40;

			if (score > bestScore) {
				bestScore = score;
				bestDir = d;
			}
		}

		if (bestDir != Game.STAY) return bestDir;

		// Pass 2: relax hard avoid (if trapped)
		for (int d : dirs) {
			int nx = stepX(px, d, b);
			int ny = stepY(py, d, b);

			if (!passable(nx, ny, b, blockPowerTiles, ghosts, code)) continue;

			int nt = minBfsDistToDangerGhost(nx, ny, b, ghosts, code, blockPowerTiles);

			int score = safeVal(nt) * 2000
					+ countExits(nx, ny, b, blockPowerTiles, ghosts, code) * 120;

			if (isRecentPos(nx, ny)) score -= 300;
			if (d == lastDir) score += 40;

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

	// ===================== PRIORITY 2+3: EAT FAST (NEAREST) =====================

	private int eatFastMove(int px, int py, int[][] b, boolean blockPowerTiles,
							GhostCL[] ghosts, int code, int curThreat) {

		int dotDist = nearestTargetDist(px, py, b, DOT, blockPowerTiles, ghosts, code);
		int powDist = nearestTargetDist(px, py, b, POWER, blockPowerTiles, ghosts, code);

		boolean dangerNear = (curThreat != Integer.MAX_VALUE && curThreat <= POWER_PREFER_IF_DANGER_LE);
		boolean powerVeryClose = (powDist != Integer.MAX_VALUE && powDist <= POWER_TAKE_IF_DIST_LE);

		if (dotDist == Integer.MAX_VALUE && powDist == Integer.MAX_VALUE) return Game.STAY;
		if (dotDist == Integer.MAX_VALUE) return bfsToNearestValueSmart(px, py, b, POWER, blockPowerTiles, ghosts, code);
		if (powDist == Integer.MAX_VALUE) return bfsToNearestValueSmart(px, py, b, DOT, blockPowerTiles, ghosts, code);

		boolean shouldTakePower =
				dangerNear ||
						powerVeryClose ||
						(powDist + 2 < dotDist);

		int target = shouldTakePower ? POWER : DOT;

		int dir = bfsToNearestValueSmart(px, py, b, target, blockPowerTiles, ghosts, code);
		if (dir != Game.STAY) return dir;

		int other = (target == DOT) ? POWER : DOT;
		return bfsToNearestValueSmart(px, py, b, other, blockPowerTiles, ghosts, code);
	}

	// ===================== BETTER NEAREST (BFS + TIE-BREAK) =====================

	/**
	 * BFS to nearest target. If multiple targets exist at the same minimal distance,
	 * pick the one that is:
	 * 1) safer from danger ghosts (bigger minBfsDistToDangerGhost),
	 * 2) has more exits,
	 * 3) is less likely to loop (avoid recent positions).
	 *
	 * Returns the FIRST MOVE direction.
	 */
	private int bfsToNearestValueSmart(int px, int py, int[][] b, int targetValue,
									   boolean blockPowerTiles, GhostCL[] ghosts, int code) {
		if (blockPowerTiles && targetValue == POWER) return Game.STAY;

		int w = b.length, h = b[0].length;

		// quick existence check (cheap)
		boolean has = false;
		for (int x = 0; x < w && !has; x++) {
			for (int y = 0; y < h; y++) {
				if (b[x][y] == targetValue) { has = true; break; }
			}
		}
		if (!has) return Game.STAY;

		boolean[][] vis = new boolean[w][h];
		int[][] firstDir = new int[w][h];
		int[][] dist = new int[w][h];

		for (int i = 0; i < w; i++) {
			for (int j = 0; j < h; j++) {
				dist[i][j] = Integer.MAX_VALUE;
				firstDir[i][j] = Game.STAY;
			}
		}

		ArrayDeque<int[]> q = new ArrayDeque<>();
		q.add(new int[]{px, py});
		vis[px][py] = true;
		dist[px][py] = 0;

		int[] dirs = {Game.UP, Game.LEFT, Game.DOWN, Game.RIGHT};

		int foundDist = Integer.MAX_VALUE;
		ArrayList<int[]> candidates = new ArrayList<>(); // each: {x,y}

		while (!q.isEmpty()) {
			int[] cur = q.poll();
			int cx = cur[0], cy = cur[1];
			int cd = dist[cx][cy];

			// once we've found candidates at minimal distance, stop expanding deeper layers
			if (cd > foundDist) break;

			for (int d : dirs) {
				int nx = stepX(cx, d, b);
				int ny = stepY(cy, d, b);

				if (vis[nx][ny]) continue;
				if (!passable(nx, ny, b, blockPowerTiles, ghosts, code)) continue;

				vis[nx][ny] = true;
				dist[nx][ny] = cd + 1;
				firstDir[nx][ny] = (cx == px && cy == py) ? d : firstDir[cx][cy];

				if (b[nx][ny] == targetValue) {
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

		if (candidates.isEmpty()) return Game.STAY;

		// Tie-break among same-distance candidates
		int bestDir = Game.STAY;
		int bestScore = Integer.MIN_VALUE;

		for (int[] c : candidates) {
			int x = c[0], y = c[1];
			int dir = firstDir[x][y];
			if (dir == Game.STAY) continue;

			int threat = minBfsDistToDangerGhost(x, y, b, ghosts, code, blockPowerTiles);
			int exits = countExits(x, y, b, blockPowerTiles, ghosts, code);

			int score = 0;
			// SAFETY dominates in tie-break
			score += safeVal(threat) * 1000;
			score += exits * 120;
			if (isRecentPos(x, y)) score -= 400;
			if (dir == lastDir) score += 40;

			if (score > bestScore) {
				bestScore = score;
				bestDir = dir;
			}
		}

		return bestDir;
	}

	// ===================== BFS HELPERS =====================

	private int nearestTargetDist(int sx, int sy, int[][] b, int targetValue,
								  boolean blockPowerTiles, GhostCL[] ghosts, int code) {
		if (blockPowerTiles && targetValue == POWER) return Integer.MAX_VALUE;

		int w = b.length, h = b[0].length;
		boolean[][] vis = new boolean[w][h];
		ArrayDeque<int[]> q = new ArrayDeque<>();
		q.add(new int[]{sx, sy, 0});
		vis[sx][sy] = true;

		int[] dirs = {Game.UP, Game.LEFT, Game.DOWN, Game.RIGHT};

		while (!q.isEmpty()) {
			int[] cur = q.poll();
			int x = cur[0], y = cur[1], dist = cur[2];

			if (b[x][y] == targetValue && !(x == sx && y == sy)) return dist;

			for (int d : dirs) {
				int nx = stepX(x, d, b);
				int ny = stepY(y, d, b);

				if (vis[nx][ny]) continue;
				if (!passable(nx, ny, b, blockPowerTiles, ghosts, code)) continue;

				vis[nx][ny] = true;
				q.add(new int[]{nx, ny, dist + 1});
			}
		}
		return Integer.MAX_VALUE;
	}

	private int minBfsDistToDangerGhost(int px, int py, int[][] b, GhostCL[] ghosts,
										int code, boolean blockPowerTiles) {
		int best = Integer.MAX_VALUE;
		if (ghosts == null) return best;

		for (GhostCL g : ghosts) {
			if (g == null) continue;
			if (g.remainTimeAsEatable(code) > 0) continue; // danger only
			int[] gp = parseXY(g.getPos(code));
			int gx = wrapX(gp[0], b), gy = wrapY(gp[1], b);
			int d = bfsDist(px, py, gx, gy, b, blockPowerTiles, ghosts, code);
			best = Math.min(best, d);
		}
		return best;
	}

	private int bfsDist(int sx, int sy, int tx, int ty, int[][] b,
						boolean blockPowerTiles, GhostCL[] ghosts, int code) {
		if (sx == tx && sy == ty) return 0;

		int w = b.length, h = b[0].length;
		boolean[][] vis = new boolean[w][h];
		ArrayDeque<int[]> q = new ArrayDeque<>();
		q.add(new int[]{sx, sy, 0});
		vis[sx][sy] = true;

		int[] dirs = {Game.UP, Game.LEFT, Game.DOWN, Game.RIGHT};

		while (!q.isEmpty()) {
			int[] cur = q.poll();
			int x = cur[0], y = cur[1], dist = cur[2];

			for (int d : dirs) {
				int nx = stepX(x, d, b);
				int ny = stepY(y, d, b);

				if (vis[nx][ny]) continue;
				if (b[nx][ny] == baseWallValue) continue;
				if (blockPowerTiles && b[nx][ny] == POWER) continue;
				if (isNonEatableGhostAt(nx, ny, ghosts, code)) continue;

				if (nx == tx && ny == ty) return dist + 1;

				vis[nx][ny] = true;
				q.add(new int[]{nx, ny, dist + 1});
			}
		}
		return Integer.MAX_VALUE;
	}

	// ===================== LOOP =====================

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

	private int breakLoopIfNeeded(int px, int py, int[][] b, int chosen,
								  boolean blockPowerTiles, GhostCL[] ghosts, int code) {
		int nx = stepX(px, chosen, b);
		int ny = stepY(py, chosen, b);
		if (!isRecentPos(nx, ny)) return chosen;

		int[] dirs = {Game.UP, Game.LEFT, Game.DOWN, Game.RIGHT};
		for (int d : dirs) {
			if (d == chosen) continue;
			int tx = stepX(px, d, b), ty = stepY(py, d, b);
			if (!passable(tx, ty, b, blockPowerTiles, ghosts, code)) continue;
			if (!isRecentPos(tx, ty)) return d;
		}
		return chosen;
	}

	// ===================== MOVEMENT HELPERS =====================

	private int countExits(int x, int y, int[][] b, boolean blockPowerTiles, GhostCL[] ghosts, int code) {
		int exits = 0;
		int[] dirs = {Game.UP, Game.LEFT, Game.DOWN, Game.RIGHT};
		for (int d : dirs) {
			int nx = stepX(x, d, b);
			int ny = stepY(y, d, b);
			if (passable(nx, ny, b, blockPowerTiles, ghosts, code)) exits++;
		}
		return exits;
	}

	private int anyLegalMove(int px, int py, int[][] b, boolean blockPowerTiles, GhostCL[] ghosts, int code) {
		int[] dirs = {Game.UP, Game.LEFT, Game.DOWN, Game.RIGHT};

		if (lastDir != Game.STAY) {
			int nx = stepX(px, lastDir, b), ny = stepY(py, lastDir, b);
			if (passable(nx, ny, b, blockPowerTiles, ghosts, code)) return lastDir;
		}

		int rev = opposite(lastDir);
		for (int d : dirs) {
			if (d == rev) continue;
			int nx = stepX(px, d, b), ny = stepY(py, d, b);
			if (passable(nx, ny, b, blockPowerTiles, ghosts, code)) return d;
		}

		for (int d : dirs) {
			int nx = stepX(px, d, b), ny = stepY(py, d, b);
			if (passable(nx, ny, b, blockPowerTiles, ghosts, code)) return d;
		}
		return Game.STAY;
	}

	private int forceDifferentLegal(int px, int py, int[][] b, int avoid,
									boolean blockPowerTiles, GhostCL[] ghosts, int code) {
		int[] dirs = {Game.UP, Game.LEFT, Game.DOWN, Game.RIGHT};
		int rev = opposite(lastDir);

		for (int d : dirs) {
			if (d == avoid) continue;
			if (d == rev) continue;
			int nx = stepX(px, d, b), ny = stepY(py, d, b);
			if (passable(nx, ny, b, blockPowerTiles, ghosts, code)) return d;
		}
		for (int d : dirs) {
			if (d == avoid) continue;
			int nx = stepX(px, d, b), ny = stepY(py, d, b);
			if (passable(nx, ny, b, blockPowerTiles, ghosts, code)) return d;
		}
		return avoid;
	}

	private int applyNoReverse(int px, int py, int[][] b, int chosen,
							   boolean blockPowerTiles, GhostCL[] ghosts, int code) {
		if (lastDir == Game.STAY) return chosen;
		int rev = opposite(lastDir);
		if (chosen != rev) return chosen;

		int[] dirs = {Game.UP, Game.LEFT, Game.DOWN, Game.RIGHT};
		for (int d : dirs) {
			if (d == rev) continue;
			int nx = stepX(px, d, b), ny = stepY(py, d, b);
			if (passable(nx, ny, b, blockPowerTiles, ghosts, code)) return d;
		}
		return chosen;
	}

	private void remember(int px, int py, int dir) {
		lastX = px;
		lastY = py;
		lastDir = dir;
	}

	// ===================== PASSABLE / WALL =====================

	private boolean passable(int x, int y, int[][] b, boolean blockPowerTiles, GhostCL[] ghosts, int code) {
		int wx = wrapX(x, b);
		int wy = wrapY(y, b);

		int v = b[wx][wy];
		if (v == baseWallValue) return false;
		if (blockPowerTiles && v == POWER) return false;
		if (isNonEatableGhostAt(wx, wy, ghosts, code)) return false;
		return true;
	}

	private boolean isNonEatableGhostAt(int x, int y, GhostCL[] ghosts, int code) {
		if (ghosts == null) return false;
		for (GhostCL g : ghosts) {
			if (g == null) continue;
			if (g.remainTimeAsEatable(code) > 0) continue;
			int[] gp = parseXY(g.getPos(code));
			if (gp[0] == x && gp[1] == y) return true;
		}
		return false;
	}

	// ===================== WALL DETECTION =====================

	private int detectWallValueStable(int[][] b) {
		Map<Integer, Integer> borderFreq = new HashMap<>();
		int w = b.length, h = b[0].length;

		for (int x = 0; x < w; x++) {
			addIfWallCandidate(borderFreq, b[x][0]);
			addIfWallCandidate(borderFreq, b[x][h - 1]);
		}
		for (int y = 0; y < h; y++) {
			addIfWallCandidate(borderFreq, b[0][y]);
			addIfWallCandidate(borderFreq, b[w - 1][y]);
		}

		int bestVal = Integer.MIN_VALUE, bestCnt = -1;
		for (Map.Entry<Integer, Integer> e : borderFreq.entrySet()) {
			if (e.getValue() > bestCnt) {
				bestCnt = e.getValue();
				bestVal = e.getKey();
			}
		}
		if (bestVal != Integer.MIN_VALUE) return bestVal;

		return mostFrequentExcluding(b, DOT, POWER);
	}

	private void addIfWallCandidate(Map<Integer, Integer> freq, int v) {
		if (v == DOT) return;
		if (v == POWER) return;
		if (v == 0) return;
		freq.put(v, freq.getOrDefault(v, 0) + 1);
	}

	private static int mostFrequentExcluding(int[][] b, int a, int c) {
		Map<Integer, Integer> freq = new HashMap<>();
		for (int x = 0; x < b.length; x++) {
			for (int y = 0; y < b[0].length; y++) {
				int v = b[x][y];
				if (v == a || v == c) continue;
				if (v == 0) continue;
				freq.put(v, freq.getOrDefault(v, 0) + 1);
			}
		}
		int bestVal = Integer.MIN_VALUE, bestCnt = -1;
		for (Map.Entry<Integer, Integer> e : freq.entrySet()) {
			if (e.getValue() > bestCnt) {
				bestCnt = e.getValue();
				bestVal = e.getKey();
			}
		}
		if (bestVal == Integer.MIN_VALUE) return mostFrequent(b);
		return bestVal;
	}

	private static int mostFrequent(int[][] b) {
		Map<Integer, Integer> freq = new HashMap<>();
		for (int x = 0; x < b.length; x++) {
			for (int y = 0; y < b[0].length; y++) {
				int v = b[x][y];
				freq.put(v, freq.getOrDefault(v, 0) + 1);
			}
		}
		int bestVal = b[0][0], bestCnt = -1;
		for (Map.Entry<Integer, Integer> e : freq.entrySet()) {
			if (e.getValue() > bestCnt) {
				bestCnt = e.getValue();
				bestVal = e.getKey();
			}
		}
		return bestVal;
	}

	// ===================== BASIC =====================

	private void initColors(int code) {
		DOT = Game.getIntColor(Color.PINK, code);
		POWER = Game.getIntColor(Color.GREEN, code);
		_inited = true;
	}

	private static int[] parseXY(Object posObj) {
		String s = String.valueOf(posObj).trim();
		int comma = s.indexOf(',');
		if (comma < 0) return new int[]{0, 0};
		try {
			int x = Integer.parseInt(s.substring(0, comma).trim());
			int y = Integer.parseInt(s.substring(comma + 1).trim());
			return new int[]{x, y};
		} catch (Exception e) {
			return new int[]{0, 0};
		}
	}

	private int wrapX(int x, int[][] b) {
		int w = b.length;
		x %= w;
		if (x < 0) x += w;
		return x;
	}

	private int wrapY(int y, int[][] b) {
		int h = b[0].length;
		y %= h;
		if (y < 0) y += h;
		return y;
	}

	private int stepX(int x, int dir, int[][] b) {
		return wrapX(x + dx(dir), b);
	}

	private int stepY(int y, int dir, int[][] b) {
		return wrapY(y + dy(dir), b);
	}

	private static int dx(int dir) {
		if (dir == Game.LEFT) return -1;
		if (dir == Game.RIGHT) return 1;
		return 0;
	}

	private static int dy(int dir) {
		if (dir == Game.UP) return 1;
		if (dir == Game.DOWN) return -1;
		return 0;
	}

	private static int opposite(int dir) {
		if (dir == Game.UP) return Game.DOWN;
		if (dir == Game.DOWN) return Game.UP;
		if (dir == Game.LEFT) return Game.RIGHT;
		if (dir == Game.RIGHT) return Game.LEFT;
		return Game.STAY;
	}

	private int openingMove(int px, int py, int[][] b, boolean blockPowerTiles, GhostCL[] ghosts, int code) {
		int rx = stepX(px, Game.RIGHT, b), ry = stepY(py, Game.RIGHT, b);
		if (passable(rx, ry, b, blockPowerTiles, ghosts, code)) return Game.RIGHT;

		int lx = stepX(px, Game.LEFT, b), ly = stepY(py, Game.LEFT, b);
		if (passable(lx, ly, b, blockPowerTiles, ghosts, code)) return Game.LEFT;

		return Game.STAY;
	}

	private void dbg(String s) {
		if (DEBUG) System.out.println(s);
	}
}
