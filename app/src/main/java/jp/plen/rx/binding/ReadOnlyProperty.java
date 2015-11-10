package jp.plen.rx.binding;

import android.support.annotation.NonNull;

import com.eccyan.optional.Optional;

import java.util.NoSuchElementException;

import rx.Observable;

public class ReadOnlyProperty<T> {
    @NonNull private Observable<T> mObservable;

    protected ReadOnlyProperty(@NonNull Observable<T> observable) {
        mObservable = Observable.concat(Observable.just(null), observable);
    }

    @NonNull
    public static <T> ReadOnlyProperty<T> create(@NonNull Observable<T> observable) {
        return new ReadOnlyProperty<>(observable);
    }

    @NonNull
    public final Observable<T> asObservable() {
        return mObservable.skip(1);
    }

    @NonNull
    public final Optional<T> get() {
        return getLatest();
    }

    @NonNull
    protected Optional<T> getLatest() {
        try {
            return Optional.ofNullable(mObservable.toBlocking().latest().iterator().next());
        } catch (NoSuchElementException e) {
            return Optional.ofNullable(mObservable.toBlocking().last());
        }
    }
}
