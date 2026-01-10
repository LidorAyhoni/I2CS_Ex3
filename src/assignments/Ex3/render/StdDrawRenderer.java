package assignments.Ex3.render;

import assignments.Ex3.model.GameState;
import assignments.Ex3.model.Ghost;
import assignments.Ex3.model.Tile;
import exe.ex3.game.StdDraw;

import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Graphical renderer using the StdDraw library for visual output.
 *
 * <p>This renderer creates a graphical window displaying:
 * <ul>
 *   <li><b>Game grid: </b> Walls, dots, and power pellets with distinct colors</li>
 *   <li><b>Pac-Man:</b> Yellow circle at current position</li>
 *   <li><b>Ghosts:</b> Colored circles (red, cyan, pink, orange, magenta)</li>
 *   <li><b>HUD:</b> Score, lives, power mode status, and AI/manual mode indicator</li>
 * </ul>
 *
 * <p>Color scheme:
 * <ul>
 *   <li>Walls: Dark gray filled squares</li>
 *   <li>Dots: Small white circles</li>
 *   <li>Power pellets:  Orange circles</li>
 *   <li>Pac-Man: Yellow circle</li>
 *   <li>Ghosts (normal): Red, cyan, pink, orange, magenta (cycling)</li>
 *   <li>Ghosts (eatable): Green</li>
 *   <li>Background: Black</li>
 * </ul>
 *
 * <p>The renderer uses reflection to ensure compatibility with multiple versions
 * of the StdDraw library that may have different method signatures.
 *
 * @author Lidor Ayhoni
 * @version 1.0
 * @since 1.0
 * @see Renderer
 */
public class StdDrawRenderer implements Renderer {

    private int w, h;

    // Ghost palette (base colors when NOT eatable)
    /**
     * Color palette for ghosts when not in eatable state.
     * Colors cycle when there are more than 5 ghosts.
     */
    private static final Color[] GHOST_COLORS = {
            Color.RED, Color.CYAN, Color.PINK, Color.ORANGE, Color.MAGENTA
    };

    /**
     * Initializes the graphics renderer and sets up the drawing canvas.
     *
     * <p>Configures the StdDraw window, coordinate system, and enables double-buffering.
     * Must be called once before rendering begins.
     *
     * @param pixels the pixel size of each grid cell (canvas will be pixels x pixels)
     * @param gridW the width of the game grid in cells
     * @param gridH the height of the game grid in cells
     */
    @Override
    public void init(int pixels, int gridW, int gridH) {
        this.w = gridW;
        this.h = gridH;

        SD.setCanvasSize(pixels, pixels);
        SD.setXscale(0, w);
        SD.setYscale(0, h);

        SD.enableDoubleBufferingIfExists();
    }

    /**
     * Renders the game state to the graphics window.
     *
     * <p>Rendering order:
     * <ol>
     *   <li>Clear canvas to black</li>
     *   <li>Draw all grid tiles (walls, dots, power pellets)</li>
     *   <li>Draw Pac-Man</li>
     *   <li>Draw all ghosts with appropriate colors</li>
     *   <li>Draw HUD with score, lives, power mode, and AI status</li>
     *   <li>Display frame</li>
     * </ol>
     *
     * @param s the game state to render
     */
    @Override
    public void render(GameState s) {
        SD.clear(Color.BLACK);

        // draw tiles
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                Tile t = s.grid[x][y];

                switch (t) {
                    case WALL -> {
                        SD.setPenColor(Color.DARK_GRAY);
                        SD.filledSquare(x + 0.5, y + 0.5, 0.5);
                    }
                    case DOT -> {
                        SD.setPenColor(Color.WHITE);
                        SD.filledCircle(x + 0.5, y + 0.5, 0.10);
                    }
                    case POWER -> {
                        SD.setPenColor(Color.ORANGE);
                        SD.filledCircle(x + 0.5, y + 0.5, 0.20);
                    }
                    default -> {
                    }
                }
            }
        }

        // draw pacman
        SD.setPenColor(Color.YELLOW);
        SD.filledCircle(s.pacX + 0.5, s.pacY + 0.5, 0.40);

        // draw ghosts (different colors)
        int idx = 0;
        for (Ghost g : s.getGhosts()) {
            Color base = GHOST_COLORS[idx % GHOST_COLORS.length];

            // In your logic: g.isEatable() means power mode effect on that ghost
            // Keep the same behavior: eatable -> GREEN, otherwise unique base color
            SD.setPenColor(g.isEatable() ? Color.GREEN : base);

            double cx = g.x() + 0.5;
            double cy = g.y() + 0.5;

            SD.filledCircle(cx, cy, 0.45);

            idx++;
        }

        // HUD (top-left)
        SD.setPenColor(Color.WHITE);

        double hudX = 0.2;
        double y1 = h - 0.3;
        double y2 = h - 0.7;
        double y3 = h - 1.1;
        double y4 = h - 1.5;

        int score = safeInt(() -> s.getScore(), s.score);
        int lives = safeInt(() -> s.getLives(), -1);

        boolean power = safeBool(() -> s.isPowerMode(), false);
        int powerLeft = getPowerTicksLeftSafe(s); // -1 if unknown

        String modeText = getModeTextSafe(s);

        SD.textLeft(hudX, y1, "Score: " + score);
        if (lives >= 0) SD.textLeft(hudX, y2, "Lives: " + lives);

        if (powerLeft >= 0) {
            SD.textLeft(hudX, y3, "Power: " + (power ? "ON" : "OFF") + " (" + powerLeft + ")");
        } else {
            SD.textLeft(hudX, y3, "Power: " + (power ? "ON" : "OFF"));
        }

        SD.textLeft(hudX, y4, modeText);

        SD.show();
    }

    /* ================= HUD safe helpers (no compile-time dependency) ================= */

    /**
     * Functional interface for safely retrieving int values via reflection.
     */
    private interface IntSupplierEx { int get() throws Exception; }

    /**
     * Functional interface for safely retrieving boolean values via reflection.
     */
    private interface BoolSupplierEx { boolean get() throws Exception; }

    /**
     * Safely invokes a getter function with exception handling.
     *
     * @param getter the getter to invoke
     * @param fallback the value to return if getter throws an exception
     * @return the result from getter, or fallback if an exception occurs
     */
    private static int safeInt(IntSupplierEx getter, int fallback) {
        try { return getter.get(); } catch (Exception ignored) { return fallback; }
    }

    /**
     * Safely invokes a getter function with exception handling.
     *
     * @param getter the getter to invoke
     * @param fallback the value to return if getter throws an exception
     * @return the result from getter, or fallback if an exception occurs
     */
    private static boolean safeBool(BoolSupplierEx getter, boolean fallback) {
        try { return getter.get(); } catch (Exception ignored) { return fallback; }
    }

    /**
     * Gets the current game mode (AI or MANUAL) using reflection.
     *
     * @param s the game state
     * @return a string describing the current mode, or "Mode: ?" if mode cannot be determined
     */
    private static String getModeTextSafe(GameState s) {
        Boolean ai = readBooleanFieldOrGetter(s, "aiEnabled", "isAiEnabled", "getAiEnabled");
        if (ai == null) return "Mode: ?";
        return "Mode: " + (ai ? "AI" : "MANUAL");
    }

    /**
     * Gets the remaining power-mode ticks using reflection.
     *
     * @param s the game state
     * @return the number of remaining power ticks, -1 if unable to determine
     */
    private static int getPowerTicksLeftSafe(GameState s) {
        Integer v = readIntFieldOrGetter(s, "powerTicksLeft", "getPowerTicksLeft", "getPowerLeft", "getPowerRemain");
        return v == null ? -1 : Math.max(0, v);
    }

    /**
     * Attempts to read a boolean value from an object using reflection.
     *
     * <p>Tries getter methods first, then fields, using the specified names.
     *
     * @param obj the object to read from
     * @param fieldName the field name to try
     * @param getters getter method names to try
     * @return the boolean value if found, null otherwise
     */
    private static Boolean readBooleanFieldOrGetter(Object obj, String fieldName, String...  getters) {
        try {
            // try getter
            for (String g : getters) {
                try {
                    Method m = obj.getClass().getMethod(g);
                    m.setAccessible(true);
                    Object val = m.invoke(obj);
                    if (val instanceof Boolean) return (Boolean) val;
                } catch (NoSuchMethodException ignored) {}
            }
            // try field
            Field f = findField(obj.getClass(), fieldName);
            if (f != null) {
                f.setAccessible(true);
                Object val = f.get(obj);
                if (val instanceof Boolean) return (Boolean) val;
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Attempts to read an integer value from an object using reflection.
     *
     * <p>Tries getter methods first, then fields, using the specified names.
     *
     * @param obj the object to read from
     * @param fieldName the field name to try
     * @param getters getter method names to try
     * @return the integer value if found, null otherwise
     */
    private static Integer readIntFieldOrGetter(Object obj, String fieldName, String... getters) {
        try {
            // try getter
            for (String g : getters) {
                try {
                    Method m = obj.getClass().getMethod(g);
                    m.setAccessible(true);
                    Object val = m.invoke(obj);
                    if (val instanceof Integer) return (Integer) val;
                } catch (NoSuchMethodException ignored) {}
            }
            // try field
            Field f = findField(obj.getClass(), fieldName);
            if (f != null) {
                f.setAccessible(true);
                Object val = f.get(obj);
                if (val instanceof Integer) return (Integer) val;
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Searches for a field by name in a class hierarchy.
     *
     * @param cls the class to search
     * @param name the field name
     * @return the Field if found, null otherwise
     */
    private static Field findField(Class<?> cls, String name) {
        Class<?> c = cls;
        while (c != null) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            }
        }
        return null;
    }

    /* ================= StdDraw reflection wrapper ================= */

    /**
     * Internal wrapper for StdDraw method invocation via reflection.
     *
     * <p>This class handles the complexity of different StdDraw library versions
     * by using reflection to attempt multiple method signatures and gracefully
     * falling back when certain methods don't exist.
     *
     * @author Lidor Ayhoni (wrapper)
     * @version 1.0
     */
    private static final class SD {
        private static final int HASH = 1;
        private SD() {}

        /**
         * Sets the canvas size using reflection.
         *
         * @param w canvas width in pixels
         * @param h canvas height in pixels
         */
        static void setCanvasSize(int w, int h) {
            if (!invokeVoid("setCanvasSize", new Class<?>[]{int.class, int.class}, w, h)) {
                mustInvokeVoid("setCanvasSize", new Class<?>[]{int.class, int.class, int.class}, w, h, HASH);
            }
        }

        /**
         * Sets the x-axis scale using reflection.
         *
         * @param min minimum x value
         * @param max maximum x value
         */
        static void setXscale(double min, double max) {
            if (!invokeVoid("setXscale", new Class<?>[]{double.class, double.class}, min, max)) {
                mustInvokeVoid("setXscale", new Class<?>[]{double.class, double.class, int.class}, min, max, HASH);
            }
        }

        /**
         * Sets the y-axis scale using reflection.
         *
         * @param min minimum y value
         * @param max maximum y value
         */
        static void setYscale(double min, double max) {
            if (!invokeVoid("setYscale", new Class<?>[]{double.class, double.class}, min, max)) {
                mustInvokeVoid("setYscale", new Class<?>[]{double.class, double.class, int.class}, min, max, HASH);
            }
        }

        /**
         * Clears the canvas with a given color using reflection.
         *
         * @param c the color to clear with
         */
        static void clear(Color c) {
            if (invokeVoid("clear", new Class<?>[]{Color.class}, c)) return;
            if (invokeVoid("clear", new Class<?>[]{Color.class, int.class}, c, HASH)) return;

            if (invokeVoid("clear", new Class<?>[]{}, new Object[]{})) return;
            mustInvokeVoid("clear", new Class<?>[]{int.class}, HASH);
        }

        /**
         * Sets the pen color using reflection.
         *
         * @param c the color to set
         */
        static void setPenColor(Color c) {
            if (!invokeVoid("setPenColor", new Class<?>[]{Color.class}, c)) {
                mustInvokeVoid("setPenColor", new Class<?>[]{Color.class, int.class}, c, HASH);
            }
        }

        /**
         * Draws a filled square using reflection.
         *
         * @param x center x coordinate
         * @param y center y coordinate
         * @param half half the side length
         */
        static void filledSquare(double x, double y, double half) {
            if (!invokeVoid("filledSquare", new Class<?>[]{double.class, double.class, double.class}, x, y, half)) {
                mustInvokeVoid("filledSquare",
                        new Class<?>[]{double.class, double.class, double.class, int.class},
                        x, y, half, HASH);
            }
        }

        /**
         * Draws a filled circle using reflection.
         *
         * @param x center x coordinate
         * @param y center y coordinate
         * @param r radius
         */
        static void filledCircle(double x, double y, double r) {
            if (!invokeVoid("filledCircle", new Class<?>[]{double.class, double.class, double.class}, x, y, r)) {
                mustInvokeVoid("filledCircle",
                        new Class<?>[]{double.class, double.class, double.class, int.class},
                        x, y, r, HASH);
            }
        }

        /**
         * Draws text aligned to the left using reflection.
         *
         * @param x x coordinate
         * @param y y coordinate
         * @param text the text to draw
         */
        static void textLeft(double x, double y, String text) {
            if (!invokeVoid("textLeft", new Class<?>[]{double.class, double.class, String.class}, x, y, text)) {
                mustInvokeVoid("textLeft",
                        new Class<?>[]{double.class, double.class, String.class, int.class},
                        x, y, text, HASH);
            }
        }

        /**
         * Updates the display using reflection.
         */
        static void show() {
            if (!invokeVoid("show", new Class<?>[]{}, new Object[]{})) {
                mustInvokeVoid("show", new Class<?>[]{int.class}, HASH);
            }
        }

        /**
         * Attempts to enable double buffering using reflection.
         * Fails silently if the method doesn't exist.
         */
        static void enableDoubleBufferingIfExists() {
            invokeVoid("enableDoubleBuffering", new Class<?>[]{}, new Object[]{});
            invokeVoid("enableDoubleBuffering", new Class<?>[]{int.class}, HASH);
        }

        /**
         * Attempts to invoke a void method on StdDraw via reflection.
         *
         * @param name the method name
         * @param types the parameter types
         * @param args the arguments
         * @return true if the invocation succeeded, false if the method doesn't exist
         */
        private static boolean invokeVoid(String name, Class<?>[] types, Object... args) {
            try {
                Method m = StdDraw.class.getMethod(name, types);
                m.invoke(null, args);
                return true;
            } catch (NoSuchMethodException e) {
                return false;
            } catch (Exception e) {
                throw new RuntimeException("StdDraw." + name + " invocation failed", e);
            }
        }

        /**
         * Invokes a void method on StdDraw via reflection, throwing an exception if it fails.
         *
         * @param name the method name
         * @param types the parameter types
         * @param args the arguments
         */
        private static void mustInvokeVoid(String name, Class<?>[] types, Object... args) {
            try {
                Method m = StdDraw.class.getMethod(name, types);
                m.invoke(null, args);
            } catch (Exception e) {
                throw new RuntimeException("StdDraw." + name + " required method missing/failed", e);
            }
        }
    }
}