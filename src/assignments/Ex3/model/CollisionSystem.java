package assignments.Ex3.model;

public class CollisionSystem {

    public void resolve(GameState s) {
        int px = s.pacX;
        int py = s.pacY;

        for (Ghost g : s.getGhosts()) {
            if (g.x() == px && g.y() == py) {
                s.onPacmanGhostCollision(g);
                return;
            }
        }
    }
}

