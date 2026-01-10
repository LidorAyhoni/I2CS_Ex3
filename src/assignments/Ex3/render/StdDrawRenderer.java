package assignments.Ex3.render;

import assignments.Ex3.model.GameState;
import assignments.Ex3.model.Ghost;
import assignments.Ex3.model.Tile;
import exe.ex3.game.StdDraw;

import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class StdDrawRenderer implements Renderer {

    private int w, h;

    // Ghost palette (base colors when NOT eatable)
    private static final Color[] GHOST_COLORS = {
            Color.RED, Color.CYAN, Color.PINK, Color.ORANGE, Color.MAGENTA
    };

    @Override
    public void init(int pixels, int gridW, int gridH) {
        this.w = gridW;
        this.h = gridH;

        SD.setCanvasSize(pixels, pixels);
        SD.setXscale(0, w);
        SD.setYscale(0, h);

        SD.enableDoubleBufferingIfExists();
    }

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

    private interface IntSupplierEx { int get() throws Exception; }
    private interface BoolSupplierEx { boolean get() throws Exception; }

    private static int safeInt(IntSupplierEx getter, int fallback) {
        try { return getter.get(); } catch (Exception ignored) { return fallback; }
    }

    private static boolean safeBool(BoolSupplierEx getter, boolean fallback) {
        try { return getter.get(); } catch (Exception ignored) { return fallback; }
    }

    /** Mode: AI/MANUAL if we can read it from GameState, else Mode: ? */
    private static String getModeTextSafe(GameState s) {
        Boolean ai = readBooleanFieldOrGetter(s, "aiEnabled", "isAiEnabled", "getAiEnabled");
        if (ai == null) return "Mode: ?";
        return "Mode: " + (ai ? "AI" : "MANUAL");
    }

    /** Returns remaining power ticks if exists, else -1 */
    private static int getPowerTicksLeftSafe(GameState s) {
        Integer v = readIntFieldOrGetter(s, "powerTicksLeft", "getPowerTicksLeft", "getPowerLeft", "getPowerRemain");
        return v == null ? -1 : Math.max(0, v);
    }

    private static Boolean readBooleanFieldOrGetter(Object obj, String fieldName, String... getters) {
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

    private static final class SD {
        private static final int HASH = 1;
        private SD() {}

        static void setCanvasSize(int w, int h) {
            if (!invokeVoid("setCanvasSize", new Class<?>[]{int.class, int.class}, w, h)) {
                mustInvokeVoid("setCanvasSize", new Class<?>[]{int.class, int.class, int.class}, w, h, HASH);
            }
        }

        static void setXscale(double min, double max) {
            if (!invokeVoid("setXscale", new Class<?>[]{double.class, double.class}, min, max)) {
                mustInvokeVoid("setXscale", new Class<?>[]{double.class, double.class, int.class}, min, max, HASH);
            }
        }

        static void setYscale(double min, double max) {
            if (!invokeVoid("setYscale", new Class<?>[]{double.class, double.class}, min, max)) {
                mustInvokeVoid("setYscale", new Class<?>[]{double.class, double.class, int.class}, min, max, HASH);
            }
        }

        static void clear(Color c) {
            if (invokeVoid("clear", new Class<?>[]{Color.class}, c)) return;
            if (invokeVoid("clear", new Class<?>[]{Color.class, int.class}, c, HASH)) return;

            if (invokeVoid("clear", new Class<?>[]{}, new Object[]{})) return;
            mustInvokeVoid("clear", new Class<?>[]{int.class}, HASH);
        }

        static void setPenColor(Color c) {
            if (!invokeVoid("setPenColor", new Class<?>[]{Color.class}, c)) {
                mustInvokeVoid("setPenColor", new Class<?>[]{Color.class, int.class}, c, HASH);
            }
        }

        static void filledSquare(double x, double y, double half) {
            if (!invokeVoid("filledSquare", new Class<?>[]{double.class, double.class, double.class}, x, y, half)) {
                mustInvokeVoid("filledSquare",
                        new Class<?>[]{double.class, double.class, double.class, int.class},
                        x, y, half, HASH);
            }
        }

        static void filledCircle(double x, double y, double r) {
            if (!invokeVoid("filledCircle", new Class<?>[]{double.class, double.class, double.class}, x, y, r)) {
                mustInvokeVoid("filledCircle",
                        new Class<?>[]{double.class, double.class, double.class, int.class},
                        x, y, r, HASH);
            }
        }

        static void textLeft(double x, double y, String text) {
            if (!invokeVoid("textLeft", new Class<?>[]{double.class, double.class, String.class}, x, y, text)) {
                mustInvokeVoid("textLeft",
                        new Class<?>[]{double.class, double.class, String.class, int.class},
                        x, y, text, HASH);
            }
        }

        static void show() {
            if (!invokeVoid("show", new Class<?>[]{}, new Object[]{})) {
                mustInvokeVoid("show", new Class<?>[]{int.class}, HASH);
            }
        }

        static void enableDoubleBufferingIfExists() {
            invokeVoid("enableDoubleBuffering", new Class<?>[]{}, new Object[]{});
            invokeVoid("enableDoubleBuffering", new Class<?>[]{int.class}, HASH);
        }

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
