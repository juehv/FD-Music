package de.heoegbr.fdmusic.data;

public class EntryPoint {
    public String label;
    public int start;
    public int stop;

    public EntryPoint(String label, int start, int stop) {
        this.label = label;
        this.start = start + MusicConstants.MUSIC_OFFSET;
        this.stop = stop + MusicConstants.MUSIC_OFFSET;
    }
}
