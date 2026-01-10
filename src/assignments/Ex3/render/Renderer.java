package assignments.Ex3.render;

import assignments.Ex3.model.GameState;

public interface Renderer {
    void init(int pixels, int gridW, int gridH);
    void render(GameState s);
}
