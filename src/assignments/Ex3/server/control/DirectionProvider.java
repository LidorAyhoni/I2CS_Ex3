package assignments.Ex3.server.control;

import assignments.Ex3.model.Direction;
import assignments.Ex3.model.GameState;

public interface DirectionProvider {
    Direction nextDirection(GameState state);
}
