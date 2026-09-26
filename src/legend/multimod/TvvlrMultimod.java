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
import legend.game.modding.events.battle.BattleEntityTurnEvent;
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
import legend.game.submap.SMap;
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

import legend.game.combat.bent.BattleEntity27c;
import legend.game.combat.bent.PlayerBattleEntity;
import legend.game.combat.postbattleactions.VictoryPostBattleAction;
import legend.game.combat.postbattleactions.VictoryPostBattleActionInstance;
import legend.game.combat.types.CombatantAsset0c;
import legend.game.types.TmdAnimationFile;

import static legend.core.GameEngine.CONFIG;
import static legend.core.GameEngine.EVENTS;
import static legend.core.GameEngine.PLATFORM;
import static legend.core.GameEngine.REGISTRIES;
import static legend.core.GameEngine.SCRIPTS;
import static legend.game.FullScreenEffects.startFadeEffect;
import static legend.game.Models.loadModelStandardAnimation;
import static legend.game.Scus94491BpeSegment_8006.battleState_8006e398;
import static legend.game.Scus94491BpeSegment_800b.postBattleAction_800bc974;
import static legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_BACK;
import static legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_CONFIRM;
import static legend.lodmod.LodMod.INPUT_ACTION_BTTL_ATTACK;

import legend.game.modding.events.submap.SubmapEncounterEvent;
import legend.game.modding.events.submap.SubmapWarpEvent;

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

  public static final RegistryDelegate<BoolConfigEntry> RANDOM_ADDITIONS_ENABLED =
    CONFIG_REGISTRAR.register("random_additions_enabled",
      () -> new BoolConfigEntry(false, ConfigStorageLocation.GLOBAL, ConfigCategory.GAMEPLAY, false));

  public static final RegistryDelegate<BoolConfigEntry> RACING_MINIGAME_ENABLED =
    CONFIG_REGISTRAR.register("racing_minigame_enabled",
      () -> new BoolConfigEntry(true, ConfigStorageLocation.GLOBAL, ConfigCategory.GAMEPLAY, false));

  public static final RegistryDelegate<BoolConfigEntry> FREE_RACE_ENTRY =
    CONFIG_REGISTRAR.register("free_race_entry",
      () -> new BoolConfigEntry(false, ConfigStorageLocation.GLOBAL, ConfigCategory.GAMEPLAY, false));

  public static final RegistryDelegate<EnumConfigEntry<ControllerTheme>> CONTROLLER_THEME =
    CONFIG_REGISTRAR.register("controller_theme",
      () -> new EnumConfigEntry<>(ControllerTheme.class, ControllerTheme.AUTO, ConfigStorageLocation.GLOBAL, ConfigCategory.GAMEPLAY));

  public static final RegistryDelegate<EnumConfigEntry<AdditionMode>> ADDITION_MODE =
    CONFIG_REGISTRAR.register("addition_mode",
      () -> new EnumConfigEntry<>(AdditionMode.class, AdditionMode.DEFAULT, ConfigStorageLocation.GLOBAL, ConfigCategory.GAMEPLAY));

  public static final RegistryDelegate<BoolConfigEntry> DANCE_MODIFIER_ENABLED =
    CONFIG_REGISTRAR.register("dance_modifier_enabled",
      () -> new BoolConfigEntry(true, ConfigStorageLocation.GLOBAL, ConfigCategory.GAMEPLAY, false));

  public static final RegistryDelegate<BoolConfigEntry> TASMAN_PRACTICE_ENABLED =
    CONFIG_REGISTRAR.register("tasman_practice_enabled",
      () -> new BoolConfigEntry(true, ConfigStorageLocation.GLOBAL, ConfigCategory.GAMEPLAY, false));

  public static final RegistryDelegate<BoolConfigEntry> FAST_TRAVEL_ENABLED =
    CONFIG_REGISTRAR.register("fast_travel_enabled",
      () -> new BoolConfigEntry(true, ConfigStorageLocation.GLOBAL, ConfigCategory.GAMEPLAY, false));

  public static final RegistryDelegate<BoolConfigEntry> FAST_TRAVEL_UNLOCK_ALL =
    CONFIG_REGISTRAR.register("fast_travel_unlock_all",
      () -> new BoolConfigEntry(false, ConfigStorageLocation.GLOBAL, ConfigCategory.GAMEPLAY, false));

  // Fallback values if config is unavailable
  private static boolean advancedAdditionsEnabledFallback = true;
  private static boolean randomAdditionsEnabledFallback = false;
  private static boolean racingMinigameEnabledFallback = true;
  private static boolean freeRaceEntryFallback = false;
  private static boolean danceModifierEnabledFallback = true;
  private static boolean tasmanPracticeEnabledFallback = true;
  private static boolean fastTravelEnabledFallback = true;
  private static boolean fastTravelUnlockAllFallback = false;
  private static ControllerTheme controllerThemeFallback = ControllerTheme.AUTO;
  private static AdditionMode additionModeFallback = AdditionMode.DEFAULT;
  private static boolean victoryDancesApplied = false;

  // Active battle tracking
  public static Battle currentBattle = null;

  // Options menu tracking
  private static final Set<OptionsCategoryScreen> HOOKED_OPTIONS_SCREENS =
    Collections.newSetFromMap(new WeakHashMap<>());
  private static Field screensField = null;

  public TvvlrMultimod() {
    EVENTS.register(this);
    TasmanBattleTutorial.init();
    TasmanPracticeBattle.initScriptHooks();
    CustomAdditionsStorage.load();
    DanceModifierStorage.load();
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

  public static boolean isRandomAdditionsEnabled() {
    try {
      if (CONFIG != null && RANDOM_ADDITIONS_ENABLED != null && RANDOM_ADDITIONS_ENABLED.isValid()) {
        return CONFIG.getConfig(RANDOM_ADDITIONS_ENABLED.get());
      }
    } catch (final Throwable t) {
      LOGGER.warn("TvvlrMultimod: Error checking if random additions enabled", t);
    }
    return randomAdditionsEnabledFallback;
  }

  public static void setRandomAdditionsEnabled(final boolean enabled) {
    randomAdditionsEnabledFallback = enabled;
    try {
      if (CONFIG != null && RANDOM_ADDITIONS_ENABLED != null && RANDOM_ADDITIONS_ENABLED.isValid()) {
        CONFIG.setConfig(RANDOM_ADDITIONS_ENABLED.get(), enabled);
      }
      persistConfig();
      LOGGER.info("TvvlrMultimod: Random Additions set to %b", enabled);
    } catch (final Throwable t) {
      LOGGER.warn("TvvlrMultimod: Error setting random additions enabled", t);
    }
  }

  public static boolean isTasmanPracticeEnabled() {
    try {
      if (CONFIG != null && TASMAN_PRACTICE_ENABLED != null && TASMAN_PRACTICE_ENABLED.isValid()) {
        return CONFIG.getConfig(TASMAN_PRACTICE_ENABLED.get());
      }
    } catch (final Throwable t) {
      LOGGER.warn("TvvlrMultimod: Error checking if tasman practice enabled", t);
    }
    return tasmanPracticeEnabledFallback;
  }

  public static void setTasmanPracticeEnabled(final boolean enabled) {
    tasmanPracticeEnabledFallback = enabled;
    try {
      if (CONFIG != null && TASMAN_PRACTICE_ENABLED != null && TASMAN_PRACTICE_ENABLED.isValid()) {
        CONFIG.setConfig(TASMAN_PRACTICE_ENABLED.get(), enabled);
      }
      if (!enabled) {
        TasmanNpcManager.cleanupNpc(EngineStates.currentEngineState_8004dd04 instanceof SMap smap ? smap : null);
      }
      persistConfig();
      LOGGER.info("TvvlrMultimod: Master Tasman Practice set to %b", enabled);
    } catch (final Throwable t) {
      LOGGER.warn("TvvlrMultimod: Error setting tasman practice enabled", t);
    }
  }

  public static boolean isFastTravelEnabled() {
    try {
      if (CONFIG != null && FAST_TRAVEL_ENABLED != null && FAST_TRAVEL_ENABLED.isValid()) {
        return CONFIG.getConfig(FAST_TRAVEL_ENABLED.get());
      }
    } catch (final Throwable t) {
      LOGGER.warn("TvvlrMultimod: Error checking if fast travel enabled", t);
    }
    return fastTravelEnabledFallback;
  }

  public static void setFastTravelEnabled(final boolean enabled) {
    fastTravelEnabledFallback = enabled;
    try {
      if (CONFIG != null && FAST_TRAVEL_ENABLED != null && FAST_TRAVEL_ENABLED.isValid()) {
        CONFIG.setConfig(FAST_TRAVEL_ENABLED.get(), enabled);
      }
      persistConfig();
      LOGGER.info("TvvlrMultimod: Fast Travel set to %b", enabled);
    } catch (final Throwable t) {
      LOGGER.warn("TvvlrMultimod: Error setting fast travel enabled", t);
    }
  }

  public static boolean isFastTravelUnlockAll() {
    try {
      if (CONFIG != null && FAST_TRAVEL_UNLOCK_ALL != null && FAST_TRAVEL_UNLOCK_ALL.isValid()) {
        return CONFIG.getConfig(FAST_TRAVEL_UNLOCK_ALL.get());
      }
    } catch (final Throwable t) {
      LOGGER.warn("TvvlrMultimod: Error checking if fast travel unlock all enabled", t);
    }
    return fastTravelUnlockAllFallback;
  }

  public static void setFastTravelUnlockAll(final boolean enabled) {
    fastTravelUnlockAllFallback = enabled;
    try {
      if (CONFIG != null && FAST_TRAVEL_UNLOCK_ALL != null && FAST_TRAVEL_UNLOCK_ALL.isValid()) {
        CONFIG.setConfig(FAST_TRAVEL_UNLOCK_ALL.get(), enabled);
      }
      persistConfig();
      LOGGER.info("TvvlrMultimod: Fast Travel Unlock All set to %b", enabled);
    } catch (final Throwable t) {
      LOGGER.warn("TvvlrMultimod: Error setting fast travel unlock all", t);
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

  public static boolean isDanceModifierEnabled() {
    try {
      if (CONFIG != null && DANCE_MODIFIER_ENABLED != null && DANCE_MODIFIER_ENABLED.isValid()) {
        return CONFIG.getConfig(DANCE_MODIFIER_ENABLED.get());
      }
    } catch (final Throwable t) {
      LOGGER.warn("TvvlrMultimod: Error checking if dance modifier enabled", t);
    }
    return danceModifierEnabledFallback;
  }

  public static void setDanceModifierEnabled(final boolean enabled) {
    danceModifierEnabledFallback = enabled;
    try {
      if (CONFIG != null && DANCE_MODIFIER_ENABLED != null && DANCE_MODIFIER_ENABLED.isValid()) {
        CONFIG.setConfig(DANCE_MODIFIER_ENABLED.get(), enabled);
      }
      persistConfig();
      LOGGER.info("TvvlrMultimod: Dance Modifier set to %b", enabled);
    } catch (final Throwable t) {
      LOGGER.warn("TvvlrMultimod: Error setting dance modifier enabled", t);
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

  public static AdditionMode getAdditionMode() {
    try {
      if (CONFIG != null && ADDITION_MODE != null && ADDITION_MODE.isValid()) {
        final AdditionMode mode = CONFIG.getConfig(ADDITION_MODE.get());
        if (mode != null) {
          return mode;
        }
      }
    } catch (final Throwable ignored) {
    }
    return additionModeFallback;
  }

  public static void setAdditionMode(final AdditionMode mode) {
    additionModeFallback = mode;
    try {
      if (CONFIG != null && ADDITION_MODE != null && ADDITION_MODE.isValid()) {
        CONFIG.setConfig(ADDITION_MODE.get(), mode);
      }
      persistConfig();
      LOGGER.info("TvvlrMultimod: Addition Mode set to %s", mode);
    } catch (final Throwable t) {
      LOGGER.warn("TvvlrMultimod: Error setting addition mode", t);
    }
  }

  public static AdditionMode cycleAdditionMode(final int direction) {
    final AdditionMode current = getAdditionMode();
    final AdditionMode[] modes = AdditionMode.values();
    final int nextIndex = (current.ordinal() + direction % modes.length + modes.length) % modes.length;
    final AdditionMode next = modes[nextIndex];
    setAdditionMode(next);
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
    if (isTasmanPracticeEnabled()) {
      TasmanNpcManager.cleanupNpc(null);
    }
    if (event.engineState instanceof Battle) {
      if (isAdvancedAdditionsEnabled()) {
        ScriptHookUtil.hookEngineStateFunction(753, AdvancedAdditionOverlaysEffect.ALLOCATOR);
        LOGGER.info("TvvlrMultimod: Hooked script opcode 753 on EngineStateChange.");
      } else {
        ScriptHookUtil.hookEngineStateFunction(753, SEffe::scriptAllocateAdditionOverlaysEffect);
      }
    }
  }

  @EventListener
  public void onBattleStarted(final BattleStartedEvent event) {
    currentBattle = event.battle;
    victoryDancesApplied = false;
    if (isDanceModifierEnabled()) {
      preInjectVictoryAnimations(event.battle);
    }
    if (isAdvancedAdditionsEnabled()) {
      TasmanBattleTutorial.onBattleStarted(event);
      RandomAdditionsManager.onBattleStarted(event);
    }
    if (isTasmanPracticeEnabled()) {
      TasmanPracticeBattle.onBattleStarted(event);
    }
  }

  @EventListener
  public void onBattleEnded(final BattleEndedEvent event) {
    currentBattle = null;
    victoryDancesApplied = false;
    if (isAdvancedAdditionsEnabled()) {
      TasmanBattleTutorial.onBattleEnded(event);
      RandomAdditionsManager.onBattleEnded(event);
    }
    if (isTasmanPracticeEnabled()) {
      TasmanPracticeBattle.onBattleEnded(event);
    }
  }

  @EventListener
  public void onTurnStarted(final BattleEntityTurnEvent<?> event) {
    if (isAdvancedAdditionsEnabled()) {
      TasmanBattleTutorial.onTurnStarted(event);
      RandomAdditionsManager.onTurnStarted(event);
    }
  }

  @EventListener
  public void onSubmapLoad(final SubmapLoadEvent event) {
    if (isRacingMinigameEnabled() && event.getSubmap() instanceof RetailSubmap retail) {
      LohanRaceNpc.onSubmapLoad(event.getEngineState(), retail, event.submapObjects);
      LohanRaceManager.onSubmapLoad(event.getEngineState(), retail, event.submapObjects);
    }
    if (isTasmanPracticeEnabled() && event.getSubmap() instanceof RetailSubmap retail) {
      TasmanNpcManager.onSubmapLoad(event.getEngineState(), retail, event.submapObjects);
    }
  }

  @EventListener
  public void onSubmapWarp(final SubmapWarpEvent event) {
    if (isTasmanPracticeEnabled()) {
      TasmanNpcManager.cleanupNpc(event.getEngineState());
    }
  }

  @EventListener
  public void onSubmapEncounter(final SubmapEncounterEvent event) {
    if (isTasmanPracticeEnabled()) {
      TasmanPracticeBattle.onSubmapEncounter(event);
    }
  }

  @EventListener
  public void onRender(final RenderEvent event) {
    // 1. Check and hook active OptionsCategoryScreen on SItem.menuStack
    checkOptionsMenuHook();

    // 2. Fast Travel Hook & Warp Execution
    FastTravelManager.onRender();

    // 3. Advanced Additions Render
    if (isAdvancedAdditionsEnabled()) {
      RandomAdditionsManager.onRender();
      TasmanBattleTutorial.onRender(event);

      if (EngineStates.currentEngineState_8004dd04 instanceof Battle) {
        ScriptHookUtil.hookEngineStateFunction(753, AdvancedAdditionOverlaysEffect.ALLOCATOR);
      }

      if (SEffe.additionOverlayActive_80119f41 != 0 && SCRIPTS != null) {
        final int scriptCount = SCRIPTS.count();
        for (int i = 0; i < scriptCount; i++) {
          final ScriptState<?> state = SCRIPTS.getState(i);
          if (state != null && state.innerStruct_00 instanceof final EffectManagerData6c<?> manager) {
            if (manager.effect_44 != null) {
              if (manager.effect_44 instanceof AdditionOverlaysEffect44 overlay) {
                if (overlay.autoCompleteType_3a != 0 && TasmanBattleTutorial.isAdvancedTutorialActive()) {
                  state.deallocateWithChildren();
                  SEffe.additionOverlayActive_80119f41 = 0;
                  continue;
                }
              }
              if (manager.effect_44.getClass() == AdditionOverlaysEffect44.class) {
                final AdditionOverlaysEffect44 vanilla = (AdditionOverlaysEffect44) manager.effect_44;
                ((EffectManagerData6c) manager).effect_44 = new AdvancedAdditionOverlaysEffect(vanilla, state);
                LOGGER.info("TvvlrMultimod: Upgraded vanilla AdditionOverlaysEffect44 to AdvancedAdditionOverlaysEffect on script %d", i);
              }
            }
          }
        }
      }
    }

    // 4. Racing Minigame Render
    if (isRacingMinigameEnabled()) {
      LohanRaceNpc.onRender();
      LohanRaceManager.onRender();
    }

    // 5. Master Tasman Render
    if (isTasmanPracticeEnabled()) {
      TasmanNpcManager.onRender();
      TasmanPracticeBattle.onRender(event);
    }

    // 6. Dance Modifier Combat Victory Hook
    if (isDanceModifierEnabled() && EngineStates.currentEngineState_8004dd04 instanceof Battle battle) {
      checkCombatVictoryDances(battle);
    }
  }

  public static class ExtendedVictoryPostBattleActionInstance extends VictoryPostBattleActionInstance {
    private final int defaultDuration;
    private int elapsedFrames = 0;

    public ExtendedVictoryPostBattleActionInstance(final VictoryPostBattleAction action, final int defaultDuration) {
      super(action);
      this.defaultDuration = defaultDuration;
      this.elapsedFrames = 0;
    }

    @Override
    public int getTotalDuration(final Battle battle) {
      this.elapsedFrames++;
      if (this.elapsedFrames >= 15) {
        if (PLATFORM.isActionPressed(INPUT_ACTION_MENU_CONFIRM.get())
            || PLATFORM.isActionPressed(INPUT_ACTION_MENU_BACK.get())
            || (INPUT_ACTION_BTTL_ATTACK != null && INPUT_ACTION_BTTL_ATTACK.isValid() && PLATFORM.isActionPressed(INPUT_ACTION_BTTL_ATTACK.get()))) {
          return 0; // Immediate skip on tap/confirm/back!
        }
      }
      return this.defaultDuration;
    }

    @Override
    public void onCameraFadeoutStart(final Battle battle) {
      super.onCameraFadeoutStart(battle);
      applyVictoryDances(battle);
    }
  }

  private static void preInjectVictoryAnimations(final Battle battle) {
    try {
      if (battleState_8006e398 == null || battleState_8006e398.playerBents_e40 == null) {
        return;
      }
      final int count = battleState_8006e398.getPlayerCount();
      for (int i = 0; i < count; i++) {
        final ScriptState<PlayerBattleEntity> state = battleState_8006e398.playerBents_e40.get(i);
        if (state == null || state.innerStruct_00 == null) {
          continue;
        }
        final PlayerBattleEntity playerBent = state.innerStruct_00;
        final int charId = playerBent.charId_272;
        final DanceType dance = DanceModifierStorage.getDanceByCharId(charId);
        final TmdAnimationFile danceAnim = ProceduralDanceSynthesizer.getDanceAnimation(charId, dance != null ? dance : DanceType.DEFAULT);
        if (danceAnim != null && playerBent.combatant_144 != null) {
          final CombatantAsset0c.AnimType customAsset15 = new CombatantAsset0c.AnimType(danceAnim);
          customAsset15.type_0a = 1;
          customAsset15.isLoaded_0b = true;
          customAsset15._09 = 1;
          playerBent.combatant_144.assets_14[15] = customAsset15;

          final CombatantAsset0c.AnimType customAsset31 = new CombatantAsset0c.AnimType(danceAnim);
          customAsset31.type_0a = 1;
          customAsset31.isLoaded_0b = true;
          customAsset31._09 = 1;
          playerBent.combatant_144.assets_14[31] = customAsset31;

          playerBent.combatant_144.assets_14[10] = customAsset15;
        }
      }
      LOGGER.info("TvvlrMultimod: Pre-injected victory animations into combatant asset slots 15 & 31 for %d players", count);
    } catch (final Throwable t) {
      LOGGER.warn("TvvlrMultimod: Failed to pre-inject victory animations", t);
    }
  }

  private static void checkCombatVictoryDances(final Battle battle) {
    if (postBattleAction_800bc974 == null || !(postBattleAction_800bc974.action instanceof VictoryPostBattleAction)) {
      return;
    }

    // Wrap the victory action in an ExtendedVictoryPostBattleActionInstance so that:
    // 1. postBattleAction_800bc974.action retains its original reference (CorePostBattleActions.VICTORY),
    //    ensuring game variable 57 returns 1 (VICTORY) to scripts so players halt rather than attack.
    // 2. Victory celebration lasts 90 frames (~3s), instantly skippable anytime after frame 15 via Confirm, Back, or screen tap.
    // 3. Victory dances are applied when Battle.fadeOutBattle() calls onCameraFadeoutStart and continuously enforced.
    if (!(postBattleAction_800bc974 instanceof ExtendedVictoryPostBattleActionInstance)) {
      postBattleAction_800bc974 = new ExtendedVictoryPostBattleActionInstance(
          (VictoryPostBattleAction) postBattleAction_800bc974.action, 90);
      LOGGER.info("TvvlrMultimod: Extended victory screen duration to 90 frames for victory dance (skippable via tap/confirm/back)");
    }

    applyVictoryDances(battle);
  }

  private static void applyVictoryDances(final Battle battle) {
    if (battleState_8006e398 == null || battleState_8006e398.alivePlayerBents_eac == null) {
      return;
    }

    try {
      final int aliveCount = battleState_8006e398.alivePlayerBents_eac.size();
      for (int i = 0; i < aliveCount; i++) {
        final ScriptState<PlayerBattleEntity> state = battleState_8006e398.alivePlayerBents_eac.get(i);
        if (state == null || state.innerStruct_00 == null) {
          continue;
        }
        final PlayerBattleEntity playerBent = state.innerStruct_00;
        final int charId = playerBent.charId_272;
        final DanceType dance = DanceModifierStorage.getDanceByCharId(charId);
        final TmdAnimationFile danceAnim = ProceduralDanceSynthesizer.getDanceAnimation(charId, dance != null ? dance : DanceType.DEFAULT);
        if (danceAnim != null) {
          if (playerBent.combatant_144 != null) {
            final CombatantAsset0c.AnimType customAsset15 = new CombatantAsset0c.AnimType(danceAnim);
            customAsset15.type_0a = 1;
            customAsset15.isLoaded_0b = true;
            customAsset15._09 = 1;
            playerBent.combatant_144.assets_14[15] = customAsset15;

            final CombatantAsset0c.AnimType customAsset31 = new CombatantAsset0c.AnimType(danceAnim);
            customAsset31.type_0a = 1;
            customAsset31.isLoaded_0b = true;
            customAsset31._09 = 1;
            playerBent.combatant_144.assets_14[31] = customAsset31;

            playerBent.combatant_144.assets_14[10] = customAsset15;
          }
          if (playerBent.model_148 != null) {
            if (playerBent.loadingAnimIndex_26e != 15 && playerBent.loadingAnimIndex_26e != 31) {
              state.clearFlag(BattleEntity27c.FLAG_ANIMATE_ONCE | BattleEntity27c.FLAG_HIDE);
              loadModelStandardAnimation(playerBent.model_148, danceAnim);
              playerBent.model_148.animationState_9c = 1;
              playerBent.loadingAnimIndex_26e = 15;
            }
          }
        }
      }
      if (!victoryDancesApplied) {
        victoryDancesApplied = true;
        LOGGER.info("TvvlrMultimod: Successfully applied victory dances into slots 15 & 31 for %d alive players", aliveCount);
      }
    } catch (final Throwable t) {
      LOGGER.error("TvvlrMultimod: Failed to apply combat victory dances", t);
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
