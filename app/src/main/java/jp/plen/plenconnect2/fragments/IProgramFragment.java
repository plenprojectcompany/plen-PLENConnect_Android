package jp.plen.plenconnect2.fragments;

import android.support.annotation.NonNull;

import jp.plen.plenconnect2.models.PlenProgramModel;
import rx.Subscription;

public interface IProgramFragment {
    @NonNull
    Subscription bind(@NonNull PlenProgramModel model);
}
