package jp.plen.scenography.views.adapters;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jp.plen.rx.binding.Property;
import jp.plen.rx.subscriptions.SubscriptionMap;
import jp.plen.scenography.R;
import jp.plen.scenography.models.entities.PlenMotion;
import jp.plen.scenography.models.preferences.MainPreferences_;
import jp.plen.scenography.views.PlenJoystickMotionView;
import jp.plen.scenography.views.PlenMotionView;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

@EBean
public class PlenMotionListAdapter extends BaseAdapter implements Subscription {
    private static final String TAG = PlenMotionListAdapter.class.getSimpleName();
    private final List<PlenMotion> mItems = new ArrayList<>();
    private final CompositeSubscription mSubscriptions = new CompositeSubscription();
    private final SubscriptionMap<Object> mSubscriptionMap = new SubscriptionMap<>();
    private final Property<Set<PlenMotion>> mMotions = Property.create();
    @NonNull private final LayoutInflater mLayoutInflater;
    @Pref MainPreferences_ mPref;
    @RootContext Context mContext;
    private boolean mDraggable;

    public PlenMotionListAdapter(@NonNull Context context) {
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @NonNull
    public Subscription bind(@NonNull Observable<Set<PlenMotion>> motions) {
        return mMotions.bind(motions);
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public PlenMotion getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Nullable
    @Override
    public View getView(int position, @Nullable View convertView, ViewGroup parent) {
        if (convertView == null) {
            if(mPref.joystickVisibility().get()) {
                convertView = mLayoutInflater.inflate(R.layout.item_plen_motion_joystick_list, parent, false);

                WindowManager wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
                ViewGroup.LayoutParams prm;

                Display disp = wm.getDefaultDisplay();
                prm = convertView.getLayoutParams();

                Point size = new Point();
                disp.getSize(size);

                final TypedArray styledAttributes = mContext.getTheme().obtainStyledAttributes(new int[]{android.R.attr.actionBarSize});
                int actionBarSize = (int) styledAttributes.getDimension(0, 0);

                prm.height = (size.y - actionBarSize*2)/3;

                Log.d(TAG, "getView: size.y: " + size.y + " actionBarSizeDp: " + actionBarSize);

                convertView.setLayoutParams(prm);
            } else {
                convertView = mLayoutInflater.inflate(R.layout.item_plen_motion_list, parent, false);
            }
        }


        if(mPref.joystickVisibility().get()) {
            bindView(position, (PlenJoystickMotionView) convertView);
        } else {
            bindView(position, (PlenMotionView) convertView);
        }
        return convertView;
    }

    public void setDraggable(boolean draggable) {
        if (mDraggable != draggable) {
            mDraggable = draggable;
            notifyDataSetChanged();
        }
    }

    @Override
    public void unsubscribe() {
        mSubscriptions.unsubscribe();
    }

    @Override
    public boolean isUnsubscribed() {
        return mSubscriptions.isUnsubscribed();
    }

    @AfterInject
    void afterInject() {
        initSubscriptions();
    }

    private void bindView(int position, @NonNull PlenMotionView view) {
        Subscription subscription = view.bind(Observable.just(getItem(position)));
        view.setRowButtonVisibility(mDraggable ? View.INVISIBLE : View.VISIBLE);
        mSubscriptionMap.put(view, subscription);
        mSubscriptionMap.put(position, subscription);
    }

    private void bindView(int position, @NonNull PlenJoystickMotionView view) {
        Subscription subscription = view.bind(Observable.just(getItem(position)));
        view.setRowButtonVisibility(mDraggable ? View.INVISIBLE : View.VISIBLE);
        mSubscriptionMap.put(view, subscription);
        mSubscriptionMap.put(position, subscription);
    }

    private void initSubscriptions() {
        mSubscriptions.clear();
        mSubscriptionMap.clear();
        mSubscriptions.add(mSubscriptionMap);

        mMotions.asObservable()
                .map(Observable::from)
                .flatMap(Observable::toSortedList)
                .filter(motions -> !mItems.equals(motions))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(motions -> {
                    mItems.clear();
                    mItems.addAll(motions);
                    notifyDataSetChanged();
                });
    }
}
