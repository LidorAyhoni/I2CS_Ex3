package assignments.Ex3.model;

/**
 * Abstract base class representing a movable entity in the game world.
 *
 * <p>This class encapsulates the core properties shared by all entities (Pac-Man, ghosts, etc.):
 * <ul>
 *   <li><b>Position</b>:  Coordinates (x, y) in the game grid</li>
 *   <li><b>Direction</b>: Current facing/movement direction</li>
 * </ul>
 *
 * <p>Subclasses must implement specific behavior such as movement logic or rendering.
 * The position and direction can be modified during gameplay to reflect entity movement.
 *
 * @author Lidor Ayhoni
 * @version 1.0
 * @since 1.0
 * @see Ghost
 */
public abstract class Entity {
    protected int x, y;
    protected Direction dir = Direction.STAY;

    protected Entity(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public final int x() { return x; }
    public final int y() { return y; }
    public final Direction dir() { return dir; }

    public final void setDir(Direction d) { this.dir = d; }
    public final void setPos(int x, int y) { this.x = x; this.y = y; }
}