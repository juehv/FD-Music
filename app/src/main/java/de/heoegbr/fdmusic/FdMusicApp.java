package de.heoegbr.fdmusic;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;

import de.heoegbr.fdmusic.ui.MainActivity;

public class FdMusicApp extends Application {
    private static final String TAG = FdMusicApp.class.getName();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "On Create");

        if (MainActivity.isPermissionsGrandedAndSetupWizardCompleted(getApplicationContext()))
            initializeApp(getApplicationContext());
    }

    public void initializeApp(Context context) {
        //TODO prepare sound files
        // dynamic loading of sound file and meta data for later
    }
}
