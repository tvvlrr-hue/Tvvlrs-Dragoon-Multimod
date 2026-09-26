package legend.multimod;

import legend.core.MathHelper;
import legend.core.gpu.Rect4i;
import legend.game.EngineStates;
import legend.game.Text;
import legend.game.scripting.ScriptFile;
import legend.game.scripting.ScriptState;
import legend.game.submap.RetailSubmap;
import legend.game.submap.SMap;
import legend.game.submap.SobjPos14;
import legend.game.submap.SubmapObject;
import legend.game.submap.SubmapObject210;
import legend.game.tim.Tim;
import legend.game.tmd.UvAdjustmentMetrics14;
import legend.game.inventory.WhichMenu;
import legend.game.submap.SubmapState;
import legend.game.types.BackgroundType;
import legend.game.types.CContainer;
import legend.game.types.LodString;
import legend.game.types.Textbox4c;
import legend.game.types.TextboxChar08;
import legend.game.types.TextboxState;
import legend.game.types.TextboxText84;
import legend.game.types.TextboxTextState;
import legend.game.types.TextboxType;
import legend.game.types.TmdAnimationFile;
import legend.game.unpacker.FileData;
import legend.game.unpacker.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import legend.game.submap.SubmapEnvState;
import static legend.core.GameEngine.GPU;
import static legend.core.GameEngine.PLATFORM;
import static legend.core.GameEngine.SCRIPTS;
import static legend.game.Menus.whichMenu_800bdc38;
import static legend.game.Models.loadModelStandardAnimation;
import static legend.game.Scus94491BpeSegment_8005.submapEnvState_80052c44;
import static legend.game.Scus94491BpeSegment_800b.sobjPositions_800bd818;
import static legend.game.Text.calculateAppropriateTextboxBounds;
import static legend.game.Text.clearTextbox;
import static legend.game.Text.clearTextboxText;
import static legend.game.Text.setTextboxArrowPosition;
import static legend.game.Text.textboxText_800bdf38;
import static legend.game.Text.textboxes_800be358;
import static legend.lodmod.LodMod.INPUT_ACTION_SMAP_INTERACT;

public class TasmanNpcManager {
  private static final Logger LOGGER = LogManager.getFormatterLogger(TasmanNpcManager.class);

  public static final float INTERACT_RADIUS = 90.0f;

  public record CitySpawn(int submapId, int cut, String cityName, Vector3f spawnPos, float rotY, String greeting) {}

  private static final List<CitySpawn> CITIES = List.of(
    // 1. Bale (Cut 68 - Slambert Plaza)
    new CitySpawn(
      8, 68, "Bale",
      new Vector3f(-46.0f, 6.0f, 20.8f), 0.0f,
      "Dart! Good to see you in\n" +
      "Slambert Plaza! Even knights in\n" +
      "heavy armor are no match for\n" +
      "sharp additions! Care to spar?"
    ),

    // 2. Hoax (Cut 95 - pre-battle daytime)
    new CitySpawn(
      10, 95, "Hoax",
      new Vector3f(13.0f, 15.0f, 223.0f), 3.14f,
      "Dart! A frontline fortress\n" +
      "is the ultimate proving ground.\n" +
      "Sandora won't wait for you,\n" +
      "so keep your blade ready!"
    ),

    // Hoax (Cut 97 - post-battle daytime)
    new CitySpawn(
      10, 97, "Hoax",
      new Vector3f(13.0f, 15.0f, 223.0f), 3.14f,
      "Dart! You held the line against\n" +
      "Sandora with true courage.\n" +
      "Never let your edge dull,\n" +
      "for greater trials lie ahead!"
    ),

    // Hoax (Cut 96 - night defense)
    new CitySpawn(
      10, 96, "Hoax",
      new Vector3f(13.0f, 15.0f, 223.0f), 3.14f,
      "Dart! Sandora strikes under\n" +
      "darkness! Stand your ground,\n" +
      "keep calm, and execute your\n" +
      "additions with precision!"
    ),

    // 3. Lohan (Cut 140)
    new CitySpawn(
      14, 140, "Lohan",
      new Vector3f(160.3f, 9.0f, -607.3f), 1.57f,
      "Ha-ha! Don't let the festival\n" +
      "distract you, Dart!\n" +
      "A swordsman must always keep\n" +
      "his rhythm sharp. Care to spar?"
    ),

    // 4. Fletz (Cut 201)
    new CitySpawn(
      19, 201, "Fletz",
      new Vector3f(-271.0f, 0.0f, -875.8f), 0.0f,
      "Dart! What a grand city!\n" +
      "The twin towers of Tiberoa\n" +
      "are impressive, but true strength\n" +
      "comes from your own two hands!"
    ),

    // 5. Donau (Cut 239)
    new CitySpawn(
      22, 239, "Donau",
      new Vector3f(-177.5f, 0.0f, -113.8f), 1.57f,
      "Dart! Donau is full of flowers,\n" +
      "but danger lurks outside.\n" +
      "Never let your guard down!\n" +
      "Care to spar with me?"
    ),

    // 7. Deningrad (Cut 347)
    new CitySpawn(
      34, 347, "Deningrad",
      new Vector3f(284.0f, 0.0f, -164.8f), 0.0f,
      "Dart! The Crystal Palace is\n" +
      "magnificent, isn't it?\n" +
      "Even in this sacred city,\n" +
      "a swordsman never stops training!"
    ),

    // 8. Rouge (Cut 563)
    new CitySpawn(
      53, 563, "Rouge",
      new Vector3f(533.5f, -7.0f, -705.5f), 1.57f,
      "Ha-ha! Dart! To think we'd meet\n" +
      "in a village by the sea!\n" +
      "The rhythm of the waves reminds\n" +
      "me of a flowing addition!"
    )
  );

  public enum DialogueState {
    IDLE,
    GREETING,
    MAIN_MENU,
    FAREWELL,
    POST_PRACTICE_RETURN
  }

  private static DialogueState state = DialogueState.IDLE;
  private static CitySpawn activeCity = null;
  private static int npcSobjIndex = -1;
  private static UvAdjustmentMetrics14 activeTasmanMetrics = null;
  private static Tim cachedTasmanTim = null;
  private static int textureUploadedForCut = -1;
  private static int cooldownTicks = 0;
  private static final Vector3f dartLockedPos = new Vector3f();
  private static ScriptState<SubmapObject210> pausedDartScript = null;
  private static ScriptState<SubmapObject210> tasmanScriptState = null;

  public static void cleanupNpc(final SMap smap) {
    if (pausedDartScript != null) {
      pausedDartScript.resume();
      pausedDartScript = null;
    }
    if (smap != null && npcSobjIndex >= 0 && npcSobjIndex < smap.sobjs_800c6880.length) {
      final ScriptState<SubmapObject210> state = smap.sobjs_800c6880[npcSobjIndex];
      if (state != null) {
        final SubmapObject210 sobj = state.innerStruct_00;
        if (sobj != null && sobj.model_00 != null) {
          sobj.model_00.deleteModelParts();
        }
        if (sobj != null && sobj.texture != null) {
          sobj.texture.delete();
          sobj.texture = null;
        }
        try {
          state.setDestructor(null);
          state.deallocateWithChildren();
        } catch (final Exception ignored) {}
        smap.sobjs_800c6880[npcSobjIndex] = null;
      }
      if (smap.submap != null) {
        if (npcSobjIndex < smap.submap.objects.size()) {
          smap.submap.objects.remove(npcSobjIndex);
        }
        if (npcSobjIndex < smap.submap.uvAdjustments.size()) {
          smap.submap.uvAdjustments.remove(npcSobjIndex);
        }
        if (smap.submap instanceof RetailSubmap retail) {
          retail.resetSobjBounds();
        }
      }
    } else if (tasmanScriptState != null) {
      final SubmapObject210 sobj = tasmanScriptState.innerStruct_00;
      if (sobj != null && sobj.model_00 != null) {
        sobj.model_00.deleteModelParts();
      }
      if (sobj != null && sobj.texture != null) {
        sobj.texture.delete();
        sobj.texture = null;
      }
      try {
        tasmanScriptState.setDestructor(null);
        tasmanScriptState.deallocateWithChildren();
      } catch (final Exception ignored) {}
    }
    if (npcSobjIndex >= 0 && npcSobjIndex < sobjPositions_800bd818.length) {
      sobjPositions_800bd818[npcSobjIndex].pos_00.zero();
      sobjPositions_800bd818[npcSobjIndex].rot_0c.zero();
    }
    tasmanScriptState = null;
    npcSobjIndex = -1;
    activeTasmanMetrics = null;
    state = DialogueState.IDLE;
    activeCity = null;
  }

  public static void onSubmapLoad(final SMap smap, final RetailSubmap retail, final List<SubmapObject> objects) {
    cleanupNpc(smap);

    final int currentCut = retail.cut;
    CitySpawn foundCity = null;
    for (final CitySpawn city : CITIES) {
      if (city.cut == currentCut) {
        foundCity = city;
        break;
      }
    }

    if (foundCity == null) {
      return;
    }

    activeCity = foundCity;

    try {
      // 1. Load Tasman's authentic 3D TMD model (Seles Cut 12, Sobj 1 TMD: SECT/DRGN21.BIN/41/33)
      final FileData modelData = Loader.loadFileSync("SECT/DRGN21.BIN/41/33");
      if (modelData == null) {
        LOGGER.error("TasmanNpcManager: Failed to load authentic Tasman TMD from SECT/DRGN21.BIN/41/33");
        return;
      }
      final CContainer tasmanModel = new CContainer("Master Tasman Model", modelData);

      // 2. Load Tasman's authentic 4-bit TIM texture (SECT/DRGN21.BIN/41/textures/1)
      final FileData texData = Loader.loadFileSync("SECT/DRGN21.BIN/41/textures/1");
      if (texData == null) {
        LOGGER.error("TasmanNpcManager: Failed to load authentic Tasman TIM from SECT/DRGN21.BIN/41/textures/1");
        return;
      }
      final Tim tasmanTim = new Tim(texData);
      cachedTasmanTim = tasmanTim;

      // 3. Load Tasman's authentic animations (SECT/DRGN21.BIN/41/34 through 42)
      // Animation 34 is his authentic idle stance.
      final List<TmdAnimationFile> tasmanAnimations = new ArrayList<>();
      for (int a = 34; a <= 42; a++) {
        final FileData aData = Loader.loadFileSync("SECT/DRGN21.BIN/41/" + a);
        if (aData != null) {
          final TmdAnimationFile anim = new TmdAnimationFile(aData);
          anim.interpolationScale = 2.0f; // Authentic relaxed breathing cadence
          tasmanAnimations.add(anim);
        }
      }
      if (tasmanAnimations.isEmpty()) {
        LOGGER.error("TasmanNpcManager: Failed to load Tasman animations");
        return;
      }

      // 4. Find an unused VRAM slot for Tasman's texture.
      // Submaps allocate up to 20 slots (slots 0..19). We search backwards from slot 19.
      final boolean[] usedSlots = new boolean[20];
      for (final UvAdjustmentMetrics14 uv : retail.uvAdjustments) {
        if (uv != null && uv.index != 0) {
          final int sx = (uv.tpageX - 576) / 16;
          final int sy = (uv.tpageY - 256) / 128;
          final int slot = sy * 12 + sx;
          if (slot >= 0 && slot < 20) {
            usedSlots[slot] = true;
          }
        }
      }
      int chosenSlot = -1;
      for (int s = 19; s >= 0; s--) {
        if (!usedSlots[s]) {
          chosenSlot = s;
          break;
        }
      }
      if (chosenSlot == -1) {
        chosenSlot = 19; // Fallback
      }

      final int slotX = 576 + (chosenSlot % 12) * 16;
      final int slotY = 256 + (chosenSlot / 12) * 128;

      // 5. Construct UvAdjustmentMetrics14 for the chosen VRAM slot
      final UvAdjustmentMetrics14 tasmanMetrics = new UvAdjustmentMetrics14(99, slotX, slotY);

      // 6. Upload Tasman's texture and palette (CLUT) to GPU VRAM
      final Rect4i imgRect = tasmanTim.getImageRect();
      final Rect4i clutRect = tasmanTim.getClutRect();
      imgRect.x = tasmanMetrics.tpageX;
      imgRect.y = tasmanMetrics.tpageY;
      clutRect.x = tasmanMetrics.clutX;
      clutRect.y = tasmanMetrics.clutY;
      GPU.uploadData15(imgRect, tasmanTim.getImageData());
      GPU.uploadData15(clutRect, tasmanTim.getClutData());
      textureUploadedForCut = currentCut;

      // 7. Assemble SubmapObject
      final SubmapObject npcObj = new SubmapObject();
      npcObj.constructor = name -> {
        final SubmapObject210 s = new SubmapObject210(name);
        s.model_00.coord2_14.transforms.scale.set(1.0f, 1.0f, 1.0f);
        return s;
      };
      npcObj.model = tasmanModel;
      npcObj.animations.addAll(tasmanAnimations);
      // Bytecode: yield (0), rewind (1)
      npcObj.script = new ScriptFile("Tasman NPC Script", new byte[]{0, 0, 0, 0, 1, 0, 0, 0});

      final int newIndex = objects.size();
      if (newIndex < smap.sobjs_800c6880.length) {
        objects.add(npcObj);
        retail.uvAdjustments.add(tasmanMetrics);
        retail.resetSobjBounds();

        // Use pre-tested verified walkable spawn position
        final Vector3f spawnPos = new Vector3f(foundCity.spawnPos);

        final SobjPos14 pos = sobjPositions_800bd818[newIndex];
        pos.pos_00.set(spawnPos);
        pos.rot_0c.set(0.0f, foundCity.rotY, 0.0f);

        npcSobjIndex = newIndex;
        activeTasmanMetrics = tasmanMetrics;
        cooldownTicks = 30;
        LOGGER.info("TasmanNpcManager: Spawned authentic Master Tasman in %s (Cut %d) at index %d, pos=(%.1f, %.1f, %.1f), vramSlot=%d (%d, %d)",
            foundCity.cityName, currentCut, newIndex, spawnPos.x, spawnPos.y, spawnPos.z, chosenSlot, slotX, slotY);
      } else {
        LOGGER.warn("TasmanNpcManager: Cannot spawn Tasman in %s, sobjs limit reached (%d)", foundCity.cityName, newIndex);
      }
    } catch (final Throwable t) {
      LOGGER.error("TasmanNpcManager: Failed to spawn Master Tasman in cut %d", currentCut, t);
    }
  }

  public static void onRender() {
    if (activeCity == null) {
      if (pausedDartScript != null) {
        pausedDartScript.resume();
        pausedDartScript = null;
      }
      state = DialogueState.IDLE;
      return;
    }

    if (!(EngineStates.currentEngineState_8004dd04 instanceof final SMap smap)) {
      cleanupNpc(null);
      return;
    }

    if (!(smap.submap instanceof final RetailSubmap retail) || retail.cut != activeCity.cut) {
      cleanupNpc(smap);
      return;
    }

    // Check if submap is transitioning or unloading
    final boolean isTransitioning = smap.smapLoadingStage_800cb430 == SubmapState.CHANGE_SUBMAP_4
        || smap.smapLoadingStage_800cb430 == SubmapState.TRANSITION_TO_SUBMAP_17
        || smap.smapLoadingStage_800cb430 == SubmapState.TRANSITION_TO_ENGINE_STATE_18
        || smap.smapLoadingStage_800cb430 == SubmapState.TRANSITION_TO_COMBAT_19
        || smap.smapLoadingStage_800cb430 == SubmapState.TRANSITION_TO_TITLE_20
        || smap.smapLoadingStage_800cb430 == SubmapState.TRANSITION_TO_FMV_21
        || submapEnvState_80052c44 == SubmapEnvState.RENDER_AND_UNLOAD_4_5
        || submapEnvState_80052c44 == SubmapEnvState.UNLOAD_3
        || EngineStates.engineStateOnceLoaded_8004dd24 != null;

    if (isTransitioning) {
      LOGGER.info("TasmanNpcManager: Submap transition/unload detected (stage=%s, envState=%s). Cleaning up Tasman NPC.",
          smap.smapLoadingStage_800cb430, submapEnvState_80052c44);
      cleanupNpc(smap);
      return;
    }


    if (cooldownTicks > 0) {
      cooldownTicks--;
    }

    if (npcSobjIndex < 0 || npcSobjIndex >= smap.sobjs_800c6880.length) {
      return;
    }

    final ScriptState<SubmapObject210> npcState = smap.sobjs_800c6880[npcSobjIndex];
    final ScriptState<SubmapObject210> dartState = smap.sobjs_800c6880[0];
    if (npcState == null || dartState == null) {
      return;
    }
    tasmanScriptState = npcState;

    final SubmapObject210 npcSobj = npcState.innerStruct_00;
    final SubmapObject210 dartSobj = dartState.innerStruct_00;

    npcSobj.hidden_128 = false;
    npcSobj.model_00.coord2_14.coord.transfer.set(activeCity.spawnPos);
    npcSobj.model_00.coord2_14.transforms.scale.set(1.0f, 1.0f, 1.0f);
    npcSobj.model_00.interpolationScale = 2.0f;

    if (activeTasmanMetrics != null) {
      npcSobj.model_00.uvAdjustments_9d = activeTasmanMetrics;
    }

    if (textureUploadedForCut != retail.cut && activeTasmanMetrics != null && cachedTasmanTim != null) {
      final Rect4i imgRect = cachedTasmanTim.getImageRect();
      final Rect4i clutRect = cachedTasmanTim.getClutRect();
      imgRect.x = activeTasmanMetrics.tpageX;
      imgRect.y = activeTasmanMetrics.tpageY;
      clutRect.x = activeTasmanMetrics.clutX;
      clutRect.y = activeTasmanMetrics.clutY;
      GPU.uploadData15(imgRect, cachedTasmanTim.getImageData());
      GPU.uploadData15(clutRect, cachedTasmanTim.getClutData());
      textureUploadedForCut = retail.cut;
    }

    final Vector3f dartPos = dartSobj.model_00.coord2_14.coord.transfer;
    final float dx = dartPos.x - activeCity.spawnPos.x;
    final float dz = dartPos.z - activeCity.spawnPos.z;
    final float distSq = dx * dx + dz * dz;
    final float dist = (float) Math.sqrt(distSq);

    // Solid collision with Tasman so Dart cannot walk through him
    final float minDist = 26.0f;
    if (dist < minDist && dist > 0.001f && state == DialogueState.IDLE) {
      final float overlap = minDist - dist;
      final float nx = dx / dist;
      final float nz = dz / dist;
      dartPos.x += nx * overlap;
      dartPos.z += nz * overlap;
    }

    final boolean isNear = distSq < (INTERACT_RADIUS * INTERACT_RADIUS);

    // Normal town NPCs do NOT display the '!' indicator over their head
    npcSobj.showAlertIndicator_194 = false;

    final float angleToNpc = MathHelper.positiveAtan2(activeCity.spawnPos.z - dartPos.z, activeCity.spawnPos.x - dartPos.x);
    final float dartRotY = dartSobj.model_00.coord2_14.transforms.rotate.y;
    final boolean isFacingNpc = angleDifference(dartRotY, angleToNpc) < 1.6f;

    // Check if returning from practice battle
    if (TasmanPracticeBattle.isReturningFromPractice() && state == DialogueState.IDLE && cooldownTicks == 0) {
      TasmanPracticeBattle.setReturningFromPractice(false);
      handleReturnFromPractice(smap, dartState, dartSobj, npcSobj);
      return;
    }

    // Smoothly turn Tasman toward his target orientation
    final float targetTasmanRotY = (state == DialogueState.IDLE)
        ? activeCity.rotY
        : MathHelper.positiveAtan2(dartLockedPos.z - activeCity.spawnPos.z, dartLockedPos.x - activeCity.spawnPos.x);

    final float currTasmanRotY = npcSobj.model_00.coord2_14.transforms.rotate.y;
    final float rotDiff = MathHelper.closestAngle(targetTasmanRotY, currTasmanRotY);
    if (Math.abs(rotDiff) < 0.12f) {
      npcSobj.model_00.coord2_14.transforms.rotate.y = targetTasmanRotY;
    } else {
      npcSobj.model_00.coord2_14.transforms.rotate.y = MathHelper.floorMod(currTasmanRotY + Math.signum(rotDiff) * 0.12f, MathHelper.TWO_PI);
    }

    if (state != DialogueState.IDLE) {
      // Dart faces Tasman during interaction
      dartSobj.model_00.coord2_14.coord.transfer.set(dartLockedPos);
      dartSobj.movementStep_148.zero();
      dartSobj.interpMovementTicks = 0;
      dartSobj.interpMovementTicksTotal = 0;
      dartSobj.movementTicks_144 = 0;
      dartSobj.movementType_170 = 0;
      dartSobj.animIndex_132 = 0;

      final float dartFacingAngle = MathHelper.positiveAtan2(activeCity.spawnPos.z - dartLockedPos.z, activeCity.spawnPos.x - dartLockedPos.x);
      dartSobj.model_00.coord2_14.transforms.rotate.y = dartFacingAngle;
      dartSobj.interpRotationTicksTotalY = 0;
      dartSobj.rotationFrames_188 = 0;
    }

    // Process dialogue flow
    switch (state) {
      case IDLE -> {
        if (isNear && isFacingNpc && cooldownTicks == 0 && canInteract(smap) && PLATFORM.isActionPressed(INPUT_ACTION_SMAP_INTERACT.get())) {
          LOGGER.info("TasmanNpcManager: Dart interacting with Master Tasman in %s", activeCity.cityName);
          startGreeting(smap, dartState, dartSobj);
        }
      }

      case GREETING -> {
        advanceTextboxOrConfirm(0, TasmanNpcManager::showMainMenu);
      }

      case MAIN_MENU -> {
        final TextboxText84 tbText0 = textboxText_800bdf38[0];
        final int selection = tbText0.selectionIndex_6c;
        if (selection >= 0) {
          LOGGER.info("TasmanNpcManager: Main menu selection: %d", selection);
          if (selection == 0) {
            // "Practice additions." -> Enter battle directly with current party!
            startPracticeBattle(smap);
          } else {
            // "I'm good for now." -> Farewell
            showFarewell();
          }
        }
      }

      case FAREWELL -> {
        advanceTextboxOrConfirm(0, TasmanNpcManager::closeDialogue);
      }

      case POST_PRACTICE_RETURN -> {
        advanceTextboxOrConfirm(0, TasmanNpcManager::closeDialogue);
      }
    }
  }

  private static void handleReturnFromPractice(final SMap smap, final ScriptState<SubmapObject210> dartState,
                                               final SubmapObject210 dartSobj, final SubmapObject210 npcSobj) {
    dartLockedPos.set(dartSobj.model_00.coord2_14.coord.transfer);
    pausedDartScript = dartState;
    dartState.pause();
    state = DialogueState.POST_PRACTICE_RETURN;
    cooldownTicks = 20;

    final String returnText =
      "A fine round of training, Dart!\n" +
      "Come speak with me anytime you\n" +
      "wish to sharpen your blade!";
    openNamedTextbox(0, 160, 150, 38, 3, "Tasman", returnText);
  }

  private static void startGreeting(final SMap smap, final ScriptState<SubmapObject210> dartState, final SubmapObject210 dartSobj) {
    dartLockedPos.set(dartSobj.model_00.coord2_14.coord.transfer);
    state = DialogueState.GREETING;
    cooldownTicks = 15;
    pausedDartScript = dartState;
    dartState.pause();

    dartSobj.animIndex_132 = 0;
    if (smap.submap != null && !smap.submap.objects.isEmpty() && dartSobj.sobjIndex_12e < smap.submap.objects.size()) {
      final SubmapObject playerObj = smap.submap.objects.get(dartSobj.sobjIndex_12e);
      if (!playerObj.animations.isEmpty()) {
        loadModelStandardAnimation(dartSobj.model_00, playerObj.animations.get(0));
      }
    }

    dartSobj.movementStep_148.zero();
    dartSobj.interpMovementTicks = 0;
    dartSobj.interpMovementTicksTotal = 0;
    dartSobj.interpRotationTicksTotalY = 0;
    dartSobj.rotationFrames_188 = 0;

    final float dartFacingAngle =
        MathHelper.positiveAtan2(activeCity.spawnPos.z - dartLockedPos.z, activeCity.spawnPos.x - dartLockedPos.x);
    dartSobj.model_00.coord2_14.transforms.rotate.y = dartFacingAngle;

    openNamedTextbox(0, 160, 150, 38, 5, "Tasman", activeCity.greeting);
  }

  private static void showMainMenu() {
    state = DialogueState.MAIN_MENU;
    cooldownTicks = 15;

    openNamedTextbox(1, 160, 95, 36, 1, "Tasman", "Care to spar with me?");

    final String choices =
      "Practice additions.\n" +
      "I'm good for now.";
    openNamedTextbox(0, 160, 165, 36, 2, "Dart", choices);
    configureSelection(0, 1, 2, 1);
  }

  private static void startPracticeBattle(final SMap smap) {
    closeDialogueBoxes();
    if (pausedDartScript != null) {
      pausedDartScript.resume();
      pausedDartScript = null;
    }
    state = DialogueState.IDLE;
    TasmanPracticeBattle.startPracticeBattle(smap);
  }

  private static void showFarewell() {
    state = DialogueState.FAREWELL;
    cooldownTicks = 15;

    final String farewellText =
      "Until next time, Dart!\n" +
      "Keep your blade sharp!";
    openNamedTextbox(0, 160, 150, 38, 2, "Tasman", farewellText);
  }

  private static boolean canInteract(final SMap smap) {
    if (smap.smapLoadingStage_800cb430 != SubmapState.RENDER_SUBMAP_12) {
      return false;
    }
    if (whichMenu_800bdc38 != WhichMenu.NONE_0) {
      return false;
    }
    if (SCRIPTS.isPaused()) {
      return false;
    }
    if (textboxText_800bdf38[0].state_00 != TextboxTextState.UNINITIALIZED_0
        || textboxes_800be358[0].state_00 != TextboxState.UNINITIALIZED_0) {
      return false;
    }
    return true;
  }

  private static void advanceTextboxOrConfirm(final int textboxIndex, final Runnable onConfirmed) {
    final TextboxText84 tbText0 = textboxText_800bdf38[textboxIndex];

    if (PLATFORM.isActionPressed(INPUT_ACTION_SMAP_INTERACT.get()) && cooldownTicks == 0) {
      if (tbText0.state_00 != TextboxTextState.WAIT_FOR_INPUT_ADVANCED_DIRTY_BOX_6
          && tbText0.state_00 != TextboxTextState.CLOSE_TEXTBOX_15
          && tbText0.charIndex_30 < tbText0.str_24.length()) {
        while (tbText0.charIndex_30 < tbText0.str_24.length()
            && tbText0.state_00 != TextboxTextState.WAIT_FOR_INPUT_ADVANCED_DIRTY_BOX_6
            && tbText0.state_00 != TextboxTextState.CLOSE_TEXTBOX_15) {
          Text.processTextboxCharacter(textboxIndex);
        }
        setTextboxArrowPosition(textboxIndex, true);
        cooldownTicks = 10;
      } else {
        onConfirmed.run();
      }
    }
  }

  private static void configureSelection(final int textboxIndex, final int minLine, final int maxLine, final int defaultLine) {
    final TextboxText84 tbText = textboxText_800bdf38[textboxIndex];
    while (tbText.charIndex_30 < tbText.str_24.length()
        && tbText.state_00 != TextboxTextState.CLOSE_TEXTBOX_15) {
      Text.processTextboxCharacter(textboxIndex);
    }
    tbText.flags_08 |= TextboxText84.SELECTION;
    tbText.minSelectionLine_72 = minLine;
    tbText.maxSelectionLine_70 = maxLine;
    tbText.selectionLine_68 = defaultLine;
    tbText.selectionIndex_6c = -1;

    tbText.ticksUntilStateTransition_64 = 10;
    tbText.stateAfterTransition_78 = TextboxTextState.SELECTION_22;
    tbText.state_00 = TextboxTextState.TRANSITION_AFTER_TIMEOUT_23;
  }

  private static void closeDialogueBoxes() {
    safelyClearTextbox(0);
    safelyClearTextbox(1);
  }

  private static void closeDialogue() {
    state = DialogueState.IDLE;
    cooldownTicks = 20;

    if (pausedDartScript != null) {
      pausedDartScript.resume();
      pausedDartScript = null;
    }

    closeDialogueBoxes();
    LOGGER.info("TasmanNpcManager: Dialogue closed.");
  }

  private static void safelyClearTextbox(final int index) {
    try {
      clearTextbox(index);
      clearTextboxText(index);

      final TextboxText84 tbText = textboxText_800bdf38[index];
      tbText.state_00 = TextboxTextState.UNINITIALIZED_0;
      tbText.chars_58 = null;

      final Textbox4c tb = textboxes_800be358[index];
      tb.state_00 = TextboxState.UNINITIALIZED_0;
      tb.flags_08 = 0;
      tb.width_1c = 0;
      tb.height_1e = 0;
      tb.oldW = 0;
      tb.oldH = 0;
    } catch (final Throwable ignored) {}
  }

  private static void openNamedTextbox(final int index, final int x, final int y, final int chars, final int lines, final String name, final String text) {
    safelyClearTextbox(index);

    final Textbox4c textbox = textboxes_800be358[index];
    final TextboxText84 textboxText = textboxText_800bdf38[index];

    textbox.backgroundType_04 = BackgroundType.NORMAL;
    textbox.renderBorder_06 = true;
    textbox.flags_08 = Textbox4c.RENDER_BACKGROUND | Textbox4c.NO_ANIMATE_OUT;
    textbox.state_00 = TextboxState._6;
    textbox.x_14 = x;
    textbox.y_16 = y;
    final int safeChars = Math.max(chars, 38);
    final int safeLines = Math.max(lines, 2);
    textbox.chars_18 = safeChars + 4;
    textbox.lines_1a = safeLines + 2;
    textbox.width_1c = textbox.chars_18 * 9 / 2;
    textbox.height_1e = textbox.lines_1a * 6;
    textbox.oldW = 0;
    textbox.oldH = 0;

    textboxText.type_04 = TextboxType.SMAP_NAMED.id;
    textboxText.flags_08 = TextboxText84.HAS_NAME | TextboxText84.SHOW_ARROW;
    textboxText.str_24 = buildNamedLodString(name, text);
    textboxText.chars_1c = textbox.chars_18 - 1;
    textboxText.lines_1e = safeLines + 1;
    textboxText.chars_58 = new TextboxChar08[textboxText.chars_1c * (textboxText.lines_1e + 1)];
    Arrays.setAll(textboxText.chars_58, i -> new TextboxChar08());
    textboxText.charIndex_30 = 0;
    textboxText.charX_34 = 0;
    textboxText.charY_36 = 0;
    textboxText.state_00 = TextboxTextState.PROCESS_TEXT_4;

    calculateAppropriateTextboxBounds(index, x, y);
  }

  private static LodString buildNamedLodString(final String name, final String text) {
    final List<Integer> list = new ArrayList<>();
    for (int i = 0; i < name.length(); i++) {
      list.add(LodString.toLodChar(name.charAt(i)));
    }
    list.add(0xa1ff); // newline

    for (int i = 0; i < text.length(); i++) {
      final char c = text.charAt(i);
      if (c == '\n') {
        list.add(0xa1ff);
      } else {
        list.add(LodString.toLodChar(c));
      }
    }
    list.add(0xa0ff); // end

    final int[] arr = new int[list.size()];
    for (int i = 0; i < list.size(); i++) {
      arr[i] = list.get(i);
    }
    return new LodString(arr);
  }

  private static float angleDifference(final float a, final float b) {
    float diff = (a - b) % ((float) Math.PI * 2.0f);
    if (diff < -Math.PI) diff += (float) Math.PI * 2.0f;
    if (diff > Math.PI) diff -= (float) Math.PI * 2.0f;
    return Math.abs(diff);
  }
}
