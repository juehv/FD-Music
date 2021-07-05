package de.heoegbr.fdmusic.data;

import android.graphics.PointF;

import java.util.HashMap;
import java.util.Map;

public class FormationShape {

    public int timePoint;
    public Map<String, PointF> positions = new HashMap<>();

    public FormationShape(int timePoint) {
        this.timePoint = timePoint;
    }
}
