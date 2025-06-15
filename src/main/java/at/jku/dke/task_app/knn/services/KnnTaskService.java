package at.jku.dke.task_app.knn.services;

import at.jku.dke.etutor.task_app.dto.ModifyTaskDto;
import at.jku.dke.etutor.task_app.dto.TaskModificationResponseDto;
import at.jku.dke.etutor.task_app.services.BaseTaskService;
import at.jku.dke.task_app.knn.data.entities.KnnTask;
import at.jku.dke.task_app.knn.data.entities.KnnTaskEntity;
import at.jku.dke.task_app.knn.data.repositories.KnnTaskEntityRepository;
import at.jku.dke.task_app.knn.data.repositories.KnnTaskRepository;
import at.jku.dke.task_app.knn.dto.ModifyKnnTaskDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

/**
 * Service for managing knn tasks.
 * <p>
 * Handles creation, update, and description logic.
 * Most computations are delegated to {@link KnnTaskEntityBuilder} and {@link KnnTaskComputation}.
 * </p>
 */
@Service
public class KnnTaskService extends
    BaseTaskService<
        KnnTask,
        ModifyKnnTaskDto> {

    private final KnnTaskRepository taskRepo;
    private final KnnTaskEntityRepository entityRepo;
    private final KnnTaskComputation computation;
    private final DescriptionBuilder                 descBuilder;

    public KnnTaskService(
        KnnTaskRepository       taskRepo,
        KnnTaskEntityRepository entityRepo,
        KnnTaskComputation computation,
        DescriptionBuilder                 descBuilder) {

        super(taskRepo);
        this.taskRepo     = taskRepo;
        this.entityRepo   = entityRepo;
        this.computation  = computation;
        this.descBuilder  = descBuilder;
    }

    /**
     * Creates a new knn task and its entity, then fills it with calculated solution, images, and description.
     */
    @Override
    protected KnnTask createTask(
        long id,
        ModifyTaskDto<ModifyKnnTaskDto> dto) {

        if (!"knn".equalsIgnoreCase(dto.taskType()))
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Invalid task type");

        var task = new KnnTask(
            id,
            dto.maxPoints(),
            dto.status(),
            null); // Solution is calculated later
        taskRepo.save(task);

        var entity = KnnTaskEntityBuilder
            .build(task, dto.additionalData());

        // Compute solution, images, and description
        computation.fillWithResults(entity);

        entityRepo.save(entity);
        return task;
    }

    /**
     * Updates a knn task and recalculates solution or images if relevant fields change.
     */
    @Override
    protected void updateTask(KnnTask task,
                              ModifyTaskDto<ModifyKnnTaskDto> dto) {

        var entity = entityRepo.findByTaskId(task.getId())
            .orElseThrow(() -> new IllegalStateException("No entity"));

        // Check and apply any changes, recalculate if needed
        boolean recalc = computation.updateEntity(entity, dto.additionalData());

        if (recalc) {
            computation.fillWithResults(entity);
        }

        // Update basic metadata
        task.setMaxPoints(dto.maxPoints());
        task.setStatus(dto.status());

        taskRepo.save(task);
        entityRepo.save(entity);
    }

    /**
     * Returns a response DTO with generated descriptions in German and English.
     */
    @Override
    protected TaskModificationResponseDto mapToReturnData(
        KnnTask task, boolean create) {

        KnnTaskEntity entity = entityRepo
            .findByTaskId(task.getId()).orElseThrow();

        String de = descBuilder.build(Locale.GERMAN , entity, task.getMaxPoints());
        String en = descBuilder.build(Locale.ENGLISH, entity, task.getMaxPoints());

        return new TaskModificationResponseDto(de, en);
    }

}
