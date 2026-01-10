package assignments.Ex3.render;

import assignments.Ex3.model.GameState;
import assignments.Ex3.model.Tile;
import exe.ex3.game.StdDraw;

import java.awt.Color;
import java.lang.reflect.Method;

public class StdDrawRenderer implements Renderer {

    private int w, h;

    @Override
    public void init(int pixels, int gridW, int gridH) {
        this.w = gridW;
        this.h = gridH;

        // אצל המרצה ראינו: setCanvasSize(int,int,int)
        SD.setCanvasSize(pixels, pixels);
        SD.setXscale(0, w);
        SD.setYscale(0, h);

        // אופציונלי: אם יש double buffering בגרסה שלהם
        SD.enableDoubleBufferingIfExists();
    }

    @Override
    public void render(GameState s) {
        SD.clear(Color.BLACK);

        // ציור המפה
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

        // פאקמן
        SD.setPenColor(Color.YELLOW);
        SD.filledCircle(s.pacX + 0.5, s.pacY + 0.5, 0.40);

        // ניקוד
        SD.setPenColor(Color.WHITE);
        SD.textLeft(0.2, h - 0.3, "Score: " + s.score);

        SD.show();
    }

    /**
     * Wrapper קטן סביב StdDraw:
     * מנסה קודם חתימה "רגילה", ואם לא קיימת אז חתימה עם int בסוף (hash).
     */
    private static final class SD {
        // כל ערך int עובד כ"דמה" עבור ה-hash, אצל המרצה הוא רק לצרכי obfuscation/validation פנימי
        private static final int HASH = 1;

        private SD() {}

        static void setCanvasSize(int w, int h) {
            // setCanvasSize(int,int) או setCanvasSize(int,int,int)
            if (!invokeVoid("setCanvasSize", new Class<?>[]{int.class, int.class}, w, h)) {
                mustInvokeVoid("setCanvasSize", new Class<?>[]{int.class, int.class, int.class}, w, h, HASH);
            }
        }

        static void setXscale(double min, double max) {
            // setXscale(double,double) או setXscale(double,double,int)
            if (!invokeVoid("setXscale", new Class<?>[]{double.class, double.class}, min, max)) {
                mustInvokeVoid("setXscale", new Class<?>[]{double.class, double.class, int.class}, min, max, HASH);
            }
        }

        static void setYscale(double min, double max) {
            // setYscale(double,double) או setYscale(double,double,int)
            if (!invokeVoid("setYscale", new Class<?>[]{double.class, double.class}, min, max)) {
                mustInvokeVoid("setYscale", new Class<?>[]{double.class, double.class, int.class}, min, max, HASH);
            }
        }

        static void clear(Color c) {
            // clear(Color) או clear(Color,int) או clear(int) fallback
            if (invokeVoid("clear", new Class<?>[]{Color.class}, c)) return;
            if (invokeVoid("clear", new Class<?>[]{Color.class, int.class}, c, HASH)) return;

            // fallback: clear() או clear(int)
            if (invokeVoid("clear", new Class<?>[]{}, new Object[]{})) return;
            mustInvokeVoid("clear", new Class<?>[]{int.class}, HASH);
        }

        static void setPenColor(Color c) {
            // setPenColor(Color) או setPenColor(Color,int)
            if (!invokeVoid("setPenColor", new Class<?>[]{Color.class}, c)) {
                mustInvokeVoid("setPenColor", new Class<?>[]{Color.class, int.class}, c, HASH);
            }
        }

        static void filledSquare(double x, double y, double half) {
            // filledSquare(double,double,double) או filledSquare(double,double,double,int)
            if (!invokeVoid("filledSquare", new Class<?>[]{double.class, double.class, double.class}, x, y, half)) {
                mustInvokeVoid("filledSquare",
                        new Class<?>[]{double.class, double.class, double.class, int.class},
                        x, y, half, HASH);
            }
        }

        static void filledCircle(double x, double y, double r) {
            // filledCircle(double,double,double) או filledCircle(double,double,double,int)
            if (!invokeVoid("filledCircle", new Class<?>[]{double.class, double.class, double.class}, x, y, r)) {
                mustInvokeVoid("filledCircle",
                        new Class<?>[]{double.class, double.class, double.class, int.class},
                        x, y, r, HASH);
            }
        }

        static void textLeft(double x, double y, String text) {
            // textLeft(double,double,String) או textLeft(double,double,String,int)
            if (!invokeVoid("textLeft", new Class<?>[]{double.class, double.class, String.class}, x, y, text)) {
                mustInvokeVoid("textLeft",
                        new Class<?>[]{double.class, double.class, String.class, int.class},
                        x, y, text, HASH);
            }
        }

        static void show() {
            // show() או show(int)
            if (!invokeVoid("show", new Class<?>[]{}, new Object[]{})) {
                mustInvokeVoid("show", new Class<?>[]{int.class}, HASH);
            }
        }

        static void enableDoubleBufferingIfExists() {
            // enableDoubleBuffering() או enableDoubleBuffering(int) — אם לא קיים, לא נופלים
            invokeVoid("enableDoubleBuffering", new Class<?>[]{}, new Object[]{});
            invokeVoid("enableDoubleBuffering", new Class<?>[]{int.class}, HASH);
        }

        /* ---------- Reflection helpers ---------- */

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
