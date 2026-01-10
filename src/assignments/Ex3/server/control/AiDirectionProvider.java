package assignments.Ex3.server.control;

import assignments.Ex3.model.Direction;
import assignments.Ex3.model.GameState;
import assignments.Ex3.server.ServerEx3Algo;

/**
 * DirectionProvider implementation that uses AI algorithms to control Pac-Man.
 *
 * <p>This provider delegates to {@link ServerEx3Algo} to compute intelligent moves
 * based on the current game state.  The AI uses pathfinding, ghost avoidance, and
 * strategic decision-making to play Pac-Man autonomously.
 *
 * <p>The AI strategy prioritizes:
 * <ol>
 *   <li>Escaping from nearby ghosts</li>
 *   <li>Efficiently collecting dots using BFS shortest paths</li>
 *   <li>Smart timing of power pellet consumption</li>
 *   <li>Avoiding loops and making stable tactical decisions</li>
 * </ol>
 *
 * @author Lidor Ayhoni
 * @version 1.0
 * @since 1.0
 * @see DirectionProvider
 * @see ServerEx3Algo
 */
public class AiDirectionProvider implements DirectionProvider {

    /**
     * The AI algorithm engine that computes Pac-Man's next moves.
     */
    private final ServerEx3Algo algo = new ServerEx3Algo();

    /**
     * Computes the next direction for Pac-Man using AI algorithms.
     *
     * <p>Analyzes the current game state (grid layout, Pac-Man position, ghost positions,
     * remaining dots, power mode status) and returns an optimal or strategic move.
     *
     * @param state the current game state
     * @return the next {@link Direction} for Pac-Man to move
     */
    @Override
    public Direction nextDirection(GameState state) {
        return algo.nextMove(state);
    }
}