package jp.plen.scenography.fragments;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.eccyan.optional.Optional;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import de.greenrobot.event.EventBus;
import jp.plen.scenography.Scenography;
import jp.plen.scenography.models.PlenProgramModel;
import jp.plen.scenography.models.entities.PlenWalk;
import jp.plen.scenography.services.PlenConnectionService;
import jp.plen.scenography.utils.PlenCommandUtil;
import rx.functions.Actions;
import rx.subscriptions.CompositeSubscription;

@EBean
public class JoystickFragmentPresenter {
    private static final String TAG = JoystickFragmentPresenter.class.getSimpleName();
    private final CompositeSubscription mSubscriptions = new CompositeSubscription();
    @RootContext Context mContext;
    @NonNull private Optional<IJoystickFragment> mView = Optional.empty();
    @NonNull private Optional<PlenProgramModel> mCurrentProgram = Optional.empty();
    @NonNull private Optional<PlenWalk> mLastWalk = Optional.empty();
    private boolean mWritable;

    public void bind(@NonNull IJoystickFragment view) {
        Log.d(TAG, "bind ");
        mView = Optional.of(view);
        mCurrentProgram = Optional.of(Scenography.getModel().currentProgram());

        EventBus.getDefault().register(this);

        // fetch program
        mCurrentProgram.ifPresent(program ->
                program.fetch().subscribe(Actions.empty(), e -> Log.e(TAG, "fetch error", e)));
        mSubscriptions.add(view.bind(mCurrentProgram.get()));

        EventBus.getDefault().post(new PlenConnectionService.StateNotificationRequest());
    }

    public void unbind() {
        Log.d(TAG, "onDestroy ");
        mSubscriptions.clear();

        EventBus.getDefault().unregister(this);

        // push program
        mCurrentProgram.ifPresent(program ->
                program.push().subscribe(Actions.empty(), e -> Log.e(TAG, "push error", e)));

        mView = Optional.empty();
        mCurrentProgram = Optional.empty();
    }

    public void onEvent(@NonNull PlenConnectionService.StateNotification notification) {
        switch (notification.getState()) {
            case SERVICE_IDLE:
                mWritable = true;
                break;
            default:
                mWritable = false;
                break;
        }
    }

    public synchronized void movePlen(@NonNull PlenWalk.Mode mode, double direction) {
        if (!mWritable) return;

        direction = Math.atan(Math.tan(direction / 2 - Math.PI * 3 / 8)) / Math.PI * 4 + 2;
        PlenWalk.Direction dir = PlenWalk.Direction.FORWARD;
        switch ((int) direction) {
            case 0:
                dir = PlenWalk.Direction.RIGHT;
                break;
            case 1:
                dir = PlenWalk.Direction.BACK;
                break;
            case 2:
                dir = PlenWalk.Direction.LEFT;
                break;
        }

        PlenWalk walk = new PlenWalk(mode, dir);
        if (mLastWalk.map(w -> !w.equals(walk)).orElse(false)) {
            stopPlen();
        }
        mLastWalk = Optional.of(walk);

        mWritable = false;
        EventBus.getDefault().post(new PlenConnectionService
                .WriteRequest(PlenCommandUtil.toCommand(walk)));
    }

    public synchronized void stopPlen() {
        String command = PlenCommandUtil.STOP_MOTION + PlenCommandUtil.STOP_MOTION;
        EventBus.getDefault().post(new PlenConnectionService
                .WriteRequest(command));
    }
}
