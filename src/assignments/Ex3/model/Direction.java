package assignments.Ex3.model;

import exe.ex3.game.Game;

/**
 * Enumeration representing the four cardinal directions (and stillness) for entity movement.
 *
 * <p>Each direction encapsulates:
 * <ul>
 *   <li><b>gameDir</b>: The corresponding game library direction constant</li>
 *   <li><b>dx</b>:  Horizontal displacement (change in x coordinate)</li>
 *   <li><b>dy</b>: Vertical displacement (change in y coordinate)</li>
 * </ul>
 *
 * <p>Direction values:
 * <ul>
 *   <li><b>UP</b>: Move up; dx=0, dy=1</li>
 *   <li><b>DOWN</b>: Move down; dx=0, dy=-1</li>
 *   <li><b>LEFT</b>: Move left; dx=-1, dy=0</li>
 *   <li><b>RIGHT</b>: Move right; dx=1, dy=0</li>
 *   <li><b>STAY</b>: Remain in place; dx=0, dy=0</li>
 * </ul>
 *
 * @author Lidor Ayhoni
 * @version 1.0
 * @since 1.0
 */
public enum Direction {
    UP(Game.UP, 0, 1),
    DOWN(Game.DOWN, 0, -1),
    LEFT(Game.LEFT, -1, 0),
    RIGHT(Game.RIGHT, 1, 0),
    STAY(Game.STAY, 0, 0);

    public final int gameDir;
    public final int dx, dy;

    Direction(int gameDir, int dx, int dy) {
        this.gameDir = gameDir;
        this.dx = dx;
        this.dy = dy;
    }
}