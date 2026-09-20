package legend.multimod;

import legend.core.platform.input.InputAction;

import static legend.lodmod.LodMod.INPUT_ACTION_BTTL_ATTACK;
import static legend.lodmod.LodMod.INPUT_ACTION_BTTL_COUNTER;

public enum AdditionButtonType {
  CROSS(72, 96, 255),
  CIRCLE(216, 96, 32),
  SQUARE(254, 166, 218),
  TRIANGLE(0, 255, 255);

  public final int r;
  public final int g;
  public final int b;

  AdditionButtonType(final int r, final int g, final int b) {
    this.r = r;
    this.g = g;
    this.b = b;
  }

  public int getR() {
    return this.getR(ControllerTheme.getActiveTheme());
  }

  public int getG() {
    return this.getG(ControllerTheme.getActiveTheme());
  }

  public int getB() {
    return this.getB(ControllerTheme.getActiveTheme());
  }

  public int[] getRgb() {
    return this.getRgb(ControllerTheme.getActiveTheme());
  }

  public int getR(final ControllerTheme theme) {
    return this.getRgb(theme)[0];
  }

  public int getG(final ControllerTheme theme) {
    return this.getRgb(theme)[1];
  }

  public int getB(final ControllerTheme theme) {
    return this.getRgb(theme)[2];
  }

  public int[] getRgb(final ControllerTheme theme) {
    switch (theme) {
      case XBOX:
        switch (this) {
          case CROSS:    return new int[] { 32, 216, 64 };   // Xbox A: Green
          case CIRCLE:   return new int[] { 235, 36, 36 };   // Xbox B: Red
          case SQUARE:   return new int[] { 0, 136, 255 };   // Xbox X: Blue
          case TRIANGLE: return new int[] { 255, 215, 0 };   // Xbox Y: Yellow
        }
        break;

      case SWITCH:
        switch (this) {
          case CROSS:    return new int[] { 255, 215, 0 };   // Switch B: Yellow
          case CIRCLE:   return new int[] { 235, 36, 36 };   // Switch A: Red
          case SQUARE:   return new int[] { 32, 216, 64 };   // Switch Y: Green
          case TRIANGLE: return new int[] { 0, 136, 255 };   // Switch X: Blue
        }
        break;

      case PLAYSTATION:
      default:
        switch (this) {
          case CROSS:    return new int[] { 72, 96, 255 };   // PS Cross: Blue
          case CIRCLE:   return new int[] { 216, 96, 32 };   // PS Circle: Red
          case SQUARE:   return new int[] { 254, 166, 218 }; // PS Square: Pink
          case TRIANGLE: return new int[] { 0, 255, 255 };   // PS Triangle: Cyan/Green
        }
        break;
    }
    return new int[] { this.r, this.g, this.b };
  }

  public InputAction getInputAction() {
    switch (this) {
      case SQUARE:
        return TvvlrMultimod.INPUT_ACTION_ADDITION_SQUARE.get();
      case TRIANGLE:
        return TvvlrMultimod.INPUT_ACTION_ADDITION_TRIANGLE.get();
      case CIRCLE:
        return INPUT_ACTION_BTTL_COUNTER.get();
      case CROSS:
      default:
        return INPUT_ACTION_BTTL_ATTACK.get();
    }
  }
}
