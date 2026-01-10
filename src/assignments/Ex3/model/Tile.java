package assignments.Ex3.model;

/**
 * Enumeration representing the different types of tiles that can appear in a Pac-Man game grid.
 *
 * <p>Each tile type has specific properties and effects in the game:
 * <ul>
 *   <li><b>WALL</b>: An obstacle that blocks movement for both Pac-Man and ghosts</li>
 *   <li><b>EMPTY</b>: An empty cell with no items, freely passable</li>
 *   <li><b>DOT</b>: A small pellet that Pac-Man can eat for points</li>
 *   <li><b>POWER</b>: A power pellet that grants Pac-Man temporary ghost-eating abilities</li>
 * </ul>
 *
 * @author Lidor Ayhoni
 * @version 1.0
 * @since 1.0
 */
public enum Tile {
    WALL, EMPTY, DOT, POWER
}