package assignments.Ex3.server.control;

import assignments.Ex3.model.Direction;
import assignments.Ex3.model.GameState;
import assignments.Ex3.server.InputController;

/**
 * DirectionProvider implementation that reads player keyboard input.
 *
 * <p>This provider allows human players to control Pac-Man via keyboard input,
 * translating keyboard presses into movement directions.
 *
 * <p>Keyboard controls:
 * <ul>
 *   <li><b>W or ↑</b>: Move up</li>
 *   <li><b>S or ↓</b>: Move down</li>
 *   <li><b>A or ←</b>: Move left</li>
 *   <li><b>D or →</b>: Move right</li>
 * </ul>
 *
 * <p>Returns {@code null} when no input is detected, allowing Pac-Man to maintain
 * its current direction.
 *
 * @author Lidor Ayhoni
 * @version 1.0
 * @since 1.0
 * @see DirectionProvider
 * @see InputController
 */
public class ManualDirectionProvider implements DirectionProvider {

    /**
     * The input controller that reads keyboard state.
     */
    private final InputController input;

    /**
     * Constructs a manual direction provider with a given input controller.
     *
     * @param input the input controller to read keyboard commands from
     */
    public ManualDirectionProvider(InputController input) {
        this.input = input;
    }

    /**
     * Gets the next direction based on current keyboard input.
     *
     * <p>Reads the player's keyboard input and returns the corresponding direction.
     * Returns {@code null} if no directional key is pressed, preserving Pac-Man's
     * current movement direction.
     *
     * @param state the current game state (unused for manual control)
     * @return the direction from keyboard input, or null if no input detected
     */
    @Override
    public Direction nextDirection(GameState state) {
        Direction d = input.nextDirection();
        return d == Direction.STAY ? null : d;
    }
}