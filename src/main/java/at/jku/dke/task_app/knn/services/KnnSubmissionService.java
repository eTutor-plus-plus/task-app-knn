package at.jku.dke.task_app.knn.services;

import at.jku.dke.etutor.task_app.dto.GradingDto;
import at.jku.dke.etutor.task_app.dto.SubmitSubmissionDto;
import at.jku.dke.etutor.task_app.services.BaseSubmissionService;
import at.jku.dke.task_app.knn.data.entities.KnnSubmission;
import at.jku.dke.task_app.knn.data.entities.KnnTask;
import at.jku.dke.task_app.knn.data.repositories.KnnSubmissionRepository;
import at.jku.dke.task_app.knn.data.repositories.KnnTaskRepository;
import at.jku.dke.task_app.knn.dto.KnnSubmissionDto;
import at.jku.dke.task_app.knn.evaluation.EvaluationService;
import org.springframework.stereotype.Service;

/**
 * This class provides methods for managing {@link KnnSubmission}s.
 */
@Service
public class KnnSubmissionService extends BaseSubmissionService<KnnTask, KnnSubmission, KnnSubmissionDto> {

    private final EvaluationService evaluationService;

    /**
     * Creates a new instance of class {@link KnnSubmissionService}.
     *
     * @param submissionRepository The input repository.
     * @param taskRepository       The task repository.
     * @param evaluationService    The evaluation service.
     */
    public KnnSubmissionService(KnnSubmissionRepository submissionRepository, KnnTaskRepository taskRepository, EvaluationService evaluationService) {
        super(submissionRepository, taskRepository);
        this.evaluationService = evaluationService;
    }

    @Override
    protected KnnSubmission createSubmissionEntity(SubmitSubmissionDto<KnnSubmissionDto> submitSubmissionDto) {
        return new KnnSubmission(submitSubmissionDto.submission().input());
    }

    @Override
    protected GradingDto evaluate(SubmitSubmissionDto<KnnSubmissionDto> submitSubmissionDto) {
        return this.evaluationService.evaluate(submitSubmissionDto);
    }

    @Override
    protected KnnSubmissionDto mapSubmissionToSubmissionData(KnnSubmission submission) {
        return new KnnSubmissionDto(submission.getSubmission());
    }

}
