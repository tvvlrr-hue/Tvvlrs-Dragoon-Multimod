package legend.multimod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static legend.core.GameEngine.CONFIG;
import static legend.game.Scus94491BpeSegment_800b.gameState_800babc8;
import static legend.game.Scus94491BpeSegment_800b.submapId_800bd808;

public final class FastTravelLocations {
  private FastTravelLocations() { }

  public static class Location {
    public final String id;
    public final String displayName;
    public final int targetCut;
    public final int targetScene;
    public final int[] wmapLocationIndices;
    public final int submapId;
    public final boolean isTown;

    public Location(final String id, final String displayName, final int targetCut, final int targetScene, final int[] wmapLocationIndices, final int submapId, final boolean isTown) {
      this.id = id;
      this.displayName = displayName;
      this.targetCut = targetCut;
      this.targetScene = targetScene;
      this.wmapLocationIndices = wmapLocationIndices;
      this.submapId = submapId;
      this.isTown = isTown;
    }

    public int getTargetCut() {
      if ("hoax".equals(this.id)) {
        if (isHoaxBattleFinished()) {
          return 97;
        }
        return 95;
      }
      if ("valley".equals(this.id)) {
        if (isValleyUnlocked()) {
          return 258;
        }
        return 251;
      }
      return this.targetCut;
    }

    public int getTargetScene() {
      if ("hoax".equals(this.id)) {
        return 2;
      }
      if ("valley".equals(this.id)) {
        return 9;
      }
      return this.targetScene;
    }

    public static boolean isValleyUnlocked() {
      if (gameState_800babc8 == null) {
        return false;
      }
      // Chapter 3 or 4 is always after Chapter 2 Valley
      if (gameState_800babc8.chapterIndex_98 > 1) {
        return true;
      }
      // If bit 38 (open Valley entrance), bit 39 (north exit), or any subsequent Tiberoa location is visited
      if (gameState_800babc8.visitedLocations_17c != null) {
        final int[] unlockedIndices = {38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49};
        for (final int idx : unlockedIndices) {
          if (idx >= 0 && idx < gameState_800babc8.visitedLocations_17c.count() * 32 && gameState_800babc8.visitedLocations_17c.get(idx)) {
            return true;
          }
        }
      }
      return false;
    }

    public static boolean isHoaxBattleFinished() {
      if (gameState_800babc8 == null) {
        return false;
      }
      // 1. Chapter 2 or later is always after Hoax
      if (gameState_800babc8.chapterIndex_98 > 0) {
        return true;
      }
      // 2. If Dart has awakened his Dragoon Spirit, the Hoax night battle has concluded
      if (gameState_800babc8.charData_32c != null && !gameState_800babc8.charData_32c.isEmpty()) {
        final legend.game.characters.CharacterData2c dart = gameState_800babc8.charData_32c.get(0);
        if (dart != null && dart.hasDragoon()) {
          return true;
        }
      }
      // 3. If any post-Hoax Serdio location has been visited on the world map
      if (gameState_800babc8.visitedLocations_17c != null) {
        final int[] postHoaxIndices = {15, 97, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27};
        for (final int idx : postHoaxIndices) {
          if (idx >= 0 && idx < gameState_800babc8.visitedLocations_17c.count() * 32 && gameState_800babc8.visitedLocations_17c.get(idx)) {
            return true;
          }
        }
      }
      return false;
    }

    public boolean isVisited() {
      // 1. Debug override if config enabled
      if (TvvlrMultimod.isFastTravelUnlockAll()) {
        return true;
      }

      // 2. Seles is Dart's starting hometown, always accessible
      if ("seles".equals(this.id)) {
        return true;
      }

      // 3. Currently inside this location's submap
      if (this.submapId >= 0 && submapId_800bd808 == this.submapId) {
        return true;
      }

      // 4. World map visited locations bitfield (the true in-game visitation record)
      if (gameState_800babc8 != null) {
        for (final int idx : this.wmapLocationIndices) {
          if (idx >= 0 && gameState_800babc8.visitedLocations_17c.get(idx)) {
            return true;
          }
        }
      }

      return false;
    }
  }

  public static final List<Location> ALL_LOCATIONS = new ArrayList<>();

  static {
    // --- Chapter 1: Serdio ---
    ALL_LOCATIONS.add(new Location("seles", "Seles", 12, 5, new int[]{3, 4, 5, 6, 98}, 3, true));
    ALL_LOCATIONS.add(new Location("forest", "Forest", 7, 12, new int[]{0, 1, 2}, 2, false));
    ALL_LOCATIONS.add(new Location("hellena", "Hellena Prison", 13, 16, new int[]{7}, 4, false));
    ALL_LOCATIONS.add(new Location("prairie", "Prairie", 38, 4, new int[]{8, 9, 10, 84}, 5, false));
    ALL_LOCATIONS.add(new Location("limestone_cave", "Limestone Cave", 45, 0, new int[]{11, 12}, 6, false));
    ALL_LOCATIONS.add(new Location("bale", "Capital Bale", 66, 40, new int[]{13}, 8, true));
    ALL_LOCATIONS.add(new Location("hoax", "Town of Hoax", 95, 2, new int[]{14, 15, 97}, 10, true));
    ALL_LOCATIONS.add(new Location("marshland", "Marshland", 106, 19, new int[]{16, 17, 18, 19}, 11, false));
    ALL_LOCATIONS.add(new Location("volcano_villude", "Volcano Villude", 114, 1, new int[]{20, 21}, 12, false));
    ALL_LOCATIONS.add(new Location("nest_of_dragon", "Nest of Dragon", 130, 11, new int[]{22, 23, 24}, 13, false));
    ALL_LOCATIONS.add(new Location("lohan", "Town of Lohan", 140, 0, new int[]{25}, 14, true));
    ALL_LOCATIONS.add(new Location("shrine_of_shirley", "Shrine of Shirley", 153, 11, new int[]{26}, 15, false));
    ALL_LOCATIONS.add(new Location("kazas", "Black Castle in Kazas", 171, 0, new int[]{27}, 17, true));

    // --- Chapter 2: Tiberoa & Illisa Bay ---
    ALL_LOCATIONS.add(new Location("fletz", "Twin Castle in Fletz", 201, 0, new int[]{28, 29, 30, 31}, 19, true));
    ALL_LOCATIONS.add(new Location("barrens", "Barrens", 231, 0, new int[]{32, 33, 34}, 21, false));
    ALL_LOCATIONS.add(new Location("donau", "Donau Flower City", 239, 7, new int[]{35, 36}, 22, true));
    ALL_LOCATIONS.add(new Location("valley", "Valley of Corrupted Gravity", 258, 9, new int[]{37, 38, 39}, 23, false));
    ALL_LOCATIONS.add(new Location("home_of_giganto", "Home of Giganto", 261, 0, new int[]{40, 41}, 24, false));
    ALL_LOCATIONS.add(new Location("lidiera", "Village of Lidiera", 297, 43, new int[]{42, 43}, 28, true));
    ALL_LOCATIONS.add(new Location("undersea_cavern", "Undersea Cavern", 301, 12, new int[]{44, 45, 46, 47}, 29, false));
    ALL_LOCATIONS.add(new Location("fueno", "City of Fueno", 309, 33, new int[]{48, 49}, 30, true));

    // --- Chapter 3: Mille Seseau & Gloriano ---
    ALL_LOCATIONS.add(new Location("furni", "Furni Water City", 330, 24, new int[]{50, 51, 52, 96}, 32, true));
    ALL_LOCATIONS.add(new Location("evergreen_forest", "Evergreen Forest", 339, 18, new int[]{53, 54, 55, 56, 58, 82}, 33, false));
    ALL_LOCATIONS.add(new Location("deningrad", "Crystal Palace in Deningrad", 349, 2, new int[]{59, 60}, 34, true));
    ALL_LOCATIONS.add(new Location("neet", "Tragic Village of Neet", 379, 1, new int[]{61}, 36, true));
    ALL_LOCATIONS.add(new Location("wingly_forest", "Forest of Winglies", 381, 0, new int[]{62}, 37, true));
    ALL_LOCATIONS.add(new Location("mortal_dragon", "Mountain of Mortal Dragon", 413, 0, new int[]{63}, 40, false));
    ALL_LOCATIONS.add(new Location("kashua_glacier", "Kashua Glacier", 433, 2, new int[]{64, 65}, 42, false));
    ALL_LOCATIONS.add(new Location("snowfield", "Snowfield", 457, 1, new int[]{66, 67}, 44, false));
    ALL_LOCATIONS.add(new Location("vellweb", "Capital Vellweb", 477, 1, new int[]{68}, 46, true));

    // --- Chapter 4: Death Frontier & Endiness ---
    ALL_LOCATIONS.add(new Location("death_frontier", "Death Frontier", 787, 2, new int[]{69}, 48, false));
    ALL_LOCATIONS.add(new Location("ulara", "Spring Breath Town, Ulara", 513, 0, new int[]{70, 71}, 49, true));
    ALL_LOCATIONS.add(new Location("zenebatos", "Law Capital Zenebatos", 528, 12, new int[]{72, 73, 77, 95}, 50, true));
    ALL_LOCATIONS.add(new Location("mayfil", "Death Capital Mayfil", 539, 0, new int[]{74, 75}, 51, false));
    ALL_LOCATIONS.add(new Location("rouge", "Outland Village Rouge", 563, 7, new int[]{78}, 53, true));
    ALL_LOCATIONS.add(new Location("aglis", "Magic Capital Aglis", 572, 29, new int[]{76}, 54, false));
  }

  public static List<Location> getVisitedLocations() {
    final List<Location> result = new ArrayList<>();
    for (final Location loc : ALL_LOCATIONS) {
      if (loc.isVisited()) {
        result.add(loc);
      }
    }
    return Collections.unmodifiableList(result);
  }
}
