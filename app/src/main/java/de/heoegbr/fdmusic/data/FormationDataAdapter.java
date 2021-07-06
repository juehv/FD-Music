package de.heoegbr.fdmusic.data;

import android.graphics.PointF;
import android.util.Log;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Adapter to read meta.json to internal container
 *
 * @author Jens
 */
public class FormationDataAdapter implements JsonDeserializer<FormationData> {
    private static final String TAG = FormationDataAdapter.class.getName();

    @Override
    public FormationData deserialize(JsonElement jsonElement, Type type,
                                     JsonDeserializationContext jsonDeserializationContext)
            throws JsonParseException {
        if (!jsonElement.isJsonNull()) {
            JsonObject fdataAsJson = jsonElement.getAsJsonObject();

            List<MusicEntryPoint> entryPoints = new ArrayList<>();
            int shapeListCounter = 0;
            List<FormationShape> shapes = new ArrayList<>();

            // Read header information
            if (!fdataAsJson.get("fdmusic-format").getAsString().equalsIgnoreCase("1.0")) {
                Log.e(TAG, "JSON file format not supported. Expected v1.0");
                return null;
            }
            // TODO evaluate formation, song name, file name

            entryPoints.add(new MusicEntryPoint(
                    "All",
                    0,
                    fdataAsJson.get("music-length").getAsInt(),
                    0
            ));
            entryPoints.add(new MusicEntryPoint(
                    "Gong2Gong",
                    fdataAsJson.get("gong-start").getAsInt(),
                    fdataAsJson.get("gong-end").getAsInt(),
                    -1
            ));

            // iterate
            Iterator<JsonElement> entryPointsIterator = fdataAsJson.get("entry-points")
                    .getAsJsonArray().iterator();
            while (entryPointsIterator.hasNext()) {
                JsonObject entryPointAsJsonObject = entryPointsIterator.next().getAsJsonObject();

                entryPoints.add(new MusicEntryPoint(
                        entryPointAsJsonObject.get("name").getAsString(),
                        entryPointAsJsonObject.get("start").getAsInt(),
                        entryPointAsJsonObject.get("end").getAsInt(),
                        shapeListCounter
                ));

                Iterator<JsonElement> shapesIterator = entryPointAsJsonObject.get("shapes")
                        .getAsJsonArray().iterator();
                while (shapesIterator.hasNext()) {
                    JsonObject shapeAsJsonObject = shapesIterator.next().getAsJsonObject();
                    FormationShape tmpShape = new FormationShape(shapeAsJsonObject.get("time").getAsInt());

                    JsonObject tmpShapeAsJsonObject = shapeAsJsonObject.get("shape").getAsJsonObject();
                    for (Map.Entry<String, JsonElement> entry : tmpShapeAsJsonObject.entrySet()) {
                        JsonObject position = entry.getValue().getAsJsonObject();
                        float x = position.get("x").getAsFloat();
                        float y = position.get("y").getAsFloat();

                        PointF point = new PointF(x, y);

                        tmpShape.positions.put(entry.getKey(), point);
                    }

                    shapes.add(tmpShape);
                    shapeListCounter++;
                }
            }

            // contruct container
            FormationData returnValue = new FormationData();
            returnValue.entryPoints = entryPoints;
            returnValue.shapes = shapes;

            return returnValue;
        } else
            return null;
    }
}
