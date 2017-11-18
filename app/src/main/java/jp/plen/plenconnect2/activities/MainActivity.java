package jp.plen.plenconnect2.activities;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.eccyan.optional.Optional;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.util.List;

import jp.plen.rx.utils.Operators;
import jp.plen.plenconnect2.R;
import jp.plen.plenconnect2.fragments.JoystickFragment_;
import jp.plen.plenconnect2.fragments.ProgrammingFragment_;
import jp.plen.plenconnect2.fragments.dialog.LocationSettingRequestDialogFragment;
import jp.plen.plenconnect2.fragments.dialog.LocationSettingRequestDialogFragment_;
import jp.plen.plenconnect2.fragments.dialog.OpenSourceLicensesDialogFragment;
import jp.plen.plenconnect2.fragments.dialog.OpenSourceLicensesDialogFragment_;
import jp.plen.plenconnect2.fragments.dialog.PlenScanningDialogFragment;
import jp.plen.plenconnect2.fragments.dialog.PlenScanningDialogFragment_;
import jp.plen.plenconnect2.fragments.dialog.SelectPlenDialogFragment;
import jp.plen.plenconnect2.fragments.dialog.SelectPlenDialogFragment_;
import jp.plen.plenconnect2.models.preferences.MainPreferences_;
import jp.plen.plenconnect2.services.PlenConnectionService_;
import jp.plen.plenconnect2.services.PlenScanService_;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

@EActivity(R.layout.activity_main)
public class MainActivity extends Activity implements IMainActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String SCANNING_DIALOG = PlenScanningDialogFragment.class.getSimpleName();
    private static final String SELECT_PLEN_DIALOG = SelectPlenDialogFragment.class.getSimpleName();
    private static final String OSS_LICENSES_DIALOG = OpenSourceLicensesDialogFragment.class.getSimpleName();
    private static final String LOCATION_SETTING_DIALOG = LocationSettingRequestDialogFragment.class.getSimpleName();
    private final ServiceConnection mPlenConnectionService = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };
    private final ServiceConnection mPlenScanService = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };
    private final CompositeSubscription mSubscriptions = new CompositeSubscription();
    @ViewById(R.id.toolbar) Toolbar mToolbar;
    @ViewById(R.id.toolbar_title) TextView mTitle;
    @Bean PlenConnectionActivityPresenter mPresenter;
    @Pref MainPreferences_ mPref;
    @NonNull private Optional<FragmentManager> mFragmentManager = Optional.empty();

    @Override
    public synchronized void notifyBluetoothUnavailable() {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        int requestCode = 1;
        startActivityForResult(intent, requestCode);
    }

    @UiThread
    @Override
    public void notifyLocationUnavailable() {
        CompositeSubscription subscriptions = new CompositeSubscription();
        LocationSettingRequestDialogFragment fragment = LocationSettingRequestDialogFragment_.builder()
                .build();
        fragment.allowEvent()
                .lift(Operators.composite(subscriptions))
                .subscribe(v -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    int requestCode = 1;
                    startActivityForResult(intent, requestCode);
                });
        mFragmentManager.ifPresent(m -> fragment.show(m, LOCATION_SETTING_DIALOG));
        mSubscriptions.add(subscriptions);
    }

    @Override
    public synchronized void notifyPlenScanning() {
        dismissDialogFragment(SCANNING_DIALOG);

        PlenScanningDialogFragment fragment = PlenScanningDialogFragment_.builder().build();
        Subscription subscription = fragment.cancelEvent().subscribe(v -> mPresenter.cancelScan());
        mFragmentManager.ifPresent(fm -> fragment.show(fm, SCANNING_DIALOG));
        mSubscriptions.add(subscription);
    }

    @Override
    public synchronized void notifyPlenScanCancel() {
        dismissDialogFragment(SCANNING_DIALOG);
    }

    @Override
    public synchronized void notifyPlenScanComplete(@NonNull List<String> addresses) {
        dismissDialogFragment(SCANNING_DIALOG);
        dismissDialogFragment(SELECT_PLEN_DIALOG);

        String defaultAddress = mPref.defaultPlenAddress().get();
        CompositeSubscription subscriptions = new CompositeSubscription();

        SelectPlenDialogFragment fragment = SelectPlenDialogFragment_.builder()
                .addresses(addresses.toArray(new String[addresses.size()]))
                .defaultAddress(defaultAddress)
                .build();

        fragment.onDeviceSelectEvent()
                .lift(Operators.composite(subscriptions))
                .subscribe(i -> mPref.edit()
                        .defaultPlenAddress().put(addresses.get(i))
                        .apply());

        fragment.cancelEvent()
                .lift(Operators.composite(subscriptions))
                .subscribe(v -> mPref.edit()
                        .defaultPlenAddress().put(defaultAddress)
                        .apply());

        fragment.okEvent()
                .lift(Operators.composite(subscriptions))
                .subscribe(v -> Optional
                        .ofNullable(mPref.defaultPlenAddress().get())
                        .filter(address -> !address.isEmpty())
                        .ifPresent(mPresenter::connectPlen));

        fragment.rescanEvent()
                .lift(Operators.composite(subscriptions))
                .subscribe(v -> mPresenter.startScan());

        mFragmentManager.ifPresent(m -> fragment.show(m, SELECT_PLEN_DIALOG));

        mSubscriptions.add(subscriptions);
    }

    @UiThread
    @Override
    public void notifyPlenConnectionChanged(boolean connected, boolean now) {
        if (now) {
            if (connected) {
                Toast.makeText(this, R.string.plen_connected, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.plen_disconnected, Toast.LENGTH_SHORT).show();
            }
            updateFragment();
        }
        updateToolbar();
    }

    @Override
    public void notifyWriteTxDataCompleted() {
        updateToolbar();
    }

    @Override
    public synchronized void notifyConnectionError(@NonNull Throwable e) {
        Toast.makeText(this, R.string.connection_error, Toast.LENGTH_SHORT).show();
        Log.e(TAG, "onConnectionError ", e);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate ");
        mSubscriptions.clear();

        Intent intent = getIntent();
        mPref.edit().joystickVisibility().put(intent.getBooleanExtra("joystick", true)).apply();

        // 通信用Service起動
        bindService(new Intent(this, PlenConnectionService_.class), mPlenConnectionService, BIND_AUTO_CREATE);
        bindService(new Intent(this, PlenScanService_.class), mPlenScanService, BIND_AUTO_CREATE);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume ");
        mFragmentManager = Optional.ofNullable(getFragmentManager());
        dismissDialogFragment(SCANNING_DIALOG);
        dismissDialogFragment(SCANNING_DIALOG);
        mFragmentManager
                .map(fm -> (SelectPlenDialogFragment) fm.findFragmentByTag(SELECT_PLEN_DIALOG))
                .map(SelectPlenDialogFragment::getAddresses)
                .ifPresent(this::notifyPlenScanComplete);
        mFragmentManager
                .map(fm -> fm.findFragmentByTag(LOCATION_SETTING_DIALOG))
                .ifPresent(fm -> notifyLocationUnavailable());
        mPresenter.bind(this);
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause ");
        mFragmentManager = Optional.empty();
        mPresenter.unbind();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        mSubscriptions.unsubscribe();
        unbindService(mPlenConnectionService);
        unbindService(mPlenScanService);
        super.onDestroy();
    }

    @AfterViews
    void afterViews() {
        updateToolbar();
        updateFragment();
    }

    @UiThread
    void updateToolbar() {
        // mToolbar.setTitle(R.string.app_name);

        mToolbar.getMenu().clear();

        if (mPref.joystickVisibility().get()) {
            mToolbar.inflateMenu(R.menu.menu_joystick);
            mTitle.setText(getResources().getString(R.string.joystick_title));
        } else {
            mToolbar.inflateMenu(R.menu.menu_program);
            mTitle.setText(getResources().getString(R.string.program_title));
        }

        mToolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_search_plen) {
                mPresenter.disconnectPlen();
                mPresenter.startScan();
            } else if (id == R.id.action_licenses) {
                mFragmentManager.ifPresent(m -> OpenSourceLicensesDialogFragment_.builder().build()
                        .show(m, OSS_LICENSES_DIALOG));
            }
            return true;
        });
    }

    @UiThread
    void updateFragment() {
        if (mPref.joystickVisibility().get()) {
            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, JoystickFragment_.builder().build())
                    .commit();
        } else {
            Bundle args = new Bundle();
            boolean writable = mPresenter.getPlenConnected() && !mPresenter.getWriting();
            args.putBoolean("WRITABLE", writable);

            Fragment programmingFragment = ProgrammingFragment_.builder().build();
            programmingFragment.setArguments(args);

            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, programmingFragment)
                    .commit();
        }
    }

    @UiThread
    void dismissDialogFragment(@NonNull String tag) {
        mFragmentManager.map(fm -> fm.findFragmentByTag(tag))
                .filter(f -> f instanceof DialogFragment)
                .map(f -> (DialogFragment) f)
                .filter(DialogFragment::getShowsDialog)
                .ifPresent(f -> f.onDismiss(f.getDialog()));
 }
}
