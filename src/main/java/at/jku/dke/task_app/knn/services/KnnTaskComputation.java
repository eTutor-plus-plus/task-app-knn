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
import java.util.stream.Collectors;

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
     *
     * @param e  The task entity to update.
     * @param ad The incoming data transfer object.
     * @return True if any relevant data changed, else false.
     */
    public boolean updateEntity(KnnTaskEntity e, ModifyKnnTaskDto ad) {

        // prepare labels
        List<String> cleanLabels = null;
        if (ad.trainLabels() != null) {
            LinkedHashSet<String> seen = new LinkedHashSet<>();
            for (String lbl : ad.trainLabels()) {
                String t = lbl == null ? "" : lbl.trim();
                if (!t.isEmpty()) seen.add(t);          // ignore empties, keep order
            }
            cleanLabels = new ArrayList<>(seen);
            // Check: do not allow more than 7 shapes for the data visualisation
            if (ad.trainLabels() != null && ad.trainLabels().size() > 7) {
                throw new IllegalArgumentException(
                    "KNN tasks support at most 7 distinct shapes (trainLabels).");
            }
        }
        // take a snapshot of fields before change
        int oldNumTrain = e.getNumTrain();
        int oldNumTest = e.getNumTest();
        int oldXMin = e.getxMin(), oldXMax = e.getxMax();
        int oldYMin = e.getyMin(), oldYMax = e.getyMax();

        List<String> oldLabels = Optional.ofNullable(e.getTrainLabels()).orElse(List.of());
        Map<String, List<int[]>> oldTrain = Optional.ofNullable(e.getTrainPoints()).orElse(Map.of());
        List<int[]> oldTest = Optional.ofNullable(e.getTestPoints()).orElse(List.of());

        boolean labelsChanged = cleanLabels != null && !Objects.equals(oldLabels, cleanLabels);
        boolean numTrainChanged = oldNumTrain != ad.numTrain();
        boolean numTestChanged = oldNumTest != ad.numTest();
        boolean rangeChanged = oldXMin != ad.xMin() || oldXMax != ad.xMax()
            || oldYMin != ad.yMin() || oldYMax != ad.yMax();

        // Copy basic fields
        e.setK(ad.k());
        e.setMetric(ad.metric());
        e.setTiebreaker(ad.tiebreaker());
        e.setxMin(ad.xMin());
        e.setxMax(ad.xMax());
        e.setyMin(ad.yMin());
        e.setyMax(ad.yMax());
        e.setxLabel(ad.xLabel());
        e.setyLabel(ad.yLabel());
        e.setNumTrain(ad.numTrain());
        e.setNumTest(ad.numTest());

        boolean changed = false;

        // DTO-train handling
        Map<String, List<int[]>> dtoTrain = null;
        boolean dtoFits = false;
        if (ad.trainPoints() != null) {
            dtoTrain = TrainPointGroup.groupListToMap(ad.trainPoints());
            dtoFits = dtoTrain.values().stream().allMatch(l -> l.size() == ad.numTrain());
        }


        // numTrain changed  -> full regeneration (unless DTO exact)
        if (numTrainChanged && !dtoFits) {
            List<String> lbls = ad.trainLabels() != null ? ad.trainLabels() : oldLabels;
            e.setTrainPoints(
                KnnDataGenerator.generateTrainData(
                    ad.numTrain(), ad.xMin(), ad.xMax(), ad.yMin(), ad.yMax(),
                    lbls.toArray(new String[0])
                )
            );
            if (labelsChanged) e.setTrainLabels(cleanLabels);
            changed = true;
        }


        // Label rename -> remap old lists
        else if (labelsChanged) {
            List<String> newL = cleanLabels;                     // <‑ statt ad.trainLabels()
            Map<String, List<int[]>> tmp = new LinkedHashMap<>();

            for (int i = 0; i < newL.size(); i++) {
                String nl = newL.get(i);
                List<int[]> pts = dtoTrain != null ? dtoTrain.get(nl) : null;
                if ((pts == null || pts.isEmpty()) && i < oldLabels.size())
                    pts = oldTrain.getOrDefault(oldLabels.get(i), List.of());
                tmp.put(nl, pts != null ? new ArrayList<>(pts) : new ArrayList<>());
            }

            Set<String> used = KnnDataGenerator.trainingPointsToSet(tmp);
            Random rnd = new Random();
            for (String lbl : newL) {
                List<int[]> list = tmp.get(lbl);
                while (list.size() < ad.numTrain()) {
                    int x = rnd.nextInt(ad.xMax() - ad.xMin() + 1) + ad.xMin();
                    int y = rnd.nextInt(ad.yMax() - ad.yMin() + 1) + ad.yMin();
                    String key = x + "/" + y;
                    if (used.add(key))
                        list.add(new int[]{x, y});
                }
            }

            e.setTrainLabels(newL);
            e.setTrainPoints(tmp);
            changed = true;
        }

        // DTO provided and counts are correct -> accept
        else if (dtoTrain != null) {
            Set<String> allowed = new HashSet<>(e.getTrainLabels());
            dtoTrain.keySet().removeIf(k -> !allowed.contains(k));
            for (String l : allowed) dtoTrain.putIfAbsent(l, new ArrayList<>());
            e.setTrainPoints(dtoTrain);
            changed = true;
        }

        // size mismatch -> regenerate
        else if (e.getTrainPoints().values().stream().anyMatch(l -> l.size() != ad.numTrain())) {
            e.setTrainPoints(
                KnnDataGenerator.generateTrainData(
                    ad.numTrain(), ad.xMin(), ad.xMax(), ad.yMin(), ad.yMax(),
                    e.getTrainLabels().toArray(new String[0])
                )
            );
            changed = true;
        }

        // axis ranged changed
        if (rangeChanged) {
            Map<String, List<int[]>> fixed = new LinkedHashMap<>();
            Set<String> used = new HashSet<>();           // global grid‑keys
            Random rnd = new Random();

            // keep only points in range
            for (String lbl : e.getTrainLabels()) {
                List<int[]> kept = e.getTrainPoints().getOrDefault(lbl, List.of())
                    .stream()
                    .filter(p -> p[0] >= ad.xMin() && p[0] <= ad.xMax()
                        && p[1] >= ad.yMin() && p[1] <= ad.yMax())
                    .peek(p -> used.add(p[0] + "/" + p[1]))
                    .collect(Collectors.toCollection(ArrayList::new));
                fixed.put(lbl, kept);
            }

            // refill points
            for (String lbl : e.getTrainLabels()) {
                List<int[]> list = fixed.get(lbl);
                while (list.size() < ad.numTrain()) {
                    int x = rnd.nextInt(ad.xMax() - ad.xMin() + 1) + ad.xMin();
                    int y = rnd.nextInt(ad.yMax() - ad.yMin() + 1) + ad.yMin();
                    String key = x + "/" + y;
                    if (used.add(key))               // only if still free!
                        list.add(new int[]{x, y});
                }
            }

            e.setTrainPoints(fixed);
            changed = true;
        }

        // test point range and duplication handling
        {
            List<int[]> baseTest = (ad.testPoints() != null)
                ? new ArrayList<>(ad.testPoints())
                : new ArrayList<>(e.getTestPoints());

            Set<String> used = KnnDataGenerator.trainingPointsToSet(e.getTrainPoints());

            List<int[]> uniqTest = new ArrayList<>();
            for (int[] p : baseTest) {
                String k = p[0] + "/" + p[1];
                if (!used.contains(k)
                    && p[0] >= ad.xMin() && p[0] <= ad.xMax()
                    && p[1] >= ad.yMin() && p[1] <= ad.yMax()
                    && used.add(k))
                    uniqTest.add(p);
            }

            Random rnd = new Random();
            if (uniqTest.size() > ad.numTest())
                uniqTest = uniqTest.subList(0, ad.numTest());
            while (uniqTest.size() < ad.numTest()) {
                int x = rnd.nextInt(ad.xMax() - ad.xMin() + 1) + ad.xMin();
                int y = rnd.nextInt(ad.yMax() - ad.yMin() + 1) + ad.yMin();
                String k = x + "/" + y;
                if (used.add(k))
                    uniqTest.add(new int[]{x, y});
            }

            if (!Objects.equals(e.getTestPoints(), uniqTest)) {
                e.setTestPoints(uniqTest);
                changed = true;
            }
        }

        // manual duplicates fail
        {
            Set<String> seen = new HashSet<>();

            if (dtoTrain != null) {
                for (var list : dtoTrain.values())
                    for (int[] p : list)
                        if (!seen.add(p[0] + "/" + p[1]))
                            throw new IllegalArgumentException("Duplicate train point " +
                                Arrays.toString(p));
            }

            if (ad.testPoints() != null) {
                for (int[] p : ad.testPoints()) {
                    String k = p[0] + "/" + p[1];
                    if (!seen.add(k))
                        throw new IllegalArgumentException("Duplicate test point " +
                            Arrays.toString(p));
                }
            }
        }

        return changed;
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
