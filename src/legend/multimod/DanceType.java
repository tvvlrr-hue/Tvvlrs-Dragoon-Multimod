package legend.multimod;

public enum DanceType {
  DEFAULT("Default");

  private final String displayName;

  DanceType(final String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return this.displayName;
  }

  public static DanceType cycle(final DanceType current, final int direction) {
    final DanceType[] values = values();
    final int next = (current.ordinal() + direction % values.length + values.length) % values.length;
    return values[next];
  }
}
