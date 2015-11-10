package jp.plen.rx.binding;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.eccyan.optional.Optional;

import java.util.Objects;

import jp.plen.rx.utils.Operators;
import rx.Observable;
import rx.Subscription;
import rx.subjects.BehaviorSubject;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

public class Property<T> extends ReadOnlyProperty<T> {
    private static final String TAG = Property.class.getSimpleName();
    @NonNull private final BehaviorSubject<T> mSubject;

    protected Property(@NonNull BehaviorSubject<T> subject) {
        super(subject);
        mSubject = subject;
    }

    @NonNull
    public static <T> Property<T> create() {
        return new Property<>(BehaviorSubject.create());
    }

    @NonNull
    public static <T> Property<T> create(T defaultValue) {
        return new Property<>(BehaviorSubject.create(defaultValue));
    }

    @NonNull
    public static <T> Subscription bindBidirectional(@NonNull Property<T> p1, @NonNull Property<T> p2) {
        if (p1 == p2) {
            return Subscriptions.unsubscribed();
        }
        Subscription s1 = p1.bind(p2);
        Subscription s2 = p2.bind(p1);
        return Subscriptions.from(s1, s2);
    }

    public final void set(@Nullable T t) {
        if (!Objects.equals(t, mSubject.getValue())) {
            mSubject.onNext(t);
        }
    }

    @NonNull
    public final Subscription bind(@NonNull Observable<T> observable) {
        return observable.subscribe(this::set, e -> Log.e(TAG, "onError", e));
    }

    @NonNull
    public final Subscription bind(@NonNull ReadOnlyProperty<T> property) {
        return bind(property.asObservable());
    }

    @NonNull
    @Override
    protected final Optional<T> getLatest() {
        return Optional.ofNullable(mSubject.getValue());
    }
}
