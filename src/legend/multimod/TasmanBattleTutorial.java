package legend.multimod;

import legend.game.EngineStates;
import legend.game.characters.CharacterData2c;
import legend.game.combat.Battle;
import legend.game.combat.types.CombatantStruct1a8;
import legend.game.modding.events.RenderEvent;
import legend.game.modding.events.battle.BattleEndedEvent;
import legend.game.modding.events.battle.BattleStartedEvent;
import legend.game.scripting.FlowControl;
import legend.game.scripting.RunningScript;
import legend.game.types.LodString;
import legend.game.types.Textbox4c;
import legend.game.types.TextboxChar08;
import legend.game.types.TextboxText84;
import legend.game.types.TextboxTextState;
import legend.lodmod.LodAdditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.legendofdragoon.modloader.registries.RegistryId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static legend.core.GameEngine.REGISTRIES;
import static legend.game.Scus94491BpeSegment_8004.scriptSubFunctions_8004e29c;
import static legend.game.Scus94491BpeSegment_800b.gameState_800babc8;
import static legend.game.Text.textboxText_800bdf38;
import static legend.game.Text.textboxes_800be358;

public class TasmanBattleTutorial {
  private static final Logger LOGGER = LogManager.getFormatterLogger(TasmanBattleTutorial.class);

  public enum TutorialState {
    IDLE,
    EXPLAIN_1,
    EXPLAIN_2,
    PROMPT_ADDITION,
    SELECTING_ADDITION,
    PRACTICE_ACTIVE,
    SELECTING_ADDITION_CHANGE
  }

  public static final List<RegistryId> ELIGIBLE_ADDITIONS = List.of(
    LodAdditions.VOLCANO.getId(),
    LodAdditions.BURNING_RUSH.getId(),
    LodAdditions.CRUSH_DANCE.getId(),
    LodAdditions.MADNESS_HERO.getId(),
    LodAdditions.MOON_STRIKE.getId(),
    LodAdditions.BLAZING_DYNAMO.getId()
  );

  private static boolean initialized = false;
  private static boolean isTasmanBattle = false;
  private static boolean isTopicMenuOpen = false;
  private static boolean isRetryMenuOpen = false;
  private static boolean isAdvancedTutorialActive = false;
  private static boolean isCounterTutorialActive = false;
  private static TutorialState state = TutorialState.IDLE;
  private static RegistryId originalPlayerAdditionId = null;

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
    if (scriptSubFunctions_8004e29c != null) {
      originalAddTextbox = scriptSubFunctions_8004e29c[200];
      scriptSubFunctions_8004e29c[200] = TasmanBattleTutorial::hookAddTextbox;

      originalGetSelectionIndex = scriptSubFunctions_8004e29c[205];
      scriptSubFunctions_8004e29c[205] = TasmanBattleTutorial::hookGetSelectionIndex;

      originalAddSelectionTextbox = scriptSubFunctions_8004e29c[207];
      scriptSubFunctions_8004e29c[207] = TasmanBattleTutorial::hookAddSelectionTextbox;

      originalIsTextboxInitialized = scriptSubFunctions_8004e29c[195];
      scriptSubFunctions_8004e29c[195] = TasmanBattleTutorial::hookIsTextboxInitialized;

      initialized = true;
      LOGGER.info("TasmanBattleTutorial: Successfully initialized script opcode hooks.");
    }
  }

  public static void onBattleStarted(final BattleStartedEvent event) {
    if (event.encounter != null && event.encounter.uniqueIds.contains(254)) {
      isTasmanBattle = true;
      isTopicMenuOpen = false;
      isRetryMenuOpen = false;
      isAdvancedTutorialActive = false;
      state = TutorialState.IDLE;

      final CharacterData2c dart = gameState_800babc8.charData_32c.get(0);
      if (dart != null) {
        originalPlayerAdditionId = dart.selectedAddition_19;
        LOGGER.info("TasmanBattleTutorial: Tasman practice battle started. Original player Dart addition: %s", originalPlayerAdditionId);
      }
    } else {
      isTasmanBattle = false;
      isTopicMenuOpen = false;
      isRetryMenuOpen = false;
      isAdvancedTutorialActive = false;
      isCounterTutorialActive = false;
      originalPlayerAdditionId = null;
    }
  }

  public static void onBattleEnded(final BattleEndedEvent event) {
    restoreDartAddition();
    isTasmanBattle = false;
    isTopicMenuOpen = false;
    isRetryMenuOpen = false;
    isAdvancedTutorialActive = false;
    isCounterTutorialActive = false;
    state = TutorialState.IDLE;
    originalPlayerAdditionId = null;
  }

  public static void onRender(final RenderEvent event) {
    if (!isTasmanBattle || !TvvlrMultimod.isAdvancedAdditionsEnabled()) {
      return;
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
          textboxText.maxSelectionLine_70 = 2;
          if (textboxText.selectionLine_68 < 0 || textboxText.selectionLine_68 > 2) {
            textboxText.selectionLine_68 = 0;
          }
        }
      }
    } else if (isAdvancedTutorialActive && (state == TutorialState.SELECTING_ADDITION || state == TutorialState.SELECTING_ADDITION_CHANGE)) {
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
      } else if (state == TutorialState.SELECTING_ADDITION && textboxText.state_00 == TextboxTextState.UNINITIALIZED_0 && textboxText.selectionIndex_6c >= 0) {
        final int chosenIndex = Math.clamp(textboxText.selectionIndex_6c, 0, ELIGIBLE_ADDITIONS.size() - 1);
        final RegistryId chosenAdditionId = ELIGIBLE_ADDITIONS.get(chosenIndex);
        if (EngineStates.currentEngineState_8004dd04 instanceof final Battle battle) {
          setDartAddition(battle, chosenAdditionId);
        }
        textboxText.selectionIndex_6c = -1;
        state = TutorialState.PRACTICE_ACTIVE;
        LOGGER.info("TasmanBattleTutorial: onRender - Addition selected and applied: %s (%d)", chosenAdditionId, chosenIndex);
      }
    }
  }

  private static FlowControl hookAddTextbox(final RunningScript script) {
    final FlowControl result = originalAddTextbox.apply(script);
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
          final CharacterData2c dart = gameState_800babc8.charData_32c.get(0);
          if (dart != null && originalPlayerAdditionId == null) {
            originalPlayerAdditionId = dart.selectedAddition_19;
            LOGGER.info("TasmanBattleTutorial: Captured originalPlayerAdditionId at menu: %s", originalPlayerAdditionId);
          }
          restoreDartAddition();
          isCounterTutorialActive = false;
          final String newMenu =
            "\"Addition Skills.\"\n" +
            "\"About Counterattacking.\"\n" +
            "\"About Defense.\"\n" +
            "\"Obtaining Additions.\"\n" +
            "\"Advanced Additions.\"\n" +
            "\"I am fine.\"";
          textboxText.str_24 = toLodString(newMenu);
          setupTextboxLines(textbox, textboxText, 6, 26);
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
        // 2. Explanation Part 1 (Only when Advanced Additions is active!)
        else if (isAdvancedTutorialActive && state == TutorialState.EXPLAIN_1 && text.contains("Select attack command.")) {
          final String explain1 =
            "In advanced combat, additions\n" +
            "combos use multiple buttons!\n" +
            "Watch the color of the sight\n" +
            "to press the correct button!";
          textboxText.str_24 = toLodString(explain1);
          setupTextboxLines(textbox, textboxText, 4, 30);
          state = TutorialState.EXPLAIN_2;
          LOGGER.info("TasmanBattleTutorial: Displaying Advanced Additions Explanation Part 1.");
        }
        // 3. Explanation Part 2 (Controller button color coding + 4-hit demonstration)
        else if (isAdvancedTutorialActive && state == TutorialState.EXPLAIN_2 && (text.contains("When this sight overlaps") || text.contains("overlaps"))) {
          final String explain2 = getExplanationColors();
          textboxText.str_24 = toLodString(explain2);
          setupTextboxLines(textbox, textboxText, 4, 30);
          state = TutorialState.PROMPT_ADDITION;
          LOGGER.info("TasmanBattleTutorial: Displaying Controller Colors Explanation Part 2.");
        }
        // Attack Tutorial (Addition Skills): replace '+' with Square icon '□' and say 'Corresponding button'
        else if (!isAdvancedTutorialActive && text.contains("When this sight overlaps")) {
          final String attackExplain =
            "When this sight overlaps\n" +
            "the □ in the center, press\n" +
            "the Corresponding button.\f" +
            "Gray □ means too fast\n" +
            "Blue □ means too slow\n" +
            "White □ means perfect.";
          textboxText.str_24 = toLodString(attackExplain);
          setupTextboxLines(textbox, textboxText, 3, 27);
          LOGGER.info("TasmanBattleTutorial: Displaying Attack Tutorial with Square icon and 'Corresponding button'.");
        }
        // Counter Tutorial (About Counterattacking): replace '+' with Square icon '□'
        else if (!isAdvancedTutorialActive && (text.contains("counterattack sight appears") || (text.contains("counterattack") && text.contains("overlaying")))) {
          final String counterExplain =
            "Press the @ button when the red\n" +
            "counterattack sight appears \n" +
            "overlaying the □.";
          textboxText.str_24 = toLodString(counterExplain);
          setupTextboxLines(textbox, textboxText, 3, 32);
          LOGGER.info("TasmanBattleTutorial: Displaying Counter Tutorial with Square icon.");
        }
        // 4. Prompt initial addition selection (replaces "Now, come on!")
        else if (isAdvancedTutorialActive && state == TutorialState.PROMPT_ADDITION && text.contains("Now, come on!")) {
          openAdditionPicker(textboxIndex);
          state = TutorialState.SELECTING_ADDITION;
          LOGGER.info("TasmanBattleTutorial: Opened Addition Practice Selection Menu.");
        }
        // 5. Tasman Retry Prompt after an attack: "How are you doing? One more time?"
        else if (isAdvancedTutorialActive && (text.contains("How are you doing?") || text.contains("One more time?"))) {
          final String retryPrompt =
            "How are you doing?\n" +
            "One more time?";
          textboxText.str_24 = toLodString(retryPrompt);
          setupTextboxLines(textbox, textboxText, 2, 24);
          LOGGER.info("TasmanBattleTutorial: Displaying retry prompt text.");
        }
        // 6. Tasman Retry Menu options: replaces "Let me do it again." / "I'm fine now." with 3 choices:
        // "Try again.", "Change Addition.", "I'm fine now."
        else if (isAdvancedTutorialActive && text.contains("Let me do it again.") && text.contains("I'm fine now.")) {
          final String retryMenu =
            "\"Try again.\"\n" +
            "\"Change Addition.\"\n" +
            "\"I'm fine now.\"";
          textboxText.str_24 = toLodString(retryMenu);
          setupTextboxLines(textbox, textboxText, 3, 20);
          textboxText.charIndex_30 = 0;
          textboxText.charX_34 = 0;
          textboxText.charY_36 = 0;
          textboxText.state_00 = TextboxTextState.PROCESS_TEXT_4;

          populateAndMakeSelection(textboxIndex, 0, 2, 0);
          isRetryMenuOpen = true;
          LOGGER.info("TasmanBattleTutorial: Opened Retry Menu (Try again / Change Addition / I'm fine now).");
        }
      }
    } catch (final Throwable t) {
      LOGGER.error("TasmanBattleTutorial: Error in hookAddTextbox", t);
    }

    return result;
  }

  private static FlowControl hookAddSelectionTextbox(final RunningScript script) {
    final FlowControl result = originalAddSelectionTextbox.apply(script);
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
          LOGGER.info("TasmanBattleTutorial: Expanded topic menu max selection line to 5 and set SELECTION_22.");
        } else if (isRetryMenuOpen) {
          textboxText.minSelectionLine_72 = 0;
          textboxText.maxSelectionLine_70 = 2;
          textboxText.selectionIndex_6c = -1;
          textboxText.flags_08 |= TextboxText84.SELECTION;
          textboxText.state_00 = TextboxTextState.SELECTION_22;
          LOGGER.info("TasmanBattleTutorial: Set retry menu SELECTION_22 and max 2.");
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
          // While user is still deciding (-1), do not consume the menu!
          if (selected < 0) {
            return originalGetSelectionIndex.apply(script);
          }

          isTopicMenuOpen = false;
          LOGGER.info("TasmanBattleTutorial: Topic menu selection confirmed: %d", selected);
          if (selected == 4) {
            // Selected "Advanced Additions." -> Always play full tutorial!
            isAdvancedTutorialActive = true;
            isCounterTutorialActive = false;
            state = TutorialState.EXPLAIN_1;
            script.params_20[1].set(0);
            return FlowControl.CONTINUE;
          } else if (selected == 0) {
            // Selected "Addition Skills." -> Always Double Slash for Dart!
            isAdvancedTutorialActive = false;
            isCounterTutorialActive = false;
            state = TutorialState.IDLE;
            if (EngineStates.currentEngineState_8004dd04 instanceof final Battle battle) {
              setDartAddition(battle, LodAdditions.DOUBLE_SLASH.getId());
              LOGGER.info("TasmanBattleTutorial: Attack tutorial selected - set Dart addition to Double Slash.");
            }
            script.params_20[1].set(0);
            return FlowControl.CONTINUE;
          } else if (selected == 1) {
            // Selected "About Counterattacking." -> Set Dart's addition to Volcano!
            isAdvancedTutorialActive = false;
            isCounterTutorialActive = true;
            state = TutorialState.IDLE;
            if (EngineStates.currentEngineState_8004dd04 instanceof final Battle battle) {
              setDartAddition(battle, LodAdditions.VOLCANO.getId());
              LOGGER.info("TasmanBattleTutorial: Counter tutorial selected - set Dart addition to Volcano.");
            }
            script.params_20[1].set(1);
            return FlowControl.CONTINUE;
          } else if (selected == 5) {
            // Selected "I am fine." -> Route script to exit tutorial (index 4 in vanilla)
            isAdvancedTutorialActive = false;
            isCounterTutorialActive = false;
            state = TutorialState.IDLE;
            restoreDartAddition();
            script.params_20[1].set(4);
            return FlowControl.CONTINUE;
          } else {
            // Selected vanilla Defense (2) or Obtaining (3)
            isAdvancedTutorialActive = false;
            isCounterTutorialActive = false;
            state = TutorialState.IDLE;
            restoreDartAddition();
            script.params_20[1].set(selected);
            return FlowControl.CONTINUE;
          }
        } else if (isRetryMenuOpen && isAdvancedTutorialActive) {
          final int selected = textboxText.selectionIndex_6c;
          if (selected < 0) {
            return originalGetSelectionIndex.apply(script);
          }

          isRetryMenuOpen = false;
          LOGGER.info("TasmanBattleTutorial: Retry menu selection confirmed: %d", selected);
          if (selected == 0) {
            // "Try again." -> Repeat practice with current addition
            state = TutorialState.PRACTICE_ACTIVE;
            script.params_20[1].set(0); // Route script to option 0: "Let me do it again."
            return FlowControl.CONTINUE;
          } else if (selected == 1) {
            // "Change Addition." -> Open addition picker and pause script until confirmed
            openAdditionPicker(textboxIndex);
            state = TutorialState.SELECTING_ADDITION_CHANGE;
            return FlowControl.PAUSE_AND_REWIND;
          } else {
            // "I'm fine now." (selected == 2) -> Exit to Tasman topic menu & reset addition to unlocked original!
            isAdvancedTutorialActive = false;
            state = TutorialState.IDLE;
            restoreDartAddition();
            script.params_20[1].set(1); // Route script to option 1: "I'm fine now."
            return FlowControl.CONTINUE;
          }
        } else if (isAdvancedTutorialActive && state == TutorialState.SELECTING_ADDITION_CHANGE) {
          final int selected = textboxText.selectionIndex_6c;
          if (selected < 0) {
            return FlowControl.PAUSE_AND_REWIND;
          }

          final int chosenIndex = Math.clamp(selected, 0, ELIGIBLE_ADDITIONS.size() - 1);
          final RegistryId chosenAdditionId = ELIGIBLE_ADDITIONS.get(chosenIndex);
          if (EngineStates.currentEngineState_8004dd04 instanceof final Battle battle) {
            setDartAddition(battle, chosenAdditionId);
          }
          state = TutorialState.PRACTICE_ACTIVE;
          script.params_20[1].set(0); // Route script to option 0: "Let me do it again."
          LOGGER.info("TasmanBattleTutorial: Changed addition to %s (%d) and restarting practice round.", chosenAdditionId, chosenIndex);
          return FlowControl.CONTINUE;
        }
      } catch (final Throwable t) {
        LOGGER.error("TasmanBattleTutorial: Error in hookGetSelectionIndex", t);
      }
    }

    return originalGetSelectionIndex.apply(script);
  }

  private static FlowControl hookIsTextboxInitialized(final RunningScript script) {
    if (isTasmanBattle && isAdvancedTutorialActive && state == TutorialState.SELECTING_ADDITION) {
      final TextboxText84 textboxText = textboxText_800bdf38[0];
      if (textboxText != null && textboxText.state_00 == TextboxTextState.UNINITIALIZED_0 && textboxText.selectionIndex_6c >= 0) {
        final int chosenIndex = Math.clamp(textboxText.selectionIndex_6c, 0, ELIGIBLE_ADDITIONS.size() - 1);
        final RegistryId chosenAdditionId = ELIGIBLE_ADDITIONS.get(chosenIndex);
        if (EngineStates.currentEngineState_8004dd04 instanceof final Battle battle) {
          setDartAddition(battle, chosenAdditionId);
        }
        textboxText.selectionIndex_6c = -1;
        state = TutorialState.PRACTICE_ACTIVE;
        LOGGER.info("TasmanBattleTutorial: hookIsTextboxInitialized - Addition applied: %s (%d)", chosenAdditionId, chosenIndex);
      }
    }

    return originalIsTextboxInitialized.apply(script);
  }

  private static void openAdditionPicker(final int textboxIndex) {
    final Textbox4c textbox = textboxes_800be358[textboxIndex];
    final TextboxText84 textboxText = textboxText_800bdf38[textboxIndex];
    if (textbox == null || textboxText == null) {
      return;
    }

    final String additionMenu =
      "\"Volcano.\"\n" +
      "\"Burning Rush.\"\n" +
      "\"Crush Dance.\"\n" +
      "\"Madness Hero.\"\n" +
      "\"Moon Strike.\"\n" +
      "\"Blazing Dynamo.\"";
    textboxText.str_24 = toLodString(additionMenu);
    setupTextboxLines(textbox, textboxText, 6, 20);
    textboxText.charIndex_30 = 0;
    textboxText.charX_34 = 0;
    textboxText.charY_36 = 0;
    textboxText.state_00 = TextboxTextState.PROCESS_TEXT_4;

    int defaultLine = 0;
    try {
      final CharacterData2c dartChar = gameState_800babc8.charData_32c.get(0);
      if (dartChar != null && dartChar.selectedAddition_19 != null) {
        final int curIdx = ELIGIBLE_ADDITIONS.indexOf(dartChar.selectedAddition_19);
        if (curIdx >= 0) {
          defaultLine = curIdx;
        }
      }
    } catch (final Throwable ignored) {}

    populateAndMakeSelection(textboxIndex, 0, 5, defaultLine);
  }

  private static void setupTextboxLines(final Textbox4c textbox, final TextboxText84 textboxText, final int lineCount, final int minChars) {
    if (textbox.chars_18 < minChars + 1) {
      textbox.chars_18 = minChars + 1;
      textbox.width_1c = textbox.chars_18 * 9 / 2;
    }
    textbox.lines_1a = lineCount + 1;
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

  public static void setDartAddition(final Battle battle, final RegistryId additionId) {
    try {
      final CharacterData2c dartChar = gameState_800babc8.charData_32c.get(0);
      if (dartChar != null) {
        dartChar.selectedAddition_19 = additionId;
      }

      for (int i = 0; i < 10; i++) {
        final CombatantStruct1a8 combatant = battle.getCombatant(i);
        if (combatant != null && combatant.isPlayer() && combatant.playerBent != null) {
          if (combatant.playerBent.character == dartChar || combatant.charIndex_1a2 == 0) {
            combatant.playerBent.character.selectedAddition_19 = additionId;
            final var entry = REGISTRIES.additions.getEntry(additionId);
            if (entry != null) {
              combatant.playerBent.addition = entry.get();
            }
            combatant.mrg_04 = null;
            battle.loadAttackAnimations(combatant);
            LOGGER.info("TasmanBattleTutorial: Loaded attack animations for Dart with %s", additionId);
            break;
          }
        }
      }
    } catch (final Throwable t) {
      LOGGER.error("TasmanBattleTutorial: Failed to set Dart addition", t);
    }
  }

  public static void restoreDartAddition() {
    if (originalPlayerAdditionId != null) {
      try {
        final CharacterData2c dartChar = gameState_800babc8.charData_32c.get(0);
        if (dartChar != null) {
          dartChar.selectedAddition_19 = originalPlayerAdditionId;
          LOGGER.info("TasmanBattleTutorial: Restored Dart's character addition to %s", originalPlayerAdditionId);
        }
        if (EngineStates.currentEngineState_8004dd04 instanceof final Battle battle) {
          setDartAddition(battle, originalPlayerAdditionId);
          LOGGER.info("TasmanBattleTutorial: Restored Dart's combatant addition to %s", originalPlayerAdditionId);
        }
      } catch (final Throwable t) {
        LOGGER.error("TasmanBattleTutorial: Error restoring Dart addition", t);
      }
    }
  }

  private static String getExplanationColors() {
    final ControllerTheme theme = ControllerTheme.getActiveTheme();
    return switch (theme) {
      case XBOX ->
        "Controller Button Colors:\n" +
        "Blue: X   |  Green: A\n" +
        "Red:  B   |  Yellow: Y\n" +
        "Hit each button on time!";
      case PLAYSTATION ->
        "Controller Button Colors:\n" +
        "Blue: Cross | Red: Circle\n" +
        "Pink: Square| Green: Triangle\n" +
        "Hit each button on time!";
      case SWITCH ->
        "Controller Button Colors:\n" +
        "Red:  A   |  Yellow: B\n" +
        "Blue: X   |  Green: Y\n" +
        "Hit each button on time!";
      default ->
        "Controller Button Colors:\n" +
        "Blue: Action 1 | Red: Action 2\n" +
        "Pink: Action 3 | Grn: Action 4\n" +
        "Hit each button on time!";
    };
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
