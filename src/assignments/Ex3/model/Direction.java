package assignments.Ex3.model;

import exe.ex3.game.Game;

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

    public static Direction fromGameDir(int d) {
        for (Direction dir : values()) if (dir.gameDir == d) return dir;
        return STAY;
    }
}
