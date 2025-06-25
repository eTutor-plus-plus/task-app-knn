package at.jku.dke.task_app.knn.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link KnnDataGenerator}.
 * The class covers:
 *   • uniqueness & disjointness
 *   • per–label counts
 *   • coordinate-range checks
 *   • error handling if grid is to small
 *   • correct conversion through {@code trainingPointsToSet}
 */
class KnnDataGeneratorTest {

    /* ------------------------------------------------------------------ */
    /*  uniqueness & disjointness        */
    /* ------------------------------------------------------------------ */
    @Test
    @DisplayName("train/test sets are unique and disjoint")
    void trainAndTestPointsAreUniqueAndDisjoint() {
        int numTrainPerClass = 8, numTest = 9;
        int xMin = 1, xMax = 5, yMin = 1, yMax = 5;
        String[] labels = {"A", "B"};

        // ---------- generate data ----------
        Map<String, List<int[]>> trainMap = KnnDataGenerator.generateTrainData(
            numTrainPerClass, xMin, xMax, yMin, yMax, labels);
        Set<String> trainSet = KnnDataGenerator.trainingPointsToSet(trainMap);
        List<int[]> testPts  = KnnDataGenerator.generateTestPoints(
            numTest, xMin, xMax, yMin, yMax, trainSet);

        // ---------- uniqueness -------------
        assertEquals(numTrainPerClass * labels.length, trainSet.size(),
            "duplicate training points detected");
        Set<String> testSet = new HashSet<>();
        for (int[] p : testPts) assertTrue(testSet.add(p[0] + "/" + p[1]),
            "duplicate test point detected");
        assertEquals(numTest, testSet.size(), "test set size mismatch");

        // ---------- disjointness ----------
        for (String key : testSet)
            assertFalse(trainSet.contains(key), "overlap between train & test: " + key);
    }

    /* ------------------------------------------------------------------ */
    /* Per-label count & coordinate bounds                              */
    /* ------------------------------------------------------------------ */
    @Test
    @DisplayName("each label gets exactly N points and all points respect bounds")
    void labelCountAndBounds() {
        int numPerClass = 4;
        int xMin = -2, xMax = 1, yMin = 0, yMax = 5;
        String[] labels = {"A", "B", "C"};

        Map<String, List<int[]>> train = KnnDataGenerator.generateTrainData(
            numPerClass, xMin, xMax, yMin, yMax, labels);

        // correct number per class --
        train.forEach((lbl, pts) ->
            assertEquals(numPerClass, pts.size(),
                "wrong count for label " + lbl));

        // all points inside the grid --
        for (List<int[]> pts : train.values()) {
            for (int[] p : pts) {
                assertTrue(p[0] >= xMin && p[0] <= xMax,
                    "x out of range: " + Arrays.toString(p));
                assertTrue(p[1] >= yMin && p[1] <= yMax,
                    "y out of range: " + Arrays.toString(p));
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /* Error handling: grid too small                                   */
    /* ------------------------------------------------------------------ */
    @Nested
    @DisplayName("error scenarios")
    class ErrorScenarios {

        @Test
        @DisplayName("train generation fails when grid is too small")
        void gridTooSmallForTraining() {
            // grid has only 4 possible coordinates
            int xMin = 0, xMax = 1, yMin = 0, yMax = 1;
            assertThrows(IllegalArgumentException.class, () ->
                KnnDataGenerator.generateTrainData(
                    3, xMin, xMax, yMin, yMax, new String[]{"A", "B"}));
        }

        @Test
        @DisplayName("test generation fails when not enough free points left")
        void gridTooSmallForTest() {
            int xMin = 0, xMax = 1, yMin = 0, yMax = 0;
            Map<String, List<int[]>> train = KnnDataGenerator.generateTrainData(
                1, xMin, xMax, yMin, yMax, new String[]{"A"});
            Set<String> used = KnnDataGenerator.trainingPointsToSet(train);
            // only one free point available, should crash
            assertThrows(IllegalArgumentException.class, () ->
                KnnDataGenerator.generateTestPoints(
                    2, xMin, xMax, yMin, yMax, used));
        }
    }

    /* ------------------------------------------------------------------ */
    /* trainingPointsToSet – correctness & no loss                      */
    /* ------------------------------------------------------------------ */
    @Test
    @DisplayName("trainingPointsToSet produces complete, duplicate-free set")
    void trainingPointsToSetConversion() {
        Map<String, List<int[]>> map = Map.of(
            "A", List.of(new int[]{0, 0}, new int[]{1, 1}),
            "B", List.of(new int[]{2, 2}));
        Set<String> set = KnnDataGenerator.trainingPointsToSet(map);

        // expected encoded coordinates
        Set<String> expected = Set.of("0/0", "1/1", "2/2");

        assertEquals(expected, set, "conversion lost or duplicated points");
    }
}
