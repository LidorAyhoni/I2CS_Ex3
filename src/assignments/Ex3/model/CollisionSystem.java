package assignments.Ex3.model;

/**
 * Handles collision detection and resolution between Pac-Man and ghosts.
 *
 * <p>This system checks if Pac-Man occupies the same grid position as any ghost,
 * and if so, delegates the collision handling to the game state.
 * It is called after both Pac-Man and ghost movements to ensure accurate collision detection.
 *
 * <p>Collision resolution includes:
 * <ul>
 *   <li>Checking if the ghost is eatable (in power mode)</li>
 *   <li>Removing the ghost from play if eaten</li>
 *   <li>Reducing Pac-Man's lives if hit by a non-eatable ghost</li>
 * </ul>
 *
 * @author Lidor Ayhoni
 * @version 1.0
 * @since 1.0
 * @see GameState
 */
public class CollisionSystem {

    /**
     * Resolves collisions between Pac-Man and all ghosts in the game state.
     *
     * <p>This method checks if Pac-Man's current position matches any ghost's position.
     * If a collision is detected, it immediately delegates the resolution to
     * {@link GameState#onPacmanGhostCollision(Ghost)} and returns.
     *
     * @param s the current game state containing Pac-Man position and all ghosts
     * @see GameState#onPacmanGhostCollision(Ghost)
     */
    public void resolve(GameState s) {
        int px = s.pacX;
        int py = s.pacY;

        for (Ghost g : s.getGhosts()) {
            if (g.x() == px && g.y() == py) {
                s.onPacmanGhostCollision(g);
                return;
            }
        }
    }
}