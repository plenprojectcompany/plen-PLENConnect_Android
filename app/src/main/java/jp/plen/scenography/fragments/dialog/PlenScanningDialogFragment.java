package jp.plen.scenography.fragments.dialog;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.RootContext;

import jp.plen.scenography.R;
import rx.Observable;
import rx.subjects.PublishSubject;

@EFragment
public class PlenScanningDialogFragment extends DialogFragment {
    private static final String TAG = PlenScanningDialogFragment.class.getSimpleName();
    private final PublishSubject<Void> mCancelEvent = PublishSubject.create();

    @NonNull
    public Observable<Void> cancelEvent() {
        return mCancelEvent;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog progressDialog = new ProgressDialog(getActivity(), R.style.DialogTheme);
        progressDialog.setTitle(getActivity().getString(R.string.scanning_dialog_title));
        progressDialog.setMessage(getActivity().getString(R.string.scanning_dialog_message));
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        return progressDialog;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        mCancelEvent.onNext(null);
    }

    @Override
    public void onDestroy() {
        mCancelEvent.onCompleted();
        super.onDestroy();
    }
}
