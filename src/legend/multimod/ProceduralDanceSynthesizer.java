package legend.multimod;

import legend.game.types.TmdAnimationFile;
import legend.game.unpacker.FileData;
import legend.game.unpacker.Loader;
import legend.game.unpacker.Unpacker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class ProceduralDanceSynthesizer {
  private static final Logger LOGGER = LogManager.getFormatterLogger(ProceduralDanceSynthesizer.class);
  private static final Map<String, TmdAnimationFile> DANCE_CACHE = new HashMap<>();

  public enum Bone {
    PELVIS,
    TORSO,
    TORSO_EXTRA,
    HEAD,
    HEAD_EXTRA,
    SHOULDER_R,
    SHOULDER_L,
    FOREARM_R,
    FOREARM_L,
    HAND_R,
    HAND_L,
    THIGH_R,
    THIGH_L,
    SHIN_R,
    SHIN_L,
    FOOT_R,
    FOOT_L,
    WEAPON
  }

  private static final Map<String, Bone[]> CHARACTER_BONES = new HashMap<>();
  private static final Map<String, Map<Bone, Integer>> CHARACTER_BONE_TO_PART = new HashMap<>();
  private static final Map<String, int[]> CHARACTER_PARENTS = new HashMap<>();
  private static final Map<String, Integer> CHARACTER_HEIGHTS = new HashMap<>();
  private static final Map<String, Vector3f> WEAPON_PRIMARY_AXIS = new HashMap<>();


  static {
    WEAPON_PRIMARY_AXIS.put("dart", new Vector3f(1.0f, 0.0f, 0.0f));
    WEAPON_PRIMARY_AXIS.put("lavitz", new Vector3f(0.0f, 0.0f, -1.0f));
    WEAPON_PRIMARY_AXIS.put("shana", new Vector3f(0.0f, 0.0f, 1.0f));
    WEAPON_PRIMARY_AXIS.put("rose", new Vector3f(0.0f, 0.0f, -1.0f));
    WEAPON_PRIMARY_AXIS.put("albert", new Vector3f(0.0f, 0.0f, -1.0f));
    WEAPON_PRIMARY_AXIS.put("meru", new Vector3f(1.0f, 0.0f, 0.0f));
    WEAPON_PRIMARY_AXIS.put("kongol", new Vector3f(0.0f, 0.0f, -1.0f));
    WEAPON_PRIMARY_AXIS.put("miranda", new Vector3f(0.0f, 0.0f, 1.0f));

    CHARACTER_HEIGHTS.put("dart", 977);

    CHARACTER_HEIGHTS.put("lavitz", 1035);
    CHARACTER_HEIGHTS.put("shana", 1092);
    CHARACTER_HEIGHTS.put("rose", 1132);
    CHARACTER_HEIGHTS.put("haschel", 1039);
    CHARACTER_HEIGHTS.put("albert", 1026);
    CHARACTER_HEIGHTS.put("meru", 1120);
    CHARACTER_HEIGHTS.put("kongol", 1232);
    CHARACTER_HEIGHTS.put("miranda", 1094);

    // Verified bone layouts based on 3D vertex and transform analysis:
    registerBones("dart", new Bone[] {
      Bone.FOREARM_R, Bone.FOREARM_L, Bone.TORSO, Bone.FOOT_R, Bone.FOOT_L,
      Bone.HAND_R, Bone.HAND_L, Bone.HEAD, Bone.PELVIS, Bone.TORSO_EXTRA,
      Bone.SHIN_R, Bone.SHIN_L, Bone.SHOULDER_R, Bone.SHOULDER_L,
      Bone.WEAPON, Bone.THIGH_R, Bone.THIGH_L
    });

    registerBones("lavitz", new Bone[] {
      Bone.FOREARM_R, Bone.FOREARM_L, Bone.TORSO, Bone.WEAPON, Bone.FOOT_R,
      Bone.FOOT_L, Bone.HAND_R, Bone.HAND_L, Bone.HEAD, Bone.PELVIS,
      Bone.SHIN_R, Bone.SHIN_L, Bone.SHOULDER_R, Bone.SHOULDER_L,
      Bone.THIGH_R, Bone.THIGH_L
    });

    registerBones("shana", new Bone[] {
      Bone.FOREARM_R, Bone.FOREARM_L, Bone.WEAPON, Bone.WEAPON, Bone.WEAPON,
      Bone.TORSO_EXTRA, Bone.TORSO, Bone.FOOT_R, Bone.FOOT_L, Bone.HEAD_EXTRA,
      Bone.HAND_R, Bone.HAND_L, Bone.HEAD, Bone.PELVIS,
      Bone.SHIN_R, Bone.SHIN_L, Bone.SHOULDER_R, Bone.SHOULDER_L,
      Bone.THIGH_R, Bone.THIGH_L
    });

    registerBones("rose", new Bone[] {
      Bone.FOREARM_R, Bone.FOREARM_L, Bone.TORSO, Bone.HEAD_EXTRA, Bone.HEAD_EXTRA,
      Bone.FOOT_R, Bone.FOOT_L, Bone.HEAD_EXTRA, Bone.HAND_R, Bone.HAND_L,
      Bone.HEAD, Bone.PELVIS, Bone.SHIN_R, Bone.SHIN_L,
      Bone.SHOULDER_R, Bone.SHOULDER_L, Bone.THIGH_R, Bone.THIGH_L,
      Bone.WEAPON
    });

    registerBones("haschel", new Bone[] {
      Bone.FOREARM_R, Bone.FOREARM_L, Bone.TORSO, Bone.FOOT_R, Bone.FOOT_L,
      Bone.HAND_R, Bone.HAND_L, Bone.HEAD, Bone.HEAD_EXTRA, Bone.HEAD_EXTRA,
      Bone.PELVIS, Bone.SHIN_R, Bone.SHIN_L, Bone.SHOULDER_R, Bone.SHOULDER_L,
      Bone.THIGH_R, Bone.THIGH_L
    });

    registerBones("albert", new Bone[] {
      Bone.FOREARM_R, Bone.FOREARM_L, Bone.TORSO, Bone.WEAPON, Bone.FOOT_L,
      Bone.FOOT_R, Bone.HAND_R, Bone.HAND_L, Bone.HEAD, Bone.PELVIS,
      Bone.TORSO_EXTRA, Bone.TORSO_EXTRA, Bone.TORSO_EXTRA, Bone.TORSO_EXTRA, Bone.TORSO_EXTRA,
      Bone.SHIN_L, Bone.SHIN_R, Bone.SHOULDER_R, Bone.SHOULDER_L,
      Bone.THIGH_L, Bone.THIGH_R
    });

    registerBones("meru", new Bone[] {
      Bone.FOREARM_R, Bone.FOREARM_L, Bone.TORSO, Bone.FOOT_R, Bone.FOOT_L,
      Bone.TORSO_EXTRA, Bone.HAND_R, Bone.HAND_L, Bone.HEAD,
      Bone.HEAD_EXTRA, Bone.HEAD_EXTRA, Bone.HEAD_EXTRA,
      Bone.PELVIS, Bone.TORSO_EXTRA, Bone.TORSO_EXTRA,
      Bone.SHIN_R, Bone.SHIN_L, Bone.SHOULDER_R, Bone.SHOULDER_L,
      Bone.THIGH_R, Bone.THIGH_L, Bone.WEAPON
    });

    registerBones("kongol", new Bone[] {
      Bone.FOREARM_R, Bone.FOREARM_L, Bone.TORSO, Bone.FOOT_R, Bone.FOOT_L,
      Bone.HAND_R, Bone.HAND_L, Bone.HEAD, Bone.PELVIS, Bone.WEAPON,
      Bone.TORSO_EXTRA, Bone.TORSO_EXTRA, Bone.SHIN_R, Bone.SHIN_L,
      Bone.SHOULDER_R, Bone.SHOULDER_L, Bone.THIGH_R, Bone.THIGH_L
    });

    registerBones("miranda", new Bone[] {
      Bone.FOREARM_R, Bone.FOREARM_L, Bone.WEAPON, Bone.WEAPON, Bone.WEAPON,
      Bone.TORSO_EXTRA, Bone.TORSO, Bone.FOOT_R, Bone.FOOT_L, Bone.HAND_R,
      Bone.HAND_L, Bone.HEAD, Bone.PELVIS, Bone.SHIN_R, Bone.SHIN_L,
      Bone.SHOULDER_R, Bone.SHOULDER_L, Bone.THIGH_R, Bone.THIGH_L
    });

    // Ground-truth bone parent graphs extracted from original TMD skeletal hierarchies
    CHARACTER_PARENTS.put("dart", new int[] {
      12, 13, 8, 10, 11, 0, 1, 2, -1, 2, 15, 16, 2, 2, 6, 8, 8
    });
    CHARACTER_PARENTS.put("lavitz", new int[] {
      12, 13, 9, 6, 10, 11, 0, 1, 2, -1, 14, 15, 2, 2, 9, 9
    });
    CHARACTER_PARENTS.put("shana", new int[] {
      16, 17, 3, 10, 3, 6, 13, 14, 15, 12, 0, 1, 6, -1, 18, 19, 6, 6, 13, 13
    });
    CHARACTER_PARENTS.put("rose", new int[] {
      14, 15, 11, 10, 3, 12, 13, 10, 0, 1, 2, -1, 16, 17, 2, 2, 11, 11, 9
    });
    CHARACTER_PARENTS.put("haschel", new int[] {
      13, 14, 10, 11, 12, 0, 1, 2, 7, 7, -1, 15, 16, 2, 2, 10, 10
    });
    CHARACTER_PARENTS.put("albert", new int[] {
      17, 18, 9, 7, 15, 16, 0, 1, 2, -1, 2, 10, 11, 12, 13, 19, 20, 2, 2, 9, 9
    });
    CHARACTER_PARENTS.put("meru", new int[] {
      17, 18, 12, 15, 16, 12, 0, 1, 2, 8, 9, 10, -1, 12, 12, 19, 20, 2, 2, 12, 12, 7
    });
    CHARACTER_PARENTS.put("kongol", new int[] {
      14, 15, 8, 12, 13, 0, 1, 2, -1, 6, 2, 2, 16, 17, 2, 2, 8, 8
    });
    CHARACTER_PARENTS.put("miranda", new int[] {
      15, 16, 3, 9, 3, 6, 12, 13, 14, 0, 1, 6, -1, 17, 18, 6, 6, 12, 12
    });
  }

  private static void registerBones(final String characterKey, final Bone[] bones) {
    CHARACTER_BONES.put(characterKey, bones);
    final Map<Bone, Integer> boneMap = new EnumMap<>(Bone.class);
    for (int p = 0; p < bones.length; p++) {
      final Bone b = bones[p];
      if (!boneMap.containsKey(b)) {
        boneMap.put(b, p);
      }
    }
    // Haschel fallback: weapon -> right hand
    if (!boneMap.containsKey(Bone.WEAPON) && boneMap.containsKey(Bone.HAND_R)) {
      boneMap.put(Bone.WEAPON, boneMap.get(Bone.HAND_R));
    }
    CHARACTER_BONE_TO_PART.put(characterKey, boneMap);
  }

  public static synchronized TmdAnimationFile getDanceAnimation(
      final String targetCharKey, final DanceType dance, final String sourceCharKey
  ) {
    if (targetCharKey == null) {
      return null;
    }
    final String cleanTarget = targetCharKey.toLowerCase().trim();
    final String cleanSource = (sourceCharKey != null) ? sourceCharKey.toLowerCase().trim() : cleanTarget;
    final String cacheKey = cleanTarget + ":" + (dance != null ? dance.name() : "DEFAULT") + ":" + cleanSource;

    if (DANCE_CACHE.containsKey(cacheKey)) {
      return DANCE_CACHE.get(cacheKey);
    }

    TmdAnimationFile anim;
    if (cleanTarget.equals(cleanSource)) {
      anim = loadDefaultVictoryAnimation(cleanTarget);
    } else {
      anim = retargetVictoryAnimation(cleanTarget, cleanSource);
    }

    if (anim != null) {
      // 1.25x scaling slows down playback by ~25% for natural animation pacing
      anim.interpolationScale = 1.25f;
      DANCE_CACHE.put(cacheKey, anim);
    }
    return anim;
  }

  public static TmdAnimationFile getDanceAnimation(final String characterKey, final DanceType dance) {
    final String source = DanceModifierStorage.getSourceCharacter(characterKey);
    return getDanceAnimation(characterKey, dance, source);
  }

  public static TmdAnimationFile getDanceAnimation(final int charId, final DanceType dance) {
    final String target = DanceModifierStorage.getCharacterKey(charId);
    final String source = DanceModifierStorage.getSourceCharacterByCharId(charId);
    return getDanceAnimation(target, dance, source);
  }

  public static int getCharacterCombatPackageDir(final String characterKey) {
    if (characterKey == null) return -1;
    return switch (characterKey.toLowerCase().trim()) {
      case "dart" -> 4031;
      case "lavitz" -> 4039;
      case "shana" -> 4047;
      case "rose" -> 4055;
      case "haschel" -> 4063;
      case "albert" -> 4071;
      case "meru" -> 4079;
      case "kongol" -> 4087;
      case "miranda" -> 4095;
      default -> -1;
    };
  }

  public static TmdAnimationFile loadDefaultVictoryAnimation(final String characterKey) {
    final int dirId = getCharacterCombatPackageDir(characterKey);
    if (dirId > 0) {
      try {
        final String path47 = "SECT/DRGN0.BIN/" + dirId + "/47";
        final String path63 = "SECT/DRGN0.BIN/" + dirId + "/63";

        final byte[] b47 = loadRawFileBytes(path47);
        final byte[] b63 = loadRawFileBytes(path63);

        if (b47 != null && b47.length > 16) {
          if (b63 != null && b63.length > 16) {
            final int parts47 = (b47[0xc] & 0xff) | ((b47[0xd] & 0xff) << 8);
            final int parts63 = (b63[0xc] & 0xff) | ((b63[0xd] & 0xff) << 8);
            if (parts47 == parts63 && parts47 > 0) {
              final int parts = parts47;
              final int frames47 = (b47[0xe] & 0xff) | ((b47[0xf] & 0xff) << 8);
              final int kf47 = frames47 / 2;

              final int frames63 = (b63[0xe] & 0xff) | ((b63[0xf] & 0xff) << 8);
              final int kf63 = frames63 / 2;

              final int repeats = (kf63 > 0) ? Math.max(3, 40 / kf63) : 1;
              final int totalKf = kf47 + kf63 * repeats;
              final int totalFrames = totalKf * 2;

              final int totalBytes = 0x10 + totalKf * parts * 12;
              final ByteBuffer buf = ByteBuffer.allocate(totalBytes).order(ByteOrder.LITTLE_ENDIAN);
              buf.put(b47, 0, 0x10);
              buf.putShort(0xe, (short) totalFrames);

              buf.put(b47, 0x10, kf47 * parts * 12);
              final int len63 = kf63 * parts * 12;
              for (int r = 0; r < repeats; r++) {
                buf.put(b63, 0x10, len63);
              }

              final TmdAnimationFile anim = new TmdAnimationFile(new FileData(buf.array()));
              anim.interpolationScale = 1.25f;
              return anim;
            }
          }
          final TmdAnimationFile anim = new TmdAnimationFile(new FileData(b47));
          anim.interpolationScale = 1.25f;
          return anim;
        }
      } catch (final Throwable t) {
        LOGGER.warn("Failed loading victory anim for %s from dir %d", characterKey, dirId, t);
      }
    }
    return loadIdleAnimation(characterKey);
  }

  private static void loadRestPose(
      final String characterKey, final int parts, final Vector3f[] outTrans, final Quaternionf[] outQuat
  ) {
    final int dirId = getCharacterCombatPackageDir(characterKey);
    byte[] b = (dirId > 0) ? loadRawFileBytes("SECT/DRGN0.BIN/" + dirId + "/58") : null;
    if (b == null || b.length < 0x10 + parts * 12) {
      b = loadRawFileBytes("characters/" + characterKey + "/models/combat/0");
    }
    if (b != null && b.length >= 0x10 + parts * 12) {
      for (int p = 0; p < parts; p++) {
        final int off = 0x10 + p * 12;
        final float rx = (((b[off] & 0xff) | ((b[off + 1] & 0xff) << 8)) & 0x0fff) * (float) (2 * Math.PI / 4096.0);
        final float ry = (((b[off + 2] & 0xff) | ((b[off + 3] & 0xff) << 8)) & 0x0fff) * (float) (2 * Math.PI / 4096.0);
        final float rz = (((b[off + 4] & 0xff) | ((b[off + 5] & 0xff) << 8)) & 0x0fff) * (float) (2 * Math.PI / 4096.0);
        if (outQuat != null) {
          outQuat[p] = new Quaternionf().rotationZYX(rz, ry, rx);
        }
        if (outTrans != null) {
          final short tx = (short) ((b[off + 6] & 0xff) | ((b[off + 7] & 0xff) << 8));
          final short ty = (short) ((b[off + 8] & 0xff) | ((b[off + 9] & 0xff) << 8));
          final short tz = (short) ((b[off + 10] & 0xff) | ((b[off + 11] & 0xff) << 8));
          outTrans[p] = new Vector3f(tx, ty, tz);
        }
      }
      if ("rose".equalsIgnoreCase(characterKey) && parts > 10) {
        // Rose's hair in file 58 was angled forward for combat crouch (pitch 35 and 64 deg).
        // Align hair parts (3 and 4) with her head (part 10) so hair hangs naturally down her back in victory dances.
        if (outQuat != null && outQuat[10] != null) {
          outQuat[3] = new Quaternionf(outQuat[10]);
          outQuat[4] = new Quaternionf(outQuat[10]);
        }
      }
    }
  }

  private static Bone getDanceWeaponHand(final String sourceCharKey) {
    return switch (sourceCharKey.toLowerCase().trim()) {
      case "lavitz", "shana", "miranda" -> Bone.HAND_R;
      default -> Bone.HAND_L; // dart, rose, haschel, albert, meru, kongol
    };
  }

  private static Bone getModelWeaponHand(final String targetCharKey) {
    return switch (targetCharKey.toLowerCase().trim()) {
      case "lavitz", "shana", "miranda" -> Bone.HAND_R;
      default -> Bone.HAND_L; // dart, rose, albert, meru, kongol
    };
  }

  private static Vector3f computeFKPosition(
      final int tp,
      final int[] tgtParents,
      final Bone[] tgtBones,
      final Map<Bone, Integer> tgtMap,
      final int[] tgtToSrc,
      final Vector3f[] restTrans,
      final Vector3f[] srcAnimTrans,
      final Quaternionf[] deltaQ,
      final Vector3f[] computed,
      final boolean[] done,
      final Vector3f pelvisPos,
      final Bone danceWeaponHand,
      final Bone modelWeaponHand,
      final boolean isUnarmedDance
  ) {
    if (done[tp]) return computed[tp];
    final Bone b = (tp < tgtBones.length) ? tgtBones[tp] : Bone.TORSO;

    if (b == Bone.PELVIS) {
      computed[tp] = pelvisPos;
      done[tp] = true;
      return pelvisPos;
    }

    final int parentPart;
    if (b == Bone.WEAPON) {
      final int staticParent = (tgtParents != null && tp < tgtParents.length) ? tgtParents[tp] : -1;
      if (staticParent >= 0 && tgtBones[staticParent] == Bone.WEAPON) {
        parentPart = staticParent;
      } else {
        parentPart = tgtMap.getOrDefault(isUnarmedDance ? modelWeaponHand : danceWeaponHand, -1);
      }
    } else {
      parentPart = (tgtParents != null && tp < tgtParents.length) ? tgtParents[tp] : -1;
    }

    if (parentPart < 0 || parentPart == tp) {
      computed[tp] = pelvisPos;
      done[tp] = true;
      return pelvisPos;
    }

    final Vector3f parentPos = computeFKPosition(
        parentPart, tgtParents, tgtBones, tgtMap, tgtToSrc, restTrans, srcAnimTrans, deltaQ, computed, done, pelvisPos, danceWeaponHand, modelWeaponHand, isUnarmedDance
    );

    if (b == Bone.WEAPON && (tgtParents == null || tp >= tgtParents.length || tgtBones[tgtParents[tp]] != Bone.WEAPON)) {
      if (isUnarmedDance) {
        final Vector3f res = new Vector3f(parentPos);
        computed[tp] = res;
        done[tp] = true;
        return res;
      }
      final Vector3f offset;
      if (danceWeaponHand != modelWeaponHand) {
        final int naturalParentPart = tgtMap.getOrDefault(modelWeaponHand, parentPart);
        final Vector3f naturalOffset = new Vector3f(restTrans[tp]).sub(restTrans[naturalParentPart]);
        offset = new Vector3f(-naturalOffset.x, naturalOffset.y, -naturalOffset.z);
      } else {
        offset = new Vector3f(restTrans[tp]).sub(restTrans[parentPart]);
      }
      final Vector3f rotated = new Vector3f(offset).rotate(deltaQ[parentPart]);
      final Vector3f res = new Vector3f(parentPos).add(rotated);
      computed[tp] = res;
      done[tp] = true;
      return res;
    }

    // Standard FK segment connection: rotate rest segment by parent's delta rotation
    final Vector3f tgtRestSegment = new Vector3f(restTrans[tp]).sub(restTrans[parentPart]);
    final Vector3f rotated = new Vector3f(tgtRestSegment).rotate(deltaQ[parentPart]);
    final Vector3f res = new Vector3f(parentPos).add(rotated);
    computed[tp] = res;
    done[tp] = true;
    return res;
  }

  public static TmdAnimationFile retargetVictoryAnimation(final String targetCharKey, final String sourceCharKey) {
    try {
      final int srcDirId = getCharacterCombatPackageDir(sourceCharKey);
      if (srcDirId <= 0) {
        return loadDefaultVictoryAnimation(targetCharKey);
      }

      final byte[] bSrc47 = loadRawFileBytes("SECT/DRGN0.BIN/" + srcDirId + "/47");
      final byte[] bSrc63 = loadRawFileBytes("SECT/DRGN0.BIN/" + srcDirId + "/63");
      if (bSrc47 == null || bSrc47.length < 16) {
        return loadDefaultVictoryAnimation(targetCharKey);
      }

      final int srcParts = (bSrc47[0xc] & 0xff) | ((bSrc47[0xd] & 0xff) << 8);
      final byte[] bTgt0 = loadRawFileBytes("characters/" + targetCharKey + "/models/combat/0");
      if (bTgt0 == null || bTgt0.length < 16) {
        return loadDefaultVictoryAnimation(targetCharKey);
      }
      final int tgtParts = (bTgt0[0xc] & 0xff) | ((bTgt0[0xd] & 0xff) << 8);

      final Bone[] tgtBones = CHARACTER_BONES.get(targetCharKey);
      final Map<Bone, Integer> tgtBoneMap = CHARACTER_BONE_TO_PART.get(targetCharKey);
      final Map<Bone, Integer> srcBoneMap = CHARACTER_BONE_TO_PART.get(sourceCharKey);
      if (tgtBones == null || tgtBoneMap == null || srcBoneMap == null) {
        return loadDefaultVictoryAnimation(targetCharKey);
      }

      final Bone danceWeaponHand = getDanceWeaponHand(sourceCharKey);
      final Bone modelWeaponHand = getModelWeaponHand(targetCharKey);
      final int[] tgtParents = CHARACTER_PARENTS.get(targetCharKey);
      final Vector3f srcWpAxis = WEAPON_PRIMARY_AXIS.get(sourceCharKey.toLowerCase().trim());
      final Vector3f tgtWpAxis = WEAPON_PRIMARY_AXIS.get(targetCharKey.toLowerCase().trim());
      final boolean isUnarmedDance = (srcWpAxis == null);

      // Height scale ratio for proportionate movement
      final int tgtHeight = CHARACTER_HEIGHTS.getOrDefault(targetCharKey, 1000);
      final int srcHeight = CHARACTER_HEIGHTS.getOrDefault(sourceCharKey, 1000);
      final float heightScale = (srcHeight > 0) ? ((float) tgtHeight / (float) srcHeight) : 1.0f;

      // Target to source part index lookup with fallback
      final int[] tgtToSrc = new int[tgtParts];
      for (int tp = 0; tp < tgtParts; tp++) {
        final Bone bone = (tp < tgtBones.length) ? tgtBones[tp] : Bone.TORSO;
        tgtToSrc[tp] = getSourcePartIndex(bone, srcBoneMap);
      }

      // Rest poses
      final Vector3f[] tgtRestTrans = new Vector3f[tgtParts];
      final Quaternionf[] tgtRestQuat = new Quaternionf[tgtParts];
      loadRestPose(targetCharKey, tgtParts, tgtRestTrans, tgtRestQuat);

      final Vector3f[] srcRestTrans = new Vector3f[srcParts];
      final Quaternionf[] srcRestQuat = new Quaternionf[srcParts];
      loadRestPose(sourceCharKey, srcParts, srcRestTrans, srcRestQuat);

      final int srcPelvisPart = srcBoneMap.getOrDefault(Bone.PELVIS, 0);
      final int tgtPelvisPart = tgtBoneMap.getOrDefault(Bone.PELVIS, 0);
      final int srcFootR = srcBoneMap.getOrDefault(Bone.FOOT_R, -1);
      final int srcFootL = srcBoneMap.getOrDefault(Bone.FOOT_L, -1);
      final int tgtFootR = tgtBoneMap.getOrDefault(Bone.FOOT_R, -1);
      final int tgtFootL = tgtBoneMap.getOrDefault(Bone.FOOT_L, -1);

      // Determine ground plane and standing leg distance from source animation loop (file 63)
      final float srcAnimGroundY;
      final float srcStandingLegDist;
      if (bSrc63 != null && bSrc63.length > 0x10 && srcFootR >= 0 && srcFootL >= 0) {
        final short sFRY = (short) ((bSrc63[0x10 + srcFootR * 12 + 8] & 0xff) | ((bSrc63[0x10 + srcFootR * 12 + 9] & 0xff) << 8));
        final short sFLY = (short) ((bSrc63[0x10 + srcFootL * 12 + 8] & 0xff) | ((bSrc63[0x10 + srcFootL * 12 + 9] & 0xff) << 8));
        final short sPelvY = (short) ((bSrc63[0x10 + srcPelvisPart * 12 + 8] & 0xff) | ((bSrc63[0x10 + srcPelvisPart * 12 + 9] & 0xff) << 8));
        srcAnimGroundY = Math.max(sFRY, sFLY);
        srcStandingLegDist = Math.max(10.0f, srcAnimGroundY - sPelvY);
      } else if (srcFootR >= 0 && srcFootL >= 0) {
        srcAnimGroundY = Math.max(srcRestTrans[srcFootR].y, srcRestTrans[srcFootL].y);
        srcStandingLegDist = Math.max(10.0f, srcAnimGroundY - srcRestTrans[srcPelvisPart].y);
      } else {
        srcAnimGroundY = 0.0f;
        srcStandingLegDist = 100.0f;
      }

      // Concatenate keyframes from 47 + repeats of 63
      final int kf47 = ((bSrc47[0xe] & 0xff) | ((bSrc47[0xf] & 0xff) << 8)) / 2;
      int kf63 = 0;
      int repeats = 0;
      if (bSrc63 != null && bSrc63.length > 16) {
        kf63 = ((bSrc63[0xe] & 0xff) | ((bSrc63[0xf] & 0xff) << 8)) / 2;
        repeats = (kf63 > 0) ? Math.max(3, 40 / kf63) : 1;
      }
      final int totalKf = kf47 + kf63 * repeats;
      final int totalFrames = totalKf * 2;

      final ByteBuffer buf = ByteBuffer.allocate(0x10 + totalKf * tgtParts * 12).order(ByteOrder.LITTLE_ENDIAN);
      buf.put(bTgt0, 0, 0x10);
      buf.putShort(0xe, (short) totalFrames);

      final Vector3f tempEuler = new Vector3f();
      final Quaternionf[] deltaQ = new Quaternionf[tgtParts];
      final Vector3f[] computedPositions = new Vector3f[tgtParts];
      final boolean[] isComputed = new boolean[tgtParts];
      final Vector3f[] srcAnimTrans = new Vector3f[srcParts];
      final Quaternionf[] srcAnimQuat = new Quaternionf[srcParts];

      for (int k = 0; k < totalKf; k++) {
        final byte[] srcBytes;
        final int srcKfIndex;
        if (k < kf47) {
          srcBytes = bSrc47;
          srcKfIndex = k;
        } else {
          srcBytes = bSrc63;
          srcKfIndex = (kf63 > 0) ? ((k - kf47) % kf63) : 0;
        }

        final int srcKfOffset = 0x10 + srcKfIndex * srcParts * 12;

        for (int sp = 0; sp < srcParts; sp++) {
          final int spOff = srcKfOffset + sp * 12;
          final float srcRx = (((srcBytes[spOff] & 0xff) | ((srcBytes[spOff + 1] & 0xff) << 8)) & 0x0fff) * (float) (2 * Math.PI / 4096.0);
          final float srcRy = (((srcBytes[spOff + 2] & 0xff) | ((srcBytes[spOff + 3] & 0xff) << 8)) & 0x0fff) * (float) (2 * Math.PI / 4096.0);
          final float srcRz = (((srcBytes[spOff + 4] & 0xff) | ((srcBytes[spOff + 5] & 0xff) << 8)) & 0x0fff) * (float) (2 * Math.PI / 4096.0);
          srcAnimQuat[sp] = new Quaternionf().rotationZYX(srcRz, srcRy, srcRx);

          final short tx = (short) ((srcBytes[spOff + 6] & 0xff) | ((srcBytes[spOff + 7] & 0xff) << 8));
          final short ty = (short) ((srcBytes[spOff + 8] & 0xff) | ((srcBytes[spOff + 9] & 0xff) << 8));
          final short tz = (short) ((srcBytes[spOff + 10] & 0xff) | ((srcBytes[spOff + 11] & 0xff) << 8));
          srcAnimTrans[sp] = new Vector3f(tx, ty, tz);
        }

        // Step 1: Compute 3D world space delta rotations for all target parts
        for (int tp = 0; tp < tgtParts; tp++) {
          final int sp = tgtToSrc[tp];
          deltaQ[tp] = new Quaternionf(srcAnimQuat[sp]).mul(new Quaternionf(srcRestQuat[sp]).invert());
        }

        // Step 2: Compute Pelvis world displacement & grounding
        final Vector3f pelvisPos;
        if (srcFootR >= 0 && srcFootL >= 0 && tgtFootR >= 0 && tgtFootL >= 0) {
          final float srcAnimFloorY = Math.max(srcAnimTrans[srcFootR].y, srcAnimTrans[srcFootL].y);
          final float srcAnimLegDist = srcAnimFloorY - srcAnimTrans[srcPelvisPart].y;
          final float extensionRatio = (srcStandingLegDist > 10.0f) ? (srcAnimLegDist / srcStandingLegDist) : 1.0f;

          final float tgtRestFloorY = Math.max(tgtRestTrans[tgtFootR].y, tgtRestTrans[tgtFootL].y);
          final float tgtRestLegDist = tgtRestFloorY - tgtRestTrans[tgtPelvisPart].y;
          final float jumpDist = Math.max(0.0f, (srcAnimGroundY - srcAnimFloorY) * heightScale);
          final float tgtPelvisY = (tgtRestFloorY - jumpDist) - (tgtRestLegDist * extensionRatio);

          final float dPx = (srcAnimTrans[srcPelvisPart].x - srcRestTrans[srcPelvisPart].x) * heightScale;
          final float dPz = (srcAnimTrans[srcPelvisPart].z - srcRestTrans[srcPelvisPart].z) * heightScale;
          pelvisPos = new Vector3f(tgtRestTrans[tgtPelvisPart].x + dPx, tgtPelvisY, tgtRestTrans[tgtPelvisPart].z + dPz);
        } else {
          final float dPx = (srcAnimTrans[srcPelvisPart].x - srcRestTrans[srcPelvisPart].x) * heightScale;
          final float dPy = (srcAnimTrans[srcPelvisPart].y - srcRestTrans[srcPelvisPart].y) * heightScale;
          final float dPz = (srcAnimTrans[srcPelvisPart].z - srcRestTrans[srcPelvisPart].z) * heightScale;
          pelvisPos = new Vector3f(tgtRestTrans[tgtPelvisPart]).add(dPx, dPy, dPz);
        }

        // Step 3: Compute Forward Kinematics translations for all parts
        for (int tp = 0; tp < tgtParts; tp++) {
          isComputed[tp] = false;
        }
        for (int tp = 0; tp < tgtParts; tp++) {
          computeFKPosition(tp, tgtParents, tgtBones, tgtBoneMap, tgtToSrc, tgtRestTrans, srcAnimTrans, deltaQ, computedPositions, isComputed, pelvisPos, danceWeaponHand, modelWeaponHand, isUnarmedDance);
        }

        // Step 4: Write rotations and FK positions to buffer
        for (int tp = 0; tp < tgtParts; tp++) {
          final Bone bone = (tp < tgtBones.length) ? tgtBones[tp] : Bone.TORSO;
          final int sp = tgtToSrc[tp];

          final Quaternionf qTgt;
          if (bone == Bone.WEAPON) {
            if (srcWpAxis != null && tgtWpAxis != null) {
              final Vector3f srcWpDir = new Vector3f(srcWpAxis).rotate(srcAnimQuat[sp]).normalize();
              final Vector3f tgtWpRestDir = new Vector3f(tgtWpAxis).rotate(tgtRestQuat[tp]).normalize();
              final Quaternionf qAlign = new Quaternionf().rotationTo(tgtWpRestDir, srcWpDir);
              qTgt = new Quaternionf(qAlign).mul(tgtRestQuat[tp]);
            } else if (tgtWpAxis != null && isUnarmedDance) {
              // Unarmed dance (e.g. Haschel): point weapon down along hip/leg
              final Vector3f targetDir = new Vector3f(0.0f, 1.0f, 0.0f);
              final Vector3f tgtWpRestDir = new Vector3f(tgtWpAxis).rotate(tgtRestQuat[tp]).normalize();
              final Quaternionf qAlign = new Quaternionf().rotationTo(tgtWpRestDir, targetDir);
              qTgt = new Quaternionf(qAlign).mul(tgtRestQuat[tp]);
            } else {
              final int handPart = tgtBoneMap.getOrDefault(danceWeaponHand, tp);
              final Quaternionf rotDelta = deltaQ[handPart];
              if (danceWeaponHand != modelWeaponHand) {
                final Quaternionf mirrorY = new Quaternionf().rotateY((float) Math.PI);
                qTgt = new Quaternionf(rotDelta).mul(tgtRestQuat[tp]).mul(mirrorY);
              } else {
                qTgt = new Quaternionf(rotDelta).mul(tgtRestQuat[tp]);
              }
            }
          } else {
            qTgt = new Quaternionf(deltaQ[tp]).mul(tgtRestQuat[tp]);
          }

          qTgt.getEulerAnglesZYX(tempEuler);
          final short tgtRotX = toAngle(tempEuler.x);
          final short tgtRotY = toAngle(tempEuler.y);
          final short tgtRotZ = toAngle(tempEuler.z);

          final Vector3f pos = computedPositions[tp];
          final short tgtTx = clampShort(pos.x);
          final short tgtTy = clampShort(pos.y);
          final short tgtTz = clampShort(pos.z);

          buf.putShort(tgtRotX);
          buf.putShort(tgtRotY);
          buf.putShort(tgtRotZ);
          buf.putShort(tgtTx);
          buf.putShort(tgtTy);
          buf.putShort(tgtTz);
        }
      }

      final TmdAnimationFile anim = new TmdAnimationFile(new FileData(buf.array()));
      anim.interpolationScale = 1.25f;
      LOGGER.info("Successfully retargeted victory dance with FK from %s to %s (%d frames)", sourceCharKey, targetCharKey, totalFrames);
      return anim;
    } catch (final Throwable t) {
      LOGGER.error("Failed retargeting victory dance from %s to %s", sourceCharKey, targetCharKey, t);
      return loadDefaultVictoryAnimation(targetCharKey);
    }
  }

  private static int getSourcePartIndex(final Bone b, final Map<Bone, Integer> srcMap) {
    if (srcMap.containsKey(b)) return srcMap.get(b);
    if (b == Bone.TORSO_EXTRA && srcMap.containsKey(Bone.TORSO)) return srcMap.get(Bone.TORSO);
    if (b == Bone.HEAD_EXTRA && srcMap.containsKey(Bone.HEAD)) return srcMap.get(Bone.HEAD);
    if (b == Bone.WEAPON) {
      if (srcMap.containsKey(Bone.WEAPON)) return srcMap.get(Bone.WEAPON);
      if (srcMap.containsKey(Bone.HAND_L)) return srcMap.get(Bone.HAND_L);
      if (srcMap.containsKey(Bone.HAND_R)) return srcMap.get(Bone.HAND_R);
    }
    return srcMap.getOrDefault(Bone.TORSO, 0);
  }

  private static short toAngle(final float rad) {
    final int v = Math.round((float) ((rad / (2.0 * Math.PI)) * 4096.0));
    return (short) (((v % 4096) + 4096) % 4096);
  }

  private static short clampShort(final float val) {
    return (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(val)));
  }

  public static TmdAnimationFile loadIdleAnimation(final String characterKey) {
    try {
      final String path = "characters/" + characterKey + "/models/combat/0";
      if (Loader.exists(path)) {
        FileData data = Loader.loadFileSync(path);
        if (data.size() > 16) {
          if (data.readInt(0x4) == 0x1a45_5042) {
            data = new FileData(Unpacker.decompress(data));
          }
          final TmdAnimationFile anim = new TmdAnimationFile(data);
          anim.interpolationScale = 1.25f;
          return anim;
        }
      }
    } catch (final Throwable t) {
      LOGGER.warn("Failed loading idle anim for %s", characterKey, t);
    }
    return null;
  }

  private static byte[] loadRawFileBytes(final String path) {
    try {
      if (Loader.exists(path)) {
        FileData data = Loader.loadFileSync(path);
        if (data.size() > 16) {
          if (data.readInt(0x4) == 0x1a45_5042) {
            data = new FileData(Unpacker.decompress(data));
          }
          return data.getBytes();
        }
      }
      final java.io.File diskFile = new java.io.File("files/" + path);
      if (diskFile.exists()) {
        FileData data = new FileData(java.nio.file.Files.readAllBytes(diskFile.toPath()));
        if (data.size() > 16) {
          if (data.readInt(0x4) == 0x1a45_5042) {
            data = new FileData(Unpacker.decompress(data));
          }
          return data.getBytes();
        }
      }
    } catch (final Throwable t) {
      LOGGER.warn("Failed loading file bytes: %s", path, t);
    }
    return null;
  }
}
