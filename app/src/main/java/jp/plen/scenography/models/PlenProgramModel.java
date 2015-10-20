package jp.plen.scenography.models;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.eccyan.optional.Optional;

import java.io.File;
import java.util.Collections;
import java.util.List;

import jp.plen.rx.binding.Property;
import jp.plen.rx.binding.ReadOnlyProperty;
import jp.plen.scenography.R;
import jp.plen.scenography.models.entities.PlenCodeUnit;
import jp.plen.scenography.models.entities.PlenMotionCategory;
import jp.plen.scenography.models.entities.PlenProgram;
import jp.plen.scenography.utils.JsonUtil;
import rx.Observable;

/**
 * PlenProgramの生成と管理
 */
public class PlenProgramModel {
    private static final String TAG = PlenProgramModel.class.getSimpleName();
    private final Property<List<PlenMotionCategory>> mMotionCategories = Property.create();
    private final Property<List<PlenCodeUnit>> mSequence = Property.create();
    private final ReadOnlyProperty<PlenProgram> mProgram = ReadOnlyProperty.create(
            Observable.combineLatest(
                    mMotionCategories.asObservable(),
                    mSequence.asObservable(),
                    PlenProgram::new));
    @NonNull private final Context mContext;
    @NonNull private Optional<File> mDirectory = Optional.empty();

    protected PlenProgramModel(@NonNull Context context) {
        mContext = context;
    }

    public void open(@NonNull File programDirectory) {
        if (!programDirectory.isDirectory()) {
            throw new IllegalArgumentException(
                    "program directory must be a directory: " + programDirectory);
        }
        mDirectory = Optional.of(programDirectory);
    }

    @NonNull
    public static PlenProgramModel create(@NonNull Context context) {
        return new PlenProgramModel(context);
    }

    @NonNull
    public Property<List<PlenCodeUnit>> sequence() {
        return mSequence;
    }

    @NonNull
    public ReadOnlyProperty<List<PlenMotionCategory>> motionCategories() {
        return mMotionCategories;
    }

    @NonNull
    public ReadOnlyProperty<PlenProgram> program() {
        return mProgram;
    }

    @NonNull
    public Observable<Void> fetch() {
        return processFetchRequest()
                .doOnNext(response -> {
                    mMotionCategories.set(response.categories);
                    mSequence.set(response.sequence);
                })
                .doOnError(e -> Log.e(TAG, "fetch error"))
                .doOnCompleted(() -> Log.i(TAG, "fetch complete"))
                .flatMap(r -> Observable.<Void>empty());
    }

    @NonNull
    public Observable<Void> push() {
        return processPushRequest()
                .doOnError(e -> Log.e(TAG, "push error"))
                .doOnCompleted(() -> Log.i(TAG, "push complete"))
                .flatMap(r -> Observable.<Void>empty());
    }

    @NonNull
    private Observable<FetchResponse> processFetchRequest() {
        Log.i(TAG, "fetch request: " + mDirectory.map(File::getPath).orElse(""));
        if (!mDirectory.isPresent()) {
            return Observable.empty();
        }
        return Observable.zip(
                JsonUtil.readPlenMotions(motionsFile().get()).toList(),
                JsonUtil.readPLenProgramSequence(sequenceFile().get()).toList(),
                FetchResponse::new);
    }

    @NonNull
    private Observable<PushResponse> processPushRequest() {
        Log.i(TAG, "push request: " + mDirectory.map(File::getPath).orElse(""));
        if (!mDirectory.isPresent()) {
            return Observable.empty();
        }
        return JsonUtil
                .writePlenProgramSequence(
                        sequenceFile().get(),
                        mSequence.get().orElse(Collections.emptyList()))
                .map(v -> new PushResponse());
    }

    private Optional<File> motionsFile() {
        return mDirectory.map(d -> d.getAbsolutePath() + "/" + mContext.getString(R.string.motions_file_name)).map(File::new);
    }

    private Optional<File> sequenceFile() {
        return mDirectory.map(d -> d.getAbsolutePath() + "/" + mContext.getString(R.string.sequence_file_name)).map(File::new);
    }

    private static class FetchResponse {
        public final List<PlenMotionCategory> categories;
        public final List<PlenCodeUnit> sequence;

        public FetchResponse(List<PlenMotionCategory> categories, List<PlenCodeUnit> sequence) {
            this.categories = categories;
            this.sequence = sequence;
        }
    }

    private static class PushResponse {
    }
}
