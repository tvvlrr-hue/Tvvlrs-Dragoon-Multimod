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

import java.lang.reflect.Method;
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
  static {
    Method m = null;
    try {
      m = VerticalLayoutScreen.class.getDeclaredMethod("highlightRow", int.class);
      m.setAccessible(true);
    } catch (final Throwable ignored) {
    }
    HIGHLIGHT_ROW_METHOD = m;
  }

  private final Runnable unload;

  // Section 1: Advanced Additions
  private final Label additionsHeader;
  private final Label additionsRow;
  private final Checkbox additionsCheckbox;

  private final Label modeRow;
  private final Button modeButton;

  private final Label configureRow;
  private final Button configureButton;

  private final Label themeRow;
  private final Button themeButton;

  // Section 2: Racing Minigame
  private final Label racingHeader;
  private final Label racingRow;
  private final Checkbox racingCheckbox;
  private final Label freeEntryRow;
  private final Checkbox freeEntryCheckbox;

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
    // Row 0: Section Header
    this.additionsHeader = this.addRow(new RawText("[ ADVANCED ADDITIONS ]"), null);
    this.additionsHeader.getFontOptions().colour(TextColour.GOLD).shadowColour(TextColour.BLACK);

    // Row 1: Additions Enabled
    this.additionsCheckbox = new Checkbox();
    this.additionsCheckbox.setHorizontalAlign(HorizontalAlign.RIGHT);
    this.additionsCheckbox.setChecked(TvvlrMultimod.isAdvancedAdditionsEnabled());
    this.additionsCheckbox.onToggled(TvvlrMultimod::setAdvancedAdditionsEnabled);
    this.additionsRow = this.addRow(new RawText("  Enabled"), this.additionsCheckbox);

    // Row 2: Addition Mode
    this.modeButton = new Button(new RawText(TvvlrMultimod.getAdditionMode().name()));
    this.modeButton.onPressed(() -> {
      final AdditionMode next = TvvlrMultimod.cycleAdditionMode(1);
      this.modeButton.setText(new RawText(next.name()));
      this.updateConfigureRowVisibility();
    });
    this.modeRow = this.addRow(new RawText("  Mode"), this.modeButton);

    // Row 3: Configure Additions (visible when Mode == CUSTOM)
    this.configureButton = new Button(new RawText("Configure"));
    this.configureButton.onPressed(this::openCustomConfig);
    this.configureRow = this.addRow(new RawText("  Configure Additions"), this.configureButton);

    // Row 4: Controller Colors
    this.themeButton = new Button(new RawText(TvvlrMultimod.getControllerTheme().name()));
    this.themeButton.onPressed(() -> {
      final ControllerTheme next = TvvlrMultimod.cycleControllerTheme();
      this.themeButton.setText(new RawText(next.name()));
    });
    this.themeRow = this.addRow(new RawText("  Controller Colors"), this.themeButton);

    // === SECTION 2: RACING MINIGAME ===
    // Row 5: Section Header
    this.racingHeader = this.addRow(new RawText("[ RACING MINIGAME ]"), null);
    this.racingHeader.getFontOptions().colour(TextColour.GOLD).shadowColour(TextColour.BLACK);

    // Row 6: Racing Enabled
    this.racingCheckbox = new Checkbox();
    this.racingCheckbox.setHorizontalAlign(HorizontalAlign.RIGHT);
    this.racingCheckbox.setChecked(TvvlrMultimod.isRacingMinigameEnabled());
    this.racingCheckbox.onToggled(TvvlrMultimod::setRacingMinigameEnabled);
    this.racingRow = this.addRow(new RawText("  Enabled"), this.racingCheckbox);

    // Row 7: Free Entry (No Tickets)
    this.freeEntryCheckbox = new Checkbox();
    this.freeEntryCheckbox.setHorizontalAlign(HorizontalAlign.RIGHT);
    this.freeEntryCheckbox.setChecked(TvvlrMultimod.isFreeRaceEntryEnabled());
    this.freeEntryCheckbox.onToggled(TvvlrMultimod::setFreeRaceEntryEnabled);
    this.freeEntryRow = this.addRow(new RawText("  Free Entry (No Tickets)"), this.freeEntryCheckbox);

    // Set initial visibility for configure row (Row 3)
    this.updateConfigureRowVisibility();

    // Initial highlight on row 1 (Enabled under Advanced Additions)
    this.selectRow(1);

    // Back hotkey
    this.addHotkey(new I18nText("lod_core.ui.options_category.back"), INPUT_ACTION_MENU_BACK, this::back);
  }

  private void updateConfigureRowVisibility() {
    final boolean isCustom = TvvlrMultimod.getAdditionMode() == AdditionMode.CUSTOM;
    this.setRowVisible(3, isCustom);
  }

  private void openCustomConfig() {
    playMenuSound(2);
    SItem.menuStack.pushScreen(new CustomAdditionsConfigScreen(() -> {
      startFadeEffect(2, 10);
      SItem.menuStack.popScreen();
    }));
  }

  private void selectRow(final int index) {
    if (HIGHLIGHT_ROW_METHOD != null) {
      try {
        HIGHLIGHT_ROW_METHOD.invoke(this, index);
      } catch (final Throwable ignored) {
      }
    }
  }

  private void back() {
    playMenuSound(3);
    this.unload.run();
  }

  private void navigateUp() {
    final Label highlighted = this.getHighlightedRow();
    final boolean configureVisible = TvvlrMultimod.getAdditionMode() == AdditionMode.CUSTOM;

    if (highlighted == this.freeEntryRow) {
      this.selectRow(6);
      playMenuSound(1);
    } else if (highlighted == this.racingRow) {
      // Skip racingHeader (row 5) -> jump to themeRow (row 4)
      this.selectRow(4);
      playMenuSound(1);
    } else if (highlighted == this.themeRow) {
      if (configureVisible) {
        this.selectRow(3);
      } else {
        this.selectRow(2);
      }
      playMenuSound(1);
    } else if (highlighted == this.configureRow) {
      this.selectRow(2);
      playMenuSound(1);
    } else if (highlighted == this.modeRow) {
      this.selectRow(1);
      playMenuSound(1);
    } else if (highlighted == this.additionsRow) {
      // Top selectable row
    } else {
      this.selectRow(1);
      playMenuSound(1);
    }
  }

  private void navigateDown() {
    final Label highlighted = this.getHighlightedRow();
    final boolean configureVisible = TvvlrMultimod.getAdditionMode() == AdditionMode.CUSTOM;

    if (highlighted == this.additionsRow) {
      this.selectRow(2);
      playMenuSound(1);
    } else if (highlighted == this.modeRow) {
      if (configureVisible) {
        this.selectRow(3);
      } else {
        this.selectRow(4);
      }
      playMenuSound(1);
    } else if (highlighted == this.configureRow) {
      this.selectRow(4);
      playMenuSound(1);
    } else if (highlighted == this.themeRow) {
      // Skip racingHeader (row 5) -> jump to racingRow (row 6)
      this.selectRow(6);
      playMenuSound(1);
    } else if (highlighted == this.racingRow) {
      this.selectRow(7);
      playMenuSound(1);
    } else if (highlighted == this.freeEntryRow) {
      // Bottom selectable row
    } else {
      this.selectRow(1);
      playMenuSound(1);
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

    if (after == this.additionsHeader || after == this.racingHeader) {
      if (before != null && before != this.additionsHeader && before != this.racingHeader) {
        if (before == this.additionsRow) this.selectRow(1);
        else if (before == this.modeRow) this.selectRow(2);
        else if (before == this.configureRow) this.selectRow(3);
        else if (before == this.themeRow) this.selectRow(4);
        else if (before == this.racingRow) this.selectRow(6);
        else if (before == this.freeEntryRow) this.selectRow(7);
      } else {
        this.selectRow(1);
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
