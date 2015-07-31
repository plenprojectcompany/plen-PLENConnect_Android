package jp.plen.scenography.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import jp.plen.bluetooth.le.IPlenBluetoothLeService;
import jp.plen.bluetooth.le.PlenBluetoothLeService;
import jp.plen.bluetooth.le.PlenScanHelper;
import jp.plen.scenography.R;
import jp.plen.scenography.models.PlenMotion;
import jp.plen.scenography.utils.PlenMotionJsonHelper;
import jp.plen.scenography.views.ProgramListView;
import jp.plen.scenography.views.adapter.PlenMotionListPagerAdapter;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * プログラム編集画面
 * Created by kzm4269 on 15/06/14.
 */
public class EditFragment extends Fragment {
    private static final String TAG = EditFragment.class.getSimpleName();
    private static final String FULL_TAG = EditFragment.class.getName();
    public static final String PREF_ROOT_DIRECTORY = FULL_TAG + ".PREF_ROOT_DIRECTORY";
    public static final String PREF_CURRENT_FILE_PATH = FULL_TAG + ".PREF_CURRENT_FILE_PATH";
    public static final String PREF_DEFAULT_PLEN_ADDRESS = FULL_TAG + ".PREF_DEFAULT_PLEN_ADDRESS";
    private static final String STATE_CURRENT_MOTION_PAGE = FULL_TAG + ".STATE_CURRENT_MOTION_PAGE";
    private static final String STATE_PROGRAM_LIST_VIEW = FULL_TAG + ".STATE_PROGRAM_LIST_VIEW";
    final int REQUEST_ENABLE_BT = 1;
    private final IntentFilter mIntentFilter = new IntentFilter();
    private ProgramListView mProgramListView;
    private ViewPager mMotionListPager;
    private File mProgramDirectory;
    private File mCurrentFile;
    private BluetoothAdapter mBluetoothAdapter;
    private CompositeSubscription subscriptions = new CompositeSubscription();
    private boolean mIsScanning = false;
    private IPlenBluetoothLeService mIPlenBluetoothLeService;
    private ProgressDialog mWritingDialog;
    private Toolbar mToolbar;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mIPlenBluetoothLeService = IPlenBluetoothLeService.Stub.asInterface(service);
            setIconEnable(mToolbar.getMenu().findItem(R.id.action_write_program), canWriteProgram());
            if (!canWriteProgram())
                scanPlenAuto();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mIPlenBluetoothLeService = null;
        }
    };
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PlenBluetoothLeService.ACTION_WRITE_FAILURE))
                disconnectPlen();
            setIconEnable(mToolbar.getMenu().findItem(R.id.action_write_program), canWriteProgram());

            if (intent.getAction().equals(PlenBluetoothLeService.ACTION_GATT_SETUP_SUCCESS))
                Toast.makeText(getActivity(), "PLENに接続しました", Toast.LENGTH_LONG).show();
            if (intent.getAction().equals(PlenBluetoothLeService.ACTION_WRITE_SUCCESS)
                    && intent.getIntExtra(PlenBluetoothLeService.EXTRA_WRITE_QUEUE_SIZE, -1) == 0) {
                dismissDialog(mWritingDialog);
                Toast.makeText(getActivity(), "プログラム送信完了しました", Toast.LENGTH_SHORT).show();
            }
            if (intent.getAction().equals(PlenBluetoothLeService.ACTION_WRITE_FAILURE)) {
                if (dismissDialog(mWritingDialog) && getActivity() != null) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle("プログラムの送信に失敗しました")
                            .setMessage("PLENとの接続を確認してください")
                            .setPositiveButton("OK", null)
                            .create().show();
                } else {
                    Toast.makeText(getActivity(), "プログラムの送信に失敗しました", Toast.LENGTH_LONG).show();
                }
            }
        }
    };

    public EditFragment() {
    }

    public static EditFragment newInstance() {
        return new EditFragment();
    }

    private boolean dismissDialog(Dialog dialog) {
        try {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
                return true;
            }
        } catch (IllegalArgumentException ignore) {
        }
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(getActivity().getBaseContext(), PlenBluetoothLeService.class);
        getActivity().bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);

        mIntentFilter.addAction(PlenBluetoothLeService.ACTION_GATT_DISCONNECTED);
        mIntentFilter.addAction(PlenBluetoothLeService.ACTION_GATT_SETUP_SUCCESS);
        mIntentFilter.addAction(PlenBluetoothLeService.ACTION_GATT_SETUP_FAILURE);
        mIntentFilter.addAction(PlenBluetoothLeService.ACTION_WRITE_SUCCESS);
        mIntentFilter.addAction(PlenBluetoothLeService.ACTION_WRITE_FAILURE);
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext())
                .registerReceiver(mBroadcastReceiver, mIntentFilter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_programming, container, false);

        // View生成
        mProgramListView = (ProgramListView) root.findViewById(R.id.program_list_view);
        mMotionListPager = (ViewPager) root.findViewById(R.id.motion_list_pager);

        // Toolbar
        mToolbar = (Toolbar) root.findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.title_edit_fragment);
        mToolbar.inflateMenu(R.menu.menu_main);
        mToolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_delete_program) {
                deleteProgram();
            } else if (id == R.id.action_search_plen) {
                scanPlen();
            } else if (id == R.id.action_write_program) {
                writeProgram();
            } else if (id == R.id.action_licenses) {
                showLicenses();
            }
            return true;
        });
        setIconEnable(mToolbar.getMenu().findItem(R.id.action_write_program), canWriteProgram());

        // MotionListのsetup
        PlenMotionListPagerAdapter mMotionListPagerAdapter;
        try {
            LinkedHashMap<CharSequence, List<PlenMotion>> motions = PlenMotionJsonHelper.parseMotionList(
                    getActivity().getResources().openRawResource(R.raw.motion_list));
            List<CharSequence> motionGroups = new ArrayList<>(motions.keySet());
            mMotionListPagerAdapter = new PlenMotionListPagerAdapter(getActivity(), motionGroups, motions);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        mMotionListPager.setAdapter(mMotionListPagerAdapter);

        // プログラム保存用ディレクトリ
        File rootDir = getActivity().getExternalFilesDir(null);
        if (rootDir == null) throw new AssertionError();
        mProgramDirectory = new File(rootDir, "src");
        if (!mProgramDirectory.exists() && mProgramDirectory.mkdir()) {
            String message = "Cannot create - " + mProgramDirectory.getPath();
            Log.e(TAG, message);
            Log.e(TAG, mProgramDirectory.exists() + "");
            Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
        }
        SharedPreferences preferences = getActivity()
                .getSharedPreferences(FULL_TAG, Context.MODE_PRIVATE);
        preferences.edit()
                .putString(PREF_ROOT_DIRECTORY, rootDir.getAbsolutePath())
                .apply();

        // 状態復元
        onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

        String path = preferences.getString(PREF_CURRENT_FILE_PATH,
                new File(mProgramDirectory, "program.json").getAbsolutePath());
        if (path == null) throw new AssertionError();
        mCurrentFile = new File(path);
        openProgram(mCurrentFile);
        return root;
    }

    private void showLicenses() {
        WebView webView = new WebView(getActivity());
        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.action_licenses)
                .setView(webView)
                .create();

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                dialog.show();
            }
        });
        webView.loadUrl("file:///android_asset/licenses.html");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_CURRENT_MOTION_PAGE, mMotionListPager.getCurrentItem());
        outState.putParcelable(STATE_PROGRAM_LIST_VIEW, mProgramListView.onSaveInstanceState());
    }

    @Override
    public void onDestroy() {
        if (mServiceConnection != null) {
            getActivity().unbindService(mServiceConnection);
        }
        if (mBroadcastReceiver != null) {
            LocalBroadcastManager.getInstance(getActivity().getApplicationContext())
                    .unregisterReceiver(mBroadcastReceiver);
        }
        saveProgram(mCurrentFile);

        super.onDestroy();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void saveProgram(File file) {
        try {
            PlenMotionJsonHelper.saveProgramList(file, mProgramListView.getList());
            Log.d(TAG, "save: " + file.getPath());
            for (PlenMotion motion : mProgramListView.getList()) {
                Log.d(TAG, motion.toString());
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            Toast.makeText(getActivity(), "ファイルが作成できません - " + file.getPath(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean canWriteProgram() {
        try {
            return mIPlenBluetoothLeService != null && mIPlenBluetoothLeService.getState() == PlenBluetoothLeService.STATE_GATT_DISCOVERED;
        } catch (RemoteException e) {
            return false;
        }
    }

    private void openProgram(File file) {
        List<PlenMotion> list;
        try {
            list = PlenMotionJsonHelper.parseProgramList(file);
            Log.d(TAG, "open: " + file.getPath());
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Not found - " + e.getMessage());
            return;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            return;
        }
        mProgramListView.setList(list);
        mCurrentFile = file;

        SharedPreferences preferences = getActivity()
                .getSharedPreferences(FULL_TAG, Context.MODE_PRIVATE);
        preferences.edit()
                .putString(PREF_CURRENT_FILE_PATH, mCurrentFile.getAbsolutePath())
                .apply();
    }

    private void deleteProgram() {
        if (getActivity() == null) return;
        new AlertDialog.Builder(getActivity())
                .setTitle("プログラムを削除しますか？")
                .setPositiveButton("OK", (dialog, which) -> mProgramListView.setList(new ArrayList<>()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void scanPlen() {
        if (getActivity() == null) return;
        if (!setupBluetoothAdapter()) return;
        if (mIsScanning) return;
        mIsScanning = true;

        disconnectPlen();
        List<String> addresses = new ArrayList<>();
        ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.setTitle("PLENを探しています");
        progressDialog.setMessage("しばらくお待ちください");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        progressDialog.show();
        subscriptions.add(PlenScanHelper.scan(mBluetoothAdapter, 1000)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(scanResult -> {
                    if (!addresses.contains(scanResult.getDevice().getAddress()))
                        addresses.add(scanResult.getDevice().getAddress());
                })
                .doOnCompleted(() -> {
                    dismissDialog(progressDialog);
                    showPlenSelectDialog(addresses);
                    mIsScanning = false;
                })
                .doOnError(throwable -> {
                    throwable.printStackTrace();
                    mIsScanning = false;
                })
                .subscribe());
    }

    private void scanPlenAuto() {
        if (getActivity() == null) return;
        if (!setupBluetoothAdapter()) return;
        if (mIsScanning) return;
        mIsScanning = true;

        List<String> addresses = new ArrayList<>();
        subscriptions.add(PlenScanHelper.scan(mBluetoothAdapter, 1000)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(scanResult -> {
                    if (!addresses.contains(scanResult.getDevice().getAddress()))
                        addresses.add(scanResult.getDevice().getAddress());
                })
                .doOnCompleted(() -> {
                    if (addresses.contains(getDefaultPlenAddress()))
                        connectDefaultPlen();
                    mIsScanning = false;
                })
                .doOnError(throwable -> {
                    throwable.printStackTrace();
                    mIsScanning = false;
                })
                .subscribe());
    }

    private boolean setupBluetoothAdapter() {
        if (isBluetoothAdapterEnable()) return true;

        BluetoothManager bluetoothManager =
                (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (isBluetoothAdapterEnable()) return true;

        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        return false;
    }

    private String getDefaultPlenAddress() {
        if (getActivity() == null) return null;

        SharedPreferences preferences = getActivity()
                .getSharedPreferences(FULL_TAG, Context.MODE_PRIVATE);
        return preferences.getString(PREF_DEFAULT_PLEN_ADDRESS, null);
    }

    private void connectDefaultPlen() {
        try {
            if (mIPlenBluetoothLeService.getState() == PlenBluetoothLeService.STATE_GATT_DISCONNECTED) {
                String address = getDefaultPlenAddress();
                if (address != null)
                    mIPlenBluetoothLeService.connectGatt(address);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private boolean isBluetoothAdapterEnable() {
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }

    private void setDefaultPlenAddress(String address) {
        if (getActivity() == null) return;

        SharedPreferences preferences = getActivity()
                .getSharedPreferences(FULL_TAG, Context.MODE_PRIVATE);
        preferences.edit()
                .putString(PREF_DEFAULT_PLEN_ADDRESS, address)
                .apply();
    }

    private void writeProgram() {
        String program = getProgram();
        if (program == null)
            return;

        dismissDialog(mWritingDialog);
        mWritingDialog = new ProgressDialog(getActivity());
        mWritingDialog.setTitle("プログラム送信中");
        mWritingDialog.setMessage("しばらくお待ちください");
        mWritingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mWritingDialog.setCancelable(true);
        mWritingDialog.show();

        try {
            mIPlenBluetoothLeService.write(program);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }

        if (savedInstanceState.containsKey(STATE_CURRENT_MOTION_PAGE)) {
            mMotionListPager.setCurrentItem(savedInstanceState.getInt(STATE_CURRENT_MOTION_PAGE));
        }
        mProgramListView.onRestoreInstanceState(savedInstanceState.getParcelable(STATE_PROGRAM_LIST_VIEW));
    }

    private void setIconEnable(MenuItem item, boolean enable) {
        item.setEnabled(enable);
        Drawable icon = item.getIcon();
        if (icon != null)
            icon.setAlpha(enable ? 255 : 64);
    }

    private void disconnectPlen() {
        try {
            mIPlenBluetoothLeService.disconnectGatt();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void showPlenSelectDialog(List<String> addresses) {
        if (getActivity() == null) return;

        if (addresses.isEmpty()) {
            showPlenNotFoundDialog();
            return;
        }

        String defaultAddress = getDefaultPlenAddress();
        String[] items = addresses.toArray(new String[addresses.size()]);
        new AlertDialog.Builder(getActivity(), R.style.DialogTheme)
                .setTitle("接続するPLENを選択してください")
                .setSingleChoiceItems(items, addresses.indexOf(defaultAddress), (dialog, which) -> {
                    setDefaultPlenAddress(addresses.get(which));
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    setDefaultPlenAddress(null);
                })
                .setPositiveButton("OK", (dialog, which) -> connectDefaultPlen())
                .setNeutralButton("再スキャン", (dialog, which) -> scanPlen())
                .show();
    }

    private void showPlenNotFoundDialog() {
        if (getActivity() == null) return;

        new AlertDialog.Builder(getActivity(), R.style.DialogTheme)
                .setTitle("PLENが見つかりませんでした")
                .setMessage(
                        "PLENが近くにあるのに接続できない場合は\n" +
                                "PLENの電源を一度切って再起動してください")
                .setPositiveButton("OK", null)
                .setNeutralButton("再スキャン", (dialog, which) -> scanPlen())
                .show();
    }

    private String getProgram() {
        if (mProgramListView.getList().isEmpty())
            return null;

        StringBuilder builder = new StringBuilder();
        for (PlenMotion motion : mProgramListView.getList())
            builder.append(String.format("#SC%02X%02X", motion.getNumber(), motion.getLoopCount()));
        return builder.append("$CR").toString();
    }
}
