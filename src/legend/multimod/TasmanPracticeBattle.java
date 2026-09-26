package legend.multimod;

import legend.game.combat.encounters.Encounter;
import legend.game.modding.events.RenderEvent;
import legend.game.modding.events.battle.BattleEndedEvent;
import legend.game.modding.events.battle.BattleStartedEvent;
import legend.game.modding.events.submap.SubmapEncounterEvent;
import legend.game.submap.RetailSubmap;
import legend.game.submap.SMap;
import legend.lodmod.LodEncounters;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static legend.game.Scus94491BpeSegment_800b.battleStage_800bb0f4;

public class TasmanPracticeBattle {
  private static final Logger LOGGER = LogManager.getFormatterLogger(TasmanPracticeBattle.class);

  public static final int HOAX_DAYTIME_STAGE = 13;
  public static final int SELES_STAGE = 2;

  private static boolean practiceActive = false;
  private static boolean inBattle = false;
  private static boolean returningFromPractice = false;

  public static void initScriptHooks() {
    // Opcode hooks are handled comprehensively by TasmanBattleTutorial in tvvlr_multimod
  }

  public static boolean isPracticeActive() {
    return practiceActive;
  }

  public static boolean isReturningFromPractice() {
    return returningFromPractice;
  }

  public static void setReturningFromPractice(final boolean returning) {
    returningFromPractice = returning;
  }

  public static void startPracticeBattle(final SMap smap) {
    practiceActive = true;
    inBattle = false;
    returningFromPractice = false;

    final boolean isSeles = (smap.submap instanceof RetailSubmap retail) && (retail.cut >= 8 && retail.cut <= 12);
    final int stageId = isSeles ? SELES_STAGE : HOAX_DAYTIME_STAGE;
    battleStage_800bb0f4 = stageId;

    try {
      final Encounter encounter = LodEncounters.ENCOUNTER_MASTER_TASMAN.get();
      LOGGER.info("TasmanPracticeBattle: Launching practice battle against Master Tasman. Stage: %d with current party", stageId);
      TasmanNpcManager.cleanupNpc(smap);
      smap.submap.prepareEncounter(encounter, true);
      smap.mapTransition(-1, 0);
    } catch (final Throwable t) {
      LOGGER.error("TasmanPracticeBattle: Failed to start practice battle", t);
    }
  }

  public static void onSubmapEncounter(final SubmapEncounterEvent event) {
    if (practiceActive || (event.encounter != null && event.encounter.uniqueIds.contains(254))) {
      final boolean isSeles = (event.submapId >= 8 && event.submapId <= 12)
          || (smapCutIsSeles(event.submapId));
      final int stageId = isSeles ? SELES_STAGE : HOAX_DAYTIME_STAGE;
      event.battleStageId = stageId;
      battleStage_800bb0f4 = stageId;
      LOGGER.info("TasmanPracticeBattle: Enforcing battle stage %d for Master Tasman encounter (isSeles=%b)", stageId, isSeles);
    }
  }

  private static boolean smapCutIsSeles(final int submapId) {
    return submapId == 10 || submapId == 11 || submapId == 12;
  }

  public static void onBattleStarted(final BattleStartedEvent event) {
    if (practiceActive || (event.encounter != null && event.encounter.uniqueIds.contains(254))) {
      inBattle = true;
      LOGGER.info("TasmanPracticeBattle: Master Tasman practice battle began.");
    }
  }

  public static void onBattleEnded(final BattleEndedEvent event) {
    if (practiceActive || inBattle) {
      LOGGER.info("TasmanPracticeBattle: Master Tasman practice battle ended.");
      inBattle = false;
      practiceActive = false;
      returningFromPractice = true;
    }
  }

  public static void onRender(final RenderEvent event) {
    // Battle render hooks handled in TasmanBattleTutorial
  }
}
