package jp.plen.plenconnect2.views;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

import de.greenrobot.event.EventBus;
import jp.plen.rx.binding.Property;
import jp.plen.rx.binding.ReadOnlyProperty;
import jp.plen.plenconnect2.R;
import jp.plen.plenconnect2.models.entities.PlenMotion;
import jp.plen.plenconnect2.services.PlenConnectionService;
import jp.plen.plenconnect2.utils.PlenCommandUtil;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

@EViewGroup(R.layout.view_plen_motion)
public class PlenMotionView extends RelativeLayout {
    private static final String TAG = PlenMotionView.class.getSimpleName();
    private final CompositeSubscription mSubscriptions = new CompositeSubscription();
    private final Property<PlenMotion> mMotion = Property.create();
    @ViewById(R.id.rowButton) Button mRowButton;
    @ViewById(R.id.motionIcon) ImageView mIconView;
    @ViewById(R.id.motionName) TextView mNameView;
    // @ViewById(R.id.motionNumber) TextView mNumberView;

    public PlenMotionView(Context context) {
        super(context);
    }

    public PlenMotionView(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PlenMotionView(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PlenMotionView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @NonNull
    public ReadOnlyProperty<PlenMotion> motion() {
        return mMotion;
    }

    @NonNull
    public Subscription bind(@NonNull Observable<PlenMotion> motion) {
        ReadOnlyProperty.create(motion).get().ifPresent(this::updateViews);
        Subscription subscription = mMotion.bind(motion);
        mSubscriptions.add(subscription);
        return subscription;
    }

    public void setRowButtonVisibility(int visibility) {
        mRowButton.setVisibility(visibility);
        if (mRowButton.getVisibility() == VISIBLE) {
            mIconView.setElevation(0);
        } else {
            mIconView.setElevation(getResources().getDimension(R.dimen.elevation_low));
        }
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
        if (isInEditMode()) return;
        Picasso.with(getContext())
                .load(getResources().getString(R.string.no_icon_path))
                .into(mIconView);
    }

    private void updateViews(@NonNull PlenMotion motion) {
        Picasso.with(getContext())
                .load(motion.getIconPath())
                .resizeDimen(R.dimen.motion_icon_width, R.dimen.motion_icon_height)
                .into(mIconView);
        mNameView.setText(motion.getName());
        // mNumberView.setText(String.format("%02X", motion.getId()));
        // mIconView.setOnLongClickListener(v -> sendMotionPlayCommand(motion));
        mRowButton.setOnClickListener(v -> sendMotionPlayCommand(motion));
    }

    private boolean sendMotionPlayCommand(@NonNull PlenMotion motion) {
        String command = PlenCommandUtil.toCommand(motion);
        EventBus.getDefault().post(new PlenConnectionService.WriteRequest(command));
        return false;
    }

    private void initSubscriptions() {
        mSubscriptions.clear();

        mSubscriptions.add(mMotion.asObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateViews));
    }
}
