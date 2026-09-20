package legend.multimod;

import legend.core.Config;
import legend.core.MathHelper;
import legend.core.platform.input.InputAction;
import legend.core.platform.input.InputCodepoints;
import legend.core.renderer.QueuedModelStandard;
import legend.core.renderer.Translucency;
import legend.game.combat.SEffe;
import legend.game.combat.effects.AdditionOverlaysBorder0e;
import legend.game.combat.effects.AdditionOverlaysEffect44;
import legend.game.combat.effects.AdditionOverlaysHit20;
import legend.game.combat.effects.EffectManagerData6c;
import legend.game.combat.effects.EffectManagerParams;
import legend.game.combat.types.BattleObject;
import legend.game.combat.ui.AdditionOverlayMode;
import legend.game.inventory.screens.FontOptions;
import legend.game.inventory.screens.HorizontalAlign;
import legend.game.inventory.screens.TextColour;
import legend.game.modding.coremod.CoreMod;
import legend.game.scripting.FlowControl;
import legend.game.scripting.RunningScript;
import legend.game.scripting.ScriptState;
import org.joml.Math;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import static legend.core.GameEngine.CONFIG;
import static legend.core.GameEngine.GPU;
import static legend.core.GameEngine.PLATFORM;
import static legend.core.GameEngine.RENDERER;
import static legend.game.Text.renderText;
import static legend.game.combat.SEffe.additionHitCompletionState_8011a014;
import static legend.game.combat.SEffe.additionOverlayActive_80119f41;
import static legend.game.combat.SEffe.allocateEffectManager;
import static legend.game.combat.SEffe.renderButtonPressHudElement1;
import static legend.lodmod.LodMod.INPUT_ACTION_BTTL_ATTACK;
import static legend.lodmod.LodMod.INPUT_ACTION_BTTL_COUNTER;

public class AdvancedAdditionOverlaysEffect extends AdditionOverlaysEffect44 {
  private static final Logger LOGGER = LogManager.getFormatterLogger(AdvancedAdditionOverlaysEffect.class);

  public static final Function<RunningScript, FlowControl> ALLOCATOR = AdvancedAdditionOverlaysEffect::scriptAllocate;

  private static final FontOptions UI_WHITE_SHADOWED = new FontOptions()
    .colour(TextColour.WHITE)
    .shadowColour(TextColour.BLACK)
    .horizontalAlign(HorizontalAlign.CENTRE);

  public AdditionButtonType[] hitButtonTypes;

  public AdvancedAdditionOverlaysEffect(final int attackerScriptIndex, final int targetScriptIndex, final int autoCompleteType) {
    super(attackerScriptIndex, targetScriptIndex, autoCompleteType);
    if (this.autoCompleteType_3a == 1 && TasmanBattleTutorial.isAdvancedTutorialActive()) {
      this.configureTutorialShowcase();
    } else {
      this.initHitButtonTypes();
    }
  }

  /**
   * Constructs an AdvancedAdditionOverlaysEffect by upgrading an existing vanilla AdditionOverlaysEffect44.
   */
  public AdvancedAdditionOverlaysEffect(final AdditionOverlaysEffect44 vanilla, final ScriptState<?> state) {
    super(vanilla.attackerScriptIndex_00, vanilla.targetScriptIndex_04, vanilla.autoCompleteType_3a);
    final byte[] savedState = additionHitCompletionState_8011a014 != null ? additionHitCompletionState_8011a014.clone() : null;
    this.count_30 = vanilla.count_30;
    this.pauseTickerAndRenderer_31 = vanilla.pauseTickerAndRenderer_31;
    this.additionComplete_32 = vanilla.additionComplete_32;
    this.currentFrame_34 = vanilla.currentFrame_34;
    this.unused_36 = vanilla.unused_36;
    this.numFramesToRenderCenterSquare_38 = vanilla.numFramesToRenderCenterSquare_38;
    this.lastCompletedHit_39 = vanilla.lastCompletedHit_39;
    this.hitOverlays_40 = vanilla.hitOverlays_40;
    if (savedState != null) {
      additionHitCompletionState_8011a014 = savedState;
    }
    if (this.autoCompleteType_3a == 1 && TasmanBattleTutorial.isAdvancedTutorialActive()) {
      this.configureTutorialShowcase();
    } else {
      this.initHitButtonTypes();
    }
  }

  public static FlowControl scriptAllocate(final RunningScript<? extends BattleObject> script) {
    final AdvancedAdditionOverlaysEffect effect = new AdvancedAdditionOverlaysEffect(
      script.params_20[0].get(),
      script.params_20[1].get(),
      script.params_20[2].get()
    );
    final ScriptState<EffectManagerData6c<EffectManagerParams.VoidType>> state = allocateEffectManager(
      "Addition overlays",
      script.scriptState_04,
      effect
    );
    state.setStor(8, 0); // Storage for counterattack state
    script.params_20[4].set(state.index);
    additionOverlayActive_80119f41 = 1;
    LOGGER.info("AdvancedAdditionOverlaysEffect: Allocated via scriptAllocate, total hits = %d", effect.count_30);
    return FlowControl.CONTINUE;
  }

  private void initHitButtonTypes() {
    this.hitButtonTypes = new AdditionButtonType[this.count_30];
    if (this.count_30 > 0) {
      if (this.autoCompleteType_3a == 1 || TasmanBattleTutorial.isCounterTutorialActive()) {
        Arrays.fill(this.hitButtonTypes, AdditionButtonType.CROSS);
      } else {
        // Rule: Hit 0 is always Cross (X)
        this.hitButtonTypes[0] = AdditionButtonType.CROSS;

        // Rule: Hits 1..count-1 are randomly Cross (X), Square, or Triangle
        for (int i = 1; i < this.count_30; i++) {
          final int roll = ThreadLocalRandom.current().nextInt(3);
          switch (roll) {
            case 1:
              this.hitButtonTypes[i] = AdditionButtonType.SQUARE;
              break;
            case 2:
              this.hitButtonTypes[i] = AdditionButtonType.TRIANGLE;
              break;
            case 0:
            default:
              this.hitButtonTypes[i] = AdditionButtonType.CROSS;
              break;
          }
        }
      }
    }

    // Apply color palette to borders
    this.applyBorderColours();
  }

  private void configureTutorialShowcase() {
    this.count_30 = 4;
    this.hitButtonTypes = new AdditionButtonType[] {
      AdditionButtonType.CROSS,
      AdditionButtonType.CIRCLE,
      AdditionButtonType.SQUARE,
      AdditionButtonType.TRIANGLE
    };

    final AdditionOverlaysHit20[] hitArray = new AdditionOverlaysHit20[4];
    Arrays.setAll(hitArray, AdditionOverlaysHit20::new);
    this.hitOverlays_40 = hitArray;
    additionHitCompletionState_8011a014 = new byte[5];

    final float scale = CONFIG.getConfig(CoreMod.ADDITION_OVERLAY_SIZE_CONFIG.get());
    final int framesPerHit = 35;
    final int firstHitTarget = 30;

    for (int hitNum = 0; hitNum < 4; hitNum++) {
      final AdditionOverlaysHit20 hitOverlay = hitArray[hitNum];
      hitOverlay.unused_00 = 1;
      hitOverlay.hitSuccessful_01 = false;
      hitOverlay.shadowColour_08 = 0;
      hitOverlay.borderColoursArrayIndex_02 = 3;
      hitOverlay.isCounter_1c = false;
      additionHitCompletionState_8011a014[hitNum] = 0;

      final int targetFrame = firstHitTarget + hitNum * framesPerHit;
      hitOverlay.totalHitFrames_0a = (short) framesPerHit;
      hitOverlay.frameBeginDisplay_0c = 15;
      hitOverlay.numSuccessFrames_0e = 3;
      hitOverlay.frameSuccessLowerBound_10 = (short) (targetFrame - 1);
      hitOverlay.frameSuccessUpperBound_12 = (short) (targetFrame + 1);

      final AdditionButtonType btnType = this.hitButtonTypes[hitNum];
      final AdditionOverlaysBorder0e[] borderArray = hitOverlay.borderArray_18;

      int val = 16;
      for (int borderNum = 0; borderNum < 17; borderNum++) {
        final AdditionOverlaysBorder0e borderOverlay = borderArray[borderNum];
        borderOverlay.size_08 = (18 - val) * 10 * 1.5f * scale;
        borderOverlay.isVisible_00 = true;
        borderOverlay.angleModifier_02 = Math.toRadians((16 - val) * 11.25f);
        borderOverlay.countFramesVisible_0c = 5;
        borderOverlay.sideEffects_0d = 0;
        borderOverlay.framesUntilRender_0a = (short) (hitOverlay.frameSuccessLowerBound_10 + (hitOverlay.numSuccessFrames_0e - 1) / 2 + val - 17);
        borderOverlay.r_04 = btnType.getR();
        borderOverlay.g_05 = btnType.getG();
        borderOverlay.b_06 = btnType.getB();
        val--;
      }

      val = 0;
      for (int borderNum = 16; borderNum >= 14; borderNum--) {
        final AdditionOverlaysBorder0e borderOverlay = borderArray[borderNum];
        borderOverlay.size_08 = (20 - val * 2) * 1.5f * scale;
        borderOverlay.angleModifier_02 = 0.0f;
        borderOverlay.countFramesVisible_0c = 0x11;
        borderOverlay.framesUntilRender_0a = (short) (hitOverlay.frameSuccessLowerBound_10 - 17);
        if (val != 1) {
          borderOverlay.r_04 = 0x30;
          borderOverlay.g_05 = 0x30;
          borderOverlay.b_06 = 0x30;
          borderOverlay.sideEffects_0d = 1;
        } else {
          borderOverlay.sideEffects_0d = -1;
        }
        val++;
      }
    }
    LOGGER.info("AdvancedAdditionOverlaysEffect: Configured 4-hit tutorial showcase: CROSS, CIRCLE, SQUARE, TRIANGLE");
  }

  private void applyBorderColours() {
    if (this.hitOverlays_40 == null) {
      return;
    }

    for (int hitNum = 0; hitNum < this.count_30 && hitNum < this.hitOverlays_40.length; hitNum++) {
      final AdditionOverlaysHit20 hitOverlay = this.hitOverlays_40[hitNum];
      if (hitOverlay == null || hitOverlay.borderArray_18 == null) {
        continue;
      }

      final AdditionButtonType type = (this.hitButtonTypes != null && hitNum < this.hitButtonTypes.length)
        ? this.hitButtonTypes[hitNum]
        : AdditionButtonType.CROSS;

      final AdditionOverlaysBorder0e[] borderArray = hitOverlay.borderArray_18;
      for (int borderNum = 0; borderNum < 17 && borderNum < borderArray.length; borderNum++) {
        final AdditionOverlaysBorder0e border = borderArray[borderNum];
        if (border == null) {
          continue;
        }

        if (borderNum == 14 || borderNum == 16) {
          // Keep dark frame border
          border.r_04 = 0x30;
          border.g_05 = 0x30;
          border.b_06 = 0x30;
        } else {
          border.r_04 = type.getR();
          border.g_05 = type.getG();
          border.b_06 = type.getB();
        }
      }
    }
  }

  private InputAction[] getWrongActions(final AdditionButtonType type) {
    switch (type) {
      case SQUARE:
        return new InputAction[] {
          INPUT_ACTION_BTTL_ATTACK.get(),
          INPUT_ACTION_BTTL_COUNTER.get(),
          TvvlrMultimod.INPUT_ACTION_ADDITION_TRIANGLE.get()
        };
      case TRIANGLE:
        return new InputAction[] {
          INPUT_ACTION_BTTL_ATTACK.get(),
          INPUT_ACTION_BTTL_COUNTER.get(),
          TvvlrMultimod.INPUT_ACTION_ADDITION_SQUARE.get()
        };
      case CROSS:
      default:
        return new InputAction[] {
          INPUT_ACTION_BTTL_COUNTER.get(),
          TvvlrMultimod.INPUT_ACTION_ADDITION_SQUARE.get(),
          TvvlrMultimod.INPUT_ACTION_ADDITION_TRIANGLE.get()
        };
    }
  }

  public AdditionButtonType getButtonType(final int hitNum) {
    if (this.hitButtonTypes != null && hitNum >= 0 && hitNum < this.hitButtonTypes.length) {
      return this.hitButtonTypes[hitNum];
    }
    return AdditionButtonType.CROSS;
  }

  @Override
  public void tick(final ScriptState<EffectManagerData6c<EffectManagerParams.VoidType>> state) {
    final EffectManagerData6c<EffectManagerParams.VoidType> manager = state.innerStruct_00;

    if (this.pauseTickerAndRenderer_31 == 0) {
      final AdditionOverlaysHit20[] hitArray = this.hitOverlays_40;
      this.currentFrame_34++;

      int hitNum;
      int hitFailed = 1;
      for (hitNum = 0; hitNum < this.count_30; hitNum++) {
        // Catch too late failure when no button pressed
        if (this.currentFrame_34 == hitArray[hitNum].frameSuccessUpperBound_12 + 1) {
          if (additionHitCompletionState_8011a014[hitNum] == 0) {
            additionHitCompletionState_8011a014[hitNum] = -2;
            this.propagateFailedHit(hitArray, hitNum);
          }
        } else if (additionHitCompletionState_8011a014[hitNum] == 0) {
          hitFailed = 0;
        }
      }

      if (hitFailed != 0) {
        this.propagateFailedHit(hitArray, hitNum);
      }

      int numberBordersRendering = 0;
      for (hitNum = 0; hitNum < this.count_30; hitNum++) {
        numberBordersRendering += this.tickBorderDisplaySub(hitNum, hitArray);
      }

      // If addition is complete and there are no more visible overlays to render, deallocate effect
      if (numberBordersRendering == 0 && this.additionComplete_32 != 0) {
        additionOverlayActive_80119f41 = 0;
        for (hitNum = 0; hitNum < this.count_30; hitNum++) {
          hitArray[hitNum].borderArray_18 = null;
        }
        state.deallocateWithChildren();
      } else {
        if (this.currentFrame_34 >= 9) {
          for (hitNum = 0; hitNum < this.count_30; hitNum++) {
            if (additionHitCompletionState_8011a014[hitNum] == 0) {
              break;
            }
          }

          if (hitNum < this.count_30) {
            final AdditionOverlaysHit20 hitOverlay = hitArray[hitNum];

            if (state.getStor(8) != 0) {
              hitOverlay.isCounter_1c = true;
              state.setStor(8, 0);
            }

            if (this.autoCompleteType_3a < 1 || this.autoCompleteType_3a > 2) {
              if (this.autoCompleteType_3a != 3) {
                final InputAction right;
                final InputAction[] wrong;
                if (hitOverlay.isCounter_1c) {
                  // Rule: Counters are always Circle (O)
                  right = INPUT_ACTION_BTTL_COUNTER.get();
                  wrong = new InputAction[] {
                    INPUT_ACTION_BTTL_ATTACK.get(),
                    TvvlrMultimod.INPUT_ACTION_ADDITION_SQUARE.get(),
                    TvvlrMultimod.INPUT_ACTION_ADDITION_TRIANGLE.get()
                  };
                } else {
                  final AdditionButtonType btnType = this.getButtonType(hitNum);
                  right = btnType.getInputAction();
                  wrong = this.getWrongActions(btnType);
                }

                final boolean rightPressed = PLATFORM.isActionPressed(right);
                boolean wrongPressed = false;
                for (final InputAction w : wrong) {
                  if (PLATFORM.isActionPressed(w)) {
                    wrongPressed = true;
                    break;
                  }
                }

                if (rightPressed || wrongPressed) {
                  additionHitCompletionState_8011a014[hitNum] = -1;

                  if (wrongPressed || !rightPressed) {
                    additionHitCompletionState_8011a014[hitNum] = -3;
                  } else if (this.currentFrame_34 >= hitOverlay.frameSuccessLowerBound_10 && this.currentFrame_34 <= hitOverlay.frameSuccessUpperBound_12) {
                    additionHitCompletionState_8011a014[hitNum] = 1;
                    hitOverlay.hitSuccessful_01 = true;
                  }

                  if (additionHitCompletionState_8011a014[hitNum] < 0) {
                    this.propagateFailedHit(hitArray, hitNum);
                  }

                  this.numFramesToRenderCenterSquare_38 = 2;
                  this.lastCompletedHit_39 = hitNum;
                }
              }
            } else { // Auto-complete
              if (this.currentFrame_34 >= hitOverlay.frameSuccessLowerBound_10 && this.currentFrame_34 <= hitOverlay.frameSuccessUpperBound_12) {
                additionHitCompletionState_8011a014[hitNum] = 1;
                hitOverlay.hitSuccessful_01 = true;
                this.numFramesToRenderCenterSquare_38 = 2;
                this.lastCompletedHit_39 = hitNum;
                if (this.autoCompleteType_3a == 1 && TasmanBattleTutorial.isAdvancedTutorialActive() && hitNum >= this.count_30 - 1) {
                  this.additionComplete_32 = 1;
                }
              }
            }
          }

          if (this.numFramesToRenderCenterSquare_38 != 0) {
            this.numFramesToRenderCenterSquare_38--;
            if (CONFIG.getConfig(CoreMod.ADDITION_OVERLAY_CONFIG.get()) != AdditionOverlayMode.OFF) {
              this.renderCenterSolidSquareSub(this, this.hitOverlays_40[this.lastCompletedHit_39], additionHitCompletionState_8011a014[this.lastCompletedHit_39], manager);
            }
          }
        }
      }
    }
  }

  private void propagateFailedHit(final AdditionOverlaysHit20[] hitArray, int hitNum) {
    final byte currentHitCompletionState = additionHitCompletionState_8011a014[hitNum];
    this.additionComplete_32 = 1;

    hitNum += 1;
    for (; hitNum < this.count_30; hitNum++) {
      final AdditionOverlaysHit20 hitOverlay = hitArray[hitNum];
      hitOverlay.frameSuccessUpperBound_12 = -1;
      hitOverlay.frameSuccessLowerBound_10 = -1;
      hitOverlay.numSuccessFrames_0e = 0;
      hitOverlay.frameBeginDisplay_0c = 0;
      additionHitCompletionState_8011a014[hitNum] = currentHitCompletionState;
    }
  }

  private int tickBorderDisplaySub(final int hitNum, final AdditionOverlaysHit20[] hitArray) {
    final AdditionOverlaysHit20 hitOverlay = hitArray[hitNum];
    if (this.currentFrame_34 >= hitOverlay.frameSuccessLowerBound_10 - 0x11) {
      hitOverlay.shadowColour_08 += 1;
      if (hitOverlay.shadowColour_08 >= 0xe) {
        hitOverlay.shadowColour_08 = 0xd;
      }
    }

    final byte currentHitCompletionState = additionHitCompletionState_8011a014[hitNum];
    final AdditionOverlaysBorder0e[] borderArray = hitOverlay.borderArray_18;
    int isRendered = 0;

    for (int borderNum = 0; borderNum < 17; borderNum++) {
      final AdditionOverlaysBorder0e borderOverlay = borderArray[borderNum];

      if (currentHitCompletionState < 0) {
        hitOverlay.shadowColour_08 -= 3;
        if (hitOverlay.shadowColour_08 < 0) {
          hitOverlay.shadowColour_08 = 0;
        }

        if (this.fadeBorders(borderOverlay, 0x20) == 3) {
          borderOverlay.isVisible_00 = false;
        }
      }

      if (borderOverlay.isVisible_00) {
        if (borderOverlay.framesUntilRender_0a > 0) {
          borderOverlay.framesUntilRender_0a--;
        } else {
          if (borderOverlay.sideEffects_0d != -1) {
            borderOverlay.sideEffects_0d++;
          }

          borderOverlay.countFramesVisible_0c--;
          if (borderOverlay.countFramesVisible_0c == 0) {
            borderOverlay.isVisible_00 = false;
          }

          if (borderNum < 14) {
            this.fadeBorders(borderOverlay, 0x4e);
          }

          isRendered = 1;
        }
      }
    }

    return isRendered;
  }

  private int fadeBorders(final AdditionOverlaysBorder0e square, final int fadeStep) {
    int numberOfNegativeComponents = 0;
    int newColour = square.r_04 - fadeStep;
    final int newR;
    if (newColour > 0) {
      newR = newColour;
    } else {
      newR = 0;
      numberOfNegativeComponents++;
    }

    newColour = square.g_05 - fadeStep;
    final int newG;
    if (newColour > 0) {
      newG = newColour;
    } else {
      newG = 0;
      numberOfNegativeComponents++;
    }

    newColour = square.b_06 - fadeStep;
    final int newB;
    if (newColour > 0) {
      newB = newColour;
    } else {
      newB = 0;
      numberOfNegativeComponents++;
    }

    square.r_04 = newR;
    square.g_05 = newG;
    square.b_06 = newB;
    return numberOfNegativeComponents;
  }

  @Override
  public void render(final ScriptState<EffectManagerData6c<EffectManagerParams.VoidType>> state) {
    final EffectManagerData6c<EffectManagerParams.VoidType> manager = state.innerStruct_00;

    if (this.pauseTickerAndRenderer_31 != 1) {
      if (manager.params_10.flags_00 >= 0) {
        final AdditionOverlaysHit20[] hitArray = this.hitOverlays_40;

        int hitNum;
        for (hitNum = 0; hitNum < this.count_30; hitNum++) {
          if (CONFIG.getConfig(CoreMod.ADDITION_OVERLAY_CONFIG.get()) == AdditionOverlayMode.FULL) {
            this.renderAdditionBordersSub(hitNum, hitArray);
          }
        }

        for (hitNum = 0; hitNum < this.count_30; hitNum++) {
          if (additionHitCompletionState_8011a014[hitNum] == 0) {
            break;
          }
        }

        if (hitNum < this.count_30) {
          final AdditionOverlaysHit20 hitOverlay = hitArray[hitNum];
          final InputAction action;
          if (hitOverlay.isCounter_1c) {
            action = INPUT_ACTION_BTTL_COUNTER.get();
          } else {
            action = this.getButtonType(hitNum).getInputAction();
          }

          if (CONFIG.getConfig(CoreMod.ADDITION_OVERLAY_CONFIG.get()) == AdditionOverlayMode.FULL) {
            this.renderPrompt(
              hitOverlay.frameSuccessLowerBound_10 + (hitOverlay.frameSuccessUpperBound_12 - hitOverlay.frameSuccessLowerBound_10) / 2 - this.currentFrame_34 - 1,
              action
            );
          }

          final byte currentFrame = (byte)this.currentFrame_34;
          if (currentFrame >= hitOverlay.frameSuccessLowerBound_10 && currentFrame <= hitOverlay.frameSuccessUpperBound_12) {
            if (CONFIG.getConfig(CoreMod.ADDITION_OVERLAY_CONFIG.get()) != AdditionOverlayMode.OFF) {
              this.renderCenterSolidSquareSub(this, hitOverlay, -2, manager);
            }
          }
        }
      }
    }
  }

  private void renderPrompt(final int frames, final InputAction action) {
    renderText(
      InputCodepoints.getActionName(action),
      GPU.getOffsetX() + 124.5f,
      GPU.getOffsetY() + 56.0f,
      UI_WHITE_SHADOWED
    );

    if (Math.abs(frames) >= 2) {
      // Arrow up position
      renderButtonPressHudElement1(0x24, 119.0f, 43.0f, Translucency.B_PLUS_F, 0x80);
    } else {
      // Arrow down position + glow
      renderButtonPressHudElement1(0x24, 119.0f, 51.0f, Translucency.B_PLUS_F, 0x80);
      renderButtonPressHudElement1(0x25, 113.5f, 50.0f, Translucency.B_PLUS_F, 0x80);
    }
  }

  private void renderAdditionBordersSub(final int hitNum, final AdditionOverlaysHit20[] hitArray) {
    final AdditionOverlaysBorder0e[] borderArray = hitArray[hitNum].borderArray_18;
    final byte currentHitCompletionState = additionHitCompletionState_8011a014[hitNum];

    for (int borderNum = 0; borderNum < 17; borderNum++) {
      final AdditionOverlaysBorder0e borderOverlay = borderArray[borderNum];

      if (borderOverlay.isVisible_00 && borderOverlay.framesUntilRender_0a <= 0) {
        this.transforms
          .scaling(borderOverlay.size_08, borderOverlay.size_08, 1.0f)
          .rotateZ(borderOverlay.angleModifier_02);

        this.transforms.transfer.set(GPU.getOffsetX(), GPU.getOffsetY() + 30.0f, 120.0f + hitNum * 0.1f);

        final QueuedModelStandard model;
        if ((borderOverlay.sideEffects_0d == 0 || borderOverlay.sideEffects_0d == -1) && currentHitCompletionState >= 0) {
          model = RENDERER.queueOrthoModel(RENDERER.lineBox, this.transforms, QueuedModelStandard.class);
        } else {
          model = RENDERER.queueOrthoModel(RENDERER.lineBoxBPlusF, this.transforms, QueuedModelStandard.class);
        }

        if (hitArray[hitNum].isCounter_1c && borderNum != 16) {
          if (Config.changeAdditionOverlayRgb()) {
            final int rgb = Config.getCounterOverlayRgb();
            final float rFactor = borderOverlay.r_04 / 255.0f;
            final float gFactor = borderOverlay.g_05 / 255.0f;
            final float bFactor = borderOverlay.b_06 / 255.0f;
            model.colour((rgb & 0xff) * rFactor / 255.0f, (rgb >> 8 & 0xff) * gFactor / 255.0f, (rgb >> 16 & 0xff) * bFactor / 255.0f);
          } else {
            // Standard counter red
            final int[] counterRgb = AdditionButtonType.CIRCLE.getRgb();
            model.colour(counterRgb[0] / 255.0f, counterRgb[1] / 255.0f, counterRgb[2] / 255.0f);
          }
        } else {
          model.colour(borderOverlay.r_04 / 255.0f, borderOverlay.g_05 / 255.0f, borderOverlay.b_06 / 255.0f);
        }

        if (borderOverlay.sideEffects_0d == 0) {
          this.renderBorderShadowSub(hitArray[hitNum], borderOverlay.angleModifier_02, borderOverlay.size_08);
          this.renderBorderShadowSub(hitArray[hitNum], borderOverlay.angleModifier_02 + MathHelper.HALF_PI, borderOverlay.size_08);
          this.renderBorderShadowSub(hitArray[hitNum], borderOverlay.angleModifier_02 + MathHelper.PI, borderOverlay.size_08);
          this.renderBorderShadowSub(hitArray[hitNum], borderOverlay.angleModifier_02 + MathHelper.PI + MathHelper.HALF_PI, borderOverlay.size_08);
        }
      }
    }
  }

  private void renderBorderShadowSub(final AdditionOverlaysHit20 hitOverlay, final float angle, final float borderSize) {
    final float offset = borderSize - 1;
    final float sin0 = MathHelper.sin(angle);
    final float cos0 = MathHelper.cosFromSin(sin0, angle);
    final float x0 = cos0 * offset / 2.0f;
    final float y0 = sin0 * offset / 2.0f;
    final int colour = hitOverlay.shadowColour_08 * 4;

    this.transforms.transfer.set(x0 + GPU.getOffsetX(), y0 + GPU.getOffsetY() + 30.0f, 124.0f);
    this.transforms
      .scaling(10.0f, borderSize, 1.0f)
      .rotateLocalZ(angle);

    RENDERER.queueOrthoModel(this.reticleBorderShadow, this.transforms, QueuedModelStandard.class)
      .monochrome(colour / 255.0f);
  }

  private void renderCenterSolidSquareSub(final AdvancedAdditionOverlaysEffect effect, final AdditionOverlaysHit20 hitOverlay, final int completionState, final EffectManagerData6c<?> manager) {
    if (manager.params_10.flags_00 >= 0) {
      final AdditionOverlaysBorder0e[] targetBorderArray = hitOverlay.borderArray_18;
      final float scale = CONFIG.getConfig(CoreMod.ADDITION_OVERLAY_SIZE_CONFIG.get());

      for (int targetBorderNum = 0; targetBorderNum < 2; targetBorderNum++) {
        final float squareSize = targetBorderArray[16].size_08 - targetBorderNum * 8 * scale;

        effect.transforms.scaling(squareSize, squareSize, 1.0f);
        effect.transforms.transfer.set(GPU.getOffsetX(), GPU.getOffsetY() + 30.0f, 120.0f);
        final QueuedModelStandard model = RENDERER.queueOrthoModel(RENDERER.centredQuadBPlusF, effect.transforms, QueuedModelStandard.class);

        if (completionState == 1) { // Success
          model.monochrome(1.0f);
        } else if (completionState != -2) { // Too early
          model.monochrome(0x30 / 255.0f);
        } else if (hitOverlay.isCounter_1c) { // Counter-attack too late
          if (Config.changeAdditionOverlayRgb()) {
            final int rgb = Config.getCounterOverlayRgb();
            model.colour((rgb & 0xff) / 255.0f, (rgb >> 8 & 0xff) / 255.0f, (rgb >> 16 & 0xff) / 255.0f);
          } else {
            final int[] counterRgb = AdditionButtonType.CIRCLE.getRgb();
            model.colour(counterRgb[0] / 255.0f, counterRgb[1] / 255.0f, counterRgb[2] / 255.0f);
          }
        } else { // Too late
          model.colour(targetBorderArray[15].r_04 / 255.0f, targetBorderArray[15].g_05 / 255.0f, targetBorderArray[15].b_06 / 255.0f);
        }
      }
    }
  }

  @Override
  public void setContinuationState(final int continuationState) {
    if (this.autoCompleteType_3a == 1 && TasmanBattleTutorial.isAdvancedTutorialActive()) {
      this.additionComplete_32 = continuationState;
      return;
    }
    super.setContinuationState(continuationState);
  }

  @Override
  public void destroy(final ScriptState<EffectManagerData6c<EffectManagerParams.VoidType>> state) {
    super.destroy(state);
  }
}
