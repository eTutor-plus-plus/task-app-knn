package at.jku.dke.task_app.knn.controllers;

import at.jku.dke.etutor.task_app.controllers.BaseTaskController;
import at.jku.dke.task_app.knn.data.entities.KnnTask;
import at.jku.dke.task_app.knn.data.entities.KnnTaskEntity;
import at.jku.dke.task_app.knn.dto.KnnTaskDto;
import at.jku.dke.task_app.knn.dto.ModifyKnnTaskDto;
import at.jku.dke.task_app.knn.services.KnnTaskService;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for knn tasks.
 */
@RestController
public class TaskController
    extends BaseTaskController<KnnTask, KnnTaskDto, ModifyKnnTaskDto> {

    private final KnnTaskService taskService;

    /**
     * Constructor.
     * @param taskService Service for knn tasks.
     */
    public TaskController(KnnTaskService taskService) {
        super(taskService);
        this.taskService = taskService;
    }

    /**
     * Maps a knnTask entity to its DTO.
     */
    @Override
    protected KnnTaskDto mapToDto(KnnTask task) {
        KnnTaskEntity entity = task.getEntity();
        return KnnTaskDto.fromEntities(task, entity);
    }
}
