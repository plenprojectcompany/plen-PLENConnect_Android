package jp.plen.scenography.models;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import jp.plen.scenography.R;
import jp.plen.scenography.exceptions.ScenographyRuntimeException;
import jp.plen.scenography.utils.JsonUtil;
import rx.functions.Actions;

public class ScenographyModel {
    private static final String TAG = ScenographyModel.class.getSimpleName();
    @NonNull private final Context mContext;
    @NonNull private final PlenProgramModel mCurrentProgram;

    protected ScenographyModel(@NonNull Context context) {
        mContext = context;
        try {
            setupSampleProgram();
        } catch (IOException e) {
            Log.e(TAG, "setup sample program failed", e);
        }
        mCurrentProgram = PlenProgramModel.create(context);

        initCurrentProgram();
    }

    @NonNull
    public static ScenographyModel create(@NonNull Context context) {
        return new ScenographyModel(context);
    }

    @NonNull
    public PlenProgramModel currentProgram() {
        return mCurrentProgram;
    }

    private void initCurrentProgram() {
        mCurrentProgram.open(new File(mContext.getFilesDir().getPath() + "/" + mContext.getString(R.string.sample_program_directory)));
    }

    private void setupSampleProgram() throws IOException {
        // setup directory
        File dir = new File(mContext.getFilesDir().getPath() + "/" + mContext.getString(R.string.sample_program_directory));
        if (dir.exists() && !dir.isDirectory()) {
            if (!dir.delete()) {
                throw new IOException("cannot delete " + dir.getPath());
            }
        }
        if (!(dir.mkdirs() || dir.isDirectory())) {
            throw new IOException("cannot create " + dir.getPath());
        }

        // setup motions
        File motionsFile = new File(dir.getPath() + "/" + mContext.getString(R.string.motions_file_name));
        try (InputStream in = mContext.getResources().openRawResource(R.raw.default_motions)) {
            JsonUtil.readPlenMotions(in)
                    .toList()
                    .flatMap(categories -> JsonUtil.writePlenMotions(motionsFile, categories))
                    .doOnCompleted(() -> Log.d(TAG, "setup motions file"))
                    .subscribe(Actions.empty(), e -> {
                        throw new ScenographyRuntimeException("setup motions file", e);
                    });
        } catch (ScenographyRuntimeException e) {
            throw new IOException(e);
        }

        // setup sequence
        File sequenceFile = new File(dir.getPath() + "/" + mContext.getString(R.string.sequence_file_name));
        if (!sequenceFile.exists()) {
            try {
                JsonUtil.writePlenMotions(sequenceFile, Collections.emptyList())
                        .doOnCompleted(() -> Log.d(TAG, "setup sequence file"))
                        .subscribe(Actions.empty(), e -> {
                            throw new ScenographyRuntimeException("setup sequence file", e);
                        });
            } catch (ScenographyRuntimeException e) {
                throw new IOException(e);
            }
        }
    }
}
