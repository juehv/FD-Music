package de.heoegbr.fdmusic.player;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleService;
import androidx.lifecycle.MutableLiveData;

import java.util.Timer;
import java.util.TimerTask;

import de.heoegbr.fdmusic.R;
import de.heoegbr.fdmusic.data.MusicConstants;
import de.heoegbr.fdmusic.ui.MainActivity;

/**
 * Foreground service for managing system media player.
 *
 * @author Jens
 */
public class SoundService extends LifecycleService implements MediaPlayer.OnErrorListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnSeekCompleteListener {
    private final static String TAG = SoundService.class.getSimpleName();

    // service state
    public static MutableLiveData<Integer> liveServiceState = new MutableLiveData<>();
    static {
        liveServiceState.postValue(MusicConstants.STATE_SERVICE.NOT_INIT);
    }
    private static int sServiceState = MusicConstants.STATE_SERVICE.NOT_INIT;

    // current track as position in entry point array
    public static MutableLiveData<Integer> livePlayingPosition = new MutableLiveData<>();
    static {
        livePlayingPosition.postValue(0);
    }
    private static int sPlayingPosition = 0;

    // playing position of the media player in milliseconds
    public static MutableLiveData<Integer> liveMediaPlayerPlayingPosition = new MutableLiveData<>();
    static {
        liveMediaPlayerPlayingPosition.postValue(0);
    }
    private Timer mUpdateMediaPlayerPlayingPositionTimer = new Timer(true);

    // loop current track
    private boolean mLoop = false;

    // playing speed
    public static MutableLiveData<Float> liveSpeed = new MutableLiveData<>();
    static {
        liveSpeed.postValue(1.0f);
    }
    private static float mSpeed = 1.0f;

    // lead time before track reaches entry point in music
    private int mLeadTime = 5;

    // continue after a "track" is finisehd (next entry point starts)
    private boolean mContinue = false;

    // indicate if player is in passage mode (playing a series of tracks)
    private boolean mPassage = false;


    private final Object mLock = new Object();
    private final Handler mHandler = new Handler();
    private MediaPlayer mPlayer;
    private NotificationManager mNotificationManager;
    private PowerManager.WakeLock mWakeLock;

    private Timer mFadeTimer = new Timer(true);
    private Timer mStopTimer = new Timer(true);

    private Handler mTimerUpdateHandler = new Handler();
    private Runnable mTimerUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            mNotificationManager.notify(MusicConstants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification(
                    MusicConstants.MUSIC_ENTRY_POINTS.get(sPlayingPosition).label
            ));
            mTimerUpdateHandler.postDelayed(this, MusicConstants.DELAY_UPDATE_NOTIFICATION_FOREGROUND_SERVICE);
        }
    };

    private Runnable mDelayedShutdown = () -> {
        destroyPlayer();
        unlockCPU();
        stopForeground(true);
        stopSelf();
    };

    public SoundService() {
        liveServiceState.observe(this,integer -> {
            sServiceState = integer;
        });
        liveSpeed.observe(this, aFloat -> {
            mSpeed = aFloat;
            if (sServiceState == MusicConstants.STATE_SERVICE.PLAY && mPlayer.isPlaying()){
                mPlayer.setPlaybackParams(mPlayer.getPlaybackParams().setSpeed(mSpeed));
            }
        });
    }

    @Override
    public IBinder onBind(Intent arg0) {
        super.onBind(arg0);
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(SoundService.class.getSimpleName(), "onCreate()");
        liveServiceState.postValue(MusicConstants.STATE_SERVICE.NOT_INIT);
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (intent == null || intent.getAction() == null) {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        // bootstrap player on first play command
        if (action == MusicConstants.ACTION.PLAY_ACTION &&
                sServiceState == MusicConstants.STATE_SERVICE.NOT_INIT) {
            action = MusicConstants.ACTION.START_ACTION;
        }

        int tmpPosition = intent.getIntExtra(MusicConstants.KEY_EXTRA.POSITION, -1);
        if (tmpPosition > -1) sPlayingPosition = tmpPosition;
        switch (action) {
            case MusicConstants.ACTION.START_ACTION:
                Log.i(TAG, "Received start Intent ");
                liveServiceState.postValue(MusicConstants.STATE_SERVICE.PREPARE);

                mSpeed = intent.getFloatExtra(MusicConstants.KEY_EXTRA.SPEED, 1f);
                mLeadTime = intent.getIntExtra(MusicConstants.KEY_EXTRA.LEAD_TIME, 5);
                mLoop = intent.getBooleanExtra(MusicConstants.KEY_EXTRA.LOOP, false);
                mContinue = intent.getBooleanExtra(MusicConstants.KEY_EXTRA.CONTINUE, false);

                startForeground(MusicConstants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification(
                        MusicConstants.MUSIC_ENTRY_POINTS.get(sPlayingPosition).label
                ));

                destroyPlayer();
                initPlayer();

                play(sPlayingPosition);
                break;

            case MusicConstants.ACTION.PLAY_ACTION:
                Log.i(TAG, "Clicked Play");
                liveServiceState.postValue(MusicConstants.STATE_SERVICE.PLAY);

                mNotificationManager.notify(MusicConstants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification(
                        MusicConstants.MUSIC_ENTRY_POINTS.get(sPlayingPosition).label
                ));

                destroyPlayer();
                initPlayer();

                play(sPlayingPosition);
                break;

            case MusicConstants.ACTION.PAUSE_ACTION:
                Log.i(TAG, "Clicked Pause");
                if (sServiceState == MusicConstants.STATE_SERVICE.PLAY ||
                        sServiceState == MusicConstants.STATE_SERVICE.PREPARE) {
                    liveServiceState.postValue(MusicConstants.STATE_SERVICE.PAUSE);

                    mNotificationManager.notify(MusicConstants.NOTIFICATION_ID_FOREGROUND_SERVICE,
                            prepareNotification(""));

                    destroyPlayer();

                    mHandler.postDelayed(mDelayedShutdown, MusicConstants.DELAY_SHUTDOWN_FOREGROUND_SERVICE);
                }

                break;


            case MusicConstants.ACTION.STOP_ACTION:
                Log.i(TAG, "Received Stop Intent");

                destroyPlayer();
                stopForeground(true);
                stopSelf();
                break;

            case MusicConstants.ACTION.SPEED_CHANGE_ACTION:
                Log.i(TAG, "Received speed change intent");

                mSpeed = intent.getFloatExtra(MusicConstants.KEY_EXTRA.SPEED, 1f);
                if (sServiceState == MusicConstants.STATE_SERVICE.PLAY) {
                    synchronized (mLock) {
                        mPlayer.setPlaybackParams(mPlayer.getPlaybackParams().setSpeed(mSpeed));
                    }
                }
                break;

            case MusicConstants.ACTION.LT_CHANGE_ACTION:
                Log.i(TAG, "Received lead time change intent");
                mLeadTime = intent.getIntExtra(MusicConstants.KEY_EXTRA.LEAD_TIME, 5);
                break;

//            case MusicConstants.ACTION.LOOP_ACTION:
//                Log.i(TAG, "Received loop change intent");
//                mLoop = intent.getBooleanExtra(MusicConstants.KEY_EXTRA.LOOP, false);
//                break;
//
//            case MusicConstants.ACTION.CONTINUE_ACTION:
//                Log.i(TAG, "Received continue change intent");
//                mContinue = intent.getBooleanExtra(MusicConstants.KEY_EXTRA.CONTINUE, false);
//                break;

            case MusicConstants.ACTION.LOOP_CONTINUE_ACTION:
                Log.i(TAG, "Received loop-continue change intent");
                mContinue = intent.getBooleanExtra(MusicConstants.KEY_EXTRA.CONTINUE, false);
                mLoop = intent.getBooleanExtra(MusicConstants.KEY_EXTRA.LOOP, false);
                mPassage = intent.getBooleanExtra(MusicConstants.KEY_EXTRA.PASSAGE, false);
                break;

            default:
                destroyPlayer();
                stopForeground(true);
                stopSelf();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        destroyPlayer();
        liveServiceState.postValue(MusicConstants.STATE_SERVICE.NOT_INIT);
        try {
            mTimerUpdateHandler.removeCallbacksAndMessages(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    private void destroyPlayer() {
        liveServiceState.postValue(MusicConstants.STATE_SERVICE.NOT_INIT);
        mHandler.postDelayed(mDelayedShutdown, MusicConstants.DELAY_SHUTDOWN_FOREGROUND_SERVICE);

        if (mPlayer != null) {
            try {
                mPlayer.reset();
                mPlayer.release();
                Log.d(TAG, "Player destroyed");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mPlayer = null;
            }
        }
        unlockCPU();

        mUpdateMediaPlayerPlayingPositionTimer.cancel();
        mUpdateMediaPlayerPlayingPositionTimer.purge();
    }

    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.d(TAG, "Player onError() what:" + what);
        destroyPlayer();
        mHandler.postDelayed(mDelayedShutdown, MusicConstants.DELAY_SHUTDOWN_FOREGROUND_SERVICE);
        mNotificationManager.notify(MusicConstants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification("Error"));
        liveServiceState.postValue(MusicConstants.STATE_SERVICE.PAUSE);
        return false;
    }

    private void initPlayer() {
        mPlayer = MediaPlayer.create(this, R.raw.title1);
        mPlayer.setOnErrorListener(this);
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnSeekCompleteListener(this);
        //mPlayer.setOnBufferingUpdateListener(this);
        mPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                Log.d(TAG, "Player onInfo(), what:" + what + ", extra:" + extra);
                return false;
            }
        });

        liveMediaPlayerPlayingPosition.postValue(0);
        lockCPU();
    }

    private void play(int position) {
        try {
            mHandler.removeCallbacksAndMessages(null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        int from = MusicConstants.MUSIC_ENTRY_POINTS.get(position).start;

        synchronized (mLock) {
            try {
                if (mPlayer == null) {
                    initPlayer();
                }
//                mPlayer.reset();
//                mPlayer.setVolume(1.0f, 1.0f);
//                mPlayer.setDataSource(this, getResources(R.raw.title1).);
//                mPlayer.prepareAsync();
                int startPoint = from - (mLeadTime * 1000);
                startPoint = startPoint < 0 ? 0 : startPoint;
                mPlayer.seekTo(startPoint);
                liveMediaPlayerPlayingPosition.postValue(startPoint);


            } catch (Exception e) {
                destroyPlayer();
                e.printStackTrace();
            }
        }
    }

    private Notification prepareNotification(String message) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
                mNotificationManager.getNotificationChannel(MusicConstants.FOREGROUND_CHANNEL_ID) == null) {
            // The user-visible name of the channel.
            CharSequence name = getResources().getString(R.string.app_name);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(MusicConstants.FOREGROUND_CHANNEL_ID, name, importance);
            mChannel.setSound(null, null);
            mChannel.enableVibration(false);
            mNotificationManager.createNotificationChannel(mChannel);
        }
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(MusicConstants.ACTION.MAIN_ACTION);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent lPauseIntent = new Intent(this, SoundService.class);
        lPauseIntent.setAction(MusicConstants.ACTION.PAUSE_ACTION);
        PendingIntent lPendingPauseIntent = PendingIntent.getService(this, 0, lPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent playIntent = new Intent(this, SoundService.class);
        playIntent.setAction(MusicConstants.ACTION.PLAY_ACTION);
        PendingIntent lPendingPlayIntent = PendingIntent.getService(this, 0, playIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent lStopIntent = new Intent(this, SoundService.class);
        lStopIntent.setAction(MusicConstants.ACTION.STOP_ACTION);
        PendingIntent lPendingStopIntent = PendingIntent.getService(this, 0, lStopIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        RemoteViews lRemoteViews = new RemoteViews(getPackageName(), R.layout.notification_player);
        lRemoteViews.setOnClickPendingIntent(R.id.ui_notification_close_button, lPendingStopIntent);
        if (!message.isEmpty()) lRemoteViews.setTextViewText(R.id.notification_message, message);

        switch (sServiceState) {

            case MusicConstants.STATE_SERVICE.PAUSE:
                lRemoteViews.setViewVisibility(R.id.ui_notification_progress_bar, View.INVISIBLE);
                lRemoteViews.setOnClickPendingIntent(R.id.ui_notification_player_button, lPendingPlayIntent);
                lRemoteViews.setImageViewResource(R.id.ui_notification_player_button, R.drawable.ic_play_arrow_24);
                break;

            case MusicConstants.STATE_SERVICE.PLAY:
                lRemoteViews.setViewVisibility(R.id.ui_notification_progress_bar, View.INVISIBLE);
                lRemoteViews.setOnClickPendingIntent(R.id.ui_notification_player_button, lPendingPauseIntent);
                lRemoteViews.setImageViewResource(R.id.ui_notification_player_button, R.drawable.ic_pause_24);
                break;

            case MusicConstants.STATE_SERVICE.PREPARE:
                lRemoteViews.setViewVisibility(R.id.ui_notification_progress_bar, View.VISIBLE);
                lRemoteViews.setOnClickPendingIntent(R.id.ui_notification_player_button, lPendingPauseIntent);
                lRemoteViews.setImageViewResource(R.id.ui_notification_player_button, R.drawable.ic_pause_24);
                break;
        }

        NotificationCompat.Builder lNotificationBuilder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            lNotificationBuilder = new NotificationCompat.Builder(this, MusicConstants.FOREGROUND_CHANNEL_ID);
        } else {
            lNotificationBuilder = new NotificationCompat.Builder(this);
        }
        lNotificationBuilder
                .setContent(lRemoteViews)
                .setSmallIcon(R.drawable.ic_launcher_foreground) //FIXME doesn't look great
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            lNotificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        }
        return lNotificationBuilder.build();

    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "Player onPrepared()");
        liveServiceState.postValue(MusicConstants.STATE_SERVICE.PLAY);
        mNotificationManager.notify(MusicConstants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification(""));
        try {
            mPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //mPlayer.start();
        mTimerUpdateHandler.postDelayed(mTimerUpdateRunnable, 0);
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {
        Log.d(TAG, "Player onSeekComplete()");
        liveServiceState.postValue(MusicConstants.STATE_SERVICE.PLAY);
        mNotificationManager.notify(MusicConstants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification(""));
        try {
            mPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mPlayer.start();
        mPlayer.setPlaybackParams(mPlayer.getPlaybackParams().setSpeed(mSpeed));
        startFadeIn();
        mPlayer.setLooping(mLoop);

        int timerWakeDelay = Math.round(
                (MusicConstants.MUSIC_ENTRY_POINTS.get(sPlayingPosition).stop - mPlayer.getCurrentPosition())
                        / mSpeed);
        if (timerWakeDelay > 0) {
            mStopTimer.cancel();
            mStopTimer.purge();
            mStopTimer = new Timer(true);
            mStopTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        if (!mContinue) {
                            if (mLoop) {
                                startFadeOutAndPlayPosition(sPlayingPosition);
                            } else
                                startFadeOutAndPlayPosition(-1);
                        }
                    } catch (Exception ignored) {
                        Log.w(TAG, "Error while trying to pause player.", ignored);
                    }
                }
            }, timerWakeDelay);
        } else {
            destroyPlayer();
        }

        mTimerUpdateHandler.postDelayed(mTimerUpdateRunnable, 0);

        mUpdateMediaPlayerPlayingPositionTimer.cancel();
        mUpdateMediaPlayerPlayingPositionTimer.purge();
        mUpdateMediaPlayerPlayingPositionTimer = new Timer(true);
        mUpdateMediaPlayerPlayingPositionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (sServiceState == MusicConstants.STATE_SERVICE.PLAY
                        && mPlayer.isPlaying()){
                    liveMediaPlayerPlayingPosition.postValue(mPlayer.getCurrentPosition());
                }
            }
        }, 100,100);
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        Log.d(TAG, "Player onBufferingUpdate():" + percent);
    }

    private void lockCPU() {
        PowerManager mgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (mgr == null) {
            return;
        }
        mWakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getSimpleName());
        mWakeLock.acquire(18000000); // 5h
        Log.d(TAG, "Player lockCPU()");
    }

    private void unlockCPU() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
            mWakeLock = null;
            Log.d(TAG, "Player unlockCPU()");
        }
    }

    private static final int FADE_DURATION = 1000;
    private float volume;

    private void startFadeIn() {
        final int FADE_INTERVAL = 250;
        final int MAX_VOLUME = 1; //The volume will increase from 0 to 1
        int numberOfSteps = FADE_DURATION / FADE_INTERVAL; //Calculate the number of fade steps
        //Calculate by how much the volume changes each step
        final float deltaVolume = MAX_VOLUME / (float) numberOfSteps;

        volume = 0.0f;
        mPlayer.setVolume(volume, volume);
        //Create a new Timer and Timer task to run the fading outside the main UI thread
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    volume += deltaVolume;
                    mPlayer.setVolume(volume, volume);
                    //Cancel and Purge the Timer if the desired volume has been reached
                    if (volume >= 1f) {
                        mFadeTimer.cancel();
                        mFadeTimer.purge();
                    }
                } catch (Exception ignored) {
                    Log.w(this.getClass().getName(), "Mediaplayer issues", ignored);
                }
            }
        };
        mFadeTimer.cancel();
        mFadeTimer.purge();
        mFadeTimer = new Timer(true);
        mFadeTimer.schedule(timerTask, FADE_INTERVAL, FADE_INTERVAL);
    }

    /**
     * @param positionToPlayAfterFadeOut use <0 for no play after fading out
     */
    private void startFadeOutAndPlayPosition(int positionToPlayAfterFadeOut) {
        final int FADE_INTERVAL = 250;
        final int MAX_VOLUME = 1; //The volume will increase from 0 to 1
        int numberOfSteps = FADE_DURATION / FADE_INTERVAL; //Calculate the number of fade steps
        //Calculate by how much the volume changes each step
        final float deltaVolume = MAX_VOLUME / (float) numberOfSteps;

        volume = 1.0f;
        mPlayer.setVolume(volume, volume);
        //Create a new Timer and Timer task to run the fading outside the main UI thread
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    volume -= deltaVolume;
                    //Cancel and Purge the Timer if the desired volume has been reached
                    if (volume <= 0f) {
                        mPlayer.setVolume(0f, 0f);
                        mFadeTimer.cancel();
                        mFadeTimer.purge();
                        destroyPlayer();
                        if (positionToPlayAfterFadeOut >= 0) {
                            play(positionToPlayAfterFadeOut);
                        }
                        return;
                    }
                    mPlayer.setVolume(volume, volume);
                } catch (Exception ignored) {
                    Log.w(this.getClass().getName(), "Mediaplayer issues", ignored);
                }
            }
        };
        mFadeTimer.cancel();
        mFadeTimer.purge();
        mFadeTimer = new Timer(true);
        mFadeTimer.schedule(timerTask, FADE_INTERVAL, FADE_INTERVAL);
    }
}
