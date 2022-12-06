package de.heoegbr.fdmusic2.data;

/**
 * Constants for managing sound sound service (and more).
 * Huge parts are stolen from stackoverflow
 *
 * @author Jens
 */
public class MusicConstants {
    //TODO don'T use hardcoded passwords
    public static final String PASSPHRASE = "E60BAB61BAB2BC7A3E3A54841A6D32616734085ACB00C67D29C6C357644A3404"; //fdabteam2021

    //FIXME hacked version of password correct visualization; as we replace this screen soon, I don't care for now
    public static boolean THIS_IS_A_HACK = false;

    public static final int NOTIFICATION_ID_FOREGROUND_SERVICE = 8466503;
    public final static String FOREGROUND_CHANNEL_ID = "de.heoegbr.fdmusic.notification";
    public static final long DELAY_SHUTDOWN_FOREGROUND_SERVICE = 60000;
    public static final long DELAY_UPDATE_NOTIFICATION_FOREGROUND_SERVICE = 10000;

    public static class ACTION {
        public static final String MAIN_ACTION = "music.action.main";
        public static final String PAUSE_ACTION = "music.action.pause";
        public static final String PLAY_ACTION = "music.action.play";
        public static final String START_ACTION = "music.action.start";
        public static final String STOP_ACTION = "music.action.stop";
        public static final String LT_CHANGE_ACTION = "music.action.leadtime";
        public static final String SPEED_CHANGE_ACTION = "music.action.speed";
        public static final String LOOP_CONTINUE_ACTION = "music.action.loopContinue";
    }

    public static class KEY_EXTRA {
        public static final String POSITION = "player.extra.position";
        public static final String LOOP = "player.extra.loop";
        public static final String CONTINUE = "player.extra.continue";
        public static final String LEAD_TIME = "player.extra.leadtime";
        public static final String SPEED = "player.extra.speed";
        public static final String PASSAGE = "player.extra.passage";
    }

    //FIXME lazy db --> should be loaded from a file or so ...
    public static int MUSIC_OFFSET = 21000;

}
