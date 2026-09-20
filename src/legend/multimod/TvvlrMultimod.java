package legend.multimod;

import legend.core.lang.I18nText;
import legend.core.lang.RawText;
import legend.core.platform.input.ButtonInputActivation;
import legend.core.platform.input.InputAction;
import legend.core.platform.input.InputActionRegistryEvent;
import legend.core.platform.input.InputButton;
import legend.core.platform.input.InputKey;
import legend.core.platform.input.ScancodeInputActivation;
import legend.game.EngineStates;
import legend.game.SItem;
import legend.game.combat.Battle;
import legend.game.combat.SEffe;
import legend.game.combat.effects.AdditionOverlaysEffect44;
import legend.game.combat.effects.EffectManagerData6c;
import legend.game.inventory.screens.MenuScreen;
import legend.game.inventory.screens.MenuStack;
import legend.game.inventory.screens.OptionsCategoryScreen;
import legend.game.inventory.screens.controls.Button;
import legend.game.modding.coremod.CoreMod;
import legend.game.modding.events.RenderEvent;
import legend.game.modding.events.battle.BattleEndedEvent;
import legend.game.modding.events.battle.BattleStartedEvent;
import legend.game.modding.events.engine.EngineStateChangeEvent;
import legend.game.modding.events.input.RegisterDefaultInputBindingsEvent;
import legend.game.modding.events.submap.SubmapLoadEvent;
import legend.game.saves.BoolConfigEntry;
import legend.game.saves.ConfigCategory;
import legend.game.saves.ConfigEntry;
import legend.game.saves.ConfigRegistryEvent;
import legend.game.saves.ConfigStorage;
import legend.game.saves.ConfigStorageLocation;
import legend.game.saves.EnumConfigEntry;
import legend.game.scripting.ScriptState;
import legend.game.submap.RetailSubmap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.legendofdragoon.modloader.Mod;
import org.legendofdragoon.modloader.events.EventListener;
import org.legendofdragoon.modloader.registries.Registrar;
import org.legendofdragoon.modloader.registries.RegistryDelegate;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Deque;
import java.util.Set;
import java.util.WeakHashMap;

import static legend.core.GameEngine.CONFIG;
import static legend.core.GameEngine.EVENTS;
import static legend.core.GameEngine.REGISTRIES;
import static legend.core.GameEngine.SCRIPTS;
import static legend.game.FullScreenEffects.startFadeEffect;
import static legend.game.Scus94491BpeSegment_8004.engineStateFunctions_8004e29c;

@Mod(id = TvvlrMultimod.MOD_ID, version = "^3.0.0")
public class TvvlrMultimod {
  public static final String MOD_ID = "tvvlr_multimod";
  private static final Logger LOGGER = LogManager.getFormatterLogger(TvvlrMultimod.class);

  // Input actions
  private static final Registrar<InputAction, InputActionRegistryEvent> INPUT_ACTION_REGISTRAR =
    new Registrar<>(REGISTRIES.inputActions, MOD_ID);

  public static final RegistryDelegate<InputAction> INPUT_ACTION_ADDITION_SQUARE =
    INPUT_ACTION_REGISTRAR.register("addition_square", InputAction::editable);

  public static final RegistryDelegate<InputAction> INPUT_ACTION_ADDITION_TRIANGLE =
    INPUT_ACTION_REGISTRAR.register("addition_triangle", InputAction::editable);

  // Config registrar
  private static final Registrar<ConfigEntry<?>, ConfigRegistryEvent> CONFIG_REGISTRAR =
    new Registrar<>(REGISTRIES.config, MOD_ID);

  public static final RegistryDelegate<BoolConfigEntry> ADVANCED_ADDITIONS_ENABLED =
    CONFIG_REGISTRAR.register("advanced_additions_enabled",
      () -> new BoolConfigEntry(true, ConfigStorageLocation.GLOBAL, ConfigCategory.GAMEPLAY, false));

  public static final RegistryDelegate<BoolConfigEntry> RACING_MINIGAME_ENABLED =
    CONFIG_REGISTRAR.register("racing_minigame_enabled",
      () -> new BoolConfigEntry(true, ConfigStorageLocation.GLOBAL, ConfigCategory.GAMEPLAY, false));

  public static final RegistryDelegate<BoolConfigEntry> FREE_RACE_ENTRY =
    CONFIG_REGISTRAR.register("free_race_entry",
      () -> new BoolConfigEntry(false, ConfigStorageLocation.GLOBAL, ConfigCategory.GAMEPLAY, false));

  public static final RegistryDelegate<EnumConfigEntry<ControllerTheme>> CONTROLLER_THEME =
    CONFIG_REGISTRAR.register("controller_theme",
      () -> new EnumConfigEntry<>(ControllerTheme.class, ControllerTheme.AUTO, ConfigStorageLocation.GLOBAL, ConfigCategory.GAMEPLAY));

  // Fallback values if config is unavailable
  private static boolean advancedAdditionsEnabledFallback = true;
  private static boolean racingMinigameEnabledFallback = true;
  private static boolean freeRaceEntryFallback = false;
  private static ControllerTheme controllerThemeFallback = ControllerTheme.AUTO;

  // Options menu tracking
  private static final Set<OptionsCategoryScreen> HOOKED_OPTIONS_SCREENS =
    Collections.newSetFromMap(new WeakHashMap<>());
  private static Field screensField = null;

  public TvvlrMultimod() {
    EVENTS.register(this);
    TasmanBattleTutorial.init();
    LOGGER.info("TvvlrMultimod: Initialized and registered with EVENTS.");
  }

  // --- Feature Toggles and Config API ---

  public static boolean isAdvancedAdditionsEnabled() {
    try {
      if (CONFIG != null && ADVANCED_ADDITIONS_ENABLED != null && ADVANCED_ADDITIONS_ENABLED.isValid()) {
        return CONFIG.getConfig(ADVANCED_ADDITIONS_ENABLED.get());
      }
    } catch (final Throwable t) {
      LOGGER.warn("TvvlrMultimod: Error checking if advanced additions enabled", t);
    }
    return advancedAdditionsEnabledFallback;
  }

  public static void setAdvancedAdditionsEnabled(final boolean enabled) {
    advancedAdditionsEnabledFallback = enabled;
    try {
      if (CONFIG != null && ADVANCED_ADDITIONS_ENABLED != null && ADVANCED_ADDITIONS_ENABLED.isValid()) {
        CONFIG.setConfig(ADVANCED_ADDITIONS_ENABLED.get(), enabled);
      }
      persistConfig();
      LOGGER.info("TvvlrMultimod: Advanced Additions set to %b", enabled);
    } catch (final Throwable t) {
      LOGGER.warn("TvvlrMultimod: Error setting advanced additions enabled", t);
    }
  }

  public static boolean isRacingMinigameEnabled() {
    try {
      if (CONFIG != null && RACING_MINIGAME_ENABLED != null && RACING_MINIGAME_ENABLED.isValid()) {
        return CONFIG.getConfig(RACING_MINIGAME_ENABLED.get());
      }
    } catch (final Throwable t) {
      LOGGER.warn("TvvlrMultimod: Error checking if racing minigame enabled", t);
    }
    return racingMinigameEnabledFallback;
  }

  public static void setRacingMinigameEnabled(final boolean enabled) {
    racingMinigameEnabledFallback = enabled;
    try {
      if (CONFIG != null && RACING_MINIGAME_ENABLED != null && RACING_MINIGAME_ENABLED.isValid()) {
        CONFIG.setConfig(RACING_MINIGAME_ENABLED.get(), enabled);
      }
      persistConfig();
      LOGGER.info("TvvlrMultimod: Racing Minigame set to %b", enabled);
    } catch (final Throwable t) {
      LOGGER.warn("TvvlrMultimod: Error setting racing minigame enabled", t);
    }
  }

  public static boolean isFreeRaceEntryEnabled() {
    try {
      if (CONFIG != null && FREE_RACE_ENTRY != null && FREE_RACE_ENTRY.isValid()) {
        return CONFIG.getConfig(FREE_RACE_ENTRY.get());
      }
    } catch (final Throwable t) {
      LOGGER.warn("TvvlrMultimod: Error checking if free race entry enabled", t);
    }
    return freeRaceEntryFallback;
  }

  public static void setFreeRaceEntryEnabled(final boolean enabled) {
    freeRaceEntryFallback = enabled;
    try {
      if (CONFIG != null && FREE_RACE_ENTRY != null && FREE_RACE_ENTRY.isValid()) {
        CONFIG.setConfig(FREE_RACE_ENTRY.get(), enabled);
      }
      persistConfig();
      LOGGER.info("TvvlrMultimod: Free race entry set to %b", enabled);
    } catch (final Throwable t) {
      LOGGER.warn("TvvlrMultimod: Error setting free race entry", t);
    }
  }

  public static ControllerTheme getControllerTheme() {
    try {
      if (CONFIG != null && CONTROLLER_THEME != null && CONTROLLER_THEME.isValid()) {
        final ControllerTheme theme = CONFIG.getConfig(CONTROLLER_THEME.get());
        if (theme != null) {
          return theme;
        }
      }
    } catch (final Throwable ignored) {
    }
    return controllerThemeFallback;
  }

  public static void setControllerTheme(final ControllerTheme theme) {
    controllerThemeFallback = theme;
    try {
      if (CONFIG != null && CONTROLLER_THEME != null && CONTROLLER_THEME.isValid()) {
        CONFIG.setConfig(CONTROLLER_THEME.get(), theme);
      }
      persistConfig();
      LOGGER.info("TvvlrMultimod: Controller theme set to %s", theme);
    } catch (final Throwable t) {
      LOGGER.warn("TvvlrMultimod: Error setting controller theme", t);
    }
  }

  public static ControllerTheme cycleControllerTheme() {
    return cycleControllerTheme(1);
  }

  public static ControllerTheme cycleControllerTheme(final int direction) {
    final ControllerTheme current = getControllerTheme();
    final ControllerTheme[] themes = ControllerTheme.values();
    final int nextIndex = (current.ordinal() + direction % themes.length + themes.length) % themes.length;
    final ControllerTheme next = themes[nextIndex];
    setControllerTheme(next);
    return next;
  }


  private static void persistConfig() {
    try {
      if (CONFIG != null) {
        ConfigStorage.saveConfig(CONFIG, ConfigStorageLocation.GLOBAL, Path.of("config.dcnf"));
      }
    } catch (final Throwable t) {
      LOGGER.warn("TvvlrMultimod: Error saving config to config.dcnf", t);
    }
  }

  // --- Mod Events ---

  @EventListener
  public void registerInputActions(final InputActionRegistryEvent event) {
    INPUT_ACTION_REGISTRAR.registryEvent(event);
    LOGGER.info("TvvlrMultimod: Registered input actions.");
  }

  @EventListener
  public void registerDefaultInputBindings(final RegisterDefaultInputBindingsEvent event) {
    if (INPUT_ACTION_ADDITION_SQUARE.isValid() && INPUT_ACTION_ADDITION_TRIANGLE.isValid()) {
      event
        .add(INPUT_ACTION_ADDITION_SQUARE.get(), new ButtonInputActivation(InputButton.X))
        .add(INPUT_ACTION_ADDITION_SQUARE.get(), new ScancodeInputActivation(InputKey.A))
        .add(INPUT_ACTION_ADDITION_SQUARE.get(), new ScancodeInputActivation(InputKey.Z))
        .add(INPUT_ACTION_ADDITION_TRIANGLE.get(), new ButtonInputActivation(InputButton.Y))
        .add(INPUT_ACTION_ADDITION_TRIANGLE.get(), new ScancodeInputActivation(InputKey.W))
        .add(INPUT_ACTION_ADDITION_TRIANGLE.get(), new ScancodeInputActivation(InputKey.E));
      LOGGER.info("TvvlrMultimod: Registered default input bindings.");
    }
  }

  @EventListener
  public void registerConfig(final ConfigRegistryEvent event) {
    CONFIG_REGISTRAR.registryEvent(event);
    LOGGER.info("TvvlrMultimod: Registered config.");
  }

  @EventListener
  public void onEngineStateChange(final EngineStateChangeEvent event) {
    if (event.engineState instanceof Battle) {
      if (engineStateFunctions_8004e29c != null) {
        if (isAdvancedAdditionsEnabled()) {
          engineStateFunctions_8004e29c[753] = AdvancedAdditionOverlaysEffect.ALLOCATOR;
          LOGGER.info("TvvlrMultimod: Hooked script opcode 753 on EngineStateChange.");
        } else {
          engineStateFunctions_8004e29c[753] = SEffe::scriptAllocateAdditionOverlaysEffect;
        }
      }
    }
  }

  @EventListener
  public void onBattleStarted(final BattleStartedEvent event) {
    if (isAdvancedAdditionsEnabled()) {
      TasmanBattleTutorial.onBattleStarted(event);
    }
  }

  @EventListener
  public void onBattleEnded(final BattleEndedEvent event) {
    if (isAdvancedAdditionsEnabled()) {
      TasmanBattleTutorial.onBattleEnded(event);
    }
  }

  @EventListener
  public void onSubmapLoad(final SubmapLoadEvent event) {
    if (isRacingMinigameEnabled() && event.getSubmap() instanceof RetailSubmap retail) {
      LohanRaceNpc.onSubmapLoad(event.getEngineState(), retail, event.submapObjects);
      LohanRaceManager.onSubmapLoad(event.getEngineState(), retail, event.submapObjects);
    }
  }

  @EventListener
  public void onRender(final RenderEvent event) {
    // 1. Check and hook active OptionsCategoryScreen on SItem.menuStack
    checkOptionsMenuHook();

    // 2. Advanced Additions Render
    if (isAdvancedAdditionsEnabled()) {
      TasmanBattleTutorial.onRender(event);

      if (EngineStates.currentEngineState_8004dd04 instanceof Battle) {
        if (engineStateFunctions_8004e29c != null && engineStateFunctions_8004e29c[753] != AdvancedAdditionOverlaysEffect.ALLOCATOR) {
          engineStateFunctions_8004e29c[753] = AdvancedAdditionOverlaysEffect.ALLOCATOR;
          LOGGER.info("TvvlrMultimod: Hooked script opcode 753 on Render.");
        }
      }

      if (SEffe.additionOverlayActive_80119f41 != 0 && SCRIPTS != null) {
        final int scriptCount = SCRIPTS.count();
        for (int i = 0; i < scriptCount; i++) {
          final ScriptState<?> state = SCRIPTS.getState(i);
          if (state != null && state.innerStruct_00 instanceof final EffectManagerData6c<?> manager) {
            if (manager.effect_44 != null && manager.effect_44.getClass() == AdditionOverlaysEffect44.class) {
              ((EffectManagerData6c) manager).effect_44 = new AdvancedAdditionOverlaysEffect((AdditionOverlaysEffect44) manager.effect_44, state);
              LOGGER.info("TvvlrMultimod: Upgraded vanilla AdditionOverlaysEffect44 to AdvancedAdditionOverlaysEffect on script %d", i);
            }
          }
        }
      }
    }

    // 3. Racing Minigame Render
    if (isRacingMinigameEnabled()) {
      LohanRaceNpc.onRender();
      LohanRaceManager.onRender();
    }
  }

  // --- Options Menu Attachment ---

  private static void checkOptionsMenuHook() {
    try {
      if (SItem.menuStack == null) {
        return;
      }
      if (screensField == null) {
        screensField = MenuStack.class.getDeclaredField("screens");
        screensField.setAccessible(true);
      }
      @SuppressWarnings("unchecked")
      final Deque<MenuScreen> screens = (Deque<MenuScreen>) screensField.get(SItem.menuStack);
      if (screens != null && !screens.isEmpty()) {
        final MenuScreen top = screens.peek();
        if (top instanceof OptionsCategoryScreen categoryScreen) {
          if (HOOKED_OPTIONS_SCREENS.add(categoryScreen)) {
            attachMultimodMenuOption(categoryScreen);
          }
        }
      }
    } catch (final Throwable t) {
      LOGGER.warn("TvvlrMultimod: Exception checking menu stack", t);
    }
  }

  private static void attachMultimodMenuOption(final OptionsCategoryScreen categoryScreen) {
    try {
      final Button button = new Button(new I18nText(CoreMod.MOD_ID + ".config.category.configure"));
      button.onPressed(() -> {
        categoryScreen.getStack().pushScreen(new MultimodOptionsScreen(() -> {
          startFadeEffect(2, 10);
          SItem.menuStack.popScreen();
        }));
      });

      categoryScreen.addRow(new RawText("Tvvlr's Multimod"), button);
      LOGGER.info("TvvlrMultimod: Successfully injected 'Tvvlr's Multimod' options row.");
    } catch (final Throwable t) {
      LOGGER.error("TvvlrMultimod: Failed to attach multimod menu option", t);
    }
  }
}
