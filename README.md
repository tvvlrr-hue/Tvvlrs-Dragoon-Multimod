# Tvvlr's Dragoon Multimod

A standalone, all-in-one expansion mod for **The Legend of Dragoon: Severed Chains**. Multimod unifies advanced combat enhancements, interactive addition customization with real-time 3D model previews, Master Tasman practice battles, instant fast travel, custom victory dances, and the Lohan creature racing minigame—all configurable through a native in-game options menu.

---

## Features

### 1. In-Game Multimod Options Menu
Accessible directly from Severed Chains' in-game Options menu (**Options -> Tvvlr's Multimod**):
- Organised into distinct visual sections:
  - **`[ ADVANCED ADDITIONS ]`**
    - **Enabled**: Toggle multi-button additions on or off.
    - **Mode**: Cycle between `DEFAULT`, `RANDOM`, and `CUSTOM`.
    - **Configure Additions**: Appears when `CUSTOM` mode is active, opening the 3D addition configuration suite.
    - **Random Additions**: When enabled, each battle randomly selects an addition from each character's unlocked additions.
    - **Controller Colors**: Select controller button theme colors (`AUTO`, `PLAYSTATION`, `XBOX`, `SWITCH`).
  - **`[ RACING MINIGAME ]`**
    - **Enabled**: Toggle the Lohan racing attraction on or off.
    - **Free Entry (No Tickets)**: Toggle free test racing without needing festival tickets.
  - **`[ DANCE MODIFIER ]`**
    - **Enabled**: Toggle custom victory dance animations on or off.
    - **Configure Dances**: Opens the victory dance manager with real-time 3D model previews.
  - **`[ MASTER TASMAN ]`**
    - **Enabled**: Toggle Master Tasman city spawns and practice battle encounters on or off.
  - **`[ FAST TRAVEL ]`**
    - **Enabled**: Toggle Fast Travel button integration in the Main Pause Menu (Slot 11).
    - **Unlock All Locations**: Debug toggle to immediately unlock all world map destinations.
- **Smart Navigation**: Automatically skips section headers when navigating with D-Pad or Arrow Keys; supports gamepad Confirm / Cancel, mouse clicks, and bidirectional cycling.
- Automatically persists user preferences to `config.dcnf` and custom sequences to `tvvlr_custom_additions.json`.

---

### 2. Advanced Additions Combat System & 3D Interactive Configuration
- **Multi-Button Prompts**: Expands addition timing boxes beyond the default Cross (`X`) button to incorporate Square (`[]`) and Triangle (`/\`) inputs.
- **Dynamic Controller Themes**: Color cues styled after PlayStation, Xbox, and Nintendo Switch layouts, or auto-detected based on connected controllers.
- **Interactive 3D Configuration Menu**:
  - Live 3D character battle model preview for the selected character and addition hit.
  - **Manual 3D Rotation**: Freely rotate the character model using the Right Analog Stick or Page Up / Page Down keys.
  - **Hit Preview Mode**: Press **Triangle** (or **R**) on any hit to preview the character's exact pose and strike animation at that frame.
  - **Full Combo Demonstration**: Press **Square** (or **F**) to watch the entire addition combo executed in real time from start to finish.
  - Customize button prompts per hit for every addition across all 7 playable characters.
- Fully compatible with additions tutorials and practice sessions.

---

### 3. Random Additions Mode
- Automatically equips a random addition for each party member at the start of battle from their pool of unlocked additions.
- **Non-Destructive In-Combat Overrides**: Switching additions in-combat via the combat menu temporarily overrides the addition for that battle only, preserving your normal equipped additions outside of battle.

---

### 4. Master Tasman City Spawns & Practice Battles
- Master Tasman spawns across major cities throughout the campaign:
  - **Seles**
  - **Bale** (Slambert Plaza)
  - **Hoax** (Central Courtyard)
  - **Lohan**
  - **Fletz**
  - **Donau**
  - **Deningrad**
  - **Rouge**
- **Risk-Free Practice Arena**: Speak to Tasman to enter a dedicated practice battle where you can train addition timings with live feedback, hit counters, and addition switching.

---

### 5. Fast Travel System
- Integrated cleanly into the Main Pause Menu (**Slot 11**).
- Warp between discovered cities and dungeons instantly with smooth submap and world map transitions.
- Includes an optional "Unlock All Locations" mode in the Multimod options menu for exploration or testing.

---

### 6. Lohan Racing Minigame
- Adds a full creature racing minigame to the festival race booth in Lohan (Cut 151).
- Compete as a racer against two AI opponents across a 3-lap track spanning Cuts 151, 150, and 149.
- Features dynamic jumping over hurdles and water gaps, lap tracking, race results, and an NPC attendant.
- Optional free entry toggle to race without spending festival tickets.

---

### 7. Victory Dance Modifier
- Customize post-battle victory dances for party members.
- Procedural skeletal animation synthesizer powering unique dance routines.
- Includes a dedicated configuration screen with real-time 3D model previews.

---

## Installation

1. Download [`tvvlr-multimod.jar`](https://github.com/tvvlrr-hue/Tvvlrs-Dragoon-Multimod/releases/latest) from the [Releases](https://github.com/tvvlrr-hue/Tvvlrs-Dragoon-Multimod/releases) tab.
2. Place `tvvlr-multimod.jar` into the `mods/` folder of your Severed Chains installation:
   ```
   Severed_Chains/
   ├── mods/
   │   └── tvvlr-multimod.jar
   ```
3. Launch Severed Chains. The mod will automatically register and appear in the Options menu under **Tvvlr's Multimod**.

---

## Repository Structure

```
├── src/legend/multimod/
│   ├── AdditionButtonType.java             # Addition button definitions and color sets
│   ├── AdditionMode.java                   # Addition mode enum (DEFAULT, RANDOM, CUSTOM)
│   ├── AdvancedAdditionOverlaysEffect.java  # Combat additions overlay renderer & evaluator
│   ├── ControllerTheme.java                # Controller button theme detection and resolution
│   ├── CustomAdditionsConfigScreen.java    # 3D interactive addition configuration & preview screen
│   ├── CustomAdditionsStorage.java         # Data storage & JSON persistence for custom additions
│   ├── DanceModifierConfigScreen.java      # Victory dance configuration & preview screen
│   ├── DanceModifierStorage.java           # Victory dance data persistence
│   ├── DanceType.java                      # Dance type definitions
│   ├── FastTravelDummyObj.java             # Warp object proxy for fast travel transitions
│   ├── FastTravelLocations.java            # World map & submap destination coordinates registry
│   ├── FastTravelManager.java              # Pause menu injection and warp state handler
│   ├── FastTravelScreen.java               # Fast travel destination selection menu
│   ├── LohanRaceManager.java               # Lohan minigame circuit logic, camera, and physics
│   ├── LohanRaceNpc.java                   # Lohan ticket booth NPC and dialogue state machine
│   ├── MultimodOptionsScreen.java          # In-game sectional options UI screen
│   ├── ProceduralDanceSynthesizer.java     # Skeletal procedural animation synthesizer
│   ├── RandomAdditionsManager.java         # In-battle addition randomization logic
│   ├── TasmanBattleTutorial.java           # Battle tutorial addition practice integration
│   ├── TasmanNpcManager.java               # Master Tasman city spawning and dialogue
│   ├── TasmanPracticeBattle.java           # Practice battle initiator and arena setup
│   └── TvvlrMultimod.java                  # Main mod entry, config registrar, and menu injection
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

The compiled `tvvlr-multimod.jar` will be generated in the root directory and automatically copied to your local `mods/` directory if present.

---

## License

This project is licensed under the [GNU Affero General Public License v3.0](LICENSE).
