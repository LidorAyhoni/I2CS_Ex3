package assignments.Ex3.model;

import assignments.Ex3.Index2D;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
/**
 * Represents the complete state of an active Pac-Man game session.
 *
 * <p>This class maintains all game data and logic:
 * <ul>
 *   <li><b>Game grid: </b> 2D tile map defining walls, dots, and power pellets</li>
 *   <li><b>Entities:</b> Pac-Man position/state and all active ghosts</li>
 *   <li><b>Game flow:</b> Score, lives, game-over status, and AI mode toggle</li>
 *   <li><b>Power mode:</b> Temporary ghost-eating capability with countdown timer</li>
 *   <li><b>Deterministic reset:</b> Spawn points for respawning entities</li>
 * </ul>
 *
 * <p>The game state serves as the central hub for game logic and is accessed by the
 * game loop, collision system, and AI algorithms.
 *
 * @author Lidor Ayhoni
 * @version 1.0
 * @since 1.0
 * @see Ghost
 * @see Direction
 * @see Tile
 */
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

    public Direction pacDir = Direction.LEFT; //Or STAY
    public boolean aiEnabled = false;
    /**
     * Constructs a game state with a given tile grid and initial Pac-Man position.
     *
     * <p>The initial state has:
     * <ul>
     *   <li>3 lives</li>
     *   <li>0 score</li>
     *   <li>Not in power mode</li>
     *   <li>Pac-Man not marked as done</li>
     *   <li>No ghosts (must be added via {@link #addGhost(Ghost)})</li>
     * </ul>
     *
     * @param grid the 2D tile map defining the game world (must be non-null and non-empty)
     * @param pacX the initial x-coordinate of Pac-Man (and spawn point)
     * @param pacY the initial y-coordinate of Pac-Man (and spawn point)
     * @throws IllegalArgumentException if grid is null, empty, or has zero-length rows
     */
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
    /**
     * Gets Pac-Man's current x-coordinate.
     * @return the x-coordinate of Pac-Man
     */
    public int getPacmanX() { return pacX; }
    /**
     * Gets Pac-Man's current y-coordinate.
     * @return the y-coordinate of Pac-Man
     */
    public int getPacmanY() { return pacY; }
    /**
     * Gets the current game score.
     * @return the total score accumulated during gameplay
     */
    public int getScore() { return score; }
    /**
     * Checks if the game is over.
     * @return {@code true} if the game has ended; {@code false} otherwise
     */
    public boolean isDone() { return done; }
    /**
     * Gets the number of remaining lives.
     * @return the current number of lives (0 if game over)
     */
    public int getLives() { return lives; }

    // ---- Helpers ----
    /**
     * Checks if the given coordinates are within the game grid boundaries.
     *
     * @param x the x-coordinate to check
     * @param y the y-coordinate to check
     * @return {@code true} if the position is within bounds; {@code false} otherwise
     */
    public boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < w && y < h;
    }
    /**
     * Checks if a position is blocked (wall or out of bounds).
     *
     * <p>This method is used to validate movement and collision detection.
     *
     * @param x the x-coordinate to check
     * @param y the y-coordinate to check
     * @return {@code true} if the position contains a wall or is out of bounds; {@code false} otherwise
     */
    public boolean isWall(int x, int y) {
        return !inBounds(x, y) || grid[x][y] == Tile.WALL;
    }

    // ---- Score / lives API ----
    /**
     * Adds points to the score.
     *
     * @param delta the number of points to add (only positive values are added)
     */
    public void addScore(int delta) {
        if (delta > 0) score += delta;
    }
    /**
     * Removes one life from the player.
     *
     * <p>If lives reach 0, the game is automatically marked as done.
     */
    public void loseLife() {
        if (done) return;
        lives = Math.max(0, lives - 1);
        if (lives == 0) done = true;
    }

    // ---- Ghosts API ----
    /**
     * Gets an unmodifiable list of all active ghosts.
     *
     * @return a list of all ghosts in the game
     */
    public List<Ghost> getGhosts() {
        return Collections.unmodifiableList(ghosts);
    }

    /**
     * Registers a ghost and its spawn point for the game.
     *
     * <p>The ghost's initial position is recorded as its spawn point for later resets.
     * The ghost must not be placed on a wall.
     *
     * @param g the ghost to add
     * @throws IllegalArgumentException if g is null or placed on a wall/out of bounds
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
    /**
     * Resets all entities to their spawn points.
     *
     * <p>This is typically called when Pac-Man loses a life (unless game over).
     * Both Pac-Man and all ghosts are returned to their original positions.
     */
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
            g.setEatable(isPowerMode());
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
                g.setEatable(isPowerMode());
                g.setDir(Direction.STAY);
                return;
            }
        }

        // Fallback: still ensure it's not eatable and doesn't keep direction
        g.setEatable(isPowerMode());
        g.setDir(Direction.STAY);
    }

    /**
     * Handles collision between Pac-Man and a ghost.
     *
     * <p>Resolution depends on whether the ghost is eatable:
     * <ul>
     *   <li>If eatable: the ghost is removed from the game and points are awarded</li>
     *   <li>If not eatable:  Pac-Man loses a life; if lives reach 0, the game ends</li>
     * </ul>
     *
     * @param g the ghost that collided with Pac-Man
     */
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
    private static void spawnGhostsNearPacman(GameState s, int count) {
        int px = s.getPacmanX();
        int py = s.getPacmanY();

        int added = 0;
        int radius = 1;

        while (added < count && radius < Math.max(s.w, s.h)) {
            for (int dx = -radius; dx <= radius && added < count; dx++) {
                for (int dy = -radius; dy <= radius && added < count; dy++) {
                    int x = px + dx;
                    int y = py + dy;

                    if (dx == 0 && dy == 0) continue;          // not on pacman
                    if (!s.inBounds(x, y)) continue;
                    if (s.isWall(x, y)) continue;

                    // avoid placing two ghosts on same cell
                    boolean occupied = false;
                    for (Ghost g : s.getGhosts()) {
                        if (g.x() == x && g.y() == y) { occupied = true; break; }
                    }
                    if (occupied) continue;

                    s.addGhost(new Ghost(x, y));
                    added++;
                }
            }
            radius++;
        }

        if (added < count) {
            throw new IllegalStateException("Could not find enough free cells to spawn ghosts");
        }
    }
    /**
     * Activates power mode for a specified duration.
     *
     * <p>During power mode, ghosts become eatable and Pac-Man gains temporary invulnerability.
     *
     * @param ticks the number of ticks the power mode lasts
     */
    public void activatePower(int ticks) {
        if (ticks <= 0) return;

        powerTicksLeft = Math.max(powerTicksLeft, ticks);

        for (Ghost g : ghosts) {
            g.setEatable(true);
        }
    }
    /**
     * Advances power mode timer by one tick.
     *
     * <p>When the power timer expires, ghosts revert to non-eatable state.
     * Typically called once per game loop iteration.
     */
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