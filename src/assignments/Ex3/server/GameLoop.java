package assignments.Ex3.server;

import assignments.Ex3.model.*;
import assignments.Ex3.render.Renderer;
import assignments.Ex3.server.control.DirectionProvider;
import assignments.Ex3.server.control.*;

/**
 * The main game loop that orchestrates game flow and updates.
 *
 * <p>The GameLoop coordinates all game systems:
 * <ul>
 *   <li><b>Input handling: </b> Reads player commands via DirectionProvider</li>
 *   <li><b>Entity movement:</b> Updates Pac-Man and ghost positions</li>
 *   <li><b>Collision detection:</b> Checks collisions after each entity movement</li>
 *   <li><b>Game logic:</b> Manages power mode, scoring, lives, and win/loss conditions</li>
 *   <li><b>Rendering: </b> Updates the visual display</li>
 *   <li><b>Timing:</b> Maintains consistent frame rate via sleep</li>
 * </ul>
 *
 * <p>The game loop runs until the game is marked as done or maxSteps is reached.
 * It ensures deterministic behavior by processing entities in a consistent order each frame.
 *
 * @author Lidor Ayhoni
 * @version 1.0
 * @since 1.0
 * @see GameState
 * @see Renderer
 * @see DirectionProvider
 * @see CollisionSystem
 * @see GhostMovement
 */
public class GameLoop {

    private final GameState s;
    private final Renderer renderer;
    private final DirectionProvider provider;
    private final InputController input;
    private final int dtMs;

    private final GhostMovement ghostMovement = new GhostMovement();
    private final CollisionSystem collisionSystem = new CollisionSystem();

    /**
     * Constructs a game loop with default parameters.
     *
     * <p>Uses default frame time of 80ms and no keyboard input.
     * Suitable for AI-only or purely provider-based control.
     *
     * @param s the game state to manage
     * @param renderer the renderer for visual output
     * @param provider the direction provider for Pac-Man control
     */
    public GameLoop(GameState s, Renderer renderer, DirectionProvider provider) {
        this(s, renderer, provider, null, 80);
    }

    /**
     * Constructs a game loop with keyboard input support.
     *
     * <p>Uses default frame time of 80ms.
     *
     * @param s the game state to manage
     * @param renderer the renderer for visual output
     * @param provider the direction provider for Pac-Man control
     * @param input the input controller for keyboard handling
     */
    public GameLoop(GameState s, Renderer renderer, DirectionProvider provider, InputController input) {
        this(s, renderer, provider, input, 80);
    }

    /**
     * Constructs a game loop with all parameters.
     *
     * @param s the game state to manage
     * @param renderer the renderer for visual output
     * @param provider the direction provider for Pac-Man control
     * @param input the input controller (may be null)
     * @param dtMs the frame time in milliseconds
     */
    public GameLoop(GameState s, Renderer renderer, DirectionProvider provider, InputController input, int dtMs) {
        this.s = s;
        this.renderer = renderer;
        this.provider = provider;
        this.input = input;
        this.dtMs = dtMs;
    }

    /**
     * Runs the main game loop.
     *
     * <p>Game loop sequence per iteration:
     * <ol>
     *   <li>Check for AI toggle input (if available)</li>
     *   <li>Get next direction from provider</li>
     *   <li>Move Pac-Man and handle dot/power collection</li>
     *   <li>Resolve collisions with ghosts</li>
     *   <li>Move all ghosts</li>
     *   <li>Resolve collisions again</li>
     *   <li>Update power mode timer</li>
     *   <li>Check win condition (all dots eaten)</li>
     *   <li>Render game state</li>
     *   <li>Sleep for frame timing</li>
     * </ol>
     *
     * <p>The loop terminates when the game is marked done or after 20,000 steps (safety limit).
     */
    public void run() {
        int steps = 0;
        int maxSteps = 20_000;

        while (!s.done && steps < maxSteps) {

            // Toggle AI with T
            if (input != null && provider instanceof ToggleDirectionProvider tdp) {
                if (input.consumeToggleAI()) {
                    tdp.toggle();
                    s.aiEnabled = tdp.isAiEnabled();
                    System.out.println("AI mode: " + (tdp.isAiEnabled() ? "ON" : "OFF"));
                }
            }

            Direction nd = provider.nextDirection(s);
            if (nd != null) s.pacDir = nd;

            stepPacman(s.pacDir);
            collisionSystem.resolve(s);
            if (s.done) break;

            moveGhosts();
            collisionSystem.resolve(s);
            s.tickPower();

            if (!hasDotsLeft()) {
                s.done = true;
            }

            renderer.render(s);

            steps++;
            sleep(dtMs);
        }

        if (!s.done) {
            System.out.println("Stopped by maxSteps (debug safety). score=" + s.score);
            s.done = true;
        }
    }

    /**
     * Moves Pac-Man one step in the given direction.
     *
     * <p>If the target cell is not a wall:
     * <ul>
     *   <li>Updates Pac-Man's position</li>
     *   <li>If the cell contains a DOT:  removes it and adds 10 points</li>
     *   <li>If the cell contains a POWER: removes it, adds 50 points, and activates power mode</li>
     * </ul>
     *
     * @param d the direction to move Pac-Man
     */
    private void stepPacman(Direction d) {
        int nx = s.pacX + d.dx;
        int ny = s.pacY + d.dy;

        if (!s.isWall(nx, ny)) {
            s.pacX = nx;
            s.pacY = ny;

            Tile t = s.grid[nx][ny];
            if (t == Tile.DOT) {
                s.grid[nx][ny] = Tile.EMPTY;
                s.addScore(10);
            } else if (t == Tile.POWER) {
                s.grid[nx][ny] = Tile.EMPTY;
                s.addScore(50);
                s.activatePower(80);
            }
        }
    }

    /**
     * Moves all ghosts one step based on their movement logic.
     *
     * <p>For each ghost:
     * <ol>
     *   <li>Computes next direction using GhostMovement AI</li>
     *   <li>Updates ghost's facing direction</li>
     *   <li>Moves ghost to new position if not blocked by wall</li>
     * </ol>
     */
    private void moveGhosts() {
        for (Ghost g : s.getGhosts()) {
            Direction next = ghostMovement.chooseNext(g, s);
            g.setDir(next);

            int nx = g.x() + next.dx;
            int ny = g.y() + next.dy;

            if (!s.isWall(nx, ny)) {
                g.setPos(nx, ny);
            }
        }
    }

    /**
     * Checks if any dots remain on the map.
     *
     * @return {@code true} if at least one DOT tile exists; {@code false} if all dots are eaten
     */
    private boolean hasDotsLeft() {
        for (int x = 0; x < s.w; x++) {
            for (int y = 0; y < s.h; y++) {
                if (s.grid[x][y] == Tile.DOT) {
                    return true;
                }
            }
        }
        return false;
    }
    /**
     * Pauses execution for the specified number of milliseconds.
     *
     * <p>Used to maintain consistent frame rate.  Exceptions are silently caught.
     *
     * @param ms the number of milliseconds to sleep
     */
    private static void sleep(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
