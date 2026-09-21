package legend.multimod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CustomAdditionsStorage {
  private static final Logger LOGGER = LogManager.getFormatterLogger(CustomAdditionsStorage.class);
  private static final Path STORAGE_FILE = Path.of("tvvlr_custom_additions.json");
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  public static class AdditionDefinition {
    public final String id;
    public final String displayName;
    public final int hitCount; // Number of timed prompts (count_30)

    public AdditionDefinition(final String id, final String displayName, final int hitCount) {
      this.id = id;
      this.displayName = displayName;
      this.hitCount = hitCount;
    }
  }

  public static class CharacterCategory {
    public final String characterName;
    public final List<AdditionDefinition> additions;

    public CharacterCategory(final String characterName, final List<AdditionDefinition> additions) {
      this.characterName = characterName;
      this.additions = Collections.unmodifiableList(additions);
    }
  }

  public static final List<CharacterCategory> CHARACTERS = new ArrayList<>();
  private static final Map<String, List<AdditionButtonType>> CUSTOM_MAPPINGS = new LinkedHashMap<>();

  static {
    // Dart
    final List<AdditionDefinition> dart = new ArrayList<>();
    dart.add(new AdditionDefinition("lod:double_slash", "Double Slash", 1));
    dart.add(new AdditionDefinition("lod:volcano", "Volcano", 3));
    dart.add(new AdditionDefinition("lod:burning_rush", "Burning Rush", 2));
    dart.add(new AdditionDefinition("lod:crush_dance", "Crush Dance", 4));
    dart.add(new AdditionDefinition("lod:madness_hero", "Madness Hero", 5));
    dart.add(new AdditionDefinition("lod:moon_strike", "Moon Strike", 6));
    dart.add(new AdditionDefinition("lod:blazing_dynamo", "Blazing Dynamo", 7));
    CHARACTERS.add(new CharacterCategory("Dart", dart));

    // Lavitz
    final List<AdditionDefinition> lavitz = new ArrayList<>();
    lavitz.add(new AdditionDefinition("lod:harpoon", "Harpoon", 1));
    lavitz.add(new AdditionDefinition("lod:spinning_cane", "Spinning Cane", 2));
    lavitz.add(new AdditionDefinition("lod:rod_typhoon", "Rod Typhoon", 4));
    lavitz.add(new AdditionDefinition("lod:gust_of_wind_dance", "Gust of Wind Dance", 6));
    lavitz.add(new AdditionDefinition("lod:flower_storm", "Flower Storm", 7));
    CHARACTERS.add(new CharacterCategory("Lavitz", lavitz));

    // Albert
    final List<AdditionDefinition> albert = new ArrayList<>();
    albert.add(new AdditionDefinition("lod:albert_harpoon", "Harpoon", 1));
    albert.add(new AdditionDefinition("lod:albert_spinning_cane", "Spinning Cane", 2));
    albert.add(new AdditionDefinition("lod:albert_rod_typhoon", "Rod Typhoon", 4));
    albert.add(new AdditionDefinition("lod:albert_gust_of_wind_dance", "Gust of Wind Dance", 6));
    albert.add(new AdditionDefinition("lod:albert_flower_storm", "Flower Storm", 7));
    CHARACTERS.add(new CharacterCategory("Albert", albert));

    // Rose
    final List<AdditionDefinition> rose = new ArrayList<>();
    rose.add(new AdditionDefinition("lod:whip_smack", "Whip Smack", 1));
    rose.add(new AdditionDefinition("lod:more_more", "More & More", 2));
    rose.add(new AdditionDefinition("lod:hard_blade", "Hard Blade", 5));
    rose.add(new AdditionDefinition("lod:demons_dance", "Demon's Dance", 7));
    CHARACTERS.add(new CharacterCategory("Rose", rose));

    // Haschel
    final List<AdditionDefinition> haschel = new ArrayList<>();
    haschel.add(new AdditionDefinition("lod:double_punch", "Double Punch", 1));
    haschel.add(new AdditionDefinition("lod:ferry_of_styx", "Flurry of Styx", 2));
    haschel.add(new AdditionDefinition("lod:summon_4_gods", "Summon 4 Gods", 3));
    haschel.add(new AdditionDefinition("lod:five_ring_shattering", "5 Ring Shattering", 4));
    haschel.add(new AdditionDefinition("lod:hex_hammer", "Hex Hammer", 6));
    haschel.add(new AdditionDefinition("lod:omni_sweep", "Omni Sweep", 7));
    CHARACTERS.add(new CharacterCategory("Haschel", haschel));

    // Meru
    final List<AdditionDefinition> meru = new ArrayList<>();
    meru.add(new AdditionDefinition("lod:double_smack", "Double Smack", 1));
    meru.add(new AdditionDefinition("lod:hammer_spin", "Hammer Spin", 3));
    meru.add(new AdditionDefinition("lod:cool_boogie", "Cool Boogie", 4));
    meru.add(new AdditionDefinition("lod:cats_cradle", "Cat's Cradle", 6));
    meru.add(new AdditionDefinition("lod:perky_step", "Perky Step", 7));
    CHARACTERS.add(new CharacterCategory("Meru", meru));

    // Kongol
    final List<AdditionDefinition> kongol = new ArrayList<>();
    kongol.add(new AdditionDefinition("lod:pursuit", "Pursuit", 1));
    kongol.add(new AdditionDefinition("lod:inferno", "Inferno", 3));
    kongol.add(new AdditionDefinition("lod:bone_crush", "Bone Crush", 5));
    CHARACTERS.add(new CharacterCategory("Kongol", kongol));

    // Initialize defaults
    initDefaults();
    load();
  }

  public static AdditionDefinition getDefinition(final String additionId) {
    if (additionId == null) {
      return null;
    }
    final String cleanId = additionId.startsWith("lod:") ? additionId : "lod:" + additionId;
    for (final CharacterCategory category : CHARACTERS) {
      for (final AdditionDefinition def : category.additions) {
        if (def.id.equalsIgnoreCase(cleanId) || def.id.equalsIgnoreCase(additionId)) {
          return def;
        }
      }
    }
    return null;
  }

  private static void initDefaults() {
    for (final CharacterCategory category : CHARACTERS) {
      for (final AdditionDefinition def : category.additions) {
        final List<AdditionButtonType> buttons = new ArrayList<>(def.hitCount);
        for (int i = 0; i < def.hitCount; i++) {
          buttons.add(AdditionButtonType.CROSS);
        }
        CUSTOM_MAPPINGS.put(def.id, buttons);
      }
    }
  }

  public static synchronized List<AdditionButtonType> getCustomButtons(final String additionId) {
    if (additionId == null) {
      return null;
    }
    List<AdditionButtonType> buttons = CUSTOM_MAPPINGS.get(additionId);
    if (buttons == null && !additionId.contains(":")) {
      buttons = CUSTOM_MAPPINGS.get("lod:" + additionId);
    }
    if (buttons == null && additionId.startsWith("lod:")) {
      buttons = CUSTOM_MAPPINGS.get(additionId.substring(4));
    }
    if (buttons == null) {
      return null;
    }
    final List<AdditionButtonType> result = new ArrayList<>(buttons);
    final AdditionDefinition def = getDefinition(additionId);
    if (def != null && !result.isEmpty()) {
      final AdditionButtonType last = result.get(result.size() - 1);
      while (result.size() < def.hitCount) {
        result.add(last);
      }
    }
    return result;
  }

  public static synchronized void setCustomButtons(final String additionId, final List<AdditionButtonType> buttons) {
    if (additionId != null && buttons != null) {
      CUSTOM_MAPPINGS.put(additionId, new ArrayList<>(buttons));
    }
  }

  public static synchronized void load() {
    if (!Files.exists(STORAGE_FILE)) {
      LOGGER.info("CustomAdditionsStorage: %s not found, using default addition buttons", STORAGE_FILE);
      return;
    }

    try (final Reader reader = Files.newBufferedReader(STORAGE_FILE)) {
      final Type mapType = new TypeToken<Map<String, List<String>>>() {}.getType();
      final Map<String, List<String>> raw = GSON.fromJson(reader, mapType);
      if (raw != null) {
        boolean needsResave = false;
        for (final Map.Entry<String, List<String>> entry : raw.entrySet()) {
          final String addId = entry.getKey();
          final List<String> list = entry.getValue();
          if (list != null && !list.isEmpty()) {
            final List<AdditionButtonType> btnList = new ArrayList<>();
            for (final String s : list) {
              try {
                btnList.add(AdditionButtonType.valueOf(s));
              } catch (final Throwable ignored) {
                btnList.add(AdditionButtonType.CROSS);
              }
            }
            final AdditionDefinition def = getDefinition(addId);
            if (def != null) {
              final AdditionButtonType fillType = btnList.get(btnList.size() - 1);
              if (btnList.size() < def.hitCount) {
                needsResave = true;
                while (btnList.size() < def.hitCount) {
                  btnList.add(fillType);
                }
              } else if (btnList.size() > def.hitCount) {
                needsResave = true;
                while (btnList.size() > def.hitCount) {
                  btnList.remove(btnList.size() - 1);
                }
              }
            }
            CUSTOM_MAPPINGS.put(addId, btnList);
          }
        }
        LOGGER.info("CustomAdditionsStorage: Successfully loaded %d custom addition mappings from %s", raw.size(), STORAGE_FILE);
        if (needsResave) {
          save();
        }
      }
    } catch (final Throwable t) {
      LOGGER.warn("CustomAdditionsStorage: Failed loading from %s", STORAGE_FILE, t);
    }
  }

  public static synchronized void save() {
    try (final Writer writer = Files.newBufferedWriter(STORAGE_FILE)) {
      final Map<String, List<String>> raw = new LinkedHashMap<>();
      for (final Map.Entry<String, List<AdditionButtonType>> entry : CUSTOM_MAPPINGS.entrySet()) {
        final List<String> names = new ArrayList<>();
        for (final AdditionButtonType type : entry.getValue()) {
          names.add(type.name());
        }
        raw.put(entry.getKey(), names);
      }
      GSON.toJson(raw, writer);
      LOGGER.info("CustomAdditionsStorage: Saved %d custom addition mappings to %s", raw.size(), STORAGE_FILE);
    } catch (final Throwable t) {
      LOGGER.warn("CustomAdditionsStorage: Failed saving to %s", STORAGE_FILE, t);
    }
  }
}
