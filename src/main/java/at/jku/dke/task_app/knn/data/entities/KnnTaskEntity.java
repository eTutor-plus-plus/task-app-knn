package at.jku.dke.task_app.knn.data.entities;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Stores the configuration, data, and images for a knn task instance.
 */
@Entity
@Table(name = "knn_tasks")
public class KnnTaskEntity {

    /** Unique ID. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Reference to the logical task. */
    @OneToOne
    @JoinColumn(name = "task_id", referencedColumnName = "id")
    private KnnTask task;

    /** K value (neighbors for KNN). */
    private Integer k;

    /** Distance metric (e.g., "euclidean"). */
    private String metric;

    /** Number of training samples. */
    @Column(name = "num_train")
    private Integer numTrain;

    /** Data boundaries and test/train set sizes. */
    @Column(name = "x_min") private Integer xMin;
    @Column(name = "x_max") private Integer xMax;
    @Column(name = "y_min") private Integer yMin;
    @Column(name = "y_max") private Integer yMax;
    @Column(name = "num_test") private Integer numTest;

    /** Axis labels. */
    @Column(name = "x_label") private String xLabel;
    @Column(name = "y_label") private String yLabel;

    /** Tiebreaker for knn. */
    @Column(name = "tiebreaker") private String tiebreaker;

    /** Task and solution images (base64 PNG, DE/EN). */
    @Column(name = "aufgaben_png_base64_de") private String aufgabenPngBase64De;
    @Column(name = "loesung_png_base64_de") private String loesungPngBase64De;
    @Column(name = "aufgaben_png_base64_en") private String aufgabenPngBase64En;
    @Column(name = "loesung_png_base64_en") private String loesungPngBase64En;

    /** Training/test data as JSON. */
    @Column(name = "train_points_json") private String trainPointsJson;
    @Column(name = "train_labels_json") private String trainLabelsJson;
    @Column(name = "test_points_json") private String testPointsJson;

    /** Entity creation/modification timestamps. */
    @Column(name = "created_date") private OffsetDateTime createdDate;
    @Column(name = "last_modified_date") private OffsetDateTime lastModifiedDate;

    /** Temporary descriptions (not persisted). */
    @Transient private String tempDescriptionDe;
    @Transient private String tempDescriptionEn;

    // Getters and setters omitted for brevity (already standard).

    // === Data serialization convenience methods ===

    /**
     * @return Training labels as list (from JSON), or empty list.
     */
    public List<String> getTrainLabels() {
        if (this.trainLabelsJson == null || this.trainLabelsJson.isBlank()) return new ArrayList<>();
        try {
            return new ObjectMapper().readValue(this.trainLabelsJson, new TypeReference<>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /** Store training labels as JSON. */
    public void setTrainLabels(List<String> labels) {
        try {
            this.trainLabelsJson = new ObjectMapper().writeValueAsString(labels != null ? labels : new ArrayList<>());
        } catch (Exception e) {
            this.trainLabelsJson = "[]";
        }
    }

    /**
     * @return Map of train points (label → list of [x, y]) or empty map.
     */
    public Map<String, List<int[]>> getTrainPoints() {
        if (this.trainPointsJson == null || this.trainPointsJson.isBlank()) return new HashMap<>();
        try {
            return new ObjectMapper().readValue(this.trainPointsJson, new TypeReference<>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /** Store train points as JSON. */
    public void setTrainPoints(Map<String, List<int[]>> points) {
        try {
            this.trainPointsJson = new ObjectMapper().writeValueAsString(points != null ? points : new HashMap<>());
        } catch (Exception e) {
            this.trainPointsJson = "{}";
        }
    }

    /**
     * @return Test points as list (from JSON), or empty list.
     */
    public List<int[]> getTestPoints() {
        if (this.testPointsJson == null || this.testPointsJson.isBlank()) return new ArrayList<>();
        try {
            return new ObjectMapper().readValue(this.testPointsJson, new TypeReference<List<int[]>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /** Store test points as JSON. */
    public void setTestPoints(List<int[]> points) {
        try {
            this.testPointsJson = new ObjectMapper().writeValueAsString(points != null ? points : new ArrayList<>());
        } catch (Exception e) {
            this.testPointsJson = "[]";
        }
    }

    /** Add a train point for a label. */
    public void addTrainPoint(String label, int[] point) {
        Map<String, List<int[]>> points = getTrainPoints();
        points.computeIfAbsent(label, k -> new ArrayList<>()).add(point);
        setTrainPoints(points);
    }

    /** Remove a train point for a label. */
    public void removeTrainPoint(String label, int[] point) {
        Map<String, List<int[]>> points = getTrainPoints();
        if (points.containsKey(label)) {
            points.get(label).removeIf(arr -> Arrays.equals(arr, point));
            setTrainPoints(points);
        }
    }

    // ======== Getters and setters ========

    /**
     * Returns the unique identifier for this task entity.
     *
     * @return The ID.
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique identifier for this task entity.
     *
     * @param id The ID.
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns the logical knn task.
     *
     * @return The associated {@link KnnTask}.
     */
    public KnnTask getTask() {
        return task;
    }

    /**
     * Sets the logical knn task.
     *
     * @param task The {@link KnnTask}.
     */
    public void setTask(KnnTask task) {
        this.task = task;
    }


    /**
     * Returns the value of K (number of neighbors).
     *
     * @return Value of k.
     */
    public Integer getK() {
        return k;
    }

    /**
     * Sets the value of K (number of neighbors).
     *
     * @param k Value of k.
     */
    public void setK(Integer k) {
        this.k = k;
    }

    /**
     * Returns the distance metric.
     *
     * @return Metric.
     */
    public String getMetric() {
        return metric;
    }

    /**
     * Sets the distance metric.
     *
     * @param metric Metric.
     */
    public void setMetric(String metric) {
        this.metric = metric;
    }

    /**
     * Returns the number of training samples.
     *
     * @return Number of training samples.
     */
    public Integer getNumTrain() {
        return numTrain;
    }

    /**
     * Sets the number of training samples.
     *
     * @param numTrain Number of training samples.
     */
    public void setNumTrain(Integer numTrain) {
        this.numTrain = numTrain;
    }

    /**
     * Returns the minimum X value for data points.
     *
     * @return Minimum X.
     */
    public Integer getxMin() {
        return xMin;
    }

    /**
     * Sets the minimum X value for data points.
     *
     * @param xMin Minimum X.
     */
    public void setxMin(Integer xMin) {
        this.xMin = xMin;
    }

    /**
     * Returns the maximum X value for data points.
     *
     * @return Maximum X.
     */
    public Integer getxMax() {
        return xMax;
    }

    /**
     * Sets the maximum X value for data points.
     *
     * @param xMax Maximum X.
     */
    public void setxMax(Integer xMax) {
        this.xMax = xMax;
    }

    /**
     * Returns the minimum Y value for data points.
     *
     * @return Minimum Y.
     */
    public Integer getyMin() {
        return yMin;
    }

    /**
     * Sets the minimum Y value for data points.
     *
     * @param yMin Minimum Y.
     */
    public void setyMin(Integer yMin) {
        this.yMin = yMin;
    }

    /**
     * Returns the maximum Y value for data points.
     *
     * @return Maximum Y.
     */
    public Integer getyMax() {
        return yMax;
    }

    /**
     * Sets the maximum Y value for data points.
     *
     * @param yMax Maximum Y.
     */
    public void setyMax(Integer yMax) {
        this.yMax = yMax;
    }

    /**
     * Returns the number of test points.
     *
     * @return Number of test points.
     */
    public Integer getNumTest() {
        return numTest;
    }

    /**
     * Sets the number of test points.
     *
     * @param numTest Number of test points.
     */
    public void setNumTest(Integer numTest) {
        this.numTest = numTest;
    }

    /**
     * Returns the label for the X-axis.
     *
     * @return X-axis label.
     */
    public String getxLabel() {
        return xLabel;
    }

    /**
     * Sets the label for the X-axis.
     *
     * @param xLabel X-axis label.
     */
    public void setxLabel(String xLabel) {
        this.xLabel = xLabel;
    }

    /**
     * Returns the label for the Y-axis.
     *
     * @return Y-axis label.
     */
    public String getyLabel() {
        return yLabel;
    }

    /**
     * Sets the label for the Y-axis.
     *
     * @param yLabel Y-axis label.
     */
    public void setyLabel(String yLabel) {
        this.yLabel = yLabel;
    }

    /**
     * Returns the tiebreaker setting.
     *
     * @return Tiebreaker.
     */
    public String getTiebreaker() {
        return tiebreaker;
    }

    /**
     * Sets the tiebreaker setting.
     *
     * @param tiebreaker Tiebreaker.
     */
    public void setTiebreaker(String tiebreaker) {
        this.tiebreaker = tiebreaker;
    }

    /**
     * Returns the German base64 PNG of the task statement.
     *
     * @return German task statement PNG as base64.
     */
    public String getAufgabenPngBase64De() {
        return aufgabenPngBase64De;
    }

    /**
     * Sets the German base64 PNG of the task statement.
     *
     * @param aufgabenPngBase64De German task statement PNG as base64.
     */
    public void setAufgabenPngBase64De(String aufgabenPngBase64De) {
        this.aufgabenPngBase64De = aufgabenPngBase64De;
    }

    /**
     * Returns the German base64 PNG of the solution.
     *
     * @return German solution PNG as base64.
     */
    public String getLoesungPngBase64De() {
        return loesungPngBase64De;
    }

    /**
     * Sets the German base64 PNG of the solution.
     *
     * @param loesungPngBase64De German solution PNG as base64.
     */
    public void setLoesungPngBase64De(String loesungPngBase64De) {
        this.loesungPngBase64De = loesungPngBase64De;
    }

    /**
     * Returns the English base64 PNG of the task statement.
     *
     * @return English task statement PNG as base64.
     */
    public String getAufgabenPngBase64En() {
        return aufgabenPngBase64En;
    }

    /**
     * Sets the English base64 PNG of the task statement.
     *
     * @param aufgabenPngBase64En English task statement PNG as base64.
     */
    public void setAufgabenPngBase64En(String aufgabenPngBase64En) {
        this.aufgabenPngBase64En = aufgabenPngBase64En;
    }

    /**
     * Returns the English base64 PNG of the solution.
     *
     * @return English solution PNG as base64.
     */
    public String getLoesungPngBase64En() {
        return loesungPngBase64En;
    }

    /**
     * Sets the English base64 PNG of the solution.
     *
     * @param loesungPngBase64En English solution PNG as base64.
     */
    public void setLoesungPngBase64En(String loesungPngBase64En) {
        this.loesungPngBase64En = loesungPngBase64En;
    }

    /**
     * Returns the serialized JSON for training points.
     *
     * @return Training points JSON string.
     */
    public String getTrainPointsJson() {
        return trainPointsJson;
    }

    /**
     * Sets the serialized JSON for training points.
     *
     * @param trainPointsJson Training points JSON string.
     */
    public void setTrainPointsJson(String trainPointsJson) {
        this.trainPointsJson = trainPointsJson;
    }

    /**
     * Returns the serialized JSON for training labels.
     *
     * @return Training labels JSON string.
     */
    public String getTrainLabelsJson() {
        return trainLabelsJson;
    }

    /**
     * Sets the serialized JSON for training labels.
     *
     * @param trainLabelsJson Training labels JSON string.
     */
    public void setTrainLabelsJson(String trainLabelsJson) {
        this.trainLabelsJson = trainLabelsJson;
    }

    /**
     * Returns the serialized JSON for test points.
     *
     * @return Test points JSON string.
     */
    public String getTestPointsJson() {
        return testPointsJson;
    }

    /**
     * Sets the serialized JSON for test points.
     *
     * @param testPointsJson Test points JSON string.
     */
    public void setTestPointsJson(String testPointsJson) {
        this.testPointsJson = testPointsJson;
    }

    /**
     * Returns the timestamp of creation.
     *
     * @return Creation date/time.
     */
    public OffsetDateTime getCreatedDate() {
        return createdDate;
    }

    /**
     * Sets the timestamp of creation.
     *
     * @param createdDate Creation date/time.
     */
    public void setCreatedDate(OffsetDateTime createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * Returns the timestamp of the last modification.
     *
     * @return Last modification date/time.
     */
    public OffsetDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    /**
     * Sets the timestamp of the last modification.
     *
     * @param lastModifiedDate Last modification date/time.
     */
    public void setLastModifiedDate(OffsetDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
}
