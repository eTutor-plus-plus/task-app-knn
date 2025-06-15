package at.jku.dke.task_app.knn.controllers;

import at.jku.dke.etutor.task_app.controllers.BaseSubmissionController;
import at.jku.dke.task_app.knn.dto.KnnSubmissionDto;
import at.jku.dke.task_app.knn.services.KnnSubmissionService;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for knn task submissions.
 */
@RestController
public class SubmissionController extends BaseSubmissionController<KnnSubmissionDto> {
    /**
     * Constructor.
     * @param submissionService Service for submission logic.
     */
    public SubmissionController(KnnSubmissionService submissionService) {
        super(submissionService);
    }
}
