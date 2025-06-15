package at.jku.dke.task_app.knn.services;

import java.util.*;
import java.util.List;

/**
 * A transparent KNN (k-nearest neighbors) classifier for 2D integer points with explainability and fair tie-breaking.
 * <p>
 * Supports Manhattan, Euclidean, and Minkowski distances. Implements various tie-breaking strategies ("sum", "mean", "nearest", "alphabetical").
 * Produces detailed explanations for each knn with neighbor information and decision reasoning.
 * </p>
 */
public class KnnClassifier {

    private int k;              // Number of neighbors
    private int p;              // Distance metric: 1 = Manhattan, 2 = Euclidean, >2 = Minkowski
    private String tiebreaker;  // Tie-break strategy
    private List<int[]> trainPoints;
    private List<String> trainLabels;

    /**
     * Full constructor for specifying all KNN options.
     * @param k          Number of neighbors (k).
     * @param p          Distance metric (1=Manhattan, 2=Euclidean, >2=Minkowski).
     * @param tiebreaker Tie-breaker ("sum", "mean", "nearest", or "alphabetical").
     */
    public KnnClassifier(int k, int p, String tiebreaker) {
        this.k = k;
        this.p = p;
        this.tiebreaker = tiebreaker == null ? "sum" : tiebreaker.toLowerCase();
        this.trainPoints = new ArrayList<>();
        this.trainLabels = new ArrayList<>();
    }

    /**
     * Simple constructor, defaults tie-breaker to "sum".
     * @param k Number of neighbors.
     * @param p Distance metric.
     */
    public KnnClassifier(int k, int p) {
        this(k, p, "sum");
    }

    /**
     * Fits the classifier to the given training data.
     * @param X List of 2D integer points (features).
     * @param y Corresponding class labels.
     * @throws IllegalArgumentException if X and y have different lengths.
     */
    public void fit(List<int[]> X, List<String> y) {
        if (X.size() != y.size())
            throw new IllegalArgumentException("Point and label list must be of equal length.");
        this.trainPoints = new ArrayList<>(X);
        this.trainLabels = new ArrayList<>(y);
    }

    /**
     * Computes the distance between two 2D points.
     */
    private double distance(int[] a, int[] b) {
        int dx = Math.abs(a[0] - b[0]);
        int dy = Math.abs(a[1] - b[1]);
        if (p == 1) return dx + dy;                         // Manhattan
        if (p == 2) return Math.sqrt(dx * dx + dy * dy);    // Euclidean
        return Math.pow(Math.pow(dx, p) + Math.pow(dy, p), 1.0 / p); // Minkowski
    }

    /**
     * Classifies and explains a single test point using the default tie-breaker.
     * @param testPoint The 2D point to classify.
     * @return Full KNN result with explanation.
     */
    public KNNResult explainPoint(int[] testPoint) {
        return explainPoint(testPoint, this.tiebreaker);
    }

    /**
     * Classifies and explains a single test point with a given tie-breaker.
     * @param testPoint  The 2D point to classify.
     * @param tiebreaker Tie-breaking method: "sum", "mean", "nearest", or "alphabetical".
     * @return Full KNN result with all details.
     */
    public KNNResult explainPoint(int[] testPoint, String tiebreaker) {
        String usedTiebreaker = (tiebreaker == null || tiebreaker.isBlank()) ? "sum" : tiebreaker.toLowerCase();

        // Compute distances to all training points.
        List<Neighbor> neighbors = new ArrayList<>();
        for (int i = 0; i < trainPoints.size(); i++) {
            double dist = distance(testPoint, trainPoints.get(i));
            neighbors.add(new Neighbor(i, trainLabels.get(i), dist));
        }
        // Sort by distance (closest first).
        neighbors.sort(Comparator.comparingDouble(n -> n.dist));

        // Select the k nearest neighbors (handle ties at k-th distance).
        double kthDist = neighbors.get(Math.min(k - 1, neighbors.size() - 1)).dist;
        List<Neighbor> selected = new ArrayList<>();
        for (Neighbor n : neighbors) {
            if (n.dist <= kthDist) selected.add(n);
        }

        // Count votes and distances for each label.
        Map<String, Integer> voteCounter = new HashMap<>();
        Map<String, Double> classDistSum = new HashMap<>();
        Map<String, Double> classDistMean = new HashMap<>();
        Map<String, Double> classDistNearest = new HashMap<>();
        for (Neighbor n : selected) {
            voteCounter.put(n.label, voteCounter.getOrDefault(n.label, 0) + 1);
            classDistSum.put(n.label, classDistSum.getOrDefault(n.label, 0.0) + n.dist);
            classDistMean.put(n.label, classDistMean.getOrDefault(n.label, 0.0) + n.dist);
            if (!classDistNearest.containsKey(n.label) || n.dist < classDistNearest.get(n.label)) {
                classDistNearest.put(n.label, n.dist);
            }
        }
        // Compute mean distances.
        for (String label : classDistMean.keySet()) {
            classDistMean.put(label, classDistMean.get(label) / voteCounter.get(label));
        }

        // Sort labels by vote count.
        List<Map.Entry<String, Integer>> sortedVotes = new ArrayList<>(voteCounter.entrySet());
        sortedVotes.sort((a, b) -> b.getValue() - a.getValue());

        String finalLabel;
        String tieBreakReasonKey;

        // Majority rule: if clear winner, use it.
        if (sortedVotes.size() == 1 || sortedVotes.get(0).getValue() > sortedVotes.get(1).getValue()) {
            finalLabel = sortedVotes.get(0).getKey();
            tieBreakReasonKey = "majority";
        } else {
            // Tie: collect all labels with max votes.
            List<String> tied = new ArrayList<>();
            int maxVotes = sortedVotes.get(0).getValue();
            for (Map.Entry<String, Integer> entry : sortedVotes) {
                if (entry.getValue() == maxVotes)
                    tied.add(entry.getKey());
            }

            // Resolve tie by the chosen strategy.
            switch (usedTiebreaker) {
                case "mean" -> {
                    double minValue = tied.stream().mapToDouble(classDistMean::get).min().orElse(Double.MAX_VALUE);
                    tied.removeIf(label -> classDistMean.get(label) > minValue);
                    tieBreakReasonKey = "meanDistance";
                }
                case "nearest" -> {
                    double minValue = tied.stream().mapToDouble(classDistNearest::get).min().orElse(Double.MAX_VALUE);
                    tied.removeIf(label -> classDistNearest.get(label) > minValue);
                    tieBreakReasonKey = "nearestNeighbor";
                }
                case "sum" -> {
                    double minValue = tied.stream().mapToDouble(classDistSum::get).min().orElse(Double.MAX_VALUE);
                    tied.removeIf(label -> classDistSum.get(label) > minValue);
                    tieBreakReasonKey = "sumDistance";
                }
                default -> {
                    double minValue = tied.stream().mapToDouble(classDistSum::get).min().orElse(Double.MAX_VALUE);
                    tied.removeIf(label -> classDistSum.get(label) > minValue);
                    tieBreakReasonKey = "sumDistance";
                }
            }
            // If still tied, pick alphabetically.
            if (tied.size() == 1) {
                finalLabel = tied.get(0);
            } else {
                Collections.sort(tied);
                finalLabel = tied.get(0);
                tieBreakReasonKey = "alphabetical";
            }
        }
        // Return the explanation object.
        return new KNNResult(finalLabel, selected, voteCounter, tieBreakReasonKey);
    }

    /**
     * Predicts and explains all test points.
     * @param testPoints List of test points to classify.
     * @return List of detailed KNN results, one per test point.
     */
    public List<KNNResult> predict(List<int[]> testPoints) {
        List<KNNResult> results = new ArrayList<>();
        for (int[] x : testPoints)
            results.add(explainPoint(x, this.tiebreaker));
        return results;
    }

    /**
     * Represents a training neighbor for explanation purposes.
     */
    public static class Neighbor {
        public final int index;     // Index in training set
        public final String label;  // Class label
        public final double dist;   // Distance to test point

        public Neighbor(int index, String label, double dist) {
            this.index = index;
            this.label = label;
            this.dist = dist;
        }
    }

    /**
     * Encapsulates the result of a KNN prediction, including prediction, neighbors, votes, and tie-breaking reason.
     */
    public static class KNNResult {
        public final String prediction;           // Predicted class
        public final List<Neighbor> neighbors;    // Neighbors used in decision
        public final Map<String, Integer> votes;  // Votes per label
        public final String tieBreakReasonKey;    // Key explaining the tie-break

        public KNNResult(String prediction, List<Neighbor> neighbors, Map<String, Integer> votes, String tieBreakReasonKey) {
            this.prediction = prediction;
            this.neighbors = neighbors;
            this.votes = votes;
            this.tieBreakReasonKey = tieBreakReasonKey;
        }
    }
}
