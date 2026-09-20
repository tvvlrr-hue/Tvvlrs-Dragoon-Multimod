package legend.multimod;

import legend.core.lang.I18nText;
import legend.core.lang.RawText;
import legend.core.platform.input.InputAction;
import legend.core.platform.input.InputMod;
import legend.game.inventory.screens.HorizontalAlign;
import legend.game.inventory.screens.InputPropagation;
import legend.game.inventory.screens.MenuScreen;
import legend.game.inventory.screens.TextColour;
import legend.game.inventory.screens.controls.Background;
import legend.game.inventory.screens.controls.Button;
import legend.game.inventory.screens.controls.Label;

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

  // Focus Area: 0 = Character header, 1 = Additions list, 2 = Hits list, 3 = Save button
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
    this.titleLabel.setPos(0, 10);
    this.titleLabel.setWidth(screenWidth);

    // Character Selector
    this.prevCharButton = this.addControl(new Button(new RawText("<")));
    this.prevCharButton.setPos(50, 24);
    this.prevCharButton.setSize(20, 12);
    this.prevCharButton.onPressed(() -> this.switchCharacter(-1));

    this.charLabel = this.addControl(new Label(new RawText("")));
    this.charLabel.getFontOptions().horizontalAlign(HorizontalAlign.CENTRE);
    this.charLabel.getFontOptions().colour(TextColour.YELLOW).shadowColour(TextColour.BLACK);
    this.charLabel.setPos(74, 24);
    this.charLabel.setWidth(screenWidth - 148);

    this.nextCharButton = this.addControl(new Button(new RawText(">")));
    this.nextCharButton.setPos(screenWidth - 70, 24);
    this.nextCharButton.setSize(20, 12);
    this.nextCharButton.onPressed(() -> this.switchCharacter(1));

    // Column Headers
    this.additionsHeader = this.addControl(new Label(new RawText("ADDITIONS")));
    this.additionsHeader.getFontOptions().colour(TextColour.GOLD).shadowColour(TextColour.BLACK);
    this.additionsHeader.setPos(20, 42);
    this.additionsHeader.setWidth(150);

    this.hitsHeader = this.addControl(new Label(new RawText("HIT BUTTONS")));
    this.hitsHeader.getFontOptions().colour(TextColour.GOLD).shadowColour(TextColour.BLACK);
    this.hitsHeader.setPos(180, 42);
    this.hitsHeader.setWidth(170);

    // Additions Buttons
    for (int i = 0; i < MAX_ADDITION_SLOTS; i++) {
      final int slot = i;
      final Button btn = new Button(new RawText(""));
      btn.setPos(20, 56 + i * 14);
      btn.setSize(150, 12);
      btn.onPressed(() -> {
        this.selectedAdditionIndex = slot;
        this.focusArea = 1;
        this.updateHitsList();
        this.refreshVisuals();
      });
      this.additionButtons[i] = this.addControl(btn);
    }

    // Hits Rows (Label + Button)
    for (int i = 0; i < MAX_HIT_SLOTS; i++) {
      final int hitSlot = i;
      final Label label = new Label(new RawText("Hit " + (i + 1) + ":"));
      label.setPos(180, 56 + i * 14);
      label.setWidth(46);
      this.hitLabels[i] = this.addControl(label);

      final Button btn = new Button(new RawText("[ CROSS ]"));
      btn.setPos(230, 56 + i * 14);
      btn.setSize(110, 12);
      btn.onPressed(() -> {
        this.selectedHitIndex = hitSlot;
        this.focusArea = 2;
        this.cycleSelectedHit(1);
      });
      this.hitButtons[i] = this.addControl(btn);
    }

    // Save Button
    this.saveButton = this.addControl(new Button(new RawText("SAVE CONFIGURATION")));
    this.saveButton.setPos((screenWidth - 170) / 2, 164);
    this.saveButton.setSize(170, 14);
    this.saveButton.getFontOptions().colour(TextColour.LIME);
    this.saveButton.onPressed(this::save);

    // Status / Saved message label
    this.statusLabel = this.addControl(new Label(new RawText("")));
    this.statusLabel.getFontOptions().horizontalAlign(HorizontalAlign.CENTRE);
    this.statusLabel.getFontOptions().colour(TextColour.LIME).shadowColour(TextColour.BLACK);
    this.statusLabel.setPos(0, 182);
    this.statusLabel.setWidth(screenWidth);

    // Initial load
    this.loadCharacter(0);

    // Back hotkey
    this.addHotkey(new I18nText("lod_core.ui.options_category.back"), INPUT_ACTION_MENU_BACK, this::back);
  }

  @Override
  protected void render() {
    // MenuScreen render hook
  }

  private void switchCharacter(final int direction) {
    playMenuSound(1);
    final int total = CustomAdditionsStorage.CHARACTERS.size();
    this.selectedCharIndex = (this.selectedCharIndex + direction % total + total) % total;
    this.selectedAdditionIndex = 0;
    this.selectedHitIndex = 0;
    this.loadCharacter(this.selectedCharIndex);
  }

  private void loadCharacter(final int index) {
    if (index < 0 || index >= CustomAdditionsStorage.CHARACTERS.size()) {
      return;
    }
    final CustomAdditionsStorage.CharacterCategory character = CustomAdditionsStorage.CHARACTERS.get(index);
    final int total = CustomAdditionsStorage.CHARACTERS.size();
    this.charLabel.setText(new RawText("<  " + character.characterName.toUpperCase() + " (" + (index + 1) + "/" + total + ")  >"));

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

    this.hitsHeader.setText(new RawText(addition.displayName.toUpperCase()));

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

  private void cycleSelectedHit(final int direction) {
    final CustomAdditionsStorage.CharacterCategory character = CustomAdditionsStorage.CHARACTERS.get(this.selectedCharIndex);
    final CustomAdditionsStorage.AdditionDefinition addition = character.additions.get(this.selectedAdditionIndex);
    List<AdditionButtonType> buttons = CustomAdditionsStorage.getCustomButtons(addition.id);
    if (buttons == null) {
      buttons = new ArrayList<>();
    }
    while (buttons.size() < addition.hitCount) {
      buttons.add(AdditionButtonType.CROSS);
    }

    final AdditionButtonType current = buttons.get(this.selectedHitIndex);
    final AdditionButtonType next = AdditionButtonType.cycleCustomButton(current, direction);
    buttons.set(this.selectedHitIndex, next);

    CustomAdditionsStorage.setCustomButtons(addition.id, buttons);
    playMenuSound(2);

    this.hitButtons[this.selectedHitIndex].setText(new RawText("[ " + next.name() + " ]"));
    this.applyButtonColour(this.hitButtons[this.selectedHitIndex], next);
    this.statusLabel.setText(new RawText(""));
  }

  private void refreshVisuals() {
    final CustomAdditionsStorage.CharacterCategory character = CustomAdditionsStorage.CHARACTERS.get(this.selectedCharIndex);

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
  }

  private void back() {
    playMenuSound(3);
    this.unload.run();
  }

  @Override
  protected InputPropagation inputActionPressed(final InputAction action, final boolean repeat) {
    if (action == INPUT_ACTION_MENU_PAGE_UP.get()) {
      this.switchCharacter(-1);
      return InputPropagation.HANDLED;
    }

    if (action == INPUT_ACTION_MENU_PAGE_DOWN.get()) {
      this.switchCharacter(1);
      return InputPropagation.HANDLED;
    }

    if (action == INPUT_ACTION_MENU_UP.get()) {
      this.handleNavigateUp();
      return InputPropagation.HANDLED;
    }

    if (action == INPUT_ACTION_MENU_DOWN.get()) {
      this.handleNavigateDown();
      return InputPropagation.HANDLED;
    }

    if (action == INPUT_ACTION_MENU_LEFT.get() && !repeat) {
      this.handleNavigateLeft();
      return InputPropagation.HANDLED;
    }

    if (action == INPUT_ACTION_MENU_RIGHT.get() && !repeat) {
      this.handleNavigateRight();
      return InputPropagation.HANDLED;
    }

    if (action == INPUT_ACTION_MENU_CONFIRM.get() && !repeat) {
      this.handleConfirm();
      return InputPropagation.HANDLED;
    }

    return super.inputActionPressed(action, repeat);
  }

  private void handleNavigateUp() {
    final CustomAdditionsStorage.CharacterCategory character = CustomAdditionsStorage.CHARACTERS.get(this.selectedCharIndex);
    final CustomAdditionsStorage.AdditionDefinition addition = character.additions.get(this.selectedAdditionIndex);

    if (this.focusArea == 1) { // Additions list
      if (this.selectedAdditionIndex > 0) {
        this.selectedAdditionIndex--;
        playMenuSound(1);
        this.updateHitsList();
        this.refreshVisuals();
      } else {
        // Move to character selector
        this.focusArea = 0;
        playMenuSound(1);
        this.refreshVisuals();
      }
    } else if (this.focusArea == 2) { // Hits list
      if (this.selectedHitIndex > 0) {
        this.selectedHitIndex--;
        playMenuSound(1);
      } else {
        // Move to character selector
        this.focusArea = 0;
        playMenuSound(1);
        this.refreshVisuals();
      }
    } else if (this.focusArea == 3) { // From save button
      this.focusArea = 1;
      playMenuSound(1);
      this.refreshVisuals();
    }
  }

  private void handleNavigateDown() {
    final CustomAdditionsStorage.CharacterCategory character = CustomAdditionsStorage.CHARACTERS.get(this.selectedCharIndex);
    final CustomAdditionsStorage.AdditionDefinition addition = character.additions.get(this.selectedAdditionIndex);

    if (this.focusArea == 0) { // Character selector
      this.focusArea = 1;
      playMenuSound(1);
      this.refreshVisuals();
    } else if (this.focusArea == 1) { // Additions list
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
    } else if (this.focusArea == 2) { // Hits list
      if (this.selectedHitIndex < addition.hitCount - 1) {
        this.selectedHitIndex++;
        playMenuSound(1);
      } else {
        // Move to Save button
        this.focusArea = 3;
        playMenuSound(1);
        this.refreshVisuals();
      }
    }
  }

  private void handleNavigateLeft() {
    if (this.focusArea == 0) {
      this.switchCharacter(-1);
    } else if (this.focusArea == 2) {
      // From hits list back to additions list
      this.focusArea = 1;
      playMenuSound(1);
      this.refreshVisuals();
    } else if (this.focusArea == 1) {
      // Switch character left
      this.switchCharacter(-1);
    }
  }

  private void handleNavigateRight() {
    if (this.focusArea == 0) {
      this.switchCharacter(1);
    } else if (this.focusArea == 1) {
      // Enter hits list
      this.focusArea = 2;
      this.selectedHitIndex = 0;
      playMenuSound(1);
      this.refreshVisuals();
    } else if (this.focusArea == 2) {
      // Cycle hit forward
      this.cycleSelectedHit(1);
    }
  }

  private void handleConfirm() {
    if (this.focusArea == 0) {
      this.switchCharacter(1);
    } else if (this.focusArea == 1) {
      // Enter hits list
      this.focusArea = 2;
      this.selectedHitIndex = 0;
      playMenuSound(1);
      this.refreshVisuals();
    } else if (this.focusArea == 2) {
      // Cycle button
      this.cycleSelectedHit(1);
    } else if (this.focusArea == 3) {
      this.save();
    }
  }
}
