package at.jku.dke.task_app.knn.services;

import java.util.*;

/**
 * Utility class for generating unique 2D training and test points for KNN tasks.
 * <p>
 * Ensures that all generated points are unique and test points do not overlap with training points.
 * </p>
 */
public class KnnDataGenerator {
    /**
     * Generates unique random training data for each class label.
     * Each class gets {@code numPerClass} unique points within the 2D grid.
     *
     * @param numPerClass Number of training points per class.
     * @param xMin        Minimum x coordinate.
     * @param xMax        Maximum x coordinate.
     * @param yMin        Minimum y coordinate.
     * @param yMax        Maximum y coordinate.
     * @param labels      Array of class labels.
     * @return Map from label to list of 2D points (int[] of length 2).
     * @throws IllegalArgumentException if not enough unique points are available.
     */
    public static Map<String, List<int[]>> generateTrainData(
        int numPerClass, int xMin, int xMax, int yMin, int yMax, String[] labels) {

        Map<String, List<int[]>> result = new HashMap<>();
        Set<String> usedPoints = new HashSet<>();
        Random rnd = new Random();

        int maxPossiblePoints = (xMax - xMin + 1) * (yMax - yMin + 1);
        int totalNeeded = numPerClass * labels.length;
        if (totalNeeded > maxPossiblePoints)
            throw new IllegalArgumentException("Not enough unique points in grid for the chosen amount!");

        for (String label : labels) {
            List<int[]> points = new ArrayList<>();
            while (points.size() < numPerClass) {
                int x = rnd.nextInt(xMax - xMin + 1) + xMin;
                int y = rnd.nextInt(yMax - yMin + 1) + yMin;
                String key = x + "/" + y;
                if (!usedPoints.contains(key)) {
                    points.add(new int[]{x, y});
                    usedPoints.add(key);
                }
            }
            result.put(label, points);
        }
        return result;
    }

    /**
     * Generates a list of unique test points, avoiding overlap with given training points.
     *
     * @param num           Number of test points to generate.
     * @param xMin          Minimum x coordinate.
     * @param xMax          Maximum x coordinate.
     * @param yMin          Minimum y coordinate.
     * @param yMax          Maximum y coordinate.
     * @param usedTrainings Set of "x/y" strings used by training points.
     * @return List of unique test points (int[] of length 2).
     * @throws IllegalArgumentException if not enough available points exist.
     */
    public static List<int[]> generateTestPoints(int num, int xMin, int xMax, int yMin, int yMax, Set<String> usedTrainings) {
        List<int[]> points = new ArrayList<>();
        Random rnd = new Random();

        int maxPossiblePoints = (xMax - xMin + 1) * (yMax - yMin + 1);
        int possible = maxPossiblePoints - usedTrainings.size();
        if (num > possible)
            throw new IllegalArgumentException("Not enough gridpoints available for chosen amount of test points!");

        Set<String> used = new HashSet<>(usedTrainings);

        while (points.size() < num) {
            int x = rnd.nextInt(xMax - xMin + 1) + xMin;
            int y = rnd.nextInt(yMax - yMin + 1) + yMin;
            String key = x + "/" + y;
            if (!used.contains(key)) {
                points.add(new int[]{x, y});
                used.add(key);
            }
        }
        return points;
    }

    /**
     * Converts a map of training points to a set of "x/y" coordinate strings for uniqueness checks.
     *
     *
     * @param trainMap Map from label to list of points.
     * @return Set of "x/y" coordinate strings.
     */
    public static Set<String> trainingPointsToSet(Map<String, List<int[]>> trainMap) {
        Set<String> result = new HashSet<>();
        for (List<int[]> list : trainMap.values()) {
            for (int[] pt : list) {
                result.add(pt[0] + "/" + pt[1]);
            }
        }
        return result;
    }
}
