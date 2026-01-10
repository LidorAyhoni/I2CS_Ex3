package assignments.Ex3.server.control;

import assignments.Ex3.model.Direction;
import assignments.Ex3.model.GameState;

public class ToggleDirectionProvider implements DirectionProvider {

    private final DirectionProvider manual;
    private final DirectionProvider ai;
    private boolean aiEnabled = false;

    public ToggleDirectionProvider(DirectionProvider manual, DirectionProvider ai) {
        this.manual = manual;
        this.ai = ai;
    }

    public void setAiEnabled(boolean enabled) {
        this.aiEnabled = enabled;
    }

    public boolean isAiEnabled() {
        return aiEnabled;
    }

    public void toggle() {
        this.aiEnabled = !this.aiEnabled;
    }

    @Override
    public Direction nextDirection(GameState state) {
        return aiEnabled ? ai.nextDirection(state) : manual.nextDirection(state);
    }
}
