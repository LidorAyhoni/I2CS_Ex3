package assignments.Ex3.model;

import assignments.Ex3.Index2D;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameState {
    public final int w, h;
    public final Tile[][] grid;

    // Pac-Man position (kept public because you already use it; provide getters anyway)
    public int pacX, pacY;

    public int score = 0;
    public boolean done = false;

    // Lives
    private int lives = 3;

    // Spawn points (deterministic reset)
    private final int pacSpawnX, pacSpawnY;

    // Ghosts + their spawns (same index)
    private final List<Ghost> ghosts = new ArrayList<>();
    private final List<Index2D> ghostSpawns = new ArrayList<>();

    // --- Power mode ---
    private int powerTicksLeft = 0;


    public GameState(Tile[][] grid, int pacX, int pacY) {
        if (grid == null || grid.length == 0 || grid[0].length == 0) {
            throw new IllegalArgumentException("grid is null/empty");
        }
        this.grid = grid;
        this.w = grid.length;
        this.h = grid[0].length;

        this.pacX = pacX;
        this.pacY = pacY;

        this.pacSpawnX = pacX;
        this.pacSpawnY = pacY;
    }

    // ---- Getters (small, useful, keeps other classes clean) ----
    public int getPacmanX() { return pacX; }
    public int getPacmanY() { return pacY; }
    public int getScore() { return score; }
    public boolean isDone() { return done; }
    public int getLives() { return lives; }

    // ---- Helpers ----
    public boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < w && y < h;
    }

    public boolean isWall(int x, int y) {
        return !inBounds(x, y) || grid[x][y] == Tile.WALL;
    }

    // ---- Score / lives API ----
    public void addScore(int delta) {
        if (delta > 0) score += delta;
    }

    public void loseLife() {
        if (done) return;
        lives = Math.max(0, lives - 1);
        if (lives == 0) done = true;
    }

    // ---- Ghosts API ----
    public List<Ghost> getGhosts() {
        return Collections.unmodifiableList(ghosts);
    }

    /**
     * Call from LevelLoader (or manually).
     * Registers spawn for deterministic reset/respawn.
     */
    public void addGhost(Ghost g) {
        if (g == null) {
            throw new IllegalArgumentException("ghost is null");
        }
        if (isWall(g.x(), g.y())) {
            throw new IllegalArgumentException("ghost spawn is on wall/out of bounds: (" + g.x() + "," + g.y() + ")");
        }
        ghosts.add(g);
        ghostSpawns.add(new Index2D(g.x(), g.y()));
    }

    // ---- Reset/respawn ----
    public void resetPositions() {
        if (done) return;

        // Pac-Man back to spawn
        pacX = pacSpawnX;
        pacY = pacSpawnY;

        // Ghosts back to their spawn points, not eatable
        for (int i = 0; i < ghosts.size(); i++) {
            Ghost g = ghosts.get(i);
            Index2D sp = ghostSpawns.get(i);
            g.setPos(sp.getX(), sp.getY());
            g.setEatable(false);
            g.setDir(Direction.STAY);
        }
    }

    public void respawnGhost(Ghost g) {
        if (g == null) return;

        // Put ghost on its original spawn if registered
        for (int i = 0; i < ghosts.size(); i++) {
            if (ghosts.get(i) == g) {
                Index2D sp = ghostSpawns.get(i);
                g.setPos(sp.getX(), sp.getY());
                g.setEatable(false);
                g.setDir(Direction.STAY);
                return;
            }
        }

        // Fallback: still ensure it's not eatable and doesn't keep direction
        g.setEatable(false);
        g.setDir(Direction.STAY);
    }

    // ---- Collision hook (single source of truth for rules) ----
    public void onPacmanGhostCollision(Ghost g) {
        if (done) return;

        if (g != null && g.isEatable()) {
            addScore(200);
            respawnGhost(g);
        } else {
            loseLife();
            if (!done) resetPositions();
        }
    }
    public void activatePower(int ticks) {
        if (ticks <= 0) return;

        // אם כבר בפאוור – מאריכים, לא מקצרים
        powerTicksLeft = Math.max(powerTicksLeft, ticks);

        for (Ghost g : ghosts) {
            g.setEatable(true);
        }
    }
    public void tickPower() {
        if (powerTicksLeft <= 0) return;

        powerTicksLeft--;

        if (powerTicksLeft == 0) {
            for (Ghost g : ghosts) {
                g.setEatable(false);
            }
        }
    }
    public boolean isPowerMode() {
        return powerTicksLeft > 0;
    }

}
