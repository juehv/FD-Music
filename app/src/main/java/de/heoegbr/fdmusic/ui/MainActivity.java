package de.heoegbr.fdmusic.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.xw.repo.BubbleSeekBar;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;

import de.heoegbr.fdmusic.BuildConfig;
import de.heoegbr.fdmusic.R;
import de.heoegbr.fdmusic.data.FormationData;
import de.heoegbr.fdmusic.data.FormationDataAdapter;
import de.heoegbr.fdmusic.data.LazyDatabase;
import de.heoegbr.fdmusic.data.MusicConstants;
import de.heoegbr.fdmusic.data.MusicEntryPoint;
import de.heoegbr.fdmusic.player.SoundService;
import de.heoegbr.fdmusic.ui.setup.SetupActivity;

/**
 * Main Activity of FDMusic
 *
 * @author Jens
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getName();

    // state constants for multi-button
    private static final class MULTI_BUTTON_STATE {
        public static final int CONTINUE = 2;
        public static final int LOOP = 1;
        public static final int OFF = 0;
        public static final int PASSAGE = 3;
    }

    // constants for pending intent request codes
    private static final class REQUEST_CODE {
        public static final int PLAY_REQUEST = 0;
        public static final int PAUSE_REQUEST = 1;
    }

    // UI elements
    private RecyclerView mTitleListRecyclerView;
    private ImageButton mPlayPauseButton;
    //    private ToggleButton loopButton;
    //    private ToggleButton continueButton;
    private ImageButton mLoopContinueButton;
    private TextView mSpeedValueText;
    private TextView mSpeedLabel;
    private TextView mLeadTimeValueText;
    private TextView mLeadTimeValueLabel;

    private BubbleSeekBar mSpeedSlider;
    private BubbleSeekBar mLeadTimeSlider;

    // GUI element state
    private static float sSpeed = 1.0f;
    private static int sLeadTimeInSeconds = 5;
    private static boolean sLoop = false;
    private static boolean sContinue = false;
    private static boolean sPassage = false;
    private static int sServiceState = SoundService.STATE_SERVICE.NOT_INIT;
    private static int sLoopContinueButtonState = MULTI_BUTTON_STATE.OFF;

    private static MusicViewAdapter sViewAdapter = null;


    /**
     * Checks for required permissions and if setup wizard was completed
     *
     * @param context
     * @return false if setup wizard should be started again
     */
    public static boolean isPermissionsGrandedAndSetupWizardCompleted(Context context) {
        boolean permission = context.checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
        boolean completed = PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(SetupActivity.SETUP_COMPLETE_KEY, 0) == BuildConfig.VERSION_CODE;
        return permission && completed;
    }

    /**
     * Initializes static data for app operation.
     *
     * @param context
     */
    private void initializeApp(Context context) {
        if (LazyDatabase.APP_INITIALIZED) {
            return;
        }

        // Load data (music + meta data)
        //TODO load from somewhere but not from resources
        InputStreamReader reader = new InputStreamReader(context.getResources().openRawResource(R.raw.meta));
        Gson gson = new GsonBuilder().registerTypeAdapter(FormationData.class, new FormationDataAdapter()).create();
        FormationData fData = gson.fromJson(reader, new TypeToken<FormationData>() {
        }.getType());

        // loading successful?
        if (fData == null) {
            // this should not happen ... (at least if data is prepared correctly)
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putInt(SetupActivity.SETUP_COMPLETE_KEY, 0).apply();

            // inform user
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage(R.string.main_nodatadialog_text)
                    .setTitle(R.string.main_nodatadialog_title)
                    .setPositiveButton(R.string.ok_button, (dialogInterface, i) -> {
                        finishAndRemoveTask();
                        System.exit(0);
                    });
            builder.create().show();
        }

        // save to (lazy in-memory) database
        LazyDatabase.FORMATION_DATA = fData;

        // initialize static variables
        sViewAdapter = new MusicViewAdapter(LazyDatabase.FORMATION_DATA.entryPoints);

        LazyDatabase.APP_INITIALIZED = true;
    }

    /**
     * attaches observers to state data of player background service
     */
    private void initializeDataObservers() {
        SoundService.liveServiceState.observe(this, serviceState -> {
            Log.d(TAG, "Update service state.");
            sServiceState = serviceState;
            updatePlayPauseButton();
        });
        SoundService.livePlayerPositionInTime.observe(this, playerPosition -> {
            Log.d(TAG, "update:" + playerPosition);
        });
    }

    // helper function to set icon in play/pause button
    private void updatePlayPauseButton() {
        if (sServiceState == SoundService.STATE_SERVICE.PLAY) {
            mPlayPauseButton.setImageResource(R.drawable.ic_pause_24);
        } else {
            mPlayPauseButton.setImageResource(R.drawable.ic_play_arrow_24);
        }
    }

    private void updateLoopContinueButton() {
        switch (sLoopContinueButtonState) {
            case MULTI_BUTTON_STATE.LOOP:
                mLoopContinueButton.setImageResource(R.drawable.ic_loop_purple_24);
                sLoop = true;
                sContinue = false;
                sPassage = false;
                sViewAdapter.setCheckboxVisibility(false);
                break;
            case MULTI_BUTTON_STATE.CONTINUE:
                mLoopContinueButton.setImageResource(R.drawable.ic_arrow_right_purple_24);
                sLoop = false;
                sContinue = true;
                sPassage = false;
                sViewAdapter.setCheckboxVisibility(false);
                break;
            case MULTI_BUTTON_STATE.PASSAGE:
                mLoopContinueButton.setImageResource(R.drawable.ic_boxes_purple_24);
                sLoop = false;
                sContinue = false;
                sPassage = true;
                sViewAdapter.setCheckboxVisibility(true);
                break;
            case MULTI_BUTTON_STATE.OFF:
            default:
                sLoopContinueButtonState = MULTI_BUTTON_STATE.OFF;
                mLoopContinueButton.setImageResource(R.drawable.ic_loop_24);
                sLoop = false;
                sContinue = false;
                sPassage = false;
                sViewAdapter.setCheckboxVisibility(false);
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // remove all observers
        SoundService.liveServiceState.removeObservers(this);
        SoundService.livePlayerPositionInTime.removeObservers(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // don't build this gui if we go to setup wizard
        if (!isPermissionsGrandedAndSetupWizardCompleted(getApplicationContext())) {
            startActivity(new Intent(MainActivity.this, SetupActivity.class));
            return;
        }

        initializeApp(getApplicationContext());

        // start building GUI
        setContentView(R.layout.activity_main);

        mTitleListRecyclerView = findViewById(R.id.titlesRecyclerView);
        LinearLayoutManager llm = new LinearLayoutManager(getApplicationContext());
        mTitleListRecyclerView.setLayoutManager(llm);
        mTitleListRecyclerView.setAdapter(sViewAdapter);

        mPlayPauseButton = findViewById(R.id.stopButton);
        mPlayPauseButton.setOnClickListener(v -> {
            Intent tmpIntent;
            if (sServiceState == SoundService.STATE_SERVICE.PLAY) {
                tmpIntent = new Intent(v.getContext(), SoundService.class);
                tmpIntent.setAction(MusicConstants.ACTION.PAUSE_ACTION);
                sendPendingIntent(v.getContext(), tmpIntent, REQUEST_CODE.PAUSE_REQUEST);
            } else {
                tmpIntent = buildPlayIntent(v.getContext(), -1);
                sendPendingIntent(v.getContext(), tmpIntent, REQUEST_CODE.PLAY_REQUEST);
            }
        });
        updatePlayPauseButton();

        mLoopContinueButton = findViewById(R.id.loopContinueButton);
        mLoopContinueButton.setOnClickListener(view -> {
            sLoopContinueButtonState++;
            updateLoopContinueButton();

            SoundService.liveLoop.setValue(sLoop);
            SoundService.liveContinue.setValue(sContinue);
            SoundService.livePassage.setValue(sPassage);
        });
        updateLoopContinueButton(); // restore button state

        mSpeedValueText = findViewById(R.id.speedValueText);
        mSpeedValueText.setOnClickListener(view -> {
            mSpeedSlider.setProgress(100);
            SoundService.liveSpeed.setValue(1.0f);
        });
        mSpeedLabel = findViewById(R.id.speedLabel);
        mSpeedLabel.setOnClickListener(view -> {
            mSpeedSlider.setProgress(100);
            SoundService.liveSpeed.setValue(1.0f);
        });
        mSpeedSlider = findViewById(R.id.speedSlider);
        mSpeedSlider.setOnProgressChangedListener(new BubbleSeekBar.OnProgressChangedListener() {
            @Override
            public void onProgressChanged(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat, boolean fromUser) {
                sSpeed = ((float) progress / 100.0f); // (((float) progress / 250.0f) + 0.8f);
                mSpeedValueText.setText(String.format("%d %%", Math.round(sSpeed * 100)));
            }

            @Override
            public void getProgressOnActionUp(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {
                Log.i(TAG, "Sending Speed Intent");
                SoundService.liveSpeed.setValue(sSpeed);
            }

            @Override
            public void getProgressOnFinally(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat, boolean fromUser) {

            }
        });
        mSpeedSlider.setProgress(Math.round(sSpeed * 100.0f)); // set restored value

        mLeadTimeValueText = findViewById(R.id.leatTimeValueText);
        mLeadTimeValueText.setOnClickListener(view -> {
            mLeadTimeSlider.setProgress(5);
            SoundService.liveLeadTime.setValue(5);
        });
        mLeadTimeValueLabel = findViewById(R.id.leadTimeLabel);
        mLeadTimeValueLabel.setOnClickListener(view -> {
            mLeadTimeSlider.setProgress(5);
            SoundService.liveLeadTime.setValue(5);
        });
        mLeadTimeSlider = findViewById(R.id.leadTimeSlider);
        mLeadTimeSlider.setOnProgressChangedListener(new BubbleSeekBar.OnProgressChangedListener() {

            @Override
            public void onProgressChanged(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat, boolean fromUser) {
                sLeadTimeInSeconds = progress; // Math.round(progress / 5);
                mLeadTimeValueText.setText(String.format("%d s", sLeadTimeInSeconds));
            }

            @Override
            public void getProgressOnActionUp(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {
                SoundService.liveLeadTime.setValue(sLeadTimeInSeconds);
            }

            @Override
            public void getProgressOnFinally(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat, boolean fromUser) {

            }
        });
        mLeadTimeSlider.setProgress(sLeadTimeInSeconds); // set restored value
    }

    @Override
    protected void onResume() {
        super.onResume();

        // synchronize data with background service
        initializeDataObservers();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //TODO kill foreground service ? Or is OnDestroy also called while foreground service is running intentionally...
    }

    // helper function to build a play intent with complete configuration set
    private Intent buildPlayIntent(Context context, int position) {
        Intent playIntent = new Intent(context, SoundService.class);
        playIntent.setAction(MusicConstants.ACTION.PLAY_ACTION);
        playIntent.putExtra(MusicConstants.KEY_EXTRA.POSITION, position);
        playIntent.putExtra(MusicConstants.KEY_EXTRA.CONTINUE, sLoopContinueButtonState == MULTI_BUTTON_STATE.CONTINUE);
        playIntent.putExtra(MusicConstants.KEY_EXTRA.LOOP, sLoopContinueButtonState == MULTI_BUTTON_STATE.LOOP);
        playIntent.putExtra(MusicConstants.KEY_EXTRA.PASSAGE, sLoopContinueButtonState == MULTI_BUTTON_STATE.PASSAGE);
        playIntent.putExtra(MusicConstants.KEY_EXTRA.SPEED, sSpeed);
        playIntent.putExtra(MusicConstants.KEY_EXTRA.LEAD_TIME, sLeadTimeInSeconds);

        return playIntent;
    }

    // helper function to send a pending intent to foreground service
    private void sendPendingIntent(Context context, Intent intent, int requestCode) {
        PendingIntent lPendingPlayIntent = PendingIntent.getService(context, requestCode,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        try {
            lPendingPlayIntent.send();
        } catch (PendingIntent.CanceledException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    // adapter code for card views in recycler view ...duh!
    public class MusicViewAdapter extends RecyclerView.Adapter<MusicEntryPointViewHolder> {

        List<MusicEntryPoint> mMusicMusicEntryPoints;
        HashMap<Integer, MusicEntryPointViewHolder> mViewHolders = new HashMap();
        int mCheckedPositionMin = -1;
        int mCheckedPositionMax = -1;

        MusicViewAdapter(List<MusicEntryPoint> mMusicMusicEntryPoints) {
            this.mMusicMusicEntryPoints = mMusicMusicEntryPoints;
        }

        @NonNull
        @Override
        public MusicEntryPointViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_music_entry_point, parent, false);
            MusicEntryPointViewHolder pvh = new MusicEntryPointViewHolder(v);
            return pvh;
        }

        @Override
        public void onBindViewHolder(@NonNull MusicEntryPointViewHolder holder, int position) {
            holder.label.setText(mMusicMusicEntryPoints.get(position).label);
            holder.position = position;

            holder.itemView.setOnClickListener(v -> {
                // start player
                Intent playIntent = buildPlayIntent(v.getContext(), position);
                sendPendingIntent(v.getContext(), playIntent, REQUEST_CODE.PLAY_REQUEST);

                // mark position for user
                for (Integer key : mViewHolders.keySet()) {
                    mViewHolders.get(key).resetAppearance();
                }
                holder.appearPlaying();
            });

            if (sLoopContinueButtonState == MULTI_BUTTON_STATE.PASSAGE &&
                position > 1) {
                holder.checkBox.setVisibility(View.VISIBLE);
                updateCheckboxState(position);
            }

            mViewHolders.put(position, holder);
        }



        @Override
        public int getItemCount() {
            return mMusicMusicEntryPoints.size();
        }

        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
        }

        @Override
        public void onViewAttachedToWindow(@NonNull MusicEntryPointViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            updateAllCheckboxStates(); //sets checkbox state after cardview is built
        }

        void setCheckboxVisibility(boolean visibleBoolean) {
            int visibility = visibleBoolean ? View.VISIBLE : View.INVISIBLE;
            for (Integer key : mViewHolders.keySet()) {
                if (key < 2) continue;
                mViewHolders.get(key).checkBox.setVisibility(visibility);
            }
            if (visibleBoolean) {
                updateAllCheckboxStates();
            }
        }

        // helper functions to generate connected passages
        void updateAllCheckboxStates() {
            for (Integer key : mViewHolders.keySet()) {
                updateCheckboxState(key);
            }
        }

        void updateCheckboxState(int position){
            try {
                if ((position >= mCheckedPositionMin && mCheckedPositionMin > 0)
                        && (position <= mCheckedPositionMax && mCheckedPositionMax > 0)) {
                    mViewHolders.get(position).checkBox.setChecked(true);
                } else {
                    mViewHolders.get(position).checkBox.setChecked(false);
                }
            } catch (NullPointerException ignore){}
        }

        void setPositionChecked(int position) {
            if (mCheckedPositionMin < 0
                    || mCheckedPositionMin > position) {
                mCheckedPositionMin = position;
            }
            if (mCheckedPositionMax < 0
                    || mCheckedPositionMax < position) {
                mCheckedPositionMax = position;
            }
            updateAllCheckboxStates();
            SoundService.livePassageData.setValue(new Pair<>(mCheckedPositionMin, mCheckedPositionMax));
        }

        void setPositionUnchecked(int position) {
            if (position == mCheckedPositionMin) {
                mCheckedPositionMin = -1;
                mCheckedPositionMax = -1;
            } else if (position < mCheckedPositionMin) {
                mCheckedPositionMin = position;
            } else {
                mCheckedPositionMax = position - 1;
            }
            updateAllCheckboxStates();
            SoundService.livePassageData.setValue(new Pair<>(mCheckedPositionMin, mCheckedPositionMax));
        }
        // end
    }

    public class MusicEntryPointViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView label;
        CheckBox checkBox;
        int position;

        public MusicEntryPointViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.musicEntryPointCardView);
            label = itemView.findViewById(R.id.musicEntryPointLabel);
            checkBox = itemView.findViewById(R.id.musicEntryPointCheckBox);
            checkBox.setOnClickListener(view -> {
                if (checkBox.isChecked()) {
                    sViewAdapter.setPositionChecked(position);
                } else {
                    sViewAdapter.setPositionUnchecked(position);
                }
            });
        }

        // resets list appearance to clear playing cards
        public void resetAppearance() {
            // TODO fix this (use color from a theme)
            cardView.setCardBackgroundColor(getResources().getColor(R.color.light_grey));
        }

        // marks one card to indicate playing entry
        public void appearPlaying() {
            // TODO fix this
            cardView.setCardBackgroundColor(getResources().getColor(R.color.teal_700));
        }
    }
}