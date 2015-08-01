package jp.plen.scenography;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import jp.plen.bluetooth.le.PlenBluetoothLeService;

/**
 * Created by kzm4269 on 15/07/25.
 */
public class Scenography extends Application {
    private static final String TAG = Scenography.class.getSimpleName();
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        Intent intent = new Intent(getBaseContext(), PlenBluetoothLeService.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onTerminate() {
        unbindService(mServiceConnection);
        super.onTerminate();
    }
}
