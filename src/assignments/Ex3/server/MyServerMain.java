package assignments.Ex3.server;

import assignments. Ex3.levels.LevelLoader;
import assignments.Ex3.model.GameState;
import assignments. Ex3.render. Renderer;
import assignments.Ex3.render.StdDrawRenderer;
import assignments.Ex3.server.control.*;

import java.util.Scanner;

/**
 * Main entry point for the Pac-Man game server.
 *
 * <p>This class orchestrates the game initialization and startup:
 * <ul>
 *   <li>Prompts the player to choose a difficulty level (small, medium, large)</li>
 *   <li>Prompts the player to choose a game mode (manual or AI control)</li>
 *   <li>Loads the selected level and initializes the game state</li>
 *   <li>Sets up the graphics renderer and input controller</li>
 *   <li>Allows toggling between manual and AI modes during gameplay</li>
 *   <li>Runs the main game loop</li>
 * </ul>
 *
 * <p>During gameplay:
 * <ul>
 *   <li><b>SPACE</b>: Start/resume the game</li>
 *   <li><b>T</b>: Toggle between AI and manual control</li>
 *   <li><b>WASD/Arrows</b>: Manual direction control (in manual mode)</li>
 * </ul>
 *
 * @author Lidor Ayhoni
 * @version 1.0
 * @since 1.0
 */
public class MyServerMain {

    /**
     * Main method - starts the Pac-Man game.
     *
     * <p>Execution flow:
     * <ol>
     *   <li>Prompt for level selection (0-2)</li>
     *   <li>Prompt for game mode (0=manual, 1=AI)</li>
     *   <li>Load the selected game level</li>
     *   <li>Initialize the graphics renderer</li>
     *   <li>Set up input and direction providers</li>
     *   <li>Display game information and wait for SPACE key</li>
     *   <li>Run the main game loop</li>
     *   <li>Display final score and game status</li>
     * </ol>
     *
     * @param args command-line arguments (not used)
     */
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

        // ✅ AI provider that uses ServerEx3Algo directly (no adapter, no jar algo)
        DirectionProvider ai = new AiDirectionProvider();

        ToggleDirectionProvider provider = new ToggleDirectionProvider(manual, ai);
        provider.setAiEnabled(startWithAI);
        s.aiEnabled = startWithAI;

        // -------- Show first frame (loaded map) --------
        r.render(s);

        // -------- Wait for SPACE --------
        System.out.println();
        System.out.println("Game loaded ✅");
        System.out.println("Level: " + level);
        System.out.println("Mode: " + (startWithAI ? "AI" : "MANUAL"));
        System.out.println("Press SPACE to start");
        System.out.println("Press 'T' anytime to toggle AI / Manual");
        System.out.println();

        while (!input.consumeStart()) {
            r.render(s);
            try { Thread.sleep(30); } catch (InterruptedException ignored) {}
        }

        // -------- Run --------
        GameLoop loop = new GameLoop(s, r, provider, input);
        loop.run();

        System.out.println("DONE ✅ score=" + s.getScore() + " lives=" + s.getLives());
    }
}