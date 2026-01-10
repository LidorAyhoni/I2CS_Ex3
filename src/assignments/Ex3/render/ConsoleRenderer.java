package assignments.Ex3.render;

import assignments.Ex3.model.GameState;
import assignments.Ex3.model.Tile;

/**
 * A simple text-based renderer that outputs the game state to the console.
 *
 * <p>This renderer is useful for debugging and testing on systems without graphical support.
 * It displays the game grid using ASCII characters and prints the score above the grid.
 *
 * <p>Character mapping:
 * <ul>
 *   <li><b>P</b>:  Pac-Man at current position</li>
 *   <li><b>#</b>: Walls (impassable obstacles)</li>
 *   <li><b>. </b>:  Dots (collectible pellets)</li>
 *   <li><b>o</b>: Power pellets (grant temporary ghost-eating ability)</li>
 *   <li><b> </b> (space): Empty tiles</li>
 * </ul>
 *
 * <p>The grid is displayed with y-axis inverted (top of output = highest y-coordinate)
 * to match typical console orientation.
 *
 * @author Lidor Ayhoni
 * @version 1.0
 * @since 1.0
 * @see Renderer
 */
public class ConsoleRenderer implements Renderer {

    /**
     * Initializes the console renderer.
     *
     * <p>This method does nothing for console rendering as no setup is required.
     * It exists to implement the {@link Renderer} interface contract.
     *
     * @param pixels ignored for console output
     * @param gridW ignored for console output
     * @param gridH ignored for console output
     */
    @Override
    public void init(int pixels, int gridW, int gridH) {
    }

    /**
     * Renders the game state to the console.
     *
     * <p>Output format:
     * <ol>
     *   <li>First line displays current score</li>
     *   <li>Following lines show the game grid with ASCII characters</li>
     *   <li>Blank lines are printed before output for visual separation</li>
     * </ol>
     *
     * @param s the game state to render
     */
    @Override
    public void render(GameState s) {
        StringBuilder sb = new StringBuilder();
        sb.append("score=").append(s.score).append("\n");

        for (int y = s.h - 1; y >= 0; y--) {
            for (int x = 0; x < s.w; x++) {
                if (x == s.pacX && y == s.pacY) {
                    sb.append('P');
                    continue;
                }
                Tile t = s.grid[x][y];
                char c = switch (t) {
                    case WALL -> '#';
                    case DOT -> '.';
                    case POWER -> 'o';
                    case EMPTY -> ' ';
                };
                sb.append(c);
            }
            sb.append('\n');
        }

        System.out.println("\n\n\n" + sb);
    }
}