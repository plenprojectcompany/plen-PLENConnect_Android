package jp.plen.rx.subscriptions;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Subscription;
import rx.exceptions.Exceptions;

/**
 * Subscriptionをキーで管理するためのクラス.
 * Created by kzm4269 on 15/09/07.
 */
public class SubscriptionMap<T> implements Subscription {
    @Nullable private Map<T, Subscription> subscriptions;
    private volatile boolean unsubscribed;

    public SubscriptionMap(@NonNull Map<T, Subscription> subscriptions) {
        this.subscriptions = new HashMap<>(subscriptions);
    }

    public SubscriptionMap() {
    }

    private static void unsubscribeFromAll(@Nullable Collection<Subscription> subscriptions) {
        if (subscriptions == null) {
            return;
        }
        List<Throwable> es = null;
        for (Subscription s : subscriptions) {
            try {
                s.unsubscribe();
            } catch (Throwable e) {
                if (es == null) {
                    es = new ArrayList<>();
                }
                es.add(e);
            }
        }
        Exceptions.throwIfAny(es);
    }

    public void put(final T key, @NonNull final Subscription s) {
        if (s.isUnsubscribed()) {
            return;
        }
        remove(key);
        if (!unsubscribed) {
            synchronized (this) {
                if (!unsubscribed) {
                    if (subscriptions == null) {
                        subscriptions = new HashMap<>(4);
                    }
                    subscriptions.put(key, s);
                    return;
                }
            }
        }
        // call after leaving the synchronized block so we're not holding a lock while executing this
        s.unsubscribe();
    }

    public void remove(final T key) {
        if (!unsubscribed) {
            Subscription s;
            synchronized (this) {
                if (unsubscribed || subscriptions == null) {
                    return;
                }
                s = subscriptions.remove(key);
            }
            if (s != null) {
                // if we removed successfully we then need to call unsubscribe on it (outside of the lock)
                s.unsubscribe();
            }
        }
    }

    public void clear() {
        if (!unsubscribed) {
            Collection<Subscription> unsubscribe;
            synchronized (this) {
                if (unsubscribed || subscriptions == null) {
                    return;
                } else {
                    unsubscribe = subscriptions.values();
                    subscriptions = null;
                }
            }
            unsubscribeFromAll(unsubscribe);
        }
    }

    @Override
    public void unsubscribe() {
        if (!unsubscribed) {
            Collection<Subscription> unsubscribe = null;
            synchronized (this) {
                if (unsubscribed) {
                    return;
                }
                unsubscribed = true;
                if (subscriptions != null) {
                    unsubscribe = subscriptions.values();
                }
                subscriptions = null;
            }
            // we will only get here once
            unsubscribeFromAll(unsubscribe);
        }
    }

    @Override
    public boolean isUnsubscribed() {
        return unsubscribed;
    }

    public boolean hasSubscriptions() {
        if (!unsubscribed) {
            synchronized (this) {
                return !unsubscribed && subscriptions != null && !subscriptions.isEmpty();
            }
        }
        return false;
    }
}
