package jp.plen.scenography.views.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.List;

import jp.plen.scenography.R;
import jp.plen.scenography.models.PlenMotion;
import jp.plen.scenography.views.PlenMotionView;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * モーションデータ用アダプタ
 * Created by kzm4269 on 15/06/14.
 */
public class PlenMotionListAdapter extends BaseAdapter {
    private static final String TAG = PlenMotionListAdapter.class.getSimpleName();

    private final List<PlenMotion> mList;
    private final LayoutInflater mLayoutInflater;

    public PlenMotionListAdapter(Context context, List<PlenMotion> objects) {
        mList = objects;
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public PlenMotion getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        PlenMotion plenMotion = getItem(position);

        if (null == convertView) {
            convertView = mLayoutInflater.inflate(R.layout.item_plen_motion_list, parent, false);
        }

        ((PlenMotionView) convertView).setMotion(plenMotion);
        return convertView;
    }
}
