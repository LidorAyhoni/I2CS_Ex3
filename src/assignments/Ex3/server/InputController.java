package assignments.Ex3.server;

import assignments.Ex3.model.Direction;
import exe.ex3.game.StdDraw;

/**
 * Handles keyboard input and converts it to game actions.
 *
 * <p>The input controller monitors keyboard state and provides methods to query user intent.
 * It handles state tracking (e.g., key held vs. just pressed) to prevent rapid repeated actions.
 *
 * <p>Supported inputs:
 * <ul>
 *   <li><b>Movement:</b> WASD keys and arrow keys for four directions</li>
 *   <li><b>Start/Pause:</b> Space bar for game start/pause</li>
 *   <li><b>AI Toggle:</b> T key to switch between manual and AI control</li>
 * </ul>
 *
 * @author Lidor Ayhoni
 * @version 1.0
 * @since 1.0
 */
public class InputController {

    private boolean toggleHeld = false;
    private boolean spaceHeld = false;

    /**
     * Gets the next movement direction based on current keyboard input.
     *
     * <p>Priorities (in order):
     * <ol>
     *   <li>W or up arrow: {@link Direction#UP}</li>
     *   <li>S or down arrow: {@link Direction#DOWN}</li>
     *   <li>A or left arrow: {@link Direction#LEFT}</li>
     *   <li>D or right arrow: {@link Direction#RIGHT}</li>
     *   <li>No input: {@link Direction#STAY}</li>
     * </ol>
     *
     * @return the intended movement direction
     */
    public Direction nextDirection() {
        if (StdDraw.isKeyPressed('W') || StdDraw.isKeyPressed(38)) return Direction.UP;
        if (StdDraw.isKeyPressed('S') || StdDraw.isKeyPressed(40)) return Direction.DOWN;
        if (StdDraw.isKeyPressed('A') || StdDraw.isKeyPressed(37)) return Direction.LEFT;
        if (StdDraw.isKeyPressed('D') || StdDraw.isKeyPressed(39)) return Direction.RIGHT;
        return Direction.STAY;
    }

    /**
     * Checks if the space bar was just pressed (not held).
     *
     * <p>This method returns {@code true} only on the first check after the space bar is pressed.
     * Subsequent checks return {@code false} until the key is released and pressed again.
     * This prevents continuous triggering of actions like game start.
     *
     * @return {@code true} if space was just pressed; {@code false} otherwise
     */
    public boolean consumeStart() {
        boolean pressed = exe.ex3.game.StdDraw.isKeyPressed(' ')
                || exe.ex3.game.StdDraw.isKeyPressed(32);

        if (pressed && !spaceHeld) {
            spaceHeld = true;
            return true;
        }
        if (!pressed) {
            spaceHeld = false;
        }
        return false;
    }

    /**
     * Checks if the T key was just pressed to toggle AI mode.
     *
     * <p>Similar to {@link #consumeStart()}, this returns {@code true} only once per key press,
     * preventing rapid repeated toggles while the key is held.
     *
     * @return {@code true} if T was just pressed; {@code false} otherwise
     */
    public boolean consumeToggleAI() {
        boolean pressed = StdDraw.isKeyPressed('T');
        if (pressed && !toggleHeld) {
            toggleHeld = true;
            return true;
        }
        if (!pressed) toggleHeld = false;
        return false;
    }
}