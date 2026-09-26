package legend.multimod;

import legend.game.characters.CharacterData2c;
import legend.game.combat.Battle;
import legend.game.combat.types.CombatantStruct1a8;
import legend.game.inventory.screens.AdditionsScreen;
import legend.game.inventory.screens.MenuScreen;
import legend.game.inventory.screens.MenuStack;
import legend.game.modding.events.battle.BattleEndedEvent;
import legend.game.modding.events.battle.BattleStartedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.legendofdragoon.modloader.registries.RegistryId;

import java.lang.reflect.Field;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static legend.core.GameEngine.REGISTRIES;
import static legend.game.SItem.menuStack;
import static legend.game.Scus94491BpeSegment_800b.gameState_800babc8;
import static legend.game.sound.Audio.playMenuSound;

public final class RandomAdditionsManager {
  private static final Logger LOGGER = LogManager.getFormatterLogger(RandomAdditionsManager.class);
  private static final Random RANDOM = new Random();

  /** Backed up additions prior to random assignment for battle */
  private static final Map<Integer, RegistryId> preBattleAdditions = new HashMap<>();

  /** Snapshot of selected additions when opening AdditionsScreen in camp menu to lock manual changes */
  private static final Map<Integer, RegistryId> additionsScreenSnapshot = new HashMap<>();
  private static boolean inAdditionsScreen = false;

  private static Field screensField;
  static {
    try {
      screensField = MenuStack.class.getDeclaredField("screens");
      screensField.setAccessible(true);
    } catch (final Throwable t) {
      LOGGER.error("RandomAdditionsManager: Failed to reflect MenuStack.screens", t);
    }
  }

  private RandomAdditionsManager() { }

  /**
   * Called when a battle begins.
   * If Random Additions is enabled, rolls a random addition from each character's unlocked additions.
   * In-battle manual changes via combat menu will apply to the battle, but revert when the battle ends.
   */
  public static void onBattleStarted(final BattleStartedEvent event) {
    if (!TvvlrMultimod.isAdvancedAdditionsEnabled() || !TvvlrMultimod.isRandomAdditionsEnabled()) {
      return;
    }
    // Do not interfere if Master Tasman tutorial practice is active
    if (TasmanPracticeBattle.isPracticeActive()) {
      return;
    }

    preBattleAdditions.clear();

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

    // 2. For each player combatant in the battle, pick a random unlocked addition
    final Battle battle = event.battle;
    for (int i = 0; i < 10; i++) {
      final CombatantStruct1a8 combatant = battle.getCombatant(i);
      if (combatant != null && combatant.isPlayer() && combatant.playerBent != null) {
        final CharacterData2c charData = combatant.playerBent.character;
        if (charData != null) {
          final List<RegistryId> unlocked = charData.getUnlockedAdditions();
          if (unlocked != null && !unlocked.isEmpty()) {
            final RegistryId randomAddition = unlocked.get(RANDOM.nextInt(unlocked.size()));
            charData.selectedAddition_19 = randomAddition;
            combatant.playerBent.character.selectedAddition_19 = randomAddition;
            final var entry = REGISTRIES.additions.getEntry(randomAddition);
            if (entry != null) {
              combatant.playerBent.addition = entry.get();
            }
            combatant.mrg_04 = null;
            battle.loadAttackAnimations(combatant);
            LOGGER.info("RandomAdditions: Character %d (%s) rolled random addition %s for battle",
                combatant.charIndex_1a2,
                charData.getName() != null ? charData.getName().get() : "",
                randomAddition);
          }
        }
      }
    }
  }

  /**
   * Called when battle ends.
   * Restores the character's addition to the pre-battle selection so that any in-combat manual selection
   * was only for that battle.
   */
  public static void onBattleEnded(final BattleEndedEvent event) {
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
   * If Random Additions is enabled, prevents manual selection of additions in the menu.
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
