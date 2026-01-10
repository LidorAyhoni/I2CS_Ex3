package assignments.Ex3.server.control;

import assignments.Ex3.model.Direction;
import assignments.Ex3.model.GameState;

/**
 * Interface for providing direction commands to Pac-Man.
 *
 * <p>A DirectionProvider abstracts the source of movement decisions, allowing:
 * <ul>
 *   <li><b>Manual control: </b> Reading player input from the keyboard</li>
 *   <li><b>AI control:</b> Computing optimal moves via pathfinding algorithms</li>
 *   <li><b>Hybrid control:</b> Switching between manual and AI modes</li>
 * </ul>
 *
 * <p>Different implementations can provide different strategies for deciding Pac-Man's movement.
 *
 * @author Lidor Ayhoni
 * @version 1.0
 * @since 1.0
 * @see ToggleDirectionProvider
 */
public interface DirectionProvider {
    /**
     * Determines the next movement direction for Pac-Man.
     *
     * <p>This method is called once per game loop iteration to get Pac-Man's desired direction.
     * The implementation may use the game state (grid, ghost positions, etc.) to make intelligent decisions.
     *
     * @param state the current game state
     * @return the next direction for Pac-Man to move, or null to maintain current direction
     */
    Direction nextDirection(GameState state);
}