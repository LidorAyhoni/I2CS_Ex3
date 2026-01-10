package assignments.Ex3.server;

import assignments.Ex3.levels.LevelLoader;
import assignments.Ex3.model.GameState;
import assignments.Ex3.render.Renderer;
import assignments.Ex3.render.StdDrawRenderer;

public class MyServerMain {

    public static void main(String[] args) {
        GameState s = LevelLoader.level0();

        Renderer r = new StdDrawRenderer();
        r.init(800, s.w, s.h);

        InputController input = new InputController();

        GameLoop loop = new GameLoop(s, r, input, 80);
        loop.run();

        System.out.println("DONE ✅ score=" + s.score);
    }
}
