# Ex3 – Pac-Man (Server + Client)

## 📌 Overview
This project is a full implementation of **Exercise 3 – Pac-Man**, including:

- A **custom server-side game engine**
- A **rendering layer** based on StdDraw
- **Manual and AI-controlled Pac-Man**
- **Multiple levels with increasing difficulty**
- A sophisticated **AI algorithm (Ex3Algo)** adapted to run on a custom server

The project was implemented in Java **without Maven/Gradle**, following the course requirements.

---

## 🧠 Game Logic Summary

### 🎮 Core Mechanics
- Pac-Man moves on a grid-based map.
- Walls block movement.
- Dots increase score.
- Power pellets activate **Power Mode**.
- Ghosts move independently and interact with Pac-Man via collisions.

### 👻 Ghost Behavior
- Ghosts move **randomly but legally**:
  - Never enter walls
  - Prefer to keep direction if possible
  - Avoid immediate reverse unless necessary
- When Pac-Man eats a POWER:
  - All ghosts become **eatable** for a fixed duration (~5 seconds)
  - Eaten ghosts respawn at their original spawn point
- After Power Mode ends:
  - Ghosts become dangerous again

### 💥 Collision Rules
- Pac-Man + non-eatable ghost → lose a life
- Pac-Man + eatable ghost → gain score and ghost respawns
- When lives reach 0 → game over

---

## 🤖 AI – Ex3Algo

The Pac-Man AI is based on the **Ex3Algo** developed earlier in the course.

### AI priorities:
1. **Escape** when dangerous ghosts are nearby (maximize distance).
2. **Eat efficiently** using BFS shortest paths.
3. **Smart tie-breaking**:
  - Prefer safer positions
  - Avoid loops
  - Prefer open areas
4. **Power policy**:
  - Avoid POWER early unless needed
  - Use POWER strategically when danger is near
  - Do not step on POWER while already protected

### Integration
Because this project uses a **custom server**, the original `Ex3Algo` is integrated via:
- `PacmanGameImpl` – an adapter exposing `GameState` as `PacmanGame`
- `AiDirectionProvider` – bridges the algorithm into the server loop

This allows the **same AI logic** to run unchanged on a custom engine.

---

## 🗺️ Levels
The game includes **3 levels**:

| Level | Description |
|-----|------------|
| Level 0 | Small map, few ghosts |
| Level 1 | Medium map, more space and ghosts |
| Level 2 | Large map, higher difficulty |

Each level:
- Has a larger board
- Contains more ghosts
- Increases challenge gradually

---

## 🧩 Project Structure

src/
└─ assignments/Ex3
├─ model // GameState, Ghost, CollisionSystem, Direction, Tile
├─ render // Renderer, StdDrawRenderer
├─ server // GameLoop, MyServerMain, PacmanGameImpl
│ └─ control // Manual / AI / Toggle Direction Providers
├─ levels // LevelLoader (maps)
├─ Ex3Algo // Original AI algorithm

---

## ▶️ How to Run (IntelliJ – Recommended)

1. Open the project in IntelliJ
2. Make sure **Java 21** is selected as the Project SDK
3. Run:
   assignments.Ex3.server.MyServerMain

### At startup:
- Choose level (0 / 1 / 2)
- Choose mode:
- Manual (keyboard)
- AI
- Press **SPACE** to start the game
- Press **T** during the game to toggle AI / Manual

---

## ▶️ Run from JAR

The project can also be executed using the provided JAR file.

### Requirements
- Java **21** installed
- `java` command available in PATH

### Run command
```bash
java -jar Ex3_PacMan_Lidor.jar
```
If java is not in PATH, run using the full path to the Java executable:
```bash
"C:\Path\To\Java\bin\java.exe" -jar Ex3_PacMan_Lidor.jar
```
## 🧪 Testing

JUnit tests are included for core logic and algorithm components.

The game was tested manually across:

All 3 levels

Manual mode

AI mode

Runtime toggling between AI and Manual

## 🎥 Demo Video

Not included.

## 👤 Author

Lidor Ayhoni

