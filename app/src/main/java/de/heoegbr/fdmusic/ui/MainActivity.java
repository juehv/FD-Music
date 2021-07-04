package de.heoegbr.fdmusic.ui;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import de.heoegbr.fdmusic.BuildConfig;
import de.heoegbr.fdmusic.R;
import de.heoegbr.fdmusic.data.EntryPoint;
import de.heoegbr.fdmusic.data.MusicConstants;
import de.heoegbr.fdmusic.player.SoundService;
import de.heoegbr.fdmusic.ui.imageplan.ImagePlanFragment;
import de.heoegbr.fdmusic.ui.setup.SetupActivity;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getName();

    private RecyclerView titleListRecyclerView;

    private Button pauseButton;
    private ToggleButton loopButton;
    private ToggleButton continueButton;
    private TextView speedValueText;
    private TextView leadTimeValueText;

    private SeekBar leadTimeSlider;
    private SeekBar speedSlider;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        currentTimeInMusic = 0;

        // don't build this gui if we go to setup wizard
        if (!isPermissionsGrandedAndSetupWizardCompleted(getApplicationContext())) {
            startActivity(new Intent(MainActivity.this, SetupActivity.class));
            return;
        }

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

        speedValueText = findViewById(R.id.speedValueText);
        speedValueText.setOnClickListener(view -> {
            speedSlider.setProgress(50);
        });
        speedSlider = findViewById(R.id.speedSlider);
        speedSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float speed = (((float) progress / 250.0f) + 0.8f);
                speedValueText.setText(String.format("%d %%", Math.round(speed * 100)));

                Intent propIntent = new Intent(getApplicationContext(), SoundService.class);
                propIntent.setAction(MusicConstants.ACTION.SPEED_CHANGE_ACTION);
                propIntent.putExtra(MusicConstants.KEY_EXTRA.SPEED, speed);
                PendingIntent lPendingPropIntent = PendingIntent.getService(getApplicationContext(), 2, propIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                try {
                    lPendingPropIntent.send();
                } catch (PendingIntent.CanceledException e) {
                    Log.e(TAG, e.getMessage());
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        leadTimeValueText = findViewById(R.id.leatTimeValueText);
        leadTimeValueText.setOnClickListener(view -> {
            leadTimeSlider.setProgress(25);
        });
        leadTimeSlider = findViewById(R.id.leadTimeSlider);
        leadTimeSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int leadTimeInSeconds = Math.round(progress / 5);
                leadTimeValueText.setText(String.format("%d s", leadTimeInSeconds));

                Intent propIntent = new Intent(getApplicationContext(), SoundService.class);
                propIntent.setAction(MusicConstants.ACTION.LT_CHANGE_ACTION);
                propIntent.putExtra(MusicConstants.KEY_EXTRA.LEAD_TIME, leadTimeInSeconds);
                PendingIntent lPendingPropIntent = PendingIntent.getService(getApplicationContext(), 1, propIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                try {
                    lPendingPropIntent.send();
                } catch (PendingIntent.CanceledException e) {
                    Log.e(TAG, e.getMessage());
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
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

        List<EntryPoint> musicEntryPoints;

        MusicViewAdapter(List<EntryPoint> musicEntryPoints) {
            this.musicEntryPoints = musicEntryPoints;
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
            holder.label.setText(musicEntryPoints.get(position).label);

            holder.itemView.setOnClickListener(v -> {
                Intent playIntent = new Intent(v.getContext(), SoundService.class);
                playIntent.setAction(MusicConstants.ACTION.PLAY_ACTION);
                playIntent.putExtra(MusicConstants.KEY_EXTRA.POSITION, position);
                PendingIntent lPendingPlayIntent = PendingIntent.getService(v.getContext(), 0, playIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                try {
                    lPendingPlayIntent.send();
                } catch (PendingIntent.CanceledException e) {
                    Log.e(TAG, e.getMessage());
                }

                // Start checking regularly for whether we need to update the image plan.
                // TODO: What is MUSIC_OFFSET for?
                int startTime = MusicConstants.MUSIC_ENTRY_POINTS.get(position).start;

                int leadTime = Math.round(leadTimeSlider.getProgress() / 5) * 1000;
                // The lead time can't be longer than the offset from start.
                leadTime = Math.min(leadTime, startTime);

                currentTimeInMusic = startTime - MusicConstants.MUSIC_OFFSET - leadTime;
                lastTime = System.currentTimeMillis();

                timer = new Timer();
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        float speed = (((float) speedSlider.getProgress() / 250.0f) + 0.8f);

                        long now = System.currentTimeMillis();
                        currentTimeInMusic = (int) (currentTimeInMusic + speed * (now - lastTime));
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