package jp.plen.plenconnect2.fragments.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.androidannotations.annotations.EFragment;

import jp.plen.plenconnect2.R;
import rx.Observable;
import rx.subjects.PublishSubject;

@EFragment
public class LocationSettingRequestDialogFragment extends DialogFragment {
    private static final String TAG = LocationSettingRequestDialogFragment.class.getSimpleName();

    private final PublishSubject<Void> mAllowEvent = PublishSubject.create();
    private final PublishSubject<Void> mDenyEvent = PublishSubject.create();

    @NonNull
    public Observable<Void> allowEvent() {
        return mAllowEvent;
    }

    @NonNull
    public Observable<Void> denyEvent() {
        return mDenyEvent;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity(), R.style.DialogTheme)
                .setMessage(R.string.location_setting_dialog_message)
                .setPositiveButton(
                        R.string.action_allow,
                        (dialog, which) -> mAllowEvent.onNext(null))
                .setNegativeButton(
                        R.string.action_deny,
                        (dialog, which) -> mDenyEvent.onNext(null))
                .create();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        mDenyEvent.onNext(null);
    }

    @Override
    public void onDestroy() {
        mAllowEvent.onCompleted();
        mDenyEvent.onCompleted();
        super.onDestroy();
    }
}
