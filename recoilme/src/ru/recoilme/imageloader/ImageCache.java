package ru.recoilme.imageloader;

import android.content.Context;
import android.graphics.Bitmap;
import ru.recoilme.AndroidApplication.OnLowMemoryListener;

import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;


public class ImageCache implements OnLowMemoryListener {

    private final ConcurrentHashMap<String, SoftReference<Bitmap>> mSoftCache;

    public ImageCache(Context context) {
        mSoftCache = new ConcurrentHashMap<String, SoftReference<Bitmap>>();
        GDUtils.getGDApplication(context).registerOnLowMemoryListener(this);
    }

    public static ImageCache from(Context context) {
        return GDUtils.getImageCache(context);
    }

    public Bitmap get(String url) {
        final SoftReference<Bitmap> ref = mSoftCache.get(url);
        if (ref == null) {
            return null;
        }

        final Bitmap bitmap = ref.get();
        if (bitmap == null) {
            mSoftCache.remove(url);
        }
        return bitmap;
    }

    public void put(String url, Bitmap bitmap) {
        mSoftCache.putIfAbsent(url, new SoftReference<Bitmap>(bitmap));
    }

    public void flush() {
        mSoftCache.clear();
    }

    public void onLowMemoryReceived() {
        flush();
    }
}
