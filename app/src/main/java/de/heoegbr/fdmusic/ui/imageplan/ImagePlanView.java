package de.heoegbr.fdmusic.ui.imageplan;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;

import java.util.SortedMap;
import java.util.TreeMap;

import de.heoegbr.fdmusic.data.FormationShape;
import de.heoegbr.fdmusic.data.MusicConstants;

/**
 * Custom view for rendering shapes of a formation choreography.
 *
 * @author David Schneider
 */
public class ImagePlanView extends View {

    /**
     * Size of the position marker, in pixels.
     * TODO: This should probably be screen-dependent.
     */
    private static final int POSITION_SIZE = 20;

    /**
     * Space for the markings of meters on the side of the floor grid.
     * TODO: This should probably be screen-dependent.
     */
    private static final int METER_MARKING_SIZE = 30;

    /**
     * Size of the dance floor in meters from the center; assumes square floors.
     */
    private static final int FLOOR_SIZE = 8;


    private Integer timeInMusic = null;

    private SortedMap<Integer, FormationShape> shapes = new TreeMap<>();


    public ImagePlanView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Create index of shapes by timestamp.
        MusicConstants.FORMATION_DATA.shapes.forEach(s -> shapes.put(s.timePoint, s));
    }

    public void skipToNextShape() {
        if (timeInMusic != null) {
            SortedMap<Integer, FormationShape> tailMap = shapes.tailMap(timeInMusic + 1);

            if (!tailMap.isEmpty())
                setTimeInMusic(tailMap.firstKey());
        }
    }

    public void skipToPreviousShape() {
        if (timeInMusic != null) {
            SortedMap<Integer, FormationShape> headMap = shapes.headMap(timeInMusic);

            if (!headMap.isEmpty())
                setTimeInMusic(headMap.lastKey());
        }
    }

    public void setTimeInMusic(int timeInMusic) {
        this.timeInMusic = timeInMusic;

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Paint paint = new Paint();

        drawFloorGrid(paint, canvas);
        drawPositions(paint, canvas);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Always report that we need as much width as we need height, thus forcing the
        // view to be square; always use the smaller of the sides.
        if (widthMeasureSpec < heightMeasureSpec)
            super.onMeasure(widthMeasureSpec, widthMeasureSpec);
        else
            super.onMeasure(heightMeasureSpec, heightMeasureSpec);
    }

    private void drawFloorGrid(Paint paint, Canvas canvas) {
        int height = getHeight();
        int width = getWidth();

        float depthInterval = getDepthInterval();
        float widthInterval = getWidthInterval();

        paint.setColor(Color.DKGRAY);

        // Draw lines for every meter of width ("Breite").
        float currentWidth = METER_MARKING_SIZE;

        for (int meter = -FLOOR_SIZE; meter <= FLOOR_SIZE; meter++) {
            paint.setStrokeWidth(getThicknessOfMeterLine(meter));
            canvas.drawLine(currentWidth, METER_MARKING_SIZE, currentWidth, (float) height - METER_MARKING_SIZE, paint);

            if (Math.abs(meter) != FLOOR_SIZE) {
                paint.setTextSize(METER_MARKING_SIZE * 0.66f);
                paint.setAntiAlias(true);
                canvas.drawText(String.format("%d", Math.abs(meter)), currentWidth - (METER_MARKING_SIZE / 4.0f), METER_MARKING_SIZE / 2.0f, paint);
            }

            currentWidth += widthInterval;
        }

        // Draw lines for every meter of depth ("Tiefe").
        float currentDepth = METER_MARKING_SIZE;

        for (int meter = -FLOOR_SIZE; meter <= FLOOR_SIZE; meter++) {
            paint.setStrokeWidth(getThicknessOfMeterLine(meter));
            canvas.drawLine(METER_MARKING_SIZE, currentDepth, (float) width - METER_MARKING_SIZE, currentDepth, paint);

            if (Math.abs(meter) != FLOOR_SIZE) {
                paint.setTextSize(METER_MARKING_SIZE * 0.66f);
                paint.setAntiAlias(true);
                canvas.drawText(String.format("%d", Math.abs(meter)), METER_MARKING_SIZE / 4.0f, currentDepth + (METER_MARKING_SIZE / 4.0f), paint);
            }

            currentDepth += depthInterval;
        }
    }

    private float getThicknessOfMeterLine(int meter) {
        if (meter == 0)
            return 4.0f;
        else if (Math.abs(meter) == 6 || Math.abs(meter) == 3)
            return 2.0f;
        else
            return 1.0f;
    }

    private void drawPositions(Paint paint, Canvas canvas) {
        int previousShapeTime = 0;
        FormationShape previousShape = null;

        int nextShapeTime = 0;
        FormationShape nextShape = null;

        // Load the last shape that is still before the current time and the first shape that is
        // after the current time, and interpolate between the two.
        // Look one millisecond later than the actual time to avoid problems with how headMap()
        // and tailMap() work when we are exactly at the time of the new shape.
        if (timeInMusic != null) {
            SortedMap<Integer, FormationShape> headMap = shapes.headMap(timeInMusic + 1);

            if (!headMap.isEmpty()) {
                previousShapeTime = headMap.lastKey();
                previousShape = shapes.get(previousShapeTime);
            }

            SortedMap<Integer, FormationShape> tailMap = shapes.tailMap(timeInMusic + 1);

            if (!tailMap.isEmpty()) {
                nextShapeTime = tailMap.firstKey();
                nextShape = shapes.get(nextShapeTime);
            }
        }

        if (previousShape == null || nextShape == null)
            return;

        // Determine where we are at, on a 0-to-1 scale, between shapes.
        float ratio = (timeInMusic - previousShapeTime) / (float) (nextShapeTime - previousShapeTime);

        for (String position : previousShape.positions.keySet()) {
            // Interpolate between the last and next position.
            PointF lastPosition = previousShape.positions.get(position);
            PointF nextPosition = getNextPosition(nextShape, position);

            float x = lastPosition.x * (1.0f - ratio) + nextPosition.x * ratio;
            float y = lastPosition.y * (1.0f - ratio) + nextPosition.y * ratio;

            // Draw paths from the current position to the next position.
            drawPath(ratio, x, y, nextPosition.x, nextPosition.y, paint, canvas);
            drawPosition(position, x, y, paint, canvas);
        }
    }

    private PointF getNextPosition(FormationShape nextShape, String position) {
        PointF nextPosition = nextShape.positions.get(position);

        if (nextPosition != null)
            return nextPosition;

        // If the pair is merging from this shape to the next, then find the position of the
        // merged pair to interpolate to.
        if (isSeparatedPair(position)) {
            nextPosition = nextShape.positions.get(position.substring(0, 1));

            if (nextPosition != null)
                return nextPosition;

            nextPosition = nextShape.positions.get(position.substring(1));

            if (nextPosition != null)
                return nextPosition;
        }

        // If the pair is separating from this shape to the next, then find the position of one of
        // the dancers to interpolate to.
        nextPosition = nextShape.positions.get("_" + position);

        if (nextPosition != null)
            return nextPosition;

        throw new RuntimeException(String.format("Next position for %s not found!", position));
    }

    private static boolean isSeparatedPair(String position) {
        return position.length() > 1;
    }
    
    private void drawPath(float ratio, float curDepth, float curWidth, float nextDepth, float nextWidth, Paint paint, Canvas canvas) {
        paint.setStrokeWidth(5.0f);
        paint.setColor(Color.BLUE);
        paint.setAlpha((int) ((1.0f - ratio) * 64));
        canvas.drawLine(
                convertDepthMetersToPixels(curDepth),
                convertWidthMetersToPixels(curWidth),
                convertDepthMetersToPixels(nextDepth),
                convertWidthMetersToPixels(nextWidth),
                paint);
    }

    private void drawPosition(String positionName, float depth, float width, Paint paint, Canvas canvas) {
        float x = convertDepthMetersToPixels(depth);
        float y = convertWidthMetersToPixels(width);

        paint.setColor(Color.BLUE);
        paint.setAntiAlias(true);
        canvas.drawCircle(x, y, POSITION_SIZE, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(POSITION_SIZE * 1.5f);
        canvas.drawText(positionName, x - (POSITION_SIZE / 2.0f), y + (POSITION_SIZE / 2.0f), paint);
    }

    private float convertDepthMetersToPixels(float depth) {
        return METER_MARKING_SIZE + getDepthInterval() * (depth + FLOOR_SIZE);
    }

    private float convertWidthMetersToPixels(float width) {
        return METER_MARKING_SIZE + getWidthInterval() * (-width + FLOOR_SIZE);
    }

    private float getDepthInterval() {
        return (getHeight() - 2 * METER_MARKING_SIZE) / (float) (FLOOR_SIZE * 2);
    }

    private float getWidthInterval() {
        return (getWidth() - 2 * METER_MARKING_SIZE) / (float) (FLOOR_SIZE * 2);
    }

}