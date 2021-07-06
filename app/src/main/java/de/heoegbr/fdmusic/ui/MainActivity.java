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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import de.heoegbr.fdmusic.data.MusicConstants;
import de.heoegbr.fdmusic.data.MusicEntryPoint;
import de.heoegbr.fdmusic.player.SoundService;
import de.heoegbr.fdmusic.ui.setup.SetupActivity;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getName();

    private RecyclerView titleListRecyclerView;

    private static final int MULTI_BUTTON_STATE_OFF = 0;
    private static final int MULTI_BUTTON_STATE_LOOP = 1;
    private static final int MULTI_BUTTON_STATE_CONTINUE = 2;
    private static final int MULTI_BUTTON_STATE_PASSAGE = 3;

    private ImageButton pauseButton;
    //    private ToggleButton loopButton;
//    private ToggleButton continueButton;
    private ImageButton loopContinueButton;
    private int loopContinueButtonState = MULTI_BUTTON_STATE_OFF;
    private TextView speedValueText;
    private TextView speedLabel;
    private TextView leadTimeValueText;
    private TextView leadTimeValueLabel;

    private BubbleSeekBar speedSlider;
    private BubbleSeekBar leadTimeSlider;

    private float mSpeed = 1.0f;
    private int mLeadTimeInSeconds = 5;
    private boolean mLoop = false;
    private boolean mContinue = false;
    private boolean mPassage = false;


    public static boolean isPermissionsGrandedAndSetupWizardCompleted(Context context) {
        boolean permission = context.checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
        boolean completed = PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(SetupActivity.SETUP_COMPLETE_KEY, 0) == BuildConfig.VERSION_CODE;
        return permission && completed;
    }

    public void initializeApp(Context context) {
        if (MusicConstants.APP_INITIALIZED) {
            return;
        }

        //TODO load from somewhere but not from ressources
        InputStreamReader reader = new InputStreamReader(context.getResources().openRawResource(R.raw.meta));
        Gson gson = new GsonBuilder().registerTypeAdapter(FormationData.class, new FormationDataAdapter()).create();
        FormationData fData = gson.fromJson(reader, new TypeToken<FormationData>() {
        }.getType());

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

        // todo find cleaner solution
        MusicConstants.MUSIC_ENTRY_POINTS = fData.entryPoints;
        MusicConstants.FORMATION_DATA = fData;
        MusicConstants.APP_INITIALIZED = true;
    }

    @Override
    protected void onPause() {
        super.onPause();

        // backup player configuration
        MusicConstants.TMP_BACKUP_LEAD_TIME = mLeadTimeInSeconds;
        MusicConstants.TMP_BACKUP_SPEED = mSpeed;
        MusicConstants.TMP_BACKUP_LOOP = mLoop ;
        MusicConstants.TMP_BACKUP_CONTINUE = mContinue ;
        MusicConstants.TMP_BACKUP_PASSAGE = mPassage;
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

        mSpeed = MusicConstants.TMP_BACKUP_SPEED;
        mLeadTimeInSeconds = MusicConstants.TMP_BACKUP_LEAD_TIME;
        mLoop =  MusicConstants.TMP_BACKUP_LOOP;
        mContinue =  MusicConstants.TMP_BACKUP_CONTINUE;
        mPassage =  MusicConstants.TMP_BACKUP_PASSAGE;

        setContentView(R.layout.activity_main);

        titleListRecyclerView = findViewById(R.id.titlesRecyclerView);
        LinearLayoutManager llm = new LinearLayoutManager(getApplicationContext());
        titleListRecyclerView.setLayoutManager(llm);
        MusicViewAdapter mvAdapter = new MusicViewAdapter(MusicConstants.MUSIC_ENTRY_POINTS);
        titleListRecyclerView.setAdapter(mvAdapter);

        pauseButton = findViewById(R.id.stopButton);
        pauseButton.setOnClickListener(v -> {
            Intent stopIntent = new Intent(v.getContext(), SoundService.class);
            stopIntent.setAction(MusicConstants.ACTION.PAUSE_ACTION);
            PendingIntent pendingStopIntent = PendingIntent.getService(v.getContext(), 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            try {
                pendingStopIntent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        });

//        loopButton = findViewById(R.id.loopButton);
//        loopButton.setOnClickListener(view -> {
//            Intent propIntent = new Intent(view.getContext(), SoundService.class);
//            propIntent.setAction(MusicConstants.ACTION.LOOP_ACTION);
//            propIntent.putExtra(MusicConstants.KEY_EXTRA.LOOP, loopButton.isChecked());
//            PendingIntent lPendingPropIntent = PendingIntent.getService(getApplicationContext(), 4, propIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//            try {
//                lPendingPropIntent.send();
//            } catch (PendingIntent.CanceledException e) {
//                Log.e(TAG, e.getMessage());
//            }
//        });
//
//        continueButton = findViewById(R.id.continueButton);
//        continueButton.setOnClickListener(view -> {
//            Intent propIntent = new Intent(view.getContext(), SoundService.class);
//            propIntent.setAction(MusicConstants.ACTION.CONTINUE_ACTION);
//            propIntent.putExtra(MusicConstants.KEY_EXTRA.CONTINUE, continueButton.isChecked());
//            PendingIntent lPendingPropIntent = PendingIntent.getService(getApplicationContext(), 3, propIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//            try {
//                lPendingPropIntent.send();
//            } catch (PendingIntent.CanceledException e) {
//                Log.e(TAG, e.getMessage());
//            }
//        });
        loopContinueButton = findViewById(R.id.loopContinueButton);
        loopContinueButton.setOnClickListener(view -> {
            switch (loopContinueButtonState){
                case MULTI_BUTTON_STATE_OFF:
                    loopContinueButtonState = MULTI_BUTTON_STATE_LOOP;
                    loopContinueButton.setImageResource(R.drawable.ic_loop_purple_24);
                    mLoop = true;
                    mContinue = false;
                    mPassage = false;
                    break;
                case MULTI_BUTTON_STATE_LOOP:
                    loopContinueButtonState = MULTI_BUTTON_STATE_CONTINUE;
                    loopContinueButton.setImageResource(R.drawable.ic_arrow_right_purple_24);
                    mLoop = false;
                    mContinue = true;
                    mPassage = false;
                    break;
                case MULTI_BUTTON_STATE_CONTINUE:
                    loopContinueButtonState = MULTI_BUTTON_STATE_PASSAGE;
                    loopContinueButton.setImageResource(R.drawable.ic_boxes_purple_24);
                    mLoop = false;
                    mContinue = false;
                    mPassage = true;
                    break;
                case MULTI_BUTTON_STATE_PASSAGE:
                    loopContinueButtonState = MULTI_BUTTON_STATE_OFF;
                    loopContinueButton.setImageResource(R.drawable.ic_loop_24);
                    mLoop = false;
                    mContinue = false;
                    mPassage = false;
                    break;
                default:
                    loopContinueButtonState = MULTI_BUTTON_STATE_OFF;
                    loopContinueButton.setImageResource(R.drawable.ic_loop_24);
                    mLoop = false;
                    mContinue = false;
                    mPassage = false;
                    break;
            }

            Intent playIntent = new Intent(view.getContext(), SoundService.class);
            playIntent.setAction(MusicConstants.ACTION.LOOP_CONTINUE_ACTION);
            playIntent.putExtra(MusicConstants.KEY_EXTRA.CONTINUE,
                    loopContinueButtonState == MULTI_BUTTON_STATE_CONTINUE);
            playIntent.putExtra(MusicConstants.KEY_EXTRA.LOOP,
                    loopContinueButtonState == MULTI_BUTTON_STATE_LOOP);
            playIntent.putExtra(MusicConstants.KEY_EXTRA.PASSAGE,
                    loopContinueButtonState == MULTI_BUTTON_STATE_PASSAGE);
            PendingIntent lPendingPlayIntent = PendingIntent.getService(view.getContext(), 0,
                    playIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            try {
                lPendingPlayIntent.send();
            } catch (PendingIntent.CanceledException e) {
                Log.e(TAG, e.getMessage());
            }
        });

        speedValueText = findViewById(R.id.speedValueText);
        speedValueText.setOnClickListener(view -> {
            speedSlider.setProgress(100);
        });
        speedLabel = findViewById(R.id.speedLabel);
        speedLabel.setOnClickListener(view -> {
            speedSlider.setProgress(100);
        });
        speedSlider = findViewById(R.id.speedSlider);
        speedSlider.setOnProgressChangedListener(new BubbleSeekBar.OnProgressChangedListener() {
            @Override
            public void onProgressChanged(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat, boolean fromUser) {
                mSpeed = ((float) progress / 100.0f); // (((float) progress / 250.0f) + 0.8f);
                speedValueText.setText(String.format("%d %%", Math.round(mSpeed * 100)));

            }

            @Override
            public void getProgressOnActionUp(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {
                Log.i(TAG, "Sending Speed Intent");
                Intent propIntent = new Intent(getApplicationContext(), SoundService.class);
                propIntent.setAction(MusicConstants.ACTION.SPEED_CHANGE_ACTION);
                propIntent.putExtra(MusicConstants.KEY_EXTRA.SPEED, mSpeed);
                PendingIntent lPendingPropIntent = PendingIntent.getService(getApplicationContext(), 2, propIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                try {
                    lPendingPropIntent.send();
                } catch (PendingIntent.CanceledException e) {
                    Log.e(TAG, e.getMessage());
                }
            }

            @Override
            public void getProgressOnFinally(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat, boolean fromUser) {

            }
        });
        speedSlider.setProgress(Math.round(mSpeed * 100.0f)); // set restored value

        leadTimeValueText = findViewById(R.id.leatTimeValueText);
        leadTimeValueText.setOnClickListener(view -> {
            leadTimeSlider.setProgress(5);
        });
        leadTimeValueLabel = findViewById(R.id.leadTimeLabel);
        leadTimeValueLabel.setOnClickListener(view -> {
            leadTimeSlider.setProgress(5);
        });
        leadTimeSlider = findViewById(R.id.leadTimeSlider);
        leadTimeSlider.setOnProgressChangedListener(new BubbleSeekBar.OnProgressChangedListener() {

            @Override
            public void onProgressChanged(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat, boolean fromUser) {
                mLeadTimeInSeconds = progress; // Math.round(progress / 5);
                leadTimeValueText.setText(String.format("%d s", mLeadTimeInSeconds));
            }

            @Override
            public void getProgressOnActionUp(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {
                Intent propIntent = new Intent(getApplicationContext(), SoundService.class);
                propIntent.setAction(MusicConstants.ACTION.LT_CHANGE_ACTION);
                propIntent.putExtra(MusicConstants.KEY_EXTRA.LEAD_TIME, mLeadTimeInSeconds);
                PendingIntent lPendingPropIntent = PendingIntent.getService(getApplicationContext(), 1, propIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                try {
                    lPendingPropIntent.send();
                } catch (PendingIntent.CanceledException e) {
                    Log.e(TAG, e.getMessage());
                }
            }

            @Override
            public void getProgressOnFinally(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat, boolean fromUser) {

            }
        });
        leadTimeSlider.setProgress(mLeadTimeInSeconds); // set restored value

        // syncronize data with background service
        SoundService.liveServiceState.observe(this, serviceState -> {
            if (serviceState == MusicConstants.STATE_SERVICE.PLAY){
                pauseButton.setImageResource(R.drawable.ic_pause_24);
            } else {
                pauseButton.setImageResource(R.drawable.ic_play_arrow_24);
            }
        });
        SoundService.liveMediaPlayerPlayingPosition.observe(this,integer -> {
            Log.d(TAG, "update:"+integer);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //TODO kill foreground service ? Or is OnDestroy also called while foreground service is running intentionally...
    }

    public class MusicViewAdapter extends RecyclerView.Adapter<MusicEntryPointViewHolder> {

        List<MusicEntryPoint> musicMusicEntryPoints;
        HashMap<Integer, MusicEntryPointViewHolder> viewHolders = new HashMap();

        MusicViewAdapter(List<MusicEntryPoint> musicMusicEntryPoints) {
            this.musicMusicEntryPoints = musicMusicEntryPoints;
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
            viewHolders.put(position, holder);
            holder.label.setText(musicMusicEntryPoints.get(position).label);

            holder.itemView.setOnClickListener(v -> {
                // start player
                Intent playIntent = new Intent(v.getContext(), SoundService.class);
                playIntent.setAction(MusicConstants.ACTION.PLAY_ACTION);
                playIntent.putExtra(MusicConstants.KEY_EXTRA.POSITION, position);
                playIntent.putExtra(MusicConstants.KEY_EXTRA.CONTINUE, loopContinueButtonState == MULTI_BUTTON_STATE_CONTINUE);
                playIntent.putExtra(MusicConstants.KEY_EXTRA.LOOP, loopContinueButtonState == MULTI_BUTTON_STATE_LOOP);
                playIntent.putExtra(MusicConstants.KEY_EXTRA.PASSAGE, loopContinueButtonState == MULTI_BUTTON_STATE_PASSAGE);
                playIntent.putExtra(MusicConstants.KEY_EXTRA.SPEED, mSpeed);
                playIntent.putExtra(MusicConstants.KEY_EXTRA.LEAD_TIME, mLeadTimeInSeconds);
                PendingIntent lPendingPlayIntent = PendingIntent.getService(v.getContext(), 0, playIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                try {
                    lPendingPlayIntent.send();
                } catch (PendingIntent.CanceledException e) {
                    Log.e(TAG, e.getMessage());
                }

                // mark position for user
                for (Integer key : viewHolders.keySet()){
                    viewHolders.get(key).resetAppearance();
                }
                holder.appearPlaying();
            });
        }

        @Override
        public int getItemCount() {
            return MusicConstants.MUSIC_ENTRY_POINTS.size();
        }

        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
        }
    }

    public class MusicEntryPointViewHolder extends RecyclerView.ViewHolder {
        CardView cv;
        TextView label;

        public MusicEntryPointViewHolder(@NonNull View itemView) {
            super(itemView);
            cv = itemView.findViewById(R.id.musicEntryPointCardView);
            label = itemView.findViewById(R.id.musicEntryPointLabel);
        }

        public void resetAppearance(){
            cv.setCardBackgroundColor(getResources().getColor(R.color.light_grey));
        }

        public void appearPlaying(){
            cv.setCardBackgroundColor(getResources().getColor(R.color.teal_700));
        }
    }
}