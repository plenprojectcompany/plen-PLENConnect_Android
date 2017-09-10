package jp.plen.scenography.utils;

import android.support.annotation.NonNull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import jp.plen.scenography.models.entities.PlenCodeUnit;
import jp.plen.scenography.models.entities.PlenMotion;
import jp.plen.scenography.models.entities.PlenMotionCategory;
import rx.Observable;
import rx.observables.AbstractOnSubscribe;

/**
 * モーションリストとプログラムの入出力関連.
 */
public final class JsonUtil {
    private static final String TAG = JsonUtil.class.getSimpleName();

    private JsonUtil() {
    }

    public static Observable<PlenMotionCategory> readPlenMotions(@NonNull InputStream in) {
        return JsonUtil.readList(in, PlenMotionCategory_.LIST).map(PlenMotionCategory_::build);
    }

    public static Observable<PlenMotionCategory> readPlenMotions(@NonNull File in) {
        return JsonUtil.readList(in, PlenMotionCategory_.LIST).map(PlenMotionCategory_::build);
    }

    public static Observable<PlenCodeUnit> readPLenProgramSequence(@NonNull InputStream in) {
        return JsonUtil.readList(in, PlenProgramUnit_.LIST).map(PlenProgramUnit_::build);
    }

    public static Observable<PlenCodeUnit> readPLenProgramSequence(@NonNull File in) {
        return JsonUtil.readList(in, PlenProgramUnit_.LIST).map(PlenProgramUnit_::build);
    }

    public static Observable<Void> writePlenMotions(@NonNull OutputStream out, @NonNull List<PlenMotionCategory> categories) {
        return JsonUtil.writeList(out, Observable.from(categories).map(PlenMotionCategory_::new));
    }

    public static Observable<Void> writePlenMotions(@NonNull File out, @NonNull List<PlenMotionCategory> categories) {
        return JsonUtil.writeList(out, Observable.from(categories).map(PlenMotionCategory_::new));
    }

    public static Observable<Void> writePlenProgramSequence(@NonNull OutputStream out, @NonNull List<PlenCodeUnit> sequence) {
        return JsonUtil.writeList(out, Observable.from(sequence).map(PlenProgramUnit_::new));
    }

    public static Observable<Void> writePlenProgramSequence(@NonNull File out, @NonNull List<PlenCodeUnit> sequence) {
        return JsonUtil.writeList(out, Observable.from(sequence).map(PlenProgramUnit_::new));
    }

    private static <T> Observable<T> readList(@NonNull InputStream in, @NonNull TypeReference<List<T>> typeReference) {
        return Observable
                .<List<T>>create(AbstractOnSubscribe.create(subscriber -> {
                    try {
                        subscriber.onNext(new ObjectMapper().<List<T>>readValue(in, typeReference));
                        subscriber.onCompleted();
                    } catch (IOException e) {
                        subscriber.onError(e);
                    }
                }))
                .flatMap(Observable::from);
    }

    private static <T> Observable<T> readList(@NonNull File in, @NonNull TypeReference<List<T>> typeReference) {
        return Observable
                .<List<T>>create(AbstractOnSubscribe.create(subscriber -> {
                    try {
                        subscriber.onNext(new ObjectMapper().<List<T>>readValue(in, typeReference));
                        subscriber.onCompleted();
                    } catch (IOException e) {
                        subscriber.onError(e);
                    }
                }))
                .flatMap(Observable::from);
    }

    private static <T> Observable<Void> writeList(@NonNull OutputStream out, @NonNull Observable<T> values) {
        return Observable
                .<Void>create(AbstractOnSubscribe.create(subscriber -> {
                    try {
                        new ObjectMapper()
                                .enable(SerializationFeature.INDENT_OUTPUT)
                                .writeValue(out, values.toList().toBlocking().first());
                        subscriber.onNext(null);
                        subscriber.onCompleted();
                    } catch (IOException e) {
                        subscriber.onError(e);
                    }
                }));
    }

    private static <T> Observable<Void> writeList(@NonNull File out, @NonNull Observable<T> values) {
        return Observable
                .<Void>create(AbstractOnSubscribe.create(subscriber -> {
                    try {
                        new ObjectMapper()
                                .enable(SerializationFeature.INDENT_OUTPUT)
                                .writeValue(out, values.toList().toBlocking().first());
                        subscriber.onNext(null);
                        subscriber.onCompleted();
                    } catch (IOException e) {
                        subscriber.onError(e);
                    }
                }));
    }

    public static class PlenMotion_ {
        public int id;
        public String name;
        public String icon;

        public PlenMotion_() {
        }

        public PlenMotion_(@NonNull PlenMotion motion) {
            this.id = motion.getId();
            this.name = motion.getName();
            this.icon = motion.getIconPath();
        }

        @NonNull
        public PlenMotion build() {
            return new PlenMotion(id, name, icon);
        }
    }

    public static class PlenMotionCategory_ {
        public static final TypeReference<List<PlenMotionCategory_>> LIST =
                new TypeReference<List<PlenMotionCategory_>>() {
                };
        public String mode;
        public String name;
        public List<PlenMotion_> motions;

        public PlenMotionCategory_() {
        }

        public PlenMotionCategory_(@NonNull PlenMotionCategory category) {
            this.mode = category.getMode();
            this.name = category.getName();
            this.motions = Observable.from(category.getMotions())
                    .map(PlenMotion_::new)
                    .toList().toBlocking()
                    .single();
        }

        @NonNull
        public PlenMotionCategory build() {
            return new PlenMotionCategory(
                    mode,
                    name,
                    Observable.from(motions)
                            .map(PlenMotion_::build)
                            .toList()
                            .toBlocking().single());
        }
    }

    public static class PlenProgramUnit_ {
        public static final TypeReference<List<PlenProgramUnit_>> LIST =
                new TypeReference<List<PlenProgramUnit_>>() {
                };

        public int id;
        public int loop;

        public PlenProgramUnit_() {
        }

        public PlenProgramUnit_(@NonNull PlenCodeUnit unit) {
            this.id = unit.getMotionId();
            this.loop = unit.getLoopCount();
        }

        @NonNull
        public PlenCodeUnit build() {
            return new PlenCodeUnit(id, loop);
        }
    }
}
