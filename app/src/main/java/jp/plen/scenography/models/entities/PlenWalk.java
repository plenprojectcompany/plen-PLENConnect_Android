package jp.plen.scenography.models.entities;

import android.support.annotation.NonNull;

import java.util.Objects;

public class PlenWalk {
    @NonNull private final Mode mMode;
    @NonNull private final Direction mDirection;

    public PlenWalk(@NonNull Mode mode, @NonNull Direction direction) {
        mMode = mode;
        mDirection = direction;
    }

    @NonNull
    public Mode getMode() {
        return mMode;
    }

    @NonNull
    public Direction getDirection() {
        return mDirection;
    }

    public enum Mode {
        NORMAL,
        BOX,
        ROLLER_SKATING,;
    }

    public enum Direction {
        FORWARD,
        BACK,
        LEFT,
        RIGHT,;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlenWalk)) return false;
        PlenWalk walk = (PlenWalk) o;
        return Objects.equals(mMode, walk.mMode) &&
                Objects.equals(mDirection, walk.mDirection);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mMode, mDirection);
    }
}
