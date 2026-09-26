package legend.multimod;

import legend.game.scripting.FlowControl;
import legend.game.scripting.RunningScript;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public final class ScriptHookUtil {
  private static final Logger LOGGER = LogManager.getFormatterLogger(ScriptHookUtil.class);
  private static Unsafe unsafe;

  static {
    try {
      final Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
      theUnsafe.setAccessible(true);
      unsafe = (Unsafe) theUnsafe.get(null);
    } catch (final Throwable t) {
      LOGGER.warn("ScriptHookUtil: Failed to obtain Unsafe instance", t);
    }
  }

  private ScriptHookUtil() { }

  public static void hookScriptSubFunction(
      final int opcode,
      final Function<RunningScript, FlowControl> hook,
      final Consumer<Function<RunningScript, FlowControl>> originalSaver) {
    hookFunctionField("scriptSubFunctions_8004e29c", opcode, hook, originalSaver);
  }

  public static void hookEngineStateFunction(
      final int opcode,
      final Function<RunningScript, FlowControl> hook) {
    hookFunctionField("engineStateFunctions_8004e29c", opcode, hook, null);
  }

  @SuppressWarnings("unchecked")
  private static void hookFunctionField(
      final String fieldName,
      final int opcode,
      final Function<RunningScript, FlowControl> hook,
      final Consumer<Function<RunningScript, FlowControl>> originalSaver) {
    try {
      final Class<?> segClass = Class.forName("legend.game.Scus94491BpeSegment_8004");
      final Field field = segClass.getDeclaredField(fieldName);
      field.setAccessible(true);

      if (field.getType().isArray()) {
        final Function<RunningScript, FlowControl>[] array = (Function<RunningScript, FlowControl>[]) field.get(null);
        if (array != null && opcode < array.length) {
          if (originalSaver != null) {
            originalSaver.accept(array[opcode]);
          }
          array[opcode] = hook;
        }
      } else if (List.class.isAssignableFrom(field.getType())) {
        if (unsafe != null) {
          final Object base = unsafe.staticFieldBase(field);
          final long offset = unsafe.staticFieldOffset(field);
          final List<Function<RunningScript, FlowControl>> list =
              (List<Function<RunningScript, FlowControl>>) unsafe.getObject(base, offset);
          if (list != null && opcode < list.size()) {
            if (originalSaver != null) {
              originalSaver.accept(list.get(opcode));
            }
            if (list.get(opcode) != hook) {
              final List<Function<RunningScript, FlowControl>> copy = new ArrayList<>(list);
              copy.set(opcode, hook);
              unsafe.putObject(base, offset, Collections.unmodifiableList(copy));
            }
          }
        }
      }
    } catch (final Throwable t) {
      LOGGER.warn("ScriptHookUtil: Failed to hook opcode %d on field %s", opcode, fieldName, t);
    }
  }
}
