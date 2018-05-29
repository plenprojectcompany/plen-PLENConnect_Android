package jp.plen.plenconnect2.utils;

import android.support.annotation.NonNull;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

public class WatchDogTimer {
    private static final String TAG = WatchDogTimer.class.getSimpleName();
    private Observable mTimer;
    @NonNull private Subscription mSubscription = Subscriptions.empty();

    public WatchDogTimer() {
    }

    public void init(Action0 action, long delay, TimeUnit unit) {
        mTimer = Observable.timer(delay, unit).doOnCompleted(action);
    }

    public void stop() {
        mSubscription.unsubscribe();
    }

    public void restart() {
        stop();
        if (mTimer != null) {
            mSubscription = mTimer.subscribe();
        }
    }
}
