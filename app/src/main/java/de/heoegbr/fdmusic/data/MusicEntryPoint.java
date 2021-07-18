package de.heoegbr.fdmusic.data;

/**
 * Container for an entry point in the music file
 *
 * @author Jens
 */
public class MusicEntryPoint {
    public String label;
    public int start;
    public int stop;

    // points to a shape in the FormationData.shapes array
    public int shapeJumpPoint;

    public MusicEntryPoint(String label, int start, int stop, int shapeJumpPoint) {
        this.label = label;
        this.start = start + MusicConstants.MUSIC_OFFSET;
        this.stop = stop + MusicConstants.MUSIC_OFFSET;
        this.shapeJumpPoint = shapeJumpPoint;
    }
}
