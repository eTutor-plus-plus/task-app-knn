package at.jku.dke.task_app.knn.data.entities;

import at.jku.dke.etutor.task_app.data.entities.BaseSubmission;
import at.jku.dke.etutor.task_app.dto.SubmissionMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

/**
 * Entity for a submission to a knn task.
 * <p>
 * Stores the user's answer for a {@link KnnTask}.
 * Inherits metadata from {@link BaseSubmission}.
 * </p>
 */
@Entity
@Table(name = "submission")
public class KnnSubmission extends BaseSubmission<KnnTask> {

    /** The submitted answer. */
    @NotNull
    @Column(name = "submission", nullable = false)
    private String submission;

    /** Default constructor. */
    public KnnSubmission() {
    }

    /**
     * Constructor with answer only.
     * @param submission The submitted answer.
     */
    public KnnSubmission(String submission) {
        this.submission = submission;
    }

    /**
     * Full constructor with all metadata.
     */
    public KnnSubmission(String userId, String assignmentId, KnnTask task, String language, int feedbackLevel, SubmissionMode mode, String submission) {
        super(userId, assignmentId, task, language, feedbackLevel, mode);
        this.submission = submission;
    }

    /** @return The submitted answer. */
    public String getSubmission() {
        return submission;
    }

    /** @param submission The answer to set. */
    public void setSubmission(String submission) {
        this.submission = submission;
    }
}
