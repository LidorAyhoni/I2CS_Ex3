package assignments.Ex3.model;

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
