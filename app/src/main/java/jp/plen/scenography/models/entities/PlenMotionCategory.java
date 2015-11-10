package jp.plen.scenography.models.entities;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import rx.Observable;

public class PlenMotionCategory {
    @NonNull private final String name;
    @NonNull private final Set<PlenMotion> motions;

    public PlenMotionCategory(@NonNull String name, @NonNull Collection<PlenMotion> motions) {
        this.name = name;
        this.motions = Collections.unmodifiableSet(new HashSet<>(motions));
    }

    @NonNull
    public static PlenMotionCategory empty() {
        return new PlenMotionCategory("", new ArrayList<>());
    }

    public static Set<PlenMotion> toMotions(@NonNull Iterable<PlenMotionCategory> categories) {
        return Observable.from(categories)
                .map(category -> category.motions)
                .<Set<PlenMotion>>reduce(new HashSet<>(), (a, b) -> {
                    a.addAll(b);
                    return a;
                })
                .toBlocking().single();
    }

    public static Set<PlenMotion> toMotions(@NonNull PlenMotionCategory... categories) {
        return toMotions(Arrays.asList(categories));
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public Set<PlenMotion> getMotions() {
        return motions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlenMotionCategory)) return false;
        PlenMotionCategory category = (PlenMotionCategory) o;
        return Objects.equals(name, category.name) &&
                Objects.equals(motions, category.motions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, motions);
    }
}
