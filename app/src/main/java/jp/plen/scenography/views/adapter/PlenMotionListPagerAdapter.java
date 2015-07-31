package jp.plen.scenography.views.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jp.plen.scenography.R;
import jp.plen.scenography.models.PlenMotion;
import jp.plen.scenography.views.MotionListView;

public class PlenMotionListPagerAdapter extends PagerAdapter {
    private final List<CharSequence> mTitles;
    private final Map<CharSequence, List<PlenMotion>> mMotionGroups;

    private Context mContext;

    public PlenMotionListPagerAdapter(Context context, List<CharSequence> titles, Map<CharSequence, List<PlenMotion>> items) {
        super();
        mTitles = titles;
        mMotionGroups = items;
        mContext = context;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams") View root = inflater.inflate(R.layout.page_motion_list, null);
        MotionListView motionListView = (MotionListView) root.findViewById(R.id.motion_list_view);
        List<PlenMotion> motions = mMotionGroups.get(mTitles.get(position));
        PlenMotionListAdapter adapter = new PlenMotionListAdapter(mContext, motions);
        motionListView.setAdapter(adapter);
        container.addView(root);
        return root;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public int getCount() {
        return mTitles.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTitles.get(position);
    }
}
