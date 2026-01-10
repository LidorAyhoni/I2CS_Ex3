package assignments.Ex3.model;

/**
 * Represents a ghost entity in the Pac-Man game.
 *
 * <p>A ghost is an opponent that chases Pac-Man through the game grid.
 * Ghosts have an <code>eatable</code> state that determines whether Pac-Man can consume them
 * (typically when Pac-Man has collected a power pellet).
 *
 * <p>Key characteristics:
 * <ul>
 *   <li>Extends {@link Entity} and therefore has position and direction properties</li>
 *   <li>Can transition between eatable and non-eatable states</li>
 *   <li>Movement is controlled by the game's AI/collision system</li>
 * </ul>
 *
 * @author Lidor Ayhoni
 * @version 1.0
 * @since 1.0
 * @see Entity
 * @see GameState
 */
public class Ghost extends Entity {
    private boolean eatable = false;

    /**
     * Constructs a ghost at the specified grid position.
     *
     * @param x the x-coordinate of the ghost's starting position
     * @param y the y-coordinate of the ghost's starting position
     */
    public Ghost(int x, int y) {
        super(x, y);
    }

    /**
     * Checks if this ghost can currently be eaten by Pac-Man.
     *
     * @return {@code true} if the ghost is in an eatable state; {@code false} otherwise
     */
    public boolean isEatable() { return eatable; }

    /**
     * Sets the eatable state of this ghost.
     *
     * @param v {@code true} to make the ghost eatable; {@code false} otherwise
     */
    public void setEatable(boolean v) { this.eatable = v; }
}