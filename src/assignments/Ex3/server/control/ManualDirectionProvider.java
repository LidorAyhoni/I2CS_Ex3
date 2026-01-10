package assignments.Ex3.server.control;

import assignments.Ex3.model.Direction;
import assignments.Ex3.model.GameState;
import assignments.Ex3.server.InputController;

public class ManualDirectionProvider implements DirectionProvider {

    private final InputController input;

    public ManualDirectionProvider(InputController input) {
        this.input = input;
    }

    @Override
    public Direction nextDirection(GameState state) {
        Direction d = input.nextDirection();
        return d == Direction.STAY ? null : d;
    }
}
