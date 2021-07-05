package de.heoegbr.fdmusic.ui.setup;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.tbuonomo.viewpagerdotsindicator.DotsIndicator;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import de.heoegbr.fdmusic.BuildConfig;
import de.heoegbr.fdmusic.R;
import de.heoegbr.fdmusic.data.MusicConstants;
import de.heoegbr.fdmusic.ui.MainActivity;

//https://developer.android.com/training/animation/screen-slide
public class SetupActivity extends FragmentActivity {
    public static final String SETUP_COMPLETE_KEY = "setup_wizard_completed";
    public static final String SETUP_LICENCE_AGEED_KEY = "eula_agreed";
    public static final String SETUP_PAYWALL_KEY = "paywall";
    public static final String SETUP_DEMO_MODE_KEY = "demo_mode";
    private static final String TAG = "SETUP_WIZARD";

    // TODO move to viewmodel?
    private static final int NUM_PAGES = 4;
    public static final int SETUP_PAGE_WELCOME = 0;
    public static final int SETUP_PAGE_STORAGE = 1;
    public static final int SETUP_PAGE_PAYWALL = 2;
    public static final int SETUP_PAGE_DONE = 3;

//    public static final int SETUP_PAGE_LICENSE = 1;
//    public static final int SETUP_PAGE_CONTRIBUTION = 2;
//    public static final int SETUP_PAGE_STORAGE = 3;
//    public static final int SETUP_PAGE_LOCATION = 4;
//    public static final int SETUP_PAGE_CAMERA = 5;
//    public static final int SETUP_PAGE_DONE = 7;


    private ViewPager2 viewPager;
    private FragmentStateAdapter pagerAdapter;
    private DotsIndicator dotsIndicator;
    private Button finishButton;
    //    private Toolbar toolbar;
    private SharedPreferences prefs;
    //    private String[] viewPagerTitle;
//    private Boolean[] setupWizardStageCompleted;
//    private boolean wasDemoModeActiveInitially = false;
    private HashMap<Integer, Fragment> fragmentStorage = new HashMap<>();

    @Override
    public void onBackPressed() {
        if (viewPager != null) {
            int pageNo = viewPager.getCurrentItem() - 1;
            viewPager.setCurrentItem(pageNo, true);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO check if it is an update or initial installtion (based on version code from prefs)
        // TODO check if demo mode is active && new version --> show reinstallation screen
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_wizard);
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        viewPager = findViewById(R.id.pager);
        dotsIndicator = findViewById(R.id.setup_dots_indicator);
        finishButton = findViewById(R.id.setup_finish_button);
//        toolbar = findViewById(R.id.setup_toolbar);

        pagerAdapter = new ScreenSlidePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        dotsIndicator.setViewPager2(viewPager);

        // setup next button
//        // initialize from preferences
//        setupWizardStageCompleted = new Boolean[NUM_PAGES];
//        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//        setupWizardStageCompleted[SETUP_PAGE_WELCOME] = true; // welcome always enabled
//        setupWizardStageCompleted[SETUP_PAGE_STORAGE] =
//                (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                        == PackageManager.PERMISSION_GRANTED);
//        setupWizardStageCompleted[SETUP_PAGE_PAYWALL] =
//                !prefs.getString(SETUP_PAYWALL_KEY, "").isEmpty();
//        setupWizardStageCompleted[SETUP_PAGE_DONE] = true; // finish screen


        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                if (position == NUM_PAGES - 1) {
                    finishButton.setVisibility(View.VISIBLE);
                } else {
                    finishButton.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    public void finishButtonListener(View view) {
        // --> finish button mode
        Context context = getApplicationContext();
        // check permissions
        if ((context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
//                    || (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
//                    != PackageManager.PERMISSION_GRANTED)
//                    || (context.checkSelfPermission(Manifest.permission.CAMERA)
//                    != PackageManager.PERMISSION_GRANTED)
        ) {
            // inform user that he has to give permissions to finish
            AlertDialog.Builder builder = new AlertDialog.Builder(SetupActivity.this);
            builder.setMessage(R.string.setupwizard_error_permission_missing_text)
                    .setTitle(R.string.setupwizard_error_permission_missing_title)
                    .setPositiveButton(R.string.ok_button, null);
            builder.create().show();
//            } else if (!prefs.getBoolean(SETUP_LICENCE_AGEED_KEY, false)) {
//                // inform user
//                AlertDialog.Builder builder = new AlertDialog.Builder(SetupActivity.this);
//                builder.setMessage(R.string.setupwizard_error_license_missing_text)
//                        .setTitle(R.string.setupwizard_error_license_missing_title)
//                        .setPositiveButton(R.string.ok_button, null);
//                builder.create().show();
        } else if (!prefs.getBoolean(SETUP_PAYWALL_KEY, false)) {
            // inform user
            AlertDialog.Builder builder = new AlertDialog.Builder(SetupActivity.this);
            builder.setMessage(R.string.setupwizard_error_paywall_text)
                    .setTitle(R.string.setupwizard_error_paywall_title)
                    .setPositiveButton(R.string.ok_button, null);
            builder.create().show();
        } else {
            // finish setup wizard
            prefs.edit().putInt(SetupActivity.SETUP_COMPLETE_KEY, BuildConfig.VERSION_CODE).apply();

            // go to main activity
            startActivity(new Intent(SetupActivity.this, MainActivity.class));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case SETUP_PAGE_STORAGE:
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                } else {
                    // reset slider of request
                    Fragment wizardPage = fragmentStorage.get(requestCode);
                    if (wizardPage instanceof SetupWelcomeFragment) {
                        ((SetupWelcomeFragment) wizardPage).resetSwitch();
                    }
                }
                return;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private class ScreenSlidePagerAdapter extends FragmentStateAdapter {
        private Executor executor = Executors.newSingleThreadExecutor();

        public ScreenSlidePagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @Override
        public Fragment createFragment(int position) {
            Context applicationContext = getApplicationContext();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
            //Log.e(TAG, "called");
            Fragment returnFragment = null;
            switch (position) {
                case SETUP_PAGE_WELCOME: // welcome
                    returnFragment = new SetupWelcomeFragment(
                            R.string.setupwizard_welcome_text,
                            false, 0, null, false,
                            false, null);
                    break;
//                case SETUP_PAGE_LICENSE: // License
//                    returnFragment = new SetupWelcomeFragment(
//                            R.string.setupwizard_licenseagreement_text,
//                            true,
//                            R.string.i_understand_and_agree,
//                            (compoundButton, b) -> {
//                                PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
//                                        .edit().putBoolean(SETUP_LICENCE_AGEED_KEY, b).apply();
//                                setupWizardStageCompleted[1] = b;
//                            },
//                            prefs.getBoolean(SETUP_LICENCE_AGEED_KEY, false));
//                    break;
                case SETUP_PAGE_STORAGE: // Ask for storage permission
                    boolean storagePermission = applicationContext.checkSelfPermission(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_GRANTED;

                    returnFragment = new SetupWelcomeFragment(
                            R.string.setupwizard_storagepermission_text,
                            true,
                            R.string.permission,
                            (compoundButton, b) -> {
                                // ask for permission and reset if failed
                                if (b) {
                                    // check for permission
                                    if (applicationContext != null
                                            && (applicationContext.checkSelfPermission(
                                            Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                            != PackageManager.PERMISSION_GRANTED)) {
                                        // ask for permission, rationale already shown
                                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                SETUP_PAGE_STORAGE);
                                    }
                                }
                            }, storagePermission, false, null);
                    break;

                case SETUP_PAGE_PAYWALL: // thank you
                    MessageDigest md = null;
                    try {
                        md = MessageDigest.getInstance("SHA-256");
                    } catch (NoSuchAlgorithmException ignored) {
                    }
                    MessageDigest finalMd = md;

                    returnFragment = new SetupWelcomeFragment(
                            R.string.setupwizard_paywall_text,
                            false, 0, null, false,
                            true, new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                        }

                        @Override
                        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                        }

                        @Override
                        public void afterTextChanged(Editable editable) {
                            // check if passwort is ok
                            if (finalMd != null) {
                                finalMd.update(editable.toString().getBytes());
                                byte[] digest = finalMd.digest();
                                String hexPhrase = bytesToHex(digest);
                                Log.i(TAG, hexPhrase);
                                if (hexPhrase.equalsIgnoreCase(MusicConstants.PASSPHRASE)) {
                                    prefs.edit().putBoolean(SETUP_PAYWALL_KEY, true).apply();
                                    Log.i(TAG, "PW confirmed");
                                    MusicConstants.THIS_IS_A_HACK = true;
                                } else {
                                    prefs.edit().putBoolean(SETUP_PAYWALL_KEY, false).apply();
                                    Log.i(TAG, "PW NOT confirmed");
                                    MusicConstants.THIS_IS_A_HACK = false;
                                }
                            }
                        }
                    });
                    break;

                case SETUP_PAGE_DONE: // thank you
                    returnFragment = new SetupWelcomeFragment(
                            R.string.setupwizard_thankyou_text,
                            false, 0, null, false,
                            false, null);
                    break;

                default:
                    throw new IllegalStateException("Unexpected value: " + position);
            }
            if (returnFragment != null) {
                fragmentStorage.put(position, returnFragment);
            }
            return returnFragment;

        }


        @Override
        public int getItemCount() {
            return NUM_PAGES;
        }


    }

    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);

    public static String bytesToHex(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }

}
