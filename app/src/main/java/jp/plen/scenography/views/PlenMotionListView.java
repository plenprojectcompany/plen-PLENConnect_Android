package jp.plen.scenography.views;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.eccyan.optional.Optional;

import org.androidannotations.annotations.EView;
import org.androidannotations.annotations.UiThread;

import java.util.concurrent.TimeUnit;

import jp.plen.scenography.R;
import jp.plen.scenography.utils.ListViewUtil;
import jp.plen.scenography.views.adapters.PlenMotionListAdapter;
import rx.Observable;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

@EView
public class PlenMotionListView extends ListView {
    private static final String TAG = PlenMotionListView.class.getSimpleName();
    private final Rect mLastHitRect = new Rect();  // 最後にタッチされた子Viewの位置
    @NonNull private Optional<PlenMotionListAdapter> mAdapter = Optional.empty();
    private Subscription mItemLongClickSubscription = Subscriptions.empty();

    public PlenMotionListView(@NonNull Context context) {
        this(context, null);
    }

    public PlenMotionListView(@NonNull Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PlenMotionListView(@NonNull Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @NonNull
    @Override
    public PlenMotionListAdapter getAdapter() {
        return (PlenMotionListAdapter) super.getAdapter();
    }

    @Override
    public void setAdapter(@NonNull ListAdapter adapter) {
        mAdapter = Optional.of((PlenMotionListAdapter) adapter);
        super.setAdapter(adapter);
    }

    @Override
    protected void onDetachedFromWindow() {
        mItemLongClickSubscription.unsubscribe();
        super.onDetachedFromWindow();
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();

        // 一定時間後にドラッグ開始
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            int position = ListViewUtil.getPositionAt(this, x, y);
            if (position != INVALID_POSITION) {
                mItemLongClickSubscription.unsubscribe();
                mItemLongClickSubscription = Observable.just(position)
                        .delay(getContext().getResources().getInteger(R.integer.motion_list_long_press_msec), TimeUnit.MILLISECONDS)
                        .doOnNext(this::startDragChild)
                        .subscribe();
                getChildAt(position - getFirstVisiblePosition())
                        .getHitRect(mLastHitRect);
            }
        } else if (event.getAction() != MotionEvent.ACTION_MOVE || !mLastHitRect.contains(x, y)) {
            mItemLongClickSubscription.unsubscribe();
        }
        return super.onTouchEvent(event);
    }

    /**
     * 指定位置の子Viewのドラッグを開始する.
     *
     * @param position 子Viewの位置
     */
    @UiThread
    void startDragChild(int position) {
        PlenMotionListAdapter adapter = mAdapter.orElseThrow(AssertionError::new);

        View pressedView = getChildAt(position - getFirstVisiblePosition());
        pressedView.setPressed(true);
        pressedView.startDrag(
                new PlenProgramView.DragDataBuilder(getContext())
                        .motion(adapter.getItem(position))
                        .build(),
                new DragShadowBuilder(pressedView), null, 0);
        pressedView.setPressed(false);
    }
}
