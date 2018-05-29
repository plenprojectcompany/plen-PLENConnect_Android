package jp.plen.plenconnect2.models;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import jp.plen.plenconnect2.R;
import jp.plen.plenconnect2.utils.JsonUtil;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Actions;

import static org.junit.Assert.assertNotNull;


@RunWith(AndroidJUnit4.class)
public class PlenProgramModelTest {
    private static final String TAG = PlenProgramModelTest.class.getSimpleName();
    private Context mContext;
    private File mMotionsFile;
    private File mSequenceFile;

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getTargetContext();
        assertNotNull(mContext);

        File dir = new File(mContext.getFilesDir().getPath() + "/" + "sample/");
        mMotionsFile = new File(dir.getPath() + "/" + "motions.json");
        mSequenceFile = new File(dir.getPath() + "/" + "sequence.json");

        // setup directory
        if (dir.exists() && !dir.isDirectory()) {
            if (!dir.delete()) {
                throw new IOException("cannot delete " + dir.getPath());
            }
        }
        if (!(dir.mkdirs() || dir.isDirectory())) {
            throw new IOException("cannot create " + dir.getPath());
        }

        // setup motions
        if (!mMotionsFile.exists()) {
            try (InputStream in = mContext.getResources().openRawResource(R.raw.default_motions_ja)) {
                JsonUtil.readPlenMotions(in)
                        .toList()
                        .flatMap(categories -> JsonUtil.writePlenMotions(mMotionsFile, categories))
                        .doOnCompleted(() -> Log.d(TAG, "setup motions file"))
                        .toBlocking().forEach(Actions.empty());
            } catch (RuntimeException e) {
                throw new IOException(e);
            }
        }

        // setup sequence
        if (!mSequenceFile.exists()) {
            try {
                JsonUtil.writePlenMotions(mSequenceFile, Collections.emptyList())
                        .doOnCompleted(() -> Log.d(TAG, "setup sequence file"))
                        .toBlocking().forEach(Actions.empty());
            } catch (RuntimeException e) {
                throw new IOException(e);
            }
        }
    }

    @Test
    public void testFetch() throws Exception {
        PlenProgramModel model = PlenProgramModel.create(mContext, mMotionsFile, mSequenceFile);
        model.fetch().observeOn(AndroidSchedulers.mainThread())
                .subscribe(v -> Log.d(TAG, "ok"));
    }
}