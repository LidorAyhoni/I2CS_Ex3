package assignments.Ex3.render;

import assignments.Ex3.model.GameState;
import assignments.Ex3.model.Tile;

public class ConsoleRenderer implements Renderer {

    @Override
    public void init(int pixels, int gridW, int gridH) {

    }

    @Override
    public void render(GameState s) {
        StringBuilder sb = new StringBuilder();
        sb.append("score=").append(s.score).append("\n");

        for (int y = s.h - 1; y >= 0; y--) {
            for (int x = 0; x < s.w; x++) {
                if (x == s.pacX && y == s.pacY) {
                    sb.append('P');
                    continue;
                }
                Tile t = s.grid[x][y];
                char c = switch (t) {
                    case WALL -> '#';
                    case DOT -> '.';
                    case POWER -> 'o';
                    case EMPTY -> ' ';
                };
                sb.append(c);
            }
            sb.append('\n');
        }

        System.out.println("\n\n\n" + sb);
    }
}
