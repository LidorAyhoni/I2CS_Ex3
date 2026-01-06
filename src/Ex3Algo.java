package assignments.Ex3;

import exe.ex3.game.Game;
import exe.ex3.game.GhostCL;
import exe.ex3.game.PacManAlgo;
import exe.ex3.game.PacmanGame;

import java.awt.*;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

public class Ex3Algo implements PacManAlgo {
	private int _count;

	private boolean _inited = false;
	private int DOT, POWER;

	// stable wall value
	private int baseWallValue = Integer.MIN_VALUE;

	private int lastX = Integer.MIN_VALUE, lastY = Integer.MIN_VALUE;
	private int lastDir = Game.STAY;

	private int stuckCount = 0;

	// board tracking (ate detection)
	private int[][] previousBoard = null;

	// opening
	private static final int OPENING_STEPS = 35;

	// timing
	private static final int TICKS_5_SECONDS = 0;

	private int ticksSinceLastPower = TICKS_5_SECONDS;
	private static final int FARM_AFTER_POWER_STEPS = 120;
	private int farmDotsTicksLeft = 0;

	private int powerTicksLeft = 0;

	// danger (BFS steps)
	private static final int DANGER_RADIUS_NORMAL = 4;
	private static final int DANGER_RADIUS_SOFT = 2;

	// loop memory
	private static final int LOOP_MEM = 10;
	private static final int STUCK_HARD = 12;
	private static final int STUCK_SOFT = 6;
	private final ArrayDeque<Long> lastPositions = new ArrayDeque<>();

	// debug
	private static final boolean DEBUG = false;
	private static final int DBG_EVERY_TICKS = 25;

	public Ex3Algo() { _count = 0; }

	@Override
	public String getInfo() {
		return "PacMan improved: stable wall detection, tunnel wrap support, BFS-distance danger, " +
				"score-based move choice, never chase ghosts, power rules preserved.";
	}

	@Override
	public int move(PacmanGame game) {
		_count++;
		int code = 0;

		int[][] board = game.getGame(code);
		if (!_inited) initColors(code);

		// wall value ONCE
		if (baseWallValue == Integer.MIN_VALUE) {
			baseWallValue = detectWallValueStable(board);
			dbg("INIT: baseWallValue fixed to " + baseWallValue);
		}

		int[] pac = parseXY(game.getPos(code));
		int px = pac[0], py = pac[1];

		// wrap safety (just in case)
		px = wrapX(px, board);
		py = wrapY(py, board);

		// detect eaten pellet by comparing previous board
		boolean atePowerNow = false;
		boolean ateDotNow = false;
		if (previousBoard != null) {
			int prev = previousBoard[px][py];
			if (prev == POWER) atePowerNow = true;
			else if (prev == DOT) ateDotNow = true;
		}
		previousBoard = deepCopyBoard(board);

		// stuck tracking
		if (px == lastX && py == lastY) stuckCount++;
		else stuckCount = 0;

		GhostCL[] ghosts = game.getGhosts(code);

		// power mode from ghosts
		int maxEatable = 0;
		for (GhostCL g : ghosts) {
			if (g == null) continue;
			maxEatable = (int) Math.max(maxEatable, g.remainTimeAsEatable(code));
		}
		powerTicksLeft = maxEatable;
		boolean powerMode = powerTicksLeft > 0;

		// spacing + farm window
		if (atePowerNow) {
			ticksSinceLastPower = 0;
			farmDotsTicksLeft = FARM_AFTER_POWER_STEPS;
		} else {
			ticksSinceLastPower++;
		}

		boolean powerAllowedNow = canGoForPower(powerMode);
		boolean blockPowerTiles = !powerAllowedNow;

		pushPos(px, py);

		boolean dbgOn = shouldDbg();
		if (dbgOn) {
			dbg("------------------------------------------------------------");
			dbg("TICK=" + _count + " POS=(" + px + "," + py + ") last=(" + lastX + "," + lastY + ") lastDir=" + dirName(lastDir)
					+ " stuck=" + stuckCount);
			dbg("baseWallValue=" + baseWallValue + " cell=" + cellValStr(px, py, board));
			dbg("powerMode=" + powerMode + " powerLeft=" + powerTicksLeft +
					" sincePower=" + ticksSinceLastPower + " farmLeft=" + farmDotsTicksLeft +
					" powerAllowedNow=" + powerAllowedNow + " blockPowerTiles=" + blockPowerTiles +
					" atePowerNow=" + atePowerNow + " ateDotNow=" + ateDotNow);
		}

		// opening with wrap support
		if (_count <= OPENING_STEPS) {
			int forced = openingMove(px, py, board, blockPowerTiles, ghosts, code);
			if (forced != Game.STAY) {
				remember(px, py, forced);
				return forced;
			}
		}

		boolean relax = stuckCount >= STUCK_HARD;
		boolean semiRelax = stuckCount >= STUCK_SOFT;

		int chosen;

		if (powerMode) {
			// ghosts are eatable: don't fear, don't chase, don't go POWER
			int bfsDot = bfsToNearestValue(px, py, board, DOT, true, ghosts, code);
			chosen = (bfsDot != Game.STAY) ? bfsDot : anyLegalMove(px, py, board, true, ghosts, code);
			if (farmDotsTicksLeft > 0) farmDotsTicksLeft--;

		} else {
			// normal mode
			if (_count <= TICKS_5_SECONDS) {
				// first 5 sec: avoid POWER
				chosen = chooseBestMoveScore(px, py, board, ghosts, code, true, semiRelax, relax);
			} else {
				if (farmDotsTicksLeft > 0) {
					// after power: farm dots, avoid POWER
					chosen = chooseBestMoveScore(px, py, board, ghosts, code, true, semiRelax, relax);
					farmDotsTicksLeft--;
				} else {
					chosen = chooseBestMoveScore(px, py, board, ghosts, code, blockPowerTiles, semiRelax, relax);

					// optional: go for POWER if allowed and safe enough
					if (powerAllowedNow && !relax) {
						int dPow = nearestTargetDist(px, py, board, POWER, ghosts, code, false);
						int dDot = nearestTargetDist(px, py, board, DOT, ghosts, code, blockPowerTiles);
						int dangerDist = minBfsDistToDangerGhost(px, py, board, ghosts, code, blockPowerTiles);

						if (dPow < Integer.MAX_VALUE && (dPow <= dDot + 2) && dangerDist > 3) {
							int bfsPower = bfsToNearestValue(px, py, board, POWER, false, ghosts, code);
							if (bfsPower != Game.STAY) chosen = bfsPower;
						}
					}
				}
			}
		}

		if (chosen == Game.STAY) chosen = anyLegalMove(px, py, board, blockPowerTiles, ghosts, code);

		// loop breaker
		chosen = breakLoopIfNeeded(px, py, board, chosen, blockPowerTiles, ghosts, code);

		// anti-stuck
		if (stuckCount >= 2) chosen = forceDifferentLegal(px, py, board, chosen, blockPowerTiles, ghosts, code);

		// no reverse
		chosen = applyNoReverse(px, py, board, chosen, blockPowerTiles, ghosts, code);

		if (dbgOn) {
			dbg("CHOSEN=" + dirName(chosen));
			int nx = stepX(px, chosen, board), ny = stepY(py, chosen, board);
			dbg("NEXT=(" + nx + "," + ny + ") passable=" + passable(nx, ny, board, blockPowerTiles, ghosts, code)
					+ " cell=" + cellValStr(nx, ny, board));
		}

		remember(px, py, chosen);
		return chosen;
	}

	// ===================== WRAP (TUNNEL) SUPPORT =====================
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

	// ===================== SCORE-BASED MOVE =====================
	private int chooseBestMoveScore(int px, int py, int[][] b, GhostCL[] ghosts, int code,
									boolean blockPowerTiles, boolean semiRelax, boolean relax) {

		int[] dirs = {Game.UP, Game.LEFT, Game.DOWN, Game.RIGHT};
		int rev = opposite(lastDir);

		int bestDir = Game.STAY;
		int bestScore = Integer.MIN_VALUE;

		int dangerRadius = semiRelax ? DANGER_RADIUS_SOFT : DANGER_RADIUS_NORMAL;
		int curDangerDist = minBfsDistToDangerGhost(px, py, b, ghosts, code, blockPowerTiles);

		for (int d : dirs) {
			int nx = stepX(px, d, b);
			int ny = stepY(py, d, b);

			if (!passable(nx, ny, b, blockPowerTiles, ghosts, code)) continue;

			int dotDist = nearestTargetDist(nx, ny, b, DOT, ghosts, code, blockPowerTiles);
			if (dotDist == Integer.MAX_VALUE) dotDist = 999;

			int dangerDist = minBfsDistToDangerGhost(nx, ny, b, ghosts, code, blockPowerTiles);
			if (dangerDist == Integer.MAX_VALUE) dangerDist = 999;

			int exits = countExits(nx, ny, b, blockPowerTiles, ghosts, code);
			boolean deadEnd = exits <= 1;

			boolean adjGhost = adjacentToNonEatableGhost(nx, ny, ghosts, code);

			int score = 0;

			// eat dots
			score += 120 - 12 * dotDist;

			// stay safe
			score += 20 * Math.min(dangerDist, 8);

			// if currently in danger => escape is priority
			if (!relax && curDangerDist <= dangerRadius) {
				score += 40 * Math.min(dangerDist, 8);
				score -= 8 * dotDist;
			}

			// penalties
			if (d == rev) score -= 25;
			if (adjGhost) score -= 60;
			if (!relax && deadEnd && dangerDist <= 6) score -= 120;
			if (isRecentPos(nx, ny)) score -= 35;
			if (d == lastDir) score += 10;

			if (score > bestScore) {
				bestScore = score;
				bestDir = d;
			}
		}

		if (bestDir != Game.STAY) return bestDir;
		return anyLegalMove(px, py, b, blockPowerTiles, ghosts, code);
	}

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

	// ===================== BFS helpers =====================
	private int nearestTargetDist(int sx, int sy, int[][] b, int targetValue,
								  GhostCL[] ghosts, int code, boolean blockPowerTiles) {
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

	private int minBfsDistToDangerGhost(int px, int py, int[][] b, GhostCL[] ghosts, int code, boolean blockPowerTiles) {
		int best = Integer.MAX_VALUE;
		if (ghosts == null) return best;

		for (GhostCL g : ghosts) {
			if (g == null) continue;
			if (g.remainTimeAsEatable(code) > 0) continue;
			int[] gp = parseXY(g.getPos(code));
			int gx = wrapX(gp[0], b), gy = wrapY(gp[1], b);
			int d = bfsDist(px, py, gx, gy, b, ghosts, code, blockPowerTiles);
			best = Math.min(best, d);
		}
		return best;
	}

	private int bfsDist(int sx, int sy, int tx, int ty, int[][] b,
						GhostCL[] ghosts, int code, boolean blockPowerTiles) {
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

				// allow measuring distance through regular passable space
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

	private int bfsToNearestValue(int px, int py, int[][] b, int targetValue,
								  boolean blockPowerTiles, GhostCL[] ghosts, int code) {
		if (blockPowerTiles && targetValue == POWER) return Game.STAY;

		int w = b.length, h = b[0].length;
		boolean has = false;
		for (int x = 0; x < w && !has; x++) {
			for (int y = 0; y < h; y++) {
				if (b[x][y] == targetValue) { has = true; break; }
			}
		}
		if (!has) return Game.STAY;

		boolean[][] vis = new boolean[w][h];
		int[][] firstDir = new int[w][h];

		ArrayDeque<int[]> q = new ArrayDeque<>();
		vis[px][py] = true;
		firstDir[px][py] = Game.STAY;
		q.add(new int[]{px, py});

		int[] dirs = {Game.UP, Game.LEFT, Game.DOWN, Game.RIGHT};

		while (!q.isEmpty()) {
			int[] cur = q.poll();
			int cx = cur[0], cy = cur[1];

			for (int d : dirs) {
				int nx = stepX(cx, d, b);
				int ny = stepY(cy, d, b);

				if (vis[nx][ny]) continue;
				if (!passable(nx, ny, b, blockPowerTiles, ghosts, code)) continue;

				vis[nx][ny] = true;
				if (cx == px && cy == py) firstDir[nx][ny] = d;
				else firstDir[nx][ny] = firstDir[cx][cy];

				if (b[nx][ny] == targetValue) return firstDir[nx][ny];
				q.add(new int[]{nx, ny});
			}
		}

		return Game.STAY;
	}

	// ===================== POWER rules =====================
	private boolean canGoForPower(boolean powerMode) {
		if (_count <= TICKS_5_SECONDS) return false;
		if (ticksSinceLastPower < TICKS_5_SECONDS) return false;
		if (powerMode) return false;
		if (farmDotsTicksLeft > 0) return false;
		return true;
	}

	// ===================== OPENING =====================
	private int openingMove(int px, int py, int[][] b, boolean blockPowerTiles, GhostCL[] ghosts, int code) {
		int rx = stepX(px, Game.RIGHT, b), ry = stepY(py, Game.RIGHT, b);
		if (passable(rx, ry, b, blockPowerTiles, ghosts, code)) return Game.RIGHT;

		int lx = stepX(px, Game.LEFT, b), ly = stepY(py, Game.LEFT, b);
		if (passable(lx, ly, b, blockPowerTiles, ghosts, code)) return Game.LEFT;

		return Game.STAY;
	}

	// ===================== WALL detection (stable) =====================
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

		// fallback
		return mostFrequentExcluding(b, DOT, POWER);
	}

	private void addIfWallCandidate(Map<Integer, Integer> freq, int v) {
		if (v == DOT) return;
		if (v == POWER) return;
		if (v == 0) return; // don't pick empty as wall
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

	// ===================== BASIC HELPERS =====================
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

	private int[][] deepCopyBoard(int[][] original) {
		if (original == null) return null;
		int[][] copy = new int[original.length][];
		for (int i = 0; i < original.length; i++) copy[i] = original[i].clone();
		return copy;
	}

	// passable WITH WRAP: x/y are interpreted cyclically
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

	private boolean adjacentToNonEatableGhost(int x, int y, GhostCL[] ghosts, int code) {
		if (ghosts == null) return false;
		for (GhostCL g : ghosts) {
			if (g == null) continue;
			if (g.remainTimeAsEatable(code) > 0) continue;
			int[] gp = parseXY(g.getPos(code));
			int dist = Math.abs(x - gp[0]) + Math.abs(y - gp[1]);
			if (dist == 1) return true;
		}
		return false;
	}

	private static int dx(int dir) {
		if (dir == Game.LEFT) return -1;
		if (dir == Game.RIGHT) return 1;
		return 0;
	}

	private static int dy(int dir) {
		// keep your convention
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

	private int forceDifferentLegal(int px, int py, int[][] b, int avoid, boolean blockPowerTiles, GhostCL[] ghosts, int code) {
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

	private int applyNoReverse(int px, int py, int[][] b, int chosen, boolean blockPowerTiles, GhostCL[] ghosts, int code) {
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

	// ===================== LOOP BREAKER =====================
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

	// ===================== REMEMBER =====================
	private void remember(int px, int py, int dir) {
		lastX = px;
		lastY = py;
		lastDir = dir;
	}

	private static String dirName(int d) {
		if (d == Game.UP) return "UP";
		if (d == Game.DOWN) return "DOWN";
		if (d == Game.LEFT) return "LEFT";
		if (d == Game.RIGHT) return "RIGHT";
		return "STAY";
	}

	// ===================== DEBUG =====================
	private boolean shouldDbg() {
		if (!DEBUG) return false;
		if (stuckCount >= 2) return true;
		return (_count % DBG_EVERY_TICKS) == 0;
	}

	private void dbg(String s) {
		if (DEBUG) System.out.println(s);
	}

	private String cellValStr(int x, int y, int[][] b) {
		int wx = wrapX(x, b);
		int wy = wrapY(y, b);
		int v = b[wx][wy];
		if (v == baseWallValue) return "WALL(" + v + ")";
		if (v == DOT) return "DOT";
		if (v == POWER) return "POWER";
		return "V=" + v;
	}
}
