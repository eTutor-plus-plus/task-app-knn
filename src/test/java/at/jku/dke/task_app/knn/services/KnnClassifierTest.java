package at.jku.dke.task_app.knn.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link KnnClassifier}.
 *
 * Each test uses tiny, hand-crafted point clouds so that the expected
 * behaviour is unambiguous and easy to verify on paper.
 */
class KnnClassifierTest {

    // Basic happy path – k = 1, Euclidean
    @Test
    @DisplayName("k = 1 → returns the single, truly nearest neighbour")
    void nearestNeighbourPrediction() {
        // two clearly separated clusters
        List<int[]> train  = List.of(
            new int[]{2, 2},    // label A
            new int[]{8, 8}     // label B
        );
        List<String> labels = List.of("A", "B");

        KnnClassifier clf = new KnnClassifier(1, 2);     // k = 1, Euclidean
        clf.fit(train, labels);

        var results = clf.predict(List.of(
            new int[]{3, 3},   // closest to (2,2)  → A
            new int[]{8, 7}    // closest to (8,8)  → B
        ));

        assertEquals("A", results.get(0).prediction);
        assertEquals("B", results.get(1).prediction);
        assertEquals(1,   results.get(0).votes.get("A")); // vote consistency
    }


    //Voting majority – clear winner, no tie-break required
    @Test
    @DisplayName("Majority vote wins without tie-break")
    void majorityWins() {
        // three neighbours, two carry label A
        List<int[]> train  = List.of(
            new int[]{1, 0}, new int[]{2, 0},            // A
            new int[]{1, 1}                              // B
        );
        List<String> labels = List.of("A", "A", "B");

        KnnClassifier clf = new KnnClassifier(3, 2);
        clf.fit(train, labels);

        var res = clf.explainPoint(new int[]{0, 0});

        assertEquals("A",       res.prediction);
        assertEquals("majority", res.tieBreakReasonKey);
    }


    // Tie-breaking strategies
    @Nested
    @DisplayName("Tie-break behaviour")
    class TieBreakTests {

        // symmetrical baseline for alphabetical fall-back (A ↔ B at equal dist)
        private final List<int[]> BASE_TRAIN = List.of(
            new int[]{ 1, 0}, new int[]{-1, 0}
        );
        private final List<String> BASE_LABELS = List.of("A", "B");
        private final int[] ORIGIN = new int[]{0, 0};

        @Test
        @DisplayName("nearest-neighbour strategy")
        void tieBreakNearest() {
            /*  two votes A / two votes B –>
             *  A’s nearest = 1   , B’s nearest = 2  → A must win
             */
            List<int[]> train  = List.of(
                new int[]{ 1, 0}, new int[]{ 3, 0},      // A  (nearest = 1)
                new int[]{ -2, 0}, new int[]{-3, 0}       // B  (nearest = 2)
            );
            List<String> lab = List.of("A", "A", "B", "B");

            KnnClassifier clf = new KnnClassifier(4, 2, "nearest");
            clf.fit(train, lab);

            var res = clf.explainPoint(ORIGIN);
            assertEquals("A",              res.prediction);
            assertEquals("nearestNeighbor", res.tieBreakReasonKey);
        }

        @Test
        @DisplayName("mean-distance strategy")
        void tieBreakMean() {
            /*  votes: 2×A vs 2×B
             *  mean(A) = (1 + 2) / 2 = 1.5
             *  mean(B) = (1 + 4) / 2 = 2.5
             */
            List<int[]> train  = List.of(
                new int[]{ 1, 0}, new int[]{ 2, 0},      // A
                new int[]{-1, 0}, new int[]{-4, 0}       // B
            );
            List<String> lab = List.of("A", "A", "B", "B");

            KnnClassifier clf = new KnnClassifier(4, 2, "mean");
            clf.fit(train, lab);

            var res = clf.explainPoint(ORIGIN);
            assertEquals("A",           res.prediction);
            assertEquals("meanDistance", res.tieBreakReasonKey);
        }

        @Test
        @DisplayName("sum-distance strategy")
        void tieBreakSum() {
            /*  votes: 2×A vs 2×B
             *  sum(A) = 1 + 4 = 5
             *  sum(B) = 1 + 2 = 3   → B wins
             */
            List<int[]> train  = List.of(
                new int[]{ 1, 0}, new int[]{ 4, 0},      // A
                new int[]{-1, 0}, new int[]{-2, 0}       // B
            );
            List<String> lab = List.of("A", "A", "B", "B");

            KnnClassifier clf = new KnnClassifier(4, 2, "sum");
            clf.fit(train, lab);

            var res = clf.explainPoint(ORIGIN);
            assertEquals("B",          res.prediction);
            assertEquals("sumDistance", res.tieBreakReasonKey);
        }

        @Test
        @DisplayName("alphabetical fall-back")
        void tieBreakAlphabetical() {
            KnnClassifier clf = new KnnClassifier(2, 2, "sum");  // any strategy
            clf.fit(BASE_TRAIN, BASE_LABELS);

            var res = clf.explainPoint(ORIGIN);
            assertEquals("A",           res.prediction);       // A < B
            assertEquals("alphabetical", res.tieBreakReasonKey);
        }
    }


    // Metric comparison: p = 1 vs 2 vs 3
    @Test
    @DisplayName("Manhattan, Euclidean and Minkowski can all prefer different labels")
    void metricsDiverge() {
        List<int[]> train = List.of(
            new int[]{-10, -2},   // label A – best for Manhattan
            new int[]{ -9, -4},   // label B – best for Euclidean
            new int[]{ -8, -6}    // label C – best for Minkowski
        );
        List<String> labels = List.of("A", "B", "C");

        int[] testPoint = new int[]{0, 0};

        // Build three single-neighbour classifiers with different metrics
        KnnClassifier l1  = new KnnClassifier(1, 1);          // Manhattan
        KnnClassifier l2  = new KnnClassifier(1, 2);          // Euclidean
        KnnClassifier l3  = new KnnClassifier(1, 3);          // Minkowski

        l1.fit(train, labels);
        l2.fit(train, labels);
        l3.fit(train, labels);

        String predL1 = l1.explainPoint(testPoint).prediction;
        String predL2 = l2.explainPoint(testPoint).prediction;
        String predL3 = l3.explainPoint(testPoint).prediction;

        // Expectations derived from hand-calculated distances
        assertEquals("A", predL1, "Manhattan should favour A");
        assertEquals("B", predL2, "Euclidean should favour B");
        assertEquals("C", predL3, "Minkowski (p=3) should favour C");

        // and they really must all differ
        assertNotEquals(predL1, predL2);
        assertNotEquals(predL1, predL3);
        assertNotEquals(predL2, predL3);
    }


    // Edge & error cases
    @Test
    @DisplayName("fit() throws when point/label length differ")
    void fitLengthMismatch() {
        KnnClassifier clf = new KnnClassifier(1, 2);
        List<int[]> pts   = List.of(new int[]{0, 0});
        List<String> lbl  = List.of("A", "B");           // length mismatch

        assertThrows(IllegalArgumentException.class,
            () -> clf.fit(pts, lbl));
    }

    @Test
    @DisplayName("predict() without training data should crash cleanly")
    void predictWithEmptyTraining() {
        KnnClassifier clf = new KnnClassifier(1, 2);
        assertThrows(IndexOutOfBoundsException.class,
            () -> clf.predict(List.of(new int[]{0, 0})));
    }
}
