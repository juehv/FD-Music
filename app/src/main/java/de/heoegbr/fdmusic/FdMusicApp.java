package de.heoegbr.fdmusic;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.InputStreamReader;
import java.io.Reader;

import de.heoegbr.fdmusic.data.FormationData;
import de.heoegbr.fdmusic.data.FormationDataAdapter;
import de.heoegbr.fdmusic.data.MusicConstants;
import de.heoegbr.fdmusic.ui.MainActivity;

public class FdMusicApp extends Application {
    private static final String TAG = FdMusicApp.class.getName();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "On Create App");
    }
}
