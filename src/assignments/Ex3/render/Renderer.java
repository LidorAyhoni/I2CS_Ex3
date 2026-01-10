package assignments.Ex3.render;

import assignments.Ex3.model.GameState;

/**
 * Interface for rendering the game state to the display.
 *
 * <p>A renderer is responsible for visualizing the game world, including the game grid,
 * Pac-Man, ghosts, and UI elements. Different implementations can support various
 * rendering technologies (graphics libraries, console output, etc.).
 *
 * <p>Typical lifecycle:
 * <ol>
 *   <li>Call {@link #init(int, int, int)} once before rendering begins</li>
 *   <li>Call {@link #render(GameState)} repeatedly in the game loop</li>
 * </ol>
 *
 * @author Lidor Ayhoni
 * @version 1.0
 * @since 1.0
 * @see GameState
 */
public interface Renderer {
    /**
     * Initializes the renderer with display parameters.
     *
     * <p>This method sets up the rendering environment and must be called
     * before the first call to {@link #render(GameState)}.
     *
     * @param pixels the pixel size of each grid cell in the display
     * @param gridW the width of the game grid in cells
     * @param gridH the height of the game grid in cells
     */
    void init(int pixels, int gridW, int gridH);

    /**
     * Renders the current game state to the display.
     *
     * <p>This method is called once per game loop iteration to update the visual display.
     * It should draw the grid, Pac-Man, ghosts, score, and any other visible game elements.
     *
     * @param s the current game state to render
     */
    void render(GameState s);
}