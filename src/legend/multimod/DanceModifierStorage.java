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

public class DanceModifierStorage {
  private static final Logger LOGGER = LogManager.getFormatterLogger(DanceModifierStorage.class);
  private static final Path STORAGE_FILE = Path.of("tvvlr_dance_config.json");
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  public static class CharacterEntry {
    public final String key;
    public final String displayName;
    public final int charId;

    public CharacterEntry(final String key, final String displayName, final int charId) {
      this.key = key;
      this.displayName = displayName;
      this.charId = charId;
    }
  }

  public static final List<CharacterEntry> CHARACTERS = new ArrayList<>();
  private static final Map<String, DanceType> DANCE_MAPPINGS = new LinkedHashMap<>();
  private static final Map<String, String> SOURCE_CHAR_MAPPINGS = new LinkedHashMap<>();

  static {
    CHARACTERS.add(new CharacterEntry("dart", "Dart", 0));
    CHARACTERS.add(new CharacterEntry("lavitz", "Lavitz", 1));
    CHARACTERS.add(new CharacterEntry("shana", "Shana", 2));
    CHARACTERS.add(new CharacterEntry("rose", "Rose", 3));
    CHARACTERS.add(new CharacterEntry("haschel", "Haschel", 4));
    CHARACTERS.add(new CharacterEntry("albert", "Albert", 5));
    CHARACTERS.add(new CharacterEntry("meru", "Meru", 6));
    CHARACTERS.add(new CharacterEntry("kongol", "Kongol", 7));
    CHARACTERS.add(new CharacterEntry("miranda", "Miranda", 8));

    initDefaults();
    load();
  }

  private static void initDefaults() {
    for (final CharacterEntry entry : CHARACTERS) {
      DANCE_MAPPINGS.put(entry.key, DanceType.DEFAULT);
      SOURCE_CHAR_MAPPINGS.put(entry.key, entry.key);
    }
  }

  public static synchronized DanceType getDance(final String characterKey) {
    if (characterKey == null) {
      return DanceType.DEFAULT;
    }
    final String clean = characterKey.toLowerCase().trim();
    return DANCE_MAPPINGS.getOrDefault(clean, DanceType.DEFAULT);
  }

  public static synchronized DanceType getDanceByCharId(final int charId) {
    for (final CharacterEntry entry : CHARACTERS) {
      if (entry.charId == charId) {
        return getDance(entry.key);
      }
    }
    return DanceType.DEFAULT;
  }

  public static synchronized String getSourceCharacter(final String characterKey) {
    if (characterKey == null) {
      return "dart";
    }
    final String clean = characterKey.toLowerCase().trim();
    return SOURCE_CHAR_MAPPINGS.getOrDefault(clean, clean);
  }

  public static synchronized String getSourceCharacterByCharId(final int charId) {
    for (final CharacterEntry entry : CHARACTERS) {
      if (entry.charId == charId) {
        return getSourceCharacter(entry.key);
      }
    }
    return "dart";
  }

  public static synchronized String getCharacterKey(final int charId) {
    for (final CharacterEntry entry : CHARACTERS) {
      if (entry.charId == charId) {
        return entry.key;
      }
    }
    return "dart";
  }

  public static synchronized void setDance(final String characterKey, final DanceType dance) {
    if (characterKey != null && dance != null) {
      final String clean = characterKey.toLowerCase().trim();
      DANCE_MAPPINGS.put(clean, dance);
    }
  }

  public static synchronized void setSourceCharacter(final String characterKey, final String sourceCharKey) {
    if (characterKey != null && sourceCharKey != null) {
      final String clean = characterKey.toLowerCase().trim();
      final String cleanSource = sourceCharKey.toLowerCase().trim();
      SOURCE_CHAR_MAPPINGS.put(clean, cleanSource);
    }
  }

  public static class SavedDanceEntry {
    public String dance;
    public String sourceCharacter;

    public SavedDanceEntry() {}

    public SavedDanceEntry(final String dance, final String sourceCharacter) {
      this.dance = dance;
      this.sourceCharacter = sourceCharacter;
    }
  }

  public static synchronized void load() {
    if (!Files.exists(STORAGE_FILE)) {
      LOGGER.info("DanceModifierStorage: %s not found, using default dances", STORAGE_FILE);
      return;
    }

    try (final Reader reader = Files.newBufferedReader(STORAGE_FILE)) {
      final Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
      final Map<String, Object> raw = GSON.fromJson(reader, mapType);
      if (raw != null) {
        for (final Map.Entry<String, Object> entry : raw.entrySet()) {
          final String charKey = entry.getKey().toLowerCase().trim();
          final Object val = entry.getValue();
          if (val instanceof String s) {
            try {
              DANCE_MAPPINGS.put(charKey, DanceType.valueOf(s));
            } catch (final Throwable ignored) {
              DANCE_MAPPINGS.put(charKey, DanceType.DEFAULT);
            }
            SOURCE_CHAR_MAPPINGS.put(charKey, charKey);
          } else if (val instanceof Map<?, ?> m) {
            final Object dObj = m.get("dance");
            final Object sObj = m.get("sourceCharacter");
            if (dObj != null) {
              try {
                DANCE_MAPPINGS.put(charKey, DanceType.valueOf(String.valueOf(dObj)));
              } catch (final Throwable ignored) {
                DANCE_MAPPINGS.put(charKey, DanceType.DEFAULT);
              }
            }
            if (sObj != null) {
              SOURCE_CHAR_MAPPINGS.put(charKey, String.valueOf(sObj).toLowerCase().trim());
            } else {
              SOURCE_CHAR_MAPPINGS.put(charKey, charKey);
            }
          }
        }
        LOGGER.info("DanceModifierStorage: Loaded %d dance configurations from %s", raw.size(), STORAGE_FILE);
      }
    } catch (final Throwable t) {
      LOGGER.warn("DanceModifierStorage: Failed loading from %s", STORAGE_FILE, t);
    }
  }

  public static synchronized void save() {
    try (final Writer writer = Files.newBufferedWriter(STORAGE_FILE)) {
      final Map<String, SavedDanceEntry> raw = new LinkedHashMap<>();
      for (final CharacterEntry entry : CHARACTERS) {
        final DanceType dance = getDance(entry.key);
        final String source = getSourceCharacter(entry.key);
        raw.put(entry.key, new SavedDanceEntry(dance.name(), source));
      }
      GSON.toJson(raw, writer);
      LOGGER.info("DanceModifierStorage: Saved %d dance configurations to %s", raw.size(), STORAGE_FILE);
    } catch (final Throwable t) {
      LOGGER.warn("DanceModifierStorage: Failed saving to %s", STORAGE_FILE, t);
    }
  }
}
