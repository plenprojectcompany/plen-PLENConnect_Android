package jp.plen.plenconnect2.models.entities;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import rx.Observable;

public class PlenProgram {
    private static final String TAG = PlenProgram.class.getSimpleName();
    @NonNull private final List<PlenMotionCategory> motionCategories;
    @NonNull private final List<PlenCodeUnit> sequence;

    public PlenProgram(@NonNull List<PlenMotionCategory> motionCategories, @NonNull List<PlenCodeUnit> sequence) {
        this.motionCategories = Collections.unmodifiableList(new ArrayList<>(motionCategories));
        this.sequence = Collections.unmodifiableList(new ArrayList<>(sequence));
    }

    @NonNull
    public List<PlenCodeUnit> getSequence() {
        return sequence;
    }

    @NonNull
    public Map<Integer, PlenMotion> getMotionMap() {
        return Collections.unmodifiableMap(
                Observable.from(motionCategories)
                        .map(PlenMotionCategory::getMotions)
                        .flatMap(Observable::from)
                        .toMap(PlenMotion::getId)
                        .toBlocking()
                        .single());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlenProgram)) return false;
        PlenProgram program = (PlenProgram) o;
        return Objects.equals(motionCategories, program.motionCategories) &&
                Objects.equals(sequence, program.sequence);
    }

    @Override
    public int hashCode() {
        return Objects.hash(motionCategories, sequence);
    }
}
