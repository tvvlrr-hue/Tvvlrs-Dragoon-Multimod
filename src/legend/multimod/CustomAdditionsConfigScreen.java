package legend.multimod;

import legend.core.lang.I18nText;
import legend.core.lang.RawText;
import legend.core.platform.input.InputAction;
import legend.core.platform.input.InputButton;
import legend.core.platform.input.InputKey;
import legend.core.platform.input.InputMod;
import legend.game.inventory.screens.HorizontalAlign;
import legend.game.inventory.screens.InputPropagation;
import legend.game.inventory.screens.MenuScreen;
import legend.game.inventory.screens.TextColour;
import legend.game.inventory.screens.controls.Background;
import legend.game.inventory.screens.controls.Button;
import legend.game.inventory.screens.controls.Label;
import legend.game.modding.coremod.CoreMod;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static legend.game.FullScreenEffects.startFadeEffect;
import static legend.game.Menus.deallocateRenderables;
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

  // Additions column (left)
  private final Button[] additionButtons = new Button[MAX_ADDITION_SLOTS];

  // Hits column (right)
  private final Label[] hitLabels = new Label[MAX_HIT_SLOTS];
  private final Button[] hitButtons = new Button[MAX_HIT_SLOTS];
  private final Label hitHintLabel;

  // Bottom action
  private final Button saveButton;
  private final Label statusLabel;

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
    this.titleLabel.setPos(0, 8);
    this.titleLabel.setWidth(screenWidth);

    // Character Selector with Bumper Indicators
    this.prevCharButton = this.addControl(new Button(new RawText("<")));
    this.prevCharButton.setPos(24, 22);
    this.prevCharButton.setSize(20, 12);
    this.prevCharButton.onPressed(() -> this.switchCharacter(-1));

    this.charLabel = this.addControl(new Label(new RawText("")));
    this.charLabel.getFontOptions().horizontalAlign(HorizontalAlign.CENTRE);
    this.charLabel.getFontOptions().colour(TextColour.YELLOW).shadowColour(TextColour.BLACK);
    this.charLabel.setPos(48, 22);
    this.charLabel.setWidth(screenWidth - 96);

    this.nextCharButton = this.addControl(new Button(new RawText(">")));
    this.nextCharButton.setPos(screenWidth - 44, 22);
    this.nextCharButton.setSize(20, 12);
    this.nextCharButton.onPressed(() -> this.switchCharacter(1));

    // Column Headers
    this.additionsHeader = this.addControl(new Label(new RawText("ADDITIONS")));
    this.additionsHeader.getFontOptions().colour(TextColour.GOLD).shadowColour(TextColour.BLACK);
    this.additionsHeader.setPos(20, 38);
    this.additionsHeader.setWidth(150);

    this.hitsHeader = this.addControl(new Label(new RawText("HITS CONFIGURATION")));
    this.hitsHeader.getFontOptions().colour(TextColour.GOLD).shadowColour(TextColour.BLACK);
    this.hitsHeader.setPos(180, 38);
    this.hitsHeader.setWidth(170);

    // Additions Buttons (Left column)
    for (int i = 0; i < MAX_ADDITION_SLOTS; i++) {
      final int slot = i;
      final Button btn = new Button(new RawText(""));
      btn.setPos(20, 52 + i * 14);
      btn.setSize(150, 12);
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

    // Hits Rows (Right column)
    for (int i = 0; i < MAX_HIT_SLOTS; i++) {
      final int hitSlot = i;
      final Label label = new Label(new RawText("Hit " + (i + 1) + ":"));
      label.setPos(180, 52 + i * 14);
      label.setWidth(56);
      this.hitLabels[i] = this.addControl(label);

      final Button btn = new Button(new RawText("[ CROSS ]"));
      btn.setPos(238, 52 + i * 14);
      btn.setSize(104, 12);
      btn.onPressed(() -> {
        this.selectedHitIndex = hitSlot;
        this.focusArea = 2;
        this.cycleHighlightedHit(1);
      });
      this.hitButtons[i] = this.addControl(btn);
    }

    // Hint label for setting hits
    this.hitHintLabel = this.addControl(new Label(new RawText("Press X, Sq, or Tri to set. O to return.")));
    this.hitHintLabel.getFontOptions().colour(TextColour.GREY).shadowColour(TextColour.BLACK);
    this.hitHintLabel.setPos(175, 140);
    this.hitHintLabel.setWidth(185);

    // Save Button
    this.saveButton = this.addControl(new Button(new RawText("SAVE CONFIGURATION")));
    this.saveButton.setPos((screenWidth - 170) / 2, 158);
    this.saveButton.setSize(170, 14);
    this.saveButton.getFontOptions().colour(TextColour.LIME);
    this.saveButton.onPressed(this::save);

    // Status message label
    this.statusLabel = this.addControl(new Label(new RawText("")));
    this.statusLabel.getFontOptions().horizontalAlign(HorizontalAlign.CENTRE);
    this.statusLabel.getFontOptions().colour(TextColour.LIME).shadowColour(TextColour.BLACK);
    this.statusLabel.setPos(0, 176);
    this.statusLabel.setWidth(screenWidth);

    // Initial load
    this.loadCharacter(0);

    // Back hotkey
    this.addHotkey(new I18nText("lod_core.ui.options_category.back"), INPUT_ACTION_MENU_BACK, this::back);
  }

  @Override
  protected void render() {
    // Render hook
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

    for (int i = 0; i < MAX_ADDITION_SLOTS; i++) {
      if (i < character.additions.size()) {
        final CustomAdditionsStorage.AdditionDefinition add = character.additions.get(i);
        this.additionButtons[i].setText(new RawText(add.displayName));
        this.additionButtons[i].show();
      } else {
        this.additionButtons[i].hide();
      }
    }

    if (this.selectedAdditionIndex >= character.additions.size()) {
      this.selectedAdditionIndex = 0;
    }
    this.updateHitsList();
    this.refreshVisuals();
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
  }

  private void save() {
    CustomAdditionsStorage.save();
    playMenuSound(2);
    this.statusLabel.setText(new RawText("Configuration Saved Successfully!"));
    // "if I click save it will Save it and go back to the characters additions"
    this.focusArea = 1;
    this.refreshVisuals();
  }

  private void back() {
    playMenuSound(3);
    this.unload.run();
  }

  @Override
  protected InputPropagation inputActionPressed(final InputAction action, final boolean repeat) {
    // Bumpers always cycle character
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
      // Up / Down scroll between hits
      if (action == INPUT_ACTION_MENU_UP.get()) {
        if (this.selectedHitIndex > 0) {
          this.selectedHitIndex--;
          playMenuSound(1);
          this.refreshVisuals();
        }
        return InputPropagation.HANDLED;
      }

      if (action == INPUT_ACTION_MENU_DOWN.get()) {
        final CustomAdditionsStorage.AdditionDefinition addition = CustomAdditionsStorage.CHARACTERS.get(this.selectedCharIndex).additions.get(this.selectedAdditionIndex);
        if (this.selectedHitIndex < addition.hitCount - 1) {
          this.selectedHitIndex++;
          playMenuSound(1);
          this.refreshVisuals();
        }
        return InputPropagation.HANDLED;
      }

      // Cannot move side to side inside hits menu
      if (action == INPUT_ACTION_MENU_LEFT.get() || action == INPUT_ACTION_MENU_RIGHT.get()) {
        return InputPropagation.HANDLED;
      }

      // O (Circle / Back) -> returns to the additions list
      if (action == INPUT_ACTION_MENU_BACK.get()) {
        this.focusArea = 1;
        playMenuSound(3);
        this.refreshVisuals();
        return InputPropagation.HANDLED;
      }

      // X (Cross) -> sets hit to CROSS
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
      if (button == InputButton.A) { // Cross (X)
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
      // Cross on addition -> puts into side menu for the addition
      this.focusArea = 2;
      this.selectedHitIndex = 0;
      playMenuSound(2);
      this.refreshVisuals();
    } else if (this.focusArea == 3) {
      this.save();
    }
  }
}
