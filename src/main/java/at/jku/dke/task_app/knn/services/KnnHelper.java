package at.jku.dke.task_app.knn.services;

import at.jku.dke.task_app.knn.data.entities.KnnTaskEntity;

import java.util.*;

/**
 * Utility class to help run KNN for all test points in a given entity.
 */
public final class KnnHelper {

    private KnnHelper() { }

    /**
     * Runs the KnnClassifier for all test points in the given entity.
     * Flattens training data, determines the distance metric, fits the classifier, and returns results.
     *
     * @param e The knn task entity with training and test data.
     * @return List of KNN results for all test points.
     */
    public static List<KnnClassifier.KNNResult> classify(KnnTaskEntity e) {

        // Flatten the training points and their labels
        List<int[]> flatPts  = new ArrayList<>();
        List<String> flatLbls = new ArrayList<>();
        e.getTrainPoints().forEach((lbl, pts) -> {
            flatPts.addAll(pts);
            flatLbls.addAll(Collections.nCopies(pts.size(), lbl));
        });

        // Determine distance metric: 1 = Manhattan, 2 = Euclidean, 3 = Minkowski (fallback)
        int p = switch (Optional.ofNullable(e.getMetric()).orElse("euclidean").toLowerCase()) {
            case "manhattan" -> 1;
            case "euclidean" -> 2;
            default          -> 3;
        };

        // Train classifier and predict all test points
        KnnClassifier knn = new KnnClassifier(
            Optional.ofNullable(e.getK()).orElse(3), // Default k=3 if not set
            p,
            e.getTiebreaker());

        knn.fit(flatPts, flatLbls);

        return knn.predict(e.getTestPoints());
    }
}
