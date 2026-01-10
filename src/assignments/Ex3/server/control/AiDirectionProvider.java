package assignments.Ex3.server.control;

import assignments.Ex3.model.Direction;
import assignments.Ex3.model.GameState;
import assignments.Ex3.server.ServerEx3Algo;

public class AiDirectionProvider implements DirectionProvider {

    private final ServerEx3Algo algo = new ServerEx3Algo();

    @Override
    public Direction nextDirection(GameState state) {
        return algo.nextMove(state);
    }
}
