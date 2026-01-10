package assignments.Ex3.server;

import assignments.Ex3.model.Direction;
import exe.ex3.game.StdDraw;

public class InputController {

    private boolean toggleHeld = false;
    private boolean spaceHeld = false;

    public Direction nextDirection() {
        if (StdDraw.isKeyPressed('W') || StdDraw.isKeyPressed(38)) return Direction.UP;
        if (StdDraw.isKeyPressed('S') || StdDraw.isKeyPressed(40)) return Direction.DOWN;
        if (StdDraw.isKeyPressed('A') || StdDraw.isKeyPressed(37)) return Direction.LEFT;
        if (StdDraw.isKeyPressed('D') || StdDraw.isKeyPressed(39)) return Direction.RIGHT;
        return Direction.STAY;
    }
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

    /** Returns true only once per press of 'T' */
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
