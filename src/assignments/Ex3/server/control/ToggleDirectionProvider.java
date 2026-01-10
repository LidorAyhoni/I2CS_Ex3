package assignments.Ex3.server.control;

import assignments.Ex3.model.Direction;
import assignments.Ex3.model.GameState;

/**
 * Provides direction control with togglable manual/AI switching.
 *
 * <p>This implementation wraps two other DirectionProviders—one for manual control and one for AI—
 * and delegates to the appropriate one based on the current mode.
 * This enables seamless switching between player control and autonomous AI during gameplay.
 *
 * <p>Typical usage:
 * <ol>
 *   <li>Wrap a manual provider (reading keyboard input) and an AI provider (pathfinding)</li>
 *   <li>Toggle between them via {@link #toggle()} or {@link #setAiEnabled(boolean)}</li>
 *   <li>Get directions via {@link #nextDirection(GameState)} - always delegates to the active provider</li>
 * </ol>
 *
 * @author Lidor Ayhoni
 * @version 1.0
 * @since 1.0
 * @see DirectionProvider
 */
public class ToggleDirectionProvider implements DirectionProvider {

    private final DirectionProvider manual;
    private final DirectionProvider ai;
    private boolean aiEnabled = false;

    /**
     * Constructs a toggle provider wrapping manual and AI direction providers.
     *
     * @param manual the provider for manual (player) control
     * @param ai the provider for automatic AI control
     */
    public ToggleDirectionProvider(DirectionProvider manual, DirectionProvider ai) {
        this.manual = manual;
        this.ai = ai;
    }

    /**
     * Sets whether AI mode is enabled.
     *
     * @param enabled {@code true} to enable AI; {@code false} for manual control
     */
    public void setAiEnabled(boolean enabled) {
        this.aiEnabled = enabled;
    }

    /**
     * Checks if AI mode is currently enabled.
     *
     * @return {@code true} if AI is active; {@code false} if manual control is active
     */
    public boolean isAiEnabled() {
        return aiEnabled;
    }

    /**
     * Toggles between AI and manual mode.
     *
     * <p>If AI is enabled, switches to manual.  If manual is enabled, switches to AI.
     */
    public void toggle() {
        this.aiEnabled = !this.aiEnabled;
    }

    /**
     * Gets the next direction by delegating to the active provider.
     *
     * @param state the current game state
     * @return the direction from the active provider (AI or manual)
     */
    @Override
    public Direction nextDirection(GameState state) {
        return aiEnabled ? ai.nextDirection(state) : manual.nextDirection(state);
    }
}