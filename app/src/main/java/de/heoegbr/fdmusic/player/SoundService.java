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
import android.util.Pair;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleService;
import androidx.lifecycle.MutableLiveData;

import java.util.Timer;
import java.util.TimerTask;

import de.heoegbr.fdmusic.R;
import de.heoegbr.fdmusic.data.LazyDatabase;
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

    // constants for service state
    public static final class STATE_SERVICE {
        public static final int PREPARE = 30;
        public static final int PLAY = 20;
        public static final int PAUSE = 10;
        public static final int NOT_INIT = 0;
    }

    // service state
    public static MutableLiveData<Integer> liveServiceState = new MutableLiveData<>();

    static {
        liveServiceState.postValue(STATE_SERVICE.NOT_INIT);
    }

    private static int sServiceState = STATE_SERVICE.NOT_INIT;

    // current track as position in entry point array
    public static MutableLiveData<Integer> livePlayingPosition = new MutableLiveData<>();

    static {
        livePlayingPosition.postValue(0);
    }

    private static int sPlayingPosition = 0;

    // playing position of the media player in milliseconds
    public static MutableLiveData<Integer> livePlayerPositionInTime = new MutableLiveData<>();

    static {
        livePlayerPositionInTime.postValue(0);
    }

    private Timer mUpdatePlayerPositionInTimeTimer = new Timer(true);

    // playing speed
    public static MutableLiveData<Float> liveSpeed = new MutableLiveData<>();

    static {
        liveSpeed.postValue(1.0f);
    }

    private static float sSpeed = 1.0f;

    // loop current track
    public static MutableLiveData<Boolean> liveLoop = new MutableLiveData<>();

    static {
        liveLoop.postValue(false);
    }

    private static boolean sLoop = false;

    // lead time before track reaches entry point in music
    public static MutableLiveData<Integer> liveLeadTime = new MutableLiveData<>();

    static {
        liveLeadTime.postValue(5);
    }

    private static int sLeadTime = 5;

    // continue after a "track" is finisehd (next entry point starts)
    public static MutableLiveData<Boolean> liveContinue = new MutableLiveData<>();

    static {
        liveContinue.postValue(false);
    }

    private static boolean sContinue = false;

    // indicate if player is in passage mode (playing a series of tracks)
    public static MutableLiveData<Boolean> livePassage = new MutableLiveData<>();

    static {
        livePassage.postValue(false);
    }

    private static boolean sPassage = false;

    // positions for start and end a passage
    public static MutableLiveData<Pair<Integer, Integer>> livePassageData = new MutableLiveData<>();

    static {
        livePassageData.postValue(new Pair<>(-1, -1));
    }

    private static Pair<Integer, Integer> sPassageData;


    //TODO
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
            //TODO
            mNotificationManager.notify(MusicConstants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification(
                    LazyDatabase.FORMATION_DATA.entryPoints.get(sPlayingPosition).label
            ));
            mTimerUpdateHandler.postDelayed(this, MusicConstants.DELAY_UPDATE_NOTIFICATION_FOREGROUND_SERVICE);
        }
    };

    private Runnable mDelayedShutdown = () -> {
        //TODO
        destroyPlayer();
        unlockCPU();
        stopForeground(true);
        stopSelf();
    };

    public SoundService() {
        //TODO
        liveServiceState.observe(this, state -> {
            sServiceState = state;
        });
        livePlayingPosition.observe(this, postition -> {
            sPlayingPosition = postition;
        });
        liveSpeed.observe(this, speed -> {
            sSpeed = speed;
            if (sServiceState == STATE_SERVICE.PLAY && mPlayer.isPlaying()) {
                mPlayer.setPlaybackParams(mPlayer.getPlaybackParams().setSpeed(sSpeed));
            }
        });
        liveLoop.observe(this, aBoolean -> {
            sLoop = aBoolean;
        });
        liveLeadTime.observe(this, leadTime -> {
            sLeadTime = leadTime;
        });
        liveContinue.observe(this, continueBool -> {
            sContinue = continueBool;
        });
        livePassage.observe(this, passage -> {
            sPassage = passage;
        });
        livePassageData.observe(this, passageData -> {
            sPassageData = passageData;
        });

    }

    @Override
    public IBinder onBind(Intent arg0) {
        //TODO
        super.onBind(arg0);
        return null;
    }

    @Override
    public void onCreate() {
        //TODO
        super.onCreate();
        Log.d(SoundService.class.getSimpleName(), "onCreate()");
        liveServiceState.postValue(STATE_SERVICE.NOT_INIT);
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        //TODO
        super.onStartCommand(intent, flags, startId);

        if (intent == null || intent.getAction() == null) {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        // bootstrap player on first play command
        if (action == MusicConstants.ACTION.PLAY_ACTION &&
                sServiceState == STATE_SERVICE.NOT_INIT) {
            action = MusicConstants.ACTION.START_ACTION;
        }

        int tmpPosition = intent.getIntExtra(MusicConstants.KEY_EXTRA.POSITION, -1);
        if (tmpPosition > -1) sPlayingPosition = tmpPosition;
        switch (action) {
            case MusicConstants.ACTION.START_ACTION:
                Log.i(TAG, "Received start Intent ");
                liveServiceState.postValue(STATE_SERVICE.PREPARE);

                sSpeed = intent.getFloatExtra(MusicConstants.KEY_EXTRA.SPEED, 1f);
                sLeadTime = intent.getIntExtra(MusicConstants.KEY_EXTRA.LEAD_TIME, 5);
                sLoop = intent.getBooleanExtra(MusicConstants.KEY_EXTRA.LOOP, false);
                sContinue = intent.getBooleanExtra(MusicConstants.KEY_EXTRA.CONTINUE, false);

                startForeground(MusicConstants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification(
                        LazyDatabase.FORMATION_DATA.entryPoints.get(sPlayingPosition).label
                ));

                destroyPlayer();

                play(sPlayingPosition);
                break;

            case MusicConstants.ACTION.PLAY_ACTION:
                Log.i(TAG, "Clicked Play");
                liveServiceState.postValue(STATE_SERVICE.PLAY);

                mNotificationManager.notify(MusicConstants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification(
                        LazyDatabase.FORMATION_DATA.entryPoints.get(sPlayingPosition).label
                ));

                destroyPlayer();

                play(sPlayingPosition);
                break;

            case MusicConstants.ACTION.PAUSE_ACTION:
                Log.i(TAG, "Clicked Pause");
                if (sServiceState == STATE_SERVICE.PLAY ||
                        sServiceState == STATE_SERVICE.PREPARE) {
                    liveServiceState.postValue(STATE_SERVICE.PAUSE);

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

                sSpeed = intent.getFloatExtra(MusicConstants.KEY_EXTRA.SPEED, 1f);
                if (sServiceState == STATE_SERVICE.PLAY) {
                    synchronized (mLock) {
                        mPlayer.setPlaybackParams(mPlayer.getPlaybackParams().setSpeed(sSpeed));
                    }
                }
                break;

            case MusicConstants.ACTION.LT_CHANGE_ACTION:
                Log.i(TAG, "Received lead time change intent");
                sLeadTime = intent.getIntExtra(MusicConstants.KEY_EXTRA.LEAD_TIME, 5);
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
                sContinue = intent.getBooleanExtra(MusicConstants.KEY_EXTRA.CONTINUE, false);
                sLoop = intent.getBooleanExtra(MusicConstants.KEY_EXTRA.LOOP, false);
                sPassage = intent.getBooleanExtra(MusicConstants.KEY_EXTRA.PASSAGE, false);
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
        liveServiceState.postValue(STATE_SERVICE.NOT_INIT);
        try {
            mTimerUpdateHandler.removeCallbacksAndMessages(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // todo kill all timer which are not killed by player
        super.onDestroy();
    }

    private void destroyPlayer() {
        //TODO
        liveServiceState.postValue(STATE_SERVICE.NOT_INIT);
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


        //TODO kill all timer
        mUpdatePlayerPositionInTimeTimer.cancel();
        mUpdatePlayerPositionInTimeTimer.purge();
    }

    public boolean onError(MediaPlayer mp, int what, int extra) {
        //TODO
        Log.d(TAG, "Player onError() what:" + what);
        destroyPlayer();
        mHandler.postDelayed(mDelayedShutdown, MusicConstants.DELAY_SHUTDOWN_FOREGROUND_SERVICE);
        mNotificationManager.notify(MusicConstants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification("Error"));
        liveServiceState.postValue(STATE_SERVICE.PAUSE);
        return false;
    }

    /**
     * Initialized the player (loads music file, sets configuration).
     */
    private void initPlayer() {
        mPlayer = MediaPlayer.create(this, R.raw.title1);
        mPlayer.setOnErrorListener(this);
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnSeekCompleteListener(this);

        lockCPU();
    }

    /**
     * Prepares player to play a track.
     *
     * @param position in the entry point array to start from.
     */
    private void play(int position) {
        liveServiceState.postValue(STATE_SERVICE.PREPARE);

        try {
            mHandler.removeCallbacksAndMessages(null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        int from = LazyDatabase.FORMATION_DATA.entryPoints.get(position).start;

        synchronized (mLock) {
            try {
                if (mPlayer == null) {
                    initPlayer();
                }
//                mPlayer.setDataSource(this, getResources(R.raw.title1).);
//                mPlayer.prepareAsync();
                //TODO move to on prepared when using files instead of reasources
                int startPoint = from - (sLeadTime * 1000);
                startPoint = startPoint < 0 ? 0 : startPoint;
                mPlayer.seekTo(startPoint);
                livePlayerPositionInTime.postValue(startPoint);

            } catch (Exception e) {
                destroyPlayer();
                e.printStackTrace();
            }
        }
    }

    /**
     * Builds the notification including the player controls for the foreground service.
     *
     * @param message current playing track.
     * @return
     */
    private Notification prepareNotification(String message) {
        //TODO
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

            case STATE_SERVICE.PAUSE:
                lRemoteViews.setViewVisibility(R.id.ui_notification_progress_bar, View.INVISIBLE);
                lRemoteViews.setOnClickPendingIntent(R.id.ui_notification_player_button, lPendingPlayIntent);
                lRemoteViews.setImageViewResource(R.id.ui_notification_player_button, R.drawable.ic_play_arrow_24);
                break;

            case STATE_SERVICE.PLAY:
                lRemoteViews.setViewVisibility(R.id.ui_notification_progress_bar, View.INVISIBLE);
                lRemoteViews.setOnClickPendingIntent(R.id.ui_notification_player_button, lPendingPauseIntent);
                lRemoteViews.setImageViewResource(R.id.ui_notification_player_button, R.drawable.ic_pause_24);
                break;

            case STATE_SERVICE.PREPARE:
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


    /**
     * Is called when player is prepared async.
     * TODO for later use ...
     *
     * @param mediaPlayer
     */
    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {

    }

    /**
     * Is called when player finished seek action. This is the entry point for playing the music.
     *
     * @param mediaPlayer
     */
    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {
        //TODO
        Log.d(TAG, "Player onSeekComplete()");
        liveServiceState.postValue(STATE_SERVICE.PLAY);
        mNotificationManager.notify(MusicConstants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification(""));
        try {
            mPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mPlayer.start();
        mPlayer.setPlaybackParams(mPlayer.getPlaybackParams().setSpeed(sSpeed));
        startFadeIn();

        int timerWakeDelay = Math.round(
                (LazyDatabase.FORMATION_DATA.entryPoints.get(sPlayingPosition).stop - mPlayer.getCurrentPosition())
                        / sSpeed);
        if (timerWakeDelay > 0) {
            mStopTimer.cancel();
            mStopTimer.purge();
            mStopTimer = new Timer(true);
            mStopTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        if (mPlayer == null || !mPlayer.isPlaying()) return;

                        if (!sContinue) {
                            if (sLoop) {
                                // restart track
                                startFadeOutAndPlayPosition(sPlayingPosition);
                            } else if (sPassage) {
                                //TODO check if position matches and stop or reschedule
                            } else {
                                // strop playing
                                startFadeOutAndPlayPosition(-1);
                            }
                        } else {
                            //TODO reschedule time for next position end, to stop when user disables continue button
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

        mUpdatePlayerPositionInTimeTimer.cancel();
        mUpdatePlayerPositionInTimeTimer.purge();
        mUpdatePlayerPositionInTimeTimer = new Timer(true);
        mUpdatePlayerPositionInTimeTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (sServiceState == STATE_SERVICE.PLAY
                        && mPlayer.isPlaying()) {
                    livePlayerPositionInTime.postValue(mPlayer.getCurrentPosition());
                }
            }
        }, 100, 100);
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
        //TODO
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
                    if (mPlayer == null || !mPlayer.isPlaying()){
                        mFadeTimer.cancel();
                        mFadeTimer.purge();
                        return;
                    }

                    volume += deltaVolume;
                    //Cancel and Purge the Timer if the desired volume has been reached
                    if (volume >= 1f) {
                        mPlayer.setVolume(1f, 1f);
                        mFadeTimer.cancel();
                        mFadeTimer.purge();
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

    /**
     * @param positionToPlayAfterFadeOut use <0 for no play after fading out
     */
    private void startFadeOutAndPlayPosition(int positionToPlayAfterFadeOut) {
        //TODO
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
                    if (mPlayer == null || !mPlayer.isPlaying()){
                        mFadeTimer.cancel();
                        mFadeTimer.purge();
                        return;
                    }

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
