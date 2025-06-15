package at.jku.dke.task_app.knn.dto;

import at.jku.dke.task_app.knn.data.entities.KnnTask;
import at.jku.dke.task_app.knn.data.entities.KnnTaskEntity;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for transferring all relevant information for a knn task.
 * <p>
 * Combines metadata from {@link KnnTask} and configuration/data from {@link KnnTaskEntity}.
 * </p>
 *
 * @param id                   Unique identifier for the task.
 * @param maxPoints            Maximum achievable points.
 * @param solution             Correct solution (if available).
 * @param k                    K parameter for KNN.
 * @param metric               Distance metric used.
 * @param numTrain             Number of training data points.
 * @param numTest              Number of test data points.
 * @param xMin                 Minimum X coordinate.
 * @param xMax                 Maximum X coordinate.
 * @param yMin                 Minimum Y coordinate.
 * @param yMax                 Maximum Y coordinate.
 * @param xLabel               X-axis label.
 * @param yLabel               Y-axis label.
 * @param tiebreaker           Tiebreaker strategy.
 * @param aufgabenPngBase64De  Base64 PNG of the German task image.
 * @param loesungPngBase64De   Base64 PNG of the German solution image.
 * @param aufgabenPngBase64En  Base64 PNG of the English task image.
 * @param loesungPngBase64En   Base64 PNG of the English solution image.
 * @param trainLabels          List of class labels.
 * @param trainPoints          Training points, grouped by label.
 * @param testPoints           List of test points ([x, y] pairs).
 */
public record KnnTaskDto(
    Long        id,
    BigDecimal  maxPoints,
    String      solution,
    Integer     k,
    String      metric,
    Integer     numTrain,
    Integer     numTest,
    Integer     xMin,
    Integer     xMax,
    Integer     yMin,
    Integer     yMax,
    String      xLabel,
    String      yLabel,
    String      tiebreaker,
    String      aufgabenPngBase64De,
    String      loesungPngBase64De,
    String      aufgabenPngBase64En,
    String      loesungPngBase64En,
    List<String> trainLabels,
    List<TrainPointGroup> trainPoints,
    List<int[]> testPoints
) {
    /**
     * Creates a DTO from the given task and entity.
     * @param task   Task metadata.
     * @param entity Model and dataset details.
     * @return The combined DTO.
     */
    public static KnnTaskDto fromEntities(KnnTask task,
                                          KnnTaskEntity entity) {
        if (entity == null)
            entity = new KnnTaskEntity();

        List<TrainPointGroup> trainPointGroups = entity.getTrainPoints().entrySet().stream()
            .map(e -> new TrainPointGroup(e.getKey(), e.getValue()))
            .toList();

        return new KnnTaskDto(
            task.getId(),
            task.getMaxPoints(),
            task.getSolution(),
            entity.getK(),
            entity.getMetric(),
            entity.getNumTrain(),
            entity.getNumTest(),
            entity.getxMin(),
            entity.getxMax(),
            entity.getyMin(),
            entity.getyMax(),
            entity.getxLabel(),
            entity.getyLabel(),
            entity.getTiebreaker(),
            entity.getAufgabenPngBase64De(),
            entity.getLoesungPngBase64De(),
            entity.getAufgabenPngBase64En(),
            entity.getLoesungPngBase64En(),
            entity.getTrainLabels(),
            trainPointGroups,
            entity.getTestPoints()
        );
    }
}
