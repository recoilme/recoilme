package ru.recoilme;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.util.Log;
import ru.recoilme.imageloader.ImageCache;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import static android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE;

public class AndroidApplication extends android.app.Application {

    private static SharedPreferences 	settings;
    private static String current_package,tag;
    private static Boolean _debug;

    @Override
    public void onCreate() {
        super.onCreate();
        current_package = this.getPackageName();
        tag = "recoilme." + current_package;
        ApplicationInfo appInfo = this.getApplicationInfo();
        _debug = (appInfo.flags & FLAG_DEBUGGABLE) != 0;
        settings = getSharedPreferences(current_package, Context.MODE_PRIVATE);
    }

    public static void putPrefsString(String name, String value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(name, value);
        editor.commit();
    }

    public static String getPrefsString(String name) {
        return settings.getString(name, "");
    }

    public static void putPrefsBoolean(String name, boolean value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(name, value);
        editor.commit();
    }

    public static boolean getPrefsBoolean(String name) {
        return settings.getBoolean(name, false);
    }

    public static void log(Object obj) {
        String msg = (obj instanceof Exception) ? Log
                .getStackTraceString((Exception) obj) : String.valueOf(obj);
        if (_debug) {
            StackTraceElement caller = Thread.currentThread().getStackTrace()[5];
            String c = caller.getClassName();
            String className = c.substring(c.lastIndexOf(".") + 1, c.length());
            StringBuilder sb = new StringBuilder(7);
            sb.append(tag);
            sb.append(".");
            sb.append(className);
            sb.append(".");
            sb.append(caller.getMethodName());
            sb.append("():");
            sb.append(caller.getLineNumber());

            Log.println(Log.DEBUG, sb.toString(), msg);
        }
    }

    //imageloader
    public static interface OnLowMemoryListener {
        public void onLowMemoryReceived();
    }


	private static final int CORE_POOL_SIZE = 5;

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "GreenDroid thread #" + mCount.getAndIncrement());
        }
    };

    private ExecutorService mExecutorService;
    private ImageCache mImageCache;
    private ArrayList<WeakReference<OnLowMemoryListener>> mLowMemoryListeners;

    public AndroidApplication() {
        mLowMemoryListeners = new ArrayList<WeakReference<OnLowMemoryListener>>();
    }

    public ExecutorService getExecutor() {
        if (mExecutorService == null) {
            mExecutorService = Executors.newFixedThreadPool(CORE_POOL_SIZE, sThreadFactory);
        }
        return mExecutorService;
    }

    public ImageCache getImageCache() {
        if (mImageCache == null) {
            mImageCache = new ImageCache(this);
        }
        return mImageCache;
    }


    public void registerOnLowMemoryListener(OnLowMemoryListener listener) {
        if (listener != null) {
            mLowMemoryListeners.add(new WeakReference<OnLowMemoryListener>(listener));
        }
    }


    public void unregisterOnLowMemoryListener(OnLowMemoryListener listener) {
        if (listener != null) {
            int i = 0;
            while (i < mLowMemoryListeners.size()) {
                final OnLowMemoryListener l = mLowMemoryListeners.get(i).get();
                if (l == null || l == listener) {
                    mLowMemoryListeners.remove(i);
                } else {
                    i++;
                }
            }
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        int i = 0;
        while (i < mLowMemoryListeners.size()) {
            final OnLowMemoryListener listener = mLowMemoryListeners.get(i).get();
            if (listener == null) {
                mLowMemoryListeners.remove(i);
            } else {
                listener.onLowMemoryReceived();
                i++;
            }
        }
    }
    //imageloader
}