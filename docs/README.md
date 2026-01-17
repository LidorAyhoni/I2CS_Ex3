# Ex3 – Pac-Man (Stage 2 Client AI + Stage 3 Custom Server)

This repository contains my full solution for **Exercise 3 – Pac-Man** in Java, including:

* **Stage 2 (Client AI)**: `Ex3Algo` implements the course `PacManAlgo` and runs on the **course-provided engine**.
* **Stage 3 (Custom Server Game)**: a complete server-side implementation (model + loop + renderer) with both **Manual** and **AI** control, plus a server-side AI (`ServerEx3Algo`) that follows the same decision philosophy directly on `GameState`.


---

## Submission Artifacts (GitHub Release)

The submission is provided as a GitHub **Release** with the following files:

* **`Ex3_2.jar`** (Stage 2 runnable – course engine + my AI)
* **`Ex3_3.jar`** (Stage 3 runnable – my custom server game)
* **`Ex3_docs.zip`** (documentation)
* **`Ex3_all_src.zip`** (full source)

---

## Quick Start

### Stage 2 – Run the course engine with my AI

Run from the Stage 2 folder (assets must be next to the JAR):

```bash
java -jar Ex3_2.jar
```

Required files in the same folder as `Ex3_2.jar`:

* `test.bit`
* sprites (e.g., `g0.png` … `g3.png`, `p1.png`, etc.)

Example:

```
Ex3_2/
  Ex3_2.jar
  test.bit
  g0.png
  g1.png
  g2.png
  g3.png
  p1.png
```

### Stage 3 – Run my custom server game

```bash
java -jar Ex3_3.jar
```

`Ex3_3.jar` is **self-contained** and can be run from any folder.

---

## Controls (Stage 3)

At startup:

* Choose level: `0 / 1 / 2`
* Choose mode: `Manual` or `AI`
* Press **SPACE** to start the game

During the game:

* Press **T** to toggle **AI / Manual** control

---

## Level 4 Result (Stage 2)

Level 4 run output:

```
123456789, 4, 6426, 0, 414, 1768148488442, 4, -915428924, -1831613651031953
```

---

## Stage 2 – Ex3Algo (Pac-Man AI)

`Ex3Algo` is a stateful agent that makes one move per tick, with a simple and effective priority system:

### High-level priorities

1. **Escape** when danger ghosts are near

  * Measures threat using **BFS distance** to the closest non-eatable ghost.
  * Chooses moves that maximize safety and prefer open tiles (more exits).

2. **Eat fast** when safe

  * Uses **BFS shortest path** to the nearest target.
  * Targets are mainly **DOT**, with smart switching to **POWER** when strategically valuable.

3. **Stability / anti-loop behavior**

  * Avoids recently visited cells (loop memory)
  * Handles “stuck” situations by forcing a different legal move
  * Avoids immediate reverse direction when possible

### Power policy

* **POWER LOCK**: while power mode is active (ghosts are eatable), avoid stepping on another POWER tile.
* **NO POWER EARLY**: in the first ticks of the game, POWER tiles are treated as blocked to reduce early risk.

### Smart BFS tie-break

When multiple targets exist at the same minimal distance, the chosen candidate is selected by:

* larger distance from danger ghosts (safer)
* more exits (less trap-prone)
* preference to keep direction (smooth movement)
* penalty for revisiting recent positions (reduce loops)

---

## Stage 3 – Custom Server Implementation

Stage 3 includes a complete server-side Pac-Man game engine:

### Core components

* **`model/`**: `GameState`, `Ghost`, `Tile`, `Direction`, collision rules, scoring, power mode, etc.
* **`server/`**: `GameLoop`, `PacmanGameImpl` (adapter), `MyServerMain`
* **`server/control/`**: manual input, AI provider, toggle provider
* **`render/`**: `StdDrawRenderer` (graphics layer)
* **`levels/`**: `LevelLoader` and built-in maps

### Server-side AI (ServerEx3Algo)

`ServerEx3Algo` mirrors the Stage 2 philosophy but operates directly on `GameState`:

* BFS threat distance to danger ghosts
* escape vs eat-fast decision pipeline
* smart tie-breaking and loop/stuck recovery
* no-reverse smoothing

---

## Levels (Stage 3)

The game includes **3 levels** with increasing difficulty:

| Level | Description                       |
| ----: | --------------------------------- |
|     0 | Small map, few ghosts             |
|     1 | Medium map, more space and ghosts |
|     2 | Large map, higher difficulty      |

Each level increases board size and ghost count to gradually raise challenge.

---

## Testing

JUnit tests are included (JUnit 5) covering:

* core model validity (tiles, movement, collisions)
* level loading sanity checks
* AI movement sanity checks
* game loop smoke tests

The server was tested to run correctly both from IntelliJ and as a standalone JAR from an external folder.

---

## Demo Video

Gameplay + features demo (short):

```text
https://drive.google.com/file/d/17cYqks4-eDXt-K6LjcoXcQ0xNdk7t2Op/view?usp=drive_link
```

---
## Design Decisions

* The server was implemented without external frameworks (no Maven/Gradle) to fully control the game loop.
* AI logic was intentionally kept stateful to reduce oscillations and improve movement stability.
* Stage 2 and Stage 3 share the same decision philosophy to demonstrate conceptual consistency.
---

## Author

**Lidor Ayhoni**
