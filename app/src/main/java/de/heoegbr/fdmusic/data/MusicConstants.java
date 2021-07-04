package de.heoegbr.fdmusic.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import de.heoegbr.fdmusic.R;

public class MusicConstants {

    public static final int NOTIFICATION_ID_FOREGROUND_SERVICE = 8466503;
    public final static String FOREGROUND_CHANNEL_ID = "de.heoegbr.fdmusic.notification";
    public static final long DELAY_SHUTDOWN_FOREGROUND_SERVICE = 20000;
    public static final long DELAY_UPDATE_NOTIFICATION_FOREGROUND_SERVICE = 10000;
    public static final String PASSPHRASE = "E60BAB61BAB2BC7A3E3A54841A6D32616734085ACB00C67D29C6C357644A3404"; //fdabteam2021

    public static class ACTION {
        public static final String MAIN_ACTION = "music.action.main";
        public static final String PAUSE_ACTION = "music.action.pause";
        public static final String PLAY_ACTION = "music.action.play";
        public static final String START_ACTION = "music.action.start";
        public static final String STOP_ACTION = "music.action.stop";
        public static final String LT_CHANGE_ACTION = "music.action.leadtime";
        public static final String SPEED_CHANGE_ACTION = "music.action.speed";
        public static final String LOOP_ACTION = "music.action.loop";
        public static final String CONTINUE_ACTION = "music.action.continue";
    }

    public static class STATE_SERVICE {
        public static final int PREPARE = 30;
        public static final int PLAY = 20;
        public static final int PAUSE = 10;
        public static final int NOT_INIT = 0;
    }

    public static class KEY_EXTRA {
        public static final String POSITION = "player.extra.position";
        public static final String LOOP = "player.extra.loop";
        public static final String CONTINUE = "player.extra.continue";
        public static final String LEAD_TIME = "player.extra.leadtime";
        public static final String SPEED = "player.extra.speed";
    }

    //FIXME lazy db --> should be loaded from a file or so ...
    public static int MUSIC_OFFSET = 21000;
    public static List<EntryPoint> MUSIC_ENTRY_POINTS = new ArrayList<>();

    static {
        MUSIC_ENTRY_POINTS.add(new EntryPoint("All", -1, 352500));
        MUSIC_ENTRY_POINTS.add(new EntryPoint("Gong2Gong", 46000, 310000));
        MUSIC_ENTRY_POINTS.add(new EntryPoint("Einmarsch", -1, 48000));
        MUSIC_ENTRY_POINTS.add(new EntryPoint("Tango 1", 46000, 60000));
        MUSIC_ENTRY_POINTS.add(new EntryPoint("WiWa 1", 61000, 71000));
        MUSIC_ENTRY_POINTS.add(new EntryPoint("LaWa 1", 70000, 84500));
        MUSIC_ENTRY_POINTS.add(new EntryPoint("QS 1", 85000, 103000));
        MUSIC_ENTRY_POINTS.add(new EntryPoint("Tango 2", 103000, 119500));
        MUSIC_ENTRY_POINTS.add(new EntryPoint("SF 1", 119000, 137500));
        MUSIC_ENTRY_POINTS.add(new EntryPoint("LaWa 2", 136000, 158500));
        MUSIC_ENTRY_POINTS.add(new EntryPoint("Tango 3", 162000, 190500));
        MUSIC_ENTRY_POINTS.add(new EntryPoint("SF 2", 188000, 209000));
        MUSIC_ENTRY_POINTS.add(new EntryPoint("LaWa 3", 208000, 232000));
        MUSIC_ENTRY_POINTS.add(new EntryPoint("WiWa 2", 230000, 242000));
        MUSIC_ENTRY_POINTS.add(new EntryPoint("Tango 4", 242000, 258000));
        MUSIC_ENTRY_POINTS.add(new EntryPoint("LaWa 4", 258000, 274000));
        MUSIC_ENTRY_POINTS.add(new EntryPoint("QS 2", 273000, 283000));
        MUSIC_ENTRY_POINTS.add(new EntryPoint("Tango 5 ", 285000, 310500));
        MUSIC_ENTRY_POINTS.add(new EntryPoint("Ausmarsch", 313000, 352500));
    }

}
