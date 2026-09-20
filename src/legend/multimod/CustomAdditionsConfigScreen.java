package legend.multimod;

import legend.core.gte.MV;
import legend.core.gte.ModelPart10;
import legend.core.lang.I18nText;
import legend.core.lang.RawText;
import legend.core.platform.input.InputAction;
import legend.core.platform.input.InputButton;
import legend.core.platform.input.InputKey;
import legend.core.platform.input.InputMod;
import legend.core.renderer.QueuedModelTmd;
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
import org.joml.Vector3f;
import org.legendofdragoon.modloader.registries.RegistryId;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static legend.core.GameEngine.RENDERER;
import static legend.game.FullScreenEffects.startFadeEffect;
import static legend.game.Graphics.GsGetLw;
import static legend.game.Graphics.lightColourMatrix_800c3508;
import static legend.game.Graphics.lightDirectionMatrix_800c34e8;
import static legend.game.Menus.deallocateRenderables;
import static legend.game.Models.animateModel;
import static legend.game.Models.applyModelRotationAndScale;
import static legend.game.Models.initModel;
import static legend.game.Models.loadModelStandardAnimation;
import static legend.core.GameEngine.REGISTRIES;
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

  // Focus Area: 1 = Additions list, 2 = Hits side menu, 3 = Save button
  private int focusArea = 1;

  // Header controls
  private final Label titleLabel;
  private final Button prevCharButton;
  private final Label charLabel;
  private final Button nextCharButton;

  // Column headers
  private final Label additionsHeader;
  private final Label hitsHeader;
  private final Label previewHeader;
  private final Label previewHitLabel;

  // Additions column (left)
  private final Button[] additionButtons = new Button[MAX_ADDITION_SLOTS];

  // Hits column (middle)
  private final Label[] hitLabels = new Label[MAX_HIT_SLOTS];
  private final Button[] hitButtons = new Button[MAX_HIT_SLOTS];
  private final Label hitHintLabel;

  // Bottom action
  private final Button saveButton;
  private final Label statusLabel;

  // 3D Battle Model Preview
  private Model124 activeModel;
  private int currentLoadedAdditionFile = -1;
  private int currentLoadedHitIndex = -1;
  private final MV tempLw = new MV();
  private final Vector3f ambientLight = new Vector3f(1.0f, 1.0f, 1.0f);

  public CustomAdditionsConfigScreen(final Runnable unload) {
    deallocateRenderables(0xff);
    startFadeEffect(2, 10);

    this.unload = unload;
    this.addControl(new Background());

    final int screenWidth = this.getWidth();

    // Screen Title
    this.titleLabel = this.addControl(new Label(new RawText("CUSTOM ADDITIONS CONFIGURATION")));
    this.titleLabel.getFontOptions().horizontalAlign(HorizontalAlign.CENTRE);
    this.titleLabel.getFontOptions().colour(TextColour.GOLD).shadowColour(TextColour.BLACK);
    this.titleLabel.setPos(0, 6);
    this.titleLabel.setWidth(screenWidth);

    // Character Selector with Bumper Indicators
    this.prevCharButton = this.addControl(new Button(new RawText("<")));
    this.prevCharButton.setPos(20, 20);
    this.prevCharButton.setSize(20, 12);
    this.prevCharButton.onPressed(() -> this.switchCharacter(-1));

    this.charLabel = this.addControl(new Label(new RawText("")));
    this.charLabel.getFontOptions().horizontalAlign(HorizontalAlign.CENTRE);
    this.charLabel.getFontOptions().colour(TextColour.YELLOW).shadowColour(TextColour.BLACK);
    this.charLabel.setPos(44, 20);
    this.charLabel.setWidth(screenWidth - 88);

    this.nextCharButton = this.addControl(new Button(new RawText(">")));
    this.nextCharButton.setPos(screenWidth - 40, 20);
    this.nextCharButton.setSize(20, 12);
    this.nextCharButton.onPressed(() -> this.switchCharacter(1));

    // Column Headers (3 columns: Additions, Hits Config, Hit Preview)
    this.additionsHeader = this.addControl(new Label(new RawText("ADDITIONS")));
    this.additionsHeader.getFontOptions().colour(TextColour.GOLD).shadowColour(TextColour.BLACK);
    this.additionsHeader.setPos(12, 35);
    this.additionsHeader.setWidth(118);

    this.hitsHeader = this.addControl(new Label(new RawText("HITS CONFIG")));
    this.hitsHeader.getFontOptions().colour(TextColour.GOLD).shadowColour(TextColour.BLACK);
    this.hitsHeader.setPos(136, 35);
    this.hitsHeader.setWidth(118);

    this.previewHeader = this.addControl(new Label(new RawText("HIT PREVIEW")));
    this.previewHeader.getFontOptions().colour(TextColour.GOLD).shadowColour(TextColour.BLACK);
    this.previewHeader.setPos(260, 35);
    this.previewHeader.setWidth(116);

    this.previewHitLabel = this.addControl(new Label(new RawText("")));
    this.previewHitLabel.getFontOptions().colour(TextColour.YELLOW).shadowColour(TextColour.BLACK);
    this.previewHitLabel.setPos(260, 47);
    this.previewHitLabel.setWidth(116);

    // Additions Buttons (Left column)
    for (int i = 0; i < MAX_ADDITION_SLOTS; i++) {
      final int slot = i;
      final Button btn = new Button(new RawText(""));
      btn.setPos(12, 50 + i * 14);
      btn.setSize(118, 12);
      btn.onPressed(() -> {
        this.selectedAdditionIndex = slot;
        // Clicking an addition enters its hits side menu
        this.focusArea = 2;
        this.selectedHitIndex = 0;
        this.updateHitsList();
        this.refreshVisuals();
        playMenuSound(2);
      });
      this.additionButtons[i] = this.addControl(btn);
    }

    // Hits Rows (Middle column)
    for (int i = 0; i < MAX_HIT_SLOTS; i++) {
      final int hitSlot = i;
      final Label label = new Label(new RawText("Hit " + (i + 1) + ":"));
      label.setPos(136, 50 + i * 14);
      label.setWidth(42);
      this.hitLabels[i] = this.addControl(label);

      final Button btn = new Button(new RawText("[ CROSS ]"));
      btn.setPos(178, 50 + i * 14);
      btn.setSize(76, 12);
      btn.onPressed(() -> {
        this.selectedHitIndex = hitSlot;
        this.focusArea = 2;
        this.cycleHighlightedHit(1);
      });
      this.hitButtons[i] = this.addControl(btn);
    }

    // Hint label for setting hits
    this.hitHintLabel = this.addControl(new Label(new RawText("X/Sq/Tri: Map  O: Back")));
    this.hitHintLabel.getFontOptions().colour(TextColour.GREY).shadowColour(TextColour.BLACK);
    this.hitHintLabel.setPos(134, 138);
    this.hitHintLabel.setWidth(122);

    // Save Button
    this.saveButton = this.addControl(new Button(new RawText("SAVE CONFIGURATION")));
    this.saveButton.setPos((screenWidth - 160) / 2, 162);
    this.saveButton.setSize(160, 14);
    this.saveButton.getFontOptions().colour(TextColour.LIME);
    this.saveButton.onPressed(this::save);

    // Status message label
    this.statusLabel = this.addControl(new Label(new RawText("")));
    this.statusLabel.getFontOptions().horizontalAlign(HorizontalAlign.CENTRE);
    this.statusLabel.getFontOptions().colour(TextColour.LIME).shadowColour(TextColour.BLACK);
    this.statusLabel.setPos(0, 180);
    this.statusLabel.setWidth(screenWidth);

    // Initial load
    this.loadCharacter(0);

    // Back hotkey
    this.addHotkey(new I18nText("lod_core.ui.options_category.back"), INPUT_ACTION_MENU_BACK, this::back);
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

      final float previewCenterX = 318.0f;
      final float previewCenterY = 155.0f; // character feet ground level
      final float scale = this.getCharacterScale(this.selectedCharIndex);

      this.activeModel.coord2_14.coord.transfer.set(previewCenterX, previewCenterY, 50.0f);
      this.activeModel.coord2_14.transforms.scale.set(scale, scale, scale);
      // Angle character slightly forward-left to face the viewer and button mapping menu
      this.activeModel.coord2_14.transforms.rotate.set(0.10f, -0.65f, 0.0f);
      applyModelRotationAndScale(this.activeModel);

      if (this.activeModel.modelParts_00 != null) {
        for (int i = 0; i < this.activeModel.modelParts_00.length; i++) {
          if ((this.activeModel.partInvisible_f4 & (1L << i)) == 0) {
            final ModelPart10 part = this.activeModel.modelParts_00[i];
            if (part != null && part.tmd_08 != null && part.tmd_08.getObj() != null) {
              GsGetLw(part.coord2_04, this.tempLw);
              RENDERER.queueOrthoModel(part.tmd_08.getObj(), this.tempLw, QueuedModelTmd.class)
                .depthOffset(-200.0f)
                .lightDirection(lightDirectionMatrix_800c34e8)
                .lightColour(lightColourMatrix_800c3508)
                .backgroundColour(this.ambientLight);
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
    this.loadCharacter(this.selectedCharIndex);
  }

  private void updateCharacterHeader() {
    final CustomAdditionsStorage.CharacterCategory character = CustomAdditionsStorage.CHARACTERS.get(this.selectedCharIndex);
    final ControllerTheme theme = ControllerTheme.getActiveTheme();
    final String lBumper = theme == ControllerTheme.XBOX ? "[LB]" : (theme == ControllerTheme.SWITCH ? "[L]" : "[L1]");
    final String rBumper = theme == ControllerTheme.XBOX ? "[RB]" : (theme == ControllerTheme.SWITCH ? "[R]" : "[R1]");
    final int total = CustomAdditionsStorage.CHARACTERS.size();
    this.charLabel.setText(new RawText(lBumper + "  < " + character.characterName.toUpperCase() + " (" + (this.selectedCharIndex + 1) + "/" + total + ") >  " + rBumper));
  }

  private void loadCharacter(final int index) {
    if (index < 0 || index >= CustomAdditionsStorage.CHARACTERS.size()) {
      return;
    }
    final CustomAdditionsStorage.CharacterCategory character = CustomAdditionsStorage.CHARACTERS.get(index);
    this.updateCharacterHeader();

    this.selectedAdditionIndex = 0;
    this.selectedHitIndex = 0;
    this.currentLoadedAdditionFile = -1;
    this.currentLoadedHitIndex = -1;

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
      case 5 -> 0.29f; // Meru
      case 6 -> 0.21f; // Kongol
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
        tim.uploadToGpu();
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
        initModel(this.activeModel, cContainer, anim);
        TmdObjLoader.fromModel("CombatantModel (" + charKey + ")", this.activeModel);
      }
    } catch (final Exception ignored) {
      this.cleanupModel();
    }
  }

  private TmdAnimationFile loadHitAnimation(final int charIndex, final int additionIndex, final int hitIndex) {
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
          return new TmdAnimationFile(data);
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

      final int hitToPreview = this.focusArea == 2 ? this.selectedHitIndex : 0;
      if (retail.additionFile == this.currentLoadedAdditionFile && hitToPreview == this.currentLoadedHitIndex) {
        return;
      }

      final TmdAnimationFile anim = this.loadHitAnimation(this.selectedCharIndex, this.selectedAdditionIndex, hitToPreview);
      if (anim != null) {
        loadModelStandardAnimation(this.activeModel, anim);
        this.activeModel.animationState_9c = 1;
        this.currentLoadedAdditionFile = retail.additionFile;
        this.currentLoadedHitIndex = hitToPreview;
      }
      this.updatePreviewLabel(def.displayName, hitToPreview, def.hitCount);
    } catch (final Exception ignored) {
    }
  }

  private void updatePreviewLabel(final String additionName, final int hitIndex, final int totalHits) {
    if (this.previewHitLabel != null) {
      this.previewHitLabel.setText(new RawText("Hit " + (hitIndex + 1) + " of " + totalHits));
    }
  }

  private void cleanupModel() {
    if (this.activeModel != null) {
      try {
        this.activeModel.deleteModelParts();
      } catch (final Exception ignored) {
      }
      this.activeModel = null;
      this.currentLoadedAdditionFile = -1;
      this.currentLoadedHitIndex = -1;
    }
  }

  private void updateHitsList() {
    final CustomAdditionsStorage.CharacterCategory character = CustomAdditionsStorage.CHARACTERS.get(this.selectedCharIndex);
    if (this.selectedAdditionIndex >= character.additions.size()) {
      return;
    }
    final CustomAdditionsStorage.AdditionDefinition addition = character.additions.get(this.selectedAdditionIndex);
    final List<AdditionButtonType> buttons = CustomAdditionsStorage.getCustomButtons(addition.id);

    this.hitsHeader.setText(new RawText(addition.displayName.toUpperCase() + " (" + addition.hitCount + " HITS)"));

    for (int i = 0; i < MAX_HIT_SLOTS; i++) {
      if (i < addition.hitCount) {
        this.hitLabels[i].show();
        this.hitButtons[i].show();

        AdditionButtonType currentBtn = AdditionButtonType.CROSS;
        if (buttons != null && i < buttons.size()) {
          currentBtn = buttons.get(i);
        }
        this.hitButtons[i].setText(new RawText("[ " + currentBtn.name() + " ]"));
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

    if (this.selectedHitIndex >= 0 && this.selectedHitIndex < addition.hitCount) {
      buttons.set(this.selectedHitIndex, type);
      CustomAdditionsStorage.setCustomButtons(addition.id, buttons);
      playMenuSound(2);

      this.updateHitsList();
      this.refreshVisuals();
      this.statusLabel.setText(new RawText(""));
    }
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

    // Style addition buttons
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

    // Style hit buttons and labels
    final List<AdditionButtonType> buttons = CustomAdditionsStorage.getCustomButtons(addition.id);
    for (int i = 0; i < addition.hitCount; i++) {
      AdditionButtonType btnType = AdditionButtonType.CROSS;
      if (buttons != null && i < buttons.size()) {
        btnType = buttons.get(i);
      }

      if (this.focusArea == 2 && i == this.selectedHitIndex) {
        this.hitLabels[i].setText(new RawText("> Hit " + (i + 1) + ":"));
        this.hitLabels[i].getFontOptions().colour(TextColour.GOLD);
        this.hitButtons[i].setText(new RawText("> [ " + btnType.name() + " ] <"));
      } else {
        this.hitLabels[i].setText(new RawText("  Hit " + (i + 1) + ":"));
        this.hitLabels[i].getFontOptions().colour(TextColour.WHITE);
        this.hitButtons[i].setText(new RawText("[ " + btnType.name() + " ]"));
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

    // Synchronize 3D model animation with current hit
    this.updateModelAnimation();
  }

  private void save() {
    CustomAdditionsStorage.save();
    playMenuSound(2);
    this.statusLabel.setText(new RawText("Custom addition mappings saved successfully!"));
    // After saving, return to additions list
    this.focusArea = 1;
    this.refreshVisuals();
  }

  private void back() {
    playMenuSound(3);
    this.cleanupModel();
    this.unload.run();
  }

  @Override
  protected InputPropagation inputActionPressed(final InputAction action, final boolean repeat) {
    // === BUMPER INPUTS (L1/R1) to cycle characters ===
    if (action == INPUT_ACTION_MENU_PAGE_UP.get()) {
      this.switchCharacter(-1);
      return InputPropagation.HANDLED;
    }
    if (action == INPUT_ACTION_MENU_PAGE_DOWN.get()) {
      this.switchCharacter(1);
      return InputPropagation.HANDLED;
    }

    // === HITS SIDE MENU FOCUS ===
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

      // Circle (O) -> Return to additions list
      if (action == INPUT_ACTION_MENU_BACK.get()) {
        this.focusArea = 1;
        playMenuSound(3);
        this.refreshVisuals();
        return InputPropagation.HANDLED;
      }

      // Cross (X) -> sets hit to CROSS
      if (action == INPUT_ACTION_MENU_CONFIRM.get() && !repeat) {
        this.setHighlightedHit(AdditionButtonType.CROSS);
        return InputPropagation.HANDLED;
      }

      // Square -> sets hit to SQUARE
      if ((action == TvvlrMultimod.INPUT_ACTION_ADDITION_SQUARE.get()
          || action == CoreMod.INPUT_ACTION_MENU_SORT.get()
          || action == CoreMod.INPUT_ACTION_MENU_DELETE.get()) && !repeat) {
        this.setHighlightedHit(AdditionButtonType.SQUARE);
        return InputPropagation.HANDLED;
      }

      // Triangle -> sets hit to TRIANGLE
      if ((action == TvvlrMultimod.INPUT_ACTION_ADDITION_TRIANGLE.get()
          || action == CoreMod.INPUT_ACTION_MENU_ADVANCED.get()
          || action == CoreMod.INPUT_ACTION_MENU_HELP.get()) && !repeat) {
        this.setHighlightedHit(AdditionButtonType.TRIANGLE);
        return InputPropagation.HANDLED;
      }

      return InputPropagation.HANDLED;
    }

    // === ADDITIONS LIST OR SAVE BUTTON FOCUS ===
    if (action == INPUT_ACTION_MENU_UP.get()) {
      this.handleNavigateUp();
      return InputPropagation.HANDLED;
    }

    if (action == INPUT_ACTION_MENU_DOWN.get()) {
      this.handleNavigateDown();
      return InputPropagation.HANDLED;
    }

    if (action == INPUT_ACTION_MENU_CONFIRM.get() && !repeat) {
      this.handleConfirm();
      return InputPropagation.HANDLED;
    }

    if (action == INPUT_ACTION_MENU_BACK.get()) {
      this.back();
      return InputPropagation.HANDLED;
    }

    return super.inputActionPressed(action, repeat);
  }

  @Override
  protected InputPropagation buttonPress(final InputButton button, final boolean repeat) {
    if (button == InputButton.LEFT_BUMPER) {
      this.switchCharacter(-1);
      return InputPropagation.HANDLED;
    }

    if (button == InputButton.RIGHT_BUMPER) {
      this.switchCharacter(1);
      return InputPropagation.HANDLED;
    }

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
      if (button == InputButton.B) { // Circle (O) -> back to additions
        this.focusArea = 1;
        playMenuSound(3);
        this.refreshVisuals();
        return InputPropagation.HANDLED;
      }
    }

    return super.buttonPress(button, repeat);
  }

  @Override
  protected InputPropagation keyPress(final InputKey key, final InputKey scancode, final Set<InputMod> mods, final boolean repeat) {
    if (key == InputKey.PAGE_UP || key == InputKey.Q) {
      this.switchCharacter(-1);
      return InputPropagation.HANDLED;
    }

    if (key == InputKey.PAGE_DOWN || key == InputKey.E) {
      this.switchCharacter(1);
      return InputPropagation.HANDLED;
    }

    if (this.focusArea == 2 && !repeat) {
      if (key == InputKey.X || key == InputKey.RETURN || key == InputKey.SPACE) {
        this.setHighlightedHit(AdditionButtonType.CROSS);
        return InputPropagation.HANDLED;
      }
      if (key == InputKey.S || key == InputKey.A) {
        this.setHighlightedHit(AdditionButtonType.SQUARE);
        return InputPropagation.HANDLED;
      }
      if (key == InputKey.T || key == InputKey.W || key == InputKey.Y) {
        this.setHighlightedHit(AdditionButtonType.TRIANGLE);
        return InputPropagation.HANDLED;
      }
      if (key == InputKey.ESCAPE || key == InputKey.O || key == InputKey.BACKSPACE) {
        this.focusArea = 1;
        playMenuSound(3);
        this.refreshVisuals();
        return InputPropagation.HANDLED;
      }
    }

    return super.keyPress(key, scancode, mods, repeat);
  }

  private void handleNavigateUp() {
    if (this.focusArea == 1) { // Additions list
      if (this.selectedAdditionIndex > 0) {
        this.selectedAdditionIndex--;
        playMenuSound(1);
        this.updateHitsList();
        this.refreshVisuals();
      }
    } else if (this.focusArea == 3) { // From Save button
      final CustomAdditionsStorage.CharacterCategory character = CustomAdditionsStorage.CHARACTERS.get(this.selectedCharIndex);
      this.focusArea = 1;
      this.selectedAdditionIndex = Math.max(0, character.additions.size() - 1);
      playMenuSound(1);
      this.updateHitsList();
      this.refreshVisuals();
    }
  }

  private void handleNavigateDown() {
    final CustomAdditionsStorage.CharacterCategory character = CustomAdditionsStorage.CHARACTERS.get(this.selectedCharIndex);

    if (this.focusArea == 1) { // Additions list
      if (this.selectedAdditionIndex < character.additions.size() - 1) {
        this.selectedAdditionIndex++;
        playMenuSound(1);
        this.updateHitsList();
        this.refreshVisuals();
      } else {
        // Move to Save button
        this.focusArea = 3;
        playMenuSound(1);
        this.refreshVisuals();
      }
    }
  }

  private void handleConfirm() {
    if (this.focusArea == 1) {
      // Cross on an addition opens its hits menu
      this.focusArea = 2;
      this.selectedHitIndex = 0;
      playMenuSound(2);
      this.refreshVisuals();
    } else if (this.focusArea == 3) {
      this.save();
    }
  }
}
