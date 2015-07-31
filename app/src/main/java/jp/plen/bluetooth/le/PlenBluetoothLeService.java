package jp.plen.bluetooth.le;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * PLENとのBLE通信を行うService.
 * bindService()して使う.
 * Created by kzm4269 on 15/07/20.
 */
public class PlenBluetoothLeService extends Service {
    public static final int STATE_GATT_DISCONNECTED = 1;
    public static final int STATE_GATT_CONNECTING = 2;
    public static final int STATE_GATT_DISCOVERED = 3;
    private static final String TAG = PlenBluetoothLeService.class.getSimpleName();
    private static final String LONG_TAG = PlenBluetoothLeService.class.getName();
    public static final String ACTION_GATT_SETUP_STARTED = LONG_TAG + ".ACTION_GATT_SETUP_STARTED";
    public static final String ACTION_GATT_SETUP_SUCCESS = LONG_TAG + ".ACTION_GATT_SETUP_SUCCESS";
    public static final String ACTION_GATT_SETUP_FAILURE = LONG_TAG + ".ACTION_GATT_SETUP_FAILURE";
    public static final String ACTION_GATT_DISCONNECTED = LONG_TAG + ".ACTION_GATT_DISCONNECTED";
    public static final String ACTION_WRITE_SUCCESS = LONG_TAG + ".ACTION_WRITE_SUCCESS";
    public static final String ACTION_WRITE_FAILURE = LONG_TAG + ".ACTION_WRITE_FAILURE";
    public static final String EXTRA_DEVICE = LONG_TAG + ".EXTRA_DEVICE";
    public static final String EXTRA_WRITE_QUEUE_SIZE = LONG_TAG + ".EXTRA_WRITE_QUEUE_SIZE";
    private static final int MAX_PACKET_SIZE = 20;
    private final Queue<String> mWriteQueue = new LinkedBlockingDeque<>();
    private BluetoothGatt mGatt;
    private BluetoothGattCharacteristic mTxCharacteristic;
    private boolean isWriting = false;
    private int mState = STATE_GATT_DISCONNECTED;
    private final BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            mGatt = gatt;
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from the GATT server.");
                mState = STATE_GATT_DISCONNECTED;
                sendLocalBroadcast(getBroadcastIntent(ACTION_GATT_DISCONNECTED));
                return;
            }

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Connecting to the GATT server failed");
                sendLocalBroadcast(getBroadcastIntent(ACTION_GATT_SETUP_FAILURE));
                closeGatt();
                return;
            }
            Log.i(TAG, "Connected to the GATT server.");
            boolean discoveryStarted = gatt.discoverServices();
            if (!discoveryStarted) {
                Log.e(TAG, "The GATT service discovery has not been started.");
                sendLocalBroadcast(getBroadcastIntent(ACTION_GATT_SETUP_FAILURE));
                closeGatt();
                return;
            }
            Log.i(TAG, "The GATT service discovery has been started.");
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            mGatt = gatt;
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "The discovery of the GATT services failed.");
                sendLocalBroadcast(getBroadcastIntent(ACTION_GATT_SETUP_FAILURE));
                closeGatt();
                return;
            }

            for (BluetoothGattService service : mGatt.getServices())
                Log.i(TAG, "Support service: UUID=" + service.getUuid());

            BluetoothGattService plenControlService = mGatt.getService(PlenGattConstants.PLEN_CONTROL_SERVICE_UUID);
            if (plenControlService == null) {
                Log.e(TAG, "Could not found the PLEN Control Service.");
                sendLocalBroadcast(getBroadcastIntent(ACTION_GATT_SETUP_FAILURE));
                closeGatt();
                return;
            }
            mTxCharacteristic = plenControlService.getCharacteristic(PlenGattConstants.TX_DATA_UUID);
            if (mTxCharacteristic == null) {
                Log.e(TAG, "Could not found the TxData characteristic.");
                sendLocalBroadcast(getBroadcastIntent(ACTION_GATT_SETUP_FAILURE));
                closeGatt();
                return;
            }
            Log.i(TAG, "The GATT services has been explored successfully.");
            mState = STATE_GATT_DISCOVERED;
            sendLocalBroadcast(getBroadcastIntent(ACTION_GATT_SETUP_SUCCESS));
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            isWriting = false;
            if (status == BluetoothGatt.GATT_SUCCESS) {
                sendLocalBroadcast(getBroadcastIntent(ACTION_WRITE_SUCCESS)
                        .putExtra(EXTRA_WRITE_QUEUE_SIZE, mWriteQueue.size()));
                writeTxCharacteristic();
                Log.i(TAG, "Write TxData characteristic: success");
            } else {
                sendLocalBroadcast(getBroadcastIntent(ACTION_WRITE_FAILURE));
                Log.w(TAG, "onCharacteristicWrite received: " + status);
            }
        }
    };
    private final IPlenBluetoothLeService.Stub mBinder = new IPlenBluetoothLeService.Stub() {
        @Override
        public void write(String data) throws RemoteException {
            PlenBluetoothLeService.this.writeToBuffer(data);
        }

        @Override
        public void connectGatt(String deviceAddress) throws RemoteException {
            PlenBluetoothLeService.this.connectGatt(deviceAddress);
        }

        @Override
        public void disconnectGatt() throws RemoteException {
            PlenBluetoothLeService.this.closeGatt();
        }

        @Override
        public int getState() throws RemoteException {
            return mState;
        }
    };

    @Override
    public void onDestroy() {
        closeGatt();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return mBinder;
    }

    private void closeGatt() {
        if (mGatt != null) {
            mGatt.close();

            Log.i(TAG, "Close the GATT server.");
            mState = STATE_GATT_DISCONNECTED;
            sendLocalBroadcast(getBroadcastIntent(ACTION_GATT_DISCONNECTED));
        }

        mGatt = null;
        mWriteQueue.clear();
    }

    private void writeToBuffer(String data) {
        if (mState != STATE_GATT_DISCOVERED) return;

        // MAX_PACKET_SIZE バイトごとに分割してバッファに貯める
        for (int i = 0; i < data.length(); i += MAX_PACKET_SIZE)
            mWriteQueue.offer(data.substring(i, Math.min(i + MAX_PACKET_SIZE, data.length())));
        writeTxCharacteristic();
    }

    private void writeTxCharacteristic() {
        if (isWriting || mWriteQueue.isEmpty()) return;
        Log.d(TAG, "writeTxCharacteristic: " + mWriteQueue.peek());
        mTxCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        mTxCharacteristic.setValue(mWriteQueue.poll());
        isWriting = mGatt.writeCharacteristic(mTxCharacteristic);
        if (!isWriting) {
            Log.w(TAG, "write failure");
            if (mGatt.connect() && mGatt.discoverServices()) {
                Log.w(TAG, "try to reconnect.");
                connectGatt(mGatt.getDevice().getAddress());
            }
        }
    }

    private void connectGatt(String deviceAddress) {
        closeGatt();

        BluetoothManager bluetoothManager = (BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Can't get BluetoothAdapter.");
            return;
        }
        mGatt = bluetoothAdapter
                .getRemoteDevice(deviceAddress)
                .connectGatt(getApplicationContext(), false, mBluetoothGattCallback);

        Log.i(TAG, "Connecting to the GATT server started.");
        mState = STATE_GATT_CONNECTING;
        sendLocalBroadcast(getBroadcastIntent(ACTION_GATT_SETUP_STARTED));
    }

    private void sendLocalBroadcast(Intent intent) {
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    private Intent getBroadcastIntent(String action) {
        return new Intent(action).putExtra(EXTRA_DEVICE, mGatt != null ? mGatt.getDevice() : null);
    }
}
