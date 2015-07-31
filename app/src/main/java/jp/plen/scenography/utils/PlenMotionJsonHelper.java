package jp.plen.scenography.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import jp.plen.scenography.models.PlenMotion;

/**
 * モーションデータの取り込み
 * Created by kzm4269 on 15/06/21.
 */
public final class PlenMotionJsonHelper {
    private static final String TAG = PlenMotionJsonHelper.class.getSimpleName();

    private PlenMotionJsonHelper() {
    }

    public static LinkedHashMap<CharSequence, List<PlenMotion>> parseMotionList(InputStream in) throws IOException {
        List<MotionListData> list = new ObjectMapper().readValue(in, new TypeReference<List<MotionListData>>() {
        });

        LinkedHashMap<CharSequence, List<PlenMotion>> result = new LinkedHashMap<>();
        for (MotionListData data : list) {
            List<PlenMotion> motions = new ArrayList<>();
            for (MotionSource source : data.motions) {
                motions.add(source.toPlenMotion());
            }
            result.put(data.category, motions);
        }
        return result;
    }

    public static void saveProgramList(File out, List<PlenMotion> motions) throws IOException {
        new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValue(out, motions);
    }

    public static List<PlenMotion> parseProgramList(File in) throws IOException {
        List<MotionSource> sources = new ObjectMapper().readValue(in, new TypeReference<List<MotionSource>>() {
        });
        List<PlenMotion> motions = new ArrayList<>();
        for (MotionSource source : sources) {
            motions.add(source.toPlenMotion());
        }
        return motions;
    }

    public static class MotionSource {
        public int number;
        public CharSequence name;
        public String iconName;
        public int loopCount = 1;

        public PlenMotion toPlenMotion() {
            PlenMotion motion = new PlenMotion(number, name, iconName);
            motion.setLoopCount(loopCount);
            return motion;
        }
    }

    public static class MotionListData {
        public String category;
        public List<MotionSource> motions;
    }
}
