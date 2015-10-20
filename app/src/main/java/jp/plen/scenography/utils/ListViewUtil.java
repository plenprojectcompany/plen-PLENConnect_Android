package jp.plen.scenography.utils;

import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

/**
 * ListView関連.
 */
public final class ListViewUtil {
    private static final String TAG = ListViewUtil.class.getSimpleName();
    private ListViewUtil() {}

    /**
     * 指定位置の子Viewの番号を取得する.
     *
     * @param listView 対象のListView
     * @param x x座標
     * @param y y座標
     * @return 子Viewの番号 (存在しない場合は {@link AdapterView#INVALID_POSITION} を返す.)
     */
    public static int getPositionAt(@NonNull ListView listView, int x, int y) {
        int firstPosition = listView.getFirstVisiblePosition();
        int lastPosition = listView.getLastVisiblePosition();
        for (int p = firstPosition; p <= lastPosition; p++) {
            View child = listView.getChildAt(p - firstPosition);
            if (child == null) continue;
            Rect hitRect = new Rect();
            child.getHitRect(hitRect);
            if (hitRect.contains(x, y)) return p;
        }
        return AdapterView.INVALID_POSITION;
    }
}
