package de.heoegbr.fdmusic.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

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
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import de.heoegbr.fdmusic.BuildConfig;
import de.heoegbr.fdmusic.R;
import de.heoegbr.fdmusic.data.FormationData;
import de.heoegbr.fdmusic.data.FormationDataAdapter;
import de.heoegbr.fdmusic.data.MusicConstants;
import de.heoegbr.fdmusic.data.MusicEntryPoint;
import de.heoegbr.fdmusic.player.SoundService;
import de.heoegbr.fdmusic.ui.imageplan.ImagePlanFragment;
import de.heoegbr.fdmusic.ui.setup.SetupActivity;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getName();

    private RecyclerView titleListRecyclerView;

    private Button pauseButton;
    private ToggleButton loopButton;
    private ToggleButton continueButton;
    //private TextView speedValueText;
    private TextView speedLabel;
    private TextView leadTimeValueLabel;

    private BubbleSeekBar speedSlider;
    private BubbleSeekBar leadTimeSlider;

    private float mSpeed = 1.0f;
    private int mLeadTimeInSeconds = 5;

    Timer timer;
    int currentTimeInMusic;
    long lastTime;

    public static boolean isPermissionsGrandedAndSetupWizardCompleted(Context context) {
        boolean permission = context.checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
//                &&
//                context.checkSelfPermission(
//                        Manifest.permission.ACCESS_FINE_LOCATION)
//                        == PackageManager.PERMISSION_GRANTED
        boolean completed = PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(SetupActivity.SETUP_COMPLETE_KEY, 0) == BuildConfig.VERSION_CODE;
        return permission && completed;
    }

    public void initializeApp(Context context) {
        if (MusicConstants.APP_INITIALIZED){
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

        MusicConstants.TMP_BACKUP_LEAD_TIME = mLeadTimeInSeconds;
        MusicConstants.TMP_BACKUP_SPEED = mSpeed;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        currentTimeInMusic = 0;

        // don't build this gui if we go to setup wizard
        if (!isPermissionsGrandedAndSetupWizardCompleted(getApplicationContext())) {
            startActivity(new Intent(MainActivity.this, SetupActivity.class));
            return;
        }
        initializeApp(getApplicationContext());

        int orientation = getRequestedOrientation();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //Remove notification bar
            this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        mSpeed = MusicConstants.TMP_BACKUP_SPEED;
        mLeadTimeInSeconds = MusicConstants.TMP_BACKUP_LEAD_TIME;

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

            if (timer != null) {
                timer.cancel();
                timer = null;
            }
        });

        loopButton = findViewById(R.id.loopButton);
        loopButton.setOnClickListener(view -> {
            Intent propIntent = new Intent(view.getContext(), SoundService.class);
            propIntent.setAction(MusicConstants.ACTION.LOOP_ACTION);
            propIntent.putExtra(MusicConstants.KEY_EXTRA.LOOP, loopButton.isChecked());
            PendingIntent lPendingPropIntent = PendingIntent.getService(getApplicationContext(), 4, propIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            try {
                lPendingPropIntent.send();
            } catch (PendingIntent.CanceledException e) {
                Log.e(TAG, e.getMessage());
            }
        });

        continueButton = findViewById(R.id.continueButton);
        continueButton.setOnClickListener(view -> {
            Intent propIntent = new Intent(view.getContext(), SoundService.class);
            propIntent.setAction(MusicConstants.ACTION.CONTINUE_ACTION);
            propIntent.putExtra(MusicConstants.KEY_EXTRA.CONTINUE, continueButton.isChecked());
            PendingIntent lPendingPropIntent = PendingIntent.getService(getApplicationContext(), 3, propIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            try {
                lPendingPropIntent.send();
            } catch (PendingIntent.CanceledException e) {
                Log.e(TAG, e.getMessage());
            }
        });

//        speedValueText = findViewById(R.id.speedValueText);
//        speedValueText.setOnClickListener(view -> {
//            speedSlider.setProgress(50);
//        });
        speedLabel = findViewById(R.id.speedLabel);
        speedLabel.setOnClickListener(view -> {
            speedSlider.setProgress(100);
        });
        speedSlider = findViewById(R.id.speedSlider);
        speedSlider.setOnProgressChangedListener(new BubbleSeekBar.OnProgressChangedListener() {
            @Override
            public void onProgressChanged(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat, boolean fromUser) {
                mSpeed = ((float) progress / 100.0f); // (((float) progress / 250.0f) + 0.8f);
                //speedValueText.setText(String.format("%d %%", Math.round(mSpeed * 100)));

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

        leadTimeValueLabel = findViewById(R.id.leadTimeLabel);
        leadTimeValueLabel.setOnClickListener(view -> {
            leadTimeSlider.setProgress(5);
        });
        leadTimeSlider = findViewById(R.id.leadTimeSlider);
        leadTimeSlider.setOnProgressChangedListener(new BubbleSeekBar.OnProgressChangedListener(){

            @Override
            public void onProgressChanged(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat, boolean fromUser) {
                mLeadTimeInSeconds = progress; // Math.round(progress / 5);
                //leadTimeValueText.setText(String.format("%d s", mLeadTimeInSeconds));
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

        // fix moving bubble on rotate?
        speedSlider.correctOffsetWhenContainerOnScrolling();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //TODO kill foreground service ? Or is OnDestroy also called while foreground service is running intentionally...

        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public class MusicViewAdapter extends RecyclerView.Adapter<MusicEntryPointViewHolder> {

        List<MusicEntryPoint> musicMusicEntryPoints;

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
            holder.label.setText(musicMusicEntryPoints.get(position).label);

            holder.itemView.setOnClickListener(v -> {
                Intent playIntent = new Intent(v.getContext(), SoundService.class);
                playIntent.setAction(MusicConstants.ACTION.PLAY_ACTION);
                playIntent.putExtra(MusicConstants.KEY_EXTRA.POSITION, position);
                playIntent.putExtra(MusicConstants.KEY_EXTRA.CONTINUE, continueButton.isChecked());
                playIntent.putExtra(MusicConstants.KEY_EXTRA.LOOP, loopButton.isChecked());
                playIntent.putExtra(MusicConstants.KEY_EXTRA.SPEED, mSpeed);
                playIntent.putExtra(MusicConstants.KEY_EXTRA.LEAD_TIME, mLeadTimeInSeconds);
                PendingIntent lPendingPlayIntent = PendingIntent.getService(v.getContext(), 0, playIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                try {
                    lPendingPlayIntent.send();
                } catch (PendingIntent.CanceledException e) {
                    Log.e(TAG, e.getMessage());
                }

                // Start checking regularly for whether we need to update the image plan.
                // TODO: What is MUSIC_OFFSET for?
                int startTime = MusicConstants.MUSIC_ENTRY_POINTS.get(position).start;

                int leadTime = mLeadTimeInSeconds * 1000;
                // The lead time can't be longer than the offset from start.
                leadTime = Math.min(leadTime, startTime);

                currentTimeInMusic = startTime - MusicConstants.MUSIC_OFFSET - leadTime;
                lastTime = System.currentTimeMillis();

                timer = new Timer();
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        long now = System.currentTimeMillis();
                        currentTimeInMusic = (int) (currentTimeInMusic + mSpeed * (now - lastTime));
                        lastTime = now;

                        // Tell the fragment what time it is and let it update itself.
                        // TODO: It is also possible to go to the view directly...
                        //       ...but maybe that's not a good idea because we won't always be displaying it.
                        ImagePlanFragment fragment = (ImagePlanFragment) getFragmentManager().findFragmentById(R.id.imagePlanFragment);
                        fragment.setTimeInMusic(currentTimeInMusic);
                    }
                }, 0, 20);
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

    public static class MusicEntryPointViewHolder extends RecyclerView.ViewHolder {
        CardView cv;
        TextView label;

        public MusicEntryPointViewHolder(@NonNull View itemView) {
            super(itemView);
            cv = itemView.findViewById(R.id.cv);
            label = itemView.findViewById(R.id.ep_label);
        }
    }
}