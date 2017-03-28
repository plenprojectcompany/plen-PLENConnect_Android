package jp.plen.scenography.views;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

import jp.plen.rx.binding.Property;
import jp.plen.rx.binding.ReadOnlyProperty;
import jp.plen.scenography.R;
import jp.plen.scenography.models.entities.PlenCodeUnit;
import jp.plen.scenography.models.entities.PlenMotion;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

@EViewGroup(R.layout.view_plen_code_unit)
public class PlenCodeUnitView extends LinearLayout {
    private static final String TAG = PlenCodeUnitView.class.getSimpleName();
    private final CompositeSubscription mSubscriptions = new CompositeSubscription();
    private final Property<Integer> mLoopCount = Property.create();
    private final Property<PlenCodeUnit> mPlenFunction = Property.create();
    @ViewById(R.id.plenMotion) PlenMotionView mPlenMotionView;
    @ViewById(R.id.loopCount) EditText mLoopCountView;

    public PlenCodeUnitView(Context context) {
        super(context);
    }

    public PlenCodeUnitView(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PlenCodeUnitView(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PlenCodeUnitView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @NonNull
    public ReadOnlyProperty<PlenMotion> motion() {
        return mPlenMotionView.motion();
    }

    @NonNull
    public ReadOnlyProperty<Integer> loopCount() {
        return mLoopCount;
    }

    @NonNull
    public ReadOnlyProperty<PlenCodeUnit> programUnit() {
        return mPlenFunction;
    }

    public void bindBlankRow() {
        setVisibility(INVISIBLE);
    }

    @NonNull
    public Subscription bind(@NonNull Observable<PlenMotion> motion, @NonNull Observable<Integer> loopCount) {
        setVisibility(VISIBLE);

        mLoopCountView.setText(String.valueOf(ReadOnlyProperty.create(loopCount).get()
                .orElse(getContext().getResources().getInteger(R.integer.motion_loop_default))));

        Subscription subscription = Subscriptions.from(
                mPlenMotionView.bind(motion),
                mLoopCount.bind(loopCount));
        mSubscriptions.add(subscription);

        return subscription;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        initSubscriptions();
    }

    @Override
    protected void onDetachedFromWindow() {
        mSubscriptions.unsubscribe();
        super.onDetachedFromWindow();
    }

    @AfterViews
    void afterViews() {
        mLoopCountView.setOnDragListener((v1, event) -> true);
        mLoopCountView.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(@NonNull Editable s) {
                String inputString = s.toString();
                if (inputString.isEmpty()) return;
                int inputNumber = Integer.parseInt(inputString);
                int min = getContext().getResources().getInteger(R.integer.motion_loop_min);
                int max = getContext().getResources().getInteger(R.integer.motion_loop_max);
                int loopCount = Math.min(Math.max(min, inputNumber), max);
                if (inputNumber != loopCount) {  // 無限ループ防止
                    s.clear();
                    s.insert(0, Integer.toString(loopCount));
                }
            }
        });

        mLoopCountView.setOnFocusChangeListener((v, hasFocus) -> {
            String input = mLoopCountView.getText().toString();
            if (input.isEmpty()) {
                input = String.valueOf(getContext().getResources().getInteger(R.integer.motion_loop_default));
                mLoopCountView.setText(input);
            }
            int loopCount = Integer.parseInt(input);
            if (mLoopCount.get().filter(i -> i != loopCount).isPresent()) {
                mLoopCount.set(loopCount);
            }
        });

        mLoopCountView.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                Log.d(TAG, "oKey:" + event);

                if(keyCode == KeyEvent.KEYCODE_ENTER){
                    String input = mLoopCountView.getText().toString();
                    if (input.isEmpty()) {
                        input = String.valueOf(getContext().getResources().getInteger(R.integer.motion_loop_default));
                        mLoopCountView.setText(input);
                    }
                    int loopCount = Integer.parseInt(input);
                    if (mLoopCount.get().filter(i -> i != loopCount).isPresent()) {
                        mLoopCount.set(loopCount);
                    }
                    // clearFocus();
                }
                return false;
            }
        });
    }

    private void initSubscriptions() {
        mSubscriptions.clear();

        mLoopCount.asObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(loopCount -> mLoopCountView.setText(String.valueOf(loopCount)));

        mPlenFunction.bind(Observable.combineLatest(
                mPlenMotionView.motion().asObservable(),
                mLoopCount.asObservable(),
                (motion, loopCount) -> new PlenCodeUnit(motion.getId(), loopCount)));
    }
}
