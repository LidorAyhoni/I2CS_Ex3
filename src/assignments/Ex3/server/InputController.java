package assignments.Ex3.server;

import assignments.Ex3.model.Direction;
import exe.ex3.game.StdDraw;

public class InputController {

    public Direction nextDirection() {
        if (StdDraw.isKeyPressed('W') || StdDraw.isKeyPressed(38)) return Direction.UP;
        if (StdDraw.isKeyPressed('S') || StdDraw.isKeyPressed(40)) return Direction.DOWN;
        if (StdDraw.isKeyPressed('A') || StdDraw.isKeyPressed(37)) return Direction.LEFT;
        if (StdDraw.isKeyPressed('D') || StdDraw.isKeyPressed(39)) return Direction.RIGHT;
        return Direction.STAY;
    }
}
