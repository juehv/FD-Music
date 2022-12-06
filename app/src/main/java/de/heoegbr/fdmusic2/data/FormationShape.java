package de.heoegbr.fdmusic2.data;

import android.graphics.PointF;

import java.util.HashMap;
import java.util.Map;

/**
 * Container for one formation shape
 *
 * @author David
 */
public class FormationShape {

    public int timePoint;
    public Map<String, PointF> positions = new HashMap<>();

    public FormationShape(int timePoint) {
        this.timePoint = timePoint;
    }
}
