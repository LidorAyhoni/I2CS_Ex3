package assignments.Ex3.model;

public class Ghost extends Entity {
    private boolean eatable = false;

    public Ghost(int x, int y) {
        super(x, y);
    }

    public boolean isEatable() { return eatable; }
    public void setEatable(boolean v) { this.eatable = v; }
}
