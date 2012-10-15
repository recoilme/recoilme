package ru.recoilme.imageloader;


import android.content.Context;
import ru.recoilme.AndroidApplication;

import java.util.concurrent.ExecutorService;

/**
 * Class that provides several utility methods related to GreenDroid.
 * 
 * @author Cyril Mottier
 */
public class GDUtils {

    private GDUtils() {
    }


    public static AndroidApplication getGDApplication(Context context) {
        return (AndroidApplication) context.getApplicationContext();
    }


    public static ImageCache getImageCache(Context context) {
        return getGDApplication(context).getImageCache();
    }


    public static ExecutorService getExecutor(Context context) {
        return getGDApplication(context).getExecutor();
    }

}
