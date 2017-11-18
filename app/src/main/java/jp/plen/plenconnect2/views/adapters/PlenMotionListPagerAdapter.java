package jp.plen.plenconnect2.views.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.util.ArrayList;
import java.util.List;

import jp.plen.rx.subscriptions.SubscriptionMap;
import jp.plen.plenconnect2.R;
import jp.plen.plenconnect2.models.entities.PlenMotionCategory;
import jp.plen.plenconnect2.models.preferences.MainPreferences_;
import jp.plen.plenconnect2.views.PlenMotionListView;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

@EBean
public class PlenMotionListPagerAdapter extends PagerAdapter implements Subscription {
    private static final String TAG = PlenMotionListPagerAdapter.class.getSimpleName();
    private final List<PlenMotionCategory> mItems = new ArrayList<>();
    private final CompositeSubscription mSubscriptions = new CompositeSubscription();
    private final SubscriptionMap<Object> mSubscriptionMap = new SubscriptionMap<>();
    @NonNull private final LayoutInflater mLayoutInflater;
    @Pref MainPreferences_ mPref;
    @RootContext Context mContext;
    private boolean mDraggable;

    public PlenMotionListPagerAdapter(@NonNull Context context) {
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public Subscription bind(@NonNull Observable<List<PlenMotionCategory>> categories) {
        return categories
                .filter(c -> !mItems.equals(c))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(c -> {
                    mItems.clear();
                    mItems.addAll(c);
                    notifyDataSetChanged();
                });
    }

    public void setDraggable(boolean draggable) {
        if (mDraggable != draggable) {
            mDraggable = draggable;
            notifyDataSetChanged();
        }
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        PlenMotionListView view = (PlenMotionListView) mLayoutInflater
                .inflate(R.layout.page_plen_motion_list_pager, null);

        PlenMotionListAdapter adapter = PlenMotionListAdapter_.getInstance_(mContext);
        adapter.setDraggable(mDraggable);
        view.setAdapter(adapter);

        mSubscriptionMap.put(position, Subscriptions.from(
                adapter,
                adapter.bind(Observable.just(mItems.get(position).getMotions()))));


        if(mPref.joystickVisibility().get()) {
            view.setNumColumns(mContext.getResources().getInteger(R.integer.number_column_joystick));
        } else {
            view.setNumColumns(mContext.getResources().getInteger(R.integer.number_column_programming));
        }
        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public String getPageTitle(int position) {
        return mItems.get(position).getName();
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

    private void initSubscriptions() {
        mSubscriptions.clear();
        mSubscriptionMap.clear();
        mSubscriptions.add(mSubscriptionMap);
    }

    @NonNull
    public String getMode(int position){
        return mItems.get(position).getMode();
    }
}
