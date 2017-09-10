package jp.plen.scenography.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.util.Log;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;

import java.util.concurrent.TimeUnit;

import jp.plen.scenography.R;
import jp.plen.scenography.models.PlenProgramModel;
import jp.plen.scenography.models.entities.PlenMotionCategory;
import jp.plen.scenography.models.entities.PlenWalk;
import jp.plen.scenography.views.JoystickView;
import jp.plen.scenography.views.adapters.PlenMotionListPagerAdapter;
import rx.Observable;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

@EFragment(R.layout.fragment_joystick)
public class JoystickFragment extends Fragment implements IJoystickFragment {
    private static final String TAG = JoystickFragment.class.getSimpleName();
    private final CompositeSubscription mSubscriptions = new CompositeSubscription();
    @ViewById(R.id.joystick) JoystickView mJoystickView;
    @ViewById(R.id.motionListPager) ViewPager mMotionListViewPager;
    @Bean PlenMotionListPagerAdapter mPlenMotionListPagerAdapter;
    @Bean JoystickFragmentPresenter mPresenter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate ");

        mSubscriptions.clear();
    }

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

    @AfterViews
    void afterViews() {
        Log.d(TAG, "afterViews ");
        mMotionListViewPager.setAdapter(mPlenMotionListPagerAdapter);
    }

    @NonNull
    private PlenMotionCategory categoryFilter(@NonNull PlenMotionCategory category) {
        return new PlenMotionCategory(
                category.getMode(),
                category.getName(),
                Observable.from(category.getMotions())
                        .filter(motion -> !(70 <= motion.getId() && motion.getId() <= 81))
                        .toList().toBlocking().single());
    }


    @NonNull
    @Override
    public Subscription bind(@NonNull PlenProgramModel model) {
        Log.d(TAG, "bind ");

        CompositeSubscription binding = new CompositeSubscription();

        // motion list
        mPlenMotionListPagerAdapter.setDraggable(false);
        binding.add(mPlenMotionListPagerAdapter.bind(
                model.motionCategories().asObservable().flatMap(categories ->
                        Observable.from(categories).map(this::categoryFilter).toList())));

        // joystick
        double gain_min = 0.5;
        int repeat_interval = getResources().getInteger(R.integer.walk_command_repeat_interval_msec);
        binding.add(Observable
                .interval(repeat_interval, TimeUnit.MILLISECONDS)
                .map(i -> mJoystickView.getStickPosition()
                        .orElse(new JoystickView.StickPosition(0, 0)))
                .filter(p -> p.gain > gain_min)
                .map(p -> p.direction)
                .doOnNext(dir -> {
                    //String modeName = mPlenMotionListPagerAdapter.getPageTitle(mMotionListViewPager.getCurrentItem());
                    String modeName = mPlenMotionListPagerAdapter.getMode(mMotionListViewPager.getCurrentItem());
                    PlenWalk.Mode mode = PlenWalk.Mode.NORMAL;
                    switch (modeName) {
                        case "BOX":
                            mode = PlenWalk.Mode.BOX;
                            break;
                        case "ROLLER SKATING":
                            mode = PlenWalk.Mode.ROLLER_SKATING;
                            break;
                    }
                    mPresenter.movePlen(mode, dir);
                })
                .subscribe());

        Observable<Boolean> stickIsActive =
                mJoystickView.stickPosition().asObservable().map(s -> s.gain > gain_min);

        binding.add(Observable.zip(stickIsActive, stickIsActive.skip(1), (a, b) -> a && !b)
                .filter(b -> b)
                .doOnNext(b -> mPresenter.stopPlen())
                .subscribe());

        return binding;
    }
}
