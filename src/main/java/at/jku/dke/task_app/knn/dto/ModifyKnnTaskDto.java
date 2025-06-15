package at.jku.dke.task_app.knn.dto;

import at.jku.dke.task_app.knn.json.PointDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

/**
 * DTO for creating or updating a knn task’s configuration and data.
 * <p>
 * Used in API requests for task creation or editing by admins/instructors.
 * </p>
 *
 * @param solution    Correct solution for the task (nullable).
 * @param metric      Distance or scoring metric.
 * @param tiebreaker  Tiebreaker strategy for knn.
 * @param k           Parameter K (e.g., number of neighbors for KNN).
 * @param numTrain    Number of training data points.
 * @param numTest     Number of test data points.
 * @param xMin        Minimum X value (coordinate system).
 * @param xMax        Maximum X value.
 * @param yMin        Minimum Y value.
 * @param yMax        Maximum Y value.
 * @param xLabel      Label for X-axis.
 * @param yLabel      Label for Y-axis.
 * @param trainLabels List of class labels for training.
 * @param trainPoints Grouped training points per label.
 * @param testPoints  List of test points ([x, y] pairs).
 */
public record ModifyKnnTaskDto(
    String  solution,
    String  metric,
    String  tiebreaker,
    Integer k,
    Integer numTrain,
    Integer numTest,
    Integer xMin,
    Integer xMax,
    Integer yMin,
    Integer yMax,
    String  xLabel,
    String  yLabel,
    List<String> trainLabels,
    List<TrainPointGroup> trainPoints,
    @JsonDeserialize(contentUsing = PointDeserializer.class)
    List<int[]> testPoints
) {}
