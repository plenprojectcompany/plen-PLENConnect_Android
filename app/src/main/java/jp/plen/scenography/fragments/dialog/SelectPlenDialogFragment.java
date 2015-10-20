package jp.plen.scenography.fragments.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jp.plen.scenography.R;
import rx.Observable;
import rx.subjects.PublishSubject;

@EFragment
public class SelectPlenDialogFragment extends DialogFragment {
    private static final String TAG = SelectPlenDialogFragment.class.getSimpleName();
    private final PublishSubject<Integer> mDeviceSelectEvent = PublishSubject.create();
    private final PublishSubject<Void> mOkEvent = PublishSubject.create();
    private final PublishSubject<Void> mCancelEvent = PublishSubject.create();
    private final PublishSubject<Void> mRescanEvent = PublishSubject.create();

    @Nullable
    @FragmentArg
    String[] addresses;

    @Nullable
    @FragmentArg
    String defaultAddress;

    @NonNull
    public Observable<Integer> onDeviceSelectEvent() {
        return mDeviceSelectEvent;
    }

    @NonNull
    public Observable<Void> okEvent() {
        return mOkEvent;
    }

    @NonNull
    public Observable<Void> cancelEvent() {
        return mCancelEvent;
    }

    @NonNull
    public Observable<Void> rescanEvent() {
        return mRescanEvent;
    }

    @NonNull
    public List<String> getAddresses() {
        return addresses != null ? Arrays.asList(addresses) : Collections.emptyList();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (addresses == null || addresses.length == 0) {
            return new AlertDialog.Builder(getActivity(), R.style.DialogTheme)
                    .setTitle(R.string.plen_not_found_dialog_title)
                    .setMessage(R.string.plen_not_found_dialog_message)
                    .setPositiveButton(R.string.action_ok, null)
                    .setNeutralButton(
                            R.string.action_rescan,
                            (dialog, which) -> mRescanEvent.onNext(null))
                    .create();
        }

        return new AlertDialog.Builder(getActivity(), R.style.DialogTheme)
                .setTitle(R.string.select_plen_dialog_title)
                .setSingleChoiceItems(
                        addresses,
                        Arrays.asList(addresses).indexOf(defaultAddress),
                        (dialog, which) -> mDeviceSelectEvent.onNext(which))
                .setPositiveButton(
                        R.string.action_ok,
                        (dialog, which) -> mOkEvent.onNext(null))
                .setNegativeButton(
                        R.string.action_cancel,
                        (dialog, which) -> mCancelEvent.onNext(null))
                .setNeutralButton(
                        R.string.action_rescan,
                        (dialog, which) -> mRescanEvent.onNext(null))
                .create();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        mCancelEvent.onNext(null);
    }

    @Override
    public void onDestroy() {
        mDeviceSelectEvent.onCompleted();
        mOkEvent.onCompleted();
        mCancelEvent.onCompleted();
        mRescanEvent.onCompleted();
        super.onDestroy();
    }
}
