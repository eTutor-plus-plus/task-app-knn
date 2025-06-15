package at.jku.dke.task_app.knn.services;

import at.jku.dke.task_app.knn.data.entities.KnnTask;
import at.jku.dke.task_app.knn.data.entities.KnnTaskEntity;
import at.jku.dke.task_app.knn.dto.ModifyKnnTaskDto;
import at.jku.dke.task_app.knn.dto.TrainPointGroup;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;

/**
 * Computes, updates, and generates knn task data.
 * <p>
 * - Updates and validates entity fields
 * - Generates missing training and test data
 * - Computes KNN, images, and descriptions
 * </p>
 */
@Component
public final class KnnTaskComputation {

    private final DescriptionBuilder descBuilder;

    public KnnTaskComputation(DescriptionBuilder descBuilder) {
        this.descBuilder = descBuilder;
    }

    /**
     * Updates entity fields and regenerates data as needed.
     * @param e  The task entity to update.
     * @param ad The incoming data transfer object.
     * @return True if any relevant data changed, else false.
     */
    public boolean updateEntity(KnnTaskEntity e, ModifyKnnTaskDto ad) {

        // Check for any change in basic configuration fields
        boolean changed =
            !Objects.equals(e.getK(),           ad.k())           ||
                !Objects.equals(e.getMetric(),      ad.metric())      ||
                !Objects.equals(e.getTiebreaker(),  ad.tiebreaker())  ||
                !Objects.equals(e.getxMin(),        ad.xMin())        ||
                !Objects.equals(e.getxMax(),        ad.xMax())        ||
                !Objects.equals(e.getyMin(),        ad.yMin())        ||
                !Objects.equals(e.getyMax(),        ad.yMax())        ||
                !Objects.equals(e.getNumTrain(),    ad.numTrain())    ||
                !Objects.equals(e.getNumTest(),     ad.numTest());

        // Copy basic fields
        e.setK(ad.k());
        e.setMetric(ad.metric());
        e.setTiebreaker(ad.tiebreaker());
        e.setxMin(ad.xMin());  e.setxMax(ad.xMax());
        e.setyMin(ad.yMin());  e.setyMax(ad.yMax());
        e.setxLabel(ad.xLabel()); e.setyLabel(ad.yLabel());
        e.setNumTrain(ad.numTrain());
        e.setNumTest (ad.numTest());

        // Handle training points
        if (ad.trainLabels() != null)
            e.setTrainLabels(ad.trainLabels());

        if (ad.trainPoints() != null) {
            // Check if number of points matches the expected count
            int expected = ad.numTrain() * e.getTrainLabels().size();
            int actual   = ad.trainPoints().stream().mapToInt(g -> g.getPoints().size()).sum();

            if (expected != actual) {
                // Mismatch: generate new training data
                e.setTrainPoints(
                    KnnDataGenerator.generateTrainData(
                        ad.numTrain(),
                        ad.xMin(), ad.xMax(),
                        ad.yMin(), ad.yMax(),
                        e.getTrainLabels().toArray(new String[0])));
                changed = true;
            } else {
                // Use provided training points, check if changed
                Map<String,List<int[]>> old = e.getTrainPoints();
                Map<String,List<int[]>> neu = TrainPointGroup.groupListToMap(ad.trainPoints());
                if (!Objects.equals(old, neu)) changed = true;
                e.setTrainPoints(neu);
            }
        } else if (e.getTrainPoints().size() != ad.numTrain()) {
            // No list provided, but number changed: generate new data
            e.setTrainPoints(
                KnnDataGenerator.generateTrainData(
                    ad.numTrain(),
                    ad.xMin(), ad.xMax(),
                    ad.yMin(), ad.yMax(),
                    e.getTrainLabels().toArray(new String[0])));
            changed = true;
        }

        // Handle test points (same logic as training points)
        if (ad.testPoints() != null) {
            if (ad.testPoints().size() != ad.numTest()) {
                // Mismatch: generate new test data
                e.setTestPoints(
                    KnnDataGenerator.generateTestPoints(
                        ad.numTest(),
                        ad.xMin(), ad.xMax(),
                        ad.yMin(), ad.yMax(),
                        KnnDataGenerator.trainingPointsToSet(e.getTrainPoints())));
                changed = true;
            } else {
                if (!Objects.equals(e.getTestPoints(), ad.testPoints()))
                    changed = true;
                e.setTestPoints(ad.testPoints());
            }
        } else if (e.getTestPoints().size() != ad.numTest()) {
            e.setTestPoints(
                KnnDataGenerator.generateTestPoints(
                    ad.numTest(),
                    ad.xMin(), ad.xMax(),
                    ad.yMin(), ad.yMax(),
                    KnnDataGenerator.trainingPointsToSet(e.getTrainPoints())));
            changed = true;
        }

        return changed; // Triggers regeneration of images and solution if true
    }

    /**
     * Fills the entity with computed results: solution, images, and descriptions.
     * @param e The knn task entity.
     */
    public void fillWithResults(KnnTaskEntity e) {

        KnnTask task = e.getTask();

        // Ensure required data exists
        ensureDataCompleteness(e);

        // Run KNN to get predictions
        var knnResults = KnnHelper.classify(e);

        // Store solution as CSV
        String csv = knnResults.stream()
            .map(r -> r.prediction)
            .reduce((a, b) -> a + "," + b)
            .orElse("");
        task.setSolution(csv);

        // Generate PNG images for both languages and solution/task
        Map<String, List<int[]>> train = e.getTrainPoints();
        List<int[]> test = e.getTestPoints();

        String taskDe = toBase64(KnnPngExporter.generateKnnImage(
            e.getxMin(), e.getxMax(), e.getyMin(), e.getyMax(),
            train, test, null, false, "de",
            e.getxLabel(), e.getyLabel()));
        String taskEn = toBase64(KnnPngExporter.generateKnnImage(
            e.getxMin(), e.getxMax(), e.getyMin(), e.getyMax(),
            train, test, null, false, "en",
            e.getxLabel(), e.getyLabel()));
        String solDe = toBase64(KnnPngExporter.generateKnnImage(
            e.getxMin(), e.getxMax(), e.getyMin(), e.getyMax(),
            train, test, knnResults, true, "de",
            e.getxLabel(), e.getyLabel()));
        String solEn = toBase64(KnnPngExporter.generateKnnImage(
            e.getxMin(), e.getxMax(), e.getyMin(), e.getyMax(),
            train, test, knnResults, true, "en",
            e.getxLabel(), e.getyLabel()));

        e.setAufgabenPngBase64De(taskDe);
        e.setAufgabenPngBase64En(taskEn);
        e.setLoesungPngBase64De(solDe);
        e.setLoesungPngBase64En(solEn);

        // Generate descriptions for both languages
        String descDe = descBuilder.build(Locale.GERMAN , e, task.getMaxPoints());
        String descEn = descBuilder.build(Locale.ENGLISH, e, task.getMaxPoints());

        task.setDescriptionDe(descDe);
        task.setDescriptionEn(descEn);
    }

    /** Ensures required training and test data exist; generates if missing. */
    private static void ensureDataCompleteness(KnnTaskEntity e) {
        int total = e.getTrainPoints().values()
            .stream().mapToInt(List::size).sum();

        if (total == 0) {
            e.setTrainPoints(
                KnnDataGenerator.generateTrainData(
                    e.getNumTrain(),
                    e.getxMin(), e.getxMax(),
                    e.getyMin(), e.getyMax(),
                    labelArray(e)));
        }

        if (e.getTestPoints().isEmpty()) {
            e.setTestPoints(
                KnnDataGenerator.generateTestPoints(
                    e.getNumTest(),
                    e.getxMin(), e.getxMax(),
                    e.getyMin(), e.getyMax(),
                    KnnDataGenerator.trainingPointsToSet(e.getTrainPoints())));
        }
    }

    /** Returns the label array for training data, or ["A", "B"] if labels are missing. */
    private static String[] labelArray(KnnTaskEntity e) {
        List<String> lbl = e.getTrainLabels();
        if (lbl == null || lbl.isEmpty()) lbl = List.of("A", "B");
        return lbl.toArray(new String[0]);
    }

    /** Returns true if data has changed. */
    private static boolean dataChanged(Object oldData, Object newData) {
        return newData != null && !Objects.equals(oldData, newData);
    }

    /** Converts a BufferedImage to a base64-encoded PNG string. */
    private static String toBase64(BufferedImage img) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", bos);
            return Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (Exception ex) {
            throw new IllegalStateException("PNG to Base64 failed", ex);
        }
    }
}
