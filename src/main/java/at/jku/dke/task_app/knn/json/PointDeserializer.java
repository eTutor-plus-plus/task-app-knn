package at.jku.dke.task_app.knn.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

/**
 * Deserializer that accepts both array style [x, y] and object style {"x": ..., "y": ...}.
 * Always returns an int[2] array.
 */
public final class PointDeserializer extends JsonDeserializer<int[]> {

    @Override
    public int[] deserialize(JsonParser p,
                             DeserializationContext ctx)
        throws IOException, JsonProcessingException {

        TreeNode n = p.readValueAsTree();

        // Array style: [x, y]
        if (n instanceof ArrayNode arr && arr.size() == 2) {
            return new int[]{
                arr.get(0).intValue(),
                arr.get(1).intValue()
            };
        }

        // Object style: {"x": ..., "y": ...}
        if (n instanceof ObjectNode obj
            && obj.has("x") && obj.has("y")) {

            return new int[]{
                obj.get("x").intValue(),
                obj.get("y").intValue()
            };
        }

        // Any other format: throw error (400 Bad Request)
        throw ctx.weirdStringException(
            n.toString(),
            int[].class,
            "point must be [x,y] or {x:..,y:..}");
    }
}
