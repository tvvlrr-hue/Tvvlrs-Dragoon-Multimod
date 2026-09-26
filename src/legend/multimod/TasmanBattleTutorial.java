package legend.multimod;

import legend.game.EngineStates;
import legend.game.characters.CharacterAdditionInfo;
import legend.game.characters.CharacterData2c;
import legend.game.combat.Battle;
import legend.game.combat.bent.BattleEntity27c;
import legend.game.combat.bent.PlayerBattleEntity;
import legend.game.combat.types.CombatantStruct1a8;
import legend.game.modding.events.RenderEvent;
import legend.game.modding.events.battle.BattleEndedEvent;
import legend.game.modding.events.battle.BattleEntityTurnEvent;
import legend.game.modding.events.battle.BattleStartedEvent;
import legend.game.scripting.FlowControl;
import legend.game.scripting.RunningScript;
import legend.game.scripting.ScriptState;
import legend.game.types.LodString;
import legend.game.types.Textbox4c;
import legend.game.types.TextboxChar08;
import legend.game.types.TextboxState;
import legend.game.types.TextboxText84;
import legend.game.types.TextboxTextState;
import legend.lodmod.LodAdditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.legendofdragoon.modloader.registries.RegistryId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static legend.core.GameEngine.REGISTRIES;
import static legend.game.Scus94491BpeSegment_8006.battleState_8006e398;
import static legend.game.Scus94491BpeSegment_800b.gameState_800babc8;
import static legend.game.Text.textboxText_800bdf38;
import static legend.game.Text.textboxes_800be358;

public class TasmanBattleTutorial {
  private static final Logger LOGGER = LogManager.getFormatterLogger(TasmanBattleTutorial.class);

  public record AdditionEntry(String name, RegistryId id) {}

  public enum TutorialState {
    IDLE,
    SELECTING_CHARACTER,
    SHOWING_ARCHER_WARNING,
    SELECTING_ADDITION,
    PRACTICE_ACTIVE,
    RETRY_MENU,
    SELECTING_CHARACTER_CHANGE,
    SELECTING_ADDITION_CHANGE
  }

  // Registry of additions for each character by charData_32c index:
  // 0: Dart, 1: Lavitz, 2: Shana, 3: Rose, 4: Haschel, 5: Albert, 6: Meru, 7: Kongol, 8: Miranda
  private static final Map<Integer, List<AdditionEntry>> CHARACTER_ADDITIONS = new LinkedHashMap<>();

  static {
    // 0: Dart
    CHARACTER_ADDITIONS.put(0, List.of(
      new AdditionEntry("Double Slash", LodAdditions.DOUBLE_SLASH.getId()),
      new AdditionEntry("Volcano", LodAdditions.VOLCANO.getId()),
      new AdditionEntry("Burning Rush", LodAdditions.BURNING_RUSH.getId()),
      new AdditionEntry("Crush Dance", LodAdditions.CRUSH_DANCE.getId()),
      new AdditionEntry("Madness Hero", LodAdditions.MADNESS_HERO.getId()),
      new AdditionEntry("Moon Strike", LodAdditions.MOON_STRIKE.getId()),
      new AdditionEntry("Blazing Dynamo", LodAdditions.BLAZING_DYNAMO.getId())
    ));

    // 1: Lavitz
    CHARACTER_ADDITIONS.put(1, List.of(
      new AdditionEntry("Harpoon", LodAdditions.HARPOON.getId()),
      new AdditionEntry("Spinning Cane", LodAdditions.SPINNING_CANE.getId()),
      new AdditionEntry("Rod Typhoon", LodAdditions.ROD_TYPHOON.getId()),
      new AdditionEntry("Gust of Wind Dance", LodAdditions.GUST_OF_WIND_DANCE.getId()),
      new AdditionEntry("Flower Storm", LodAdditions.FLOWER_STORM.getId())
    ));

    // 3: Rose
    CHARACTER_ADDITIONS.put(3, List.of(
      new AdditionEntry("Whip Smack", LodAdditions.WHIP_SMACK.getId()),
      new AdditionEntry("More & More", LodAdditions.MORE_MORE.getId()),
      new AdditionEntry("Hard Blade", LodAdditions.HARD_BLADE.getId()),
      new AdditionEntry("Demon's Dance", LodAdditions.DEMONS_DANCE.getId())
    ));

    // 4: Haschel
    CHARACTER_ADDITIONS.put(4, List.of(
      new AdditionEntry("Double Punch", LodAdditions.DOUBLE_PUNCH.getId()),
      new AdditionEntry("Flurry of Styx", LodAdditions.FERRY_OF_STYX.getId()),
      new AdditionEntry("Summon 4 Gods", LodAdditions.SUMMON_4_GODS.getId()),
      new AdditionEntry("5 Ring Shattering", LodAdditions.FIVE_RING_SHATTERING.getId()),
      new AdditionEntry("Hex Hammer", LodAdditions.HEX_HAMMER.getId()),
      new AdditionEntry("Omni-Sweep", LodAdditions.OMNI_SWEEP.getId())
    ));

    // 5: Albert
    CHARACTER_ADDITIONS.put(5, List.of(
      new AdditionEntry("Harpoon", LodAdditions.ALBERT_HARPOON.getId()),
      new AdditionEntry("Spinning Cane", LodAdditions.ALBERT_SPINNING_CANE.getId()),
      new AdditionEntry("Rod Typhoon", LodAdditions.ALBERT_ROD_TYPHOON.getId()),
      new AdditionEntry("Gust of Wind Dance", LodAdditions.ALBERT_GUST_OF_WIND_DANCE.getId()),
      new AdditionEntry("Flower Storm", LodAdditions.ALBERT_FLOWER_STORM.getId())
    ));

    // 6: Meru
    CHARACTER_ADDITIONS.put(6, List.of(
      new AdditionEntry("Double Smack", LodAdditions.DOUBLE_SMACK.getId()),
      new AdditionEntry("Hammer Spin", LodAdditions.HAMMER_SPIN.getId()),
      new AdditionEntry("Cool Boogie", LodAdditions.COOL_BOOGIE.getId()),
      new AdditionEntry("Cat's Cradle", LodAdditions.CATS_CRADLE.getId()),
      new AdditionEntry("Perky Step", LodAdditions.PERKY_STEP.getId())
    ));

    // 7: Kongol
    CHARACTER_ADDITIONS.put(7, List.of(
      new AdditionEntry("Pursuit", LodAdditions.PURSUIT.getId()),
      new AdditionEntry("Inferno", LodAdditions.INFERNO.getId()),
      new AdditionEntry("Bone Crush", LodAdditions.BONE_CRUSH.getId())
    ));
  }

  private static boolean initialized = false;
  private static boolean isTasmanBattle = false;
  private static boolean isTopicMenuOpen = false;
  private static boolean isRetryMenuOpen = false;
  private static boolean isAdvancedTutorialActive = false;
  private static boolean isCounterTutorialActive = false;
  private static TutorialState state = TutorialState.IDLE;

  private static int selectedCharacterId = 0;
  private static RegistryId selectedAdditionId = null;
  private static boolean preAttackPromptShown = false;

  // Backup of all party characters' additions
  private static final Map<Integer, RegistryId> originalAdditions = new LinkedHashMap<>();
  // Tracks any addition info temporarily created for practice so NPEs don't happen
  private static final Map<Integer, List<RegistryId>> temporarilyAddedAdditions = new LinkedHashMap<>();

  public static boolean isAdvancedTutorialActive() {
    return isTasmanBattle && isAdvancedTutorialActive;
  }

  public static boolean isCounterTutorialActive() {
    return isTasmanBattle && isCounterTutorialActive;
  }

  private static Function<RunningScript, FlowControl> originalAddTextbox;
  private static Function<RunningScript, FlowControl> originalGetSelectionIndex;
  private static Function<RunningScript, FlowControl> originalAddSelectionTextbox;
  private static Function<RunningScript, FlowControl> originalIsTextboxInitialized;

  public static void init() {
    if (initialized) {
      return;
    }
    ScriptHookUtil.hookScriptSubFunction(200, TasmanBattleTutorial::hookAddTextbox, orig -> originalAddTextbox = orig);
    ScriptHookUtil.hookScriptSubFunction(205, TasmanBattleTutorial::hookGetSelectionIndex, orig -> originalGetSelectionIndex = orig);
    ScriptHookUtil.hookScriptSubFunction(207, TasmanBattleTutorial::hookAddSelectionTextbox, orig -> originalAddSelectionTextbox = orig);
    ScriptHookUtil.hookScriptSubFunction(195, TasmanBattleTutorial::hookIsTextboxInitialized, orig -> originalIsTextboxInitialized = orig);

    initialized = true;
    LOGGER.info("TasmanBattleTutorial: Successfully initialized script opcode hooks.");
  }

  public static void onBattleStarted(final BattleStartedEvent event) {
    if (event.encounter != null && event.encounter.uniqueIds.contains(254)) {
      isTasmanBattle = true;
      isTopicMenuOpen = false;
      isRetryMenuOpen = false;
      isAdvancedTutorialActive = false;
      preAttackPromptShown = false;
      state = TutorialState.IDLE;
      selectedCharacterId = 0;
      selectedAdditionId = null;

      backupPartyAdditions();
      LOGGER.info("TasmanBattleTutorial: Tasman practice battle started. Backed up party additions.");
    } else {
      isTasmanBattle = false;
      isTopicMenuOpen = false;
      isRetryMenuOpen = false;
      isAdvancedTutorialActive = false;
      isCounterTutorialActive = false;
      preAttackPromptShown = false;
      state = TutorialState.IDLE;
      originalAdditions.clear();
      temporarilyAddedAdditions.clear();
    }
  }

  public static void onBattleEnded(final BattleEndedEvent event) {
    restoreAllAdditions();
    isTasmanBattle = false;
    isTopicMenuOpen = false;
    isRetryMenuOpen = false;
    isAdvancedTutorialActive = false;
    isCounterTutorialActive = false;
    preAttackPromptShown = false;
    state = TutorialState.IDLE;
    originalAdditions.clear();
    temporarilyAddedAdditions.clear();
  }

  public static void onTurnStarted(final BattleEntityTurnEvent<?> event) {
    if (!isTasmanBattle || !isAdvancedTutorialActive) {
      return;
    }

    if (event.state != null && event.state.innerStruct_00 instanceof PlayerBattleEntity player) {
      if (player.charId_272 != selectedCharacterId) {
        if (battleState_8006e398 != null && battleState_8006e398.playerBents_e40 != null) {
          for (final ScriptState<PlayerBattleEntity> bentState : battleState_8006e398.playerBents_e40) {
            if (bentState != null && bentState.innerStruct_00 != null && bentState.innerStruct_00.charId_272 == selectedCharacterId) {
              event.state.clearFlag(BattleEntity27c.FLAG_CURRENT_TURN).clearFlag(BattleEntity27c.FLAG_RELOAD_BATTLE_ACTIONS);
              bentState.setFlag(BattleEntity27c.FLAG_CURRENT_TURN).setFlag(BattleEntity27c.FLAG_RELOAD_BATTLE_ACTIONS);
              event.battle.currentTurnBent_800c66c8 = bentState;
              event.battle.forcedTurnBent_800c66bc = bentState;
              event.battle.cameraFocusedBent_800c6914 = bentState;
              LOGGER.info("TasmanBattleTutorial: Redirected player turn from %s to %s!",
                  player.getName(), bentState.innerStruct_00.getName());
              break;
            }
          }
        }
      }
    }
  }

  public static void onRender(final RenderEvent event) {
    if (!isTasmanBattle || !TvvlrMultimod.isAdvancedAdditionsEnabled()) {
      return;
    }

    if (isAdvancedTutorialActive && (state == TutorialState.PRACTICE_ACTIVE || state == TutorialState.SELECTING_ADDITION || state == TutorialState.SELECTING_CHARACTER)) {
      setForcedTurnCharacter(selectedCharacterId);
      if (EngineStates.currentEngineState_8004dd04 instanceof final Battle battle) {
        if (battle.currentTurnBent_800c66c8 != null && battle.currentTurnBent_800c66c8.innerStruct_00 instanceof PlayerBattleEntity player) {
          if (player.charId_272 != selectedCharacterId) {
            if (battleState_8006e398 != null && battleState_8006e398.playerBents_e40 != null) {
              for (final ScriptState<PlayerBattleEntity> bentState : battleState_8006e398.playerBents_e40) {
                if (bentState != null && bentState.innerStruct_00 != null && bentState.innerStruct_00.charId_272 == selectedCharacterId) {
                  battle.currentTurnBent_800c66c8.clearFlag(BattleEntity27c.FLAG_CURRENT_TURN).clearFlag(BattleEntity27c.FLAG_RELOAD_BATTLE_ACTIONS);
                  bentState.setFlag(BattleEntity27c.FLAG_CURRENT_TURN).setFlag(BattleEntity27c.FLAG_RELOAD_BATTLE_ACTIONS);
                  battle.currentTurnBent_800c66c8 = bentState;
                  battle.forcedTurnBent_800c66bc = bentState;
                  battle.cameraFocusedBent_800c6914 = bentState;
                  LOGGER.info("TasmanBattleTutorial: onRender redirected current turn to %s!", bentState.innerStruct_00.getName());
                  break;
                }
              }
            }
          }
        }
      }
    }

    final TextboxText84 textboxText = textboxText_800bdf38[0];
    if (textboxText == null) {
      return;
    }

    if (isTopicMenuOpen) {
      if (textboxText.state_00 != TextboxTextState.UNINITIALIZED_0 && textboxText.selectionIndex_6c < 0) {
        if (textboxText.state_00 != TextboxTextState.SELECTION_22
            && textboxText.state_00 != TextboxTextState.TICK_STATE_TRANSITION_TIMER_19
            && textboxText.state_00 != TextboxTextState.PROCESS_STATE_TRANSITION_18) {
          textboxText.state_00 = TextboxTextState.SELECTION_22;
          textboxText.flags_08 |= TextboxText84.SELECTION;
          textboxText.minSelectionLine_72 = 0;
          textboxText.maxSelectionLine_70 = 5;
          if (textboxText.selectionLine_68 < 0 || textboxText.selectionLine_68 > 5) {
            textboxText.selectionLine_68 = 0;
          }
        }
      }
    } else if (isRetryMenuOpen && isAdvancedTutorialActive) {
      if (textboxText.state_00 != TextboxTextState.UNINITIALIZED_0 && textboxText.selectionIndex_6c < 0) {
        if (textboxText.state_00 != TextboxTextState.SELECTION_22
            && textboxText.state_00 != TextboxTextState.TICK_STATE_TRANSITION_TIMER_19
            && textboxText.state_00 != TextboxTextState.PROCESS_STATE_TRANSITION_18) {
          textboxText.state_00 = TextboxTextState.SELECTION_22;
          textboxText.flags_08 |= TextboxText84.SELECTION;
          textboxText.minSelectionLine_72 = 0;
          textboxText.maxSelectionLine_70 = 3;
          if (textboxText.selectionLine_68 < 0 || textboxText.selectionLine_68 > 3) {
            textboxText.selectionLine_68 = 0;
          }
        }
      }
    } else if (isAdvancedTutorialActive && (state == TutorialState.SELECTING_CHARACTER || state == TutorialState.SELECTING_CHARACTER_CHANGE)) {
      final List<Integer> party = getActivePartyMembers();
      final int maxLine = party.size() + 1; // Options 1..party.size() + 1 (Back)
      if (textboxText.state_00 != TextboxTextState.UNINITIALIZED_0 && textboxText.selectionIndex_6c < 0) {
        if (textboxText.state_00 != TextboxTextState.SELECTION_22
            && textboxText.state_00 != TextboxTextState.TICK_STATE_TRANSITION_TIMER_19
            && textboxText.state_00 != TextboxTextState.PROCESS_STATE_TRANSITION_18) {
          textboxText.state_00 = TextboxTextState.SELECTION_22;
          textboxText.flags_08 |= TextboxText84.SELECTION;
          textboxText.minSelectionLine_72 = 1;
          textboxText.maxSelectionLine_70 = maxLine;
          if (textboxText.selectionLine_68 < 1 || textboxText.selectionLine_68 > maxLine) {
            textboxText.selectionLine_68 = 1;
          }
        }
      }
    } else if (isAdvancedTutorialActive && (state == TutorialState.SELECTING_ADDITION || state == TutorialState.SELECTING_ADDITION_CHANGE)) {
      final List<AdditionEntry> additions = getAdditionsForCharacter(selectedCharacterId);
      final int maxLine = additions.size(); // Options 1..additions.size()
      if (textboxText.state_00 != TextboxTextState.UNINITIALIZED_0 && textboxText.selectionIndex_6c < 0) {
        if (textboxText.state_00 != TextboxTextState.SELECTION_22
            && textboxText.state_00 != TextboxTextState.TICK_STATE_TRANSITION_TIMER_19
            && textboxText.state_00 != TextboxTextState.PROCESS_STATE_TRANSITION_18) {
          textboxText.state_00 = TextboxTextState.SELECTION_22;
          textboxText.flags_08 |= TextboxText84.SELECTION;
          textboxText.minSelectionLine_72 = 1;
          textboxText.maxSelectionLine_70 = maxLine;
          if (textboxText.selectionLine_68 < 1 || textboxText.selectionLine_68 > maxLine) {
            textboxText.selectionLine_68 = 1;
          }
        }
      }
    }
  }

  private static FlowControl hookAddTextbox(final RunningScript script) {
    final FlowControl result = originalAddTextbox != null ? originalAddTextbox.apply(script) : FlowControl.CONTINUE;
    if (!isTasmanBattle || !TvvlrMultimod.isAdvancedAdditionsEnabled()) {
      return result;
    }

    try {
      final int textboxIndex = script.params_20[0].get();
      final Textbox4c textbox = textboxes_800be358[textboxIndex];
      final TextboxText84 textboxText = textboxText_800bdf38[textboxIndex];

      if (textboxText != null && textboxText.str_24 != null) {
        final String text = textboxText.str_24.get();

        // 1. Tasman Topic Menu
        if (text.contains("Addition Skills.") && text.contains("About Counterattacking.")) {
          backupPartyAdditions();
          restoreAllAdditions();
          isCounterTutorialActive = false;
          final String newMenu =
            "\"Addition Skills.\"\n" +
            "\"About Counterattacking.\"\n" +
            "\"About Defense.\"\n" +
            "\"Obtaining Additions.\"\n" +
            "\"Advanced Additions.\"\n" +
            "\"I am fine.\"";
          textboxText.str_24 = toLodString(newMenu);
          setupTextboxLines(textbox, textboxText, 6, 28);
          textboxText.charIndex_30 = 0;
          textboxText.charX_34 = 0;
          textboxText.charY_36 = 0;
          textboxText.state_00 = TextboxTextState.PROCESS_TEXT_4;
          populateAndMakeSelection(textboxIndex, 0, 5);
          isTopicMenuOpen = true;
          isRetryMenuOpen = false;
          isAdvancedTutorialActive = false;
          state = TutorialState.IDLE;
          LOGGER.info("TasmanBattleTutorial: Injected 'Advanced Additions.' into Tasman's topic menu.");
        }
        // 2. Character Picker (When clicking into Advanced Additions - immediately prompt who wants to practice!)
        else if (isAdvancedTutorialActive && (state == TutorialState.SELECTING_CHARACTER || state == TutorialState.SELECTING_CHARACTER_CHANGE || state == TutorialState.IDLE)
            && (text.contains("Select attack command.") || text.contains("When this sight overlaps") || text.contains("overlaps"))) {
          openCharacterPicker(textboxIndex);
        }
        // 3. Addition Picker (When advancing or prompt displayed)
        else if (isAdvancedTutorialActive && (state == TutorialState.SELECTING_ADDITION || state == TutorialState.SELECTING_ADDITION_CHANGE)
            && (text.contains("When this sight overlaps") || text.contains("overlaps") || text.contains("Select attack command."))) {
          openAdditionPicker(textboxIndex, selectedCharacterId);
        }
        // 4. Pre-Attack encouragement: replaces "Now, come on!" or "Select attack command." (only show ONCE)
        else if (isAdvancedTutorialActive && state == TutorialState.PRACTICE_ACTIVE
            && (text.contains("Now, come on!") || text.contains("Select attack command.") || text.contains("When this sight overlaps") || text.contains("overlaps"))) {
          if (!preAttackPromptShown) {
            preAttackPromptShown = true;
            final String prompt = "Show me your skill, " + getCharacterName(selectedCharacterId) + "!\nCome at me!";
            textboxText.str_24 = toLodString(prompt);
            setupTextboxLines(textbox, textboxText, 2, 32);
            setForcedTurnCharacter(selectedCharacterId);
            LOGGER.info("TasmanBattleTutorial: Ready for attack with %s (prompt shown once)", getCharacterName(selectedCharacterId));
          } else {
            // Already told character to come at him once; auto-dismiss redundant secondary prompt
            textboxText.delete();
            textboxText.state_00 = TextboxTextState.UNINITIALIZED_0;
            textboxes_800be358[textboxIndex].state_00 = TextboxState.UNINITIALIZED_0;
            legend.game.Text.setTextboxArrowPosition(textboxIndex, false);
            LOGGER.info("TasmanBattleTutorial: Dismissed secondary prompt to avoid repeating.");
          }
        }
        // Normal Attack Tutorial (Addition Skills): replace '+' with Square icon '□' and say 'Corresponding button'
        else if (!isAdvancedTutorialActive && text.contains("When this sight overlaps")) {
          final String attackExplain =
            "When this sight overlaps\n" +
            "the □ in the center, press\n" +
            "the Corresponding button.\f" +
            "Gray □ means too fast\n" +
            "Blue □ means too slow\n" +
            "White □ means perfect.";
          textboxText.str_24 = toLodString(attackExplain);
          setupTextboxLines(textbox, textboxText, 3, 28);
          LOGGER.info("TasmanBattleTutorial: Displaying Attack Tutorial with Square icon.");
        }
        // Normal Counter Tutorial (About Counterattacking): replace '+' with Square icon '□'
        else if (!isAdvancedTutorialActive && (text.contains("counterattack sight appears") || (text.contains("counterattack") && text.contains("overlaying")))) {
          final String counterExplain =
            "Press the @ button when the red\n" +
            "counterattack sight appears \n" +
            "overlaying the □.";
          textboxText.str_24 = toLodString(counterExplain);
          setupTextboxLines(textbox, textboxText, 3, 32);
          LOGGER.info("TasmanBattleTutorial: Displaying Counter Tutorial with Square icon.");
        }
        // 5. Tasman Retry Prompt after an attack: "How are you doing? One more time?"
        else if (isAdvancedTutorialActive && (text.contains("How are you doing?") || text.contains("One more time?"))) {
          final String retryPrompt =
            "How was that addition?\n" +
            "What would you like to do?";
          textboxText.str_24 = toLodString(retryPrompt);
          setupTextboxLines(textbox, textboxText, 2, 28);
          LOGGER.info("TasmanBattleTutorial: Displaying retry prompt question.");
        }
        // 6. Tasman Retry Menu options: replaces "Let me do it again." / "I'm fine now." with 4 choices:
        // "Try again.", "Change addition.", "Change character.", "I'm fine now."
        else if (isAdvancedTutorialActive && text.contains("Let me do it again.") && text.contains("I'm fine now.")) {
          openRetryMenu(textboxIndex);
        }
      }
    } catch (final Throwable t) {
      LOGGER.error("TasmanBattleTutorial: Error in hookAddTextbox", t);
    }

    return result;
  }

  private static FlowControl hookAddSelectionTextbox(final RunningScript script) {
    final FlowControl result = originalAddSelectionTextbox != null ? originalAddSelectionTextbox.apply(script) : FlowControl.CONTINUE;
    if (!isTasmanBattle || !TvvlrMultimod.isAdvancedAdditionsEnabled()) {
      return result;
    }

    try {
      final int textboxIndex = script.params_20[0].get();
      final TextboxText84 textboxText = textboxText_800bdf38[textboxIndex];
      if (textboxText != null) {
        if (isTopicMenuOpen) {
          textboxText.minSelectionLine_72 = 0;
          textboxText.maxSelectionLine_70 = 5;
          textboxText.selectionIndex_6c = -1;
          textboxText.flags_08 |= TextboxText84.SELECTION;
          textboxText.state_00 = TextboxTextState.SELECTION_22;
        } else if (isRetryMenuOpen) {
          textboxText.minSelectionLine_72 = 0;
          textboxText.maxSelectionLine_70 = 3;
          textboxText.selectionIndex_6c = -1;
          textboxText.flags_08 |= TextboxText84.SELECTION;
          textboxText.state_00 = TextboxTextState.SELECTION_22;
        } else if (isAdvancedTutorialActive && (state == TutorialState.SELECTING_CHARACTER || state == TutorialState.SELECTING_CHARACTER_CHANGE)) {
          final List<Integer> party = getActivePartyMembers();
          textboxText.minSelectionLine_72 = 1;
          textboxText.maxSelectionLine_70 = party.size() + 1;
          textboxText.selectionIndex_6c = -1;
          textboxText.flags_08 |= TextboxText84.SELECTION;
          textboxText.state_00 = TextboxTextState.SELECTION_22;
        } else if (isAdvancedTutorialActive && (state == TutorialState.SELECTING_ADDITION || state == TutorialState.SELECTING_ADDITION_CHANGE)) {
          final List<AdditionEntry> additions = getAdditionsForCharacter(selectedCharacterId);
          textboxText.minSelectionLine_72 = 1;
          textboxText.maxSelectionLine_70 = additions.size();
          textboxText.selectionIndex_6c = -1;
          textboxText.flags_08 |= TextboxText84.SELECTION;
          textboxText.state_00 = TextboxTextState.SELECTION_22;
        }
      }
    } catch (final Throwable t) {
      LOGGER.error("TasmanBattleTutorial: Error in hookAddSelectionTextbox", t);
    }

    return result;
  }

  private static FlowControl hookGetSelectionIndex(final RunningScript script) {
    final int textboxIndex = script.params_20[0].get();
    final TextboxText84 textboxText = textboxText_800bdf38[textboxIndex];

    if (isTasmanBattle && TvvlrMultimod.isAdvancedAdditionsEnabled() && textboxText != null) {
      try {
        if (isTopicMenuOpen) {
          final int selected = textboxText.selectionIndex_6c;
          if (selected < 0) {
            return originalGetSelectionIndex != null ? originalGetSelectionIndex.apply(script) : FlowControl.CONTINUE;
          }

          isTopicMenuOpen = false;
          LOGGER.info("TasmanBattleTutorial: Topic menu selection confirmed: %d", selected);
          if (selected == 4) {
            // Selected "Advanced Additions." -> Route forward to character picker!
            isAdvancedTutorialActive = true;
            isCounterTutorialActive = false;
            state = TutorialState.SELECTING_CHARACTER;
            script.params_20[1].set(0);
            return FlowControl.CONTINUE;
          } else if (selected == 0) {
            // "Addition Skills." -> Double Slash for Dart
            isAdvancedTutorialActive = false;
            isCounterTutorialActive = false;
            state = TutorialState.IDLE;
            if (EngineStates.currentEngineState_8004dd04 instanceof final Battle battle) {
              applyAdditionToCharacter(battle, 0, LodAdditions.DOUBLE_SLASH.getId());
            }
            script.params_20[1].set(0);
            return FlowControl.CONTINUE;
          } else if (selected == 1) {
            // "About Counterattacking." -> Volcano for Dart
            isAdvancedTutorialActive = false;
            isCounterTutorialActive = true;
            state = TutorialState.IDLE;
            if (EngineStates.currentEngineState_8004dd04 instanceof final Battle battle) {
              applyAdditionToCharacter(battle, 0, LodAdditions.VOLCANO.getId());
            }
            script.params_20[1].set(1);
            return FlowControl.CONTINUE;
          } else if (selected == 5) {
            // "I am fine." -> Route script to exit tutorial (index 4 in vanilla)
            isAdvancedTutorialActive = false;
            isCounterTutorialActive = false;
            state = TutorialState.IDLE;
            restoreAllAdditions();
            script.params_20[1].set(4);
            return FlowControl.CONTINUE;
          } else {
            // Selected vanilla Defense (2) or Obtaining (3)
            isAdvancedTutorialActive = false;
            isCounterTutorialActive = false;
            state = TutorialState.IDLE;
            restoreAllAdditions();
            script.params_20[1].set(selected);
            return FlowControl.CONTINUE;
          }
        } else if (isRetryMenuOpen && isAdvancedTutorialActive) {
          final int selected = textboxText.selectionIndex_6c;
          if (selected < 0) {
            return originalGetSelectionIndex != null ? originalGetSelectionIndex.apply(script) : FlowControl.CONTINUE;
          }

          isRetryMenuOpen = false;
          LOGGER.info("TasmanBattleTutorial: Retry menu selection confirmed: %d", selected);
          switch (selected) {
            case 0 -> {
              // "Try again." -> Re-execute addition attack with current character & addition
              preAttackPromptShown = false;
              state = TutorialState.PRACTICE_ACTIVE;
              setForcedTurnCharacter(selectedCharacterId);
              script.params_20[1].set(0); // Vanilla option 0 = "Let me do it again."
              return FlowControl.CONTINUE;
            }
            case 1 -> {
              // "Change addition." -> Open addition picker for current character
              openAdditionPicker(textboxIndex, selectedCharacterId);
              state = TutorialState.SELECTING_ADDITION;
              textboxText.selectionIndex_6c = -1;
              return FlowControl.PAUSE_AND_REWIND;
            }
            case 2 -> {
              // "Change character." -> Open "Who would like to practice?" character picker
              openCharacterPicker(textboxIndex);
              state = TutorialState.SELECTING_CHARACTER;
              textboxText.selectionIndex_6c = -1;
              return FlowControl.PAUSE_AND_REWIND;
            }
            default -> {
              // "I'm fine now." (3) -> Exit practice and return to Tasman's topic menu
              isAdvancedTutorialActive = false;
              state = TutorialState.IDLE;
              restoreAllAdditions();
              script.params_20[1].set(1); // Vanilla option 1 = "I'm fine now." (returns to topic menu)
              return FlowControl.CONTINUE;
            }
          }
        } else if (isAdvancedTutorialActive && (state == TutorialState.SELECTING_ADDITION_CHANGE || state == TutorialState.SELECTING_ADDITION)) {
          int selected = textboxText.selectionIndex_6c;
          if (selected < 0) {
            return FlowControl.PAUSE_AND_REWIND;
          }
          final List<AdditionEntry> additions = getAdditionsForCharacter(selectedCharacterId);
          if (selected >= additions.size()) {
            selected = textboxText.selectionLine_68 - textboxText.minSelectionLine_72;
          }

          final int chosenIndex = Math.clamp(selected, 0, additions.size() - 1);
          final AdditionEntry chosenAddition = additions.get(chosenIndex);
          selectedAdditionId = chosenAddition.id();

          if (EngineStates.currentEngineState_8004dd04 instanceof final Battle battle) {
            applyAdditionToCharacter(battle, selectedCharacterId, selectedAdditionId);
          }
          setForcedTurnCharacter(selectedCharacterId);
          preAttackPromptShown = false;
          state = TutorialState.PRACTICE_ACTIVE;
          textboxText.selectionIndex_6c = -1;
          script.params_20[1].set(0); // Repeat attack round
          LOGGER.info("TasmanBattleTutorial: Selected addition %s for %s and starting attack.",
              chosenAddition.name(), getCharacterName(selectedCharacterId));
          return FlowControl.CONTINUE;
        } else if (isAdvancedTutorialActive && (state == TutorialState.SELECTING_CHARACTER_CHANGE || state == TutorialState.SELECTING_CHARACTER)) {
          int selected = textboxText.selectionIndex_6c;
          if (selected < 0) {
            return FlowControl.PAUSE_AND_REWIND;
          }
          final List<Integer> party = getActivePartyMembers();
          if (selected > party.size()) {
            selected = textboxText.selectionLine_68 - textboxText.minSelectionLine_72;
          }

          if (selected == party.size()) {
            // "Back." chosen on character selection -> Return to Tasman's topic menu!
            isAdvancedTutorialActive = false;
            state = TutorialState.IDLE;
            restoreAllAdditions();
            textboxText.selectionIndex_6c = -1;
            script.params_20[1].set(1); // Option 1 returns to topic menu
            LOGGER.info("TasmanBattleTutorial: Player pressed Back on character selection. Returning to topic menu.");
            return FlowControl.CONTINUE;
          }

          final int chosenSlot = Math.clamp(selected, 0, party.size() - 1);
          final int chosenCharId = party.get(chosenSlot);

          if (chosenCharId == 2 || chosenCharId == 8) {
            // Shana (2) / Miranda (8) has no additions
            showArcherWarning(textboxIndex);
            return FlowControl.PAUSE_AND_REWIND;
          }

          selectedCharacterId = chosenCharId;
          openAdditionPicker(textboxIndex, selectedCharacterId);
          state = TutorialState.SELECTING_ADDITION;
          textboxText.selectionIndex_6c = -1;
          LOGGER.info("TasmanBattleTutorial: Character chosen: %s (%d). Opening addition picker.",
              getCharacterName(chosenCharId), chosenCharId);
          return FlowControl.PAUSE_AND_REWIND;
        } else if (isAdvancedTutorialActive && state == TutorialState.SHOWING_ARCHER_WARNING) {
          if (textboxText.state_00 == TextboxTextState.UNINITIALIZED_0 || textboxText.selectionIndex_6c >= 0) {
            openCharacterPicker(textboxIndex);
            state = TutorialState.SELECTING_CHARACTER;
          }
          return FlowControl.PAUSE_AND_REWIND;
        }
      } catch (final Throwable t) {
        LOGGER.error("TasmanBattleTutorial: Error in hookGetSelectionIndex", t);
      }
    }

    return originalGetSelectionIndex != null ? originalGetSelectionIndex.apply(script) : FlowControl.CONTINUE;
  }

  private static FlowControl hookIsTextboxInitialized(final RunningScript script) {
    if (isTasmanBattle && isAdvancedTutorialActive) {
      final TextboxText84 textboxText = textboxText_800bdf38[0];
      if (textboxText != null) {
        // Initial character picker confirmation
        if ((state == TutorialState.SELECTING_CHARACTER || state == TutorialState.SELECTING_CHARACTER_CHANGE)
            && textboxText.state_00 == TextboxTextState.UNINITIALIZED_0 && textboxText.selectionIndex_6c >= 0) {
          final List<Integer> party = getActivePartyMembers();
          int selected = textboxText.selectionIndex_6c;
          if (selected > party.size()) {
            selected = textboxText.selectionLine_68 - textboxText.minSelectionLine_72;
          }

          if (selected == party.size()) {
            // "Back." chosen -> Return to topic menu
            isAdvancedTutorialActive = false;
            state = TutorialState.IDLE;
            restoreAllAdditions();
            textboxText.selectionIndex_6c = -1;
            script.params_20[1].set(1);
            return FlowControl.CONTINUE;
          }

          final int chosenSlot = Math.clamp(selected, 0, party.size() - 1);
          final int chosenCharId = party.get(chosenSlot);

          if (chosenCharId == 2 || chosenCharId == 8) {
            showArcherWarning(0);
          } else {
            selectedCharacterId = chosenCharId;
            openAdditionPicker(0, selectedCharacterId);
            state = TutorialState.SELECTING_ADDITION;
            textboxText.selectionIndex_6c = -1;
            LOGGER.info("TasmanBattleTutorial: Character chosen: %s (%d). Moving to addition selection.",
                getCharacterName(chosenCharId), chosenCharId);
          }
        }
        // Addition picker confirmation
        else if ((state == TutorialState.SELECTING_ADDITION || state == TutorialState.SELECTING_ADDITION_CHANGE)
            && textboxText.state_00 == TextboxTextState.UNINITIALIZED_0 && textboxText.selectionIndex_6c >= 0) {
          final List<AdditionEntry> additions = getAdditionsForCharacter(selectedCharacterId);
          int selected = textboxText.selectionIndex_6c;
          if (selected >= additions.size()) {
            selected = textboxText.selectionLine_68 - textboxText.minSelectionLine_72;
          }

          final int chosenIndex = Math.clamp(selected, 0, additions.size() - 1);
          final AdditionEntry chosenAddition = additions.get(chosenIndex);
          selectedAdditionId = chosenAddition.id();

          if (EngineStates.currentEngineState_8004dd04 instanceof final Battle battle) {
            applyAdditionToCharacter(battle, selectedCharacterId, selectedAdditionId);
          }
          setForcedTurnCharacter(selectedCharacterId);
          preAttackPromptShown = false;
          textboxText.selectionIndex_6c = -1;
          state = TutorialState.PRACTICE_ACTIVE;
          LOGGER.info("TasmanBattleTutorial: Addition chosen: %s for %s. Practice attack active.",
              chosenAddition.name(), getCharacterName(selectedCharacterId));
        }
        // Archer warning dismissed
        else if (state == TutorialState.SHOWING_ARCHER_WARNING && textboxText.state_00 == TextboxTextState.UNINITIALIZED_0) {
          openCharacterPicker(0);
        }
      }
    }

    return originalIsTextboxInitialized != null ? originalIsTextboxInitialized.apply(script) : FlowControl.CONTINUE;
  }

  private static void openCharacterPicker(final int textboxIndex) {
    final Textbox4c textbox = textboxes_800be358[textboxIndex];
    final TextboxText84 textboxText = textboxText_800bdf38[textboxIndex];
    if (textbox == null || textboxText == null) {
      return;
    }

    final List<Integer> party = getActivePartyMembers();
    final StringBuilder sb = new StringBuilder();
    sb.append("Who would like to practice?\n");
    for (int i = 0; i < party.size(); i++) {
      final int cid = party.get(i);
      final String name = getCharacterName(cid);
      if (cid == 2 || cid == 8) {
        sb.append("\"").append(name).append(" (No Additions).\"\n");
      } else {
        sb.append("\"").append(name).append(".\"\n");
      }
    }
    sb.append("\"Back.\"");

    textboxText.str_24 = toLodString(sb.toString());
    final int lineCount = party.size() + 2;
    setupTextboxLines(textbox, textboxText, lineCount, 32);
    textboxText.charIndex_30 = 0;
    textboxText.charX_34 = 0;
    textboxText.charY_36 = 0;
    textboxText.state_00 = TextboxTextState.PROCESS_TEXT_4;

    populateAndMakeSelection(textboxIndex, 1, party.size() + 1, 1);
    state = TutorialState.SELECTING_CHARACTER;
    LOGGER.info("TasmanBattleTutorial: Displayed character picker with %d party members + Back.", party.size());
  }

  private static void openAdditionPicker(final int textboxIndex, final int charId) {
    final Textbox4c textbox = textboxes_800be358[textboxIndex];
    final TextboxText84 textboxText = textboxText_800bdf38[textboxIndex];
    if (textbox == null || textboxText == null) {
      return;
    }

    final List<AdditionEntry> additions = getAdditionsForCharacter(charId);
    final StringBuilder sb = new StringBuilder();
    sb.append("Which addition to practice?\n");
    for (int i = 0; i < additions.size(); i++) {
      if (i == additions.size() - 1) {
        sb.append("\"").append(additions.get(i).name()).append(".\"");
      } else {
        sb.append("\"").append(additions.get(i).name()).append(".\"\n");
      }
    }

    textboxText.str_24 = toLodString(sb.toString());
    final int lineCount = additions.size() + 1;
    setupTextboxLines(textbox, textboxText, lineCount, 30);
    textboxText.charIndex_30 = 0;
    textboxText.charX_34 = 0;
    textboxText.charY_36 = 0;
    textboxText.state_00 = TextboxTextState.PROCESS_TEXT_4;

    int defaultLine = 1;
    try {
      if (gameState_800babc8 != null && charId < gameState_800babc8.charData_32c.size()) {
        final CharacterData2c cd = gameState_800babc8.charData_32c.get(charId);
        if (cd != null && cd.selectedAddition_19 != null) {
          for (int i = 0; i < additions.size(); i++) {
            if (additions.get(i).id().equals(cd.selectedAddition_19)) {
              defaultLine = i + 1;
              break;
            }
          }
        }
      }
    } catch (final Throwable ignored) {}

    populateAndMakeSelection(textboxIndex, 1, additions.size(), defaultLine);
    state = TutorialState.SELECTING_ADDITION;
    LOGGER.info("TasmanBattleTutorial: Displayed addition picker for %s (%d additions).",
        getCharacterName(charId), additions.size());
  }

  private static void openRetryMenu(final int textboxIndex) {
    final Textbox4c textbox = textboxes_800be358[textboxIndex];
    final TextboxText84 textboxText = textboxText_800bdf38[textboxIndex];
    if (textbox == null || textboxText == null) {
      return;
    }

    final String retryMenu =
      "\"Try again.\"\n" +
      "\"Change addition.\"\n" +
      "\"Change character.\"\n" +
      "\"I'm fine now.\"";
    textboxText.str_24 = toLodString(retryMenu);
    setupTextboxLines(textbox, textboxText, 4, 24);
    textboxText.charIndex_30 = 0;
    textboxText.charX_34 = 0;
    textboxText.charY_36 = 0;
    textboxText.state_00 = TextboxTextState.PROCESS_TEXT_4;

    populateAndMakeSelection(textboxIndex, 0, 3, 0);
    isRetryMenuOpen = true;
    state = TutorialState.RETRY_MENU;
    LOGGER.info("TasmanBattleTutorial: Opened Retry Menu (Try again / Change addition / Change character / I'm fine now).");
  }

  private static void showArcherWarning(final int textboxIndex) {
    final Textbox4c textbox = textboxes_800be358[textboxIndex];
    final TextboxText84 textboxText = textboxText_800bdf38[textboxIndex];
    if (textbox == null || textboxText == null) {
      return;
    }

    final String warning =
      "Archers do not use additions!\n" +
      "Please choose another companion.";
    textboxText.str_24 = toLodString(warning);
    setupTextboxLines(textbox, textboxText, 2, 34);
    textboxText.charIndex_30 = 0;
    textboxText.charX_34 = 0;
    textboxText.charY_36 = 0;
    textboxText.state_00 = TextboxTextState.PROCESS_TEXT_4;
    textboxText.selectionIndex_6c = -1;
    state = TutorialState.SHOWING_ARCHER_WARNING;
    LOGGER.info("TasmanBattleTutorial: Displayed archer warning.");
  }

  public static List<Integer> getActivePartyMembers() {
    final List<Integer> party = new ArrayList<>();
    if (gameState_800babc8 != null && gameState_800babc8.charIds_88 != null) {
      for (int i = 0; i < gameState_800babc8.charIds_88.size(); i++) {
        final int charId = gameState_800babc8.charIds_88.getInt(i);
        if (charId >= 0 && charId < 9 && !party.contains(charId)) {
          party.add(charId);
        }
      }
    }
    if (party.isEmpty()) {
      party.add(0);
    }
    return party;
  }

  public static String getCharacterName(final int charId) {
    if (gameState_800babc8 != null && charId >= 0 && charId < gameState_800babc8.charData_32c.size()) {
      final CharacterData2c data = gameState_800babc8.charData_32c.get(charId);
      if (data != null && data.getName() != null) {
        final String n = data.getName().get();
        if (n != null && !n.trim().isEmpty()) {
          return n.trim();
        }
      }
    }
    return switch (charId) {
      case 0 -> "Dart";
      case 1 -> "Lavitz";
      case 2 -> "Shana";
      case 3 -> "Rose";
      case 4 -> "Haschel";
      case 5 -> "Albert";
      case 6 -> "Meru";
      case 7 -> "Kongol";
      case 8 -> "Miranda";
      default -> "Companion";
    };
  }

  public static List<AdditionEntry> getAdditionsForCharacter(final int charId) {
    // If charId is 1 but name is Albert, use Albert's additions
    if (charId == 1) {
      if (gameState_800babc8 != null && gameState_800babc8.charData_32c.size() > 1) {
        final CharacterData2c c1 = gameState_800babc8.charData_32c.get(1);
        if (c1 != null && c1.getName() != null && "Albert".equalsIgnoreCase(c1.getName().get())) {
          return CHARACTER_ADDITIONS.getOrDefault(5, List.of());
        }
      }
    }
    return CHARACTER_ADDITIONS.getOrDefault(charId, List.of());
  }

  private static void backupPartyAdditions() {
    originalAdditions.clear();
    if (gameState_800babc8 != null && gameState_800babc8.charData_32c != null) {
      for (int i = 0; i < gameState_800babc8.charData_32c.size(); i++) {
        final CharacterData2c c = gameState_800babc8.charData_32c.get(i);
        if (c != null && c.selectedAddition_19 != null) {
          originalAdditions.put(i, c.selectedAddition_19);
        }
      }
    }
  }

  public static void restoreAllAdditions() {
    if (gameState_800babc8 != null) {
      // 1. Remove any temporary addition infos that were added for practice
      for (final Map.Entry<Integer, List<RegistryId>> tempEntry : temporarilyAddedAdditions.entrySet()) {
        final int cid = tempEntry.getKey();
        if (cid < gameState_800babc8.charData_32c.size()) {
          final CharacterData2c c = gameState_800babc8.charData_32c.get(cid);
          if (c != null) {
            for (final RegistryId tempId : tempEntry.getValue()) {
              c.removeAddition(tempId);
            }
          }
        }
      }
      temporarilyAddedAdditions.clear();

      // 2. Restore original selected additions
      if (!originalAdditions.isEmpty()) {
        for (final Map.Entry<Integer, RegistryId> entry : originalAdditions.entrySet()) {
          final int charId = entry.getKey();
          final RegistryId origId = entry.getValue();
          if (charId < gameState_800babc8.charData_32c.size()) {
            final CharacterData2c c = gameState_800babc8.charData_32c.get(charId);
            if (c != null) {
              c.selectedAddition_19 = origId;
            }
          }
          if (EngineStates.currentEngineState_8004dd04 instanceof final Battle battle) {
            applyAdditionToCharacter(battle, charId, origId);
          }
        }
        LOGGER.info("TasmanBattleTutorial: Restored original additions for all party members.");
      }
      preAttackPromptShown = false;
    }
  }

  public static void applyAdditionToCharacter(final Battle battle, final int charId, final RegistryId additionId) {
    try {
      CharacterData2c charData = null;
      if (gameState_800babc8 != null && charId < gameState_800babc8.charData_32c.size()) {
        charData = gameState_800babc8.charData_32c.get(charId);
        if (charData != null) {
          charData.selectedAddition_19 = additionId;
          // CRITICAL FIX: Ensure additionInfo exists so SimpleAddition.getSpMultiplier does not throw NPE
          if (charData.getAdditionInfo(additionId) == null) {
            final CharacterAdditionInfo info = new CharacterAdditionInfo(List.of());
            info.unlock(0);
            info.level = 1;
            charData.addAddition(additionId, info);
            temporarilyAddedAdditions.computeIfAbsent(charId, k -> new ArrayList<>()).add(additionId);
            LOGGER.info("TasmanBattleTutorial: Created temporary CharacterAdditionInfo for char %d addition %s", charId, additionId);
          }
        }
      }

      final var entry = REGISTRIES.additions.getEntry(additionId);
      for (int i = 0; i < 10; i++) {
        final CombatantStruct1a8 combatant = battle.getCombatant(i);
        if (combatant != null && combatant.isPlayer() && combatant.playerBent != null) {
          if (combatant.charIndex_1a2 == charId || (charData != null && combatant.playerBent.character == charData)) {
            if (combatant.playerBent.character != null) {
              combatant.playerBent.character.selectedAddition_19 = additionId;
            }
            if (entry != null) {
              combatant.playerBent.addition = entry.get();
            }
            combatant.mrg_04 = null;
            battle.loadAttackAnimations(combatant);
            LOGGER.info("TasmanBattleTutorial: Applied addition %s to combatant %d (%s)",
                additionId, charId, getCharacterName(charId));
            break;
          }
        }
      }
    } catch (final Throwable t) {
      LOGGER.error("TasmanBattleTutorial: Error applying addition to character " + charId, t);
    }
  }

  public static void setForcedTurnCharacter(final int charId) {
    try {
      if (battleState_8006e398 != null) {
        if (battleState_8006e398.aliveBents_e78 != null) {
          for (int i = 0; i < battleState_8006e398.aliveBents_e78.size(); i++) {
            final ScriptState<? extends BattleEntity27c> bentState = battleState_8006e398.aliveBents_e78.get(i);
            if (bentState != null && bentState.innerStruct_00 instanceof PlayerBattleEntity player) {
              if (player.charId_272 == charId) {
                bentState.setFlag(BattleEntity27c.FLAG_TAKE_FORCED_TURN);
              } else {
                bentState.clearFlag(BattleEntity27c.FLAG_TAKE_FORCED_TURN);
              }
            }
          }
        }
        if (battleState_8006e398.playerBents_e40 != null) {
          for (int i = 0; i < battleState_8006e398.playerBents_e40.size(); i++) {
            final ScriptState<PlayerBattleEntity> playerBentState = battleState_8006e398.playerBents_e40.get(i);
            if (playerBentState != null && playerBentState.innerStruct_00 != null) {
              if (playerBentState.innerStruct_00.charId_272 == charId) {
                playerBentState.setFlag(BattleEntity27c.FLAG_TAKE_FORCED_TURN);
              } else {
                playerBentState.clearFlag(BattleEntity27c.FLAG_TAKE_FORCED_TURN);
              }
            }
          }
        }
      }
    } catch (final Throwable t) {
      LOGGER.error("TasmanBattleTutorial: Error setting forced turn for char " + charId, t);
    }
  }

  private static void setupTextboxLines(final Textbox4c textbox, final TextboxText84 textboxText, final int lineCount, final int minChars) {
    final int safeChars = Math.max(textbox.chars_18, minChars + 4);
    textbox.chars_18 = safeChars;
    textbox.lines_1a = lineCount + 1;
    textbox.width_1c = textbox.chars_18 * 9 / 2;
    textbox.height_1e = textbox.lines_1a * 6;
    textboxText.chars_1c = textbox.chars_18 - 1;
    textboxText.lines_1e = lineCount;
    textboxText.chars_58 = new TextboxChar08[textboxText.chars_1c * (textboxText.lines_1e + 1)];
    Arrays.setAll(textboxText.chars_58, i -> new TextboxChar08());
    textboxText._18 = textboxText.x_14 - textboxText.chars_1c * 9 / 2;
    textboxText._1a = textboxText.y_16 - textboxText.lines_1e * 6;
  }

  private static void populateAndMakeSelection(final int textboxIndex, final int minLine, final int maxLine) {
    populateAndMakeSelection(textboxIndex, minLine, maxLine, minLine);
  }

  private static void populateAndMakeSelection(final int textboxIndex, final int minLine, final int maxLine, final int defaultLine) {
    final TextboxText84 textboxText = textboxText_800bdf38[textboxIndex];
    if (textboxText == null) {
      return;
    }
    while (textboxText.state_00 != TextboxTextState.CLOSE_TEXTBOX_15
        && textboxText.state_00 != TextboxTextState.UNINITIALIZED_0
        && textboxText.charIndex_30 < textboxText.str_24.length()) {
      legend.game.Text.processTextboxCharacter(textboxIndex);
    }
    textboxText.state_00 = TextboxTextState.SELECTION_22;
    textboxText.flags_08 |= TextboxText84.SELECTION;
    textboxText.minSelectionLine_72 = minLine;
    textboxText.maxSelectionLine_70 = maxLine;
    textboxText.selectionLine_68 = defaultLine;
    textboxText.selectionIndex_6c = -1;
  }

  public static LodString toLodString(final String text) {
    final List<Integer> codes = new ArrayList<>();
    for (int i = 0; i < text.length(); i++) {
      final char ch = text.charAt(i);
      if (ch == '□') {
        codes.add(0x56);
      } else if (ch == '\f') {
        codes.add(0xa3ff);
      } else if (ch == '\n') {
        codes.add(0xa1ff);
      } else {
        codes.add(LodString.toLodChar(ch));
      }
    }
    codes.add(0xa0ff);
    final int[] arr = new int[codes.size()];
    for (int i = 0; i < codes.size(); i++) {
      arr[i] = codes.get(i);
    }
    return new LodString(arr);
  }
}
