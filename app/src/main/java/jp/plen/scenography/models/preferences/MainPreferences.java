package jp.plen.scenography.models.preferences;

import android.support.annotation.NonNull;

import org.androidannotations.annotations.sharedpreferences.DefaultBoolean;
import org.androidannotations.annotations.sharedpreferences.DefaultString;
import org.androidannotations.annotations.sharedpreferences.SharedPref;

@SharedPref
public interface MainPreferences {
    @DefaultBoolean(false)
    boolean joystickVisibility();

    @NonNull
    @DefaultString("sample/")
    String sampleProgram();

    @NonNull
    String defaultPlenAddress();
}
