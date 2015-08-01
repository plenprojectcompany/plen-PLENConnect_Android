package jp.plen.scenography.views.adapter;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.util.List;

import jp.plen.scenography.R;
import jp.plen.scenography.models.PlenMotion;

public class ProgramListAdapter extends PlenMotionListAdapter {
    private static final String TAG = ProgramListAdapter.class.getSimpleName();
    private final LayoutInflater mLayoutInflater;

    public ProgramListAdapter(Context context, List<PlenMotion> objects) {
        super(context, objects);
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public static PlenMotion newBlankRow() {
        return new PlenMotion(-1, "", "");
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        PlenMotion plenMotion = getItem(position);

        if (null == convertView) {
            convertView = mLayoutInflater.inflate(R.layout.item_program_list, parent, false);
        }
        convertView = super.getView(position, convertView, parent);

        convertView.setAlpha(isBlankRow(plenMotion) ? 0 : 1);
        return convertView;
    }

    private static boolean isBlankRow(PlenMotion motion) {
        return motion.getNumber() < 0;
    }
}
