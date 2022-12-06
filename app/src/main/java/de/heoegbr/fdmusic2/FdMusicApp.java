package de.heoegbr.fdmusic2;

import android.app.Application;
import android.util.Log;

/**
 * Startup Entry point... Just in case we need it at some point
 *
 * @author Jens
 */
public class FdMusicApp extends Application {
    private static final String TAG = FdMusicApp.class.getName();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "On Create App");
    }
}
