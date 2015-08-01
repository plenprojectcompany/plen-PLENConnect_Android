package jp.plen.scenography.views;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.io.Serializable;

import jp.plen.scenography.R;
import jp.plen.scenography.views.adapter.PlenMotionListAdapter;

/**
 * モーションリスト
 * Created by kzm4269 on 15/06/14.
 */
public class MotionListView extends ListView {
    private static final String TAG = MotionListView.class.getSimpleName();
    private Runnable mLongClickCallback;
    private Rect mLastHitRect = new Rect();

    public MotionListView(Context context) {
        this(context, null);
    }

    public MotionListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MotionListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        final int x = (int) event.getX();
        final int y = (int) event.getY();
        final int position = getPositionAt(x, y);
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (position != INVALID_POSITION) {
                if (mLongClickCallback != null)
                    removeCallbacks(mLongClickCallback);
                final View target = getChildAt(position - getFirstVisiblePosition());
                mLongClickCallback = () -> {
                    View child = getChildAt(position - getFirstVisiblePosition());
                    Intent intent = new Intent().putExtra("motion_list", getAdapter().getItem(position));
                    ClipData data = ClipData.newIntent("intent", intent);
                    if (child == target) {
                        child.setPressed(true);
                        child.startDrag(data, new DragShadowBuilder(child), null, 0);
                    }
                };
                postDelayed(mLongClickCallback, getContext().getResources().getInteger(R.integer.motion_list_long_press_msec));
                getChildAt(position - getFirstVisiblePosition()).getHitRect(mLastHitRect);
            }
        } else if (event.getAction() != MotionEvent.ACTION_MOVE || !mLastHitRect.contains(x, y)) {
            if (mLongClickCallback != null)
                removeCallbacks(mLongClickCallback);
            mLongClickCallback = null;
        }
        return super.onTouchEvent(event);
    }

    private int getPositionAt(int x, int y) {
        int firstPosition = getFirstVisiblePosition();
        int lastPosition = getLastVisiblePosition();
        for (int position = firstPosition; position <= lastPosition; position++) {
            View child = getChildAt(position - firstPosition);
            if (child == null) continue;
            Rect rect = new Rect();
            child.getHitRect(rect);
            if (rect.contains(x, y))
                return position;
        }
        return INVALID_POSITION;
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        if (!(adapter instanceof PlenMotionListAdapter))
            throw new UnsupportedOperationException();
        super.setAdapter(adapter);
    }

    @Override
    public PlenMotionListAdapter getAdapter() {
        return (PlenMotionListAdapter) super.getAdapter();
    }
}
