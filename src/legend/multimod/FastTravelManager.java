package legend.multimod;

import legend.core.lang.RawText;
import legend.game.SItem;
import legend.game.inventory.WhichMenu;
import legend.game.inventory.screens.MainMenuScreen;
import legend.game.inventory.screens.MenuScreen;
import legend.game.inventory.screens.MenuStack;
import legend.game.inventory.screens.TextColour;
import legend.game.inventory.screens.controls.Button;
import legend.game.submap.SMap;
import legend.game.submap.SubmapState;
import legend.game.types.Model124;
import legend.game.wmap.WMap;
import legend.game.wmap.WMapModelAndAnimData258;
import legend.lodmod.LodEngineStateTypes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.Deque;
import java.util.List;

import static legend.game.EngineStates.currentEngineState_8004dd04;
import static legend.game.Menus.whichMenu_800bdc38;
import static legend.game.Scus94491BpeSegment_8005.collidedPrimitiveIndex_80052c38;
import static legend.game.Scus94491BpeSegment_8005.submapCut_80052c30;
import static legend.game.Scus94491BpeSegment_8005.submapScene_80052c34;
import static legend.game.sound.Audio.playMenuSound;

public final class FastTravelManager {
  private static final Logger LOGGER = LogManager.getFormatterLogger(FastTravelManager.class);

  public static FastTravelLocations.Location pendingWarp;

  private static MainMenuScreen lastHookedScreen;
  private static Field menuButtonsField;
  private static Field loadingStageField;
  private static Field screensField;
  private static Field smapLoadingStageField;
  private static Field smapTransitioningField;
  private static Field wmapModelAndAnimDataField;

  static {
    try {
      menuButtonsField = MainMenuScreen.class.getDeclaredField("menuButtons");
      menuButtonsField.setAccessible(true);
      loadingStageField = MainMenuScreen.class.getDeclaredField("loadingStage");
      loadingStageField.setAccessible(true);
    } catch (final Throwable t) {
      LOGGER.error("FastTravelManager: Failed to reflect MainMenuScreen fields", t);
    }

    try {
      screensField = MenuStack.class.getDeclaredField("screens");
      screensField.setAccessible(true);
    } catch (final Throwable t) {
      LOGGER.error("FastTravelManager: Failed to reflect MenuStack.screens", t);
    }

    try {
      smapLoadingStageField = SMap.class.getDeclaredField("smapLoadingStage_800cb430");
      smapLoadingStageField.setAccessible(true);
      smapTransitioningField = SMap.class.getDeclaredField("transitioning_800f7e4c");
      smapTransitioningField.setAccessible(true);
    } catch (final Throwable t) {
      LOGGER.error("FastTravelManager: Failed to reflect SMap transition fields", t);
    }

    try {
      wmapModelAndAnimDataField = WMap.class.getDeclaredField("modelAndAnimData_800c66a8");
      wmapModelAndAnimDataField.setAccessible(true);
    } catch (final Throwable t) {
      LOGGER.error("FastTravelManager: Failed to reflect WMap.modelAndAnimData_800c66a8", t);
    }
  }

  private FastTravelManager() { }

  public static void onRender() {
    if (!TvvlrMultimod.isFastTravelEnabled()) {
      return;
    }

    // Process pending warp once menu is closed
    if (pendingWarp != null && whichMenu_800bdc38 == WhichMenu.NONE_0) {
      final FastTravelLocations.Location loc = pendingWarp;
      pendingWarp = null;
      executeWarp(loc);
    }

    // Hook MainMenuScreen button 11
    final MenuScreen currentScreen = getCurrentScreen();
    if (currentScreen instanceof MainMenuScreen mainMenuScreen) {
      if (mainMenuScreen != lastHookedScreen) {
        lastHookedScreen = mainMenuScreen;
        hookMainMenu(mainMenuScreen);
      }
    }
  }

  private static MenuScreen getCurrentScreen() {
    try {
      if (screensField != null && SItem.menuStack != null) {
        @SuppressWarnings("unchecked")
        final Deque<MenuScreen> screens = (Deque<MenuScreen>) screensField.get(SItem.menuStack);
        if (screens != null) {
          return screens.peek();
        }
      }
    } catch (final Throwable ignored) {
    }
    return null;
  }

  private static void hookMainMenu(final MainMenuScreen screen) {
    if (menuButtonsField == null) {
      return;
    }

    try {
      @SuppressWarnings("unchecked")
      final List<Button> buttons = (List<Button>) menuButtonsField.get(screen);
      if (buttons != null && buttons.size() > 11) {
        final Button ftButton = buttons.get(11);
        ftButton.setText(new RawText("Fast Travel"));
        ftButton.setTextColour(TextColour.BROWN);
        ftButton.show();
        ftButton.onPressed(() -> {
          try {
            if (loadingStageField != null && loadingStageField.getInt(screen) != 2) {
              return;
            }
          } catch (final Throwable ignored) {
            return;
          }
          playMenuSound(2);
          SItem.menuStack.pushScreen(new FastTravelScreen(() -> SItem.menuStack.popScreen()));
        });
        LOGGER.info("FastTravelManager: Successfully hooked MainMenuScreen button 11 for Fast Travel.");
      }
    } catch (final Throwable t) {
      LOGGER.error("FastTravelManager: Error hooking MainMenuScreen", t);
    }
  }

  public static void executeWarp(final FastTravelLocations.Location loc) {
    final int cut = loc.getTargetCut();
    final int scene = loc.getTargetScene();
    LOGGER.info("FastTravelManager: Executing warp to %s (cut=%d, scene=%d)", loc.displayName, cut, scene);
    submapCut_80052c30 = cut;
    submapScene_80052c34 = scene;
    collidedPrimitiveIndex_80052c38 = scene;

    if (currentEngineState_8004dd04 instanceof SMap smap) {
      try {
        if (smapLoadingStageField != null) {
          smapLoadingStageField.set(smap, SubmapState.RENDER_SUBMAP_12);
        }
        if (smapTransitioningField != null) {
          smapTransitioningField.setBoolean(smap, false);
        }
      } catch (final Throwable ignored) {
      }
      smap.mapTransition(cut, scene);
    } else if (currentEngineState_8004dd04 instanceof WMap wmap) {
      if (wmapModelAndAnimDataField != null) {
        try {
          final WMapModelAndAnimData258 data = (WMapModelAndAnimData258) wmapModelAndAnimDataField.get(wmap);
          if (data != null) {
            if (data.shadowObj == null) {
              data.shadowObj = new FastTravelDummyObj();
            }
            for (int i = 0; i < 4; i++) {
              if (data.models_0c[i] == null) {
                data.models_0c[i] = new Model124("FastTravel Dummy");
              }
            }
          }
        } catch (final Throwable t) {
          LOGGER.error("FastTravelManager: Failed to safeguard WMap player models", t);
        }
      }
      wmap.transitionToEngineState(LodEngineStateTypes.SUBMAP.get());
    }
  }
}
