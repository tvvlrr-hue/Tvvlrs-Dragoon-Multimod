package legend.multimod;

import legend.core.lang.I18nText;
import legend.core.lang.RawText;
import legend.core.platform.input.InputAction;
import legend.core.platform.input.InputMod;
import legend.game.SItem;
import legend.game.inventory.screens.HorizontalAlign;
import legend.game.inventory.screens.InputPropagation;
import legend.game.inventory.screens.TextColour;
import legend.game.inventory.screens.VerticalLayoutScreen;
import legend.game.inventory.screens.controls.Background;
import legend.game.inventory.screens.controls.Button;
import legend.game.inventory.screens.controls.Checkbox;
import legend.game.inventory.screens.controls.Label;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static legend.game.FullScreenEffects.startFadeEffect;
import static legend.game.Menus.deallocateRenderables;
import static legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_BACK;
import static legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_CONFIRM;
import static legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_DOWN;
import static legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_LEFT;
import static legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_RIGHT;
import static legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_UP;
import static legend.game.sound.Audio.playMenuSound;

public class MultimodOptionsScreen extends VerticalLayoutScreen {
  private static final Method HIGHLIGHT_ROW_METHOD;
  private static final Method UPDATE_ENTRIES_METHOD;
  private static final Field SCROLL_FIELD;

  static {
    Method highlightMethod = null;
    Method updateMethod = null;
    Field scroll = null;
    try {
      highlightMethod = VerticalLayoutScreen.class.getDeclaredMethod("highlightRow", int.class);
      highlightMethod.setAccessible(true);
      updateMethod = VerticalLayoutScreen.class.getDeclaredMethod("updateEntries");
      updateMethod.setAccessible(true);
      scroll = VerticalLayoutScreen.class.getDeclaredField("scroll");
      scroll.setAccessible(true);
    } catch (final Throwable ignored) {
    }
    HIGHLIGHT_ROW_METHOD = highlightMethod;
    UPDATE_ENTRIES_METHOD = updateMethod;
    SCROLL_FIELD = scroll;
  }

  private final Runnable unload;

  // Ordered list of selectable rows
  private final List<Label> selectableRows = new ArrayList<>();
  private final List<Label> headerRows = new ArrayList<>();
  private final List<Label> allRows = new ArrayList<>();

  // Section 1: Advanced Additions
  private final Label additionsHeader;
  private final Label additionsRow;
  private final Checkbox additionsCheckbox;
  private final Label modeRow;
  private final Button modeButton;
  private final Label configureRow;
  private final Button configureButton;
  private final Label randomAdditionsRow;
  private final Checkbox randomAdditionsCheckbox;
  private final Label themeRow;
  private final Button themeButton;

  // Section 2: Racing Minigame
  private final Label racingHeader;
  private final Label racingRow;
  private final Checkbox racingCheckbox;
  private final Label freeEntryRow;
  private final Checkbox freeEntryCheckbox;

  // Section 3: Dance Modifier
  private final Label danceHeader;
  private final Label danceRow;
  private final Checkbox danceCheckbox;
  private final Label configureDancesRow;
  private final Button configureDancesButton;

  // Section 4: Master Tasman Practice
  private final Label tasmanHeader;
  private final Label tasmanRow;
  private final Checkbox tasmanCheckbox;

  // Section 5: Fast Travel
  private final Label fastTravelHeader;
  private final Label fastTravelRow;
  private final Checkbox fastTravelCheckbox;
  private final Label fastTravelUnlockAllRow;
  private final Checkbox fastTravelUnlockAllCheckbox;

  public MultimodOptionsScreen(final Runnable unload) {
    deallocateRenderables(0xff);
    startFadeEffect(2, 10);

    this.unload = unload;
    this.addControl(new Background());

    // Title / Header
    final Label title = this.addControl(new Label(new RawText("Tvvlr's Multimod Settings")));
    title.getFontOptions().horizontalAlign(HorizontalAlign.CENTRE);
    title.getFontOptions().colour(TextColour.GOLD).shadowColour(TextColour.BLACK);
    title.setPos(0, 16);
    title.setWidth(this.getWidth());

    // === SECTION 1: ADVANCED ADDITIONS ===
    this.additionsHeader = this.addHeaderRow("[ ADVANCED ADDITIONS ]");

    this.additionsCheckbox = new Checkbox();
    this.additionsCheckbox.setHorizontalAlign(HorizontalAlign.RIGHT);
    this.additionsCheckbox.setChecked(TvvlrMultimod.isAdvancedAdditionsEnabled());
    this.additionsCheckbox.onToggled(TvvlrMultimod::setAdvancedAdditionsEnabled);
    this.additionsRow = this.addOptionRow("  Enabled", this.additionsCheckbox);

    this.modeButton = new Button(new RawText(TvvlrMultimod.getAdditionMode().name()));
    this.modeButton.onPressed(() -> {
      final AdditionMode next = TvvlrMultimod.cycleAdditionMode(1);
      this.modeButton.setText(new RawText(next.name()));
      this.updateConfigureRowVisibility();
    });
    this.modeRow = this.addOptionRow("  Mode", this.modeButton);

    this.configureButton = new Button(new RawText("Configure"));
    this.configureButton.onPressed(this::openCustomConfig);
    this.configureRow = this.addOptionRow("  Configure Additions", this.configureButton);

    this.randomAdditionsCheckbox = new Checkbox();
    this.randomAdditionsCheckbox.setHorizontalAlign(HorizontalAlign.RIGHT);
    this.randomAdditionsCheckbox.setChecked(TvvlrMultimod.isRandomAdditionsEnabled());
    this.randomAdditionsCheckbox.onToggled(TvvlrMultimod::setRandomAdditionsEnabled);
    this.randomAdditionsRow = this.addOptionRow("  Random Additions", this.randomAdditionsCheckbox);

    this.themeButton = new Button(new RawText(TvvlrMultimod.getControllerTheme().name()));
    this.themeButton.onPressed(() -> {
      final ControllerTheme next = TvvlrMultimod.cycleControllerTheme();
      this.themeButton.setText(new RawText(next.name()));
    });
    this.themeRow = this.addOptionRow("  Controller Colors", this.themeButton);

    // === SECTION 2: RACING MINIGAME ===
    this.racingHeader = this.addHeaderRow("[ RACING MINIGAME ]");

    this.racingCheckbox = new Checkbox();
    this.racingCheckbox.setHorizontalAlign(HorizontalAlign.RIGHT);
    this.racingCheckbox.setChecked(TvvlrMultimod.isRacingMinigameEnabled());
    this.racingCheckbox.onToggled(TvvlrMultimod::setRacingMinigameEnabled);
    this.racingRow = this.addOptionRow("  Enabled", this.racingCheckbox);

    this.freeEntryCheckbox = new Checkbox();
    this.freeEntryCheckbox.setHorizontalAlign(HorizontalAlign.RIGHT);
    this.freeEntryCheckbox.setChecked(TvvlrMultimod.isFreeRaceEntryEnabled());
    this.freeEntryCheckbox.onToggled(TvvlrMultimod::setFreeRaceEntryEnabled);
    this.freeEntryRow = this.addOptionRow("  Free Entry (No Tickets)", this.freeEntryCheckbox);

    // === SECTION 3: DANCE MODIFIER ===
    this.danceHeader = this.addHeaderRow("[ DANCE MODIFIER ]");

    this.danceCheckbox = new Checkbox();
    this.danceCheckbox.setHorizontalAlign(HorizontalAlign.RIGHT);
    this.danceCheckbox.setChecked(TvvlrMultimod.isDanceModifierEnabled());
    this.danceCheckbox.onToggled(TvvlrMultimod::setDanceModifierEnabled);
    this.danceRow = this.addOptionRow("  Enabled", this.danceCheckbox);

    this.configureDancesButton = new Button(new RawText("Configure"));
    this.configureDancesButton.onPressed(this::openDanceConfig);
    this.configureDancesRow = this.addOptionRow("  Configure Dances", this.configureDancesButton);

    // === SECTION 4: MASTER TASMAN ===
    this.tasmanHeader = this.addHeaderRow("[ MASTER TASMAN ]");

    this.tasmanCheckbox = new Checkbox();
    this.tasmanCheckbox.setHorizontalAlign(HorizontalAlign.RIGHT);
    this.tasmanCheckbox.setChecked(TvvlrMultimod.isTasmanPracticeEnabled());
    this.tasmanCheckbox.onToggled(TvvlrMultimod::setTasmanPracticeEnabled);
    this.tasmanRow = this.addOptionRow("  Enabled", this.tasmanCheckbox);

    // === SECTION 5: FAST TRAVEL ===
    this.fastTravelHeader = this.addHeaderRow("[ FAST TRAVEL ]");

    this.fastTravelCheckbox = new Checkbox();
    this.fastTravelCheckbox.setHorizontalAlign(HorizontalAlign.RIGHT);
    this.fastTravelCheckbox.setChecked(TvvlrMultimod.isFastTravelEnabled());
    this.fastTravelCheckbox.onToggled(TvvlrMultimod::setFastTravelEnabled);
    this.fastTravelRow = this.addOptionRow("  Enabled", this.fastTravelCheckbox);

    this.fastTravelUnlockAllCheckbox = new Checkbox();
    this.fastTravelUnlockAllCheckbox.setHorizontalAlign(HorizontalAlign.RIGHT);
    this.fastTravelUnlockAllCheckbox.setChecked(TvvlrMultimod.isFastTravelUnlockAll());
    this.fastTravelUnlockAllCheckbox.onToggled(TvvlrMultimod::setFastTravelUnlockAll);
    this.fastTravelUnlockAllRow = this.addOptionRow("  Unlock All Locations", this.fastTravelUnlockAllCheckbox);

    // Initial visibility for configure row
    this.updateConfigureRowVisibility();

    // Initial highlight on first selectable option (additionsRow)
    this.selectRow(this.additionsRow);

    // Back hotkey
    this.addHotkey(new I18nText("lod_core.ui.options_category.back"), INPUT_ACTION_MENU_BACK, this::back);
  }

  private Label addHeaderRow(final String title) {
    final Label label = this.addRow(new RawText(title), null);
    label.getFontOptions().colour(TextColour.GOLD).shadowColour(TextColour.BLACK);
    this.headerRows.add(label);
    this.allRows.add(label);
    return label;
  }

  private <T extends legend.game.inventory.screens.Control> Label addOptionRow(final String text, final T control) {
    final Label label = this.addRow(new RawText(text), control);
    this.selectableRows.add(label);
    this.allRows.add(label);
    return label;
  }

  private void updateConfigureRowVisibility() {
    final boolean isCustom = TvvlrMultimod.getAdditionMode() == AdditionMode.CUSTOM;
    final int configRowIndex = this.allRows.indexOf(this.configureRow);
    if (configRowIndex >= 0) {
      this.setRowVisible(configRowIndex, isCustom);
    }
  }

  private void openCustomConfig() {
    playMenuSound(2);
    SItem.menuStack.pushScreen(new CustomAdditionsConfigScreen(() -> {
      startFadeEffect(2, 10);
      SItem.menuStack.popScreen();
    }));
  }

  private void openDanceConfig() {
    playMenuSound(2);
    SItem.menuStack.pushScreen(new DanceModifierConfigScreen(() -> {
      startFadeEffect(2, 10);
      SItem.menuStack.popScreen();
    }));
  }

  private void selectRow(final Label targetRow) {
    final int targetIndex = this.allRows.indexOf(targetRow);
    if (targetIndex < 0) {
      return;
    }

    try {
      if (SCROLL_FIELD != null) {
        int currentScroll = SCROLL_FIELD.getInt(this);
        final int maxVisible = this.maxVisibleEntries();
        if (targetIndex < currentScroll) {
          currentScroll = targetIndex;
          SCROLL_FIELD.setInt(this, currentScroll);
          if (UPDATE_ENTRIES_METHOD != null) {
            UPDATE_ENTRIES_METHOD.invoke(this);
          }
        } else if (targetIndex >= currentScroll + maxVisible) {
          currentScroll = targetIndex - maxVisible + 1;
          SCROLL_FIELD.setInt(this, currentScroll);
          if (UPDATE_ENTRIES_METHOD != null) {
            UPDATE_ENTRIES_METHOD.invoke(this);
          }
        }
      }

      if (HIGHLIGHT_ROW_METHOD != null) {
        HIGHLIGHT_ROW_METHOD.invoke(this, targetIndex);
      }
    } catch (final Throwable ignored) {
    }
  }

  private void back() {
    playMenuSound(3);
    this.unload.run();
  }

  private boolean isRowActive(final Label row) {
    if (row == this.configureRow) {
      return TvvlrMultimod.getAdditionMode() == AdditionMode.CUSTOM;
    }
    return true;
  }

  private void navigateUp() {
    final Label current = this.getHighlightedRow();
    int currentIndex = this.selectableRows.indexOf(current);
    if (currentIndex < 0) {
      this.selectRow(this.selectableRows.get(0));
      playMenuSound(1);
      return;
    }

    // Look backwards for previous active row
    for (int i = currentIndex - 1; i >= 0; i--) {
      final Label candidate = this.selectableRows.get(i);
      if (this.isRowActive(candidate)) {
        this.selectRow(candidate);
        playMenuSound(1);
        return;
      }
    }

    // Wrap to bottom
    for (int i = this.selectableRows.size() - 1; i > currentIndex; i--) {
      final Label candidate = this.selectableRows.get(i);
      if (this.isRowActive(candidate)) {
        this.selectRow(candidate);
        playMenuSound(1);
        return;
      }
    }
  }

  private void navigateDown() {
    final Label current = this.getHighlightedRow();
    int currentIndex = this.selectableRows.indexOf(current);
    if (currentIndex < 0) {
      this.selectRow(this.selectableRows.get(0));
      playMenuSound(1);
      return;
    }

    // Look forwards for next active row
    for (int i = currentIndex + 1; i < this.selectableRows.size(); i++) {
      final Label candidate = this.selectableRows.get(i);
      if (this.isRowActive(candidate)) {
        this.selectRow(candidate);
        playMenuSound(1);
        return;
      }
    }

    // Wrap to top
    for (int i = 0; i < currentIndex; i++) {
      final Label candidate = this.selectableRows.get(i);
      if (this.isRowActive(candidate)) {
        this.selectRow(candidate);
        playMenuSound(1);
        return;
      }
    }
  }

  private void handleAction(final int direction) {
    final Label highlighted = this.getHighlightedRow();

    if (highlighted == this.additionsRow && this.additionsCheckbox != null) {
      playMenuSound(2);
      this.additionsCheckbox.setChecked(!this.additionsCheckbox.isChecked());
    } else if (highlighted == this.modeRow && this.modeButton != null) {
      final AdditionMode next = TvvlrMultimod.cycleAdditionMode(direction);
      this.modeButton.setText(new RawText(next.name()));
      this.updateConfigureRowVisibility();
      playMenuSound(2);
    } else if (highlighted == this.configureRow && this.configureButton != null) {
      this.openCustomConfig();
    } else if (highlighted == this.randomAdditionsRow && this.randomAdditionsCheckbox != null) {
      playMenuSound(2);
      this.randomAdditionsCheckbox.setChecked(!this.randomAdditionsCheckbox.isChecked());
    } else if (highlighted == this.themeRow && this.themeButton != null) {
      final ControllerTheme next = TvvlrMultimod.cycleControllerTheme(direction);
      this.themeButton.setText(new RawText(next.name()));
      playMenuSound(2);
    } else if (highlighted == this.racingRow && this.racingCheckbox != null) {
      playMenuSound(2);
      this.racingCheckbox.setChecked(!this.racingCheckbox.isChecked());
    } else if (highlighted == this.freeEntryRow && this.freeEntryCheckbox != null) {
      playMenuSound(2);
      this.freeEntryCheckbox.setChecked(!this.freeEntryCheckbox.isChecked());
    } else if (highlighted == this.danceRow && this.danceCheckbox != null) {
      playMenuSound(2);
      this.danceCheckbox.setChecked(!this.danceCheckbox.isChecked());
    } else if (highlighted == this.configureDancesRow && this.configureDancesButton != null) {
      this.openDanceConfig();
    } else if (highlighted == this.tasmanRow && this.tasmanCheckbox != null) {
      playMenuSound(2);
      this.tasmanCheckbox.setChecked(!this.tasmanCheckbox.isChecked());
    } else if (highlighted == this.fastTravelRow && this.fastTravelCheckbox != null) {
      playMenuSound(2);
      this.fastTravelCheckbox.setChecked(!this.fastTravelCheckbox.isChecked());
    } else if (highlighted == this.fastTravelUnlockAllRow && this.fastTravelUnlockAllCheckbox != null) {
      playMenuSound(2);
      this.fastTravelUnlockAllCheckbox.setChecked(!this.fastTravelUnlockAllCheckbox.isChecked());
    }
  }

  @Override
  protected InputPropagation inputActionPressed(final InputAction action, final boolean repeat) {
    if (action == INPUT_ACTION_MENU_UP.get()) {
      this.navigateUp();
      return InputPropagation.HANDLED;
    }

    if (action == INPUT_ACTION_MENU_DOWN.get()) {
      this.navigateDown();
      return InputPropagation.HANDLED;
    }

    if (action == INPUT_ACTION_MENU_CONFIRM.get() && !repeat) {
      this.handleAction(1);
      return InputPropagation.HANDLED;
    }

    if (action == INPUT_ACTION_MENU_RIGHT.get() && !repeat) {
      this.handleAction(1);
      return InputPropagation.HANDLED;
    }

    if (action == INPUT_ACTION_MENU_LEFT.get() && !repeat) {
      this.handleAction(-1);
      return InputPropagation.HANDLED;
    }

    return super.inputActionPressed(action, repeat);
  }

  @Override
  protected InputPropagation mouseMove(final double x, final double y) {
    final Label before = this.getHighlightedRow();
    final InputPropagation result = super.mouseMove(x, y);
    final Label after = this.getHighlightedRow();

    if (this.headerRows.contains(after)) {
      if (before != null && !this.headerRows.contains(before)) {
        this.selectRow(before);
      } else {
        this.selectRow(this.selectableRows.get(0));
      }
    }
    return result;
  }

  @Override
  protected InputPropagation mouseClick(final double x, final double y, final int button, final Set<InputMod> mods) {
    if (super.mouseClick(x, y, button, mods) == InputPropagation.HANDLED) {
      return InputPropagation.HANDLED;
    }

    if (button == 0) {
      this.handleAction(1);
      return InputPropagation.HANDLED;
    }

    return InputPropagation.PROPAGATE;
  }
}
