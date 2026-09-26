package legend.multimod;

import legend.game.EngineStates;
import legend.game.characters.CharacterData2c;
import legend.game.combat.Battle;
import legend.game.combat.types.CombatantStruct1a8;
import legend.game.combat.bent.PlayerBattleEntity;
import legend.game.combat.ui.AdditionListMenu;
import legend.game.combat.ui.ListMenu;
import legend.game.inventory.screens.AdditionsScreen;
import legend.game.inventory.screens.MenuScreen;
import legend.game.inventory.screens.MenuStack;
import legend.game.modding.events.battle.BattleEndedEvent;
import legend.game.modding.events.battle.BattleEntityTurnEvent;
import legend.game.modding.events.battle.BattleStartedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.legendofdragoon.modloader.registries.RegistryId;

import java.lang.reflect.Field;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static legend.core.GameEngine.REGISTRIES;
import static legend.game.SItem.menuStack;
import static legend.game.Scus94491BpeSegment_800b.gameState_800babc8;
import static legend.game.sound.Audio.playMenuSound;

public final class RandomAdditionsManager {
  private static final Logger LOGGER = LogManager.getFormatterLogger(RandomAdditionsManager.class);
  private static final Random RANDOM = new Random();

  /** Backed up additions prior to random assignment for battle */
  private static final Map<Integer, RegistryId> preBattleAdditions = new HashMap<>();

  /** Characters whose addition was manually changed in combat during the current battle */
  private static final Set<Integer> manualOverrideCharacters = new HashSet<>();

  /** The last random addition assigned to each character during this battle */
  private static final Map<Integer, RegistryId> lastAssignedRandomAdditions = new HashMap<>();

  /** Snapshot of selected additions when opening AdditionsScreen in camp menu to lock manual changes */
  private static final Map<Integer, RegistryId> additionsScreenSnapshot = new HashMap<>();
  private static boolean inAdditionsScreen = false;

  private static Field screensField;
  private static Field listMenuStateField;
  private static Field listMenuSelectionStateField;

  static {
    try {
      screensField = MenuStack.class.getDeclaredField("screens");
      screensField.setAccessible(true);
    } catch (final Throwable t) {
      LOGGER.error("RandomAdditionsManager: Failed to reflect MenuStack.screens", t);
    }
    try {
      listMenuStateField = ListMenu.class.getDeclaredField("menuState_00");
      listMenuStateField.setAccessible(true);
    } catch (final Throwable t) {
      LOGGER.warn("RandomAdditionsManager: Failed to reflect ListMenu.menuState_00", t);
    }
    try {
      listMenuSelectionStateField = ListMenu.class.getDeclaredField("selectionState_a0");
      listMenuSelectionStateField.setAccessible(true);
    } catch (final Throwable t) {
      LOGGER.warn("RandomAdditionsManager: Failed to reflect ListMenu.selectionState_a0", t);
    }
  }

  private RandomAdditionsManager() { }

  /**
   * Called when a battle begins.
   * If Random Additions is enabled, backs up original additions and pre-assigns
   * a random unlocked addition for each player combatant in the battle.
   */
  public static void onBattleStarted(final BattleStartedEvent event) {
    if (!TvvlrMultimod.isAdvancedAdditionsEnabled() || !TvvlrMultimod.isRandomAdditionsEnabled()) {
      return;
    }
    // Do not interfere if Master Tasman tutorial practice is active
    if (TasmanPracticeBattle.isPracticeActive() || TasmanBattleTutorial.isAdvancedTutorialActive()) {
      return;
    }

    preBattleAdditions.clear();
    manualOverrideCharacters.clear();
    lastAssignedRandomAdditions.clear();

    if (gameState_800babc8 == null || gameState_800babc8.charData_32c == null) {
      return;
    }

    // 1. Back up original selected addition for all characters
    for (int i = 0; i < gameState_800babc8.charData_32c.size(); i++) {
      final CharacterData2c c = gameState_800babc8.charData_32c.get(i);
      if (c != null && c.selectedAddition_19 != null) {
        preBattleAdditions.put(i, c.selectedAddition_19);
      }
    }

    // 2. Pre-assign a random unlocked addition for each player combatant in the battle
    final Battle battle = event.battle;
    for (int i = 0; i < 10; i++) {
      final CombatantStruct1a8 combatant = battle.getCombatant(i);
      if (combatant != null && combatant.isPlayer() && combatant.playerBent != null) {
        assignRandomAddition(battle, combatant.playerBent);
      }
    }
  }

  /**
   * Called when an entity's turn starts.
   * If Random Additions is enabled, rolls a new random addition for each player attack,
   * unless the character had their addition manually changed in combat during this battle.
   */
  public static void onTurnStarted(final BattleEntityTurnEvent<?> event) {
    if (!TvvlrMultimod.isAdvancedAdditionsEnabled() || !TvvlrMultimod.isRandomAdditionsEnabled()) {
      return;
    }
    if (TasmanPracticeBattle.isPracticeActive() || TasmanBattleTutorial.isAdvancedTutorialActive()) {
      return;
    }
    if (event != null && event.bent instanceof PlayerBattleEntity player) {
      final int charId = player.charId_272;

      // If the character's addition was manually selected during this battle, overrule random additions
      if (manualOverrideCharacters.contains(charId)) {
        LOGGER.info("RandomAdditions: Character %d (%s) addition %s is manually overridden for this battle - skipping random roll.",
            charId,
            player.character != null && player.character.getName() != null ? player.character.getName().get() : "",
            player.character != null ? player.character.selectedAddition_19 : "none");
        return;
      }

      // Check if selectedAddition_19 changed outside RandomAdditionsManager (i.e. via in-combat menu)
      final RegistryId lastRandom = lastAssignedRandomAdditions.get(charId);
      if (lastRandom != null && player.character != null && !lastRandom.equals(player.character.selectedAddition_19)) {
        manualOverrideCharacters.add(charId);
        player.selectedAddition_58 = player.character.selectedAddition_19;
        LOGGER.info("RandomAdditions: Detected manual addition change for character %d (%s -> %s) - overrules random additions for this battle.",
            charId,
            lastRandom,
            player.character.selectedAddition_19);
        return;
      }

      assignRandomAddition(event.battle, player);
    }
  }

  /**
   * Assigns a randomly selected unlocked addition to the player character and loads its attack animations.
   */
  public static void assignRandomAddition(final Battle battle, final PlayerBattleEntity player) {
    if (player == null || player.isDragoon()) {
      return;
    }
    final CharacterData2c charData = player.character;
    if (charData == null) {
      return;
    }
    final List<RegistryId> unlocked = charData.getUnlockedAdditions();
    if (unlocked == null || unlocked.isEmpty()) {
      return;
    }

    final RegistryId randomAddition = unlocked.get(RANDOM.nextInt(unlocked.size()));
    final RegistryId previousAddition = player.character.selectedAddition_19;

    charData.selectedAddition_19 = randomAddition;
    player.character.selectedAddition_19 = randomAddition;
    player.selectedAddition_58 = randomAddition;
    lastAssignedRandomAdditions.put(player.charId_272, randomAddition);

    final var entry = REGISTRIES.additions.getEntry(randomAddition);
    if (entry != null) {
      player.addition = entry.get();
    }

    final CombatantStruct1a8 combatant = player.combatant_144;
    if (combatant != null && battle != null) {
      if (!randomAddition.equals(previousAddition) || combatant.mrg_04 == null) {
        combatant.mrg_04 = null;
        battle.loadAttackAnimations(combatant);
      }
    }

    LOGGER.info("RandomAdditions: Character %d (%s) assigned random addition %s",
        player.charId_272,
        charData.getName() != null ? charData.getName().get() : "",
        randomAddition);
  }

  /**
   * Called during render tick. Checks camp menu lock and monitors in-combat addition menu changes.
   */
  public static void onRender() {
    checkMenuLock();
    checkCombatMenuChange();
  }

  /**
   * Monitors if the player confirms an addition swap via the in-combat AdditionListMenu.
   */
  private static void checkCombatMenuChange() {
    if (!TvvlrMultimod.isAdvancedAdditionsEnabled() || !TvvlrMultimod.isRandomAdditionsEnabled()) {
      return;
    }
    if (EngineStates.currentEngineState_8004dd04 instanceof Battle battle) {
      if (battle.hud != null && battle.hud.listMenu_800c6b60 instanceof AdditionListMenu menu) {
        final PlayerBattleEntity player = battle.hud.battleMenu_800c6c34.player_04;
        if (player != null) {
          int menuState = -1;
          int selectionState = 0;
          try {
            if (listMenuStateField != null) {
              menuState = listMenuStateField.getInt(menu);
            }
            if (listMenuSelectionStateField != null) {
              selectionState = listMenuSelectionStateField.getInt(menu);
            }
          } catch (final Throwable ignored) {
          }

          if (menuState == 8 || selectionState == 1) {
            if (player.character != null) {
              player.selectedAddition_58 = player.character.selectedAddition_19;
            }
            if (manualOverrideCharacters.add(player.charId_272)) {
              LOGGER.info("RandomAdditions: Character %d manually confirmed addition %s in combat - overrules random additions for this battle.",
                  player.charId_272,
                  player.character != null ? player.character.selectedAddition_19 : "unknown");
            }
          }
        }
      }
    }
  }

  /**
   * Called when battle ends.
   * Restores the character's addition to the pre-battle selection so that any in-combat manual selection
   * was only for that battle, and clears all battle override state.
   */
  public static void onBattleEnded(final BattleEndedEvent event) {
    manualOverrideCharacters.clear();
    lastAssignedRandomAdditions.clear();

    if (preBattleAdditions.isEmpty()) {
      return;
    }

    if (gameState_800babc8 != null && gameState_800babc8.charData_32c != null) {
      for (final Map.Entry<Integer, RegistryId> entry : preBattleAdditions.entrySet()) {
        final int charId = entry.getKey();
        if (charId < gameState_800babc8.charData_32c.size()) {
          final CharacterData2c c = gameState_800babc8.charData_32c.get(charId);
          if (c != null) {
            c.selectedAddition_19 = entry.getValue();
          }
        }
      }
      LOGGER.info("RandomAdditions: Battle ended. Reverted character additions back to pre-battle state.");
    }
    preBattleAdditions.clear();
  }

  /**
   * Checks if AdditionsScreen is open in the camp menu.
   * If Random Additions is enabled, prevents manual selection of additions in the camp menu.
   */
  public static void checkMenuLock() {
    if (!TvvlrMultimod.isAdvancedAdditionsEnabled() || !TvvlrMultimod.isRandomAdditionsEnabled()) {
      inAdditionsScreen = false;
      additionsScreenSnapshot.clear();
      return;
    }

    final MenuScreen currentScreen = getCurrentScreen();
    if (currentScreen instanceof AdditionsScreen) {
      if (!inAdditionsScreen) {
        // Just entered AdditionsScreen: take snapshot of all characters' selected additions
        inAdditionsScreen = true;
        additionsScreenSnapshot.clear();
        if (gameState_800babc8 != null && gameState_800babc8.charData_32c != null) {
          for (int i = 0; i < gameState_800babc8.charData_32c.size(); i++) {
            final CharacterData2c c = gameState_800babc8.charData_32c.get(i);
            if (c != null) {
              additionsScreenSnapshot.put(i, c.selectedAddition_19);
            }
          }
        }
      } else {
        // Continuously check if user tried to change selectedAddition_19 in AdditionsScreen
        if (gameState_800babc8 != null && gameState_800babc8.charData_32c != null) {
          for (final Map.Entry<Integer, RegistryId> entry : additionsScreenSnapshot.entrySet()) {
            final int charId = entry.getKey();
            if (charId < gameState_800babc8.charData_32c.size()) {
              final CharacterData2c c = gameState_800babc8.charData_32c.get(charId);
              if (c != null && c.selectedAddition_19 != null && !c.selectedAddition_19.equals(entry.getValue())) {
                // Revert change
                c.selectedAddition_19 = entry.getValue();
                playMenuSound(40); // Error buzz
                LOGGER.info("RandomAdditions: Blocked manual addition selection for character %d in AdditionsScreen", charId);
              }
            }
          }
        }
      }
    } else {
      if (inAdditionsScreen) {
        inAdditionsScreen = false;
        additionsScreenSnapshot.clear();
      }
    }
  }

  private static MenuScreen getCurrentScreen() {
    try {
      if (screensField != null && menuStack != null) {
        @SuppressWarnings("unchecked")
        final Deque<MenuScreen> screens = (Deque<MenuScreen>) screensField.get(menuStack);
        if (screens != null) {
          return screens.peek();
        }
      }
    } catch (final Throwable ignored) {
    }
    return null;
  }
}

