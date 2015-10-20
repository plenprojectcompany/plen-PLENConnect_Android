package jp.plen.scenography.fragments;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.eccyan.optional.Optional;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import java.util.Collections;

import de.greenrobot.event.EventBus;
import jp.plen.rx.binding.ReadOnlyProperty;
import jp.plen.scenography.Scenography;
import jp.plen.scenography.models.PlenProgramModel;
import jp.plen.scenography.models.entities.PlenProgram;
import jp.plen.scenography.services.PlenConnectionService;
import jp.plen.scenography.utils.PlenCommandUtil;
import rx.functions.Actions;
import rx.subscriptions.CompositeSubscription;

@EBean
public class ProgrammingFragmentPresenter {
    private static final String TAG = ProgrammingFragmentPresenter.class.getSimpleName();
    @RootContext Context mContext;
    private final CompositeSubscription mSubscriptions = new CompositeSubscription();
    @NonNull private Optional<IProgramFragment> mView = Optional.empty();
    @NonNull private Optional<PlenProgramModel> mCurrentProgram = Optional.empty();

    public void bind(@NonNull IProgramFragment view) {
        Log.d(TAG, "bind ");
        mView = Optional.of(view);
        mCurrentProgram = Optional.of(Scenography.getModel().currentProgram());

        // fetch program
        mCurrentProgram.ifPresent(program ->
                program.fetch().subscribe(Actions.empty(), e -> Log.e(TAG, "fetch error", e)));
        mSubscriptions.add(view.bind(mCurrentProgram.get()));
    }

    public void unbind() {
        Log.d(TAG, "onDestroy ");
        mSubscriptions.clear();

        // push program
        mCurrentProgram.ifPresent(program ->
                program.push().subscribe(Actions.empty(), e -> Log.e(TAG, "push error", e)));

        mView = Optional.empty();
        mCurrentProgram = Optional.empty();
    }
}
