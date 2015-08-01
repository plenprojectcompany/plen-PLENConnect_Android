package jp.plen.scenography.views;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;

import jp.plen.scenography.R;
import jp.plen.scenography.models.PlenMotion;

public class ProgramUnitView extends PlenMotionView {
    private static final String TAG = ProgramUnitView.class.getSimpleName();

    public ProgramUnitView(Context context, AttributeSet attrs) {
        super(context, attrs, R.layout.view_program_unit);

        EditText loopCountEdit = (EditText) findViewById(R.id.loop_count_edit);

        loopCountEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                PlenMotion motion = getMotion();
                if (motion == null) return;

                EditText loopCountEdit = (EditText) findViewById(R.id.loop_count_edit);

                if (s.toString().isEmpty()) {
                    motion.setLoopCount(Integer.parseInt(loopCountEdit.getHint().toString()));
                    return;
                }

                int input;
                try {
                    input = Integer.parseInt(s.toString());
                } catch (NumberFormatException e) {
                    loopCountEdit.setText("");
                    return;
                }

                int loopCount = Math.min(Math.max(0x01, input), 0xFF);
                if (loopCount != input) {
                    loopCountEdit.setText(Integer.toString(loopCount));
                    loopCountEdit.setSelection(Integer.toString(loopCount).length());
                }
                motion.setLoopCount(loopCount);
            }
        });
    }

    @Override
    public void setMotion(PlenMotion plenMotion) {
        super.setMotion(plenMotion);

        EditText loopCountEdit = (EditText) findViewById(R.id.loop_count_edit);
        loopCountEdit.setText(Integer.toString(plenMotion.getLoopCount()));
    }
}
