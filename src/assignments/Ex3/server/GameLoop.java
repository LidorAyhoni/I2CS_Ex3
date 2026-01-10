package assignments.Ex3.server;

import assignments.Ex3.model.Direction;
import assignments.Ex3.model.GameState;
import assignments.Ex3.model.Tile;
import assignments.Ex3.render.Renderer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import assignments.Ex3.model.Direction;

public class GameLoop {
    private final GameState s;
    private final Renderer renderer;
    private final int dtMs;

    private final Random rnd = new Random();
    private final InputController input;

    public GameLoop(GameState s, Renderer renderer, InputController input, int dtMs) {
        this.s = s;
        this.renderer = renderer;
        this.input = input;
        this.dtMs = dtMs;
    }


    public void run() {
        int steps = 0;
        int maxSteps = 20_000; // מספיק גדול

        while (!s.done && steps < maxSteps) {
            Direction d = input.nextDirection();
            stepPacman(d);
            renderer.render(s);

            if (!hasDotsLeft()) s.done = true;

            steps++;
            sleep(dtMs);
        }

        if (!s.done) {
            System.out.println("Stopped by maxSteps (debug safety). score=" + s.score);
            s.done = true;
        }
    }


    private Direction chooseAutoDirection() {
        List<Direction> opts = new ArrayList<>(4);

        for (Direction d : new Direction[]{Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT}) {
            int nx = s.pacX + d.dx;
            int ny = s.pacY + d.dy;
            if (!s.isWall(nx, ny)) opts.add(d);
        }

        if (opts.isEmpty()) return Direction.STAY;
        return opts.get(rnd.nextInt(opts.size()));
    }

    private void stepPacman(Direction d) {
        int nx = s.pacX + d.dx;
        int ny = s.pacY + d.dy;

        if (!s.isWall(nx, ny)) {
            s.pacX = nx;
            s.pacY = ny;

            if (s.grid[nx][ny] == Tile.DOT) {
                s.grid[nx][ny] = Tile.EMPTY;
                s.score += 10;
            } else if (s.grid[nx][ny] == Tile.POWER) {
                s.grid[nx][ny] = Tile.EMPTY;
                s.score += 50;
            }
        }
    }

    private boolean hasDotsLeft() {
        for (int x = 0; x < s.w; x++) {
            for (int y = 0; y < s.h; y++) {
                Tile t = s.grid[x][y];
                if (t == Tile.DOT || t == Tile.POWER) return true;
            }
        }
        return false;
    }

    private static void sleep(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
