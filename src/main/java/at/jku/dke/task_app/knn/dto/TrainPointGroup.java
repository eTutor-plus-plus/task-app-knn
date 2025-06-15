package at.jku.dke.task_app.knn.dto;

import at.jku.dke.task_app.knn.json.PointDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DTO representing all training points for a single class label.
 * <p>
 * Used to group points by label for easy transfer and serialization.
 * </p>
 */
public class TrainPointGroup {
    /** Class label for this group (e.g., "A", "B"). */
    private String label;

    /** Training points for this label ([x, y] arrays). */
    @JsonDeserialize(contentUsing = PointDeserializer.class)
    private List<int[]> points;

    /** Default constructor. */
    public TrainPointGroup() {}

    /**
     * Constructor with label and points.
     * @param label Class label.
     * @param points Points for this label.
     */
    public TrainPointGroup(String label, List<int[]> points) {
        this.label = label;
        this.points = points;
    }

    /** @return The class label. */
    public String getLabel() { return label; }

    /** @param label The class label. */
    public void setLabel(String label) { this.label = label; }

    /** @return The training points. */
    public List<int[]> getPoints() { return points; }

    /** @param points The training points. */
    public void setPoints(List<int[]> points) { this.points = points; }

    /**
     * Converts a map (label → points) to a list of groups.
     */
    public static List<TrainPointGroup> mapToGroupList(Map<String, List<int[]>> map) {
        if (map == null) return List.of();
        return map.entrySet().stream()
            .map(e -> new TrainPointGroup(e.getKey(), e.getValue()))
            .toList();
    }

    /**
     * Converts a list of groups to a map (label → points).
     */
    public static Map<String, List<int[]>> groupListToMap(List<TrainPointGroup> list) {
        if (list == null) return Map.of();
        return list.stream().collect(Collectors.toMap(TrainPointGroup::getLabel, TrainPointGroup::getPoints));
    }
}
