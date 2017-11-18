package jp.plen.plenconnect2.views;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import com.eccyan.optional.Optional;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

import jp.plen.rx.binding.Property;
import jp.plen.rx.binding.ReadOnlyProperty;
import jp.plen.plenconnect2.R;

@EViewGroup(R.layout.view_joystick)
public class JoystickView extends RelativeLayout {
    private static final String TAG = JoystickView.class.getSimpleName();
    private final ObjectAnimator mAnimatorX = new ObjectAnimator();
    private final ObjectAnimator mAnimatorY = new ObjectAnimator();
    private final Property<StickPosition> mStickPosition = Property.create();
    @ViewById(R.id.joystickBackground) View mBackground;
    @ViewById(R.id.joystickStick) View mStick;
    private boolean mInitialized = false;
    private int mCenterX;
    private int mCenterY;
    private int mStickR;
    private int mBackgroundR;

    public JoystickView(Context context) {
        this(context, null);
    }

    public JoystickView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public JoystickView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public Optional<StickPosition> getStickPosition() {
        return mStickPosition.get();
    }

    public ReadOnlyProperty<StickPosition> stickPosition() {
        return mStickPosition;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            double x = event.getX() - mCenterX;
            double y = event.getY() - mCenterY;
            double t = Math.atan2(y, x);
            double r = Math.hypot(x, y);
            double rMax = mBackgroundR - mStickR;
            r = Math.min(r, rMax);
            x = mCenterX - mStickR + r * Math.cos(t);
            y = mCenterY - mStickR + r * Math.sin(t);
            if (mStick.getX() != (int) x || mStick.getY() != (int) y) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    animateStick(x, y);
                } else {
                    mStick.setX((int) x);
                    mStick.setY((int) y);
                }
                mStickPosition.set(new StickPosition(r / rMax, t));
            }
        } else {
            animateStick(mCenterX - mStickR, mCenterY - mStickR);
            mStickPosition.set(new StickPosition(0, 0));
        }
        return true;
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        if (!mInitialized) {
            mInitialized = true;
            mCenterX = canvas.getWidth() / 2;
            mCenterY = canvas.getHeight() / 2;
            mBackgroundR = (int) Math.min(
                    Math.min(mCenterX, mCenterY) * 0.8,
                    getResources().getDimension(R.dimen.joystick_background_size) / 2);
            mBackground.setLayoutParams(new LayoutParams(2 * mBackgroundR, 2 * mBackgroundR));
            mBackground.setX(mCenterX - mBackgroundR);
            mBackground.setY(mCenterY - mBackgroundR);
            mStickR = (int) (mBackgroundR * 0.6);
            mStick.setLayoutParams(new LayoutParams(2 * mStickR, 2 * mStickR));
            mStick.setX(mCenterX - mStickR);
            mStick.setY(mCenterY - mStickR);
        }
        super.dispatchDraw(canvas);
    }

    @AfterViews
    void afterViews() {
        mBackground.setLayoutParams(new LayoutParams(0, 0));
        mStick.setLayoutParams(new LayoutParams(0, 0));

        mBackground.getBackground().setLevel(1);

        mAnimatorX.setPropertyName("x");
        mAnimatorY.setPropertyName("y");
        mAnimatorX.setTarget(mStick);
        mAnimatorY.setTarget(mStick);
    }

    private void animateStick(double x, double y) {
        double r = Math.hypot(
                mStick.getX() - x,
                mStick.getY() - y);
        int duration = (int) r / 2;

        mAnimatorX.setDuration(duration);
        mAnimatorX.setFloatValues((float) x);
        mAnimatorY.setDuration(duration);
        mAnimatorY.setFloatValues((float) y);
        mAnimatorX.start();
        mAnimatorY.start();
    }

    public static class StickPosition {
        public final double gain;
        public final double direction;

        public StickPosition(double gain, double direction) {
            this.gain = gain;
            this.direction = direction;
        }
    }
}
