package de.heoegbr.fdmusic.ui.imageplan;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import de.heoegbr.fdmusic.R;
import de.heoegbr.fdmusic.data.Image;

/**
 * Custom view for rendering images of a formation choreography.
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
     * Size of the dance floor in meters from the center; assumes square floors.
     */
    private static final int FLOOR_SIZE = 8;


    private Integer timeInMusic = null;

    private SortedMap<Integer, Image> images = new TreeMap<>();


    class ImageInTime {
        public int time;
        public Image image;
    }

    class ImageDeserializer implements JsonDeserializer<Image> {

        @Override
        public Image deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext)
                throws JsonParseException {
            if (!jsonElement.isJsonNull()) {
                Image image = new Image();

                for (Map.Entry<String, JsonElement> entry : jsonElement.getAsJsonObject().entrySet()) {
                    JsonObject position = entry.getValue().getAsJsonObject();
                    float width = position.get("width").getAsFloat();
                    float depth = position.get("depth").getAsFloat();

                    PointF point = new PointF(width, depth);

                    image.positions.put(entry.getKey(), point);
                }

                return image;
            }
            else
                return null;
        }
    }


    public ImagePlanView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Load the images.
        Reader reader = new InputStreamReader(context.getResources().openRawResource(R.raw.images));
        Gson gson = new GsonBuilder().registerTypeAdapter(Image.class, new ImageDeserializer()).create();
        Type listType = new TypeToken<List<ImageInTime>>() {}.getType();

        List<ImageInTime> imageList = gson.fromJson(reader, listType);
        imageList.forEach(i -> images.put(i.time, i.image));
    }

    public void setTimeInMusic(int timeInMusic) {
        this.timeInMusic = timeInMusic;

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Paint paint = new Paint();

        drawGrid(paint, canvas);
        drawPositions(paint, canvas);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Always report that we need as much width as we need height, thus forcing the
        // view to be square.
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }

    private void drawGrid(Paint paint, Canvas canvas) {
        int height = getHeight();
        int width = getWidth();

        float depthInterval = getDepthInterval();
        float widthInterval = getWidthInterval();

        paint.setColor(Color.DKGRAY);

        // Draw lines for every meter of width ("Breite").
        float currentWidth = 0.0f;

        for (int meter = -FLOOR_SIZE; meter < FLOOR_SIZE; meter++) {
            paint.setStrokeWidth(getThicknessOfMeterLine(meter));
            canvas.drawLine(currentWidth, 0.0f, currentWidth, (float) height, paint);
            currentWidth += widthInterval;
        }

        // Draw lines for every meter of depth ("Tiefe").
        float currentDepth = 0.0f;

        for (int meter = -FLOOR_SIZE; meter < FLOOR_SIZE; meter++) {
            paint.setStrokeWidth(getThicknessOfMeterLine(meter));
            canvas.drawLine(0.0f, currentDepth, (float) width, currentDepth, paint);
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
        int lastImageTime = 0;
        Image lastImage = null;

        int nextImageTime = 0;
        Image nextImage = null;

        // Load the last image that is still before the current time and the first image that is
        // after the current time, and interpolate between the two.
        // Look one millisecond later than the actual time to avoid problems with how headMap()
        // and tailMap() work when we are exactly at the time of the new image.
        if (timeInMusic != null) {
            SortedMap<Integer, Image> headMap = images.headMap(timeInMusic + 1);

            if (!headMap.isEmpty()) {
                lastImageTime = headMap.lastKey();
                lastImage = images.get(lastImageTime);
            }

            SortedMap<Integer, Image> tailMap = images.tailMap(timeInMusic + 1);

            if (!tailMap.isEmpty()) {
                nextImageTime = tailMap.firstKey();
                nextImage = images.get(nextImageTime);
            }
        }

        if (lastImage == null || nextImage == null)
            return;

        // Determine where we are at, on a 0-to-1 scale, between images.
        float ratio = (timeInMusic - lastImageTime) / (float) (nextImageTime - lastImageTime);

        for (String position : lastImage.positions.keySet()) {
            // Interpolate between the last and next position.
            PointF lastPosition = lastImage.positions.get(position);
            PointF nextPosition = nextImage.positions.get(position);

            float x = lastPosition.x * (1.0f - ratio) + nextPosition.x * ratio;
            float y = lastPosition.y * (1.0f - ratio) + nextPosition.y * ratio;

            drawPosition(position, x, y, paint, canvas);
        }
    }

    private void drawPosition(String positionName, float depth, float width, Paint paint, Canvas canvas) {
        float x = getDepthInterval() * (depth + FLOOR_SIZE);
        float y = getWidthInterval() * (-width + FLOOR_SIZE);

        paint.setColor(Color.BLUE);
        paint.setAntiAlias(true);
        canvas.drawCircle(x, y, POSITION_SIZE, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(POSITION_SIZE * 1.5f);
        canvas.drawText(positionName, x - (POSITION_SIZE / 2), y + (POSITION_SIZE / 2), paint);
    }

    private float getDepthInterval() {
        return getHeight() / (FLOOR_SIZE * 2);
    }

    private float getWidthInterval() {
        return getWidth() / (FLOOR_SIZE * 2);
    }

}