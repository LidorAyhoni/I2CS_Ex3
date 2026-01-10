package assignments.Ex3.server;

import assignments.Ex3.levels.LevelLoader;
import assignments.Ex3.model.GameState;
import assignments.Ex3.render.ConsoleRenderer;
import assignments.Ex3.render.Renderer;

public class MyServerMain {
    public static void main(String[] args) {
        GameState s = LevelLoader.level0();
        Renderer r = new ConsoleRenderer();

        GameLoop loop = new GameLoop(s, r, 120);
        loop.run();

        System.out.println("DONE ✅ score=" + s.score);
    }
}
