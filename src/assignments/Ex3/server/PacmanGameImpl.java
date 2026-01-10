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
 * Adapter that exposes the server-side {@link GameState} as a {@link PacmanGame}
 * compatible with the course's game engine API.
 *
 * <p>This adapter bridges the gap between the course's PacmanGame interface
 * (used by provided algorithms like Ex3Algo) and our custom GameState implementation.
 * It allows student algorithms to run on our server-based game using reflection
 * to handle multiple jar versions with different API variations.
 *
 * <p>Key responsibilities:
 * <ul>
 *   <li>Convert GameState grid to game color values</li>
 *   <li>Expose Pac-Man and ghost positions</li>
 *   <li>Handle color encoding/decoding for different jar versions</li>
 *   <li>Provide reflection-based access to game status (score, lives, power mode)</li>
 * </ul>
 *
 * @author Lidor Ayhoni
 * @version 1.0
 * @since 1.0
 * @see PacmanGame
 * @see GameState
 */
public class PacmanGameImpl implements PacmanGame {

    /**
     * The underlying game state being adapted.
     */
    private GameState s;

    /**
     * The current color code used for encoding game values.
     * Different jar versions may use different codes for the same logical values.
     */
    private int currentCode = 0;

    /**
     * Cached color value for dots in the current encoding.
     */
    private int DOT;

    /**
     * Cached color value for power pellets in the current encoding.
     */
    private int POWER;

    /**
     * Cached color value for walls in the current encoding.
     */
    private int WALL;

    /**
     * Constructs a PacmanGame adapter for the given GameState.
     *
     * @param s the GameState to adapt
     */
    public PacmanGameImpl(GameState s) {
        this.s = s;
        refreshColors(0);
    }

    /**
     * Changes the underlying GameState being adapted.
     *
     * @param s the new GameState
     */
    public void bind(GameState s) {
        this.s = s;
    }

    /**
     * Refreshes cached color values for a given code.
     *
     * <p>Different jar versions encode colors differently based on a "code" parameter.
     * This method caches the relevant color values for efficient access.
     *
     * @param code the color encoding code from the jar version
     */
    private void refreshColors(int code) {
        this.DOT = Game.getIntColor(Color.PINK, code);
        this.POWER = Game.getIntColor(Color.GREEN, code);
        this.WALL = Game.getIntColor(Color.BLUE, code);
    }

    // ---------------- PacmanGame interface ----------------

    /**
     * Gets keyboard input character.
     *
     * <p>Not used when running algorithms directly via reflection.
     *
     * @return null (not used)
     */
    @Override
    public Character getKeyChar() {
        return null;
    }

    /**
     * Gets the current position of Pac-Man.
     *
     * @param code the color encoding code (unused)
     * @return a string in format "x,y" representing Pac-Man's position
     */
    @Override
    public String getPos(int code) {
        return s.getPacmanX() + "," + s.getPacmanY();
    }

    /**
     * Gets all active ghosts as an array of GhostCL objects.
     *
     * <p>Attempts to construct GhostCL objects via reflection to match jar API,
     * gracefully handling jars that don't provide accessible constructors.
     *
     * @param code the color encoding code
     * @return an array of GhostCL objects (may contain nulls if construction fails)
     */
    @Override
    public GhostCL[] getGhosts(int code) {
        GhostCL[] arr = new GhostCL[s.getGhosts().size()];
        for (int i = 0; i < s.getGhosts().size(); i++) {
            Ghost g = s.getGhosts().get(i);
            arr[i] = buildGhostCL(g, code);
        }
        return arr;
    }

    /**
     * Gets the game board as a 2D integer array.
     *
     * <p>Converts the tile-based grid to a color-encoded integer grid matching
     * the course's PacmanGame API.  Handles color encoding changes via the code parameter.
     *
     * @param code the color encoding code (determines how colors are represented)
     * @return a 2D array where [x][y] contains the encoded tile value
     */
    @Override
    public int[][] getGame(int code) {
        // Ensure colors match the given code
        if (code != currentCode) {
            currentCode = code;
            refreshColors(code);
        }

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
     * Processes a move command.
     *
     * <p>Not used when running algorithms directly (they call nextMove directly).
     * This is a stub implementation required by the PacmanGame interface.
     *
     * @param code the color code (unused)
     * @return "OK"
     */
    @Override
    public String move(int code) {
        return "OK";
    }

    /**
     * Starts the game.
     *
     * <p>Not used in server-based gameplay. Stub implementation.
     */
    @Override
    public void play() {
    }

    /**
     * Ends the game.
     *
     * @param code the color code (unused)
     * @return "DONE"
     */
    @Override
    public String end(int code) {
        return "DONE";
    }

    /**
     * Gets game data as a string.
     *
     * @param code the color code (unused)
     * @return a string with current score and lives
     */
    @Override
    public String getData(int code) {
        return "score=" + s.getScore() + ", lives=" + s.getLives();
    }

    /**
     * Gets the current game status.
     *
     * @return DONE if game is over, PLAY otherwise
     */
    @Override
    public int getStatus() {
        return s.isDone() ? DONE : PLAY;
    }

    /**
     * Checks if the game board wraps around (cyclic).
     *
     * @return false (our game does not use cyclic wrapping)
     */
    @Override
    public boolean isCyclic() {
        return false;
    }

    /**
     * Initializes the game with course-provided parameters.
     *
     * @param code the color encoding code
     * @param var2 map data (unused)
     * @param var3 cyclic flag (unused)
     * @param var4 unused
     * @param var6 unused
     * @param var8 unused
     * @param var9 unused
     * @return "OK"
     */
    @Override
    public String init(int code, String var2, boolean var3, long var4, double var6, int var8, int var9) {
        this.currentCode = code;
        refreshColors(code);
        return "OK";
    }

    // ---------------- Reflection bridge to GhostCL ----------------

    /**
     * Builds a GhostCL object from a server Ghost via reflection.
     *
     * <p>Attempts to construct GhostCL and set its properties using reflection
     * to handle varying jar API signatures. Returns null if construction fails,
     * allowing the game to continue gracefully.
     *
     * @param g the server Ghost
     * @param code the color encoding code
     * @return a GhostCL object, or null if it cannot be constructed
     */
    private GhostCL buildGhostCL(Ghost g, int code) {
        GhostCL obj = safeConstructGhostCL(g.x(), g.y());
        if (obj == null) {
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

        // Try common setters (jar-dependent).
        tryInvoke(obj, "setRemainTimeAsEatable", new Class[]{int.class, int.class}, new Object[]{code, eatTime});
        tryInvoke(obj, "setEatableTime", new Class[]{int.class}, new Object[]{eatTime});
        tryInvoke(obj, "setRemainTime", new Class[]{int.class}, new Object[]{eatTime});

        return obj;
    }

    /**
     * Gets the remaining power-mode duration using reflection.
     *
     * <p>Attempts multiple common method and field names to find power timing info.
     * Returns 0 if unable to determine, or 20 as a fallback if power mode is active.
     *
     * @return the number of remaining power ticks
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

    /**
     * Finds a no-argument method in a class by name.
     *
     * @param cls the class to search
     * @param names the method names to try
     * @return the Method if found, null otherwise
     */
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

    /**
     * Finds a field in a class hierarchy by name.
     *
     * @param cls the class to search
     * @param names the field names to try
     * @return the Field if found, null otherwise
     */
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
     * Safely constructs a GhostCL instance via reflection.
     *
     * <p>Attempts public constructors first, then declared constructors.
     * Returns null instead of throwing if construction fails.
     *
     * @param x the x-coordinate for the ghost
     * @param y the y-coordinate for the ghost
     * @return a new GhostCL instance, or null if it cannot be constructed
     */
    private GhostCL safeConstructGhostCL(int x, int y) {
        try {
            return tryConstructGhostCL(x, y);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Attempts to construct a GhostCL instance.
     *
     * <p>Tries common constructor signatures:  no-arg and (int, int).
     *
     * @param x the x-coordinate
     * @param y the y-coordinate
     * @return a new GhostCL instance
     * @throws Exception if no compatible constructor is found
     */
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

    /**
     * Attempts to invoke a method using reflection, ignoring failures.
     *
     * @param target the target object
     * @param name the method name
     * @param sig the parameter types
     * @param args the arguments
     */
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