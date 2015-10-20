package jp.plen.scenography.views.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jp.plen.rx.binding.Property;
import jp.plen.rx.subscriptions.SubscriptionMap;
import jp.plen.scenography.R;
import jp.plen.scenography.models.entities.PlenCodeUnit;
import jp.plen.scenography.models.entities.PlenMotion;
import jp.plen.scenography.models.entities.PlenProgram;
import jp.plen.scenography.views.PlenCodeUnitView;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

/**
 * PlenProgramView用のListAdapter
 */
@EBean
public class PlenProgramAdapter extends BaseAdapter implements Subscription {
    private static final String TAG = PlenProgramAdapter.class.getSimpleName();
    @Nullable private static final BlankRow BLANK_ROW = new BlankRow();
    private final List<PlenCodeUnit> mItems = new ArrayList<>();
    private final CompositeSubscription mSubscriptions = new CompositeSubscription();
    private final SubscriptionMap<Object> mSubscriptionMap = new SubscriptionMap<>();
    private final Property<Map<Integer, PlenMotion>> mMotionMap = Property.create();
    private final Property<List<PlenCodeUnit>> mSequence = Property.create();
    @NonNull private final LayoutInflater mLayoutInflater;
    @RootContext Context mContext;

    public PlenProgramAdapter(@NonNull Context context) {
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @NonNull
    public Property<List<PlenCodeUnit>> sequence() {
        return mSequence;
    }

    @NonNull
    public Subscription bind(@NonNull Observable<PlenProgram> program) {
        return Subscriptions.from(
                mMotionMap.bind(program
                        .map(PlenProgram::getMotionMap)
                        .filter(map -> !mMotionMap.get().map(map::equals).orElse(false))),
                mSequence.bind(program
                        .map(PlenProgram::getSequence)
                        .filter(seq -> !seq.equals(stageWithoutBlankRow()))));
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public PlenCodeUnit getItem(int position) {
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
            convertView = mLayoutInflater.inflate(R.layout.item_plen_program, parent, false);
        }
        bindView(position, (PlenCodeUnitView) convertView);
        return convertView;
    }

    public int positionOfBlankRow() {
        int position = mItems.indexOf(BLANK_ROW);
        return position != ListView.INVALID_POSITION ? position : ListView.INVALID_POSITION;
    }

    public void moveBlankRowToLast() {
        mItems.remove(BLANK_ROW);
        mItems.add(BLANK_ROW);
        notifyDataSetChanged();
    }

    public void moveBlankRow(int position) {
        if (positionOfBlankRow() == position) return;
        mItems.remove(BLANK_ROW);
        mItems.add(position, BLANK_ROW);
        notifyDataSetChanged();
    }

    public void removeBlankRow() {
        if (!mItems.contains(BLANK_ROW)) return;
        mItems.remove(BLANK_ROW);
        notifyDataSetChanged();
    }

    public void replaceByBlankRow(int position) {
        if (isBlankRow(mItems.get(position))) return;
        mItems.remove(BLANK_ROW);
        mItems.set(position, BLANK_ROW);
        notifyDataSetChanged();
        commit();
    }

    public void dropToBlankRow(@NonNull PlenCodeUnit unit) {
        if (!mItems.contains(BLANK_ROW)) return;
        mItems.add(positionOfBlankRow(), unit);
        mItems.remove(BLANK_ROW);
        notifyDataSetChanged();
        commit();
    }

    public void setUnit(int location, PlenCodeUnit unit) {
        if (Objects.equals(unit, mItems.get(location))) return;
        mItems.set(location, unit);
        notifyDataSetChanged();
        commit();
    }

    @Override
    public void unsubscribe() {
        mSubscriptions.unsubscribe();
    }

    @Override
    public boolean isUnsubscribed() {
        return mSubscriptions.isUnsubscribed();
    }

    private List<PlenCodeUnit> stageWithoutBlankRow() {
        return Observable.from(mItems).filter(u -> !isBlankRow(u)).toList().toBlocking().single();
    }

    private void commit() {
        mSequence.set(stageWithoutBlankRow());
    }

    private void bindView(int position, @NonNull PlenCodeUnitView view) {
        mSubscriptionMap.remove(view);
        mSubscriptionMap.remove(position);
        PlenCodeUnit unit = getItem(position);
        if (isBlankRow(unit)) {
            view.bindBlankRow();
            return;
        }

        Subscription subscription = Subscriptions.from(
                view.bind(mMotionMap.asObservable().map(m -> m.get(unit.getMotionId())),
                        Observable.just(unit.getLoopCount())),
                view.programUnit().asObservable()
                        .skipWhile(u -> !Objects.equals(u, unit))
                        .subscribe(u -> setUnit(position, u)));
        mSubscriptionMap.put(view, subscription);
        mSubscriptionMap.put(position, subscription);
    }

    private boolean isBlankRow(@Nullable PlenCodeUnit unit) {
        return unit instanceof BlankRow;
    }

    @AfterInject
    void afterInject() {
        initSubscriptions();
    }

    private void initSubscriptions() {
        mSubscriptions.clear();
        mSubscriptionMap.clear();
        mSubscriptions.add(mSubscriptionMap);

        Observable
                .combineLatest(
                        mMotionMap.asObservable(),
                        mSequence.asObservable(),
                        (map, seq) -> seq)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(seq -> {
                    if (!seq.equals(stageWithoutBlankRow())) {
                        mItems.clear();
                        mItems.addAll(seq);
                    }
                    notifyDataSetChanged();
                })
                .subscribe();
    }

    private static class BlankRow extends PlenCodeUnit {
        private static final long serialVersionUID = -7485794770680714919L;

        public BlankRow() {
            super(-1, -1);
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }
    }
}
