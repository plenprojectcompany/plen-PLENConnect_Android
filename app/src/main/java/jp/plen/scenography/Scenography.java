package jp.plen.scenography;

import android.app.Application;
import android.support.annotation.NonNull;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EApplication;

import jp.plen.scenography.models.ScenographyModel;

@EApplication
public class Scenography extends Application {
    private static final String TAG = Scenography.class.getSimpleName();
    private static Scenography sInstance;

    private ScenographyModel mModel;

    public Scenography() {
        sInstance = this;
    }

    @AfterInject
    void afterInject() {
        mModel = ScenographyModel.create(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @NonNull
    public static ScenographyModel getModel() {
        if (sInstance == null) throw new AssertionError();
        if (sInstance.mModel == null) throw new AssertionError();
        return sInstance.mModel;
    }
}
