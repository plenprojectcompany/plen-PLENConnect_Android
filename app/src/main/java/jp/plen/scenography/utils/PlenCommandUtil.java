package jp.plen.scenography.utils;

import android.support.annotation.NonNull;

import java.util.List;

import jp.plen.scenography.models.entities.PlenCodeUnit;
import jp.plen.scenography.models.entities.PlenMotion;
import jp.plen.scenography.models.entities.PlenWalk;

/**
 * PLENとのBLE通信時のコマンド関連.
 */
public final class PlenCommandUtil {
    public static final String STOP_MOTION = "$SM";
    public static final String PLAY_MOTION = "$PM";
    private static final String TAG = PlenCommandUtil.class.getSimpleName();

    private PlenCommandUtil() {
    }

    public static String toCommand(@NonNull PlenMotion motion) {
        return String.format(PLAY_MOTION + "%02X", motion.getId());
    }

    public static String toCommand(@NonNull List<PlenCodeUnit> program) {
        StringBuilder builder = new StringBuilder("#RI#RI");
        for (PlenCodeUnit unit : program) {
            builder.append(String.format("#PU%02X%02X", unit.getMotionId(), unit.getLoopCount() - 1));
        }
        return builder.append("#PO").toString();
    }

    public static String toCommand(@NonNull PlenWalk walk) {
        switch (walk.getMode()) {
            case NORMAL:
                switch (walk.getDirection()) {
                    case FORWARD:
                        return PLAY_MOTION + "46";
                    case LEFT:
                        return PLAY_MOTION + "47";
                    case RIGHT:
                        return PLAY_MOTION + "48";
                    case BACK:
                        return PLAY_MOTION + "49";
                    default:
                        throw new AssertionError();
                }
            case BOX:
                switch (walk.getDirection()) {
                    case FORWARD:
                        return PLAY_MOTION + "4A";
                    case LEFT:
                        return PLAY_MOTION + "4B";
                    case RIGHT:
                        return PLAY_MOTION + "4C";
                    case BACK:
                        return PLAY_MOTION + "4D";
                    default:
                        throw new AssertionError();
                }
            case ROLLER_SKATING:
                switch (walk.getDirection()) {
                    case FORWARD:
                        return PLAY_MOTION + "4E";
                    case LEFT:
                        return PLAY_MOTION + "4F";
                    case RIGHT:
                        return PLAY_MOTION + "50";
                    case BACK:
                        return PLAY_MOTION + "51";
                    default:
                        throw new AssertionError();
                }
            default:
                throw new AssertionError();
        }
    }
}
