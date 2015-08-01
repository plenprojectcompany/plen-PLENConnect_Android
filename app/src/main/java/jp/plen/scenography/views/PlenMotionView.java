package jp.plen.scenography.views;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import jp.plen.bluetooth.le.IPlenBluetoothLeService;
import jp.plen.bluetooth.le.PlenBluetoothLeService;
import jp.plen.scenography.R;
import jp.plen.scenography.models.PlenMotion;
import jp.plen.scenography.utils.PlenMotionIconLoader;

public class PlenMotionView extends LinearLayout {
    private static final String TAG = PlenMotionView.class.getSimpleName();
    private IPlenBluetoothLeService mService;
    private PlenMotion mMotion;
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = IPlenBluetoothLeService.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    public PlenMotionView(Context context, AttributeSet attrs) {
        this(context, attrs, R.layout.view_plen_motion);
    }

    protected PlenMotionView(Context context, AttributeSet attrs, int layout) {
        super(context, attrs);

        LayoutInflater.from(context).inflate(layout, this);
    }

    public PlenMotion getMotion() {
        return mMotion;
    }

    public void setMotion(PlenMotion plenMotion) {
        mMotion = plenMotion;

        TextView nameView = (TextView) findViewById(R.id.motion_name_view);
        nameView.setText(plenMotion.getName());
        TextView numberView;
        numberView = (TextView) findViewById(R.id.motion_number_view);
        numberView.setText(String.format("%02X", plenMotion.getNumber()));
        final ImageButton iconView = (ImageButton) findViewById(R.id.motion_icon_view);

        iconView.setOnLongClickListener(v -> {
            if (mService == null || mMotion == null) return false;
            try {
                mService.write("$MP" + String.format("%02X", mMotion.getNumber()));
                return true;
            } catch (RemoteException e) {
                e.printStackTrace();
                return false;
            }
        });

        PlenMotionIconLoader.load(iconView, plenMotion.getIconName());
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        Intent intent = new Intent(getContext(), PlenBluetoothLeService.class);
        getContext().bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDetachedFromWindow() {
        getContext().unbindService(mServiceConnection);
        super.onDetachedFromWindow();
    }
}
