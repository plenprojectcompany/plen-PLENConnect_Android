package jp.plen.scenography.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

import jp.plen.scenography.R;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public final class PlenMotionIconLoader {
    private static final String TAG = PlenMotionIconLoader.class.getSimpleName();
    private static final int CACHE_SIZE = 1024 * 1024;  // Byte
    private static final LruCache<String, Bitmap> mCache = new LruCache<String, Bitmap>(CACHE_SIZE) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getRowBytes() * value.getHeight();
        }
    };
    private static Bitmap sNoImageBitmap;

    private PlenMotionIconLoader() {
    }

    public static void load(@NonNull ImageView imageView, @NonNull final String resourceName) {
        final Context context = imageView.getContext();
        final Bitmap cached = mCache.get(resourceName);
        if (cached != null) {
            imageView.setImageBitmap(cached);
            return;
        }

        if (resourceName.equals(imageView.getTag())) {
            return;
        }

        final WeakReference<ImageView> imageViewWeakReference = new WeakReference<>(imageView);
        imageView.setImageBitmap(sNoImageBitmap);
        imageView.setTag(resourceName);

        Observable
                .<Bitmap>create(subscriber -> {
                    Bitmap resource = decodeResourceBitmap(context, resourceName);
                    Bitmap resized = resizeBitmap(resource, context.getResources().getDimension(R.dimen.motion_icon_width), context.getResources().getDimension(R.dimen.motion_icon_height));
                    if (resource != sNoImageBitmap)
                        resource.recycle();
                    mCache.put(resourceName, resized);
                    subscriber.onNext(resized);
                    subscriber.onCompleted();
                })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(progress -> {
                    ImageView view = imageViewWeakReference.get();
                    if (view != null && resourceName.equals(view.getTag())) {
                        view.setImageBitmap(progress);
                        view.setTag(null);
                    }
                })
                .doOnError(e -> Log.e(TAG, e.getMessage()))
                .subscribe();
    }

    private static Bitmap decodeResourceBitmap(Context context, String resourceName) {
        Log.d(TAG, "decode Bitmap");

        Resources resources = context.getResources();
        int id = resources.getIdentifier(resourceName, "drawable", context.getPackageName());
        if (id == 0) {
            return getNoImageBitmap(context);
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        options.inPreferredConfig = Bitmap.Config.ARGB_4444;
        options.inDensity = displayMetrics.densityDpi;
        return BitmapFactory.decodeResource(resources, id, options);
    }

    private static Bitmap resizeBitmap(Bitmap source, float width, float height) {
        Matrix resizeMatrix = new Matrix();
        float resizeRatio = Math.min(width / source.getWidth(), height / source.getHeight());
        resizeMatrix.postScale(resizeRatio, resizeRatio);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), resizeMatrix, true);
    }

    private static Bitmap getNoImageBitmap(Context context) {
        if (sNoImageBitmap != null) return sNoImageBitmap;

        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.no_image);
        if (drawable != null) {
            sNoImageBitmap = ((BitmapDrawable) drawable).getBitmap();
        } else {
            throw new AssertionError("Can't load: R.drawable.no_image");
        }
        return sNoImageBitmap;
    }
}
