package at.jku.dke.task_app.knn.services;

import at.jku.dke.task_app.knn.data.entities.KnnTask;
import at.jku.dke.task_app.knn.data.entities.KnnTaskEntity;
import at.jku.dke.task_app.knn.dto.ModifyKnnTaskDto;
import at.jku.dke.task_app.knn.dto.TrainPointGroup;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

/**
 * Utility class for building a new {@link KnnTaskEntity} from DTOs and models.
 */
public final class KnnTaskEntityBuilder {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private KnnTaskEntityBuilder() { /* utility class: do not instantiate */ }

    /**
     * Creates and fills a new {@link KnnTaskEntity} with values from the given DTO.
     * @param task The related knn task.
     * @param ad   The data transfer object with configuration and data.
     * @return A fully initialized entity instance.
     */
    public static KnnTaskEntity build(
        KnnTask task,
        ModifyKnnTaskDto ad) {

        // Check: do not allow more than 7 shapes for the data visualisation
        if (ad.trainLabels() != null && ad.trainLabels().size() > 7) {
            throw new IllegalArgumentException(
                "KNN tasks support at most 7 distinct shapes (trainLabels).");
        }

        KnnTaskEntity e = new KnnTaskEntity();
        e.setTask(task);

        // Copy basic configuration fields
        e.setK(ad.k());
        e.setMetric(ad.metric());
        e.setTiebreaker(ad.tiebreaker());
        e.setxMin(ad.xMin());  e.setxMax(ad.xMax());
        e.setyMin(ad.yMin());  e.setyMax(ad.yMax());
        e.setxLabel(ad.xLabel()); e.setyLabel(ad.yLabel());
        e.setNumTrain(ad.numTrain());
        e.setNumTest (ad.numTest());

        // Training data: use provided or create empty
        if (ad.trainPoints() != null)
            e.setTrainPoints(TrainPointGroup.groupListToMap(ad.trainPoints()));
        else
            e.setTrainPoints(new HashMap<>());

        if (ad.trainLabels() != null)
            e.setTrainLabels(ad.trainLabels());
        else
            e.setTrainLabels(List.of());

        // Test data: use provided or create empty
        if (ad.testPoints() != null)
            e.setTestPoints(ad.testPoints());
        else
            e.setTestPoints(List.of());

        return e;
    }
}
