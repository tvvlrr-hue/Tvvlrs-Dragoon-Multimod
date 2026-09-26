package legend.multimod;

import legend.core.MathHelper;
import legend.core.gpu.Gpu;
import legend.core.gte.MV;
import legend.core.gte.ModelPart10;
import legend.core.lang.I18nText;
import legend.core.lang.RawText;
import legend.core.platform.input.InputAction;
import legend.core.gpu.Rect4i;
import legend.core.renderer.DepthComparator;
import legend.core.renderer.QueuedModelBattleTmd;
import legend.game.inventory.screens.HorizontalAlign;
import legend.game.inventory.screens.InputPropagation;
import legend.game.inventory.screens.MenuScreen;
import legend.game.inventory.screens.TextColour;
import legend.game.inventory.screens.controls.Background;
import legend.game.inventory.screens.controls.Button;
import legend.game.inventory.screens.controls.Dropdown;
import legend.game.inventory.screens.controls.Label;
import legend.game.tim.Tim;
import legend.game.tmd.TmdObjLoader;
import legend.game.types.CContainer;
import legend.game.types.Model124;
import legend.game.types.TmdAnimationFile;
import legend.game.unpacker.FileData;
import legend.game.unpacker.Loader;
import legend.game.unpacker.Unpacker;
import org.joml.Matrix3f;
import org.joml.Vector3f;

import java.util.List;

import static legend.core.GameEngine.PLATFORM;
import static legend.core.GameEngine.RENDERER;
import static legend.game.FullScreenEffects.startFadeEffect;
import static legend.game.Graphics.GsGetLw;
import static legend.game.Graphics.getProjectionPlaneDistance;
import static legend.game.Graphics.setProjectionPlaneDistance;
import static legend.game.Menus.deallocateRenderables;
import static legend.game.Models.animateModel;
import static legend.game.Models.applyModelRotationAndScale;
import static legend.game.Models.initModel;
import static legend.game.Models.loadModelStandardAnimation;
import static legend.game.Models.vramSlots_8005027c;
import static legend.game.combat.Battle.combatantTimRects_800fa6e0;
import static legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_BACK;
import static legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_CONFIRM;
import static legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_DOWN;
import static legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_END;
import static legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_HOME;
import static legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_LEFT;
import static legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_PAGE_DOWN;
import static legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_PAGE_UP;
import static legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_RIGHT;
import static legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_UP;
import static legend.game.sound.Audio.playMenuSound;

public class DanceModifierConfigScreen extends MenuScreen {
  private final Runnable unload;

  private int selectedCharIndex = 0;
  private int focusArea = 1; // 1 = Dropdown / Dance, 2 = Save Button

  // Header controls
  private final Label titleLabel;
  private final Button prevCharButton;
  private final Label charLabel;
  private final Button nextCharButton;

  // Dances Panel (Left: x = 12..174)
  private final Label dancesHeader;
  private final Button defaultDanceButton;
  private final Dropdown<DanceModifierStorage.CharacterEntry> sourceDropdown;

  // 3D Preview Panel (Right: x = 180..356)
  private final Label previewHeader;
  private final Label previewDanceLabel;
  private final Label previewRotateHintLabel;
  private Model124 activeModel;
  private Gpu characterGpu;
  private String currentLoadedSource = null;
  private final MV tempLw = new MV();
  private float previewRotationY = 0.0f;
  private float baseRootX = 0.0f;
  private float baseRootZ = 0.0f;
  private boolean baseRootRecorded = false;

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

  // Bottom action
  private final Button saveButton;
  private final Label statusLabel;
  private final float originalProjectionPlaneDistance;

  public DanceModifierConfigScreen(final Runnable unload) {
    this.originalProjectionPlaneDistance = getProjectionPlaneDistance();
    setProjectionPlaneDistance(1.0f);

    deallocateRenderables(0xff);
    startFadeEffect(2, 10);

    this.unload = unload;
    this.addControl(new Background());

    final int screenWidth = this.getWidth(); // 368

    // Screen Title (y = 6)
    this.titleLabel = this.addControl(new Label(new RawText("DANCE MODIFIER")));
    this.titleLabel.getFontOptions().horizontalAlign(HorizontalAlign.CENTRE);
    this.titleLabel.getFontOptions().colour(TextColour.GOLD).shadowColour(TextColour.BLACK);
    this.titleLabel.setPos(0, 6);
    this.titleLabel.setWidth(screenWidth);

    // Character Selector (y = 20)
    this.prevCharButton = this.addControl(new Button(new RawText("[L1]")));
    this.prevCharButton.setPos(12, 20);
    this.prevCharButton.setSize(38, 14);
    this.prevCharButton.getFontOptions().colour(TextColour.CYAN);
    this.prevCharButton.onPressed(() -> this.switchCharacter(-1));

    this.charLabel = this.addControl(new Label(new RawText("< Dart >")));
    this.charLabel.getFontOptions().horizontalAlign(HorizontalAlign.CENTRE);
    this.charLabel.getFontOptions().colour(TextColour.YELLOW).shadowColour(TextColour.BLACK);
    this.charLabel.setPos(54, 21);
    this.charLabel.setWidth(screenWidth - 108);

    this.nextCharButton = this.addControl(new Button(new RawText("[R1]")));
    this.nextCharButton.setPos(screenWidth - 50, 20);
    this.nextCharButton.setSize(38, 14);
    this.nextCharButton.getFontOptions().colour(TextColour.CYAN);
    this.nextCharButton.onPressed(() -> this.switchCharacter(1));

    // Left Column: Dance & Source Dropdown
    this.dancesHeader = this.addControl(new Label(new RawText("[ VICTORY DANCES ]")));
    this.dancesHeader.getFontOptions().colour(TextColour.GOLD).shadowColour(TextColour.BLACK);
    this.dancesHeader.setPos(12, 42);
    this.dancesHeader.setWidth(160);

    this.defaultDanceButton = this.addControl(new Button(new RawText("[x] Default Dance:")));
    this.defaultDanceButton.setPos(12, 58);
    this.defaultDanceButton.setSize(140, 16);
    this.defaultDanceButton.getFontOptions().colour(TextColour.LIME);
    this.defaultDanceButton.onPressed(() -> {
      this.focusArea = 1;
      playMenuSound(2);
      this.refreshVisuals();
    });

    this.sourceDropdown = this.addControl(new Dropdown<>((i, entry) -> new RawText(entry.displayName)));
    this.sourceDropdown.setPos(18, 76);
    this.sourceDropdown.setSize(134, 16);
    for (final DanceModifierStorage.CharacterEntry entry : DanceModifierStorage.CHARACTERS) {
      this.sourceDropdown.addOption(entry);
    }
    this.sourceDropdown.onSelection(index -> {
      final DanceModifierStorage.CharacterEntry chosen = this.sourceDropdown.getOption(index);
      if (chosen != null) {
        final String charKey = this.getCharacterKey(this.selectedCharIndex);
        DanceModifierStorage.setSourceCharacter(charKey, chosen.key);
        this.updateModelAnimation();
        this.refreshVisuals();
      }
    });

    // Right Column: 3D Preview (x = 180..356)
    this.previewHeader = this.addControl(new Label(new RawText("[ PREVIEW ]")));
    this.previewHeader.getFontOptions().horizontalAlign(HorizontalAlign.CENTRE);
    this.previewHeader.getFontOptions().colour(TextColour.GOLD).shadowColour(TextColour.BLACK);
    this.previewHeader.setPos(180, 42);
    this.previewHeader.setWidth(176);

    this.previewDanceLabel = this.addControl(new Label(new RawText("[ DART DANCE ]")));
    this.previewDanceLabel.getFontOptions().horizontalAlign(HorizontalAlign.CENTRE);
    this.previewDanceLabel.getFontOptions().colour(TextColour.LIME).shadowColour(TextColour.BLACK);
    this.previewDanceLabel.setPos(180, 56);
    this.previewDanceLabel.setWidth(176);

    this.previewRotateHintLabel = this.addControl(new Label(new RawText("[LT / RT] Rotate")));
    this.previewRotateHintLabel.getFontOptions().horizontalAlign(HorizontalAlign.CENTRE);
    this.previewRotateHintLabel.getFontOptions().colour(TextColour.GREY).shadowColour(TextColour.BLACK);
    this.previewRotateHintLabel.setPos(180, 178);
    this.previewRotateHintLabel.setWidth(176);

    // Save Button (y = 196)
    this.saveButton = this.addControl(new Button(new RawText("SAVE CONFIGURATION")));
    this.saveButton.setPos((screenWidth - 170) / 2, 196);
    this.saveButton.setSize(170, 14);
    this.saveButton.getFontOptions().colour(TextColour.LIME);
    this.saveButton.onPressed(this::save);

    // Status message label (y = 214)
    this.statusLabel = this.addControl(new Label(new RawText("")));
    this.statusLabel.getFontOptions().horizontalAlign(HorizontalAlign.CENTRE);
    this.statusLabel.getFontOptions().colour(TextColour.LIME).shadowColour(TextColour.BLACK);
    this.statusLabel.setPos(0, 214);
    this.statusLabel.setWidth(screenWidth);

    // Initial load
    this.loadCharacter(0);

    // Back hotkey
    this.addHotkey(new I18nText("lod_core.ui.options_category.back"), INPUT_ACTION_MENU_BACK, this::handleBackAction);
  }

  @Override
  protected void delete() {
    setProjectionPlaneDistance(this.originalProjectionPlaneDistance);
    this.cleanupModel();
    super.delete();
  }

  @Override
  protected void render() {
    setProjectionPlaneDistance(1.0f);
    if (this.activeModel != null) {
      // Rotation with triggers
      float lt = 0.0f;
      float rt = 0.0f;
      try {
        lt = Math.max(lt, PLATFORM.getAxis(INPUT_ACTION_MENU_HOME.get()));
        rt = Math.max(rt, PLATFORM.getAxis(INPUT_ACTION_MENU_END.get()));
      } catch (final Throwable ignored) {}

      final float rotSpeed = 0.05f;
      if (lt > 0.05f) this.previewRotationY -= lt * rotSpeed;
      if (rt > 0.05f) this.previewRotationY += rt * rotSpeed;

      if (this.previewRotationY > MathHelper.PI) {
        this.previewRotationY -= MathHelper.TWO_PI;
      } else if (this.previewRotationY < -MathHelper.PI) {
        this.previewRotationY += MathHelper.TWO_PI;
      }

      // 5 render frames per keyframe matches the relaxed 1.25x scaling (smooth & not too fast)
      animateModel(this.activeModel, 5);

      final float previewCenterX = 270.0f;
      final float previewCenterY = 160.0f;
      final float previewCenterZ = 200.0f;
      final float scale = this.getCharacterScale(this.selectedCharIndex);

      this.activeModel.coord2_14.flg = 0;
      if (this.activeModel.modelParts_00 != null) {
        for (final ModelPart10 part : this.activeModel.modelParts_00) {
          if (part != null && part.coord2_04 != null) {
            part.coord2_04.flg = 0;
          }
        }
      }

      this.activeModel.coord2_14.transforms.scale.set(scale, scale, scale);
      this.activeModel.coord2_14.transforms.rotate.set(0.05f, this.previewRotationY, 0.0f);
      applyModelRotationAndScale(this.activeModel);

      // Root centering
      float rootOffsetX = 0.0f;
      float rootOffsetZ = 0.0f;
      if (this.baseRootRecorded) {
        final Vector3f disp = new Vector3f(this.baseRootX, 0.0f, this.baseRootZ);
        final Vector3f worldDisp = new Vector3f();
        disp.mul(this.activeModel.coord2_14.coord, worldDisp);
        rootOffsetX = worldDisp.x;
        rootOffsetZ = worldDisp.z;
      }

      this.activeModel.coord2_14.coord.transfer.set(previewCenterX - rootOffsetX, previewCenterY, previewCenterZ - rootOffsetZ);

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

  private float getCharacterScale(final int charIndex) {
    final String key = this.getCharacterKey(charIndex);
    return switch (key) {
      case "meru" -> 0.08f;
      case "kongol" -> 0.055f;
      default -> 0.07f;
    };
  }

  private String getCharacterKey(final int charIndex) {
    return DanceModifierStorage.CHARACTERS.get(charIndex).key;
  }

  private void switchCharacter(final int direction) {
    playMenuSound(1);
    final int count = DanceModifierStorage.CHARACTERS.size();
    this.selectedCharIndex = (this.selectedCharIndex + direction % count + count) % count;
    this.loadCharacter(this.selectedCharIndex);
  }

  private void loadCharacter(final int charIndex) {
    final DanceModifierStorage.CharacterEntry entry = DanceModifierStorage.CHARACTERS.get(charIndex);
    this.charLabel.setText(new RawText("< " + entry.displayName.toUpperCase() + " >"));

    // Find configured source character
    final String currentSource = DanceModifierStorage.getSourceCharacter(entry.key);
    for (int i = 0; i < DanceModifierStorage.CHARACTERS.size(); i++) {
      if (DanceModifierStorage.CHARACTERS.get(i).key.equalsIgnoreCase(currentSource)) {
        this.sourceDropdown.setSelectedIndex(i);
        break;
      }
    }

    this.loadCharacterModel(charIndex);
    this.refreshVisuals();
  }

  private void loadCharacterModel(final int charIndex) {
    this.cleanupModel();
    final String charKey = this.getCharacterKey(charIndex);
    final String sourceKey = DanceModifierStorage.getSourceCharacter(charKey);

    try {
      final String modelPath = "characters/" + charKey + "/models/combat/32";
      if (!Loader.exists(modelPath)) return;

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

        final Rect4i combatantTimRect = combatantTimRects_800fa6e0[1];
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

      final TmdAnimationFile anim = ProceduralDanceSynthesizer.getDanceAnimation(charKey, DanceType.DEFAULT, sourceKey);

      if (anim != null) {
        this.activeModel = new Model124("CombatantModel (" + charKey + ")");
        this.activeModel.uvAdjustments_9d = vramSlots_8005027c[1];
        initModel(this.activeModel, cContainer, anim);
        TmdObjLoader.fromModel("CombatantModel (" + charKey + ")", this.activeModel);
        this.currentLoadedSource = sourceKey;
        this.previewRotationY = 0.0f;

        if (this.activeModel.modelParts_00 != null
            && this.activeModel.modelParts_00.length > 0
            && this.activeModel.modelParts_00[0] != null
            && this.activeModel.modelParts_00[0].coord2_04 != null) {
          this.baseRootX = this.activeModel.modelParts_00[0].coord2_04.coord.transfer.x;
          this.baseRootZ = this.activeModel.modelParts_00[0].coord2_04.coord.transfer.z;
          this.baseRootRecorded = true;
        }
      }
    } catch (final Throwable t) {
      t.printStackTrace();
      this.cleanupModel();
    }
  }

  private void updateModelAnimation() {
    if (this.activeModel == null) return;
    final String charKey = this.getCharacterKey(this.selectedCharIndex);
    final String sourceKey = DanceModifierStorage.getSourceCharacter(charKey);
    if (sourceKey.equalsIgnoreCase(this.currentLoadedSource)) return;

    final TmdAnimationFile anim = ProceduralDanceSynthesizer.getDanceAnimation(charKey, DanceType.DEFAULT, sourceKey);
    if (anim != null) {
      loadModelStandardAnimation(this.activeModel, anim);
      this.activeModel.animationState_9c = 1;
      this.currentLoadedSource = sourceKey;
      this.previewRotationY = 0.0f;

      if (this.activeModel.modelParts_00 != null
          && this.activeModel.modelParts_00.length > 0
          && this.activeModel.modelParts_00[0] != null
          && this.activeModel.modelParts_00[0].coord2_04 != null) {
        this.baseRootX = this.activeModel.modelParts_00[0].coord2_04.coord.transfer.x;
        this.baseRootZ = this.activeModel.modelParts_00[0].coord2_04.coord.transfer.z;
        this.baseRootRecorded = true;
      }
    }
  }

  private void cleanupModel() {
    this.baseRootRecorded = false;
    if (this.activeModel != null) {
      try {
        this.activeModel.deleteModelParts();
      } catch (final Throwable ignored) {}
      this.activeModel = null;
    }
    if (this.characterGpu != null) {
      try {
        if (this.characterGpu.vramTexture15 != null) {
          this.characterGpu.vramTexture15.delete();
        }
      } catch (final Throwable ignored) {}
      this.characterGpu = null;
    }
    this.currentLoadedSource = null;
  }

  private void refreshVisuals() {
    final String charKey = this.getCharacterKey(this.selectedCharIndex);
    final String sourceKey = DanceModifierStorage.getSourceCharacter(charKey);

    DanceModifierStorage.CharacterEntry sourceEntry = null;
    for (final DanceModifierStorage.CharacterEntry e : DanceModifierStorage.CHARACTERS) {
      if (e.key.equalsIgnoreCase(sourceKey)) {
        sourceEntry = e;
        break;
      }
    }
    final String sourceName = (sourceEntry != null) ? sourceEntry.displayName : "Default";

    if (this.focusArea == 1) {
      this.defaultDanceButton.getFontOptions().colour(TextColour.GOLD);
    } else {
      this.defaultDanceButton.getFontOptions().colour(TextColour.LIME);
    }

    this.previewDanceLabel.setText(new RawText("[ " + sourceName.toUpperCase() + " DANCE ]"));
    this.previewDanceLabel.getFontOptions().colour(TextColour.CYAN);

    if (this.focusArea == 2) {
      this.saveButton.setText(new RawText("> SAVE CONFIGURATION <"));
      this.saveButton.getFontOptions().colour(TextColour.GOLD);
    } else {
      this.saveButton.setText(new RawText("SAVE CONFIGURATION"));
      this.saveButton.getFontOptions().colour(TextColour.LIME);
    }

    this.updateModelAnimation();
  }

  private void save() {
    DanceModifierStorage.save();
    playMenuSound(2);
    this.statusLabel.setText(new RawText("Dance configuration saved successfully!"));
    this.focusArea = 1;
    this.refreshVisuals();
  }

  private void handleBackAction() {
    playMenuSound(3);
    setProjectionPlaneDistance(this.originalProjectionPlaneDistance);
    this.cleanupModel();
    this.unload.run();
  }

  @Override
  protected InputPropagation inputActionPressed(final InputAction action, final boolean repeat) {
    if (action == INPUT_ACTION_MENU_BACK.get() && !repeat) {
      this.handleBackAction();
      return InputPropagation.HANDLED;
    }

    // Bumpers (L1/R1) switch characters
    if (action == INPUT_ACTION_MENU_PAGE_UP.get()) {
      this.switchCharacter(-1);
      return InputPropagation.HANDLED;
    }
    if (action == INPUT_ACTION_MENU_PAGE_DOWN.get()) {
      this.switchCharacter(1);
      return InputPropagation.HANDLED;
    }

    if (this.focusArea == 1) {
      if (action == INPUT_ACTION_MENU_DOWN.get()) {
        this.focusArea = 2; // Jump to Save button
        playMenuSound(1);
        this.refreshVisuals();
        return InputPropagation.HANDLED;
      }

      if (action == INPUT_ACTION_MENU_LEFT.get()) {
        // Cycle source character left
        final int cur = this.sourceDropdown.getSelectedIndex();
        final int next = (cur - 1 + DanceModifierStorage.CHARACTERS.size()) % DanceModifierStorage.CHARACTERS.size();
        this.sourceDropdown.setSelectedIndex(next);
        final DanceModifierStorage.CharacterEntry chosen = this.sourceDropdown.getOption(next);
        final String charKey = this.getCharacterKey(this.selectedCharIndex);
        DanceModifierStorage.setSourceCharacter(charKey, chosen.key);
        playMenuSound(1);
        this.updateModelAnimation();
        this.refreshVisuals();
        return InputPropagation.HANDLED;
      }
      if (action == INPUT_ACTION_MENU_RIGHT.get()) {
        // Cycle source character right
        final int cur = this.sourceDropdown.getSelectedIndex();
        final int next = (cur + 1) % DanceModifierStorage.CHARACTERS.size();
        this.sourceDropdown.setSelectedIndex(next);
        final DanceModifierStorage.CharacterEntry chosen = this.sourceDropdown.getOption(next);
        final String charKey = this.getCharacterKey(this.selectedCharIndex);
        DanceModifierStorage.setSourceCharacter(charKey, chosen.key);
        playMenuSound(1);
        this.updateModelAnimation();
        this.refreshVisuals();
        return InputPropagation.HANDLED;
      }

      if (action == INPUT_ACTION_MENU_CONFIRM.get() && !repeat) {
        // Confirm action opens the source character dropdown
        playMenuSound(2);
        return super.inputActionPressed(action, repeat);
      }
    } else if (this.focusArea == 2) {
      if (action == INPUT_ACTION_MENU_UP.get()) {
        this.focusArea = 1;
        playMenuSound(1);
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
}
