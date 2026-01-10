package assignments.Ex3.server.control;

import assignments.Ex3.model.Direction;
import assignments.Ex3.model.GameState;
import assignments.Ex3.server.PacmanGameImpl;
import exe.ex3.game.Game;
import exe.ex3.game.PacManAlgo;

public class AiDirectionProvider implements DirectionProvider {

    private final PacManAlgo algo;
    private final PacmanGameImpl adapter;

    public AiDirectionProvider(PacManAlgo algo, PacmanGameImpl adapter) {
        this.algo = algo;
        this.adapter = adapter;
    }

    @Override
    public Direction nextDirection(GameState state) {
        adapter.bind(state);

        int dir = algo.move(adapter);

        if (dir == Game.UP) return Direction.UP;
        if (dir == Game.DOWN) return Direction.DOWN;
        if (dir == Game.LEFT) return Direction.LEFT;
        if (dir == Game.RIGHT) return Direction.RIGHT;

        return null;
    }
}
