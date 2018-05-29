package jp.plen.plenconnect2.fragments.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.androidannotations.annotations.EFragment;

import jp.plen.plenconnect2.R;


@EFragment
public class OpenSourceLicensesDialogFragment extends DialogFragment {
    private static final String TAG = OpenSourceLicensesDialogFragment.class.getSimpleName();

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
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
        return dialog;
    }
}
