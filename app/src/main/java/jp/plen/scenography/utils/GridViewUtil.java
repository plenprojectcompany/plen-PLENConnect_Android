package jp.plen.scenography.utils;

import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

/**
 * ListView関連.
 */
public final class GridViewUtil {
    private static final String TAG = GridViewUtil.class.getSimpleName();
    private GridViewUtil() {}

    /**
     * 指定位置の子Viewの番号を取得する.
     *
     * @param gridView 対象のGridView
     * @param x x座標
     * @param y y座標
     * @return 子Viewの番号 (存在しない場合は {@link AdapterView#INVALID_POSITION} を返す.)
     */
    public static int getPositionAt(@NonNull GridView gridView, int x, int y) {
        int firstPosition = gridView.getFirstVisiblePosition();
        int lastPosition = gridView.getLastVisiblePosition();
        for (int p = firstPosition; p <= lastPosition; p++) {
            View child = gridView.getChildAt(p - firstPosition);
            if (child == null) continue;
            Rect hitRect = new Rect();
            child.getHitRect(hitRect);
            if (hitRect.contains(x, y)) return p;
        }
        return AdapterView.INVALID_POSITION;
    }
}
