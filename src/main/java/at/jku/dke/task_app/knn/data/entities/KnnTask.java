package at.jku.dke.task_app.knn.data.entities;

import at.jku.dke.etutor.task_app.data.entities.BaseTask;
import at.jku.dke.etutor.task_app.dto.TaskStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Entity for a single knn task.
 * <p>
 * Stores the solution and descriptions for the task.
 * </p>
 */
@Entity
@Table(name = "task")
public class KnnTask extends BaseTask {

    /** Details for this knn task (one-to-one). */
    @OneToOne(mappedBy = "task",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY)
    private KnnTaskEntity entity;

    /** German description of the task. */
    @Column(name = "description_de", columnDefinition = "TEXT")
    private String descriptionDe;

    /** English description of the task. */
    @Column(name = "description_en", columnDefinition = "TEXT")
    private String descriptionEn;

    /**
     * The correct solution for the task.
     * <p>Can be null if not set or if grading is manual.</p>
     */
    @Column(name = "solution", nullable = true)
    private String solution;

    /** Default constructor. */
    public KnnTask() {}

    /**
     * Constructor with solution only.
     * @param solution The correct solution.
     */
    public KnnTask(String solution) {
        this.solution = solution;
    }

    /**
     * Constructor with points, status, group, and solution.
     */
    public KnnTask(BigDecimal maxPoints, TaskStatus status, String solution) {
        super(maxPoints, status);
        this.solution = solution;
    }

    /**
     * Constructor with id, points, status, group, and solution.
     */
    public KnnTask(Long id, BigDecimal maxPoints, TaskStatus status, String solution) {
        super(id, maxPoints, status);
        this.solution = solution;
    }

    /** @return German description. */
    public String getDescriptionDe() {
        return descriptionDe;
    }
    /** @param descriptionDe German description. */
    public void setDescriptionDe(String descriptionDe) {
        this.descriptionDe = descriptionDe;
    }

    /** @return English description. */
    public String getDescriptionEn() {
        return descriptionEn;
    }
    /** @param descriptionEn English description. */
    public void setDescriptionEn(String descriptionEn) {
        this.descriptionEn = descriptionEn;
    }

    /** @return Task details entity. */
    public KnnTaskEntity getEntity() {
        return entity;
    }

    /**
     * Sets or changes the details entity, updating the bidirectional reference.
     */
    public void setEntity(KnnTaskEntity entity) {
        if (this.entity != null)
            this.entity.setTask(null);
        this.entity = entity;
        if (entity != null)
            entity.setTask(this);
    }

    /** @return The correct solution (can be null). */
    public String getSolution() {
        return solution;
    }

    /** @param solution The correct solution. */
    public void setSolution(String solution) {
        this.solution = solution;
    }
}
