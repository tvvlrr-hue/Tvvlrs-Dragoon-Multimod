package legend.multimod;

import legend.core.platform.input.InputGamepadType;

import static legend.core.GameEngine.PLATFORM;

public enum ControllerTheme {
  AUTO,
  PLAYSTATION,
  XBOX,
  SWITCH;

  public static ControllerTheme getActiveTheme() {
    try {
      final ControllerTheme configuredTheme = TvvlrMultimod.getControllerTheme();
      if (configuredTheme != null && configuredTheme != ControllerTheme.AUTO) {
        return configuredTheme;
      }
    } catch (final Throwable ignored) {
    }
    return detectTheme();
  }

  public static ControllerTheme detectTheme() {
    try {
      if (PLATFORM != null) {
        final InputGamepadType gamepadType = PLATFORM.getGamepadType();
        if (gamepadType != null) {
          switch (gamepadType) {
            case XBOX_360:
            case XBOX_ONE:
              return ControllerTheme.XBOX;
            case SWITCH:
              return ControllerTheme.SWITCH;
            case PLAYSTATION:
              return ControllerTheme.PLAYSTATION;
            case STANDARD:
            default:
              break;
          }
        }
      }
    } catch (final Throwable ignored) {
    }
    return ControllerTheme.PLAYSTATION;
  }
}
