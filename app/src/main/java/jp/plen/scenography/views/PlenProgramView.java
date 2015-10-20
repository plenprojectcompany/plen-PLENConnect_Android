package jp.plen.scenography.views;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.eccyan.optional.Optional;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EView;
import org.androidannotations.annotations.UiThread;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import jp.plen.scenography.R;
import jp.plen.scenography.models.entities.PlenCodeUnit;
import jp.plen.scenography.models.entities.PlenMotion;
import jp.plen.scenography.utils.ListViewUtil;
import jp.plen.scenography.utils.SerializableUtil;
import jp.plen.scenography.views.adapters.PlenProgramAdapter;
import rx.Observable;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

@EView
public class PlenProgramView extends ListView {
    private static final String TAG = PlenProgramView.class.getSimpleName();
    private static final String DRAG_DATA_LABEL = TAG + ".DRAG_DATA_LABEL";
    private final Runnable mRemoveBlankRowCallback = () -> getAdapter().removeBlankRow();
    private final Rect mLastHitRect = new Rect();
    @NonNull private Optional<PlenProgramAdapter> mAdapter = Optional.empty();
    private Subscription mItemLongClickSubscription = Subscriptions.empty();

    public PlenProgramView(@NonNull Context context) {
        this(context, null);
    }

    public PlenProgramView(@NonNull Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PlenProgramView(@NonNull Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @NonNull
    private static Optional<PlenCodeUnit> getDragData(@NonNull ClipData data) {
        try {
            return Optional.of((PlenCodeUnit) SerializableUtil
                    .fromString(data.getItemAt(0).getText().toString()));
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            return Optional.empty();
        }
    }

    @NonNull
    @Override
    public PlenProgramAdapter getAdapter() {
        return (PlenProgramAdapter) super.getAdapter();
    }

    @Override
    public void setAdapter(@NonNull ListAdapter adapter) {
        mAdapter = Optional.of((PlenProgramAdapter) adapter);
        super.setAdapter(adapter);
    }

    @Override
    public boolean onDragEvent(@NonNull DragEvent event) {
        PlenProgramAdapter adapter = mAdapter.orElseThrow(AssertionError::new);

        ClipDescription clipDescription = event.getClipDescription();
        if (clipDescription == null || !DRAG_DATA_LABEL.equals(clipDescription.getLabel())) {
            return false;
        }

        int x = (int) event.getX();
        int y = (int) event.getY();
        int position = pointToPosition(x, y);

        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_ENTERED:
                if (adapter.positionOfBlankRow() == INVALID_POSITION) {
                    adapter.moveBlankRowToLast();
                }
                break;

            case DragEvent.ACTION_DRAG_LOCATION:
                removeCallbacks(mRemoveBlankRowCallback);
                // ドロップ予定地に空白行
                if (position != INVALID_POSITION) {
                    View view = getChildAt(position - getFirstVisiblePosition());
                    double viewCenter = view.getY() + view.getHeight() / 2.;
                    if (Math.abs(y - viewCenter) < view.getHeight() / 2.) {
                        adapter.moveBlankRow(position);
                    }
                }
                // 端にドラッグすると自動スクロール
                View child = getChildAt(0);
                double scrollZone = child != null ? child.getHeight() : 0;
                final int firstPosition = getFirstVisiblePosition();
                final int lastPosition = getLastVisiblePosition();
                if (y < scrollZone) smoothScrollToPosition(firstPosition - 1);
                if (y > getHeight() - scrollZone) smoothScrollToPosition(lastPosition + 1);
                break;

            case DragEvent.ACTION_DROP:
                getDragData(event.getClipData()).ifPresent(adapter::dropToBlankRow);
                break;

            case DragEvent.ACTION_DRAG_ENDED:
            case DragEvent.ACTION_DRAG_EXITED:
                adapter.removeBlankRow();
                break;
        }
        return true;
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
        PlenProgramAdapter adapter = mAdapter.orElseThrow(AssertionError::new);

        View pressedView = getChildAt(position - getFirstVisiblePosition());
        PlenCodeUnit unit = adapter.getItem(position);
        adapter.replaceByBlankRow(position);

        pressedView.setPressed(true);
        pressedView.startDrag(
                new DragDataBuilder(getContext())
                        .unit(unit)
                        .build(),
                new DragShadowBuilder(pressedView), null, 0);
    }

    @AfterInject
    void afterInject() {
        setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                mItemLongClickSubscription.unsubscribe();
            }
        });
    }

    public static class DragDataBuilder {
        private Context mContext;
        private PlenCodeUnit mUnit;

        public DragDataBuilder(Context context) {
            mContext = context;
        }

        @NonNull
        public ClipData build() {
            try {
                return ClipData.newPlainText(DRAG_DATA_LABEL, SerializableUtil.toString(mUnit));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @NonNull
        public DragDataBuilder unit(PlenCodeUnit unit) {
            mUnit = unit;
            return this;
        }

        @NonNull
        public DragDataBuilder motion(@NonNull PlenMotion motion) {
            mUnit = new PlenCodeUnit(motion.getId(), mContext.getResources().getInteger(R.integer.motion_loop_default));
            return this;
        }
    }
}
