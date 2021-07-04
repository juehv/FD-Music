package de.heoegbr.fdmusic.data;

public class MusicEntryPoint {
    public String label;
    public int start;
    public int stop;
    public int shapeJumpPoint;

    public MusicEntryPoint(String label, int start, int stop, int shapeJumpPoint) {
        this.label = label;
        this.start = start + MusicConstants.MUSIC_OFFSET;
        this.stop = stop + MusicConstants.MUSIC_OFFSET;
        this.shapeJumpPoint = shapeJumpPoint;
    }
}
