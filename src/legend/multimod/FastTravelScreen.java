package legend.multimod;

import legend.core.lang.I18nText;
import legend.core.lang.RawText;
import legend.core.platform.input.InputAction;
import legend.game.inventory.WhichMenu;
import legend.game.inventory.screens.InputPropagation;
import legend.game.inventory.screens.MessageBoxScreen;
import legend.game.inventory.screens.TextColour;
import legend.game.inventory.screens.VerticalLayoutScreen;
import legend.game.inventory.screens.controls.Background;
import legend.game.inventory.screens.controls.Button;
import legend.game.inventory.screens.controls.Label;
import legend.game.modding.coremod.CoreMod;
import legend.game.types.MessageBoxResult;
import legend.game.types.MessageBoxType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

import static legend.game.Menus.whichMenu_800bdc38;
import static legend.game.SItem.menuStack;
import static legend.game.Scus94491BpeSegment_8005.collidedPrimitiveIndex_80052c38;
import static legend.game.Scus94491BpeSegment_8005.submapCut_80052c30;
import static legend.game.Scus94491BpeSegment_8005.submapScene_80052c34;
import static legend.game.Scus94491BpeSegment_800b.gameState_800babc8;
import static legend.game.sound.Audio.playMenuSound;

public class FastTravelScreen extends VerticalLayoutScreen {
  private static final Logger LOGGER = LogManager.getFormatterLogger(FastTravelScreen.class);

  private final Runnable unload;

  public FastTravelScreen(final Runnable unload) {
    this.unload = unload;

    this.addControl(new Background());

    final Label title = this.addControl(new Label(new RawText("Fast Travel")));
    title.setPos(this.getPadding(), 14);
    title.setScale(0.85f);
    title.getFontOptions().colour(TextColour.GOLD);

    final List<FastTravelLocations.Location> visited = FastTravelLocations.getVisitedLocations();
    if (visited.isEmpty()) {
      this.addRow(new RawText("No visited locations"), null);
    } else {
      for (final FastTravelLocations.Location loc : visited) {
        final Button warpBtn = new Button(new RawText("Warp"));
        warpBtn.onPressed(() -> this.promptWarp(loc));
        this.addRow(new RawText(loc.displayName), warpBtn);
      }
    }

    this.addHotkey(new I18nText("lod_core.ui.back"), CoreMod.INPUT_ACTION_MENU_BACK, this.unload);
  }

  @Override
  protected InputPropagation inputActionPressed(final InputAction action, final boolean repeat) {
    if (action == CoreMod.INPUT_ACTION_MENU_BACK.get() && !repeat) {
      playMenuSound(0);
      this.unload.run();
      return InputPropagation.HANDLED;
    }
    return super.inputActionPressed(action, repeat);
  }

  private void promptWarp(final FastTravelLocations.Location loc) {
    menuStack.pushScreen(new MessageBoxScreen("Warp to " + loc.displayName + "?", MessageBoxType.CONFIRMATION, result -> {
      if (result == MessageBoxResult.YES) {
        this.doWarp(loc);
      }
    }));
  }

  private void doWarp(final FastTravelLocations.Location loc) {
    final int cut = loc.getTargetCut();
    final int scene = loc.getTargetScene();
    LOGGER.info("FastTravelScreen: Warping to %s (cut=%d, scene=%d)", loc.displayName, cut, scene);

    // Save destination coordinates
    if (gameState_800babc8 != null) {
      gameState_800babc8.submapCut_a8 = cut;
      gameState_800babc8.submapScene_a4 = scene;
    }

    submapCut_80052c30 = cut;
    submapScene_80052c34 = scene;
    collidedPrimitiveIndex_80052c38 = scene;

    // Queue pending warp to execute after the menu cleanly finishes unloading
    FastTravelManager.pendingWarp = loc;

    // Close the pause menu
    whichMenu_800bdc38 = WhichMenu.UNLOAD;
  }
}
