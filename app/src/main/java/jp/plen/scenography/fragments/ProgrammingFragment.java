package jp.plen.scenography.fragments;

import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.widget.ImageButton;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import java.util.Collections;

import de.greenrobot.event.EventBus;
import jp.plen.rx.binding.Property;
import jp.plen.scenography.R;
import jp.plen.scenography.Scenography;
import jp.plen.scenography.models.PlenProgramModel;
import jp.plen.scenography.services.PlenConnectionService;
import jp.plen.scenography.utils.PlenCommandUtil;
import jp.plen.scenography.views.PlenProgramView;
import jp.plen.scenography.views.adapters.PlenMotionListPagerAdapter;
import jp.plen.scenography.views.adapters.PlenProgramAdapter;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

@EFragment(R.layout.fragment_programming)
public class ProgrammingFragment extends Fragment implements IProgramFragment {
    private static final String TAG = ProgrammingFragment.class.getSimpleName();
    private final CompositeSubscription mSubscriptions = new CompositeSubscription();
    @ViewById(R.id.program) PlenProgramView mProgramView;
    @ViewById(R.id.motionListPager) ViewPager mMotionListViewPager;
    @ViewById(R.id.playIcon) ImageButton mPlayIcon;
    @ViewById(R.id.deleteIcon) ImageButton mDeleteIcon;
    @Bean PlenMotionListPagerAdapter mPlenMotionListPagerAdapter;
    @Bean PlenProgramAdapter mPlenProgramAdapter;
    @Bean ProgrammingFragmentPresenter mPresenter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate ");

        mSubscriptions.clear();
        mSubscriptions.add(mPlenMotionListPagerAdapter);
        mSubscriptions.add(mPlenProgramAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume ");
        mPresenter.bind(this);
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause ");
        mPresenter.unbind();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy ");
        mSubscriptions.unsubscribe();
        super.onDestroy();
    }

    @NonNull
    @Override
    public Subscription bind(@NonNull PlenProgramModel model) {
        Log.d(TAG, "bind ");

        CompositeSubscription binding = new CompositeSubscription();

        // motion list
        mPlenMotionListPagerAdapter.setDraggable(true);
        binding.add(mPlenMotionListPagerAdapter.bind(model.motionCategories().asObservable()));

        // program
        binding.add(mPlenProgramAdapter.bind(model.program().asObservable()));
        binding.add(Property.bindBidirectional(mPlenProgramAdapter.sequence(), model.sequence()));

        return binding;
    }

    @AfterViews
    void afterViews() {
        Log.d(TAG, "afterViews ");
        mMotionListViewPager.setAdapter(mPlenMotionListPagerAdapter);
        mProgramView.setAdapter(mPlenProgramAdapter);

        mPlayIcon.setOnClickListener(v ->
                Scenography.getModel().currentProgram().sequence().get()
                        .map(PlenCommandUtil::toCommand)
                        .ifPresent(program -> EventBus.getDefault()
                                .post(new PlenConnectionService.WriteRequest(program))));

        mDeleteIcon.setOnClickListener(v ->
                        Scenography.getModel().currentProgram().sequence().set(Collections.emptyList())
        );

        Bundle args = getArguments();
        boolean writable = args.getBoolean("WRITABLE");
        setImageButtonEnable(mPlayIcon, writable);
    }

    @UiThread
    void setImageButtonEnable(ImageButton button, boolean enable) {
        button.setEnabled(enable);
        Drawable image = button.getDrawable();
        if(image != null){
            image.setAlpha(enable ? 255 : 64);
        }
    }
}
