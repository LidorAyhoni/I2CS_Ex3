package assignments.Ex3.render;

import assignments.Ex3.model.GameState;
// import edu.princeton.cs.algs4.StdDraw;  // או StdDraw מהקורס, תלוי איפה הוא יושב

public class StdDrawRenderer implements Renderer {
    private int w, h;

    @Override
    public void init(int pixels, int gridW, int gridH) {
        this.w = gridW;
        this.h = gridH;

        // StdDraw.setCanvasSize(pixels, pixels);
        // StdDraw.setXscale(0, w);
        // StdDraw.setYscale(0, h);
        // StdDraw.enableDoubleBuffering();
    }

    @Override
    public void render(GameState s) {
        // StdDraw.clear();

        // ציור גריד בסיסי / רקע:
        // for (int x = 0; x < w; x++) for (int y = 0; y < h; y++) { ... }

        // ציור פאקמן/נקודות/רוחות בהמשך

        // StdDraw.show();
    }
}
