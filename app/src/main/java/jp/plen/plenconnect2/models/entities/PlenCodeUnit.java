package jp.plen.plenconnect2.models.entities;

import android.support.annotation.NonNull;

import java.io.Serializable;
import java.util.Objects;

public class PlenCodeUnit implements Serializable, Cloneable {
    private static final long serialVersionUID = 4755656351457424484L;

    private final int motionId;
    private final int loopCount;

    public PlenCodeUnit(int motionId, int loopCount) {
        this.motionId = motionId;
        this.loopCount = loopCount;
    }

    public int getMotionId() {
        return motionId;
    }

    public int getLoopCount() {
        return loopCount;
    }

    @NonNull
    @Override
    public PlenCodeUnit clone() {
        try {
            return (PlenCodeUnit) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlenCodeUnit)) return false;
        PlenCodeUnit that = (PlenCodeUnit) o;
        return Objects.equals(loopCount, that.loopCount) &&
                Objects.equals(motionId, that.motionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(motionId, loopCount);
    }

    @NonNull
    @Override
    public String toString() {
        return "PlenCodeUnit{" +
                "motion=" + String.format("0x%02X", motionId) +
                ", loop=" + loopCount +
                '}';
    }
}
