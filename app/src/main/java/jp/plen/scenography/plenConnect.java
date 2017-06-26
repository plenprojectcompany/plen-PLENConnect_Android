package jp.plen.scenography;

import android.app.Application;
import android.support.annotation.NonNull;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EApplication;

import jp.plen.scenography.models.plenConnectModel;

@EApplication
public class plenConnect extends Application {
    private static final String TAG = plenConnect.class.getSimpleName();
    private static plenConnect sInstance;

    private plenConnectModel mModel;

    public plenConnect() {
        sInstance = this;
    }

    @AfterInject
    void afterInject() {
        mModel = plenConnectModel.create(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @NonNull
    public static plenConnectModel getModel() {
        if (sInstance == null) throw new AssertionError();
        if (sInstance.mModel == null) throw new AssertionError();
        return sInstance.mModel;
    }
}
