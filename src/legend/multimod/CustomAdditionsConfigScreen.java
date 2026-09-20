package legend.multimod;

import legend.core.gpu.Gpu;
import legend.core.gpu.Rect4i;
import legend.core.gte.MV;
import legend.core.gte.ModelPart10;
import legend.core.lang.I18nText;
import legend.core.lang.RawText;
import legend.core.platform.input.InputAction;
import legend.core.platform.input.InputButton;
import legend.core.platform.input.InputKey;
import legend.core.platform.input.InputMod;
import legend.core.renderer.DepthComparator;
import legend.core.renderer.QueuedModelBattleTmd;
import legend.game.additions.Addition;
import legend.game.inventory.screens.HorizontalAlign;
import legend.game.inventory.screens.InputPropagation;
import legend.game.inventory.screens.MenuScreen;
import legend.game.inventory.screens.TextColour;
import legend.game.inventory.screens.controls.Background;
import legend.game.inventory.screens.controls.Button;
import legend.game.inventory.screens.controls.Label;
import legend.game.modding.coremod.CoreMod;
import legend.game.tim.Tim;
import legend.game.tmd.TmdObjLoader;
import legend.game.types.CContainer;
import legend.game.types.Model124;
import legend.game.types.TmdAnimationFile;
import legend.game.unpacker.FileData;
import legend.game.unpacker.Loader;
import legend.game.unpacker.Unpacker;
import legend.lodmod.additions.RetailAddition;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.legendofdragoon.modloader.registries.RegistryId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static legend.core.GameEngine.GPU;
import static legend.core.GameEngine.REGISTRIES;
import static legend.core.GameEngine.RENDERER;
import static legend.game.FullScreenEffects.startFadeEffect;
import static legend.game.Graphics.GsGetLw;
import static legend.game.Menus.deallocateRenderables;
import static legend.game.Models.adjustModelUvs;
import static legend.game.Models.animateModel;
import static legend.game.Models.applyModelRotationAndScale;
import static legend.game.Models.initModel;
import static legend.game.Models.loadModelStandardAnimation;
import static legend.game.Models.vramSlots_8005027c;
import static legend.game.combat.Battle.combatantTimRects_800fa6e0;
import static legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_BACK;
import static legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_CONFIRM;
import static legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_DOWN;
import static legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_LEFT;
import static legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_PAGE_DOWN;
import static legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_PAGE_UP;
import static legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_RIGHT;
import static legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_UP;
import static legend.game.sound.Audio.playMenuSound;

public class CustomAdditionsConfigScreen extends MenuScreen {
  private static final int MAX_ADDITION_SLOTS = 7;
  private static final int MAX_HIT_SLOTS = 6;

  private final Runnable unload;

  private int selectedCharIndex = 0;
  private int selectedAdditionIndex = 0;
  private int selectedHitIndex = 0;

  // Focus Area: 1 = Additions list, 2 = Hits panel, 3 = Save button
  private int focusArea = 1;

  // Header controls
  private final Label titleLabel;
  private final Button prevCharButton;
  private final Label charLabel;
  private final Button nextCharButton;

  // Column headers
  private final Label additionsHeader;
  private final Label hitsHeader;
  private final Label hitsSubHeader;
  private final Label previewHeader;
  private final Label previewHitLabel;

  // Column 1: Additions (Left: x = 12..124)
  private final Button[] additionButtons = new Button[MAX_ADDITION_SLOTS];

  // Column 2: Hits (Middle: x = 132..244)
  private final Label[] hitLabels = new Label[MAX_HIT_SLOTS];
  private final Button[] hitButtons = new Button[MAX_HIT_SLOTS];
  private final Label hitHintLabel1;
  private final Label hitHintLabel2;

  // Column 3: 3D Battle Model Preview
  private Model124 activeModel;
  private Gpu characterGpu;
  private int currentLoadedAdditionFile = -1;
  private int currentLoadedHitIndex = -1;
  private int currentDemonstrationHit = 0;
  private final TmdAnimationFile[] currentAdditionAnimCache = new TmdAnimationFile[16];
  private final MV tempLw = new MV();
  private final Matrix3f studioLightDir = new Matrix3f(
    -0.577f, -0.577f, 0.577f,
    -0.577f, -0.577f, 0.577f,
    -0.577f, -0.577f, 0.577f
  );
  private final Matrix3f studioLightColour = new Matrix3f(
    0.9f, 0.9f, 0.9f,
    0.9f, 0.9f, 0.9f,
    0.9f, 0.9f, 0.9f
  );
  private final Vector3f studioAmbient = new Vector3f(0.85f, 0.85f, 0.85f);

  // Bottom action
  private final Button saveButton;
  private final Label statusLabel;

  public CustomAdditionsConfigScreen(final Runnable unload) {
    deallocateRenderables(0xff);
    startFadeEffect(2, 10);

    this.unload = unload;
    this.addControl(new Background());

    final int screenWidth = this.getWidth(); // 368

    // Screen Title (y = 6)
    this.titleLabel = this.addControl(new Label(new RawText("CUSTOM ADDITIONS CONFIGURATION")));
    this.titleLabel.getFontOptions().horizontalAlign(HorizontalAlign.CENTRE);
    this.titleLabel.getFontOptions().colour(TextColour.GOLD).shadowColour(TextColour.BLACK);
    this.titleLabel.setPos(0, 6);
    this.titleLabel.setWidth(screenWidth);

    // Character Selector with Bumper Indicators (y = 20)
    this.prevCharButton = this.addControl(new Button(new RawText("[L1]")));
    this.prevCharButton.setPos(40, 20);
    this.prevCharButton.setSize(28, 12);
    this.prevCharButton.getFontOptions().colour(TextColour.LIME);
    this.prevCharButton.onPressed(() -> this.switchCharacter(-1));

    this.charLabel = this.addControl(new Label(new RawText("")));
    this.charLabel.getFontOptions().horizontalAlign(HorizontalAlign.CENTRE);
    this.charLabel.getFontOptions().colour(TextColour.YELLOW).shadowColour(TextColour.BLACK);
    this.charLabel.setPos(70, 20);
    this.charLabel.setWidth(screenWidth - 140);

    this.nextCharButton = this.addControl(new Button(new RawText("[R1]")));
    this.nextCharButton.setPos(screenWidth - 68, 20);
    this.nextCharButton.setSize(28, 12);
    this.nextCharButton.getFontOptions().colour(TextColour.LIME);
    this.nextCharButton.onPressed(() -> this.switchCharacter(1));

    // Column 1 Header: ADDITIONS (x = 12..124)
    this.additionsHeader = this.addControl(new Label(new RawText("ADDITIONS")));
    this.additionsHeader.getFontOptions().colour(TextColour.GOLD).shadowColour(TextColour.BLACK);
    this.additionsHeader.setPos(12, 36);
    this.additionsHeader.setWidth(112);

    // Column 2 Headers: HITS CONFIG (x = 132..244)
    this.hitsHeader = this.addControl(new Label(new RawText("HITS CONFIG")));
    this.hitsHeader.getFontOptions().colour(TextColour.GOLD).shadowColour(TextColour.BLACK);
    this.hitsHeader.setPos(132, 36);
    this.hitsHeader.setWidth(112);

    this.hitsSubHeader = this.addControl(new Label(new RawText("")));
    this.hitsSubHeader.getFontOptions().colour(TextColour.CYAN).shadowColour(TextColour.BLACK);
    this.hitsSubHeader.setPos(132, 48);
    this.hitsSubHeader.setWidth(112);

    // Column 3 Headers: PREVIEW (x = 250..362)
    this.previewHeader = this.addControl(new Label(new RawText("PREVIEW")));
    this.previewHeader.getFontOptions().horizontalAlign(HorizontalAlign.CENTRE);
    this.previewHeader.getFontOptions().colour(TextColour.GOLD).shadowColour(TextColour.BLACK);
    this.previewHeader.setPos(250, 36);
    this.previewHeader.setWidth(112);

    this.previewHitLabel = this.addControl(new Label(new RawText("[ FULL COMBO ]")));
    this.previewHitLabel.getFontOptions().horizontalAlign(HorizontalAlign.CENTRE);
    this.previewHitLabel.getFontOptions().colour(TextColour.LIME).shadowColour(TextColour.BLACK);
    this.previewHitLabel.setPos(250, 48);
    this.previewHitLabel.setWidth(112);

    // Additions Buttons (Left column: x = 12..124)
    for (int i = 0; i < MAX_ADDITION_SLOTS; i++) {
      final int slot = i;
      final Button btn = new Button(new RawText(""));
      btn.setPos(12, 50 + i * 15);
      btn.setSize(112, 13);
      btn.onPressed(() -> {
        this.selectedAdditionIndex = slot;
        this.focusArea = 2;
        this.selectedHitIndex = 0;
        this.clearAnimCache();
        this.updateHitsList();
        this.refreshVisuals();
        playMenuSound(2);
      });
      this.additionButtons[i] = this.addControl(btn);
    }

    // Hits Rows (Middle column: x = 130..242)
    // Label: x = 130, width = 50. Button: x = 182, width = 60. ZERO OVERLAP!
    for (int i = 0; i < MAX_HIT_SLOTS; i++) {
      final int hitSlot = i;
      final Label label = new Label(new RawText("Hit " + (i + 1) + ":"));
      label.setPos(130, 60 + i * 14);
      label.setWidth(50);
      this.hitLabels[i] = this.addControl(label);

      final Button btn = new Button(new RawText("CROSS"));
      btn.setPos(182, 60 + i * 14);
      btn.setSize(60, 13);
      btn.onPressed(() -> {
        this.selectedHitIndex = hitSlot;
        this.focusArea = 2;
        this.cycleHighlightedHit(1);
      });
      this.hitButtons[i] = this.addControl(btn);
    }

    // Help text below hits (x = 132)
    this.hitHintLabel1 = this.addControl(new Label(new RawText("X/Sq/Tri: Map Hit")));
    this.hitHintLabel1.getFontOptions().colour(TextColour.GREY).shadowColour(TextColour.BLACK);
    this.hitHintLabel1.setPos(132, 166);
    this.hitHintLabel1.setWidth(112);

    this.hitHintLabel2 = this.addControl(new Label(new RawText("O: Back to Combo")));
    this.hitHintLabel2.getFontOptions().colour(TextColour.GREY).shadowColour(TextColour.BLACK);
    this.hitHintLabel2.setPos(132, 178);
    this.hitHintLabel2.setWidth(112);

    // Save Button (y = 198)
    this.saveButton = this.addControl(new Button(new RawText("SAVE CONFIGURATION")));
    this.saveButton.setPos((screenWidth - 170) / 2, 198);
    this.saveButton.setSize(170, 14);
    this.saveButton.getFontOptions().colour(TextColour.LIME);
    this.saveButton.onPressed(this::save);

    // Status message label (y = 216)
    this.statusLabel = this.addControl(new Label(new RawText("")));
    this.statusLabel.getFontOptions().horizontalAlign(HorizontalAlign.CENTRE);
    this.statusLabel.getFontOptions().colour(TextColour.LIME).shadowColour(TextColour.BLACK);
    this.statusLabel.setPos(0, 216);
    this.statusLabel.setWidth(screenWidth);

    // Initial load
    this.loadCharacter(0);

    // Back hotkey
    this.addHotkey(new I18nText("lod_core.ui.options_category.back"), INPUT_ACTION_MENU_BACK, this::handleBackAction);
  }

  @Override
  protected void delete() {
    this.cleanupModel();
    super.delete();
  }

  @Override
  protected void render() {
    if (this.activeModel != null) {
      animateModel(this.activeModel, 2);

      // Whole addition demonstration flow when browsing additions (focusArea == 1)
      if (this.focusArea == 1 && this.activeModel.remainingFrames_9e == 0) {
        final CustomAdditionsStorage.CharacterCategory charCat = CustomAdditionsStorage.CHARACTERS.get(this.selectedCharIndex);
        if (this.selectedAdditionIndex < charCat.additions.size()) {
          final CustomAdditionsStorage.AdditionDefinition def = charCat.additions.get(this.selectedAdditionIndex);
          final int totalComboHits = def.hitCount + 1;
          this.currentDemonstrationHit = (this.currentDemonstrationHit + 1) % totalComboHits;
          final TmdAnimationFile nextAnim = this.loadHitAnimation(this.selectedCharIndex, this.selectedAdditionIndex, this.currentDemonstrationHit);
          if (nextAnim != null) {
            loadModelStandardAnimation(this.activeModel, nextAnim);
            this.activeModel.animationState_9c = 1;
          }
          this.previewHitLabel.setText(new RawText("[ COMBO " + (this.currentDemonstrationHit + 1) + "/" + totalComboHits + " ]"));
        }
      }

      final float previewCenterX = 306.0f;
      final float previewCenterY = 135.0f; // character hips ground level
      final float previewCenterZ = 200.0f; // Safe positive Z (> 160)
      final float scale = this.getCharacterScale(this.selectedCharIndex);

      this.activeModel.coord2_14.flg = 0;
      if (this.activeModel.modelParts_00 != null) {
        for (final ModelPart10 part : this.activeModel.modelParts_00) {
          if (part != null && part.coord2_04 != null) {
            part.coord2_04.flg = 0;
          }
        }
      }

      // Set scale and rotation, apply, THEN set translation
      this.activeModel.coord2_14.transforms.scale.set(scale, scale, scale);
      this.activeModel.coord2_14.transforms.rotate.set(0.05f, -0.40f, 0.0f);
      applyModelRotationAndScale(this.activeModel);
      this.activeModel.coord2_14.coord.transfer.set(previewCenterX, previewCenterY, previewCenterZ);

      if (this.activeModel.modelParts_00 != null) {
        for (int i = 0; i < this.activeModel.modelParts_00.length; i++) {
          if ((this.activeModel.partInvisible_f4 & (1L << i)) == 0) {
            final ModelPart10 part = this.activeModel.modelParts_00[i];
            if (part != null && part.tmd_08 != null && part.tmd_08.getObj() != null) {
              GsGetLw(part.coord2_04, this.tempLw);
              final QueuedModelBattleTmd queue = RENDERER.queueOrthoModel(part.tmd_08.getObj(), this.tempLw, QueuedModelBattleTmd.class)
                .lightDirection(this.studioLightDir)
                .lightColour(this.studioLightColour)
                .backgroundColour(new Vector3f(1.0f, 1.0f, 1.0f))
                .battleColour(new Vector3f(1.0f, 1.0f, 1.0f))
                .ctmdFlags(0)
                .tmdTranslucency(this.activeModel.tpage_108 >>> 5 & 0b11)
                .opaqueDepthComparator(DepthComparator.LESS_THAN_OR_EQUAL);

              if (this.characterGpu != null && this.characterGpu.vramTexture15 != null) {
                queue.texture(this.characterGpu.vramTexture15, 1);
              }
            }
          }
        }
      }
    }
  }

  private void switchCharacter(final int direction) {
    playMenuSound(1);
    final int total = CustomAdditionsStorage.CHARACTERS.size();
    this.selectedCharIndex = (this.selectedCharIndex + direction % total + total) % total;
    this.selectedAdditionIndex = 0;
    this.selectedHitIndex = 0;
    this.focusArea = 1;
    this.clearAnimCache();
    this.loadCharacter(this.selectedCharIndex);
  }

  private void updateCharacterHeader() {
    final CustomAdditionsStorage.CharacterCategory character = CustomAdditionsStorage.CHARACTERS.get(this.selectedCharIndex);
    final ControllerTheme theme = ControllerTheme.getActiveTheme();
    final String lBumper = theme == ControllerTheme.XBOX ? "[LB]" : (theme == ControllerTheme.SWITCH ? "[L]" : "[L1]");
    final String rBumper = theme == ControllerTheme.XBOX ? "[RB]" : (theme == ControllerTheme.SWITCH ? "[R]" : "[R1]");
    final int total = CustomAdditionsStorage.CHARACTERS.size();

    this.prevCharButton.setText(new RawText(lBumper));
    this.charLabel.setText(new RawText("<  " + character.characterName.toUpperCase() + " (" + (this.selectedCharIndex + 1) + "/" + total + ")  >"));
    this.nextCharButton.setText(new RawText(rBumper));
  }

  private void loadCharacter(final int index) {
    if (index < 0 || index >= CustomAdditionsStorage.CHARACTERS.size()) {
      return;
    }
    final CustomAdditionsStorage.CharacterCategory character = CustomAdditionsStorage.CHARACTERS.get(index);
    this.updateCharacterHeader();

    this.selectedAdditionIndex = 0;
    this.selectedHitIndex = 0;
    this.clearAnimCache();

    this.loadCharacterModel(index);

    for (int i = 0; i < MAX_ADDITION_SLOTS; i++) {
      if (i < character.additions.size()) {
        final CustomAdditionsStorage.AdditionDefinition add = character.additions.get(i);
        this.additionButtons[i].setText(new RawText(add.displayName));
        this.additionButtons[i].show();
      } else {
        this.additionButtons[i].hide();
      }
    }

    this.updateHitsList();
    this.refreshVisuals();
  }

  private String getCharacterKey(final int charIndex) {
    return switch (charIndex) {
      case 0 -> "dart";
      case 1 -> "lavitz";
      case 2 -> "rose";
      case 3 -> "haschel";
      case 4 -> "albert";
      case 5 -> "meru";
      case 6 -> "kongol";
      default -> "dart";
    };
  }

  private float getCharacterScale(final int charIndex) {
    return switch (charIndex) {
      case 0 -> 0.26f; // Dart
      case 1 -> 0.25f; // Lavitz
      case 2 -> 0.26f; // Rose
      case 3 -> 0.26f; // Haschel
      case 4 -> 0.25f; // Albert
      case 5 -> 0.28f; // Meru
      case 6 -> 0.20f; // Kongol
      default -> 0.25f;
    };
  }

  private void loadCharacterModel(final int charIndex) {
    this.cleanupModel();

    final String charKey = this.getCharacterKey(charIndex);
    try {
      final String modelPath = "characters/" + charKey + "/models/combat/32";
      if (!Loader.exists(modelPath)) {
        return;
      }
      FileData modelData = Loader.loadFileSync(modelPath);
      if (modelData.readInt(0x4) == 0x1a45_5042) {
        modelData = new FileData(Unpacker.decompress(modelData));
      }
      final CContainer cContainer = new CContainer("CombatantModel (" + charKey + ")", modelData);

      final String texPath = "characters/" + charKey + "/textures/combat";
      if (Loader.exists(texPath)) {
        final FileData texData = Loader.loadFileSync(texPath);
        final Tim tim = new Tim(texData);

        if (this.characterGpu != null && this.characterGpu.vramTexture15 != null) {
          this.characterGpu.vramTexture15.delete();
        }
        this.characterGpu = new Gpu();

        final Rect4i combatantTimRect = combatantTimRects_800fa6e0[1]; // (320, 256, 64, 256)
        this.characterGpu.uploadData15(combatantTimRect, tim.getImageData());

        if (tim.hasClut()) {
          final Rect4i clutRect = tim.getClutRect();
          clutRect.x = combatantTimRect.x;
          clutRect.y = combatantTimRect.y + 240;
          this.characterGpu.uploadData15(clutRect, tim.getClutData());
        }

        this.characterGpu.initVram();
        this.characterGpu.updateVramTexture();
      }

      // Initial animation: hit 0 of current addition or fallback to combat/0
      TmdAnimationFile anim = this.loadHitAnimation(charIndex, 0, 0);
      if (anim == null) {
        final String defaultAnimPath = "characters/" + charKey + "/models/combat/0";
        if (Loader.exists(defaultAnimPath)) {
          FileData animData = Loader.loadFileSync(defaultAnimPath);
          if (animData.readInt(0x4) == 0x1a45_5042) {
            animData = new FileData(Unpacker.decompress(animData));
          }
          anim = new TmdAnimationFile(animData);
        }
      }

      if (anim != null) {
        this.activeModel = new Model124("CombatantModel (" + charKey + ")");
        this.activeModel.uvAdjustments_9d = vramSlots_8005027c[1];
        initModel(this.activeModel, cContainer, anim);
        TmdObjLoader.fromModel("CombatantModel (" + charKey + ")", this.activeModel);
      }
    } catch (final Exception e) {
      e.printStackTrace();
      this.cleanupModel();
    }
  }

  private void clearAnimCache() {
    Arrays.fill(this.currentAdditionAnimCache, null);
    this.currentLoadedAdditionFile = -1;
    this.currentLoadedHitIndex = -1;
    this.currentDemonstrationHit = 0;
  }

  private TmdAnimationFile loadHitAnimation(final int charIndex, final int additionIndex, final int hitIndex) {
    if (hitIndex >= 0 && hitIndex < this.currentAdditionAnimCache.length && this.currentAdditionAnimCache[hitIndex] != null) {
      return this.currentAdditionAnimCache[hitIndex];
    }

    try {
      final CustomAdditionsStorage.CharacterCategory charCat = CustomAdditionsStorage.CHARACTERS.get(charIndex);
      if (additionIndex >= charCat.additions.size()) {
        return null;
      }
      final CustomAdditionsStorage.AdditionDefinition def = charCat.additions.get(additionIndex);
      final RegistryId regId = new RegistryId(def.id);
      if (!REGISTRIES.additions.hasEntry(regId)) {
        return null;
      }
      final Addition addition = REGISTRIES.additions.getEntry(regId).get();
      if (!(addition instanceof RetailAddition retail)) {
        return null;
      }

      final String animPath = "SECT/DRGN0.BIN/" + retail.additionFile + "/" + (16 + hitIndex);
      if (Loader.exists(animPath)) {
        FileData data = Loader.loadFileSync(animPath);
        if (data.size() > 0) {
          if (data.readInt(0x4) == 0x1a45_5042) {
            data = new FileData(Unpacker.decompress(data));
          }
          final TmdAnimationFile anim = new TmdAnimationFile(data);
          if (hitIndex >= 0 && hitIndex < this.currentAdditionAnimCache.length) {
            this.currentAdditionAnimCache[hitIndex] = anim;
          }
          return anim;
        }
      }
    } catch (final Exception ignored) {
    }
    return null;
  }

  private void updateModelAnimation() {
    if (this.activeModel == null) {
      return;
    }

    try {
      final CustomAdditionsStorage.CharacterCategory charCat = CustomAdditionsStorage.CHARACTERS.get(this.selectedCharIndex);
      if (this.selectedAdditionIndex >= charCat.additions.size()) {
        return;
      }
      final CustomAdditionsStorage.AdditionDefinition def = charCat.additions.get(this.selectedAdditionIndex);
      final RegistryId regId = new RegistryId(def.id);
      if (!REGISTRIES.additions.hasEntry(regId)) {
        return;
      }
      final Addition addition = REGISTRIES.additions.getEntry(regId).get();
      if (!(addition instanceof RetailAddition retail)) {
        return;
      }

      if (this.focusArea == 2) {
        // In hits panel: demonstrate ONLY the hit currently hovered
        final int hitToPreview = this.selectedHitIndex;
        if (retail.additionFile != this.currentLoadedAdditionFile || hitToPreview != this.currentLoadedHitIndex) {
          final TmdAnimationFile anim = this.loadHitAnimation(this.selectedCharIndex, this.selectedAdditionIndex, hitToPreview);
          if (anim != null) {
            loadModelStandardAnimation(this.activeModel, anim);
            this.activeModel.animationState_9c = 1;
            this.currentLoadedAdditionFile = retail.additionFile;
            this.currentLoadedHitIndex = hitToPreview;
          }
        }
        this.previewHitLabel.setText(new RawText("[ HIT " + (hitToPreview + 1) + " OF " + def.hitCount + " ]"));
        this.previewHitLabel.getFontOptions().colour(TextColour.CYAN);
      } else {
        // In additions panel: demonstrate WHOLE addition
        if (retail.additionFile != this.currentLoadedAdditionFile) {
          this.currentDemonstrationHit = 0;
          final TmdAnimationFile anim = this.loadHitAnimation(this.selectedCharIndex, this.selectedAdditionIndex, 0);
          if (anim != null) {
            loadModelStandardAnimation(this.activeModel, anim);
            this.activeModel.animationState_9c = 1;
            this.currentLoadedAdditionFile = retail.additionFile;
            this.currentLoadedHitIndex = 0;
          }
        }
        this.previewHitLabel.setText(new RawText("[ COMBO " + (this.currentDemonstrationHit + 1) + "/" + (def.hitCount + 1) + " ]"));
        this.previewHitLabel.getFontOptions().colour(TextColour.LIME);
      }
    } catch (final Exception ignored) {
    }
  }

  private void cleanupModel() {
    if (this.activeModel != null) {
      try {
        this.activeModel.deleteModelParts();
      } catch (final Exception ignored) {
      }
      this.activeModel = null;
    }
    if (this.characterGpu != null) {
      try {
        if (this.characterGpu.vramTexture15 != null) {
          this.characterGpu.vramTexture15.delete();
        }
      } catch (final Exception ignored) {
      }
      this.characterGpu = null;
    }
    this.clearAnimCache();
  }

  private void updateHitsList() {
    final CustomAdditionsStorage.CharacterCategory character = CustomAdditionsStorage.CHARACTERS.get(this.selectedCharIndex);
    if (this.selectedAdditionIndex >= character.additions.size()) {
      return;
    }
    final CustomAdditionsStorage.AdditionDefinition addition = character.additions.get(this.selectedAdditionIndex);
    final List<AdditionButtonType> buttons = CustomAdditionsStorage.getCustomButtons(addition.id);

    // Truncate addition name if needed so it stays cleanly in 112px
    String addName = addition.displayName.toUpperCase();
    if (addName.length() > 14) {
      addName = addName.substring(0, 14);
    }
    this.hitsHeader.setText(new RawText(addName));
    this.hitsSubHeader.setText(new RawText("(" + addition.hitCount + " HITS)"));

    for (int i = 0; i < MAX_HIT_SLOTS; i++) {
      if (i < addition.hitCount) {
        this.hitLabels[i].show();
        this.hitButtons[i].show();

        AdditionButtonType currentBtn = AdditionButtonType.CROSS;
        if (buttons != null && i < buttons.size()) {
          currentBtn = buttons.get(i);
        }
        this.hitButtons[i].setText(new RawText(currentBtn.name()));
        this.applyButtonColour(this.hitButtons[i], currentBtn);
      } else {
        this.hitLabels[i].hide();
        this.hitButtons[i].hide();
      }
    }

    if (this.selectedHitIndex >= addition.hitCount) {
      this.selectedHitIndex = 0;
    }
  }

  private void applyButtonColour(final Button button, final AdditionButtonType type) {
    switch (type) {
      case SQUARE:
        button.getFontOptions().colour(TextColour.PURPLE);
        break;
      case TRIANGLE:
        button.getFontOptions().colour(TextColour.GREEN);
        break;
      case CROSS:
      default:
        button.getFontOptions().colour(TextColour.CYAN);
        break;
    }
  }

  private void setHighlightedHit(final AdditionButtonType type) {
    final CustomAdditionsStorage.CharacterCategory character = CustomAdditionsStorage.CHARACTERS.get(this.selectedCharIndex);
    final CustomAdditionsStorage.AdditionDefinition addition = character.additions.get(this.selectedAdditionIndex);
    List<AdditionButtonType> buttons = CustomAdditionsStorage.getCustomButtons(addition.id);
    if (buttons == null) {
      buttons = new ArrayList<>();
    }
    while (buttons.size() < addition.hitCount) {
      buttons.add(AdditionButtonType.CROSS);
    }

    buttons.set(this.selectedHitIndex, type);
    CustomAdditionsStorage.setCustomButtons(addition.id, buttons);

    playMenuSound(2);
    this.updateHitsList();
    this.refreshVisuals();
  }

  private void cycleHighlightedHit(final int direction) {
    final CustomAdditionsStorage.CharacterCategory character = CustomAdditionsStorage.CHARACTERS.get(this.selectedCharIndex);
    final CustomAdditionsStorage.AdditionDefinition addition = character.additions.get(this.selectedAdditionIndex);
    List<AdditionButtonType> buttons = CustomAdditionsStorage.getCustomButtons(addition.id);
    AdditionButtonType current = AdditionButtonType.CROSS;
    if (buttons != null && this.selectedHitIndex < buttons.size()) {
      current = buttons.get(this.selectedHitIndex);
    }
    final AdditionButtonType next = AdditionButtonType.cycleCustomButton(current, direction);
    this.setHighlightedHit(next);
  }

  private void refreshVisuals() {
    final CustomAdditionsStorage.CharacterCategory character = CustomAdditionsStorage.CHARACTERS.get(this.selectedCharIndex);
    final CustomAdditionsStorage.AdditionDefinition addition = character.additions.get(this.selectedAdditionIndex);

    // Style addition buttons (Column 1)
    for (int i = 0; i < character.additions.size(); i++) {
      final CustomAdditionsStorage.AdditionDefinition add = character.additions.get(i);
      if (i == this.selectedAdditionIndex) {
        if (this.focusArea == 1) {
          this.additionButtons[i].setText(new RawText("> " + add.displayName));
          this.additionButtons[i].getFontOptions().colour(TextColour.GOLD);
        } else {
          this.additionButtons[i].setText(new RawText("* " + add.displayName));
          this.additionButtons[i].getFontOptions().colour(TextColour.YELLOW);
        }
      } else {
        this.additionButtons[i].setText(new RawText("  " + add.displayName));
        this.additionButtons[i].getFontOptions().colour(TextColour.WHITE);
      }
    }

    // Style hit rows (Column 2)
    final List<AdditionButtonType> buttons = CustomAdditionsStorage.getCustomButtons(addition.id);
    for (int i = 0; i < addition.hitCount; i++) {
      AdditionButtonType btnType = AdditionButtonType.CROSS;
      if (buttons != null && i < buttons.size()) {
        btnType = buttons.get(i);
      }

      if (this.focusArea == 2 && i == this.selectedHitIndex) {
        this.hitLabels[i].setText(new RawText("> Hit " + (i + 1) + ":"));
        this.hitLabels[i].getFontOptions().colour(TextColour.GOLD);
        this.hitButtons[i].setText(new RawText("> " + btnType.name() + " <"));
      } else {
        this.hitLabels[i].setText(new RawText("  Hit " + (i + 1) + ":"));
        this.hitLabels[i].getFontOptions().colour(TextColour.WHITE);
        this.hitButtons[i].setText(new RawText(btnType.name()));
      }
      this.applyButtonColour(this.hitButtons[i], btnType);
    }

    // Style save button
    if (this.focusArea == 3) {
      this.saveButton.setText(new RawText("> SAVE CONFIGURATION <"));
      this.saveButton.getFontOptions().colour(TextColour.GOLD);
    } else {
      this.saveButton.setText(new RawText("SAVE CONFIGURATION"));
      this.saveButton.getFontOptions().colour(TextColour.LIME);
    }

    // Synchronize 3D model animation with current view mode
    this.updateModelAnimation();
  }

  private void save() {
    CustomAdditionsStorage.save();
    playMenuSound(2);
    this.statusLabel.setText(new RawText("Custom addition mappings saved successfully!"));
    // After saving, return focus to additions list
    this.focusArea = 1;
    this.refreshVisuals();
  }

  private void back() {
    playMenuSound(3);
    this.cleanupModel();
    this.unload.run();
  }

  private void handleBackAction() {
    if (this.focusArea == 2) {
      this.focusArea = 1;
      playMenuSound(3);
      this.clearAnimCache();
      this.refreshVisuals();
    } else {
      this.back();
    }
  }

  @Override
  protected InputPropagation inputActionPressed(final InputAction action, final boolean repeat) {
    // Global Back action (Circle / O / Escape / Back)
    if (action == INPUT_ACTION_MENU_BACK.get() && !repeat) {
      this.handleBackAction();
      return InputPropagation.HANDLED;
    }

    // === BUMPER INPUTS (L1/R1 or LB/RB) to cycle characters ===
    if (action == INPUT_ACTION_MENU_PAGE_UP.get()) {
      this.switchCharacter(-1);
      return InputPropagation.HANDLED;
    }
    if (action == INPUT_ACTION_MENU_PAGE_DOWN.get()) {
      this.switchCharacter(1);
      return InputPropagation.HANDLED;
    }

    // === HITS PANEL FOCUS (focusArea == 2) ===
    if (this.focusArea == 2) {
      // Up / Down navigate between hits ONLY (no side-to-side navigation)
      if (action == INPUT_ACTION_MENU_UP.get()) {
        if (this.selectedHitIndex > 0) {
          this.selectedHitIndex--;
          playMenuSound(1);
          this.refreshVisuals();
        }
        return InputPropagation.HANDLED;
      }

      if (action == INPUT_ACTION_MENU_DOWN.get()) {
        final CustomAdditionsStorage.CharacterCategory character = CustomAdditionsStorage.CHARACTERS.get(this.selectedCharIndex);
        final CustomAdditionsStorage.AdditionDefinition addition = character.additions.get(this.selectedAdditionIndex);
        if (this.selectedHitIndex < addition.hitCount - 1) {
          this.selectedHitIndex++;
          playMenuSound(1);
          this.refreshVisuals();
        }
        return InputPropagation.HANDLED;
      }

      // Left / Right are blocked inside the hits menu
      if (action == INPUT_ACTION_MENU_LEFT.get() || action == INPUT_ACTION_MENU_RIGHT.get()) {
        return InputPropagation.HANDLED;
      }

      // Cross (X) -> sets hit to CROSS
      if (action == INPUT_ACTION_MENU_CONFIRM.get() && !repeat) {
        this.setHighlightedHit(AdditionButtonType.CROSS);
        return InputPropagation.HANDLED;
      }
    }

    // === ADDITIONS LIST FOCUS (focusArea == 1) ===
    if (this.focusArea == 1) {
      if (action == INPUT_ACTION_MENU_UP.get()) {
        if (this.selectedAdditionIndex > 0) {
          this.selectedAdditionIndex--;
          playMenuSound(1);
          this.clearAnimCache();
          this.updateHitsList();
          this.refreshVisuals();
        }
        return InputPropagation.HANDLED;
      }

      if (action == INPUT_ACTION_MENU_DOWN.get()) {
        final CustomAdditionsStorage.CharacterCategory character = CustomAdditionsStorage.CHARACTERS.get(this.selectedCharIndex);
        if (this.selectedAdditionIndex < character.additions.size() - 1) {
          this.selectedAdditionIndex++;
          playMenuSound(1);
          this.clearAnimCache();
          this.updateHitsList();
          this.refreshVisuals();
        } else {
          // Move to Save button
          this.focusArea = 3;
          playMenuSound(1);
          this.refreshVisuals();
        }
        return InputPropagation.HANDLED;
      }

      // Confirm (Cross) on addition -> enters hits panel
      if (action == INPUT_ACTION_MENU_CONFIRM.get() && !repeat) {
        this.focusArea = 2;
        this.selectedHitIndex = 0;
        playMenuSound(2);
        this.refreshVisuals();
        return InputPropagation.HANDLED;
      }

      // Left / Right also cycle characters
      if (action == INPUT_ACTION_MENU_LEFT.get() && !repeat) {
        this.switchCharacter(-1);
        return InputPropagation.HANDLED;
      }
      if (action == INPUT_ACTION_MENU_RIGHT.get() && !repeat) {
        this.switchCharacter(1);
        return InputPropagation.HANDLED;
      }
    }

    // === SAVE BUTTON FOCUS (focusArea == 3) ===
    if (this.focusArea == 3) {
      if (action == INPUT_ACTION_MENU_UP.get()) {
        final CustomAdditionsStorage.CharacterCategory character = CustomAdditionsStorage.CHARACTERS.get(this.selectedCharIndex);
        this.focusArea = 1;
        this.selectedAdditionIndex = Math.max(0, character.additions.size() - 1);
        playMenuSound(1);
        this.clearAnimCache();
        this.updateHitsList();
        this.refreshVisuals();
        return InputPropagation.HANDLED;
      }

      if (action == INPUT_ACTION_MENU_CONFIRM.get() && !repeat) {
        this.save();
        return InputPropagation.HANDLED;
      }
    }

    return super.inputActionPressed(action, repeat);
  }

  @Override
  protected InputPropagation buttonPress(final InputButton button, final boolean repeat) {
    // Physical bumpers
    if (button == InputButton.LEFT_BUMPER && !repeat) {
      this.switchCharacter(-1);
      return InputPropagation.HANDLED;
    }
    if (button == InputButton.RIGHT_BUMPER && !repeat) {
      this.switchCharacter(1);
      return InputPropagation.HANDLED;
    }

    // Direct mapping in hits panel
    if (this.focusArea == 2 && !repeat) {
      if (button == InputButton.A) { // Cross
        this.setHighlightedHit(AdditionButtonType.CROSS);
        return InputPropagation.HANDLED;
      }
      if (button == InputButton.X) { // Square
        this.setHighlightedHit(AdditionButtonType.SQUARE);
        return InputPropagation.HANDLED;
      }
      if (button == InputButton.Y) { // Triangle
        this.setHighlightedHit(AdditionButtonType.TRIANGLE);
        return InputPropagation.HANDLED;
      }
    }

    return super.buttonPress(button, repeat);
  }

  @Override
  protected InputPropagation keyPress(final InputKey key, final InputKey scancode, final Set<InputMod> mods, final boolean repeat) {
    // Q / E for bumper cycling on keyboard
    if (key == InputKey.Q && !repeat) {
      this.switchCharacter(-1);
      return InputPropagation.HANDLED;
    }
    if (key == InputKey.E && !repeat) {
      this.switchCharacter(1);
      return InputPropagation.HANDLED;
    }

    if (this.focusArea == 2 && !repeat) {
      // Keyboard 1 = Cross, 2 = Square, 3 = Triangle
      if (key == InputKey.NUM_1 || key == InputKey.KP_1) {
        this.setHighlightedHit(AdditionButtonType.CROSS);
        return InputPropagation.HANDLED;
      }
      if (key == InputKey.NUM_2 || key == InputKey.KP_2 || key == InputKey.S) {
        this.setHighlightedHit(AdditionButtonType.SQUARE);
        return InputPropagation.HANDLED;
      }
      if (key == InputKey.NUM_3 || key == InputKey.KP_3 || key == InputKey.T) {
        this.setHighlightedHit(AdditionButtonType.TRIANGLE);
        return InputPropagation.HANDLED;
      }
    }

    return super.keyPress(key, scancode, mods, repeat);
  }
}
