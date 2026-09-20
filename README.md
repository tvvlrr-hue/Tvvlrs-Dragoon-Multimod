# Tvvlr's Dragoon Multimod

A standalone, all-in-one mod for **The Legend of Dragoon: Severed Chains** that brings together the **Advanced Additions** combat system and the **Lohan Racing Minigame**, complete with an integrated in-game options menu for configuration.

---

## Features

### 1. In-Game Multimod Options Menu
Accessible directly from Severed Chains' in-game Options menu:
- Organised into clean visual sections with headers:
  - **`[ ADVANCED ADDITIONS ]`**
    - **Enabled**: Toggle multi-button additions on or off.
    - **Controller Colors**: Cycle theme colors (`AUTO`, `PLAYSTATION`, `XBOX`, `SWITCH`).
  - **`[ RACING MINIGAME ]`**
    - **Enabled**: Toggle the Lohan racing attraction on or off.
    - **Free Entry (No Tickets)**: Toggle free test racing without needing tickets.
- **Smart Navigation**: Smoothly skips section headers when navigating via D-Pad or Arrow Keys; supports gamepad Confirm / Cancel, mouse clicks, and bidirectional cycling.
- Automatically saves preferences to `config.dcnf`.

### 2. Advanced Additions Combat System
- Overhauls the battle addition timing system to support multi-button prompts (`Square` and `Triangle` alongside `Cross`).
- **Dynamic Controller Themes**: Color cues styled after PlayStation, Xbox, and Nintendo Switch layouts, or auto-detected based on connected controllers.
- Full compatibility with addition practice tutorials in Tasman.

### 3. Lohan Racing Minigame
- Adds a full creature racing minigame to the festival race booth in Lohan (Cut 151).
- Compete as a racer against two AI opponents across a 3-lap track spanning Cuts 151, 150, and 149.
- Features dynamic jumping over hurdles and water gaps, lap tracking, race results, and an NPC attendant.

---

## Installation

1. Download [`tvvlr-multimod.jar`](tvvlr-multimod.jar) from this repository.
2. Place `tvvlr-multimod.jar` into the `mods/` folder of your Severed Chains installation:
   ```
   Severed_Chains/
   ├── mods/
   │   └── tvvlr-multimod.jar
   ```
3. Start Severed Chains. The mod will automatically register and appear in the Options menu under **Tvvlr's Multimod**.

---

## Repository Structure

```
├── src/legend/multimod/
│   ├── TvvlrMultimod.java                  # Main mod entry, config registrar, and menu injection
│   ├── MultimodOptionsScreen.java          # In-game sectional options UI screen
│   ├── AdditionButtonType.java             # Addition button definitions and color sets
│   ├── AdvancedAdditionOverlaysEffect.java  # Combat additions overlay renderer & evaluator
│   ├── ControllerTheme.java                # Controller button theme detection and resolution
│   ├── LohanRaceManager.java               # Lohan minigame circuit logic, camera, and physics
│   ├── LohanRaceNpc.java                   # Lohan ticket booth NPC and dialogue state machine
│   └── TasmanBattleTutorial.java           # Battle tutorial addition practice integration
├── resources/tvvlr_multimod/lang/
│   └── en.lang                             # English localized text strings
├── tvvlr-multimod.jar                      # Pre-built ready-to-use mod JAR
├── build.bat                               # Build script for Windows
├── LICENSE                                 # GNU Affero General Public License v3
└── README.md                               # Project documentation
```

---

## Building from Source

Requires **JDK 25** and the Severed Chains engine libraries.

Run the build script from the repository root:
```bat
build.bat
```

The resulting `tvvlr-multimod.jar` will be compiled and packaged automatically.

---

## License

This project is licensed under the [GNU Affero General Public License v3.0](LICENSE).
