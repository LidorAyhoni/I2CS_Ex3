# Ex3 – Pac-Man Game
**Introduction to Computer Science – Ariel University**

Author: **Lidor Ayhoni**  
Java Version: **Java 21**

---

## 1. Project Overview

This project implements a complete **Pac-Man game** as part of **Exercise 3 (Ex3)** in the *Introduction to Computer Science* course at Ariel University.

The system combines:
- A **server-side game engine** that manages the game loop, state updates, collisions, ghost behavior, scoring, and power mode.
- A **client-side AI algorithm** (`Ex3Algo`) that controls Pac-Man according to the official Ex3 API.
- A **manual control mode** for human gameplay.
- A **live toggle mechanism** that allows switching between Manual and AI control during gameplay.
- A graphical interface based on the course-provided **StdDraw** library (used via a wrapper, without modifying the lecturer’s JAR).

The project demonstrates object-oriented design, separation of concerns, and a clear distinction between **game logic**, **input**, **rendering**, and **AI decision making**.

---

## 2. Gameplay Flow

1. The game starts with a **console menu**, where the user selects:
    - A level (0 / 1 / 2)
    - Initial control mode (Manual or AI)

2. The game window opens and waits for user input.
3. The game **starts only after pressing SPACE**.
4. During gameplay:
    - Pac-Man moves (manually or via AI)
    - Ghosts move autonomously
    - Collisions are resolved
    - Score, lives, and power mode are updated
5. At any time, the player can press **T** to toggle between AI and Manual control.

---

## 3. Controls

| Key | Action |
|----|------|
| Arrow Keys / WASD | Move Pac-Man (Manual mode) |
| **SPACE** | Start the game |
| **T** | Toggle AI / Manual control |

---

## 4. Levels and Difficulty Scaling

The game includes **three levels**, each increasing the difficulty:

- **Level 0**  
  Small board, few ghosts, basic navigation.

- **Level 1**  
  Larger board, more ghosts, increased path complexity.

- **Level 2 (XL)**  
  Very large board, multiple ghosts, high navigation and survival complexity.

As the level increases:
- The board dimensions grow
- The number of ghosts increases
- The AI must handle longer paths and higher risk

Levels are defined programmatically and loaded via `LevelLoader`.

---

## 5. Architecture Overview

Although all classes reside under the base package `assignments.Ex3`, the project is logically divided into the following layers:

---

### 5.1 Model Layer

Responsible for representing the **game state and rules**.

Key classes:
- **`GameState`**  
  Holds the entire mutable state of the game:
    - Tile grid
    - Pac-Man position
    - Ghost list
    - Score and lives
    - Power mode state and timer
    - Game over / win flags
    - Current control mode (AI / Manual)

- **`Pacman`**, **`Ghost`**, **`Entity`**  
  Represent game entities and their positions.

- **`CollisionSystem`**  
  Resolves interactions:
    - Pac-Man vs DOT
    - Pac-Man vs POWER
    - Pac-Man vs Ghost (with or without power mode)

- **`GhostMovement`**  
  Controls ghost movement logic independently of Pac-Man.

---

### 5.2 Server Layer (Game Engine)

Responsible for **running the game**.

- **`GameLoop`**  
  The core loop of the game.  
  On each tick:
    1. Determines Pac-Man’s next direction
    2. Moves Pac-Man
    3. Moves all ghosts
    4. Applies collision logic
    5. Updates power mode timer
    6. Renders the updated state

- **`InputController`**  
  Handles keyboard input:
    - Movement keys
    - Game start (SPACE)
    - Control toggle (T)

- **`MyServerMain`**  
  Entry point:
    - Console menu
    - Level loading
    - Game initialization
    - Game startup

---

### 5.3 Rendering Layer

Responsible only for **visual output**.

- **`StdDrawRenderer`**
    - Draws walls, dots, power pellets
    - Draws Pac-Man and ghosts
    - Ghosts change color during power mode
    - Renders HUD (Score, Lives, Power, Control Mode)

Rendering is completely decoupled from game logic.

---

### 5.4 Client AI Layer

The AI is treated as a **client** that communicates with the game engine via an adapter.

- **`Ex3Algo`**  
  Implements the official Ex3 algorithm interface.

- **`PacmanGameImpl`**  
  Adapter that exposes the server-side `GameState` through the `PacmanGame` API required by the AI.

- **Direction Providers**
    - `ManualDirectionProvider`
    - `AiDirectionProvider`
    - `ToggleDirectionProvider` – enables live switching between AI and Manual control

---

## 6. AI Algorithm – Detailed Explanation

The AI controls Pac-Man using **context-aware path-finding** based on the current game state.

---

### 6.1 Board Representation

- The game board is treated as a **grid-based graph**.
- Each walkable tile is a node.
- Valid moves correspond to edges between adjacent tiles.

---

### 6.2 Path-Finding

The AI uses **Breadth-First Search (BFS)** to compute shortest paths.

BFS is used to:
- Find the nearest DOT
- Find the nearest POWER pellet
- Chase ghosts during power mode
- Escape from nearby ghosts when in danger

BFS guarantees:
- Shortest path in number of steps
- Deterministic and predictable behavior

---

### 6.3 Decision Strategy

The AI operates in **three main modes**, evaluated every tick:

---

#### 1️⃣ Power Mode (Aggressive)

If power mode is active:
- Target the **nearest ghost**
- Move along the shortest path toward it
- Maximize score by eating ghosts safely

---

#### 2️⃣ Danger Mode (Defensive)

If power mode is inactive **and ghosts are nearby**:
- Evaluate paths that **increase distance from ghosts**
- Prefer safe tiles with fewer immediate threats
- Avoid corridors that can lead to dead ends

---

#### 3️⃣ Normal Mode (Collecting)

If no immediate danger exists:
- Target the **nearest DOT**
- If beneficial, prioritize a POWER pellet
- Choose shortest safe path

---

### 6.4 Direction Selection

Once a target is chosen:
1. BFS computes the shortest path
2. The **first step** of the path determines the next direction
3. This direction is returned to the server via the adapter

The same game logic is shared between AI and Manual control.

---

## 7. Testing

The project includes **JUnit tests**, primarily focused on validating:

- Correct behavior of `Ex3Algo`
- Legal and consistent direction decisions
- Compliance with the Ex3 API

All tests pass successfully.

---

## 8. How to Run

### Requirements
- Java **21**
- IntelliJ IDEA (recommended)

### Running the Game
1. Open the project in IntelliJ.
2. Run `MyServerMain`.
3. Choose level and control mode via the console.
4. Press **SPACE** to start the game.

---

## 9. Demo Video

A short demo video (up to 120 seconds) will be added before submission, demonstrating:
- Console level selection
- Manual gameplay
- Power mode behavior
- Ghost interactions
- AI control
- Live toggling between AI and Manual modes

---

## 10. Academic Integrity

This project was implemented independently and follows the academic integrity guidelines of Ariel University.

Only the officially provided course libraries were used.

---

## 11. Summary

This project demonstrates:
- Full server-side game loop implementation
- Clean separation between logic, rendering, input, and AI
- A functional and adaptive AI using BFS
- Stable and interactive gameplay
- A complete and playable Pac-Man game

---

