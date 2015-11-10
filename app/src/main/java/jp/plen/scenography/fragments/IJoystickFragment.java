package jp.plen.scenography.fragments;

import android.support.annotation.NonNull;

import jp.plen.scenography.models.PlenProgramModel;
import rx.Subscription;

public interface IJoystickFragment {
    @NonNull
    Subscription bind(@NonNull PlenProgramModel model);
}
