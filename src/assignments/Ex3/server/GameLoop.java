package assignments.Ex3.server;

import assignments.Ex3.model.*;
import assignments.Ex3.render.Renderer;
import assignments.Ex3.server.control.DirectionProvider;
import assignments.Ex3.server.control.*;


public class GameLoop {

    private final GameState s;
    private final Renderer renderer;
    private final DirectionProvider provider;
    private final InputController input;
    private final int dtMs;

    private final GhostMovement ghostMovement = new GhostMovement();
    private final CollisionSystem collisionSystem = new CollisionSystem();


    public GameLoop(GameState s, Renderer renderer, DirectionProvider provider) {
        this(s, renderer, provider, null, 80);
    }

    public GameLoop(GameState s, Renderer renderer, DirectionProvider provider, InputController input) {
        this(s, renderer, provider, input, 80);
    }

    public GameLoop(GameState s, Renderer renderer, DirectionProvider provider, InputController input, int dtMs) {
        this.s = s;
        this.renderer = renderer;
        this.provider = provider;
        this.input = input;
        this.dtMs = dtMs;
    }


    public void run() {
        int steps = 0;
        int maxSteps = 20_000;

        while (!s.done && steps < maxSteps) {

            // Toggle AI with T
            if (input != null && provider instanceof ToggleDirectionProvider tdp) {
                if (input.consumeToggleAI()) {
                    tdp.toggle();
                    s.aiEnabled = tdp.isAiEnabled();
                    System.out.println("AI mode: " + (tdp.isAiEnabled() ? "ON" : "OFF"));
                }
            }

            Direction nd = provider.nextDirection(s);
            if (nd != null) s.pacDir = nd;

            stepPacman(s.pacDir);
            collisionSystem.resolve(s); // ✅ collision after pacman move
            if (s.done) break;          // optional safety

            moveGhosts();
            collisionSystem.resolve(s); // ✅ collision after ghosts move
            s.tickPower();

            if (!hasDotsLeft()) {
                s.done = true;
            }

            renderer.render(s);

            steps++;
            sleep(dtMs);
        }

        if (!s.done) {
            System.out.println("Stopped by maxSteps (debug safety). score=" + s.score);
            s.done = true;
        }
    }


    private void stepPacman(Direction d) {
        int nx = s.pacX + d.dx;
        int ny = s.pacY + d.dy;

        if (!s.isWall(nx, ny)) {
            s.pacX = nx;
            s.pacY = ny;

            Tile t = s.grid[nx][ny];
            if (t == Tile.DOT) {
                s.grid[nx][ny] = Tile.EMPTY;
                s.addScore(10);
            } else if (t == Tile.POWER) {
                s.grid[nx][ny] = Tile.EMPTY;
                s.addScore(50);
                s.activatePower(80);
            }
        }
    }

    private void moveGhosts() {
        for (Ghost g : s.getGhosts()) {
            Direction next = ghostMovement.chooseNext(g, s);
            g.setDir(next);

            int nx = g.x() + next.dx;
            int ny = g.y() + next.dy;

            if (!s.isWall(nx, ny)) {
                g.setPos(nx, ny);
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
