package assignments.Ex3.server;

import assignments.Ex3.model.GameState;
import assignments.Ex3.model.Ghost;
import assignments.Ex3.model.Tile;
import exe.ex3.game.Game;
import exe.ex3.game.GhostCL;
import exe.ex3.game.PacmanGame;

import java.awt.Color;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Adapter that exposes our server-side GameState as the course PacmanGame API,
 * so we can run Ex3Algo on our own server.
 */
public class PacmanGameImpl implements PacmanGame {

    private GameState s;

    /** "code" used by the course engine (passed to methods). */
    private int currentCode = 0;

    /** Cached colors for the current code. */
    private int DOT;
    private int POWER;
    private int WALL;

    public PacmanGameImpl(GameState s) {
        this.s = s;
        refreshColors(0);
    }

    public void bind(GameState s) {
        this.s = s;
    }

    private void refreshColors(int code) {
        this.DOT = Game.getIntColor(Color.PINK, code);
        this.POWER = Game.getIntColor(Color.GREEN, code);
        this.WALL = Game.getIntColor(Color.BLUE, code);
    }

    // ---------------- PacmanGame interface ----------------

    @Override
    public Character getKeyChar() {
        // Not used when we run Ex3Algo (we call algo.move(game) directly)
        return null;
    }

    @Override
    public String getPos(int code) {
        // Expected format: "x,y"
        return s.getPacmanX() + "," + s.getPacmanY();
    }

    @Override
    public GhostCL[] getGhosts(int code) {
        GhostCL[] arr = new GhostCL[s.getGhosts().size()];
        for (int i = 0; i < s.getGhosts().size(); i++) {
            Ghost g = s.getGhosts().get(i);
            arr[i] = buildGhostCL(g, code); // might be null if jar blocks construction
        }
        return arr;
    }

    @Override
    public int[][] getGame(int code) {
        // Ensure colors match the given code
        if (code != currentCode) {
            currentCode = code;
            refreshColors(code);
        }

        // If you discover that the board orientation is flipped,
        // change to [s.h][s.w] and write b[y][x] accordingly.
        int[][] b = new int[s.w][s.h];

        for (int x = 0; x < s.w; x++) {
            for (int y = 0; y < s.h; y++) {
                Tile t = s.grid[x][y];
                b[x][y] = switch (t) {
                    case WALL -> WALL;
                    case DOT -> DOT;
                    case POWER -> POWER;
                    default -> 0; // EMPTY
                };
            }
        }
        return b;
    }

    /**
     * Not used by Ex3Algo (we call algo.move(this)), but required by interface.
     * Keep a safe stub.
     */
    @Override
    public String move(int code) {
        return "OK";
    }

    @Override
    public void play() {
        // Not used in our server-run
    }

    @Override
    public String end(int code) {
        return "DONE";
    }

    @Override
    public String getData(int code) {
        return "score=" + s.getScore() + ", lives=" + s.getLives();
    }

    @Override
    public int getStatus() {
        return s.isDone() ? DONE : PLAY;
    }

    @Override
    public boolean isCyclic() {
        return false;
    }

    @Override
    public String init(int code, String var2, boolean var3, long var4, double var6, int var8, int var9) {
        this.currentCode = code;
        refreshColors(code);
        return "OK";
    }

    // ---------------- Reflection bridge to GhostCL ----------------

    private GhostCL buildGhostCL(Ghost g, int code) {
        GhostCL obj = safeConstructGhostCL(g.x(), g.y());
        if (obj == null) {
            // Some jar versions do not allow constructing GhostCL (no accessible ctor).
            // Returning null is safer than crashing.
            return null;
        }

        // Set position (jar-dependent)
        tryInvoke(obj, "setPos", new Class[]{int.class, int.class}, new Object[]{g.x(), g.y()});
        tryInvoke(obj, "setLocation", new Class[]{int.class, int.class}, new Object[]{g.x(), g.y()});

        // Provide meaningful eatable time based on power mode
        int eatTime = getPowerTicksLeftSafe();
        if (!g.isEatable()) {
            eatTime = 0;
        }

        // Try common setters (jar-dependent). It's OK if none exist.
        tryInvoke(obj, "setRemainTimeAsEatable", new Class[]{int.class, int.class}, new Object[]{code, eatTime});
        tryInvoke(obj, "setEatableTime", new Class[]{int.class}, new Object[]{eatTime});
        tryInvoke(obj, "setRemainTime", new Class[]{int.class}, new Object[]{eatTime});

        return obj;
    }

    /**
     * Best-effort power time remaining.
     * If your GameState already has a getter like getPowerTicksLeft(), replace this method with:
     *   return s.getPowerTicksLeft();
     */
    private int getPowerTicksLeftSafe() {
        try {
            // 1) Common getter names
            Method m = findNoArgMethod(s.getClass(), "getPowerTicksLeft", "getPowerLeft", "getPowerRemain", "powerLeft");
            if (m != null) {
                Object v = m.invoke(s);
                if (v instanceof Integer) return Math.max(0, (Integer) v);
            }

            // 2) Common field names
            Field f = findField(s.getClass(), "powerTicksLeft", "powerLeft", "powerRemain", "powerTimer", "power");
            if (f != null) {
                f.setAccessible(true);
                Object v = f.get(s);
                if (v instanceof Integer) return Math.max(0, (Integer) v);
            }

            // 3) Fallback if only boolean exists
            if (s.isPowerMode()) return 20;

        } catch (Exception ignored) {
        }
        return 0;
    }

    private static Method findNoArgMethod(Class<?> cls, String... names) {
        for (String n : names) {
            try {
                Method m = cls.getMethod(n);
                m.setAccessible(true);
                return m;
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static Field findField(Class<?> cls, String... names) {
        for (String n : names) {
            Class<?> c = cls;
            while (c != null) {
                try {
                    Field f = c.getDeclaredField(n);
                    return f;
                } catch (Exception ignored) {
                    c = c.getSuperclass();
                }
            }
        }
        return null;
    }

    /**
     * IMPORTANT: Some jar versions provide NO accessible constructors for GhostCL.
     * In that case we return null (instead of throwing), to avoid crashing the game.
     */
    private GhostCL safeConstructGhostCL(int x, int y) {
        try {
            return tryConstructGhostCL(x, y);
        } catch (Exception e) {
            return null;
        }
    }

    private GhostCL tryConstructGhostCL(int x, int y) throws Exception {
        // Try public constructors first
        Constructor<?>[] ctors = GhostCL.class.getConstructors();
        for (Constructor<?> c : ctors) {
            Class<?>[] p = c.getParameterTypes();

            if (p.length == 0) {
                return (GhostCL) c.newInstance();
            }
            if (p.length == 2 && p[0] == int.class && p[1] == int.class) {
                return (GhostCL) c.newInstance(x, y);
            }
        }

        Constructor<?>[] dctors = GhostCL.class.getDeclaredConstructors();
        for (Constructor<?> c : dctors) {
            c.setAccessible(true);
            Class<?>[] p = c.getParameterTypes();

            if (p.length == 0) {
                return (GhostCL) c.newInstance();
            }
            if (p.length == 2 && p[0] == int.class && p[1] == int.class) {
                return (GhostCL) c.newInstance(x, y);
            }
        }

        throw new IllegalStateException("No compatible GhostCL constructor found");
    }

    private static void tryInvoke(Object target, String name, Class<?>[] sig, Object[] args) {
        if (target == null) return;
        try {
            Method m = target.getClass().getMethod(name, sig);
            m.setAccessible(true);
            m.invoke(target, args);
        } catch (Exception ignored) {
        }
    }
}
