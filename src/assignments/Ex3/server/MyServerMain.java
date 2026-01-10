package assignments.Ex3.server;

import assignments.Ex3.Ex3Algo;
import assignments.Ex3.levels.LevelLoader;
import assignments.Ex3.model.GameState;
import assignments.Ex3.render.Renderer;
import assignments.Ex3.render.StdDrawRenderer;
import assignments.Ex3.server.control.*;

import java.util.Scanner;

public class MyServerMain {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        // -------- Choose level --------
        System.out.println("Choose level:");
        System.out.println("0 - Small map");
        System.out.println("1 - Medium map");
        System.out.println("2 - Large map");
        System.out.print("> ");

        int level = 0;
        try { level = sc.nextInt(); } catch (Exception ignored) {}

        // -------- Choose mode --------
        System.out.println("Choose mode:");
        System.out.println("0 - Manual (keyboard)");
        System.out.println("1 - AI");
        System.out.print("> ");

        int mode = 0;
        try { mode = sc.nextInt(); } catch (Exception ignored) {}

        boolean startWithAI = (mode == 1);

        // -------- Load level --------
        GameState s = switch (level) {
            case 0 -> LevelLoader.level0();
            case 1 -> LevelLoader.level1();
            case 2 -> LevelLoader.level2();
            default -> LevelLoader.level1();
        };


        // -------- Renderer --------
        Renderer r = new StdDrawRenderer();
        r.init(800, s.w, s.h);

        // -------- Providers --------
        InputController input = new InputController();
        DirectionProvider manual = new ManualDirectionProvider(input);

        PacmanGameImpl adapter = new PacmanGameImpl(s);
        DirectionProvider ai = new AiDirectionProvider(new Ex3Algo(), adapter);

        ToggleDirectionProvider provider = new ToggleDirectionProvider(manual, ai);
        provider.setAiEnabled(startWithAI);
        s.aiEnabled = startWithAI;

        // -------- Show first frame (loaded map) --------
        r.render(s);

        // -------- Wait for SPACE --------
        System.out.println();
        System.out.println("Game loaded ✅");
        System.out.println("Level: " + (level == 0 ? 0 : 1));
        System.out.println("Mode: " + (startWithAI ? "AI" : "MANUAL"));
        System.out.println("Press SPACE to start");
        System.out.println("Press 'T' anytime to toggle AI / Manual");
        System.out.println();

        while (!input.consumeStart()) {
            // keep rendering so the window stays responsive
            r.render(s);
            try { Thread.sleep(30); } catch (InterruptedException ignored) {}
        }

        // -------- Run --------
        GameLoop loop = new GameLoop(s, r, provider, input);
        loop.run();

        System.out.println("DONE ✅ score=" + s.getScore() + " lives=" + s.getLives());
    }
}
